package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BoostTier
import com.example.data.model.FeeCalculator
import com.example.data.model.OwnerConfig
import com.example.data.model.PropertyListing
import com.example.ui.components.InteractivePinMapBox
import com.example.ui.components.SocialHousingSearchSheet
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryCoral
import com.example.ui.theme.TertiaryAmber
import com.example.ui.util.PropertyPhoto
import com.example.ui.util.buildInstagramHousingTagUrl
import com.example.ui.util.buildTikTokHousingSearchUrl
import com.example.ui.util.copyToClipboard
import com.example.ui.util.formatRupiah
import com.example.ui.util.openExternalUrl
import com.example.ui.util.openGoogleMaps
import com.example.ui.util.openPhoneDialer
import com.example.ui.util.openWhatsApp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PropertyDetailScreen(
    listing: PropertyListing,
    isFavorite: Boolean,
    isOwnerOfListing: Boolean,
    ownerConfig: OwnerConfig,
    defaultReferralCode: String,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditListing: () -> Unit,
    onDeleteListing: () -> Unit,
    onSubmitBookingOrder: (promoCode: String, paymentNote: String, onDone: (Result<String>) -> Unit) -> Unit,
    onSubmitBoostOrder: (boostTier: BoostTier, paymentNote: String, onDone: (Result<String>) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val photos = listing.photoUrls.ifEmpty { listOf("drawable:img_kosan_modern") }
    val pagerState = rememberPagerState(pageCount = { photos.size })

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBookingSheet by remember { mutableStateOf(false) }
    var showBoostSheet by remember { mutableStateOf(false) }
    var showSocialSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(listing.resolvedType().label, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.testTag("detail_fav_btn")) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Simpan favorit",
                            tint = if (isFavorite) TertiaryAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (isOwnerOfListing) {
                        IconButton(onClick = onEditListing, modifier = Modifier.testTag("detail_edit_btn")) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit properti")
                        }
                        IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.testTag("detail_delete_btn")) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus properti")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Photo Gallery Pager
                Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        PropertyPhoto(
                            photoRef = photos[page],
                            contentDescription = "${listing.title} foto ${page + 1}",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${photos.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    // Badges & Rating
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(8.dp), color = PrimaryTeal) {
                            Text(
                                text = listing.resolvedType().label,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (listing.resolvedBoostTier() != BoostTier.NONE) {
                            Surface(shape = RoundedCornerShape(8.dp), color = TertiaryAmber) {
                                Text(
                                    text = listing.resolvedBoostTier().label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF281A00),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = TertiaryAmber, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "${String.format("%.1f", listing.rating)} • ${listing.availableRooms} unit tersedia",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${formatRupiah(listing.priceIdr)} ${listing.resolvedBillingPeriod().label}",
                        style = MaterialTheme.typography.titleLarge,
                        color = SecondaryCoral,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${listing.address}, ${listing.city}, ${listing.province}, ${listing.countryName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Owner Boost Upsell button
                    if (isOwnerOfListing && listing.resolvedBoostTier() == BoostTier.NONE) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showBoostSheet = true },
                            modifier = Modifier.fillMaxWidth().testTag("btn_upgrade_boost"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TertiaryAmber)
                            Spacer(Modifier.width(8.dp))
                            Text("Naikkan Iklan ke Posisi Sorotan")
                        }
                    }

                    // Facilities
                    if (listing.facilities.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text("Fasilitas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listing.facilities.forEach { facility ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = facility,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Deskripsi Hunian", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = listing.description,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    // Interactive Map + Open in Google Maps
                    Spacer(Modifier.height(18.dp))
                    Text("Lokasi & Peta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    InteractivePinMapBox(
                        latitude = listing.latitude,
                        longitude = listing.longitude,
                        title = listing.title,
                        interactivePick = false
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            openGoogleMaps(context, listing.latitude, listing.longitude, listing.title)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_open_google_maps"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Buka di Google Maps")
                    }

                    // TikTok & Instagram City Video Reference Card
                    Spacer(Modifier.height(14.dp))
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = PrimaryTeal)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Referensi Video & Review di ${listing.city}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Bandingkan room tour ${listing.resolvedType().label.lowercase()} dan suasana area ${listing.city} langsung di TikTok atau Instagram.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val url = buildTikTokHousingSearchUrl(
                                            city = listing.city,
                                            typeLabel = listing.resolvedType().label.lowercase(),
                                            extraKeyword = "review"
                                        )
                                        openExternalUrl(context, url, "TikTok")
                                    },
                                    modifier = Modifier.weight(1f).testTag("btn_detail_tiktok"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("TikTok")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val url = buildInstagramHousingTagUrl(
                                            city = listing.city,
                                            typeLabel = listing.resolvedType().label.lowercase()
                                        )
                                        openExternalUrl(context, url, "Instagram")
                                    },
                                    modifier = Modifier.weight(1f).testTag("btn_detail_instagram"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Instagram")
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            TextButton(
                                onClick = { showSocialSheet = true },
                                modifier = Modifier.align(Alignment.End).testTag("btn_detail_social_in_app")
                            ) {
                                Text("Buka Pencarian TikTok / IG di Aplikasi ->")
                            }
                        }
                    }

                    // Contact Person Card
                    Spacer(Modifier.height(18.dp))
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Text("Contact Person / Pemilik", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = PrimaryTeal.copy(alpha = 0.15f), modifier = Modifier.size(42.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal)
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(listing.contactName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(listing.contactPhone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { copyToClipboard(context, "Nomor Pemilik", listing.contactPhone) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Salin nomor")
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { openWhatsApp(context, listing.contactWhatsapp, listing.title) },
                                    modifier = Modifier.weight(1f).testTag("btn_chat_whatsapp"),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("WhatsApp")
                                }
                                OutlinedButton(
                                    onClick = { openPhoneDialer(context, listing.contactPhone) },
                                    modifier = Modifier.weight(1f).testTag("btn_call_owner"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Telepon")
                                }
                            }
                        }
                    }

                    // Verified Booking CTA
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { showBookingSheet = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("btn_booking_verified"),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryCoral),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Ajukan Booking Terverifikasi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Properti?") },
            text = { Text("Iklan \"${listing.title}\" akan dihapus permanen dari daftar pencarian.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteListing()
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showBookingSheet) {
        BookingOrderBottomSheet(
            listing = listing,
            ownerConfig = ownerConfig,
            defaultPromoCode = defaultReferralCode,
            onDismiss = { showBookingSheet = false },
            onSubmit = { promo, note ->
                onSubmitBookingOrder(promo, note) { res ->
                    res.onSuccess {
                        Toast.makeText(context, "Pengajuan booking terkirim! Menunggu verifikasi.", Toast.LENGTH_LONG).show()
                        showBookingSheet = false
                    }.onFailure { err ->
                        Toast.makeText(context, err.localizedMessage ?: "Gagal mengirim booking.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showBoostSheet) {
        BoostUpgradeBottomSheet(
            listing = listing,
            ownerConfig = ownerConfig,
            onDismiss = { showBoostSheet = false },
            onSubmit = { tier, note ->
                onSubmitBoostOrder(tier, note) { res ->
                    res.onSuccess {
                        Toast.makeText(context, "Order Sorotan dikirim! Menunggu konfirmasi admin.", Toast.LENGTH_LONG).show()
                        showBoostSheet = false
                    }.onFailure { err ->
                        Toast.makeText(context, err.localizedMessage ?: "Gagal mengirim order.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showSocialSheet) {
        SocialHousingSearchSheet(
            initialCity = listing.city,
            initialType = listing.resolvedType(),
            onDismiss = { showSocialSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingOrderBottomSheet(
    listing: PropertyListing,
    ownerConfig: OwnerConfig,
    defaultPromoCode: String,
    onDismiss: () -> Unit,
    onSubmit: (promoCode: String, paymentNote: String) -> Unit
) {
    var promoCode by remember { mutableStateOf(defaultPromoCode) }
    var paymentNote by remember { mutableStateOf("") }
    val platformFee = FeeCalculator.calculateBookingPlatformFee(listing.priceIdr, ownerConfig.bookingFeePercent)
    val (promoShare, ownerNet) = FeeCalculator.calculateFeeSplit(
        grossAmountIdr = platformFee,
        hasPromoter = promoCode.isNotBlank(),
        promoterRatePercent = ownerConfig.promoterFeePercent
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Booking Terverifikasi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Biaya layanan platform ${ownerConfig.bookingFeePercent}% (min Rp 25rb, maks Rp 350rb): ${formatRupiah(platformFee)}",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryCoral,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Transfer ke Rekening Pemilik Aplikasi:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("${ownerConfig.bankName} • ${ownerConfig.bankAccountNumber}", style = MaterialTheme.typography.bodyMedium)
                    Text("a.n. ${ownerConfig.bankAccountHolder}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = promoCode,
                onValueChange = { promoCode = it.uppercase() },
                label = { Text("Kode Referral Promotor (Opsional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (promoCode.isNotBlank()) {
                Text(
                    text = "Simulasi pembagian fee: Promotor ${ownerConfig.promoterFeePercent}% (${formatRupiah(promoShare)}) • Pemilik (${formatRupiah(ownerNet)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryTeal,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = paymentNote,
                onValueChange = { paymentNote = it },
                label = { Text("Catatan Transfer / Tanggal Masuk") },
                placeholder = { Text("Contoh: Masuk tgl 1, bukti transfer BCA...") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { onSubmit(promoCode, paymentNote) },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_confirm_booking_order"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Text("Kirim Konfirmasi Booking (${formatRupiah(platformFee)})")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoostUpgradeBottomSheet(
    listing: PropertyListing,
    ownerConfig: OwnerConfig,
    onDismiss: () -> Unit,
    onSubmit: (BoostTier, String) -> Unit
) {
    var selectedTier by remember { mutableStateOf(BoostTier.BOOST_7D) }
    var note by remember { mutableStateOf("") }
    val tiers = listOf(
        BoostTier.BOOST_7D to ownerConfig.boost7dIdr,
        BoostTier.BOOST_30D to ownerConfig.boost30dIdr,
        BoostTier.BOOST_90D to ownerConfig.boost90dIdr
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Sorotan Iklan: ${listing.title}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            tiers.forEach { (tier, price) ->
                val isSelected = selectedTier == tier
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    onClick = { selectedTier = tier }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tier.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(formatRupiah(price), style = MaterialTheme.typography.titleSmall, color = SecondaryCoral, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Transfer ke ${ownerConfig.bankName} ${ownerConfig.bankAccountNumber} a.n. ${ownerConfig.bankAccountHolder}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan / Nama Pengirim Transfer") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onSubmit(selectedTier, note) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Text("Ajukan Paket Sorotan")
            }
        }
    }
}
