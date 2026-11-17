package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.MusicPlatform
import com.example.data.model.MusicTrack
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryCoral
import com.example.ui.theme.TertiaryAmber

@Composable
fun ObuyJammingMiniPlayerBar(
    tracks: List<MusicTrack>,
    currentIndex: Int,
    selectedPlatform: MusicPlatform,
    isWebExpanded: Boolean,
    onToggleWebExpanded: () -> Unit,
    onOpenSheet: () -> Unit,
    onPrevTrack: () -> Unit,
    onNextTrack: () -> Unit,
    onCloseBar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTrack = tracks.getOrNull(currentIndex) ?: return
    val context = LocalContext.current
    val playerUrl = resolveInAppUrl(activeTrack, selectedPlatform)

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        // Collapsible live embedded WebView player (stays active while scrolling kosan!)
        AnimatedVisibility(visible = isWebExpanded) {
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().height(190.dp)
            ) {
                Box {
                    EmbeddedPlatformWebView(url = playerUrl, modifier = Modifier.matchParentSize())
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 10.dp),
                        color = PrimaryTeal.copy(alpha = 0.9f),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "Obuy Jamming • ${selectedPlatform.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Compact floating mini-bar
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isWebExpanded) 0.dp else 16.dp,
                topEnd = if (isWebExpanded) 0.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenSheet() }
                .testTag("obuy_mini_player")
        ) {
            Row(
                modifier = Modifier
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0B5555), Color(0xFF143C3C))))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(TertiaryAmber),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Pemutar Obuy Jamming",
                        tint = Color(0xFF281A00)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeTrack.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${activeTrack.artist} • ${selectedPlatform.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
                IconButton(onClick = onPrevTrack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Lagu sebelumnya", tint = Color.White)
                }
                IconButton(
                    onClick = onToggleWebExpanded,
                    modifier = Modifier.size(36.dp).testTag("toggle_inline_player")
                ) {
                    Icon(
                        imageVector = if (isWebExpanded) Icons.Default.ExpandMore else Icons.Default.PlayArrow,
                        contentDescription = if (isWebExpanded) "Kecilkan pemutar" else "Putar di aplikasi",
                        tint = TertiaryAmber
                    )
                }
                IconButton(onClick = onNextTrack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Lagu berikutnya", tint = Color.White)
                }
                IconButton(
                    onClick = {
                        openExternalMusicUrl(context, resolveExternalUrl(activeTrack, selectedPlatform))
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Buka di ${selectedPlatform.label}",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onCloseBar, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup pemutar", tint = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObuyJammingPlaylistSheet(
    tracks: List<MusicTrack>,
    currentIndex: Int,
    selectedPlatform: MusicPlatform,
    onSelectPlatform: (MusicPlatform) -> Unit,
    onSelectTrack: (Int) -> Unit,
    onPlayInApp: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activeTrack = tracks.getOrNull(currentIndex)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Radio Kos: Obuy Jamming",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dengarkan musik santai sambil pilih kosan terbaik",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Platform selector: YouTube, Spotify, Meta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MusicPlatform.entries.forEach { platform ->
                    FilterChip(
                        selected = selectedPlatform == platform,
                        onClick = { onSelectPlatform(platform) },
                        label = { Text(platform.label) },
                        modifier = Modifier.weight(1f).testTag("music_platform_${platform.name}")
                    )
                }
            }

            if (activeTrack != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            text = "Sedang dipilih: ${activeTrack.title}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${activeTrack.artist} • ${activeTrack.genre} • ${activeTrack.durationLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onPlayInApp()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f).testTag("btn_play_in_app"),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Putar di Aplikasi")
                            }
                            OutlinedButton(
                                onClick = {
                                    openExternalMusicUrl(context, resolveExternalUrl(activeTrack, selectedPlatform))
                                },
                                modifier = Modifier.weight(1f).testTag("btn_open_music_platform")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Buka ${selectedPlatform.label}")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "Daftar Lagu Obuy Jamming (${tracks.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(230.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(tracks, key = { idx, item -> item.id.ifBlank { "$idx-${item.title}" } }) { index, track ->
                    val isSelected = index == currentIndex
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTrack(index) }
                            .testTag("music_track_$index")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (isSelected) PrimaryTeal else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = "${track.artist} • ${track.genre}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = track.durationLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resolveInAppUrl(track: MusicTrack, platform: MusicPlatform): String {
    return when (platform) {
        MusicPlatform.YOUTUBE -> track.embedUrl.ifBlank { track.youtubeUrl }
        MusicPlatform.SPOTIFY -> track.spotifyUrl
        MusicPlatform.META -> track.metaUrl
    }
}

private fun resolveExternalUrl(track: MusicTrack, platform: MusicPlatform): String {
    return when (platform) {
        MusicPlatform.YOUTUBE -> track.youtubeUrl
        MusicPlatform.SPOTIFY -> track.spotifyUrl
        MusicPlatform.META -> track.metaUrl
    }
}

private fun openExternalMusicUrl(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
        Toast.makeText(context, "Tidak dapat membuka tautan musik.", Toast.LENGTH_SHORT).show()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EmbeddedPlatformWebView(url: String, modifier: Modifier = Modifier) {
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
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        }
    )
}
