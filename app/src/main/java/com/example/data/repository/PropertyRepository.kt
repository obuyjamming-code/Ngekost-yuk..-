package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.APP_OWNER_EMAIL
import com.example.data.model.BillingPeriod
import com.example.data.model.BoostTier
import com.example.data.model.FavoriteListing
import com.example.data.model.FeeCalculator
import com.example.data.model.MonetizationOrder
import com.example.data.model.MusicTrack
import com.example.data.model.OwnerConfig
import com.example.data.model.PayoutRequest
import com.example.data.model.PropertyListing
import com.example.data.model.PropertyType
import com.example.data.model.ReferralCodeLookup
import com.example.data.model.ReferralCommission
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PropertyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
    }

    private fun isCurrentUserAppOwner(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email.equals(APP_OWNER_EMAIL, ignoreCase = true) && user.isEmailVerified
    }

    // --- LISTINGS ---

    fun observePublicListings(): Flow<List<PropertyListing>> = flow {
        val uid = requireUserId()
        val path = "listings"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PropertyListing::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleFlowError(error, OperationType.LIST, path, uid); throw error }
        )
    }

    fun observeMyListings(ownerId: String): Flow<List<PropertyListing>> = flow {
        val uid = requireUserId()
        val path = "listings"
        emitAll(
            db.collection(path)
                .whereEqualTo("ownerId", ownerId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PropertyListing::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleFlowError(error, OperationType.LIST, path, uid); throw error }
        )
    }

    fun observeFavorites(userId: String): Flow<List<FavoriteListing>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/favorites"
        emitAll(
            db.collection("users").document(userId).collection("favorites")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FavoriteListing::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleFlowError(error, OperationType.LIST, path, uid); throw error }
        )
    }

    // --- USER PROFILE & REFERRAL CODE ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error -> handleFlowError(error, OperationType.GET, path, uid); throw error }
        )
    }

    suspend fun ensureUserProfile(user: FirebaseUser): Result<UserProfile> {
        val path = "users/${user.uid}"
        return try {
            val uid = requireUserId()
            require(uid == user.uid) { "UID pengguna tidak sesuai." }
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val safeName = (user.displayName?.takeIf { it.isNotBlank() } ?: "Sahabat Ngekost").take(80)
                val safeEmail = (user.email?.takeIf { it.isNotBlank() } ?: "user@ngekostyuk.id").take(120)
                val cleanPrefix = safeName.filter { it.isLetterOrDigit() }.uppercase().take(4).ifBlank { "USER" }
                val suffix = uid.filter { it.isLetterOrDigit() }.uppercase().take(4).ifBlank { "88" }
                val generatedCode = "NK$cleanPrefix$suffix".take(16)

                val batch = db.batch()
                val userPayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "preferredRegion" to "Seluruh Indonesia",
                    "referralCode" to generatedCode,
                    "referredByCode" to "",
                    "referredByUid" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, userPayload)
                val lookupRef = db.collection("referral_codes").document(generatedCode)
                batch.set(
                    lookupRef,
                    mapOf(
                        "code" to generatedCode,
                        "promoterUid" to uid,
                        "promoterName" to safeName,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.commit().await()
                val created = docRef.get().await()
                    .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(userId = uid, displayName = safeName, email = safeEmail, referralCode = generatedCode)
                Result.success(created)
            } else {
                val existing = snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(userId = uid)
                Result.success(existing)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun applyReferralCode(userId: String, rawCode: String): Result<String> {
        val code = rawCode.trim().uppercase()
        val path = "referral_codes/$code"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Hanya dapat diterapkan ke akun sendiri." }
            require(code.length in 4..20 && code.matches(Regex("^[A-Za-z0-9_\\-]+$"))) {
                "Format kode referral tidak valid."
            }
            val lookupSnap = db.collection("referral_codes").document(code).get().await()
            if (!lookupSnap.exists()) {
                return Result.failure(IllegalArgumentException("Kode referral '$code' tidak ditemukan."))
            }
            val lookup = lookupSnap.toObject(ReferralCodeLookup::class.java)
                ?: return Result.failure(IllegalStateException("Data kode referral tidak terbaca."))
            if (lookup.promoterUid == uid) {
                return Result.failure(IllegalArgumentException("Tidak bisa menggunakan kode referral milik sendiri."))
            }
            db.collection("users").document(uid).update(
                mapOf(
                    "referredByCode" to lookup.code,
                    "referredByUid" to lookup.promoterUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(lookup.promoterName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun resolveReferralCode(rawCode: String): ReferralCodeLookup? {
        val code = rawCode.trim().uppercase()
        if (code.length !in 4..20 || !code.matches(Regex("^[A-Za-z0-9_\\-]+$"))) return null
        return try {
            val snap = db.collection("referral_codes").document(code).get().await()
            if (snap.exists()) snap.toObject(ReferralCodeLookup::class.java) else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "referral_codes/$code")
            null
        }
    }

    // --- MONETIZATION & OWNER CONFIG ---

    fun observeOwnerConfig(): Flow<OwnerConfig> = flow {
        val uid = requireUserId()
        val path = "owner_config/pricing"
        emitAll(
            db.collection("owner_config").document("pricing")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(OwnerConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: OwnerConfig()
                    } else {
                        OwnerConfig()
                    }
                }
                .catch { error -> handleFlowError(error, OperationType.GET, path, uid); throw error }
        )
    }

    suspend fun ensureOwnerConfigIfOwner(): Result<Unit> {
        if (!isCurrentUserAppOwner()) return Result.success(Unit)
        val path = "owner_config/pricing"
        return try {
            val docRef = db.collection("owner_config").document("pricing")
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val defaults = OwnerConfig()
                docRef.set(
                    mapOf(
                        "configKey" to "pricing",
                        "bankName" to defaults.bankName,
                        "bankAccountNumber" to defaults.bankAccountNumber,
                        "bankAccountHolder" to defaults.bankAccountHolder,
                        "boost7dIdr" to defaults.boost7dIdr,
                        "boost30dIdr" to defaults.boost30dIdr,
                        "boost90dIdr" to defaults.boost90dIdr,
                        "bookingFeePercent" to defaults.bookingFeePercent,
                        "promoterFeePercent" to defaults.promoterFeePercent,
                        "promoterProFeePercent" to defaults.promoterProFeePercent,
                        "minWithdrawIdr" to defaults.minWithdrawIdr,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateOwnerConfig(config: OwnerConfig): Result<Unit> {
        val path = "owner_config/pricing"
        return try {
            require(isCurrentUserAppOwner()) { "Hanya pemilik aplikasi yang dapat mengubah tarif." }
            db.collection("owner_config").document("pricing").set(
                mapOf(
                    "configKey" to "pricing",
                    "bankName" to config.bankName.trim().take(40),
                    "bankAccountNumber" to config.bankAccountNumber.trim().take(40),
                    "bankAccountHolder" to config.bankAccountHolder.trim().take(80),
                    "boost7dIdr" to config.boost7dIdr.coerceIn(10_000L, 10_000_000L),
                    "boost30dIdr" to config.boost30dIdr.coerceIn(10_000L, 20_000_000L),
                    "boost90dIdr" to config.boost90dIdr.coerceIn(10_000L, 50_000_000L),
                    "bookingFeePercent" to config.bookingFeePercent.coerceIn(1L, 25L),
                    "promoterFeePercent" to config.promoterFeePercent.coerceIn(5L, 40L),
                    "promoterProFeePercent" to config.promoterProFeePercent.coerceIn(config.promoterFeePercent, 50L),
                    "minWithdrawIdr" to config.minWithdrawIdr.coerceIn(10_000L, 1_000_000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeMyOrders(buyerId: String): Flow<List<MonetizationOrder>> = flow {
        val uid = requireUserId()
        val path = "monetization_orders"
        emitAll(
            db.collection(path)
                .whereEqualTo("buyerId", buyerId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MonetizationOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleFlowError(error, OperationType.LIST, path, uid); throw error }
        )
    }

    fun observeAllOrdersForOwner(): Flow<List<MonetizationOrder>> = flow {
        val uid = requireUserId()
        val path = "monetization_orders"
        if (!isCurrentUserAppOwner()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MonetizationOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleFlowError(error, OperationType.LIST, path, uid); throw error }
        )
    }

    suspend fun createMonetizationOrder(
        buyerName: String,
        listing: PropertyListing,
        orderType: String,
        grossAmountIdr: Long,
        promoterCode: String,
        promoterUid: String,
        promoterFeeRatePercent: Long,
        paymentNote: String
    ): Result<String> {
        var docPath = "monetization_orders"
        return try {
            val uid = requireUserId()
            val hasPromoter = promoterUid.isNotBlank() && promoterUid != uid && promoterCode.isNotBlank()
            val effectiveRate = if (hasPromoter) promoterFeeRatePercent.coerceIn(1L, 50L) else 0L
            val (promoterFee, ownerNet) = FeeCalculator.calculateFeeSplit(
                grossAmountIdr = grossAmountIdr,
                hasPromoter = hasPromoter,
                promoterRatePercent = effectiveRate
            )
            val docRef = db.collection("monetization_orders").document()
            docPath = docRef.path
            val payload = mapOf(
                "buyerId" to uid,
                "buyerName" to buyerName.trim().ifBlank { "Pengguna" }.take(80),
                "listingId" to listing.id.take(128),
                "listingTitle" to listing.title.trim().take(100),
                "orderType" to orderType,
                "propertyPriceIdr" to listing.priceIdr.coerceIn(0L, 500_000_000L),
                "grossAmountIdr" to grossAmountIdr.coerceIn(10_000L, 50_000_000L),
                "promoterCode" to if (hasPromoter) promoterCode.trim().take(20) else "",
                "promoterUid" to if (hasPromoter) promoterUid.trim().take(128) else "",
                "promoterFeeRatePercent" to effectiveRate,
                "promoterFeeIdr" to promoterFee,
                "ownerNetIdr" to ownerNet,
                "paymentNote" to paymentNote.trim().take(200),
                "status" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun verifyOrRejectOrderByOwner(order: MonetizationOrder, approve: Boolean): Result<Unit> {
        val docPath = "monetization_orders/${order.id}"
        return try {
            require(isCurrentUserAppOwner()) { "Hanya pemilik aplikasi yang dapat memverifikasi order." }
            require(order.id.isNotBlank() && order.status == "PENDING") { "Order tidak valid atau sudah diproses." }
            val batch = db.batch()
            val orderRef = db.collection("monetization_orders").document(order.id)
            batch.update(
                orderRef,
                mapOf(
                    "status" to if (approve) "VERIFIED" else "REJECTED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (approve) {
                if (order.orderType.startsWith("BOOST_") && order.listingId.isNotBlank()) {
                    val tier = BoostTier.fromRaw(order.orderType)
                    val listingRef = db.collection("listings").document(order.listingId)
                    batch.update(
                        listingRef,
                        mapOf(
                            "boostTier" to tier.name,
                            "featuredRank" to tier.rankWeight,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                if (order.promoterUid.isNotBlank() && order.promoterFeeIdr >= 1000L) {
                    val commRef = db.collection("referral_commissions").document()
                    batch.set(
                        commRef,
                        mapOf(
                            "promoterUid" to order.promoterUid,
                            "promoterCode" to order.promoterCode,
                            "orderId" to order.id,
                            "orderTitle" to "${order.orderType}: ${order.listingTitle}".take(120),
                            "grossOrderIdr" to order.grossAmountIdr,
                            "feePercent" to order.promoterFeeRatePercent.coerceIn(1L, 50L),
                            "commissionIdr" to order.promoterFeeIdr,
                            "status" to "AVAILABLE",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- COMMISSIONS & PAYOUTS ---

    fun observeMyCommissions(promoterUid: String): Flow<List<ReferralCommission>> = flow {
        val uid = requireUserId()
        val path = "referral_commissions"
        emitAll(
            db.collection(path)
                .whereEqualTo("promoterUid", promoterUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ReferralCommission::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleFlowError(error, OperationType.LIST, path, uid); throw error }
        )
    }

    fun observeAllCommissionsForOwner(): Flow<List<ReferralCommission>> = flow {
        val uid = requireUserId()
        val path = "referral_commissions"
        if (!isCurrentUserAppOwner()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ReferralCommission::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleFlowError(error, OperationType.LIST, path, uid); throw error }
        )
    }

    fun observeMyPayoutRequests(promoterUid: String): Flow<List<PayoutRequest>> = flow {
        val uid = requireUserId()
        val path = "payout_requests"
        emitAll(
            db.collection(path)
                .whereEqualTo("promoterUid", promoterUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PayoutRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleFlowError(error, OperationType.LIST, path, uid); throw error }
        )
    }

    fun observeAllPayoutRequestsForOwner(): Flow<List<PayoutRequest>> = flow {
        val uid = requireUserId()
        val path = "payout_requests"
        if (!isCurrentUserAppOwner()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PayoutRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleFlowError(error, OperationType.LIST, path, uid); throw error }
        )
    }

    suspend fun requestPayout(
        promoterName: String,
        promoterCode: String,
        amountIdr: Long,
        payoutMethod: String,
        accountNumber: String,
        accountHolder: String
    ): Result<String> {
        var docPath = "payout_requests"
        return try {
            val uid = requireUserId()
            require(amountIdr in 50_000L..100_000_000L) { "Minimal penarikan dana Rp 50.000." }
            require(accountNumber.trim().length in 4..40) { "Nomor rekening / e-wallet tidak valid." }
            require(accountHolder.trim().length in 2..80) { "Nama pemilik rekening minimal 2 karakter." }
            val docRef = db.collection("payout_requests").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "promoterUid" to uid,
                    "promoterName" to promoterName.trim().ifBlank { "Promotor" }.take(80),
                    "promoterCode" to promoterCode.trim().take(20),
                    "amountIdr" to amountIdr,
                    "payoutMethod" to payoutMethod.trim().take(30),
                    "accountNumber" to accountNumber.trim().take(40),
                    "accountHolder" to accountHolder.trim().take(80),
                    "status" to "REQUESTED",
                    "adminNote" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun processPayoutByOwner(
        payout: PayoutRequest,
        approvePaid: Boolean,
        adminNote: String
    ): Result<Unit> {
        val docPath = "payout_requests/${payout.id}"
        return try {
            require(isCurrentUserAppOwner()) { "Hanya pemilik aplikasi yang dapat memproses pencairan." }
            require(payout.id.isNotBlank() && payout.status == "REQUESTED") { "Pengajuan tidak valid." }
            db.collection("payout_requests").document(payout.id).update(
                mapOf(
                    "status" to if (approvePaid) "PAID" else "REJECTED",
                    "adminNote" to adminNote.trim().take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- OBUY JAMMING MUSIC TRACKS ---

    fun observeMusicTracks(): Flow<List<MusicTrack>> = flow {
        val uid = requireUserId()
        val path = "music_tracks"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snapshot ->
                    val docs = snapshot.toObjects(
                        MusicTrack::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    (if (docs.isNotEmpty()) docs else defaultObuyJammingTracks())
                        .sortedBy { it.sortOrder }
                }
                .catch { error -> handleFlowError(error, OperationType.LIST, path, uid); throw error }
        )
    }

    suspend fun seedObuyJammingTracksIfOwner(): Result<Unit> {
        if (!isCurrentUserAppOwner()) return Result.success(Unit)
        val path = "music_tracks"
        return try {
            val existing = db.collection(path).whereEqualTo("visibility", "public").limit(1).get().await()
            if (!existing.isEmpty) return Result.success(Unit)
            val batch = db.batch()
            defaultObuyJammingTracks().forEach { track ->
                val ref = db.collection(path).document()
                batch.set(
                    ref,
                    mapOf(
                        "title" to track.title,
                        "artist" to track.artist,
                        "genre" to track.genre,
                        "durationLabel" to track.durationLabel,
                        "youtubeUrl" to track.youtubeUrl,
                        "spotifyUrl" to track.spotifyUrl,
                        "metaUrl" to track.metaUrl,
                        "embedUrl" to track.embedUrl,
                        "sortOrder" to track.sortOrder,
                        "visibility" to "public",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun addMusicTrackByOwner(track: MusicTrack): Result<String> {
        var docPath = "music_tracks"
        return try {
            require(isCurrentUserAppOwner()) { "Hanya pemilik yang dapat mengelola lagu Obuy Jamming." }
            val title = track.title.trim()
            require(title.length in 2..100) { "Judul lagu minimal 2 karakter." }
            val ref = db.collection("music_tracks").document()
            docPath = ref.path
            val encodedQuery = java.net.URLEncoder.encode("Obuy Jamming $title", "UTF-8")
            val ytUrl = track.youtubeUrl.trim().ifBlank { "https://music.youtube.com/search?q=$encodedQuery" }.take(300)
            val spUrl = track.spotifyUrl.trim().ifBlank { "https://open.spotify.com/search/$encodedQuery" }.take(300)
            val mtUrl = track.metaUrl.trim().ifBlank { "https://www.facebook.com/search/top/?q=$encodedQuery" }.take(300)
            val embUrl = track.embedUrl.trim().ifBlank {
                "https://www.youtube.com/embed?listType=search&list=$encodedQuery"
            }.take(300)

            ref.set(
                mapOf(
                    "title" to title,
                    "artist" to track.artist.trim().ifBlank { "Obuy Jamming" }.take(80),
                    "genre" to track.genre.trim().ifBlank { "Reggae Nusantara" }.take(40),
                    "durationLabel" to track.durationLabel.trim().ifBlank { "03:45" }.take(20),
                    "youtubeUrl" to ytUrl,
                    "spotifyUrl" to spUrl,
                    "metaUrl" to mtUrl,
                    "embedUrl" to embUrl,
                    "sortOrder" to track.sortOrder.coerceIn(0L, 1000L),
                    "visibility" to "public",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMusicTrackByOwner(trackId: String): Result<Unit> {
        val docPath = "music_tracks/$trackId"
        return try {
            require(isCurrentUserAppOwner()) { "Hanya pemilik yang dapat menghapus lagu." }
            require(trackId.isNotBlank()) { "ID lagu tidak valid." }
            db.collection("music_tracks").document(trackId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- LISTING CRUD & SEEDING ---

    suspend fun seedInitialIndonesiaCatalogIfEmpty(ownerId: String, ownerName: String): Result<Unit> {
        val path = "listings"
        return try {
            val uid = requireUserId()
            require(uid == ownerId) { "UID tidak sesuai." }
            val existingSnap = db.collection(path)
                .whereEqualTo("visibility", "public")
                .get()
                .await()
            val existingTitles = existingSnap.documents
                .mapNotNull { it.getString("title") }
                .toSet()

            val starterItems = buildInitialIndonesianCatalog(uid, ownerName.ifBlank { "Mitra Ngekost" })
                .filter { !existingTitles.contains(it.title) }
            if (starterItems.isEmpty()) {
                return Result.success(Unit)
            }

            val batch = db.batch()
            starterItems.forEach { item ->
                val docRef = db.collection(path).document()
                batch.set(docRef, toCreatePayload(item))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createListing(input: PropertyListing): Result<String> {
        var docPath = "listings"
        return try {
            val uid = requireUserId()
            val sanitized = validateAndNormalizeListing(
                input.copy(ownerId = uid, boostTier = BoostTier.NONE.name, featuredRank = 0L)
            )
            val docRef = db.collection("listings").document()
            docPath = docRef.path
            docRef.set(toCreatePayload(sanitized)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateListing(listingId: String, updated: PropertyListing): Result<Unit> {
        val docPath = "listings/$listingId"
        return try {
            val uid = requireUserId()
            require(listingId.isNotBlank()) { "ID properti tidak valid." }
            val sanitized = validateAndNormalizeListing(updated.copy(ownerId = uid))
            val updateMap = mapOf(
                "title" to sanitized.title,
                "propertyType" to sanitized.propertyType,
                "description" to sanitized.description,
                "priceIdr" to sanitized.priceIdr,
                "billingPeriod" to sanitized.billingPeriod,
                "province" to sanitized.province,
                "city" to sanitized.city,
                "address" to sanitized.address,
                "latitude" to sanitized.latitude,
                "longitude" to sanitized.longitude,
                "contactName" to sanitized.contactName,
                "contactPhone" to sanitized.contactPhone,
                "contactWhatsapp" to sanitized.contactWhatsapp,
                "facilities" to sanitized.facilities,
                "photoUrls" to sanitized.photoUrls,
                "availableRooms" to sanitized.availableRooms,
                "visibility" to sanitized.visibility,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("listings").document(listingId).update(updateMap).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteListing(listingId: String): Result<Unit> {
        val docPath = "listings/$listingId"
        return try {
            requireUserId()
            require(listingId.isNotBlank()) { "ID properti tidak valid." }
            db.collection("listings").document(listingId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(
        userId: String,
        listing: PropertyListing,
        currentlyFavorite: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId/favorites/${listing.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Akses favorit hanya untuk pemilik akun." }
            require(listing.id.isNotBlank()) { "ID properti tidak valid." }
            val favRef = db.collection("users").document(uid).collection("favorites").document(listing.id)
            if (currentlyFavorite) {
                favRef.delete().await()
            } else {
                val payload = mapOf(
                    "listingId" to listing.id,
                    "title" to listing.title.take(100),
                    "city" to listing.city.take(60),
                    "priceIdr" to listing.priceIdr.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                favRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private fun handleFlowError(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    private fun validateAndNormalizeListing(item: PropertyListing): PropertyListing {
        val title = item.title.trim()
        require(title.length in 3..100) { "Nama properti minimal 3 dan maksimal 100 karakter." }
        val description = item.description.trim()
        require(description.length in 10..1500) { "Deskripsi minimal 10 dan maksimal 1500 karakter." }
        require(item.priceIdr in 50_000L..500_000_000L) { "Harga harus antara Rp 50.000 hingga Rp 500.000.000." }
        val province = item.province.trim()
        val city = item.city.trim()
        val address = item.address.trim()
        require(province.length in 2..60) { "Provinsi tidak valid." }
        require(city.length in 2..60) { "Kota tidak valid." }
        require(address.length in 5..250) { "Alamat lengkap minimal 5 karakter." }
        val contactName = item.contactName.trim()
        val contactPhone = item.contactPhone.trim()
        val contactWhatsapp = item.contactWhatsapp.trim().ifBlank { contactPhone }
        require(contactName.length in 2..80) { "Nama kontak minimal 2 karakter." }
        require(contactPhone.length in 8..25) { "Nomor telepon minimal 8 digit." }
        require(contactWhatsapp.length in 8..25) { "Nomor WhatsApp minimal 8 digit." }
        val photos = item.photoUrls.filter { it.length in 5..350_000 }.take(4)
        require(photos.isNotEmpty()) { "Minimal harus ada 1 foto bangunan." }

        val typeName = PropertyType.fromRaw(item.propertyType).name
        val periodName = BillingPeriod.fromRaw(item.billingPeriod).name
        val safeFacilities = item.facilities.map { it.trim().take(40) }.filter { it.isNotBlank() }.take(15)
        val safeVisibility = if (item.visibility == "hidden") "hidden" else "public"

        return item.copy(
            ownerDisplayName = item.ownerDisplayName.trim().ifBlank { contactName }.take(80),
            title = title,
            propertyType = typeName,
            description = description,
            billingPeriod = periodName,
            countryCode = item.countryCode.ifBlank { "ID" }.take(4),
            countryName = item.countryName.ifBlank { "Indonesia" }.take(60),
            province = province,
            city = city,
            address = address,
            latitude = item.latitude.coerceIn(-90.0, 90.0),
            longitude = item.longitude.coerceIn(-180.0, 180.0),
            contactName = contactName,
            contactPhone = contactPhone,
            contactWhatsapp = contactWhatsapp,
            facilities = safeFacilities,
            photoUrls = photos,
            rating = item.rating.coerceIn(0.0, 5.0),
            availableRooms = item.availableRooms.coerceIn(0L, 500L),
            visibility = safeVisibility
        )
    }

    private fun toCreatePayload(item: PropertyListing): Map<String, Any> = mapOf(
        "ownerId" to item.ownerId,
        "ownerDisplayName" to item.ownerDisplayName,
        "title" to item.title,
        "propertyType" to item.propertyType,
        "description" to item.description,
        "priceIdr" to item.priceIdr,
        "billingPeriod" to item.billingPeriod,
        "countryCode" to item.countryCode,
        "countryName" to item.countryName,
        "province" to item.province,
        "city" to item.city,
        "address" to item.address,
        "latitude" to item.latitude,
        "longitude" to item.longitude,
        "contactName" to item.contactName,
        "contactPhone" to item.contactPhone,
        "contactWhatsapp" to item.contactWhatsapp,
        "facilities" to item.facilities,
        "photoUrls" to item.photoUrls,
        "rating" to item.rating,
        "availableRooms" to item.availableRooms,
        "visibility" to item.visibility,
        "boostTier" to item.boostTier,
        "featuredRank" to item.featuredRank,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun defaultObuyJammingTracks(): List<MusicTrack> = listOf(
        MusicTrack(
            id = "obuy_1",
            title = "Lalat, bunga dan kupu kupu",
            artist = "Obuy Jamming",
            genre = "Reggae Nusantara",
            durationLabel = "04:12",
            youtubeUrl = "https://music.youtube.com/search?q=Obuy+Jamming+Lalat+bunga+dan+kupu+kupu",
            spotifyUrl = "https://open.spotify.com/search/Obuy%20Jamming%20Lalat%20bunga",
            metaUrl = "https://www.facebook.com/search/top/?q=Obuy%20Jamming%20Lalat%20bunga",
            embedUrl = "https://www.youtube.com/embed?listType=search&list=Obuy+Jamming+Lalat+bunga+dan+kupu+kupu",
            sortOrder = 1L
        ),
        MusicTrack(
            id = "obuy_2",
            title = "Indah Pada Waktu Nya",
            artist = "Obuy Jamming",
            genre = "Reggae Santai",
            durationLabel = "03:54",
            youtubeUrl = "https://music.youtube.com/search?q=Obuy+Jamming+Indah+Pada+Waktu+Nya",
            spotifyUrl = "https://open.spotify.com/search/Obuy%20Jamming%20Indah%20Pada%20Waktunya",
            metaUrl = "https://www.instagram.com/explore/search/keyword/?q=Obuy%20Jamming",
            embedUrl = "https://www.youtube.com/embed?listType=search&list=Obuy+Jamming+Indah+Pada+Waktu+Nya",
            sortOrder = 2L
        ),
        MusicTrack(
            id = "obuy_3",
            title = "Lampu Hijau",
            artist = "Obuy Jamming",
            genre = "Reggae",
            durationLabel = "03:40",
            youtubeUrl = "https://music.youtube.com/search?q=Obuy+Jamming+Lampu+Hijau",
            spotifyUrl = "https://open.spotify.com/search/Obuy%20Jamming%20Lampu%20Hijau",
            metaUrl = "https://www.facebook.com/search/top/?q=Obuy%20Jamming%20Lampu%20Hijau",
            embedUrl = "https://www.youtube.com/embed?listType=search&list=Obuy+Jamming+Lampu+Hijau",
            sortOrder = 3L
        ),
        MusicTrack(
            id = "obuy_4",
            title = "Sintetis",
            artist = "Obuy Jamming",
            genre = "Reggae Indie",
            durationLabel = "03:28",
            youtubeUrl = "https://music.youtube.com/search?q=Obuy+Jamming+Sintetis",
            spotifyUrl = "https://open.spotify.com/search/Obuy%20Jamming%20Sintetis",
            metaUrl = "https://www.facebook.com/search/top/?q=Obuy%20Jamming%20Sintetis",
            embedUrl = "https://www.youtube.com/embed?listType=search&list=Obuy+Jamming+Sintetis",
            sortOrder = 4L
        ),
        MusicTrack(
            id = "obuy_5",
            title = "Kang O",
            artist = "Obuy Jamming",
            genre = "Reggae Grooves",
            durationLabel = "03:35",
            youtubeUrl = "https://music.youtube.com/search?q=Obuy+Jamming+Kang+O",
            spotifyUrl = "https://open.spotify.com/search/Obuy%20Jamming%20Kang%20O",
            metaUrl = "https://www.instagram.com/explore/search/keyword/?q=Obuy%20Jamming",
            embedUrl = "https://www.youtube.com/embed?listType=search&list=Obuy+Jamming+Kang+O",
            sortOrder = 5L
        )
    )

    private fun buildInitialIndonesianCatalog(ownerId: String, ownerName: String): List<PropertyListing> = listOf(
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Kosan Melati Hemat Dekat UGM",
            propertyType = PropertyType.KOSAN.name,
            description = "Kosan bersih, aman, dan sangat ramah kantong mahasiswa di kawasan Pogung Yogyakarta. Akses jalan kaki ke kampus UGM, dekat warung makan dan laundry.",
            priceIdr = 450_000L,
            billingPeriod = BillingPeriod.PER_BULAN.name,
            province = "DI Yogyakarta",
            city = "Sleman",
            address = "Jl. Pogung Baru Blok C No. 14, Mlati, Sleman",
            latitude = -7.7615,
            longitude = 110.3748,
            contactName = "Ibu Sri Wahyuni",
            contactPhone = "+6281227104455",
            contactWhatsapp = "6281227104455",
            facilities = listOf("WiFi Kencang", "Kasur & Lemari", "Parkir Motor", "Dapur Bersama"),
            photoUrls = listOf("drawable:img_kosan_modern"),
            rating = 4.6,
            availableRooms = 4L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Kosan Paviliun Cendana Dago",
            propertyType = PropertyType.KOSAN.name,
            description = "Kamar kos nyaman dengan sirkulasi udara sejuk khas Bandung Utara. Tersedia kamar mandi dalam, air panas, dan meja belajar lapang untuk pekerja remote maupun mahasiswa ITB.",
            priceIdr = 1_350_000L,
            billingPeriod = BillingPeriod.PER_BULAN.name,
            province = "Jawa Barat",
            city = "Bandung",
            address = "Jl. Ir. H. Juanda Gg. Cendana No. 8, Coblong, Bandung",
            latitude = -6.8856,
            longitude = 107.6138,
            contactName = "Kang Dedi Kurniawan",
            contactPhone = "+6281320987711",
            contactWhatsapp = "6281320987711",
            facilities = listOf("WiFi Kencang", "Kamar Mandi Dalam", "Air Panas", "Meja Kerja", "Keamanan 24 Jam"),
            photoUrls = listOf("drawable:img_kosan_modern", "drawable:img_wisma_asri"),
            rating = 4.8,
            availableRooms = 3L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Wisma Arta Tropis Ubud",
            propertyType = PropertyType.WISMA.name,
            description = "Wisma butik bernuansa arsitektur Bali asri dengan taman tropis rindang. Cocok untuk staycation tenang maupun perjalanan kerja jangka pendek di Ubud.",
            priceIdr = 320_000L,
            billingPeriod = BillingPeriod.PER_MALAM.name,
            province = "Bali",
            city = "Ubud",
            address = "Jl. Hanoman No. 42, Ubud, Gianyar, Bali",
            latitude = -8.5132,
            longitude = 115.2634,
            contactName = "Bli Made Wijaya",
            contactPhone = "+6281999234510",
            contactWhatsapp = "6281999234510",
            facilities = listOf("WiFi Kencang", "AC", "Sarapan", "Air Panas", "Parkir Motor"),
            photoUrls = listOf("drawable:img_wisma_asri", "drawable:img_kosan_modern"),
            rating = 4.9,
            availableRooms = 5L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Kosan Eksekutif Kemang Raya",
            propertyType = PropertyType.KOSAN.name,
            description = "Hunian eksklusif di jantung Jakarta Selatan dengan akses cepat ke SCBD dan Kemang Village. Full furnished, smart door lock, dan layanan kebersihan mingguan.",
            priceIdr = 3_400_000L,
            billingPeriod = BillingPeriod.PER_BULAN.name,
            province = "DKI Jakarta",
            city = "Jakarta Selatan",
            address = "Jl. Kemang Timur No. 27, Mampang Prapatan, Jakarta Selatan",
            latitude = -6.2654,
            longitude = 106.8195,
            contactName = "Mbak Nadia Putri",
            contactPhone = "+6281188776655",
            contactWhatsapp = "6281188776655",
            facilities = listOf("WiFi Kencang", "AC", "Kamar Mandi Dalam", "Parkir Mobil", "Keamanan 24 Jam", "Mesin Cuci"),
            photoUrls = listOf("drawable:img_kosan_modern", "drawable:img_hotel_luxury"),
            rating = 4.9,
            availableRooms = 2L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Hotel Samudera Skyline Sudirman",
            propertyType = PropertyType.HOTEL.name,
            description = "Kamar suite modern menghadap gemerlap gedung pencakar langit Sudirman Jakarta. Dilengkapi fasilitas gym, kolam renang rooftop, dan sarapan prasmanan internasional.",
            priceIdr = 1_850_000L,
            billingPeriod = BillingPeriod.PER_MALAM.name,
            province = "DKI Jakarta",
            city = "Jakarta Pusat",
            address = "Jl. Jend. Sudirman Kav. 52, Tanah Abang, Jakarta Pusat",
            latitude = -6.2155,
            longitude = 106.8168,
            contactName = "Reservasi Hotel Samudera",
            contactPhone = "+622155509988",
            contactWhatsapp = "6281288990011",
            facilities = listOf("WiFi Kencang", "AC", "Sarapan", "Kolam Renang", "Parkir Mobil", "Keamanan 24 Jam"),
            photoUrls = listOf("drawable:img_hotel_luxury"),
            rating = 4.9,
            availableRooms = 12L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Kontrakan Asri Keluarga Citraland",
            propertyType = PropertyType.KONTRAKAN.name,
            description = "Rumah kontrakan 2 lantai siap huni untuk keluarga di kawasan barat Surabaya. Memiliki 3 kamar tidur, carport luas, taman depan sejuk, dan lingkungan klaster satu pintu.",
            priceIdr = 4_800_000L,
            billingPeriod = BillingPeriod.PER_BULAN.name,
            province = "Jawa Timur",
            city = "Surabaya",
            address = "Perumahan Taman Puspa Blok B2 No. 9, Sambikerep, Surabaya",
            latitude = -7.2879,
            longitude = 112.6485,
            contactName = "Pak Hendra Gunawan",
            contactPhone = "+6281550112233",
            contactWhatsapp = "6281550112233",
            facilities = listOf("Parkir Mobil", "AC", "Dapur Bersama", "Keamanan 24 Jam", "Kamar Mandi Dalam"),
            photoUrls = listOf("drawable:img_kontrakan_nyaman"),
            rating = 4.7,
            availableRooms = 1L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Wisma Harmoni Medan Johor",
            propertyType = PropertyType.WISMA.name,
            description = "Penginapan keluarga dan pekerja dinas di Medan dengan harga bersahabat. Dekat pusat kuliner, akses mudah ke Bandara Kualanamu via tol.",
            priceIdr = 250_000L,
            billingPeriod = BillingPeriod.PER_MALAM.name,
            province = "Sumatera Utara",
            city = "Medan",
            address = "Jl. Karya Wisata No. 88, Medan Johor, Medan",
            latitude = 3.5326,
            longitude = 98.6657,
            contactName = "Bang Rizal Harahap",
            contactPhone = "+6281260443322",
            contactWhatsapp = "6281260443322",
            facilities = listOf("WiFi Kencang", "AC", "Parkir Mobil", "Sarapan"),
            photoUrls = listOf("drawable:img_wisma_asri"),
            rating = 4.5,
            availableRooms = 6L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Kosan Pantai Losari Makassar",
            propertyType = PropertyType.KOSAN.name,
            description = "Kosan bersih strategis hanya 5 menit dari Anjungan Pantai Losari. Cocok untuk karyawan perbankan, mahasiswa UNHAS/UMI, lengkap listrik dan WiFi cepat.",
            priceIdr = 950_000L,
            billingPeriod = BillingPeriod.PER_BULAN.name,
            province = "Sulawesi Selatan",
            city = "Makassar",
            address = "Jl. Penghibur Lr. 3 No. 12, Ujung Pandang, Makassar",
            latitude = -5.1432,
            longitude = 119.4079,
            contactName = "Daeng Andi Baso",
            contactPhone = "+6281144229900",
            contactWhatsapp = "6281144229900",
            facilities = listOf("WiFi Kencang", "AC", "Listrik Termasuk", "Parkir Motor"),
            photoUrls = listOf("drawable:img_kosan_modern"),
            rating = 4.6,
            availableRooms = 5L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Kontrakan Villa Tahunan Balikpapan",
            propertyType = PropertyType.KONTRAKAN.name,
            description = "Rumah kontrakan mewah 2 lantai dekat pusat bisnis Balikpapan dan akses cepat ke IKN Nusantara. Cocok untuk ekspatriat, tim proyek, atau keluarga besar.",
            priceIdr = 65_000_000L,
            billingPeriod = BillingPeriod.PER_TAHUN.name,
            province = "Kalimantan Timur",
            city = "Balikpapan",
            address = "Jl. Ruhui Rahayu II No. 19, Sepinggan, Balikpapan",
            latitude = -1.2485,
            longitude = 116.8851,
            contactName = "Ibu Ratna Sari",
            contactPhone = "+6281254887766",
            contactWhatsapp = "6281254887766",
            facilities = listOf("AC", "Parkir Mobil", "Keamanan 24 Jam", "Kamar Mandi Dalam", "Dapur Bersama"),
            photoUrls = listOf("drawable:img_kontrakan_nyaman", "drawable:img_hotel_luxury"),
            rating = 4.9,
            availableRooms = 1L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Hotel Teluk Youtefa Jayapura",
            propertyType = PropertyType.HOTEL.name,
            description = "Hotel nyaman menghadap Teluk Jayapura dengan kamar luas, restoran seafood segar, dan layanan antar-jemput Bandara Sentani.",
            priceIdr = 780_000L,
            billingPeriod = BillingPeriod.PER_MALAM.name,
            province = "Papua",
            city = "Jayapura",
            address = "Jl. Pantai Holtekamp No. 5, Jayapura Selatan, Papua",
            latitude = -2.6062,
            longitude = 140.7081,
            contactName = "Bapak Yohanes Tabuni",
            contactPhone = "+6281344556677",
            contactWhatsapp = "6281344556677",
            facilities = listOf("WiFi Kencang", "AC", "Sarapan", "Parkir Mobil", "Keamanan 24 Jam"),
            photoUrls = listOf("drawable:img_hotel_luxury", "drawable:img_wisma_asri"),
            rating = 4.7,
            availableRooms = 8L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Kosan Anggrek Murah Margonda",
            propertyType = PropertyType.KOSAN.name,
            description = "Kosan strategis 3 menit jalan kaki ke Stasiun UI dan kampus Depok. Harga paling hemat di kawasan Margonda, cocok untuk mahasiswa baru.",
            priceIdr = 650_000L,
            billingPeriod = BillingPeriod.PER_BULAN.name,
            province = "Jawa Barat",
            city = "Depok",
            address = "Jl. Kober Gang Sawo No. 7, Beji, Depok",
            latitude = -6.3702,
            longitude = 106.8321,
            contactName = "Bang Ujang Depok",
            contactPhone = "+6281290112244",
            contactWhatsapp = "6281290112244",
            facilities = listOf("WiFi Kencang", "Kasur & Lemari", "Parkir Motor", "Dapur Bersama"),
            photoUrls = listOf("drawable:img_kosan_modern"),
            rating = 4.5,
            availableRooms = 6L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Kosan Pahlawan Simpang Lima",
            propertyType = PropertyType.KOSAN.name,
            description = "Kosan bersih di pusat Kota Semarang dekat Simpang Lima dan kawasan perkantoran Pemuda. Bebas banjir dan akses angkutan umum 24 jam.",
            priceIdr = 850_000L,
            billingPeriod = BillingPeriod.PER_BULAN.name,
            province = "Jawa Tengah",
            city = "Semarang",
            address = "Jl. Pleburan Barat No. 21, Semarang Selatan",
            latitude = -6.9932,
            longitude = 110.4225,
            contactName = "Ibu Retno Semarang",
            contactPhone = "+6281565432100",
            contactWhatsapp = "6281565432100",
            facilities = listOf("WiFi Kencang", "AC", "Kamar Mandi Dalam", "Parkir Motor"),
            photoUrls = listOf("drawable:img_kosan_modern"),
            rating = 4.7,
            availableRooms = 3L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Kosan Brawijaya Sejuk Malang",
            propertyType = PropertyType.KOSAN.name,
            description = "Kosan adem khas Kota Malang dekat gerbang Universitas Brawijaya dan Suhat. Termasuk listrik wajar, air PDAM lancar, dan ruang tamu bersama.",
            priceIdr = 550_000L,
            billingPeriod = BillingPeriod.PER_BULAN.name,
            province = "Jawa Timur",
            city = "Malang",
            address = "Jl. Watugong No. 18, Lowokwaru, Malang",
            latitude = -7.9514,
            longitude = 112.6152,
            contactName = "Mas Arif Malang",
            contactPhone = "+6281334567890",
            contactWhatsapp = "6281334567890",
            facilities = listOf("WiFi Kencang", "Listrik Termasuk", "Parkir Motor", "Kasur & Lemari"),
            photoUrls = listOf("drawable:img_kosan_modern"),
            rating = 4.7,
            availableRooms = 7L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Wisma Keraton Surya Solo",
            propertyType = PropertyType.WISMA.name,
            description = "Wisma bergaya Jawa klasik yang tenang di tengah Kota Surakarta, hanya 5 menit dari Keraton dan Pasar Gede. Harga sangat terjangkau.",
            priceIdr = 185_000L,
            billingPeriod = BillingPeriod.PER_MALAM.name,
            province = "Jawa Tengah",
            city = "Surakarta (Solo)",
            address = "Jl. Slamet Riyadi Gg. Kusuma No. 10, Laweyan, Solo",
            latitude = -7.5689,
            longitude = 110.8082,
            contactName = "Pak Joko Widodo Putra",
            contactPhone = "+6281226655443",
            contactWhatsapp = "6281226655443",
            facilities = listOf("WiFi Kencang", "AC", "Sarapan", "Parkir Motor"),
            photoUrls = listOf("drawable:img_wisma_asri"),
            rating = 4.6,
            availableRooms = 4L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Hotel Resort Seminyak Bali",
            propertyType = PropertyType.HOTEL.name,
            description = "Resort mewah tepi pantai Kuta-Seminyak dengan infinity pool, spa privat, dan kamar suite balkon luas menghadap matahari terbenam.",
            priceIdr = 2_450_000L,
            billingPeriod = BillingPeriod.PER_MALAM.name,
            province = "Bali",
            city = "Kuta / Badung",
            address = "Jl. Kayu Aya Pantai No. 101, Seminyak, Badung, Bali",
            latitude = -8.6869,
            longitude = 115.1544,
            contactName = "Concierge Seminyak",
            contactPhone = "+62361730999",
            contactWhatsapp = "6281911223344",
            facilities = listOf("WiFi Kencang", "AC", "Kolam Renang", "Sarapan", "Parkir Mobil"),
            photoUrls = listOf("drawable:img_hotel_luxury", "drawable:img_wisma_asri"),
            rating = 4.9,
            availableRooms = 9L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Kosan Kebun Raya Bogor",
            propertyType = PropertyType.KOSAN.name,
            description = "Kosan bersih berudara sejuk dekat Kebun Raya dan kampus IPB Baranangsiang. Lingkungan asri, bebas bising, cocok untuk karyawan & mahasiswa.",
            priceIdr = 750_000L,
            billingPeriod = BillingPeriod.PER_BULAN.name,
            province = "Jawa Barat",
            city = "Bogor",
            address = "Jl. Malabar Ujung No. 15, Bogor Tengah, Bogor",
            latitude = -6.5934,
            longitude = 106.8058,
            contactName = "Teh Lina Bogor",
            contactPhone = "+6281281122334",
            contactWhatsapp = "6281281122334",
            facilities = listOf("WiFi Kencang", "Kamar Mandi Dalam", "Parkir Motor", "Dapur Bersama"),
            photoUrls = listOf("drawable:img_kosan_modern"),
            rating = 4.6,
            availableRooms = 3L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Kontrakan Minimalis BSD Tangerang",
            propertyType = PropertyType.KONTRAKAN.name,
            description = "Rumah kontrakan klaster modern di Tangerang Selatan/BSD dengan 2 kamar tidur, kanopi mobil, taman belakang, dan keamanan CCTV 24 jam.",
            priceIdr = 3_600_000L,
            billingPeriod = BillingPeriod.PER_BULAN.name,
            province = "Banten",
            city = "Tangerang",
            address = "Klaster Green Serpong Blok E4 No. 12, Tangerang",
            latitude = -6.2865,
            longitude = 106.6632,
            contactName = "Pak Benny Tangerang",
            contactPhone = "+6281198877665",
            contactWhatsapp = "6281198877665",
            facilities = listOf("AC", "Parkir Mobil", "Keamanan 24 Jam", "Kamar Mandi Dalam"),
            photoUrls = listOf("drawable:img_kontrakan_nyaman"),
            rating = 4.8,
            availableRooms = 1L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Kosan Darussalam Banda Aceh",
            propertyType = PropertyType.KOSAN.name,
            description = "Kosan syariah bersih dan nyaman dekat Universitas Syiah Kuala Darussalam. Tersedia mushola, ruang belajar, dan parkir motor beratap.",
            priceIdr = 380_000L,
            billingPeriod = BillingPeriod.PER_BULAN.name,
            province = "Aceh",
            city = "Banda Aceh",
            address = "Jl. T. Nyak Arief Lorong Indah No. 9, Syiah Kuala, Banda Aceh",
            latitude = 5.5681,
            longitude = 95.3672,
            contactName = "Cut Rahma Aceh",
            contactPhone = "+6281269001122",
            contactWhatsapp = "6281269001122",
            facilities = listOf("WiFi Kencang", "Kasur & Lemari", "Parkir Motor", "Keamanan 24 Jam"),
            photoUrls = listOf("drawable:img_kosan_modern"),
            rating = 4.7,
            availableRooms = 5L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Wisma Ranah Minang Padang",
            propertyType = PropertyType.WISMA.name,
            description = "Wisma keluarga strategis dekat Pantai Padang dan pusat kuliner Minang. Kamar luas, bersih, cocok untuk rombongan wisata dan pekerja dinas.",
            priceIdr = 240_000L,
            billingPeriod = BillingPeriod.PER_MALAM.name,
            province = "Sumatera Barat",
            city = "Padang",
            address = "Jl. Veteran Purus No. 33, Padang Barat, Padang",
            latitude = -0.9384,
            longitude = 100.3541,
            contactName = "Uda Fajar Padang",
            contactPhone = "+6281374005566",
            contactWhatsapp = "6281374005566",
            facilities = listOf("WiFi Kencang", "AC", "Sarapan", "Parkir Mobil"),
            photoUrls = listOf("drawable:img_wisma_asri"),
            rating = 4.6,
            availableRooms = 6L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Hotel Harbour View Batam Centre",
            propertyType = PropertyType.HOTEL.name,
            description = "Hotel bisnis modern dekat Pelabuhan Feri Internasional Batam Centre dan Mega Mall. Akses cepat ke kawasan industri Batamindo.",
            priceIdr = 680_000L,
            billingPeriod = BillingPeriod.PER_MALAM.name,
            province = "Kepulauan Riau",
            city = "Batam",
            address = "Jl. Engku Putri Kav. 12, Batam Centre, Batam",
            latitude = 1.1294,
            longitude = 104.0537,
            contactName = "Front Desk Batam",
            contactPhone = "+62778460112",
            contactWhatsapp = "6281270088990",
            facilities = listOf("WiFi Kencang", "AC", "Sarapan", "Parkir Mobil", "Keamanan 24 Jam"),
            photoUrls = listOf("drawable:img_hotel_luxury"),
            rating = 4.7,
            availableRooms = 10L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Kosan Ampera Ilir Palembang",
            propertyType = PropertyType.KOSAN.name,
            description = "Kosan lengkap di Palembang dekat stasiun LRT dan kampus UNSRI Bukit Besar. Kamar full AC, bebas jam malam untuk pekerja shift.",
            priceIdr = 700_000L,
            billingPeriod = BillingPeriod.PER_BULAN.name,
            province = "Sumatera Selatan",
            city = "Palembang",
            address = "Jl. Srijaya Negara No. 45, Ilir Barat I, Palembang",
            latitude = -2.9841,
            longitude = 104.7324,
            contactName = "Cek Ayu Palembang",
            contactPhone = "+6281271122334",
            contactWhatsapp = "6281271122334",
            facilities = listOf("WiFi Kencang", "AC", "Parkir Motor", "Listrik Termasuk"),
            photoUrls = listOf("drawable:img_kosan_modern"),
            rating = 4.6,
            availableRooms = 4L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Kontrakan Asri Pontianak Kota",
            propertyType = PropertyType.KONTRAKAN.name,
            description = "Rumah kontrakan nyaman di kawasan aman Pontianak Selatan. Memiliki 2 kamar tidur, air lancar dengan tandon besar, serta pagar besi rapat.",
            priceIdr = 1_900_000L,
            billingPeriod = BillingPeriod.PER_BULAN.name,
            province = "Kalimantan Barat",
            city = "Pontianak",
            address = "Jl. Podomoro Gg. Damai No. 8, Pontianak Selatan",
            latitude = -0.0485,
            longitude = 109.3321,
            contactName = "Pak Rudy Pontianak",
            contactPhone = "+6281256778899",
            contactWhatsapp = "6281256778899",
            facilities = listOf("Parkir Mobil", "Kamar Mandi Dalam", "Dapur Bersama", "Keamanan 24 Jam"),
            photoUrls = listOf("drawable:img_kontrakan_nyaman"),
            rating = 4.6,
            availableRooms = 1L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Wisma Bunaken Boulevard Manado",
            propertyType = PropertyType.WISMA.name,
            description = "Wisma bersih tepat di jalur Boulevard Manado menghadap laut. Akses mudah menuju pelabuhan wisata Bunaken dan pusat kuliner Wakeke.",
            priceIdr = 290_000L,
            billingPeriod = BillingPeriod.PER_MALAM.name,
            province = "Sulawesi Utara",
            city = "Manado",
            address = "Jl. Piere Tendean Boulevard No. 72, Sario, Manado",
            latitude = 1.4721,
            longitude = 124.8328,
            contactName = "Ko Steven Manado",
            contactPhone = "+6281243218765",
            contactWhatsapp = "6281243218765",
            facilities = listOf("WiFi Kencang", "AC", "Air Panas", "Parkir Mobil"),
            photoUrls = listOf("drawable:img_wisma_asri"),
            rating = 4.8,
            availableRooms = 5L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Hotel Komodo Marina Labuan Bajo",
            propertyType = PropertyType.HOTEL.name,
            description = "Hotel butik modern hanya 4 menit ke Dermaga Labuan Bajo dan 10 menit dari Bandara Komodo. Cocok untuk pelancong sailing trip NTT.",
            priceIdr = 1_150_000L,
            billingPeriod = BillingPeriod.PER_MALAM.name,
            province = "Nusa Tenggara Timur",
            city = "Labuan Bajo",
            address = "Jl. Soekarno Hatta No. 19, Komodo, Manggarai Barat, NTT",
            latitude = -8.4902,
            longitude = 119.8781,
            contactName = "Kak Maria Bajo",
            contactPhone = "+6281339887766",
            contactWhatsapp = "6281339887766",
            facilities = listOf("WiFi Kencang", "AC", "Kolam Renang", "Sarapan", "Air Panas"),
            photoUrls = listOf("drawable:img_hotel_luxury", "drawable:img_wisma_asri"),
            rating = 4.9,
            availableRooms = 7L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Kosan Senggigi Mataram Lombok",
            propertyType = PropertyType.KOSAN.name,
            description = "Kosan eksklusif rasa resort di Mataram Lombok. Tenang, dekat Epicentrum Mall, halaman parkir luas dan dapur bersama yang bersih.",
            priceIdr = 900_000L,
            billingPeriod = BillingPeriod.PER_BULAN.name,
            province = "Nusa Tenggara Barat",
            city = "Mataram",
            address = "Jl. Langko No. 54, Ampenan, Mataram, NTB",
            latitude = -8.5795,
            longitude = 116.0912,
            contactName = "Lalu Arif Mataram",
            contactPhone = "+6281907766554",
            contactWhatsapp = "6281907766554",
            facilities = listOf("WiFi Kencang", "AC", "Kamar Mandi Dalam", "Parkir Motor"),
            photoUrls = listOf("drawable:img_kosan_modern"),
            rating = 4.7,
            availableRooms = 4L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Kosan Pattimura Manise Ambon",
            propertyType = PropertyType.KOSAN.name,
            description = "Kosan aman dan bersih di jantung Kota Ambon dekat Lapangan Merdeka dan perkantoran provinsi. Air jernih 24 jam dan WiFi stabil.",
            priceIdr = 600_000L,
            billingPeriod = BillingPeriod.PER_BULAN.name,
            province = "Maluku",
            city = "Ambon",
            address = "Jl. Dr. Kayadoe No. 28, Nusaniwe, Ambon",
            latitude = -3.7024,
            longitude = 128.1745,
            contactName = "Bung Frans Ambon",
            contactPhone = "+6281343009988",
            contactWhatsapp = "6281343009988",
            facilities = listOf("WiFi Kencang", "Kasur & Lemari", "Keamanan 24 Jam", "Parkir Motor"),
            photoUrls = listOf("drawable:img_kosan_modern"),
            rating = 4.6,
            availableRooms = 3L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Kosan Ekonomis Sudirman Pekanbaru",
            propertyType = PropertyType.KOSAN.name,
            description = "Kosan paling ekonomis di Pekanbaru dengan fasilitas lengkap dan akses mudah ke Jl. Jend. Sudirman maupun kampus UNRI.",
            priceIdr = 490_000L,
            billingPeriod = BillingPeriod.PER_BULAN.name,
            province = "Riau",
            city = "Pekanbaru",
            address = "Jl. Paus Gang Sejahtera No. 11, Marpoyan Damai, Pekanbaru",
            latitude = 0.4912,
            longitude = 101.4384,
            contactName = "Bang Andre Riau",
            contactPhone = "+6281275112299",
            contactWhatsapp = "6281275112299",
            facilities = listOf("WiFi Kencang", "Listrik Termasuk", "Parkir Motor", "Kasur & Lemari"),
            photoUrls = listOf("drawable:img_kosan_modern"),
            rating = 4.5,
            availableRooms = 5L
        ),
        PropertyListing(
            ownerId = ownerId,
            ownerDisplayName = ownerName,
            title = "Kosan Kedaton Hemat Bandar Lampung",
            propertyType = PropertyType.KOSAN.name,
            description = "Kosan ramah budget dekat Universitas Lampung dan Mall Boemi Kedaton. Lingkungan bersih, nyaman untuk mahasiswa maupun pekerja muda.",
            priceIdr = 350_000L,
            billingPeriod = BillingPeriod.PER_BULAN.name,
            province = "Lampung",
            city = "Bandar Lampung",
            address = "Jl. Zainal Abidin Pagar Alam Gg. Cempaka No. 4, Bandar Lampung",
            latitude = -5.3745,
            longitude = 105.2498,
            contactName = "Mbak Sari Lampung",
            contactPhone = "+6281272334455",
            contactWhatsapp = "6281272334455",
            facilities = listOf("WiFi Kencang", "Kasur & Lemari", "Parkir Motor"),
            photoUrls = listOf("drawable:img_kosan_modern"),
            rating = 4.5,
            availableRooms = 6L
        )
    )
}
