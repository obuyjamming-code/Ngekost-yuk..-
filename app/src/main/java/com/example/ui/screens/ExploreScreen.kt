package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BoostTier
import com.example.data.model.IndonesiaRegion
import com.example.data.model.PriceSortOption
import com.example.data.model.PriceTierFilter
import com.example.data.model.PropertyListing
import com.example.data.model.PropertyType
import com.example.ui.components.IndonesiaPropertyMap
import com.example.ui.components.SocialHousingSearchSheet
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryCoral
import com.example.ui.theme.TertiaryAmber
import com.example.ui.util.PropertyPhoto
import com.example.ui.util.formatRupiah
import com.example.ui.viewmodel.ExploreFilterState
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    listingsState: UiState<List<PropertyListing>>,
    filteredListings: List<PropertyListing>,
    favoriteIds: Set<String>,
    filterState: ExploreFilterState,
    onSearchChange: (String) -> Unit,
    onTypeSelected: (PropertyType?) -> Unit,
    onRegionSelected: (IndonesiaRegion) -> Unit,
    onPriceTierSelected: (PriceTierFilter) -> Unit,
    onSortSelected: (PriceSortOption) -> Unit,
    onToggleMapView: (Boolean) -> Unit,
    onOpenMusicPlayer: () -> Unit,
    onListingClick: (PropertyListing) -> Unit,
    onToggleFavorite: (PropertyListing) -> Unit,
    modifier: Modifier = Modifier
) {
    var showGlobalRoadmapSheet by remember { mutableStateOf(false) }
    var showSocialSearchSheet by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ngekost yuk..!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable { showGlobalRoadmapSheet = true }
                                .testTag("global_roadmap_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Public,
                                    contentDescription = "Cakupan wilayah",
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Indonesia • Next: Seluruh Dunia",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Social TikTok/IG + Obuy Jamming Music Triggers
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SecondaryCoral.copy(alpha = 0.16f),
                            modifier = Modifier
                                .clickable { showSocialSearchSheet = true }
                                .testTag("open_social_ref_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.VideoLibrary,
                                    contentDescription = "Cari di TikTok dan Instagram",
                                    tint = SecondaryCoral,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "TikTok/IG",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryCoral
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = TertiaryAmber.copy(alpha = 0.22f),
                            modifier = Modifier
                                .clickable { onOpenMusicPlayer() }
                                .testTag("open_obuy_music_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = "Putar lagu Obuy Jamming",
                                    tint = Color(0xFF8A5600),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Musik",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5E3B00)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Search Field
                OutlinedTextField(
                    value = filterState.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Cari kota, kosan, wisma, hotel, kontrakan...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (filterState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus pencarian")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("search_property_input")
                )

                Spacer(Modifier.height(8.dp))

                // Property Type Chips
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterState.selectedType == null,
                        onClick = { onTypeSelected(null) },
                        label = { Text("Semua") },
                        modifier = Modifier.testTag("chip_type_all")
                    )
                    PropertyType.entries.forEach { type ->
                        FilterChip(
                            selected = filterState.selectedType == type,
                            onClick = {
                                onTypeSelected(if (filterState.selectedType == type) null else type)
                            },
                            label = { Text(type.label) },
                            modifier = Modifier.testTag("chip_type_${type.name}")
                        )
                    }
                }

                // Sort, Price Tier & Region Chips
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PriceSortOption.entries.forEach { sortOpt ->
                        FilterChip(
                            selected = filterState.sortOption == sortOpt,
                            onClick = { onSortSelected(sortOpt) },
                            label = { Text(sortOpt.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SecondaryCoral.copy(alpha = 0.16f),
                                selectedLabelColor = SecondaryCoral
                            ),
                            modifier = Modifier.testTag("chip_sort_${sortOpt.name}")
                        )
                    }
                    PriceTierFilter.entries.filter { it != PriceTierFilter.ALL }.forEach { tier ->
                        FilterChip(
                            selected = filterState.selectedPriceTier == tier,
                            onClick = {
                                onPriceTierSelected(
                                    if (filterState.selectedPriceTier == tier) PriceTierFilter.ALL else tier
                                )
                            },
                            label = { Text(tier.label) },
                            modifier = Modifier.testTag("chip_price_${tier.name}")
                        )
                    }
                    IndonesiaRegion.entries.filter { it != IndonesiaRegion.ALL }.forEach { region ->
                        FilterChip(
                            selected = filterState.selectedRegion == region,
                            onClick = {
                                onRegionSelected(
                                    if (filterState.selectedRegion == region) IndonesiaRegion.ALL else region
                                )
                            },
                            label = { Text(region.label) }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // List vs Map Segmented Switch + count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${filteredListings.size} hunian ditemukan",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = !filterState.isMapView,
                            onClick = { onToggleMapView(false) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("toggle_list_view")
                        ) {
                            Text("Daftar")
                        }
                        SegmentedButton(
                            selected = filterState.isMapView,
                            onClick = { onToggleMapView(true) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = { Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("toggle_map_view")
                        ) {
                            Text("Peta")
                        }
                    }
                }
            }
        }

        // Main Body: Loading / Error / Map / Grid List
        when (listingsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryTeal)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = listingsState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is UiState.Success -> {
                if (filterState.isMapView) {
                    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        IndonesiaPropertyMap(
                            listings = filteredListings,
                            onListingSelected = onListingClick
                        )
                    }
                } else if (filteredListings.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Belum ada hunian yang cocok di katalog ini",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Ubah filter, atau langsung cari video & info hunian kota tersebut via TikTok / Instagram.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SecondaryCoral,
                                modifier = Modifier
                                    .clickable { showSocialSearchSheet = true }
                                    .testTag("empty_social_search_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (filterState.searchQuery.isNotBlank()) {
                                            "Cari \"${filterState.searchQuery}\" di TikTok & IG"
                                        } else {
                                            "Cari Referensi Kota di TikTok & IG"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columns = if (maxWidth >= 600.dp) 2 else 1
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(filteredListings, key = { it.id.ifBlank { it.title } }) { listing ->
                                PropertyListingCard(
                                    listing = listing,
                                    isFavorite = favoriteIds.contains(listing.id),
                                    onClick = { onListingClick(listing) },
                                    onToggleFavorite = { onToggleFavorite(listing) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGlobalRoadmapSheet) {
        ModalBottomSheet(onDismissRequest = { showGlobalRoadmapSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Peta Cakupan: Indonesia -> Dunia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Saat ini Ngekost yuk..! aktif melayani pencarian kosan, wisma, hotel, dan kontrakan di 38 provinsi seluruh Indonesia (Jawa, Bali, Sumatera, Kalimantan, Sulawesi, Nusa Tenggara, Maluku & Papua).",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Rencana Tahap Berikutnya (Global Ready): Struktur data properti sudah menyimpan kode negara (countryCode), nama negara (countryName), dan koordinat GPS global sehingga siap dikembangkan untuk mencari hunian di seluruh dunia.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

    if (showSocialSearchSheet) {
        SocialHousingSearchSheet(
            initialCity = filterState.searchQuery.trim().ifBlank { "Jakarta Selatan" },
            initialType = filterState.selectedType,
            onDismiss = { showSocialSearchSheet = false }
        )
    }
}

@Composable
fun PropertyListingCard(
    listing: PropertyListing,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBoosted = listing.resolvedBoostTier() != BoostTier.NONE
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("property_card_${listing.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isBoosted) 5.dp else 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
            ) {
                PropertyPhoto(
                    photoRef = listing.photoUrls.firstOrNull().orEmpty(),
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxSize()
                )

                // Top badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryTeal
                        ) {
                            Text(
                                text = listing.resolvedType().label,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (isBoosted) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = TertiaryAmber
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF281A00),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = "Sorotan",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF281A00),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.testTag("fav_btn_${listing.id}")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isFavorite) "Hapus dari favorit" else "Simpan ke favorit",
                                tint = if (isFavorite) TertiaryAmber else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Bottom-left price tag pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SecondaryCoral,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "${formatRupiah(listing.priceIdr)} ${listing.resolvedBillingPeriod().shortLabel}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = TertiaryAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", listing.rating),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${listing.city}, ${listing.province}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (listing.facilities.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = listing.facilities.take(3).joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryTeal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
