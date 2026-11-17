package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.BillingPeriod
import com.example.data.model.BoostTier
import com.example.data.model.IndonesiaCities
import com.example.data.model.OwnerConfig
import com.example.data.model.PropertyListing
import com.example.data.model.PropertyType
import com.example.ui.components.InteractivePinMapBox
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryCoral
import com.example.ui.util.PropertyPhoto
import com.example.ui.util.compressImageUriToDataUri
import com.example.ui.util.formatRupiah
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UploadListingScreen(
    editingListing: PropertyListing?,
    ownerConfig: OwnerConfig,
    onCancelEdit: () -> Unit,
    onSubmitListing: (
        listing: PropertyListing,
        editingId: String?,
        boostTier: BoostTier,
        paymentNote: String,
        onDone: (Result<String>) -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember(editingListing?.id) { mutableStateOf(editingListing?.title ?: "") }
    var selectedType by remember(editingListing?.id) { mutableStateOf(editingListing?.resolvedType() ?: PropertyType.KOSAN) }
    var priceText by remember(editingListing?.id) { mutableStateOf(editingListing?.priceIdr?.toString() ?: "") }
    var selectedPeriod by remember(editingListing?.id) { mutableStateOf(editingListing?.resolvedBillingPeriod() ?: BillingPeriod.PER_BULAN) }
    var roomsText by remember(editingListing?.id) { mutableStateOf(editingListing?.availableRooms?.toString() ?: "3") }
    var selectedCity by remember(editingListing?.id) {
        mutableStateOf(
            IndonesiaCities.all.firstOrNull { it.name.equals(editingListing?.city, ignoreCase = true) }
                ?: IndonesiaCities.all.first()
        )
    }
    var cityDropdownExpanded by remember { mutableStateOf(false) }
    var address by remember(editingListing?.id) { mutableStateOf(editingListing?.address ?: "") }
    var latitude by remember(editingListing?.id) { mutableDoubleStateOf(editingListing?.latitude ?: selectedCity.latitude) }
    var longitude by remember(editingListing?.id) { mutableDoubleStateOf(editingListing?.longitude ?: selectedCity.longitude) }
    var contactName by remember(editingListing?.id) { mutableStateOf(editingListing?.contactName ?: "") }
    var contactPhone by remember(editingListing?.id) { mutableStateOf(editingListing?.contactPhone ?: "") }
    var contactWhatsapp by remember(editingListing?.id) { mutableStateOf(editingListing?.contactWhatsapp ?: "") }
    var description by remember(editingListing?.id) { mutableStateOf(editingListing?.description ?: "") }
    var selectedBoost by remember(editingListing?.id) { mutableStateOf(BoostTier.NONE) }
    var boostPaymentNote by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val selectedFacilities = remember(editingListing?.id) {
        mutableStateListOf<String>().apply {
            addAll(editingListing?.facilities ?: listOf("WiFi Kencang", "Parkir Motor"))
        }
    }
    val photoRefs = remember(editingListing?.id) {
        mutableStateListOf<String>().apply {
            if (editingListing != null && editingListing.photoUrls.isNotEmpty()) {
                addAll(editingListing.photoUrls)
            }
        }
    }

    // Zero-permission Android Photo Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val compressed = withContext(Dispatchers.IO) {
                    uris.mapNotNull { compressImageUriToDataUri(context, it) }
                }
                compressed.forEach { encoded ->
                    if (photoRefs.size < 3) photoRefs.add(encoded)
                }
                if (compressed.isEmpty()) {
                    Toast.makeText(context, "Gagal memproses foto, coba foto lain.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val client = LocationServices.getFusedLocationProviderClient(context)
                client.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        latitude = loc.latitude
                        longitude = loc.longitude
                        Toast.makeText(context, "Koordinat GPS diperbarui.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Lokasi belum tersedia, silakan ketuk peta.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: SecurityException) {
                Toast.makeText(context, "Izin lokasi diperlukan.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Izin lokasi ditolak; ketuk peta untuk memilih titik.", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (editingListing == null) "Pasang Iklan Properti" else "Edit Properti",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                    Text(
                        text = "Terbuka untuk umum — tayangkan kosan, wisma, hotel, atau kontrakan Anda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (editingListing != null) {
                    OutlinedButton(onClick = onCancelEdit) { Text("Batal Edit") }
                }
            }

            // Photos section
            Text("Foto Bangunan (Maks. 3 Foto)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                photoRefs.forEachIndexed { idx, ref ->
                    Box(modifier = Modifier.size(96.dp)) {
                        PropertyPhoto(
                            photoRef = ref,
                            contentDescription = "Foto bangunan ${idx + 1}",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(26.dp)
                        ) {
                            IconButton(onClick = { photoRefs.removeAt(idx) }, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus foto", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                if (photoRefs.size < 3) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.height(96.dp).testTag("btn_pick_property_photo"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(Modifier.height(4.dp))
                            Text("Pilih Foto", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Property Type
            Text("Jenis Usaha / Hunian", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PropertyType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.label) },
                        modifier = Modifier.testTag("upload_type_${type.name}")
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nama Properti / Kosan *") },
                placeholder = { Text("Contoh: Kosan Mawar Asri Dekat Kampus") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_property_title")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Harga (Rp) *") },
                    placeholder = { Text("750000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1.4f).testTag("input_property_price")
                )
                OutlinedTextField(
                    value = roomsText,
                    onValueChange = { roomsText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Unit Kosong") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BillingPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period.label) }
                    )
                }
            }

            // City Dropdown across Indonesia
            ExposedDropdownMenuBox(
                expanded = cityDropdownExpanded,
                onExpandedChange = { cityDropdownExpanded = !cityDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = "${selectedCity.name} (${selectedCity.province})",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kota & Provinsi di Indonesia") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = cityDropdownExpanded,
                    onDismissRequest = { cityDropdownExpanded = false }
                ) {
                    IndonesiaCities.all.forEach { city ->
                        DropdownMenuItem(
                            text = { Text("${city.name} — ${city.province}") },
                            onClick = {
                                selectedCity = city
                                latitude = city.latitude
                                longitude = city.longitude
                                cityDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Alamat Lengkap *") },
                placeholder = { Text("Jl. Merdeka No. 12, Kecamatan...") },
                modifier = Modifier.fillMaxWidth().testTag("input_property_address")
            )

            // Map Coordinate Picker + Coarse GPS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Titik Lokasi di Peta", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedButton(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            try {
                                LocationServices.getFusedLocationProviderClient(context)
                                    .lastLocation.addOnSuccessListener { loc ->
                                        if (loc != null) {
                                            latitude = loc.latitude
                                            longitude = loc.longitude
                                        }
                                    }
                            } catch (_: SecurityException) { }
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    },
                    modifier = Modifier.testTag("btn_use_gps")
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Lokasi Saya", style = MaterialTheme.typography.labelSmall)
                }
            }

            InteractivePinMapBox(
                latitude = latitude,
                longitude = longitude,
                title = title.ifBlank { selectedCity.name },
                interactivePick = true,
                onCoordinatePicked = { lat, lon ->
                    latitude = lat
                    longitude = lon
                }
            )

            // Contact Person
            Text("Contact Person Pemilik / Pengelola", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = contactName,
                onValueChange = { contactName = it },
                label = { Text("Nama Contact Person *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_contact_name")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = { Text("No. Telepon *") },
                    placeholder = { Text("08123456789") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("input_contact_phone")
                )
                OutlinedTextField(
                    value = contactWhatsapp,
                    onValueChange = { contactWhatsapp = it },
                    label = { Text("No. WhatsApp") },
                    placeholder = { Text("Samakan jika sama") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Facilities
            Text("Fasilitas Tersedia", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IndonesiaCities.commonFacilities.forEach { fac ->
                    val chosen = selectedFacilities.contains(fac)
                    FilterChip(
                        selected = chosen,
                        onClick = {
                            if (chosen) selectedFacilities.remove(fac) else selectedFacilities.add(fac)
                        },
                        label = { Text(fac, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi Bangunan & Lingkungan *") },
                placeholder = { Text("Jelaskan akses jalan, aturan kos, dekat kampus/stasiun, dll.") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("input_property_desc")
            )

            // Optional Boost Upgrade upon publishing
            if (editingListing == null) {
                Text("Opsi Penayangan Iklan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                val boostOptions = listOf(
                    BoostTier.NONE to 0L,
                    BoostTier.BOOST_7D to ownerConfig.boost7dIdr,
                    BoostTier.BOOST_30D to ownerConfig.boost30dIdr,
                    BoostTier.BOOST_90D to ownerConfig.boost90dIdr
                )
                boostOptions.forEach { (tier, price) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedBoost == tier) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        onClick = { selectedBoost = tier },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tier.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (price == 0L) "Gratis" else formatRupiah(price),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (price == 0L) PrimaryTeal else SecondaryCoral,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (selectedBoost != BoostTier.NONE) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(
                                text = "Transfer biaya sorotan ke ${ownerConfig.bankName} ${ownerConfig.bankAccountNumber} (${ownerConfig.bankAccountHolder})",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = boostPaymentNote,
                                onValueChange = { boostPaymentNote = it },
                                label = { Text("Nama Pengirim / Catatan Transfer") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            errorMsg?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val price = priceText.toLongOrNull() ?: 0L
                    val rooms = roomsText.toLongOrNull() ?: 1L
                    val defaultCover = when (selectedType) {
                        PropertyType.KOSAN -> "drawable:img_kosan_modern"
                        PropertyType.WISMA -> "drawable:img_wisma_asri"
                        PropertyType.HOTEL -> "drawable:img_hotel_luxury"
                        PropertyType.KONTRAKAN -> "drawable:img_kontrakan_nyaman"
                    }
                    val finalPhotos = if (photoRefs.isEmpty()) listOf(defaultCover) else photoRefs.toList()
                    val candidate = PropertyListing(
                        id = editingListing?.id ?: "",
                        ownerId = editingListing?.ownerId ?: "",
                        title = title.trim(),
                        propertyType = selectedType.name,
                        description = description.trim(),
                        priceIdr = price,
                        billingPeriod = selectedPeriod.name,
                        province = selectedCity.province,
                        city = selectedCity.name,
                        address = address.trim(),
                        latitude = latitude,
                        longitude = longitude,
                        contactName = contactName.trim(),
                        contactPhone = contactPhone.trim(),
                        contactWhatsapp = contactWhatsapp.trim().ifBlank { contactPhone.trim() },
                        facilities = selectedFacilities.toList(),
                        photoUrls = finalPhotos,
                        rating = editingListing?.rating ?: 4.8,
                        availableRooms = rooms.coerceIn(0L, 500L),
                        visibility = "public"
                    )

                    if (candidate.title.length < 3 || candidate.priceIdr < 50_000L ||
                        candidate.address.length < 5 || candidate.contactName.length < 2 ||
                        candidate.contactPhone.length < 8 || candidate.description.length < 10
                    ) {
                        errorMsg = "Lengkapi semua kolom wajib (*) — harga minimal Rp 50.000 & deskripsi minimal 10 huruf."
                        return@Button
                    }

                    errorMsg = null
                    isSubmitting = true
                    onSubmitListing(candidate, editingListing?.id, selectedBoost, boostPaymentNote) { res ->
                        isSubmitting = false
                        res.onSuccess {
                            Toast.makeText(
                                context,
                                if (editingListing == null) "Properti berhasil ditayangkan ke publik!" else "Perubahan properti disimpan.",
                                Toast.LENGTH_LONG
                            ).show()
                        }.onFailure { err ->
                            errorMsg = err.localizedMessage ?: "Gagal menyimpan iklan properti."
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth().height(54.dp).testTag("btn_publish_listing"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (editingListing == null) "Tayangkan Usaha Sekarang" else "Simpan Perubahan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
