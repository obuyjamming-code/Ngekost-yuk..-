package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.FeeCalculator
import com.example.data.model.MonetizationOrder
import com.example.data.model.MusicTrack
import com.example.data.model.OwnerConfig
import com.example.data.model.PayoutRequest
import com.example.data.model.ReferralCommission
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryCoral
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TertiaryAmber
import com.example.ui.util.formatRupiah
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerRevenueVaultScreen(
    isAppOwner: Boolean,
    pinUnlocked: Boolean,
    ownerConfig: OwnerConfig,
    allOrders: List<MonetizationOrder>,
    allCommissions: List<ReferralCommission>,
    allPayouts: List<PayoutRequest>,
    musicTracks: List<MusicTrack>,
    onUnlockPin: (String) -> Boolean,
    onLockPin: () -> Unit,
    onUpdatePin: (String) -> Unit,
    onVerifyOrder: (MonetizationOrder, Boolean, (Result<Unit>) -> Unit) -> Unit,
    onProcessPayout: (PayoutRequest, Boolean, String, (Result<Unit>) -> Unit) -> Unit,
    onUpdateOwnerConfig: (OwnerConfig, (Result<Unit>) -> Unit) -> Unit,
    onAddMusicTrack: (MusicTrack, (Result<String>) -> Unit) -> Unit,
    onDeleteMusicTrack: (String, (Result<Unit>) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Brankas Pribadi Pemilik") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("vault_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (isAppOwner && pinUnlocked) {
                        IconButton(onClick = onLockPin, modifier = Modifier.testTag("vault_lock_btn")) {
                            Icon(Icons.Default.Lock, contentDescription = "Kunci Brankas")
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
            if (!isAppOwner) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Akses ditolak. Dasbor ini khusus untuk akun pemilik aplikasi terverifikasi.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (!pinUnlocked) {
                // PIN Lock Gate
                Column(
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(34.dp))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Kunci Keamanan Pemilik", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Masukkan PIN brankas untuk melihat laporan omzet pribadi, menyetujui iklan/booking, dan mengatur fee promotor. (PIN Default: 2026)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it.filter { ch -> ch.isDigit() }.take(8) },
                        label = { Text("PIN Brankas") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("owner_pin_input")
                    )
                    pinError?.let { err ->
                        Spacer(Modifier.height(6.dp))
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            if (onUnlockPin(pinInput)) {
                                pinError = null
                                pinInput = ""
                            } else {
                                pinError = "PIN salah. Coba lagi (default: 2026)."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("owner_pin_unlock_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                    ) {
                        Text("Buka Brankas")
                    }
                }
            } else {
                // Unlocked Owner Revenue Vault
                val verifiedOrders = allOrders.filter { it.status == "VERIFIED" }
                val totalGross = verifiedOrders.sumOf { it.grossAmountIdr }
                val totalPromoterFee = verifiedOrders.sumOf { it.promoterFeeIdr }
                val totalOwnerNet = verifiedOrders.sumOf { it.ownerNetIdr }
                val paidCommissionsTotal = allCommissions.filter { it.status == "PAID" }.sumOf { it.commissionIdr }

                Column(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize()
                ) {
                    // Private Financial Summary Header
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E4949)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(
                                text = "Laba Bersih Pribadi Anda",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = formatRupiah(totalOwnerNet),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TertiaryAmber
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Omzet Kotor", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                    Text(formatRupiah(totalGross), style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Hak Promotor", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                    Text(formatRupiah(totalPromoterFee), style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Fee Dicairkan", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                    Text(formatRupiah(paidCommissionsTotal), style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    val tabs = listOf("Verifikasi Order", "Pencairan Fee", "Tarif & Kalkulator", "Musik Obuy")
                    ScrollableTabRow(selectedTabIndex = activeTab, edgePadding = 12.dp) {
                        tabs.forEachIndexed { idx, label ->
                            Tab(
                                selected = activeTab == idx,
                                onClick = { activeTab = idx },
                                text = { Text(label) }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (activeTab) {
                            0 -> OrdersVerificationSection(
                                orders = allOrders,
                                onVerify = { order, approve ->
                                    onVerifyOrder(order, approve) { res ->
                                        res.onSuccess {
                                            Toast.makeText(
                                                context,
                                                if (approve) "Order disetujui & fee promotor dicatat!" else "Order ditolak.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, err.localizedMessage ?: "Gagal memproses order.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                            1 -> PayoutApprovalSection(
                                payouts = allPayouts,
                                onProcess = { payout, approve, note ->
                                    onProcessPayout(payout, approve, note) { res ->
                                        res.onSuccess {
                                            Toast.makeText(
                                                context,
                                                if (approve) "Pencairan ditandai PAID." else "Pencairan ditolak.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, err.localizedMessage ?: "Gagal memproses.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                            2 -> OwnerPricingAndCalculatorSection(
                                config = ownerConfig,
                                onSaveConfig = { updated ->
                                    onUpdateOwnerConfig(updated) { res ->
                                        res.onSuccess {
                                            Toast.makeText(context, "Tarif & rekening pemilik disimpan.", Toast.LENGTH_SHORT).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, err.localizedMessage ?: "Gagal menyimpan tarif.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onSavePin = { newPin ->
                                    onUpdatePin(newPin)
                                    Toast.makeText(context, "PIN sesi brankas diperbarui.", Toast.LENGTH_SHORT).show()
                                }
                            )
                            3 -> OwnerMusicManagerSection(
                                tracks = musicTracks,
                                onAddTrack = { track ->
                                    onAddMusicTrack(track) { res ->
                                        res.onSuccess {
                                            Toast.makeText(context, "Lagu Obuy Jamming ditambahkan!", Toast.LENGTH_SHORT).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, err.localizedMessage ?: "Gagal menambah lagu.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onDeleteTrack = { id ->
                                    onDeleteMusicTrack(id) { res ->
                                        res.onSuccess {
                                            Toast.makeText(context, "Lagu dihapus.", Toast.LENGTH_SHORT).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, err.localizedMessage ?: "Gagal menghapus lagu.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersVerificationSection(
    orders: List<MonetizationOrder>,
    onVerify: (MonetizationOrder, Boolean) -> Unit
) {
    val sorted = orders.sortedBy { if (it.status == "PENDING") 0 else 1 }
    if (sorted.isEmpty()) {
        Text("Belum ada order Boost Iklan atau Booking Terverifikasi.", style = MaterialTheme.typography.bodyMedium)
        return
    }
    sorted.forEach { order ->
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(order.listingTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (order.status) {
                            "VERIFIED" -> SuccessEmerald.copy(alpha = 0.16f)
                            "REJECTED" -> MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                            else -> TertiaryAmber.copy(alpha = 0.25f)
                        }
                    ) {
                        Text(order.status, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
                Text("Jenis: ${order.orderType} • Pembeli: ${order.buyerName}", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "Kotor: ${formatRupiah(order.grossAmountIdr)} • Hak Promotor (${order.promoterFeeRatePercent}%): ${formatRupiah(order.promoterFeeIdr)} • Laba Bersih: ${formatRupiah(order.ownerNetIdr)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryTeal,
                    fontWeight = FontWeight.SemiBold
                )
                if (order.promoterCode.isNotBlank()) {
                    Text("Kode Referral: ${order.promoterCode}", style = MaterialTheme.typography.labelSmall)
                }
                if (order.paymentNote.isNotBlank()) {
                    Text("Catatan: ${order.paymentNote}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (order.status == "PENDING") {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onVerify(order, true) },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Setujui")
                        }
                        OutlinedButton(
                            onClick = { onVerify(order, false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Tolak")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PayoutApprovalSection(
    payouts: List<PayoutRequest>,
    onProcess: (PayoutRequest, Boolean, String) -> Unit
) {
    val sorted = payouts.sortedBy { if (it.status == "REQUESTED") 0 else 1 }
    if (sorted.isEmpty()) {
        Text("Belum ada pengajuan pencairan fee promotor.", style = MaterialTheme.typography.bodyMedium)
        return
    }
    sorted.forEach { payout ->
        var note by remember(payout.id) { mutableStateOf("") }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${payout.promoterName} (${payout.promoterCode})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(payout.status, style = MaterialTheme.typography.labelSmall, color = PrimaryTeal, fontWeight = FontWeight.Bold)
                }
                Text(formatRupiah(payout.amountIdr), style = MaterialTheme.typography.titleMedium, color = SecondaryCoral, fontWeight = FontWeight.Bold)
                Text("${payout.payoutMethod} • ${payout.accountNumber} a.n. ${payout.accountHolder}", style = MaterialTheme.typography.bodySmall)
                if (payout.status == "REQUESTED") {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan Transfer (Opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onProcess(payout, true, note.ifBlank { "Transfer berhasil" }) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sudah Transfer")
                        }
                        OutlinedButton(
                            onClick = { onProcess(payout, false, note.ifBlank { "Ditolak admin" }) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Tolak")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerPricingAndCalculatorSection(
    config: OwnerConfig,
    onSaveConfig: (OwnerConfig) -> Unit,
    onSavePin: (String) -> Unit
) {
    var bankName by remember(config.bankName) { mutableStateOf(config.bankName) }
    var bankAccount by remember(config.bankAccountNumber) { mutableStateOf(config.bankAccountNumber) }
    var bankHolder by remember(config.bankAccountHolder) { mutableStateOf(config.bankAccountHolder) }
    var boost7d by remember(config.boost7dIdr) { mutableStateOf(config.boost7dIdr.toString()) }
    var boost30d by remember(config.boost30dIdr) { mutableStateOf(config.boost30dIdr.toString()) }
    var boost90d by remember(config.boost90dIdr) { mutableStateOf(config.boost90dIdr.toString()) }
    var bookingPct by remember(config.bookingFeePercent) { mutableStateOf(config.bookingFeePercent.toString()) }
    var promoPct by remember(config.promoterFeePercent) { mutableStateOf(config.promoterFeePercent.toString()) }
    var promoProPct by remember(config.promoterProFeePercent) { mutableStateOf(config.promoterProFeePercent.toString()) }
    var minWithdraw by remember(config.minWithdrawIdr) { mutableStateOf(config.minWithdrawIdr.toString()) }
    var newPin by remember { mutableStateOf("") }

    var monthlyBoosts by remember { mutableFloatStateOf(40f) }
    var monthlyBookings by remember { mutableFloatStateOf(25f) }
    var referralSharePct by remember { mutableFloatStateOf(50f) }

    val b30 = boost30d.toLongOrNull() ?: config.boost30dIdr
    val bPct = bookingPct.toLongOrNull() ?: config.bookingFeePercent
    val pPct = promoPct.toLongOrNull() ?: config.promoterFeePercent
    val simGross = (monthlyBoosts.roundToLong() * b30) +
        (monthlyBookings.roundToLong() * FeeCalculator.calculateBookingPlatformFee(2_000_000L, bPct))
    val avgReferralFee = ((simGross * (referralSharePct / 100.0) * (pPct / 100.0))).roundToLong()
    val simOwnerProfit = simGross - avgReferralFee

    // Profit Calculator Card
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Kalkulator Proyeksi Laba Pemilik", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Sorotan 30H/bln: ${monthlyBoosts.roundToLong()} • Booking/bln: ${monthlyBookings.roundToLong()} • Trafik Promotor: ${referralSharePct.roundToLong()}%", style = MaterialTheme.typography.bodySmall)
            Slider(value = monthlyBoosts, onValueChange = { monthlyBoosts = it }, valueRange = 0f..200f)
            Slider(value = monthlyBookings, onValueChange = { monthlyBookings = it }, valueRange = 0f..200f)
            Slider(value = referralSharePct, onValueChange = { referralSharePct = it }, valueRange = 0f..100f)
            Text("Estimasi Omzet Kotor: ${formatRupiah(simGross)}", style = MaterialTheme.typography.bodySmall)
            Text("Estimasi Fee Promotor: ${formatRupiah(avgReferralFee)}", style = MaterialTheme.typography.bodySmall, color = SecondaryCoral)
            Text("Estimasi Laba Bersih Pemilik: ${formatRupiah(simOwnerProfit)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = SuccessEmerald)
        }
    }

    // Config Form
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Pengaturan Rekening & Tarif Platform", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = bankName, onValueChange = { bankName = it }, label = { Text("Bank / QRIS Penerima") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = bankAccount, onValueChange = { bankAccount = it }, label = { Text("Nomor Rekening") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = bankHolder, onValueChange = { bankHolder = it }, label = { Text("Nama Pemilik Rekening") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = boost7d, onValueChange = { boost7d = it.filter(Char::isDigit) }, label = { Text("Boost 7H") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = boost30d, onValueChange = { boost30d = it.filter(Char::isDigit) }, label = { Text("Boost 30H") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = boost90d, onValueChange = { boost90d = it.filter(Char::isDigit) }, label = { Text("Boost 90H") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = bookingPct, onValueChange = { bookingPct = it.filter(Char::isDigit) }, label = { Text("Fee Book %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = promoPct, onValueChange = { promoPct = it.filter(Char::isDigit) }, label = { Text("Fee Prom %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = promoProPct, onValueChange = { promoProPct = it.filter(Char::isDigit) }, label = { Text("Pro %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = minWithdraw, onValueChange = { minWithdraw = it.filter(Char::isDigit) }, label = { Text("Min. Tarik Promotor (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    onSaveConfig(
                        config.copy(
                            bankName = bankName,
                            bankAccountNumber = bankAccount,
                            bankAccountHolder = bankHolder,
                            boost7dIdr = boost7d.toLongOrNull() ?: config.boost7dIdr,
                            boost30dIdr = boost30d.toLongOrNull() ?: config.boost30dIdr,
                            boost90dIdr = boost90d.toLongOrNull() ?: config.boost90dIdr,
                            bookingFeePercent = bookingPct.toLongOrNull() ?: config.bookingFeePercent,
                            promoterFeePercent = promoPct.toLongOrNull() ?: config.promoterFeePercent,
                            promoterProFeePercent = promoProPct.toLongOrNull() ?: config.promoterProFeePercent,
                            minWithdrawIdr = minWithdraw.toLongOrNull() ?: config.minWithdrawIdr
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Text("Simpan Tarif & Rekening")
            }

            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it.filter(Char::isDigit).take(8) },
                    label = { Text("PIN Baru Brankas (4-8 digit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { if (newPin.length in 4..8) { onSavePin(newPin); newPin = "" } },
                    enabled = newPin.length in 4..8
                ) {
                    Text("Ubah PIN")
                }
            }
        }
    }
}

@Composable
private fun OwnerMusicManagerSection(
    tracks: List<MusicTrack>,
    onAddTrack: (MusicTrack) -> Unit,
    onDeleteTrack: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("Reggae Nusantara") }
    var duration by remember { mutableStateOf("03:45") }
    var ytUrl by remember { mutableStateOf("") }
    var spUrl by remember { mutableStateOf("") }
    var mtUrl by remember { mutableStateOf("") }

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = PrimaryTeal)
                Spacer(Modifier.width(6.dp))
                Text("Tambah Lagu Obuy Jamming", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Judul Lagu *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Durasi") }, singleLine = true, modifier = Modifier.weight(0.7f))
            }
            OutlinedTextField(value = ytUrl, onValueChange = { ytUrl = it }, label = { Text("URL YouTube (opsional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = spUrl, onValueChange = { spUrl = it }, label = { Text("URL Spotify (opsional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = mtUrl, onValueChange = { mtUrl = it }, label = { Text("URL Meta FB/IG (opsional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    if (title.trim().length >= 2) {
                        onAddTrack(
                            MusicTrack(
                                title = title.trim(),
                                artist = "Obuy Jamming",
                                genre = genre.trim(),
                                durationLabel = duration.trim(),
                                youtubeUrl = ytUrl.trim(),
                                spotifyUrl = spUrl.trim(),
                                metaUrl = mtUrl.trim(),
                                sortOrder = (tracks.size + 1).toLong()
                            )
                        )
                        title = ""
                        ytUrl = ""
                        spUrl = ""
                        mtUrl = ""
                    }
                },
                enabled = title.trim().length >= 2,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Simpan ke Playlist Aplikasi")
            }
        }
    }

    Text("Daftar Lagu Aktif (${tracks.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    tracks.forEach { tr ->
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(tr.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("${tr.artist} • ${tr.genre} • ${tr.durationLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (tr.id.isNotBlank() && !tr.id.startsWith("obuy_")) {
                    IconButton(onClick = { onDeleteTrack(tr.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus lagu", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
