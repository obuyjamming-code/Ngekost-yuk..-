package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.APP_OWNER_EMAIL
import com.example.data.model.BoostTier
import com.example.data.model.FavoriteListing
import com.example.data.model.FeeCalculator
import com.example.data.model.IndonesiaRegion
import com.example.data.model.MonetizationOrder
import com.example.data.model.MusicPlatform
import com.example.data.model.MusicTrack
import com.example.data.model.OwnerConfig
import com.example.data.model.PayoutRequest
import com.example.data.model.PriceSortOption
import com.example.data.model.PriceTierFilter
import com.example.data.model.PropertyListing
import com.example.data.model.PropertyType
import com.example.data.model.ReferralCommission
import com.example.data.model.UserProfile
import com.example.data.repository.PropertyRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ExploreFilterState(
    val searchQuery: String = "",
    val selectedType: PropertyType? = null,
    val selectedRegion: IndonesiaRegion = IndonesiaRegion.ALL,
    val selectedPriceTier: PriceTierFilter = PriceTierFilter.ALL,
    val sortOption: PriceSortOption = PriceSortOption.CHEAPEST_FIRST,
    val isMapView: Boolean = false
)

class MainViewModel(
    private val repository: PropertyRepository,
    val currentUserId: String,
    private val currentUserEmail: String,
    private val currentUserName: String,
    private val currentFirebaseUser: FirebaseUser?
) : ViewModel() {

    val isAppOwner: Boolean =
        currentUserEmail.equals(APP_OWNER_EMAIL, ignoreCase = true) &&
            (currentFirebaseUser?.isEmailVerified == true)

    private val _filterState = MutableStateFlow(ExploreFilterState())
    val filterState: StateFlow<ExploreFilterState> = _filterState.asStateFlow()

    private val _isMusicBarVisible = MutableStateFlow(false)
    val isMusicBarVisible: StateFlow<Boolean> = _isMusicBarVisible.asStateFlow()

    private val _isMusicSheetOpen = MutableStateFlow(false)
    val isMusicSheetOpen: StateFlow<Boolean> = _isMusicSheetOpen.asStateFlow()

    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()

    private val _selectedMusicPlatform = MutableStateFlow(MusicPlatform.YOUTUBE)
    val selectedMusicPlatform: StateFlow<MusicPlatform> = _selectedMusicPlatform.asStateFlow()

    private val _isWebPlayerExpanded = MutableStateFlow(false)
    val isWebPlayerExpanded: StateFlow<Boolean> = _isWebPlayerExpanded.asStateFlow()

    private val _ownerPinUnlocked = MutableStateFlow(false)
    val ownerPinUnlocked: StateFlow<Boolean> = _ownerPinUnlocked.asStateFlow()

    private val _ownerPinCode = MutableStateFlow("2026")

    val listingsUiState: StateFlow<UiState<List<PropertyListing>>> =
        repository.observePublicListings()
            .asUiState("Gagal memuat daftar properti.")

    val filteredListings: StateFlow<List<PropertyListing>> = combine(
        listingsUiState,
        _filterState
    ) { state, filter ->
        val raw = (state as? UiState.Success)?.data ?: emptyList()
        val q = filter.searchQuery.trim().lowercase()
        val filtered = raw.filter { item ->
            val matchesQuery = q.isBlank() ||
                item.title.lowercase().contains(q) ||
                item.city.lowercase().contains(q) ||
                item.province.lowercase().contains(q) ||
                item.address.lowercase().contains(q)
            val matchesType = filter.selectedType == null || item.resolvedType() == filter.selectedType
            val matchesRegion = filter.selectedRegion == IndonesiaRegion.ALL ||
                item.resolvedRegion() == filter.selectedRegion
            val matchesTier = when (filter.selectedPriceTier) {
                PriceTierFilter.ALL -> true
                else -> {
                    val minOk = item.priceIdr >= filter.selectedPriceTier.minIdr
                    val maxOk = filter.selectedPriceTier.maxIdr?.let { item.priceIdr <= it } ?: true
                    minOk && maxOk
                }
            }
            matchesQuery && matchesType && matchesRegion && matchesTier
        }
        when (filter.sortOption) {
            PriceSortOption.CHEAPEST_FIRST -> filtered.sortedWith(
                compareByDescending<PropertyListing> { it.featuredRank }
                    .thenBy { it.priceIdr }
            )
            PriceSortOption.EXPENSIVE_FIRST -> filtered.sortedWith(
                compareByDescending<PropertyListing> { it.featuredRank }
                    .thenByDescending { it.priceIdr }
            )
            PriceSortOption.NEWEST -> filtered.sortedWith(
                compareByDescending<PropertyListing> { it.featuredRank }
                    .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            )
            PriceSortOption.TOP_RATED -> filtered.sortedWith(
                compareByDescending<PropertyListing> { it.featuredRank }
                    .thenByDescending { it.rating }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val myListings: StateFlow<List<PropertyListing>> =
        repository.observeMyListings(currentUserId)
            .safeCatch(emptyList(), "Gagal memuat usaha saya")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val favorites: StateFlow<List<FavoriteListing>> =
        repository.observeFavorites(currentUserId)
            .safeCatch(emptyList(), "Gagal memuat favorit")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val favoriteIds: StateFlow<Set<String>> = favorites
        .map { list -> list.map { it.listingId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

    val userProfile: StateFlow<UserProfile?> =
        repository.observeUserProfile(currentUserId)
            .safeCatch(null, "Gagal memuat profil")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val ownerConfig: StateFlow<OwnerConfig> =
        repository.observeOwnerConfig()
            .safeCatch(OwnerConfig(), "Gagal memuat tarif aplikasi")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OwnerConfig())

    val myOrders: StateFlow<List<MonetizationOrder>> =
        repository.observeMyOrders(currentUserId)
            .safeCatch(emptyList(), "Gagal memuat riwayat order")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val myCommissions: StateFlow<List<ReferralCommission>> =
        repository.observeMyCommissions(currentUserId)
            .safeCatch(emptyList(), "Gagal memuat komisi")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val myPayouts: StateFlow<List<PayoutRequest>> =
        repository.observeMyPayoutRequests(currentUserId)
            .safeCatch(emptyList(), "Gagal memuat penarikan")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val allOrdersOwner: StateFlow<List<MonetizationOrder>> =
        repository.observeAllOrdersForOwner()
            .safeCatch(emptyList(), "Gagal memuat order pemilik")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val allCommissionsOwner: StateFlow<List<ReferralCommission>> =
        repository.observeAllCommissionsForOwner()
            .safeCatch(emptyList(), "Gagal memuat komisi pemilik")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val allPayoutsOwner: StateFlow<List<PayoutRequest>> =
        repository.observeAllPayoutRequestsForOwner()
            .safeCatch(emptyList(), "Gagal memuat pencairan pemilik")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val musicTracks: StateFlow<List<MusicTrack>> =
        repository.observeMusicTracks()
            .safeCatch(emptyList(), "Gagal memuat playlist Obuy Jamming")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        viewModelScope.launch {
            currentFirebaseUser?.let { repository.ensureUserProfile(it) }
            repository.seedInitialIndonesiaCatalogIfEmpty(currentUserId, currentUserName)
            if (isAppOwner) {
                repository.ensureOwnerConfigIfOwner()
                repository.seedObuyJammingTracksIfOwner()
            }
        }
    }

    // --- EXPLORE FILTERS ---

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun selectPropertyType(type: PropertyType?) {
        _filterState.value = _filterState.value.copy(selectedType = type)
    }

    fun selectRegion(region: IndonesiaRegion) {
        _filterState.value = _filterState.value.copy(selectedRegion = region)
    }

    fun selectPriceTier(tier: PriceTierFilter) {
        _filterState.value = _filterState.value.copy(selectedPriceTier = tier)
    }

    fun selectSortOption(option: PriceSortOption) {
        _filterState.value = _filterState.value.copy(sortOption = option)
    }

    fun setMapView(isMap: Boolean) {
        _filterState.value = _filterState.value.copy(isMapView = isMap)
    }

    // --- LISTINGS & FAVORITES ---

    fun toggleFavorite(listing: PropertyListing) {
        val currentlyFav = favoriteIds.value.contains(listing.id)
        viewModelScope.launch {
            repository.toggleFavorite(currentUserId, listing, currentlyFav)
        }
    }

    fun submitListing(
        listing: PropertyListing,
        editingId: String?,
        requestedBoost: BoostTier,
        paymentNote: String,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            val result = if (editingId.isNullOrBlank()) {
                repository.createListing(listing.copy(ownerDisplayName = currentUserName))
            } else {
                repository.updateListing(editingId, listing).map { editingId }
            }
            result.onSuccess { savedId ->
                if (requestedBoost != BoostTier.NONE) {
                    val cfg = ownerConfig.value
                    val boostPrice = when (requestedBoost) {
                        BoostTier.BOOST_7D -> cfg.boost7dIdr
                        BoostTier.BOOST_30D -> cfg.boost30dIdr
                        BoostTier.BOOST_90D -> cfg.boost90dIdr
                        BoostTier.NONE -> 0L
                    }
                    val profile = userProfile.value
                    val hasRef = !profile?.referredByUid.isNullOrBlank()
                    val rate = if (hasRef) cfg.promoterFeePercent else 0L
                    repository.createMonetizationOrder(
                        buyerName = currentUserName,
                        listing = listing.copy(id = savedId),
                        orderType = requestedBoost.name,
                        grossAmountIdr = boostPrice,
                        promoterCode = profile?.referredByCode ?: "",
                        promoterUid = profile?.referredByUid ?: "",
                        promoterFeeRatePercent = rate,
                        paymentNote = paymentNote.ifBlank { "Ajuan ${requestedBoost.label}" }
                    )
                }
            }
            onResult(result)
        }
    }

    fun deleteListing(listingId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.deleteListing(listingId))
        }
    }

    // --- MONETIZATION & REFERRAL ---

    fun applyReferralCode(code: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.applyReferralCode(currentUserId, code))
        }
    }

    fun submitBookingOrder(
        listing: PropertyListing,
        customPromoCode: String,
        paymentNote: String,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            val cfg = ownerConfig.value
            val feeAmount = FeeCalculator.calculateBookingPlatformFee(
                propertyPriceIdr = listing.priceIdr,
                feePercent = cfg.bookingFeePercent
            )
            val profile = userProfile.value
            val codeToCheck = customPromoCode.trim().uppercase()
                .ifBlank { profile?.referredByCode ?: "" }
            val lookup = if (codeToCheck.isNotBlank()) repository.resolveReferralCode(codeToCheck) else null
            val promoterUid = lookup?.promoterUid?.takeIf { it != currentUserId } ?: ""
            val promoterCode = if (promoterUid.isNotBlank()) lookup?.code ?: "" else ""
            val rate = if (promoterUid.isNotBlank()) cfg.promoterFeePercent else 0L

            val res = repository.createMonetizationOrder(
                buyerName = currentUserName,
                listing = listing,
                orderType = "BOOKING_FEE",
                grossAmountIdr = feeAmount,
                promoterCode = promoterCode,
                promoterUid = promoterUid,
                promoterFeeRatePercent = rate,
                paymentNote = paymentNote.ifBlank { "Booking ${listing.title}" }
            )
            onResult(res)
        }
    }

    fun submitBoostOrderForExisting(
        listing: PropertyListing,
        boostTier: BoostTier,
        paymentNote: String,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            val cfg = ownerConfig.value
            val price = when (boostTier) {
                BoostTier.BOOST_7D -> cfg.boost7dIdr
                BoostTier.BOOST_30D -> cfg.boost30dIdr
                BoostTier.BOOST_90D -> cfg.boost90dIdr
                BoostTier.NONE -> 0L
            }
            val profile = userProfile.value
            val hasRef = !profile?.referredByUid.isNullOrBlank()
            val rate = if (hasRef) cfg.promoterFeePercent else 0L
            val res = repository.createMonetizationOrder(
                buyerName = currentUserName,
                listing = listing,
                orderType = boostTier.name,
                grossAmountIdr = price,
                promoterCode = profile?.referredByCode ?: "",
                promoterUid = profile?.referredByUid ?: "",
                promoterFeeRatePercent = rate,
                paymentNote = paymentNote.ifBlank { "Upgrade ${boostTier.label}" }
            )
            onResult(res)
        }
    }

    fun requestPayout(
        amountIdr: Long,
        payoutMethod: String,
        accountNumber: String,
        accountHolder: String,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            val code = userProfile.value?.referralCode ?: "NKUSER"
            onResult(
                repository.requestPayout(
                    promoterName = currentUserName,
                    promoterCode = code,
                    amountIdr = amountIdr,
                    payoutMethod = payoutMethod,
                    accountNumber = accountNumber,
                    accountHolder = accountHolder
                )
            )
        }
    }

    // --- OWNER VAULT ACTIONS ---

    fun unlockOwnerPin(pin: String): Boolean {
        val ok = isAppOwner && pin.trim() == _ownerPinCode.value
        if (ok) _ownerPinUnlocked.value = true
        return ok
    }

    fun updateOwnerPin(newPin: String) {
        if (isAppOwner && newPin.trim().length in 4..8) {
            _ownerPinCode.value = newPin.trim()
        }
    }

    fun lockOwnerPin() {
        _ownerPinUnlocked.value = false
    }

    fun verifyOrderByOwner(order: MonetizationOrder, approve: Boolean, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.verifyOrRejectOrderByOwner(order, approve))
        }
    }

    fun processPayoutByOwner(
        payout: PayoutRequest,
        approvePaid: Boolean,
        adminNote: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(repository.processPayoutByOwner(payout, approvePaid, adminNote))
        }
    }

    fun updateOwnerConfig(config: OwnerConfig, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.updateOwnerConfig(config))
        }
    }

    // --- OBUY JAMMING MUSIC PLAYER ---

    fun openMusicBarAndPlay(index: Int = _currentTrackIndex.value) {
        val list = musicTracks.value
        if (list.isNotEmpty()) {
            _currentTrackIndex.value = index.coerceIn(0, list.lastIndex)
        }
        _isMusicBarVisible.value = true
    }

    fun toggleMusicSheet(open: Boolean) {
        _isMusicSheetOpen.value = open
        if (open) _isMusicBarVisible.value = true
    }

    fun toggleWebPlayerExpanded() {
        _isWebPlayerExpanded.value = !_isWebPlayerExpanded.value
        _isMusicBarVisible.value = true
    }

    fun hideMusicBar() {
        _isMusicBarVisible.value = false
        _isMusicSheetOpen.value = false
        _isWebPlayerExpanded.value = false
    }

    fun selectTrack(index: Int) {
        val list = musicTracks.value
        if (list.isNotEmpty()) {
            _currentTrackIndex.value = index.coerceIn(0, list.lastIndex)
            _isMusicBarVisible.value = true
        }
    }

    fun nextTrack() {
        val list = musicTracks.value
        if (list.isNotEmpty()) {
            _currentTrackIndex.value = (_currentTrackIndex.value + 1) % list.size
        }
    }

    fun prevTrack() {
        val list = musicTracks.value
        if (list.isNotEmpty()) {
            _currentTrackIndex.value = (_currentTrackIndex.value - 1 + list.size) % list.size
        }
    }

    fun selectMusicPlatform(platform: MusicPlatform) {
        _selectedMusicPlatform.value = platform
    }

    fun addMusicTrackByOwner(track: MusicTrack, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.addMusicTrackByOwner(track))
        }
    }

    fun deleteMusicTrackByOwner(trackId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.deleteMusicTrackByOwner(trackId))
        }
    }

    // --- FLOW HELPERS ---

    private fun <T> Flow<T>.asUiState(fallbackMsg: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackMsg, rootError)
                emit(UiState.Error(rootError.message ?: fallbackMsg))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    private fun <T> Flow<T>.safeCatch(defaultValue: T, logMsg: String): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, logMsg, firestoreError ?: error)
            emit(defaultValue)
        }

    private companion object {
        const val TAG = "MainViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
