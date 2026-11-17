package com.example.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private val indonesianLocale = Locale.Builder().setLanguage("id").setRegion("ID").build()

fun formatRupiah(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(indonesianLocale)
    return "Rp ${formatter.format(amount)}"
}

fun formatCompactRupiah(amount: Long): String {
    return when {
        amount >= 1_000_000_000L -> {
            val valM = amount / 1_000_000_000.0
            "Rp ${String.format(indonesianLocale, "%.1f", valM)}M"
        }
        amount >= 1_000_000L -> {
            val valJt = amount / 1_000_000.0
            val formatted = if (valJt % 1.0 == 0.0) "${valJt.toInt()}" else String.format(indonesianLocale, "%.1f", valJt)
            "Rp ${formatted}jt"
        }
        amount >= 1_000L -> {
            val valRb = amount / 1_000L
            "Rp ${valRb}rb"
        }
        else -> formatRupiah(amount)
    }
}

fun drawableResForKey(photoKey: String): Int? {
    return when (photoKey.removePrefix("drawable:")) {
        "img_kosan_modern" -> R.drawable.img_kosan_modern_1794939771782
        "img_wisma_asri" -> R.drawable.img_wisma_asri_1794939770624
        "img_hotel_luxury" -> R.drawable.img_hotel_luxury_1794939771715
        "img_kontrakan_nyaman" -> R.drawable.img_kontrakan_nyaman_1794939771947
        else -> null
    }
}

@Composable
fun PropertyPhoto(
    photoRef: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val drawableRes = remember(photoRef) { drawableResForKey(photoRef) }
    if (drawableRes != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(drawableRes)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = contentScale
        )
    } else if (photoRef.startsWith("data:image/")) {
        val bitmap by produceState<ImageBitmap?>(initialValue = null, photoRef) {
            value = withContext(Dispatchers.Default) {
                try {
                    val base64Part = photoRef.substringAfter("base64,", "")
                    if (base64Part.isNotBlank()) {
                        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    } else null
                } catch (_: Exception) {
                    null
                }
            }
        }
        val decoded = bitmap
        if (decoded != null) {
            Image(
                bitmap = decoded,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(R.drawable.img_kosan_modern_1794939771782)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = contentScale
            )
        }
    } else if (photoRef.startsWith("http")) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photoRef)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = contentScale
        )
    } else {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(R.drawable.img_kosan_modern_1794939771782)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale
            )
        }
    }
}

fun compressImageUriToDataUri(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = inputStream.use { BitmapFactory.decodeStream(it) } ?: return null
        val maxSide = 560
        val scale = maxSide.toFloat() / max(originalBitmap.width, originalBitmap.height).toFloat()
        val targetBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                originalBitmap,
                (originalBitmap.width * scale).roundToInt().coerceAtLeast(1),
                (originalBitmap.height * scale).roundToInt().coerceAtLeast(1),
                true
            )
        } else {
            originalBitmap
        }
        val output = ByteArrayOutputStream()
        targetBitmap.compress(Bitmap.CompressFormat.JPEG, 58, output)
        val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        if (targetBitmap != originalBitmap) targetBitmap.recycle()
        originalBitmap.recycle()
        val dataUri = "data:image/jpeg;base64,$encoded"
        if (dataUri.length <= 340_000) dataUri else null
    } catch (_: Exception) {
        null
    }
}

fun openGoogleMaps(context: Context, latitude: Double, longitude: Double, label: String) {
    val encodedLabel = Uri.encode(label)
    val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
    try {
        context.startActivity(mapIntent)
    } catch (_: Exception) {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        } catch (_: Exception) {
            Toast.makeText(context, "Tidak ada aplikasi peta yang tersedia.", Toast.LENGTH_SHORT).show()
        }
    }
}

fun openWhatsApp(context: Context, phoneRaw: String, propertyTitle: String) {
    val digits = phoneRaw.filter { it.isDigit() }
    val normalized = when {
        digits.startsWith("0") -> "62${digits.drop(1)}"
        digits.startsWith("62") -> digits
        else -> "62$digits"
    }
    val message = Uri.encode("Halo, saya tertarik dengan properti \"$propertyTitle\" di aplikasi Ngekost yuk..! Apakah masih tersedia?")
    val waUri = Uri.parse("https://wa.me/$normalized?text=$message")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, waUri))
    } catch (_: Exception) {
        Toast.makeText(context, "Tidak dapat membuka WhatsApp.", Toast.LENGTH_SHORT).show()
    }
}

fun openPhoneDialer(context: Context, phoneRaw: String) {
    val dialUri = Uri.parse("tel:${phoneRaw.trim()}")
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, dialUri))
    } catch (_: Exception) {
        Toast.makeText(context, "Tidak dapat membuka dialer telepon.", Toast.LENGTH_SHORT).show()
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Disalin: $text", Toast.LENGTH_SHORT).show()
}

fun buildTikTokHousingSearchUrl(
    city: String,
    typeLabel: String = "kosan",
    extraKeyword: String = ""
): String {
    val cleanCity = city.trim().ifBlank { "Indonesia" }
    val cleanType = typeLabel.trim().ifBlank { "kosan" }
    val query = listOf(cleanType, cleanCity, extraKeyword.trim())
        .filter { it.isNotBlank() }
        .joinToString(" ")
    return "https://www.tiktok.com/search?q=${Uri.encode(query)}"
}

fun buildTikTokHousingTagUrl(
    city: String,
    typeLabel: String = "infokos"
): String {
    val slugCity = city.lowercase()
        .replace(Regex("[^a-z0-9]"), "")
        .ifBlank { "indonesia" }
    val prefix = when (typeLabel.lowercase()) {
        "wisma" -> "wisma"
        "hotel" -> "hotel"
        "kontrakan" -> "kontrakan"
        else -> "infokos"
    }
    return "https://www.tiktok.com/tag/$prefix$slugCity"
}

fun buildInstagramHousingTagUrl(
    city: String,
    typeLabel: String = "infokos"
): String {
    val slugCity = city.lowercase()
        .replace(Regex("[^a-z0-9]"), "")
        .ifBlank { "indonesia" }
    val prefix = when (typeLabel.lowercase()) {
        "wisma" -> "wisma"
        "hotel" -> "hotel"
        "kontrakan" -> "kontrakan"
        else -> "infokos"
    }
    return "https://www.instagram.com/explore/tags/$prefix$slugCity/"
}

fun buildInstagramHousingSearchUrl(
    city: String,
    typeLabel: String = "kosan"
): String {
    val cleanCity = city.trim().ifBlank { "Indonesia" }
    val cleanType = typeLabel.trim().ifBlank { "kosan" }
    return "https://www.instagram.com/explore/search/keyword/?q=${Uri.encode("$cleanType $cleanCity")}"
}

fun openExternalUrl(context: Context, url: String, platformName: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
        Toast.makeText(context, "Tidak dapat membuka $platformName.", Toast.LENGTH_SHORT).show()
    }
}

