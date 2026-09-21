package com.veilreader.app.ui.screens

import android.graphics.Color as AndroidColor
import android.view.ActionMode
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.github.barteksc.pdfviewer.PDFView
import com.veilreader.app.data.GameRepository
import com.veilreader.app.data.LocalLibraryRepository
import com.veilreader.app.data.OpenedPublication
import com.veilreader.app.data.toVeilPersistedJson
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.ui.reader.ReaderViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.adapter.pdfium.navigator.PdfiumEngineProvider
import org.readium.adapter.pdfium.navigator.PdfiumDefaults
import org.readium.adapter.pdfium.navigator.PdfiumNavigatorFragment
import org.readium.adapter.pdfium.navigator.PdfiumPreferences
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.SelectableNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Color as ReadiumColor
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

@OptIn(ExperimentalReadiumApi::class, ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ReaderScreen(
    opened: OpenedPublication,
    library: LocalLibraryRepository,
    game: GameRepository,
    readerAppearance: ReaderAppearance,
    onReaderAppearanceChange: (ReaderAppearance) -> Unit,
    onClose: () -> Unit
) {
    val activity = requireNotNull(LocalActivity.current as? FragmentActivity) {
        "Veil Reader requires a FragmentActivity host."
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val readerViewModel: ReaderViewModel = viewModel(
        key = "veil-reader-state",
        factory = remember(library, game) { ReaderViewModel.factory(library, game) }
    )
    val progressFlow = remember(readerViewModel, opened.book.id, opened.book.progress) {
        readerViewModel.uiState
            .map { state ->
                if (state.bookId == opened.book.id) state.progress else opened.book.progress
            }
            .distinctUntilChanged()
    }
    val progress by progressFlow.collectAsStateWithLifecycle(initialValue = opened.book.progress)

    var navigator by remember(opened.book.id) { mutableStateOf<Navigator?>(null) }
    var controlsVisible by remember(opened.book.id) { mutableStateOf(false) }
    val paperCurlState = remember(opened.book.id) { PaperCurlState() }
    var showAppearance by remember { mutableStateOf(false) }
    var showPdfZoom by remember { mutableStateOf(false) }
    var appearance by remember(opened.book.id) { mutableStateOf(readerAppearance) }
    val latestAppearance = rememberUpdatedState(appearance)
    val paperCurlConfig = remember(appearance.theme) {
        when (appearance.theme) {
            ReaderTheme.PAPER -> PaperCurlVisualConfig(
                backPageColor = Color(0xFFF2E8D8),
                backPageContentAlpha = 0.13f
            )
            ReaderTheme.SEPIA -> PaperCurlVisualConfig(
                backPageColor = Color(0xFFE7D2AA),
                backPageContentAlpha = 0.15f
            )
            ReaderTheme.DUSK -> PaperCurlVisualConfig(
                backPageColor = Color(0xFF27222C),
                backPageContentAlpha = 0.09f,
                edgeHighlight = Color(0xFFE8DFF0)
            )
            ReaderTheme.OLED -> PaperCurlVisualConfig(
                backPageColor = Color(0xFF111111),
                backPageContentAlpha = 0.07f,
                edgeHighlight = Color(0xFFD8D8D8)
            )
        }
    }
    var showNotebook by remember { mutableStateOf(false) }

    LaunchedEffect(readerAppearance, opened.book.id) {
        appearance = readerAppearance
    }
    val bookHighlightsFlow = remember(library, opened.book.id) {
        library.highlights
            .map { items -> items.filter { it.bookId == opened.book.id } }
            .distinctUntilChanged()
    }
    val bookBookmarksFlow = remember(library, opened.book.id) {
        library.bookmarks
            .map { items -> items.filter { it.bookId == opened.book.id } }
            .distinctUntilChanged()
    }
    val bookHighlights by bookHighlightsFlow.collectAsStateWithLifecycle(
        initialValue = library.highlightsFor(opened.book.id)
    )
    val bookBookmarks by bookBookmarksFlow.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )
    var readerMessage by remember { mutableStateOf<String?>(null) }
    var pendingNoteHighlightId by remember { mutableStateOf<String?>(null) }
    var pendingNoteText by remember { mutableStateOf("") }
    var noteSaving by remember { mutableStateOf(false) }
    var locationTitle by remember(opened.book.id) {
        mutableStateOf(opened.book.currentChapter.takeUnless { it == "Not started" }.orEmpty())
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarBottom by animateDpAsState(
        targetValue = if (controlsVisible) 104.dp else 16.dp,
        animationSpec = tween(220),
        label = "reader-snackbar-offset"
    )

    val selectionActionModeCallback = remember(opened.book.id, library, readerViewModel, scope) {
        ReaderSelectionActionModeCallback(
            coroutineScope = scope,
            navigatorProvider = { navigator as? SelectableNavigator },
            onAction = { action, locator, quote ->
                try {
                    val locatorJson = locator.toVeilPersistedJson(opened.format)
                    val existing = library.highlightsFor(opened.book.id).firstOrNull {
                        it.locatorJson == locatorJson && it.quote == quote
                    }
                    val highlight = existing ?: library.addHighlight(
                        bookId = opened.book.id,
                        quote = quote,
                        locatorJson = locatorJson
                    )
                    val isNew = existing == null
                    if (isNew) {
                        library.flushWrites()
                        readerViewModel.onHighlightAdded()
                    } else {
                        readerViewModel.onUserInteraction()
                    }

                    when (action) {
                        ReaderSelectionAction.HIGHLIGHT -> {
                            readerMessage = if (isNew) "Highlighted" else "Already highlighted"
                        }
                        ReaderSelectionAction.NOTE -> {
                            pendingNoteHighlightId = highlight.id
                            pendingNoteText = highlight.note
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    readerMessage = error.message ?: "That passage could not be saved."
                }
            }
        )
    }

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
            val json = locator.toVeilPersistedJson(opened.format)
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

    BackHandler(
        enabled = !showNotebook && !showAppearance && !showPdfZoom && !paperCurlState.active
    ) { closeReader() }

    val fragmentFactory = remember(opened.book.id, selectionActionModeCallback) {
        createReaderFactory(opened, appearance, selectionActionModeCallback)
    }
    val onNavigatorReady = remember<(Navigator) -> Unit>(opened.book.id) {
        { ready -> navigator = ready }
    }
    val onDisposePublication = remember(opened.book.id) {
        { opened.close() }
    }

    DisposableEffect(paperCurlState) {
        onDispose { paperCurlState.dispose() }
    }

    DisposableEffect(lifecycle, readerViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> readerViewModel.onResume()
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> readerViewModel.onPause()
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

                val paperPreviewActive =
                    opened.format == BookFormat.EPUB &&
                        latestAppearance.value.pageTurnStyle == PageTurnStyle.PAPER &&
                        paperCurlState.active
                if (paperPreviewActive) return@collect

                val json = locator.toVeilPersistedJson(opened.format)
                readerViewModel.onLocatorChanged(
                    bookId = opened.book.id,
                    progression = locator.locations.totalProgression
                        ?: readerViewModel.uiState.value.progress.toDouble(),
                    locatorJson = json,
                    locationKey = "${opened.book.id}:$json",
                    countPageTurn = opened.format != BookFormat.EPUB ||
                        latestAppearance.value.pageTurnStyle == PageTurnStyle.SLIDE
                )
            }
    }

    DisposableEffect(navigator, opened.book.id) {
        val nav = navigator as? OverflowableNavigator
        if (nav == null) {
            onDispose { }
        } else {
            val listeners = mutableListOf<InputListener>()

            if (navigator is EpubNavigatorFragment) {
                listeners += PaperCurlInputListener(
                    navigator = nav,
                    state = paperCurlState,
                    isEnabled = {
                        !latestAppearance.value.scroll &&
                            latestAppearance.value.pageTurnStyle == PageTurnStyle.PAPER
                    },
                    scope = scope,
                    onInteraction = {
                        readerViewModel.onUserInteraction()
                        controlsVisible = false
                    },
                    onCommittedTurn = {
                        val locator = nav.currentLocator.value
                        val json = locator.toVeilPersistedJson(opened.format)
                        readerViewModel.onLocatorChanged(
                            bookId = opened.book.id,
                            progression = locator.locations.totalProgression
                                ?: readerViewModel.uiState.value.progress.toDouble(),
                            locatorJson = json,
                            locationKey = "${opened.book.id}:$json"
                        )
                    }
                )
            }

            listeners += VeilDirectionalNavigationInputListener(
                navigator = nav,
                isAnimated = {
                    latestAppearance.value.pageTurnStyle == PageTurnStyle.SLIDE
                }
            )

            listeners += object : InputListener {
                override fun onTap(event: TapEvent): Boolean {
                    readerViewModel.onUserInteraction()
                    controlsVisible = !controlsVisible
                    return true
                }
            }

            listeners.forEach(nav::addInputListener)
            onDispose {
                listeners.forEach(nav::removeInputListener)
            }
        }
    }

    LaunchedEffect(navigator, appearance, opened.format) {
        game.pauseReading()
        readerViewModel.onUserInteraction()
        when (opened.format) {
            BookFormat.EPUB ->
                (navigator as? EpubNavigatorFragment)
                    ?.submitPreferences(appearance.toEpubPreferences())

            BookFormat.PDF -> {
                @Suppress("UNCHECKED_CAST")
                val pdfNavigator = navigator as? PdfiumNavigatorFragment
                pdfNavigator?.submitPreferences(appearance.toPdfiumPreferences())
            }

            else -> Unit
        }
    }

    LaunchedEffect(navigator, opened.book.id, bookHighlights) {
        val decorable = navigator as? DecorableNavigator ?: return@LaunchedEffect
        val decorations = bookHighlights.mapNotNull { item ->
            val locator = runCatching { Locator.fromJSON(JSONObject(item.locatorJson)) }.getOrNull()
                ?: return@mapNotNull null
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
            onNavigatorReady = onNavigatorReady,
            onDisposePublication = onDisposePublication,
            modifier = Modifier.fillMaxSize()
        )

        if (opened.format == BookFormat.EPUB) {
            PaperCurlOverlay(
                state = paperCurlState,
                config = paperCurlConfig,
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(tween(170)) + slideInVertically(tween(220)) { -it / 2 },
            exit = fadeOut(tween(120)) + slideOutVertically(tween(180)) { -it / 2 }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 1.dp,
                shadowElevation = 10.dp
            ) {
                Column(
                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReaderChromeButton(ReaderAction.BACK, "Close reader") { closeReader() }
                        Column(Modifier.weight(1f)) {
                            Text(
                                opened.book.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                locationTitle.ifBlank { opened.book.author.ifBlank { opened.format.name } },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            modifier = Modifier.semantics {
                                contentDescription = "${(progress.coerceIn(0f, 1f) * 100).toInt()} percent read"
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.74f)
                        ) {
                            Text(
                                "${(progress * 100).toInt()}%",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(170)) + slideInVertically(tween(220)) { it / 2 },
            exit = fadeOut(tween(120)) + slideOutVertically(tween(180)) { it / 2 }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 2.dp,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReaderControl(
                        action = ReaderAction.NOTEBOOK,
                        label = "Notebook",
                        modifier = Modifier.weight(1f)
                    ) {
                        readerViewModel.onUserInteraction()
                        showNotebook = true
                    }
                    ReaderControl(
                        action = ReaderAction.BOOKMARK,
                        label = "Bookmark",
                        modifier = Modifier.weight(1f),
                        enabled = navigator != null
                    ) {
                        readerViewModel.onUserInteraction()
                        val locator = navigator?.currentLocator?.value
                        if (locator != null) {
                            val added = library.addBookmark(
                                opened.book.id,
                                "${(progress * 100).toInt()}% · ${locator.title ?: opened.book.title}",
                                locator.toVeilPersistedJson(opened.format)
                            )
                            readerMessage = if (added) "Bookmark saved" else "This location is already bookmarked"
                        }
                    }
                    ReaderControl(
                        action = if (opened.format == BookFormat.EPUB) {
                            ReaderAction.APPEARANCE
                        } else {
                            ReaderAction.ZOOM
                        },
                        label = if (opened.format == BookFormat.EPUB) "Settings" else "Zoom",
                        modifier = Modifier.weight(1f),
                        enabled = navigator != null
                    ) {
                        readerViewModel.onUserInteraction()
                        if (opened.format == BookFormat.EPUB) {
                            showAppearance = true
                        } else {
                            showPdfZoom = true
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 18.dp)
                .padding(bottom = snackbarBottom)
        )
    }

    pendingNoteHighlightId?.let { highlightId ->
        AlertDialog(
            onDismissRequest = {
                if (!noteSaving) {
                    pendingNoteHighlightId = null
                    pendingNoteText = ""
                }
            },
            title = { Text("Note on this passage") },
            text = {
                OutlinedTextField(
                    value = pendingNoteText,
                    onValueChange = { pendingNoteText = it },
                    enabled = !noteSaving,
                    label = { Text("Your note") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !noteSaving,
                    onClick = {
                        scope.launch {
                            noteSaving = true
                            try {
                                library.updateHighlightNote(highlightId, pendingNoteText)
                                library.flushWrites()
                                readerViewModel.onNoteSaved(highlightId, pendingNoteText)
                                pendingNoteHighlightId = null
                                pendingNoteText = ""
                                readerMessage = "Note saved"
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                readerMessage = error.message ?: "Note could not be saved."
                            } finally {
                                noteSaving = false
                            }
                        }
                    }
                ) { Text(if (noteSaving) "Saving…" else "Save") }
            },
            dismissButton = {
                TextButton(
                    enabled = !noteSaving,
                    onClick = {
                        pendingNoteHighlightId = null
                        pendingNoteText = ""
                    }
                ) { Text("Cancel") }
            }
        )
    }

    if (showNotebook) {
        ReaderNotebook(
            opened = opened,
            highlights = bookHighlights,
            bookmarks = bookBookmarks,
            onDismiss = { showNotebook = false },
            onGo = { json ->
                readerViewModel.onUserInteraction()
                game.pauseReading()
                val locator = runCatching { Locator.fromJSON(JSONObject(json)) }.getOrNull()
                if (locator != null && navigator?.go(locator, animated = true) == true) {
                    showNotebook = false
                } else {
                    showNotebook = false
                    readerMessage = "That saved location could not be opened."
                }
            },
            onChapter = { link ->
                readerViewModel.onUserInteraction()
                game.pauseReading()
                if (navigator?.go(link, animated = true) == true) {
                    showNotebook = false
                } else {
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
                format = opened.format,
                appearance = appearance,
                onChange = {
                    readerViewModel.onUserInteraction()
                    appearance = it
                    onReaderAppearanceChange(it)
                },
                onDone = { showAppearance = false }
            )
        }
    }

    if (showPdfZoom) {
        ModalBottomSheet(onDismissRequest = { showPdfZoom = false }) {
            PdfZoomControls(
                navigator = navigator,
                appearance = appearance,
                onAppearanceChange = {
                    readerViewModel.onUserInteraction()
                    appearance = it
                    onReaderAppearanceChange(it)
                },
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 32.dp),
                onDone = { showPdfZoom = false }
            )
        }
    }
}

@OptIn(ExperimentalReadiumApi::class, DelicateReadiumApi::class)
private fun createReaderFactory(
    opened: OpenedPublication,
    appearance: ReaderAppearance,
    selectionActionModeCallback: ActionMode.Callback
): FragmentFactory = when (opened.format) {
    BookFormat.EPUB -> EpubNavigatorFactory(opened.publication)
        .createFragmentFactory(
            initialLocator = opened.initialLocator,
            initialPreferences = appearance.toEpubPreferences(),
            configuration = EpubNavigatorFragment.Configuration {
                useReadiumCssFontSize = false
                disablePageTurnsWhileScrolling = false
                this.selectionActionModeCallback = selectionActionModeCallback
                decorationTemplates = HtmlDecorationTemplates.defaultTemplates(
                    alpha = 1.0,
                    experimentalPositioning = true
                )
            }
        )

    BookFormat.PDF -> PdfNavigatorFactory(
        publication = opened.publication,
        pdfEngineProvider = PdfiumEngineProvider(
            defaults = PdfiumDefaults(),
            listener = object : PdfiumEngineProvider.Listener {
                override fun onConfigurePdfView(configurator: PDFView.Configurator) {
                    // Lock the gesture contract instead of relying on AndroidPdfViewer defaults.
                    configurator.enableSwipe(true)
                    configurator.enableDoubletap(true)
                    configurator.enableAntialiasing(true)
                }
            }
        )
    ).createFragmentFactory(
        initialLocator = opened.initialLocator,
        initialPreferences = appearance.toPdfiumPreferences()
    )

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
        factory = { context -> FragmentContainerView(context).apply { id = containerId } },
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

private enum class ReaderAction { BACK, NOTEBOOK, BOOKMARK, APPEARANCE, ZOOM }

@Composable
private fun ReaderChromeButton(
    action: ReaderAction,
    accessibilityLabel: String,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = accessibilityLabel }
    ) {
        ReaderActionIcon(action, Modifier.size(22.dp), MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun ReaderControl(
    action: ReaderAction,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minWidth = 0.dp, minHeight = 58.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ReaderActionIcon(
                action = action,
                modifier = Modifier.size(21.dp),
                tint = if (enabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReaderActionIcon(action: ReaderAction, modifier: Modifier, tint: Color) {
    if (action == ReaderAction.APPEARANCE) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Aa", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = tint)
        }
        return
    }

    Canvas(modifier) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        when (action) {
            ReaderAction.BACK -> {
                drawLine(tint, Offset(w * .72f, h * .20f), Offset(w * .34f, h * .50f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * .34f, h * .50f), Offset(w * .72f, h * .80f), stroke.width, StrokeCap.Round)
            }
            ReaderAction.NOTEBOOK -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * .18f, h * .14f),
                    size = Size(w * .64f, h * .72f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = stroke
                )
                drawLine(tint, Offset(w * .34f, h * .34f), Offset(w * .68f, h * .34f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * .34f, h * .50f), Offset(w * .68f, h * .50f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * .34f, h * .66f), Offset(w * .58f, h * .66f), stroke.width, StrokeCap.Round)
            }
            ReaderAction.BOOKMARK -> {
                val path = Path().apply {
                    moveTo(w * .28f, h * .12f)
                    lineTo(w * .72f, h * .12f)
                    lineTo(w * .72f, h * .86f)
                    lineTo(w * .50f, h * .69f)
                    lineTo(w * .28f, h * .86f)
                    close()
                }
                drawPath(path, tint, style = stroke)
            }
            ReaderAction.APPEARANCE -> Unit
            ReaderAction.ZOOM -> {
                drawCircle(
                    color = tint,
                    radius = w * .22f,
                    center = Offset(w * .43f, h * .40f),
                    style = stroke
                )
                drawLine(
                    tint,
                    Offset(w * .58f, h * .56f),
                    Offset(w * .80f, h * .80f),
                    stroke.width,
                    StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun AppearancePanel(
    format: BookFormat,
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
            Text(
                if (format == BookFormat.PDF) "PDF reading controls" else "Reader settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (format == BookFormat.PDF) {
                    "Choose paginated or continuous reading. Pinch or double-tap the document to zoom."
                } else {
                    "Tune the page once, then get back to the book. These choices stay on your device."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (format == BookFormat.EPUB) {
            Text("Presets", fontWeight = FontWeight.SemiBold)
            Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppearancePreset("Book", appearance.theme == ReaderTheme.PAPER) {
                onChange(
                    appearance.withTheme(ReaderTheme.PAPER).copy(
                        fontScale = 1.0,
                        lineHeight = 1.45,
                        pageMargins = 1.0,
                        scroll = false
                    )
                )
            }
            AppearancePreset("Comfort", appearance.theme == ReaderTheme.SEPIA) {
                onChange(
                    appearance.withTheme(ReaderTheme.SEPIA).copy(
                        fontScale = 1.08,
                        lineHeight = 1.6,
                        pageMargins = 1.15,
                        scroll = false
                    )
                )
            }
            AppearancePreset("Night", appearance.theme == ReaderTheme.DUSK) {
                onChange(
                    appearance.withTheme(ReaderTheme.DUSK).copy(
                        fontScale = 1.05,
                        lineHeight = 1.55,
                        pageMargins = 1.1
                    )
                )
            }
            AppearancePreset("OLED", appearance.theme == ReaderTheme.OLED) {
                onChange(
                    appearance.withTheme(ReaderTheme.OLED).copy(
                        fontScale = 1.05,
                        lineHeight = 1.55,
                        pageMargins = 1.1
                    )
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
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Continuous scroll", fontWeight = FontWeight.SemiBold)
                Text(
                    if (format == BookFormat.PDF) {
                        "Turn this off for horizontal page snapping. Zoom stays available in both modes."
                    } else {
                        "Turn this off for paginated reading without the slide-like transition."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = appearance.scroll,
                onCheckedChange = { onChange(appearance.copy(scroll = it)) },
                modifier = Modifier.semantics { contentDescription = "Continuous scroll" }
            )
        }

        if (format == BookFormat.EPUB) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Page turn", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = appearance.pageTurnStyle == PageTurnStyle.PAPER,
                        onClick = { onChange(appearance.copy(pageTurnStyle = PageTurnStyle.PAPER)) },
                        enabled = !appearance.scroll,
                        label = { Text("Paper curl") },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    )
                    FilterChip(
                        selected = appearance.pageTurnStyle == PageTurnStyle.SLIDE,
                        onClick = { onChange(appearance.copy(pageTurnStyle = PageTurnStyle.SLIDE)) },
                        enabled = !appearance.scroll,
                        label = { Text("Simple slide") },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    )
                }
                Text(
                    if (appearance.scroll) {
                        "Page-turn effects are paused while continuous scroll is on."
                    } else {
                        "Paper curl follows your drag; Simple slide keeps Readium's native animated fallback."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Publisher styling", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Keep the book's original typography and layout when possible.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = appearance.publisherStyles,
                    onCheckedChange = { onChange(appearance.copy(publisherStyles = it)) },
                    modifier = Modifier.semantics { contentDescription = "Publisher styling" }
                )
            }
        }

        if (format == BookFormat.PDF) {
            Text(
                if (appearance.scroll) {
                    "PDF scroll mode: pages flow vertically and fit the screen width."
                } else {
                    "PDF paginated mode: pages snap horizontally and fit inside the viewport."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Back to reading")
        }
    }
}

@Composable
private fun AppearancePreset(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.heightIn(min = 48.dp)
    )
}

@OptIn(ExperimentalReadiumApi::class)
internal fun ReaderAppearance.toEpubPreferences(): EpubPreferences {
    val colors = if (publisherStyles) null else readiumThemeColors(theme)
    return EpubPreferences(
        theme = when (theme) {
            ReaderTheme.PAPER -> Theme.LIGHT
            ReaderTheme.SEPIA -> Theme.SEPIA
            ReaderTheme.DUSK, ReaderTheme.OLED -> Theme.DARK
        },
        backgroundColor = colors?.first?.let(::ReadiumColor),
        textColor = colors?.second?.let(::ReadiumColor),
        fontSize = readiumFontSizeRatio(fontScale),
        lineHeight = lineHeight.coerceIn(1.1, 2.0),
        pageMargins = pageMargins.coerceIn(0.5, 2.0),
        scroll = scroll,
        publisherStyles = publisherStyles
    )
}

internal fun readiumFontSizeRatio(scale: Double): Double =
    (if (scale.isFinite()) scale else 1.0).coerceIn(0.75, 1.8)

internal fun readiumThemeColors(theme: ReaderTheme): Pair<Int, Int> = when (theme) {
    ReaderTheme.PAPER -> 0xFFF6F1EA.toInt() to 0xFF252128.toInt()
    ReaderTheme.SEPIA -> 0xFFF1E5C9.toInt() to 0xFF3D3325.toInt()
    ReaderTheme.DUSK -> 0xFF18151D.toInt() to 0xFFF5F0F7.toInt()
    ReaderTheme.OLED -> 0xFF000000.toInt() to 0xFFF5F0F7.toInt()
}

@OptIn(ExperimentalReadiumApi::class)
internal fun ReaderAppearance.toPdfiumPreferences(): PdfiumPreferences = PdfiumPreferences(
    fit = if (scroll) Fit.WIDTH else Fit.CONTAIN,
    pageSpacing = if (scroll) 12.0 else 6.0,
    scroll = scroll,
    scrollAxis = if (scroll) Axis.VERTICAL else null
)

private const val HIGHLIGHT_GROUP = "veil-highlights"
