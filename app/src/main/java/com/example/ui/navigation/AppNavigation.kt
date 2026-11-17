package com.example.ui.navigation

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.auth.authStateFlow
import com.example.data.model.PropertyListing
import com.example.data.repository.PropertyRepository
import com.example.ui.components.ObuyJammingMiniPlayerBar
import com.example.ui.components.ObuyJammingPlaylistSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.OwnerRevenueVaultScreen
import com.example.ui.screens.PromoterAndAccountScreen
import com.example.ui.screens.PropertyDetailScreen
import com.example.ui.screens.UploadListingScreen
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

private object AppRoutes {
    const val EXPLORE = "explore"
    const val FAVORITES = "favorites"
    const val UPLOAD = "upload"
    const val ACCOUNT = "account"
    const val DETAIL = "detail/{listingId}"
    const val OWNER_VAULT = "owner_vault"

    fun detailRoute(listingId: String): String = "detail/$listingId"
}

private enum class TopLevelTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    EXPLORE(AppRoutes.EXPLORE, "Jelajah", Icons.Default.Explore),
    FAVORITES(AppRoutes.FAVORITES, "Favorit", Icons.Default.Bookmark),
    UPLOAD(AppRoutes.UPLOAD, "Pasang Iklan", Icons.Default.AddCircle),
    ACCOUNT(AppRoutes.ACCOUNT, "Promotor & Akun", Icons.Default.AccountCircle)
}

@Composable
fun AppNavigation() {
    val authFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    if (currentUser == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(currentUser!!.uid) {
            AuthenticatedAppShell(user = currentUser!!)
        }
    }
}

