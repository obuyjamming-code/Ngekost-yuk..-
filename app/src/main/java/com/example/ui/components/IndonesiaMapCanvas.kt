package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.BoostTier
import com.example.data.model.PropertyListing
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryCoral
import com.example.ui.theme.TertiaryAmber
import com.example.ui.util.PropertyPhoto
import com.example.ui.util.formatCompactRupiah
import com.example.ui.util.formatRupiah
import kotlin.math.roundToInt

private const val MIN_LAT = -11.5
private const val MAX_LAT = 6.5
private const val MIN_LON = 94.5
private const val MAX_LON = 141.5

private val sumateraPoly = listOf(
    5.5 to 95.3, 3.8 to 98.7, 1.4 to 101.8, -0.9 to 104.2,
    -3.2 to 105.9, -5.8 to 105.7, -5.4 to 104.2, -2.5 to 101.0, 1.5 to 98.6, 4.4 to 96.2
)
private val jawaPoly = listOf(
    -6.0 to 105.8, -6.2 to 108.5, -6.9 to 110.4, -7.2 to 112.9,
    -8.2 to 114.4, -8.5 to 112.6, -7.8 to 110.2, -7.4 to 107.6, -6.9 to 105.9
)
private val baliNusaPoly = listOf(
    -8.1 to 114.5, -8.7 to 115.6, -8.6 to 116.5, -8.8 to 118.8,
    -8.4 to 120.8, -10.2 to 123.7, -9.5 to 124.9, -8.3 to 122.9, -8.5 to 118.2, -8.8 to 115.1
)
private val kalimantanPoly = listOf(
    4.2 to 117.6, 1.5 to 118.9, -1.2 to 117.2, -3.5 to 115.9,
    -3.3 to 113.2, -2.6 to 110.2, 0.2 to 109.1, 1.9 to 109.7, 1.4 to 113.8, 3.9 to 115.9
)
private val sulawesiPoly = listOf(
    1.6 to 125.1, 0.5 to 123.5, 0.9 to 120.5, -1.5 to 119.3,
    -5.4 to 119.4, -4.8 to 122.6, -2.4 to 121.6, -0.5 to 122.8, 1.0 to 124.8
)
private val papuaPoly = listOf(
    -0.8 to 131.0, -2.1 to 134.5, -2.5 to 140.9, -8.3 to 140.9,
    -7.8 to 138.2, -5.2 to 135.4, -3.7 to 133.2, -2.5 to 130.5
)
private val malukuPoly = listOf(
    1.8 to 127.8, 0.4 to 128.6, -3.6 to 128.4, -3.9 to 130.7, -2.9 to 127.4
)

private val allIslandPolys = listOf(
    sumateraPoly, jawaPoly, baliNusaPoly, kalimantanPoly, sulawesiPoly, malukuPoly, papuaPoly
)

private fun latLonToScreen(
    lat: Double,
    lon: Double,
    width: Float,
    height: Float,
    zoom: Float,
    pan: Offset
): Offset {
    val normX = ((lon - MIN_LON) / (MAX_LON - MIN_LON)).toFloat()
    val normY = (1.0 - (lat - MIN_LAT) / (MAX_LAT - MIN_LAT)).toFloat()
    val baseX = normX * width
    val baseY = normY * height
    val cx = width / 2f
    val cy = height / 2f
    return Offset(
        x = (baseX - cx) * zoom + cx + pan.x,
        y = (baseY - cy) * zoom + cy + pan.y
    )
}

private fun screenToLatLon(
    screen: Offset,
    width: Float,
    height: Float,
    zoom: Float,
    pan: Offset
): Pair<Double, Double> {
    val cx = width / 2f
    val cy = height / 2f
    val baseX = (screen.x - pan.x - cx) / zoom + cx
    val baseY = (screen.y - pan.y - cy) / zoom + cy
    val normX = (baseX / width).coerceIn(0f, 1f)
    val normY = (baseY / height).coerceIn(0f, 1f)
    val lon = MIN_LON + normX * (MAX_LON - MIN_LON)
    val lat = MIN_LAT + (1f - normY) * (MAX_LAT - MIN_LAT)
    return lat to lon
}

