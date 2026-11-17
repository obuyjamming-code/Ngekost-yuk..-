package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.auth.signOutUser
import com.example.data.model.BoostTier
import com.example.data.model.FeeCalculator
import com.example.data.model.MonetizationOrder
import com.example.data.model.OwnerConfig
import com.example.data.model.PayoutRequest
import com.example.data.model.PropertyListing
import com.example.data.model.ReferralCommission
import com.example.data.model.UserProfile
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryCoral
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TertiaryAmber
import com.example.ui.util.PropertyPhoto
import com.example.ui.util.copyToClipboard
import com.example.ui.util.formatRupiah
import kotlin.math.roundToLong

@Composable
fun PromoterAndAccountScreen(
    userProfile: UserProfile?,
    currentUserName: String,
    currentUserEmail: String,
    isAppOwner: Boolean,
    ownerConfig: OwnerConfig,
    myListings: List<PropertyListing>,
    myOrders: List<MonetizationOrder>,
    myCommissions: List<ReferralCommission>,
    myPayouts: List<PayoutRequest>,
    pendingOwnerOrdersCount: Int,
    pendingOwnerPayoutsCount: Int,
    onApplyReferralCode: (String, (Result<String>) -> Unit) -> Unit,
    onRequestPayout: (Long, String, String, String, (Result<String>) -> Unit) -> Unit,
    onOpenListingDetail: (PropertyListing) -> Unit,
    onNavigateToUpload: () -> Unit,
    onOpenMusicPlayer: () -> Unit,
    onOpenOwnerVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var inputRefCode by remember { mutableStateOf("") }
    var isApplyingCode by remember { mutableStateOf(false) }

    // Promoter Earnings Calculator State
    var simBoostCount by remember { mutableFloatStateOf(5f) }
    var simBookingCount by remember { mutableFloatStateOf(4f) }

    // Payout Request State
    var payoutMethod by remember { mutableStateOf("DANA") }
    var accountNumber by remember { mutableStateOf("") }
    var accountHolder by remember(currentUserName) { mutableStateOf(currentUserName) }
    var payoutAmountText by remember { mutableStateOf("") }
    var isRequestingPayout by remember { mutableStateOf(false) }

    var showSignOutDialog by remember { mutableStateOf(false) }

    val referralCode = userProfile?.referralCode?.takeIf { it.isNotBlank() } ?: "NKUSER"
    val totalEarned = myCommissions.sumOf { it.commissionIdr }
    val totalPaid = myPayouts.filter { it.status == "PAID" }.sumOf { it.amountIdr }
    val totalRequested = myPayouts.filter { it.status == "REQUESTED" }.sumOf { it.amountIdr }
    val availableBalance = (totalEarned - totalPaid - totalRequested).coerceAtLeast(0L)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Account Identity Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(PrimaryTeal, Color(0xFF1A9494)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentUserName.firstOrNull()?.uppercaseChar()?.toString() ?: "N",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUserName.ifBlank { "Sahabat Ngekost" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentUserEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isAppOwner) TertiaryAmber else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (isAppOwner) "Pemilik Resmi Aplikasi" else "Mitra Usaha & Promotor",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isAppOwner) Color(0xFF281A00) else PrimaryTeal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenMusicPlayer,
                        modifier = Modifier.testTag("profile_music_btn")
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = "Radio Obuy Jamming",
                            tint = PrimaryTeal
                        )
                    }
                }
            }

            // 2. Private Owner Vault Entry (STRICTLY visible only for obuyjamming@gmail.com)
            if (isAppOwner) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenOwnerVault() }
                        .testTag("btn_open_owner_vault"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0E4949))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = TertiaryAmber,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = "Brankas Pemilik",
                                    tint = Color(0xFF281A00)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Brankas Penghasilan Pribadi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Verifikasi iklan/booking ($pendingOwnerOrdersCount), pencairan ($pendingOwnerPayoutsCount), atur tarif & lagu Obuy Jamming.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // 3. Program Fee Promotor ("Iklankan & Dapat Fee")
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = SecondaryCoral)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Iklankan Aplikasi & Dapat Fee!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Ajak pemilik kos/wisma/hotel pasang iklan Sorotan atau ajak pencari kos booking pakai kode Anda. Dapatkan komisi ${ownerConfig.promoterFeePercent}% (Reguler) hingga ${ownerConfig.promoterProFeePercent}% (Mitra Pro >= 10 konversi) dari pendapatan platform!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Kode Promotor Anda",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryTeal
                                )
                                Text(
                                    text = referralCode,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryTeal
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = { copyToClipboard(context, "Kode Promotor", referralCode) },
                                    modifier = Modifier.testTag("btn_copy_referral")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Salin kode", tint = PrimaryTeal)
                                }
                                IconButton(
                                    onClick = {
                                        val shareText = "Cari kosan, wisma, hotel & kontrakan termurah hingga mewah di seluruh Indonesia? Atau mau iklankan usahamu gratis? Download Ngekost yuk..! dan masukkan kode referral saya: $referralCode"
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Bagikan Promosi Ngekost yuk..!"))
                                    },
                                    modifier = Modifier.testTag("btn_share_referral")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Bagikan promosi", tint = SecondaryCoral)
                                }
                            }
                        }
                    }

                    // Bind someone else's referral code
                    Spacer(Modifier.height(12.dp))
                    val boundCode = userProfile?.referredByCode.orEmpty()
                    if (boundCode.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SuccessEmerald.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessEmerald, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Terhubung dengan kode promotor: $boundCode",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SuccessEmerald,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = inputRefCode,
                                onValueChange = { inputRefCode = it.uppercase() },
                                label = { Text("Masukkan Kode Pengundang") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("input_apply_ref")
                            )
                            Button(
                                onClick = {
                                    isApplyingCode = true
                                    onApplyReferralCode(inputRefCode) { res ->
                                        isApplyingCode = false
                                        res.onSuccess { name ->
                                            Toast.makeText(context, "Kode diterapkan dari $name!", Toast.LENGTH_LONG).show()
                                            inputRefCode = ""
                                        }.onFailure { err ->
                                            Toast.makeText(context, err.localizedMessage ?: "Kode tidak valid.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = !isApplyingCode && inputRefCode.trim().length >= 4,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                modifier = Modifier.height(52.dp).testTag("btn_apply_ref")
                            ) {
                                Text("Pakai")
                            }
                        }
                    }
                }
            }

            // 4. Interactive Promoter Fee Calculator
            val boostCnt = simBoostCount.roundToLong()
            val bookCnt = simBookingCount.roundToLong()
            val totalConversions = boostCnt + bookCnt
            val effectiveRate = if (totalConversions >= 10L) ownerConfig.promoterProFeePercent else ownerConfig.promoterFeePercent
            val avgBookingFee = FeeCalculator.calculateBookingPlatformFee(2_000_000L, ownerConfig.bookingFeePercent)
            val simGross = (boostCnt * ownerConfig.boost30dIdr) + (bookCnt * avgBookingFee)
            val (simPromoterFee, simOwnerNet) = FeeCalculator.calculateFeeSplit(
                grossAmountIdr = simGross,
                hasPromoter = totalConversions > 0L,
                promoterRatePercent = effectiveRate
            )

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = PrimaryTeal)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Kalkulator Simulasi Fee Promotor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Iklan Sorotan 30 Hari (${formatRupiah(ownerConfig.boost30dIdr)}): $boostCnt iklan/bln",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = simBoostCount,
                        onValueChange = { simBoostCount = it },
                        valueRange = 0f..30f,
                        colors = SliderDefaults.colors(thumbColor = PrimaryTeal, activeTrackColor = PrimaryTeal)
                    )
                    Text(
                        text = "Booking Terverifikasi (est. fee ${formatRupiah(avgBookingFee)}): $bookCnt booking/bln",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = simBookingCount,
                        onValueChange = { simBookingCount = it },
                        valueRange = 0f..30f,
                        colors = SliderDefaults.colors(thumbColor = SecondaryCoral, activeTrackColor = SecondaryCoral)
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tier Komisi ($totalConversions konversi)", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = if (totalConversions >= 10L) "Mitra Pro ($effectiveRate%)" else "Reguler ($effectiveRate%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryTeal
                                )
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Transaksi Platform", style = MaterialTheme.typography.bodySmall)
                                Text(formatRupiah(simGross), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Estimasi Fee Untuk Anda", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(formatRupiah(simPromoterFee), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = SuccessEmerald)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Hak Bersih Pemilik Aplikasi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatRupiah(simOwnerNet), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // 5. Promoter Commission Wallet & Payout Request
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = TertiaryAmber)
                        Spacer(Modifier.width(8.dp))
                        Text("Dompet Komisi Promotor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WalletStatBox(label = "Siap Cair", amount = availableBalance, highlight = true, modifier = Modifier.weight(1f))
                        WalletStatBox(label = "Diproses", amount = totalRequested, highlight = false, modifier = Modifier.weight(1f))
                        WalletStatBox(label = "Total Masuk", amount = totalEarned, highlight = false, modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Ajukan Pencairan (Min. ${formatRupiah(ownerConfig.minWithdrawIdr)})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("DANA", "GoPay", "OVO", "BCA", "BRI", "Mandiri").forEach { method ->
                            FilterChip(
                                selected = payoutMethod == method,
                                onClick = { payoutMethod = method },
                                label = { Text(method) }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = payoutAmountText,
                        onValueChange = { payoutAmountText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Nominal Tarik (Rp)") },
                        placeholder = { Text(availableBalance.coerceAtLeast(ownerConfig.minWithdrawIdr).toString()) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it },
                            label = { Text("No. E-Wallet / Rek") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = accountHolder,
                            onValueChange = { accountHolder = it },
                            label = { Text("Atas Nama") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val reqAmount = payoutAmountText.toLongOrNull() ?: 0L
                            if (reqAmount < ownerConfig.minWithdrawIdr) {
                                Toast.makeText(context, "Minimal pencairan ${formatRupiah(ownerConfig.minWithdrawIdr)}.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (reqAmount > availableBalance) {
                                Toast.makeText(context, "Saldo siap cair tidak mencukupi.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isRequestingPayout = true
                            onRequestPayout(reqAmount, payoutMethod, accountNumber, accountHolder) { res ->
                                isRequestingPayout = false
                                res.onSuccess {
                                    Toast.makeText(context, "Pengajuan pencairan dikirim ke pemilik aplikasi!", Toast.LENGTH_LONG).show()
                                    payoutAmountText = ""
                                }.onFailure { err ->
                                    Toast.makeText(context, err.localizedMessage ?: "Gagal mengajukan pencairan.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isRequestingPayout && availableBalance >= ownerConfig.minWithdrawIdr,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_request_payout"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                    ) {
                        Text(if (isRequestingPayout) "Mengirim..." else "Ajukan Pencairan Fee")
                    }

                    if (myCommissions.isNotEmpty() || myPayouts.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Riwayat Komisi (${myCommissions.size}) • Pencairan (${myPayouts.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        myCommissions.take(3).forEach { c ->
                            Text(
                                text = "+ ${formatRupiah(c.commissionIdr)} (${c.feePercent}%) — ${c.orderTitle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SuccessEmerald
                            )
                        }
                        myPayouts.take(3).forEach { p ->
                            Text(
                                text = "• Tarik ${formatRupiah(p.amountIdr)} (${p.payoutMethod}) [${p.status}]",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 6. Kelola Usaha Saya ("My Uploaded Properties")
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Usaha Properti Saya (${myListings.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onNavigateToUpload) {
                            Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Tambah")
                        }
                    }
                    if (myListings.isEmpty()) {
                        Text(
                            text = "Anda belum mengunggah kosan, wisma, hotel, atau kontrakan sendiri.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        myListings.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onOpenListingDetail(item) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PropertyPhoto(
                                        photoRef = item.photoUrls.firstOrNull().orEmpty(),
                                        contentDescription = item.title,
                                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${item.city} • ${formatRupiah(item.priceIdr)}", style = MaterialTheme.typography.bodySmall, color = SecondaryCoral)
                                        if (item.resolvedBoostTier() != BoostTier.NONE) {
                                            Text(item.resolvedBoostTier().label, style = MaterialTheme.typography.labelSmall, color = PrimaryTeal, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (myOrders.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("Status Order & Booking Saya", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        myOrders.take(5).forEach { ord ->
                            Text(
                                text = "• ${ord.listingTitle} (${ord.orderType}) — ${formatRupiah(ord.grossAmountIdr)} [${ord.status}]",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 7. Sign Out
            OutlinedButton(
                onClick = { showSignOutDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_sign_out"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Keluar dari Akun Google")
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Keluar Akun?") },
            text = { Text("Anda perlu masuk kembali dengan Google untuk mengelola iklan dan komisi Anda.") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    signOutUser(credentialManager, scope)
                }) {
                    Text("Keluar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun WalletStatBox(
    label: String,
    amount: Long,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatRupiah(amount),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (highlight) PrimaryTeal else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
