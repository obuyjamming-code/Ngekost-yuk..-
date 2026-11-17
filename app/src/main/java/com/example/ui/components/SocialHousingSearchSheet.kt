package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.IndonesiaCities
import com.example.data.model.PropertyType
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryCoral
import com.example.ui.util.buildInstagramHousingSearchUrl
import com.example.ui.util.buildInstagramHousingTagUrl
import com.example.ui.util.buildTikTokHousingSearchUrl
import com.example.ui.util.buildTikTokHousingTagUrl
import com.example.ui.util.openExternalUrl

private enum class SocialRefSource(val label: String, val platformTitle: String) {
    TIKTOK_SEARCH("TikTok Pencarian", "TikTok"),
    TIKTOK_HASHTAG("TikTok #Hashtag", "TikTok"),
    INSTAGRAM_TAG("Instagram #Hashtag", "Instagram"),
    INSTAGRAM_SEARCH("Instagram Pencarian", "Instagram")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialHousingSearchSheet(
    initialCity: String,
    initialType: PropertyType?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var cityInput by remember(initialCity) {
        mutableStateOf(initialCity.trim().ifBlank { "Jakarta Selatan" })
    }
    var selectedType by remember(initialType) { mutableStateOf(initialType) }
    var selectedSource by remember { mutableStateOf(SocialRefSource.TIKTOK_SEARCH) }
    var showInlineBrowser by remember { mutableStateOf(false) }
    var reloadNonce by remember { mutableStateOf(0) }

    val typeKeyword = selectedType?.label?.lowercase() ?: "kosan wisma kontrakan hotel"
    val tagPrefix = selectedType?.label?.lowercase() ?: "infokos"

    val targetUrl = remember(cityInput, selectedType, selectedSource) {
        when (selectedSource) {
            SocialRefSource.TIKTOK_SEARCH -> buildTikTokHousingSearchUrl(
                city = cityInput,
                typeLabel = typeKeyword,
                extraKeyword = "murah review"
            )
            SocialRefSource.TIKTOK_HASHTAG -> buildTikTokHousingTagUrl(
                city = cityInput,
                typeLabel = tagPrefix
            )
            SocialRefSource.INSTAGRAM_TAG -> buildInstagramHousingTagUrl(
                city = cityInput,
                typeLabel = tagPrefix
            )
            SocialRefSource.INSTAGRAM_SEARCH -> buildInstagramHousingSearchUrl(
                city = cityInput,
                typeLabel = selectedType?.label ?: "kosan"
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SecondaryCoral.copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = SecondaryCoral)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cari Referensi di TikTok & Instagram",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Lihat video room tour, kondisi lingkungan & info kos terbaru per kota.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = cityInput,
                onValueChange = { cityInput = it },
                label = { Text("Kota / Kabupaten / Daerah Tujuan") },
                placeholder = { Text("Contoh: Bandung, Sleman, Malang, Bali...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("social_city_input")
            )

            // Quick Indonesian City Chips
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IndonesiaCities.all.take(16).forEach { city ->
                    FilterChip(
                        selected = cityInput.equals(city.name, ignoreCase = true),
                        onClick = { cityInput = city.name },
                        label = { Text(city.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Property Type Chips
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { selectedType = null },
                    label = { Text("Semua Hunian") }
                )
                PropertyType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = if (selectedType == type) null else type },
                        label = { Text(type.label) }
                    )
                }
            }

            // Platform Source Selector
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SocialRefSource.entries.forEach { src ->
                    FilterChip(
                        selected = selectedSource == src,
                        onClick = { selectedSource = src },
                        label = { Text(src.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (src.platformTitle == "TikTok") Icons.Default.PlayCircle else Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("social_src_${src.name}")
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            ) {
                Text(
                    text = "Target: ${selectedSource.platformTitle} • ${selectedType?.label ?: "Semua Hunian"} di ${cityInput.ifBlank { "Indonesia" }}",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryTeal,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        openExternalUrl(context, targetUrl, selectedSource.platformTitle)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("btn_open_social_external"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Buka ${selectedSource.platformTitle}")
                }
                OutlinedButton(
                    onClick = { showInlineBrowser = !showInlineBrowser },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("btn_toggle_social_webview"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (showInlineBrowser) "Tutup Preview" else "Lihat di Sini")
                }
            }

            AnimatedVisibility(visible = showInlineBrowser) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth().height(340.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PrimaryTeal)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${selectedSource.platformTitle} • ${cityInput.ifBlank { "Indonesia" }}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                            Row {
                                IconButton(onClick = { reloadNonce++ }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Muat ulang", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { showInlineBrowser = false }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Tutup preview", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        SocialSearchWebView(
                            url = targetUrl,
                            reloadKey = reloadNonce,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SocialSearchWebView(
    url: String,
    reloadKey: Int,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                loadUrl(url)
            }
        },
        update = { webView ->
            val tagKey = "$url#$reloadKey"
            if (webView.tag != tagKey) {
                webView.tag = tagKey
                webView.loadUrl(url)
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        }
    )
}
