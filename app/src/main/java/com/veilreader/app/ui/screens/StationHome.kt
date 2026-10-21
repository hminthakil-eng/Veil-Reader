package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.domain.Book
import com.veilreader.app.ui.navigation.LibraryShelfDestination
import com.veilreader.app.ui.theme.LocalVeilHighContrast
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.withVeilContentScript

/** Artwork is only scenery. All labels and interactions are native, scalable controls. */
@Composable
internal fun StationDoorway(bookCount: Int, hasCurrentBook: Boolean) {
    val contrast = LocalVeilHighContrast.current
    val scale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth().background(VeilPalette.Ink)) {
        val doorwayWidth = maxWidth
        val artworkHeight = if (hasCurrentBook && scale >= 1.5f) 72.dp
            else (doorwayWidth / 2).coerceIn(128.dp, 188.dp)
        val signBrand = scale <= 1.05f && !contrast
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth().height(artworkHeight)) {
                Image(painterResource(R.drawable.station_home_v1), contentDescription = null,
                    contentScale = ContentScale.Crop, alignment = Alignment.TopCenter,
                    alpha = if (contrast) .25f else 1f, modifier = Modifier.matchParentSize())
                if (signBrand) {
                    Box(Modifier.align(Alignment.TopCenter).offset(y = artworkHeight * .13f)
                        .width(doorwayWidth * .39f).height(artworkHeight * .16f), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.app_name), color = VeilPalette.Brass,
                            style = MaterialTheme.typography.titleLarge, fontSize = 18.sp,
                            lineHeight = 22.sp, maxLines = 1, textAlign = TextAlign.Center)
                    }
                }
            }
            if (!signBrand) {
                Text(stringResource(R.string.app_name), color = VeilPalette.Brass,
                    style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp))
            }
            Text(stringResource(when {
                bookCount == 0 -> R.string.threshold_title_unwritten
                bookCount == 1 -> R.string.threshold_title_first_volume
                hasCurrentBook -> R.string.threshold_title_library_awaits
                else -> R.string.threshold_title_return_archive
            }), style = if (hasCurrentBook) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.headlineSmall,
                color = VeilPalette.Moon, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp).semantics { heading() })
            if (!hasCurrentBook) {
                Text(stringResource(when {
                    bookCount == 0 -> R.string.threshold_body_unwritten
                    bookCount == 1 -> R.string.threshold_body_first_volume
                    else -> R.string.threshold_body_return_archive
                }), style = MaterialTheme.typography.bodyMedium, color = VeilPalette.Moon,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
            }
            BrassRule(Modifier.fillMaxWidth().padding(horizontal = 12.dp), strong = true)
        }
    }
}

@Composable
internal fun StationOrnamentFrame(modifier: Modifier = Modifier) {
    val contrast = LocalVeilHighContrast.current
    Box(modifier.border(1.dp, VeilPalette.Brass.copy(alpha = if (contrast) 1f else .75f), RoundedCornerShape(4.dp))) {
        if (!contrast) {
            val corner = painterResource(R.drawable.station_brass_corner_v1)
            // Metal has physical edges; it must not swap its rotations with RTL layout.
            listOf(AbsoluteAlignment.TopLeft to 0f, AbsoluteAlignment.TopRight to 90f,
                AbsoluteAlignment.BottomRight to 180f, AbsoluteAlignment.BottomLeft to 270f).forEach { (alignment, angle) ->
                Image(corner, contentDescription = null, modifier = Modifier.align(alignment).size(36.dp).rotate(angle))
            }
        }
    }
}

@Composable
internal fun StationLibraryPanel(
    books: List<Book>, hasCurrentBook: Boolean, onOpenLibrary: () -> Unit,
    onOpenShelf: (LibraryShelfDestination) -> Unit
) {
    val counts = remember(books) { LibraryShelfDestination.entries.associateWith { shelf -> books.count(shelf::contains) } }
    // Decode the atlas once, then paint four source rectangles without copying bitmaps.
    val atlas = ImageBitmap.imageResource(R.drawable.station_library_portals_v1)
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val contrast = LocalVeilHighContrast.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StationSectionHeading(stringResource(R.string.home_library_title))
        if (hasCurrentBook) {
            OutlinedButton(onClick = onOpenLibrary,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, VeilPalette.Brass),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VeilPalette.Moon)) {
                Text(stringResource(R.string.threshold_enter_library), textAlign = TextAlign.Center)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (maxWidth.value / scale >= 360f) 4 else 2
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LibraryShelfDestination.entries.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { destination ->
                            val index = destination.ordinal
                            val painter = remember(atlas, index) {
                                BitmapPainter(atlas, srcOffset = IntOffset(index * atlas.width / 4, 0),
                                    srcSize = IntSize(atlas.width / 4, atlas.height))
                            }
                            val label = stringResource(when (destination) {
                                LibraryShelfDestination.ALL -> R.string.library_shelf_all
                                LibraryShelfDestination.READING -> R.string.library_shelf_reading
                                LibraryShelfDestination.COMPLETED -> R.string.library_shelf_completed
                                LibraryShelfDestination.FAVORITES -> R.string.library_shelf_favorites
                            })
                            val count = counts.getValue(destination)
                            Surface(onClick = { onOpenShelf(destination) },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                shape = RoundedCornerShape(4.dp), color = VeilPalette.Archive,
                                border = BorderStroke(1.dp, VeilPalette.Brass), tonalElevation = 0.dp) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Image(painter, contentDescription = null, contentScale = ContentScale.Crop,
                                        alignment = Alignment.TopCenter, alpha = if (contrast) .3f else 1f,
                                        modifier = Modifier.fillMaxWidth().aspectRatio(1.05f).clip(RoundedCornerShape(4.dp)))
                                    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(label, style = MaterialTheme.typography.labelLarge.withVeilContentScript(label),
                                            color = VeilPalette.Moon, textAlign = TextAlign.Center)
                                        Text(pluralStringResource(R.plurals.library_series_books, count, count),
                                            style = MaterialTheme.typography.labelMedium, color = VeilPalette.Moon,
                                            textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun StationSectionHeading(title: String, trailing: (@Composable () -> Unit)? = null) {
    Box(Modifier.fillMaxWidth().background(VeilPalette.Archive)) {
        StationOrnamentFrame(Modifier.matchParentSize())
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, Modifier.weight(1f).padding(vertical = 6.dp).semantics { heading() },
                style = MaterialTheme.typography.titleMedium.withVeilContentScript(title), color = VeilPalette.Moon)
            trailing?.invoke()
        }
    }
}
