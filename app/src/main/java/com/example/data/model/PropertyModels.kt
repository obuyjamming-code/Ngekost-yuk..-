package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val APP_OWNER_EMAIL = "obuyjamming@gmail.com"

enum class PropertyType(val label: String, val pluralLabel: String) {
    KOSAN("Kosan", "Kosan"),
    WISMA("Wisma", "Wisma"),
    HOTEL("Hotel", "Hotel"),
    KONTRAKAN("Kontrakan", "Kontrakan");

    companion object {
        fun fromRaw(raw: String): PropertyType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: KOSAN
    }
}

enum class BillingPeriod(val label: String, val shortLabel: String) {
    PER_BULAN("Per Bulan", "/bln"),
    PER_MALAM("Per Malam", "/mlm"),
    PER_TAHUN("Per Tahun", "/thn");

    companion object {
        fun fromRaw(raw: String): BillingPeriod =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PER_BULAN
    }
}

enum class BoostTier(val label: String, val durationDays: Int, val rankWeight: Long) {
    NONE("Gratis", 0, 0L),
    BOOST_7D("Sorotan 7 Hari", 7, 1L),
    BOOST_30D("Sorotan 30 Hari", 30, 2L),
    BOOST_90D("Prioritas Nasional 90 Hari", 90, 3L);

    companion object {
        fun fromRaw(raw: String): BoostTier =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: NONE
    }
}

enum class PriceSortOption(val label: String) {
    CHEAPEST_FIRST("Termurah"),
    EXPENSIVE_FIRST("Termahal"),
    NEWEST("Terbaru"),
    TOP_RATED("Rating Tertinggi")
}

enum class PriceTierFilter(val label: String, val minIdr: Long, val maxIdr: Long?) {
    ALL("Semua Harga", 0L, null),
    BUDGET("Hemat (< 1 Jt)", 0L, 999_999L),
    MID("Menengah (1 - 3 Jt)", 1_000_000L, 3_000_000L),
    PREMIUM("Eksklusif (3 - 10 Jt)", 3_000_001L, 10_000_000L),
    LUXURY("Mewah (> 10 Jt)", 10_000_001L, null)
}

enum class IndonesiaRegion(val label: String) {
    ALL("Seluruh Indonesia"),
    JAWA_BALI("Jawa & Bali"),
    SUMATERA("Sumatera"),
    KALIMANTAN("Kalimantan"),
    SULAWESI("Sulawesi"),
    MALUKU_PAPUA_NT("Nusa Tenggara, Maluku & Papua")
}

enum class MusicPlatform(val label: String) {
    YOUTUBE("YouTube"),
    SPOTIFY("Spotify"),
    META("Meta (FB/IG)")
}