@Composable
fun IndonesiaPropertyMap(
    listings: List<PropertyListing>,
    onListingSelected: (PropertyListing) -> Unit,
    modifier: Modifier = Modifier
) {
    var zoom by remember { mutableFloatStateOf(1.15f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var selectedListing by remember { mutableStateOf<PropertyListing?>(listings.firstOrNull()) }

    val oceanStart = Color(0xFFD8F0F0)
    val oceanEnd = Color(0xFFBFE2E4)
    val landColor = Color(0xFF0D6E6E).copy(alpha = 0.24f)
    val landStroke = Color(0xFF0D6E6E).copy(alpha = 0.55f)
    val gridColor = Color(0xFF0D6E6E).copy(alpha = 0.09f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(oceanStart, oceanEnd)))
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { _, panChange, zoomChange, _ ->
                    val newZoom = (zoom * zoomChange).coerceIn(1f, 5.5f)
                    zoom = newZoom
                    val maxOffset = size.width * (newZoom - 0.7f)
                    pan = Offset(
                        x = (pan.x + panChange.x).coerceIn(-maxOffset, maxOffset),
                        y = (pan.y + panChange.y).coerceIn(-maxOffset, maxOffset)
                    )
                }
            }
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArchipelagoGridAndIslands(
                width = size.width,
                height = size.height,
                zoom = zoom,
                pan = pan,
                gridColor = gridColor,
                landColor = landColor,
                landStroke = landStroke
            )
        }

        // Render interactive property price-tag pins
        listings.forEach { item ->
            val pos = latLonToScreen(item.latitude, item.longitude, widthPx, heightPx, zoom, pan)
            if (pos.x in -80f..(widthPx + 80f) && pos.y in -60f..(heightPx + 60f)) {
                val isSelected = selectedListing?.id == item.id
                val isBoosted = item.resolvedBoostTier() != BoostTier.NONE
                val bgColor = when {
                    isSelected -> SecondaryCoral
                    isBoosted -> TertiaryAmber
                    else -> PrimaryTeal
                }
                val textColor = if (isBoosted && !isSelected) Color(0xFF281A00) else Color.White

                Surface(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (pos.x - 42.dp.toPx()).roundToInt(),
                                y = (pos.y - 18.dp.toPx()).roundToInt()
                            )
                        }
                        .testTag("map_pin_${item.id}")
                        .clickable { selectedListing = item },
                    shape = RoundedCornerShape(14.dp),
                    color = bgColor,
                    shadowElevation = if (isSelected) 8.dp else 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = Color.White,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatCompactRupiah(item.priceIdr),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Top badge + zoom controls
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 2.dp
        ) {
            Text(
                text = "Peta Nusantara • ${listings.size} properti",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilledTonalIconButton(
                onClick = { zoom = (zoom * 1.35f).coerceAtMost(5.5f) },
                modifier = Modifier.size(40.dp).testTag("map_zoom_in")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Perbesar peta")
            }
            FilledTonalIconButton(
                onClick = { zoom = (zoom / 1.35f).coerceAtLeast(1f) },
                modifier = Modifier.size(40.dp).testTag("map_zoom_out")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Perkecil peta")
            }
            FilledTonalIconButton(
                onClick = {
                    zoom = 1.15f
                    pan = Offset.Zero
                },
                modifier = Modifier.size(40.dp).testTag("map_reset")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Reset peta Indonesia")
            }
        }

        // Bottom floating selected property card
        selectedListing?.let { current ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clickable { onListingSelected(current) }
                    .testTag("map_preview_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PropertyPhoto(
                        photoRef = current.photoUrls.firstOrNull().orEmpty(),
                        contentDescription = current.title,
                        modifier = Modifier
                            .size(74.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = current.resolvedType().label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = TertiaryAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = String.format("%.1f", current.rating),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = current.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${current.city}, ${current.province}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = "${formatRupiah(current.priceIdr)} ${current.resolvedBillingPeriod().shortLabel}",
                            style = MaterialTheme.typography.titleSmall,
                            color = SecondaryCoral,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractivePinMapBox(
    latitude: Double,
    longitude: Double,
    title: String,
    interactivePick: Boolean,
    onCoordinatePicked: ((Double, Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val oceanStart = Color(0xFFD8F0F0)
    val oceanEnd = Color(0xFFBFE2E4)
    val landColor = Color(0xFF0D6E6E).copy(alpha = 0.25f)
    val landStroke = Color(0xFF0D6E6E).copy(alpha = 0.55f)
    val gridColor = Color(0xFF0D6E6E).copy(alpha = 0.08f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(oceanStart, oceanEnd)))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .pointerInput(interactivePick) {
                if (interactivePick && onCoordinatePicked != null) {
                    detectTapGestures { tapOffset ->
                        val (lat, lon) = screenToLatLon(
                            screen = tapOffset,
                            width = size.width.toFloat(),
                            height = size.height.toFloat(),
                            zoom = 1.05f,
                            pan = Offset.Zero
                        )
                        onCoordinatePicked(lat, lon)
                    }
                }
            }
    ) {
        val density = LocalDensity.current
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArchipelagoGridAndIslands(
                width = size.width,
                height = size.height,
                zoom = 1.05f,
                pan = Offset.Zero,
                gridColor = gridColor,
                landColor = landColor,
                landStroke = landStroke
            )
        }

        val pinOffset = latLonToScreen(latitude, longitude, w, h, 1.05f, Offset.Zero)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (pinOffset.x - 16.dp.toPx()).roundToInt(),
                        y = (pinOffset.y - 30.dp.toPx()).roundToInt()
                    )
                }
                .size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = title,
                tint = SecondaryCoral,
                modifier = Modifier.size(32.dp)
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ) {
            Text(
                text = if (interactivePick) {
                    "Ketuk peta untuk geser pin (${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)})"
                } else {
                    "Koordinat: ${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}"
                },
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

private fun DrawScope.drawArchipelagoGridAndIslands(
    width: Float,
    height: Float,
    zoom: Float,
    pan: Offset,
    gridColor: Color,
    landColor: Color,
    landStroke: Color
) {
    // Draw subtle lat/lon grid
    for (lon in 95..140 step 5) {
        val top = latLonToScreen(MAX_LAT, lon.toDouble(), width, height, zoom, pan)
        val bot = latLonToScreen(MIN_LAT, lon.toDouble(), width, height, zoom, pan)
        drawLine(gridColor, top, bot, strokeWidth = 1f)
    }
    for (lat in -10..5 step 5) {
        val left = latLonToScreen(lat.toDouble(), MIN_LON, width, height, zoom, pan)
        val right = latLonToScreen(lat.toDouble(), MAX_LON, width, height, zoom, pan)
        drawLine(gridColor, left, right, strokeWidth = 1f)
    }

    // Draw Indonesian archipelago silhouettes
    allIslandPolys.forEach { poly ->
        val path = Path()
        poly.forEachIndexed { idx, (lat, lon) ->
            val pt = latLonToScreen(lat, lon, width, height, zoom, pan)
            if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        path.close()
        drawPath(path = path, color = landColor)
        drawPath(path = path, color = landStroke, style = Stroke(width = 2f))
    }
}
