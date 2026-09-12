package com.veilreader.app.ui.screens

import android.graphics.Color as AndroidColor
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.veilreader.app.data.GameRepository
import com.veilreader.app.data.LocalLibraryRepository
import com.veilreader.app.data.OpenedPublication
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.domain.ReadingPolicy
import com.veilreader.app.ui.reader.ReaderViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.adapter.pdfium.navigator.PdfiumEngineProvider
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.SelectableNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.navigator.preferences.Color as ReadiumColor
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.navigator.util.DirectionalNavigationAdapter
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

@OptIn(ExperimentalReadiumApi::class, ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ReaderScreen(
    opened: OpenedPublication,
    library: LocalLibraryRepository,
    game: GameRepository,
    onClose: () -> Unit
) {
    val activity = requireNotNull(LocalActivity.current as? FragmentActivity) {
        "Veil Reader requires a FragmentActivity host."
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val readerViewModel: ReaderViewModel = viewModel(
        key = "veil-reader-state",
        factory = remember(library, game) {
            ReaderViewModel.factory(library, game)
        }
    )
    val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
    val progress = if (readerState.bookId == opened.book.id) readerState.progress else opened.book.progress

    var topBarPx by remember { mutableIntStateOf(0) }
    var bottomBarPx by remember { mutableIntStateOf(0) }
    var navigator by remember(opened.book.id) { mutableStateOf<Navigator?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var showAppearance by remember { mutableStateOf(false) }
    var appearance by remember { mutableStateOf(library.loadAppearance()) }
    var showNotebook by remember { mutableStateOf(false) }
    val highlights by library.highlights.collectAsState()
    val bookmarks by library.bookmarks.collectAsState()
    var readerMessage by remember { mutableStateOf<String?>(null) }
    var locationTitle by remember(opened.book.id) {
        mutableStateOf(opened.book.currentChapter.takeUnless { it == "Not started" }.orEmpty())
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(opened.book.id) {
        readerViewModel.openBook(opened.book.id, opened.book.progress)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) readerViewModel.onResume()
    }

    LaunchedEffect(readerMessage) {
        val message = readerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = message,
            withDismissAction = true,
            duration = SnackbarDuration.Short
        )
        if (readerMessage == message) readerMessage = null
    }

    fun closeReader() {
        navigator?.currentLocator?.value?.let { locator ->
            val json = locator.toJSON().toString()
            readerViewModel.onLocatorChanged(
                bookId = opened.book.id,
                progression = locator.locations.totalProgression ?: readerViewModel.uiState.value.progress.toDouble(),
                locatorJson = json,
                locationKey = "${opened.book.id}:$json",
                countPageTurn = false
            )
        }
        readerViewModel.closeBook()
        onClose()
    }
    BackHandler(enabled = !showNotebook && !showAppearance) { closeReader() }

    val fragmentFactory = remember(opened.book.id) {
        createReaderFactory(opened, appearance)
    }

    DisposableEffect(lifecycle, readerViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> readerViewModel.onResume()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> readerViewModel.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            readerViewModel.onPause()
            lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(navigator, opened.book.id) {
        val nav = navigator ?: return@LaunchedEffect
        nav.currentLocator
            .debounce(500)
            .collect { locator ->
                locationTitle = locator.title?.trim().orEmpty()
                val json = locator.toJSON().toString()
                readerViewModel.onLocatorChanged(
                    bookId = opened.book.id,
                    progression = locator.locations.totalProgression
                        ?: readerViewModel.uiState.value.progress.toDouble(),
                    locatorJson = json,
                    locationKey = "${opened.book.id}:$json"
                )
            }
    }

    LaunchedEffect(navigator, opened.book.id) {
        val nav = navigator as? OverflowableNavigator ?: return@LaunchedEffect
        nav.addInputListener(
            DirectionalNavigationAdapter(
                navigator = nav,
                animatedTransition = true
            )
        )
    }

    LaunchedEffect(navigator, appearance) {
        game.pauseReading()
        readerViewModel.onUserInteraction()
        val epub = navigator as? EpubNavigatorFragment ?: return@LaunchedEffect
        epub.submitPreferences(appearance.toEpubPreferences())
    }

    LaunchedEffect(navigator, opened.book.id, highlights) {
        val decorable = navigator as? DecorableNavigator ?: return@LaunchedEffect
        val decorations = library.highlightsFor(opened.book.id).mapNotNull { item ->
            val locator = runCatching { Locator.fromJSON(JSONObject(item.locatorJson)) }.getOrNull() ?: return@mapNotNull null
            Decoration(
                id = item.id,
                locator = locator,
                style = Decoration.Style.Highlight(tint = AndroidColor.rgb(232, 201, 118))
            )
        }
        decorable.applyDecorations(decorations, HIGHLIGHT_GROUP)
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ReaderFragmentHost(
            activity = activity,
            fragmentFactory = fragmentFactory,
            fragmentClassName = when (opened.format) {
                BookFormat.EPUB -> EpubNavigatorFragment::class.java.name
                BookFormat.PDF -> PdfNavigatorFragment::class.java.name
                else -> error("Unsupported reader format")
            },
            tag = "reader-${opened.book.id}",
            onNavigatorReady = { navigator = it },
            onDisposePublication = { opened.close() },
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = with(density) { (if (controlsVisible) topBarPx else 0).toDp() },
                    bottom = with(density) { (if (controlsVisible) bottomBarPx else 0).toDp() }
                )
        )

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { topBarPx = it.height }
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReaderChromeButton("‹", "Close reader") { closeReader() }
                        Column(Modifier.weight(1f)) {
                            Text(
                                opened.book.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                locationTitle.ifBlank { opened.book.author },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                "${(progress * 100).toInt()}%",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        ReaderChromeButton("⌄", "Hide reading controls") {
                            readerViewModel.onUserInteraction()
                            controlsVisible = false
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(2.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                }
            }
        }

        if (!controlsVisible) {
            FilledTonalButton(
                onClick = {
                    readerViewModel.onUserInteraction()
                    controlsVisible = true
                },
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(10.dp)
            ) {
                Text("•••", fontSize = 14.sp, letterSpacing = 1.sp)
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { bottomBarPx = it.height }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 4.dp,
                shadowElevation = 10.dp,
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReaderControl("‹", "Previous", Modifier.weight(1f)) {
                            readerViewModel.onUserInteraction()
                            (navigator as? OverflowableNavigator)?.goBackward(animated = true)
                        }
                        ReaderControl("≡", "Contents", Modifier.weight(1f)) {
                            readerViewModel.onUserInteraction()
                            showNotebook = true
                        }
                        ReaderControl(
                            "✦",
                            "Highlight",
                            Modifier.weight(1f),
                            enabled = opened.format == BookFormat.EPUB
                        ) {
                            scope.launch {
                                val selectable = navigator as? SelectableNavigator
                                val selection = selectable?.currentSelection()
                                if (selection == null) {
                                    readerMessage = "Select some text first, then tap Highlight."
                                    return@launch
                                }
                                val quote = selection.locator.text.highlight.orEmpty().trim()
                                if (quote.isBlank()) {
                                    readerMessage = "I couldn't capture that selection. Try selecting the text again."
                                    return@launch
                                }
                                try {
                                    val countBefore = library.highlightsFor(opened.book.id).size
                                    library.addHighlight(
                                        bookId = opened.book.id,
                                        quote = quote,
                                        locatorJson = selection.locator.toJSON().toString()
                                    )
                                    val isNew = library.highlightsFor(opened.book.id).size > countBefore
                                    if (isNew) {
                                        library.flushWrites()
                                        readerViewModel.onHighlightAdded()
                                    } else {
                                        readerViewModel.onUserInteraction()
                                    }
                                    selectable.clearSelection()
                                    readerMessage = if (isNew) "Highlight saved" else "This passage is already highlighted"
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (error: Exception) {
                                    readerMessage = error.message ?: "Highlight could not be saved."
                                }
                            }
                        }
                        ReaderControl("◇", "Bookmark", Modifier.weight(1f), enabled = navigator != null) {
                            readerViewModel.onUserInteraction()
                            val locator = navigator?.currentLocator?.value
                            if (locator != null) {
                                val added = library.addBookmark(
                                    opened.book.id,
                                    "${(progress * 100).toInt()}% · ${locator.title ?: opened.book.title}",
                                    locator.toJSON().toString()
                                )
                                readerMessage = if (added) "Bookmark saved" else "This location is already bookmarked"
                            }
                        }
                        ReaderControl(
                            "Aa",
                            "Appearance",
                            Modifier.weight(1f),
                            enabled = opened.format == BookFormat.EPUB
                        ) {
                            readerViewModel.onUserInteraction()
                            showAppearance = true
                        }
                        ReaderControl("›", "Next", Modifier.weight(1f)) {
                            readerViewModel.onUserInteraction()
                            (navigator as? OverflowableNavigator)?.goForward(animated = true)
                        }
                    }
                    Text(
                        if (opened.format == BookFormat.EPUB) {
                            "Tap the page to turn · select text to highlight"
                        } else {
                            "Swipe or use the arrows to move through the document"
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 1.dp, bottom = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 18.dp)
                .padding(
                    bottom = with(density) {
                        (if (controlsVisible) bottomBarPx else 0).toDp()
                    } + 12.dp
                )
        )
    }

    if (showNotebook) {
        ReaderNotebook(
            opened = opened,
            highlights = highlights.filter { it.bookId == opened.book.id },
            bookmarks = bookmarks.filter { it.bookId == opened.book.id },
            onDismiss = { showNotebook = false },
            onGo = { json ->
                readerViewModel.onUserInteraction()
                game.pauseReading()
                val locator = runCatching { Locator.fromJSON(JSONObject(json)) }.getOrNull()
                if (locator != null && navigator?.go(locator, animated = true) == true) showNotebook = false
                else {
                    showNotebook = false
                    readerMessage = "That saved location could not be opened."
                }
            },
            onChapter = { link ->
                readerViewModel.onUserInteraction()
                game.pauseReading()
                if (navigator?.go(link, animated = true) == true) showNotebook = false
                else {
                    showNotebook = false
                    readerMessage = "This chapter could not be opened."
                }
            },
            onSaveNote = { id, note ->
                library.updateHighlightNote(id, note)
                library.flushWrites()
                readerViewModel.onNoteSaved(id, note)
                readerMessage = "Note saved"
            },
            onDeleteHighlight = library::deleteHighlight,
            onDeleteBookmark = library::deleteBookmark
        )
    }

    if (showAppearance) {
        ModalBottomSheet(onDismissRequest = { showAppearance = false }) {
            AppearancePanel(
                appearance = appearance,
                onChange = {
                    readerViewModel.onUserInteraction()
                    appearance = it
                    library.saveAppearance(it)
                },
                onDone = { showAppearance = false }
            )
        }
    }
}

@OptIn(ExperimentalReadiumApi::class)
private fun createReaderFactory(
    opened: OpenedPublication,
    appearance: ReaderAppearance
): FragmentFactory = when (opened.format) {
    BookFormat.EPUB -> EpubNavigatorFactory(opened.publication)
        .createFragmentFactory(
            initialLocator = opened.initialLocator,
            initialPreferences = appearance.toEpubPreferences(),
            configuration = EpubNavigatorFragment.Configuration {
                disablePageTurnsWhileScrolling = false
            }
        )

    BookFormat.PDF -> PdfNavigatorFactory(
        publication = opened.publication,
        pdfEngineProvider = PdfiumEngineProvider()
    ).createFragmentFactory(initialLocator = opened.initialLocator)

    else -> error("Unsupported reader format")
}

@Composable
private fun ReaderFragmentHost(
    activity: FragmentActivity,
    fragmentFactory: FragmentFactory,
    fragmentClassName: String,
    tag: String,
    onNavigatorReady: (Navigator) -> Unit,
    onDisposePublication: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerId = remember(tag) { View.generateViewId() }
    AndroidView(
        factory = { context ->
            FragmentContainerView(context).apply { id = containerId }
        },
        modifier = modifier
    )

    DisposableEffect(activity, tag, containerId) {
        val fm = activity.supportFragmentManager
        val previousFactory = fm.fragmentFactory
        val existing = fm.findFragmentByTag(tag)
        val fragment: Fragment = existing ?: run {
            fm.fragmentFactory = fragmentFactory
            val created = fragmentFactory.instantiate(activity.classLoader, fragmentClassName)
            fm.beginTransaction().replace(containerId, created, tag).commitNow()
            created
        }
        fm.fragmentFactory = previousFactory
        (fragment as? Navigator)?.let(onNavigatorReady)

        onDispose {
            runCatching {
                if (fragment.isAdded) {
                    fm.beginTransaction().remove(fragment).commitNowAllowingStateLoss()
                }
            }
            onDisposePublication()
        }
    }
}

@Composable
private fun ReaderChromeButton(
    glyph: String,
    accessibilityLabel: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.defaultMinSize(minWidth = 42.dp, minHeight = 42.dp)
    ) {
        Text(glyph, fontSize = 24.sp, lineHeight = 24.sp)
    }
}

@Composable
private fun ReaderControl(
    glyph: String,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minWidth = 0.dp, minHeight = 48.dp),
        contentPadding = PaddingValues(horizontal = 1.dp, vertical = 3.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(glyph, fontSize = 17.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold)
            Text(
                label,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AppearancePanel(
    appearance: ReaderAppearance,
    onChange: (ReaderAppearance) -> Unit,
    onDone: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Page & light", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Shape the page for this moment. Your choice stays local and follows you between books.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text("Quick atmosphere", fontWeight = FontWeight.SemiBold)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = {
                    onChange(
                        appearance.copy(
                            theme = ReaderTheme.PAPER,
                            fontScale = 1.0,
                            lineHeight = 1.45,
                            pageMargins = 1.0,
                            scroll = false,
                            publisherStyles = true
                        )
                    )
                },
                label = { Text("Book") }
            )
            AssistChip(
                onClick = {
                    onChange(
                        appearance.copy(
                            theme = ReaderTheme.SEPIA,
                            fontScale = 1.08,
                            lineHeight = 1.6,
                            pageMargins = 1.15,
                            scroll = false,
                            publisherStyles = false
                        )
                    )
                },
                label = { Text("Comfort") }
            )
            AssistChip(
                onClick = {
                    onChange(
                        appearance.copy(
                            theme = ReaderTheme.DUSK,
                            fontScale = 1.05,
                            lineHeight = 1.55,
                            pageMargins = 1.1,
                            publisherStyles = false
                        )
                    )
                },
                label = { Text("Night") }
            )
            AssistChip(
                onClick = {
                    onChange(
                        appearance.copy(
                            theme = ReaderTheme.OLED,
                            fontScale = 1.05,
                            lineHeight = 1.55,
                            pageMargins = 1.1,
                            publisherStyles = false
                        )
                    )
                },
                label = { Text("OLED") }
            )
        }

        Text("Theme", fontWeight = FontWeight.SemiBold)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReaderTheme.entries.forEach { theme ->
                FilterChip(
                    selected = appearance.theme == theme,
                    onClick = { onChange(appearance.copy(theme = theme)) },
                    label = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Text("Text size · ${(appearance.fontScale * 100).toInt()}%", fontWeight = FontWeight.SemiBold)
        Slider(
            value = appearance.fontScale.toFloat(),
            onValueChange = { onChange(appearance.copy(fontScale = it.toDouble(), publisherStyles = false)) },
            valueRange = .75f..1.8f
        )

        Text("Line height · ${"%.2f".format(appearance.lineHeight)}", fontWeight = FontWeight.SemiBold)
        Slider(
            value = appearance.lineHeight.toFloat(),
            onValueChange = { onChange(appearance.copy(lineHeight = it.toDouble(), publisherStyles = false)) },
            valueRange = 1.1f..2.0f
        )

        Text("Page margins · ${"%.2f".format(appearance.pageMargins)}", fontWeight = FontWeight.SemiBold)
        Slider(
            value = appearance.pageMargins.toFloat(),
            onValueChange = { onChange(appearance.copy(pageMargins = it.toDouble(), publisherStyles = false)) },
            valueRange = .5f..2.0f
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Continuous scroll", fontWeight = FontWeight.SemiBold)
                Text(
                    "Off uses paginated reading with animated page turns.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = appearance.scroll, onCheckedChange = { onChange(appearance.copy(scroll = it)) })
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Publisher styling", fontWeight = FontWeight.SemiBold)
                Text(
                    "Keep the book's original typography when possible.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = appearance.publisherStyles,
                onCheckedChange = { onChange(appearance.copy(publisherStyles = it)) }
            )
        }

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Return to reading") }
    }
}

@OptIn(ExperimentalReadiumApi::class)
private fun ReaderAppearance.toEpubPreferences(): EpubPreferences = EpubPreferences(
    theme = when (theme) {
        ReaderTheme.PAPER -> Theme.LIGHT
        ReaderTheme.SEPIA -> Theme.SEPIA
        ReaderTheme.DUSK, ReaderTheme.OLED -> Theme.DARK
    },
    backgroundColor = when (theme) {
        ReaderTheme.OLED -> ReadiumColor(AndroidColor.BLACK)
        ReaderTheme.DUSK -> ReadiumColor(AndroidColor.rgb(24, 21, 29))
        else -> null
    },
    fontSize = ReadingPolicy.fontSizePercent(fontScale),
    lineHeight = lineHeight,
    pageMargins = pageMargins,
    scroll = scroll,
    publisherStyles = publisherStyles
)

private const val HIGHLIGHT_GROUP = "veil-highlights"