@Keep
@IgnoreExtraProperties
data class PropertyListing(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerDisplayName: String = "",
    val title: String = "",
    val propertyType: String = PropertyType.KOSAN.name,
    val description: String = "",
    val priceIdr: Long = 500_000L,
    val billingPeriod: String = BillingPeriod.PER_BULAN.name,
    val countryCode: String = "ID",
    val countryName: String = "Indonesia",
    val province: String = "",
    val city: String = "",
    val address: String = "",
    val latitude: Double = -6.2088,
    val longitude: Double = 106.8456,
    val contactName: String = "",
    val contactPhone: String = "",
    val contactWhatsapp: String = "",
    val facilities: List<String> = emptyList(),
    val photoUrls: List<String> = emptyList(),
    val rating: Double = 4.8,
    val availableRooms: Long = 1L,
    val visibility: String = "public",
    val boostTier: String = BoostTier.NONE.name,
    val featuredRank: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedType(): PropertyType = PropertyType.fromRaw(propertyType)
    fun resolvedBillingPeriod(): BillingPeriod = BillingPeriod.fromRaw(billingPeriod)
    fun resolvedBoostTier(): BoostTier = BoostTier.fromRaw(boostTier)
    fun resolvedRegion(): IndonesiaRegion = IndonesiaCities.regionForProvince(province)
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val preferredRegion: String = IndonesiaRegion.ALL.label,
    val referralCode: String = "",
    val referredByCode: String = "",
    val referredByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReferralCodeLookup(
    @DocumentId val id: String = "",
    val code: String = "",
    val promoterUid: String = "",
    val promoterName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FavoriteListing(
    @DocumentId val id: String = "",
    val listingId: String = "",
    val title: String = "",
    val city: String = "",
    val priceIdr: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MonetizationOrder(
    @DocumentId val id: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val orderType: String = "BOOST_7D",
    val propertyPriceIdr: Long = 0L,
    val grossAmountIdr: Long = 25_000L,
    val promoterCode: String = "",
    val promoterUid: String = "",
    val promoterFeeRatePercent: Long = 0L,
    val promoterFeeIdr: Long = 0L,
    val ownerNetIdr: Long = 25_000L,
    val paymentNote: String = "",
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReferralCommission(
    @DocumentId val id: String = "",
    val promoterUid: String = "",
    val promoterCode: String = "",
    val orderId: String = "",
    val orderTitle: String = "",
    val grossOrderIdr: Long = 0L,
    val feePercent: Long = 20L,
    val commissionIdr: Long = 0L,
    val status: String = "AVAILABLE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PayoutRequest(
    @DocumentId val id: String = "",
    val promoterUid: String = "",
    val promoterName: String = "",
    val promoterCode: String = "",
    val amountIdr: Long = 50_000L,
    val payoutMethod: String = "DANA",
    val accountNumber: String = "",
    val accountHolder: String = "",
    val status: String = "REQUESTED",
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OwnerConfig(
    @DocumentId val id: String = "",
    val configKey: String = "pricing",
    val bankName: String = "BCA / QRIS Pemilik",
    val bankAccountNumber: String = "8890-1122-3344",
    val bankAccountHolder: String = "Obuy Jamming (Owner)",
    val boost7dIdr: Long = 25_000L,
    val boost30dIdr: Long = 75_000L,
    val boost90dIdr: Long = 175_000L,
    val bookingFeePercent: Long = 5L,
    val promoterFeePercent: Long = 20L,
    val promoterProFeePercent: Long = 25L,
    val minWithdrawIdr: Long = 50_000L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MusicTrack(
    @DocumentId val id: String = "",
    val title: String = "",
    val artist: String = "Obuy Jamming",
    val genre: String = "Reggae Nusantara",
    val durationLabel: String = "03:45",
    val youtubeUrl: String = "",
    val spotifyUrl: String = "",
    val metaUrl: String = "",
    val embedUrl: String = "",
    val sortOrder: Long = 1L,
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object FeeCalculator {
    fun calculateBookingPlatformFee(propertyPriceIdr: Long, feePercent: Long = 5L): Long {
        val raw = (propertyPriceIdr * feePercent) / 100L
        return raw.coerceIn(25_000L, 350_000L)
    }

    fun calculateFeeSplit(
        grossAmountIdr: Long,
        hasPromoter: Boolean,
        promoterRatePercent: Long
    ): Pair<Long, Long> {
        if (!hasPromoter || promoterRatePercent <= 0L) {
            return 0L to grossAmountIdr
        }
        val promoterFee = ((grossAmountIdr * promoterRatePercent) / 100L).coerceIn(0L, grossAmountIdr)
        val ownerNet = grossAmountIdr - promoterFee
        return promoterFee to ownerNet
    }
}

data class IndonesianCity(
    val name: String,
    val province: String,
    val region: IndonesiaRegion,
    val latitude: Double,
    val longitude: Double
)

object IndonesiaCities {
    val all: List<IndonesianCity> = listOf(
        IndonesianCity("Jakarta Selatan", "DKI Jakarta", IndonesiaRegion.JAWA_BALI, -6.2615, 106.8106),
        IndonesianCity("Jakarta Pusat", "DKI Jakarta", IndonesiaRegion.JAWA_BALI, -6.1862, 106.8341),
        IndonesianCity("Jakarta Barat", "DKI Jakarta", IndonesiaRegion.JAWA_BALI, -6.1674, 106.7637),
        IndonesianCity("Bandung", "Jawa Barat", IndonesiaRegion.JAWA_BALI, -6.9175, 107.6191),
        IndonesianCity("Depok", "Jawa Barat", IndonesiaRegion.JAWA_BALI, -6.4025, 106.7942),
        IndonesianCity("Bogor", "Jawa Barat", IndonesiaRegion.JAWA_BALI, -6.5971, 106.8060),
        IndonesianCity("Bekasi", "Jawa Barat", IndonesiaRegion.JAWA_BALI, -6.2383, 106.9756),
        IndonesianCity("Tangerang", "Banten", IndonesiaRegion.JAWA_BALI, -6.1783, 106.6319),
        IndonesianCity("Semarang", "Jawa Tengah", IndonesiaRegion.JAWA_BALI, -6.9667, 110.4167),
        IndonesianCity("Surakarta (Solo)", "Jawa Tengah", IndonesiaRegion.JAWA_BALI, -7.5755, 110.8243),
        IndonesianCity("Yogyakarta", "DI Yogyakarta", IndonesiaRegion.JAWA_BALI, -7.7956, 110.3695),
        IndonesianCity("Sleman", "DI Yogyakarta", IndonesiaRegion.JAWA_BALI, -7.7161, 110.3554),
        IndonesianCity("Surabaya", "Jawa Timur", IndonesiaRegion.JAWA_BALI, -7.2575, 112.7521),
        IndonesianCity("Malang", "Jawa Timur", IndonesiaRegion.JAWA_BALI, -7.9666, 112.6326),
        IndonesianCity("Denpasar", "Bali", IndonesiaRegion.JAWA_BALI, -8.6705, 115.2126),
        IndonesianCity("Ubud", "Bali", IndonesiaRegion.JAWA_BALI, -8.5069, 115.2625),
        IndonesianCity("Kuta / Badung", "Bali", IndonesiaRegion.JAWA_BALI, -8.7185, 115.1686),
        IndonesianCity("Medan", "Sumatera Utara", IndonesiaRegion.SUMATERA, 3.5952, 98.6722),
        IndonesianCity("Banda Aceh", "Aceh", IndonesiaRegion.SUMATERA, 5.5483, 95.3238),
        IndonesianCity("Padang", "Sumatera Barat", IndonesiaRegion.SUMATERA, -0.9471, 100.4172),
        IndonesianCity("Pekanbaru", "Riau", IndonesiaRegion.SUMATERA, 0.5071, 101.4478),
        IndonesianCity("Batam", "Kepulauan Riau", IndonesiaRegion.SUMATERA, 1.0456, 104.0305),
        IndonesianCity("Palembang", "Sumatera Selatan", IndonesiaRegion.SUMATERA, -2.9761, 104.7754),
        IndonesianCity("Bandar Lampung", "Lampung", IndonesiaRegion.SUMATERA, -5.4500, 105.2667),
        IndonesianCity("Pontianak", "Kalimantan Barat", IndonesiaRegion.KALIMANTAN, -0.0263, 109.3425),
        IndonesianCity("Banjarmasin", "Kalimantan Selatan", IndonesiaRegion.KALIMANTAN, -3.3186, 114.5944),
        IndonesianCity("Balikpapan", "Kalimantan Timur", IndonesiaRegion.KALIMANTAN, -1.2379, 116.8529),
        IndonesianCity("Samarinda", "Kalimantan Timur", IndonesiaRegion.KALIMANTAN, -0.5022, 117.1536),
        IndonesianCity("Makassar", "Sulawesi Selatan", IndonesiaRegion.SULAWESI, -5.1477, 119.4327),
        IndonesianCity("Manado", "Sulawesi Utara", IndonesiaRegion.SULAWESI, 1.4748, 124.8421),
        IndonesianCity("Kendari", "Sulawesi Tenggara", IndonesiaRegion.SULAWESI, -3.9985, 122.5129),
        IndonesianCity("Palu", "Sulawesi Tengah", IndonesiaRegion.SULAWESI, -0.8917, 119.8707),
        IndonesianCity("Mataram", "Nusa Tenggara Barat", IndonesiaRegion.MALUKU_PAPUA_NT, -8.5833, 116.1167),
        IndonesianCity("Labuan Bajo", "Nusa Tenggara Timur", IndonesiaRegion.MALUKU_PAPUA_NT, -8.4964, 119.8877),
        IndonesianCity("Kupang", "Nusa Tenggara Timur", IndonesiaRegion.MALUKU_PAPUA_NT, -10.1772, 123.6070),
        IndonesianCity("Ambon", "Maluku", IndonesiaRegion.MALUKU_PAPUA_NT, -3.6954, 128.1814),
        IndonesianCity("Jayapura", "Papua", IndonesiaRegion.MALUKU_PAPUA_NT, -2.5916, 140.6690)
    )

    val commonFacilities: List<String> = listOf(
        "WiFi Kencang",
        "AC",
        "Kamar Mandi Dalam",
        "Parkir Motor",
        "Parkir Mobil",
        "Dapur Bersama",
        "Keamanan 24 Jam",
        "Air Panas",
        "Kasur & Lemari",
        "Meja Kerja",
        "Mesin Cuci",
        "Listrik Termasuk",
        "Sarapan",
        "Kolam Renang"
    )

    fun regionForProvince(province: String): IndonesiaRegion {
        val lower = province.lowercase()
        return when {
            lower.contains("jakarta") || lower.contains("jawa") || lower.contains("banten") ||
                lower.contains("yogyakarta") || lower.contains("bali") -> IndonesiaRegion.JAWA_BALI
            lower.contains("sumatera") || lower.contains("aceh") || lower.contains("riau") ||
                lower.contains("lampung") || lower.contains("jambi") || lower.contains("bengkulu") ||
                lower.contains("bangka") -> IndonesiaRegion.SUMATERA
            lower.contains("kalimantan") -> IndonesiaRegion.KALIMANTAN
            lower.contains("sulawesi") || lower.contains("gorontalo") -> IndonesiaRegion.SULAWESI
            else -> IndonesiaRegion.MALUKU_PAPUA_NT
        }
    }
}