@Composable
private fun AuthenticatedAppShell(user: FirebaseUser) {
    val appContext = LocalContext.current.applicationContext as Application
    val viewModel: MainViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                MainViewModel(
                    repository = PropertyRepository(appContext),
                    currentUserId = user.uid,
                    currentUserEmail = user.email ?: "",
                    currentUserName = user.displayName ?: "Sahabat Ngekost",
                    currentFirebaseUser = user
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val listingsState by viewModel.listingsUiState.collectAsStateWithLifecycle()
    val filteredListings by viewModel.filteredListings.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val myListings by viewModel.myListings.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val ownerConfig by viewModel.ownerConfig.collectAsStateWithLifecycle()
    val myOrders by viewModel.myOrders.collectAsStateWithLifecycle()
    val myCommissions by viewModel.myCommissions.collectAsStateWithLifecycle()
    val myPayouts by viewModel.myPayouts.collectAsStateWithLifecycle()
    val allOrdersOwner by viewModel.allOrdersOwner.collectAsStateWithLifecycle()
    val allCommissionsOwner by viewModel.allCommissionsOwner.collectAsStateWithLifecycle()
    val allPayoutsOwner by viewModel.allPayoutsOwner.collectAsStateWithLifecycle()
    val musicTracks by viewModel.musicTracks.collectAsStateWithLifecycle()

    val isMusicBarVisible by viewModel.isMusicBarVisible.collectAsStateWithLifecycle()
    val isMusicSheetOpen by viewModel.isMusicSheetOpen.collectAsStateWithLifecycle()
    val currentTrackIndex by viewModel.currentTrackIndex.collectAsStateWithLifecycle()
    val selectedPlatform by viewModel.selectedMusicPlatform.collectAsStateWithLifecycle()
    val isWebExpanded by viewModel.isWebPlayerExpanded.collectAsStateWithLifecycle()
    val ownerPinUnlocked by viewModel.ownerPinUnlocked.collectAsStateWithLifecycle()

    var editingListing by remember { mutableStateOf<PropertyListing?>(null) }

    val topLevelRoutes = remember { TopLevelTab.entries.map { it.route }.toSet() }
    val showMainNav = currentRoute in topLevelRoutes

    val navigateToTopTab: (String) -> Unit = { route ->
        if (route != AppRoutes.UPLOAD) {
            editingListing = null
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isMusicBarVisible && currentRoute != AppRoutes.OWNER_VAULT) {
                        ObuyJammingMiniPlayerBar(
                            tracks = musicTracks,
                            currentIndex = currentTrackIndex,
                            selectedPlatform = selectedPlatform,
                            isWebExpanded = isWebExpanded,
                            onToggleWebExpanded = { viewModel.toggleWebPlayerExpanded() },
                            onOpenSheet = { viewModel.toggleMusicSheet(true) },
                            onPrevTrack = { viewModel.prevTrack() },
                            onNextTrack = { viewModel.nextTrack() },
                            onCloseBar = { viewModel.hideMusicBar() }
                        )
                    }
                    if (showMainNav && !useNavRail) {
                        NavigationBar {
                            TopLevelTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentRoute == tab.route,
                                    onClick = { navigateToTopTab(tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_${tab.route}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showMainNav && useNavRail) {
                    NavigationRail {
                        TopLevelTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = currentRoute == tab.route,
                                onClick = { navigateToTopTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_${tab.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.EXPLORE,
                    modifier = Modifier.weight(1f).fillMaxSize()
                ) {
                    composable(AppRoutes.EXPLORE) {
                        ExploreScreen(
                            listingsState = listingsState,
                            filteredListings = filteredListings,
                            favoriteIds = favoriteIds,
                            filterState = filterState,
                            onSearchChange = viewModel::updateSearchQuery,
                            onTypeSelected = viewModel::selectPropertyType,
                            onRegionSelected = viewModel::selectRegion,
                            onPriceTierSelected = viewModel::selectPriceTier,
                            onSortSelected = viewModel::selectSortOption,
                            onToggleMapView = viewModel::setMapView,
                            onOpenMusicPlayer = { viewModel.toggleMusicSheet(true) },
                            onListingClick = { listing ->
                                navController.navigate(AppRoutes.detailRoute(listing.id))
                            },
                            onToggleFavorite = viewModel::toggleFavorite
                        )
                    }

                    composable(AppRoutes.FAVORITES) {
                        FavoritesScreen(
                            listingsState = listingsState,
                            favorites = favorites,
                            favoriteIds = favoriteIds,
                            onListingClick = { listing ->
                                navController.navigate(AppRoutes.detailRoute(listing.id))
                            },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onExploreClick = { navigateToTopTab(AppRoutes.EXPLORE) }
                        )
                    }

                    composable(AppRoutes.UPLOAD) {
                        UploadListingScreen(
                            editingListing = editingListing,
                            ownerConfig = ownerConfig,
                            onCancelEdit = {
                                editingListing = null
                                navController.popBackStack()
                            },
                            onSubmitListing = { candidate, editId, boostTier, note, onDone ->
                                viewModel.submitListing(candidate, editId, boostTier, note) { res ->
                                    onDone(res)
                                    if (res.isSuccess) {
                                        editingListing = null
                                        navigateToTopTab(AppRoutes.EXPLORE)
                                    }
                                }
                            }
                        )
                    }

                    composable(AppRoutes.ACCOUNT) {
                        PromoterAndAccountScreen(
                            userProfile = userProfile,
                            currentUserName = user.displayName ?: "Sahabat Ngekost",
                            currentUserEmail = user.email ?: "",
                            isAppOwner = viewModel.isAppOwner,
                            ownerConfig = ownerConfig,
                            myListings = myListings,
                            myOrders = myOrders,
                            myCommissions = myCommissions,
                            myPayouts = myPayouts,
                            pendingOwnerOrdersCount = allOrdersOwner.count { it.status == "PENDING" },
                            pendingOwnerPayoutsCount = allPayoutsOwner.count { it.status == "REQUESTED" },
                            onApplyReferralCode = viewModel::applyReferralCode,
                            onRequestPayout = viewModel::requestPayout,
                            onOpenListingDetail = { listing ->
                                navController.navigate(AppRoutes.detailRoute(listing.id))
                            },
                            onNavigateToUpload = {
                                editingListing = null
                                navigateToTopTab(AppRoutes.UPLOAD)
                            },
                            onOpenMusicPlayer = { viewModel.toggleMusicSheet(true) },
                            onOpenOwnerVault = {
                                if (viewModel.isAppOwner) {
                                    navController.navigate(AppRoutes.OWNER_VAULT)
                                }
                            }
                        )
                    }

                    composable(
                        route = AppRoutes.DETAIL,
                        arguments = listOf(navArgument("listingId") { type = NavType.StringType })
                    ) { entry ->
                        val listingId = entry.arguments?.getString("listingId").orEmpty()
                        val publicList = (listingsState as? UiState.Success)?.data ?: emptyList()
                        val listing = publicList.firstOrNull { it.id == listingId }
                            ?: myListings.firstOrNull { it.id == listingId }

                        if (listing == null) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Properti tidak ditemukan.")
                            }
                        } else {
                            PropertyDetailScreen(
                                listing = listing,
                                isFavorite = favoriteIds.contains(listing.id),
                                isOwnerOfListing = listing.ownerId == viewModel.currentUserId,
                                ownerConfig = ownerConfig,
                                defaultReferralCode = userProfile?.referredByCode.orEmpty(),
                                onBack = { navController.popBackStack() },
                                onToggleFavorite = { viewModel.toggleFavorite(listing) },
                                onEditListing = {
                                    editingListing = listing
                                    navigateToTopTab(AppRoutes.UPLOAD)
                                },
                                onDeleteListing = {
                                    viewModel.deleteListing(listing.id) { res ->
                                        if (res.isSuccess) navController.popBackStack()
                                    }
                                },
                                onSubmitBookingOrder = { promo, note, done ->
                                    viewModel.submitBookingOrder(listing, promo, note, done)
                                },
                                onSubmitBoostOrder = { tier, note, done ->
                                    viewModel.submitBoostOrderForExisting(listing, tier, note, done)
                                }
                            )
                        }
                    }

                    composable(AppRoutes.OWNER_VAULT) {
                        OwnerRevenueVaultScreen(
                            isAppOwner = viewModel.isAppOwner,
                            pinUnlocked = ownerPinUnlocked,
                            ownerConfig = ownerConfig,
                            allOrders = allOrdersOwner,
                            allCommissions = allCommissionsOwner,
                            allPayouts = allPayoutsOwner,
                            musicTracks = musicTracks,
                            onUnlockPin = viewModel::unlockOwnerPin,
                            onLockPin = viewModel::lockOwnerPin,
                            onUpdatePin = viewModel::updateOwnerPin,
                            onVerifyOrder = viewModel::verifyOrderByOwner,
                            onProcessPayout = viewModel::processPayoutByOwner,
                            onUpdateOwnerConfig = viewModel::updateOwnerConfig,
                            onAddMusicTrack = viewModel::addMusicTrackByOwner,
                            onDeleteMusicTrack = viewModel::deleteMusicTrackByOwner,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    if (isMusicSheetOpen) {
        ObuyJammingPlaylistSheet(
            tracks = musicTracks,
            currentIndex = currentTrackIndex,
            selectedPlatform = selectedPlatform,
            onSelectPlatform = viewModel::selectMusicPlatform,
            onSelectTrack = viewModel::selectTrack,
            onPlayInApp = {
                viewModel.openMusicBarAndPlay(currentTrackIndex)
                if (!isWebExpanded) {
                    viewModel.toggleWebPlayerExpanded()
                }
            },
            onDismiss = { viewModel.toggleMusicSheet(false) }
        )
    }
}
