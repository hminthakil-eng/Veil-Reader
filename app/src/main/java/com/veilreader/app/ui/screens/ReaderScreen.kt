package com.veilreader.app.ui.screens

import android.graphics.Color as AndroidColor
import android.view.ActionMode
import android.view.accessibility.AccessibilityManager
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.veilreader.app.data.toVeilPersistedJson
import com.veilreader.app.diagnostics.ReaderTrace
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.BookReturnRitual
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderColumnMode
import com.veilreader.app.domain.ReaderFontFamily
import com.veilreader.app.domain.ReaderPreferenceToggle
import com.veilreader.app.domain.ReadingContinuitySummary
import com.veilreader.app.domain.ReaderNavigationMode
import com.veilreader.app.domain.ReaderTextAlignment
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.ui.reader.ReaderLocatorEvent
import com.veilreader.app.ui.reader.ReaderViewModel
import com.veilreader.app.ui.reader.awaitDurableReaderClose
import com.veilreader.app.ui.sensory.VeilSensoryEvent
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilMotion
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSanctuary
import com.veilreader.app.ui.theme.sanctuaryPageMaterialFor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
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
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Color as ReadiumColor
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.TextAlign as ReadiumTextAlign
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
    entryContinuity: ReadingContinuitySummary? = null,
    returnRitual: BookReturnRitual? = null,
    initialReturnLocatorJson: String? = null,
    onSensoryEvent: (VeilSensoryEvent) -> Unit = {},
    onClose: () -> Unit,
    onLocatorCheckpoint: (String) -> Unit = {}
) {
    val activity = requireNotNull(LocalActivity.current as? FragmentActivity) {
        "Veil Reader requires a FragmentActivity host."
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    var entryVisible by remember(opened.book.id) { mutableStateOf(true) }
    var navigatorAttached by remember(opened.book.id) { mutableStateOf(false) }
    var previousLocationJson by rememberSaveable(opened.book.id) {
        mutableStateOf(initialReturnLocatorJson)
    }

    LaunchedEffect(initialReturnLocatorJson, opened.book.id) {
        if (previousLocationJson == null && !initialReturnLocatorJson.isNullOrBlank()) {
            previousLocationJson = initialReturnLocatorJson
        }
    }

    LaunchedEffect(opened.book.id) {
        // Safety ceiling: a Reader failure must never leave an opaque transition permanently stuck.
        delay(2400)
        entryVisible = false
    }

    LaunchedEffect(navigatorAttached, opened.book.id) {
        if (navigatorAttached) {
            // Rare return rituals get only a slightly longer handoff; they never block reading.
            delay(if (returnRitual != null) 760 else 520)
            entryVisible = false
        }
    }
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
    val latestNavigator = rememberUpdatedState(navigator)
    var controlsVisible by remember(opened.book.id) { mutableStateOf(false) }
    var selectionModeActive by remember(opened.book.id) { mutableStateOf(false) }
    val accessibilityManager = remember(activity) {
        activity.getSystemService(AccessibilityManager::class.java)
    }
    val touchExplorationEnabled = accessibilityManager?.isTouchExplorationEnabled == true
    val reducedMotion = LocalVeilReducedMotion.current
    val paperCurlState = remember(opened.book.id) { PaperCurlState() }
    var showAppearance by remember { mutableStateOf(false) }
    var showPdfZoom by remember { mutableStateOf(false) }
    val latestAppearance = rememberUpdatedState(readerAppearance)
    val paperCurlConfig = remember(readerAppearance.theme) {
        when (readerAppearance.theme) {
            ReaderTheme.PAPER -> PaperCurlVisualConfig(
                backPageColor = Color(0xFFE3D3B5),
                backPageContentAlpha = 0.10f,
                shadowAlpha = 0.40f,
                shadowRadius = 30.dp,
                edgeHighlight = Color(0xFFFFF6E5),
                creaseHighlightAlpha = 0.28f,
                creaseShadowAlpha = 0.22f,
                backPageShadeAlpha = 0.17f,
                contactShadowAlpha = 0.20f
            )
            ReaderTheme.SEPIA -> PaperCurlVisualConfig(
                backPageColor = Color(0xFFD8C39D),
                backPageContentAlpha = 0.11f,
                shadowAlpha = 0.38f,
                shadowRadius = 29.dp,
                edgeHighlight = Color(0xFFF8E7C8),
                creaseHighlightAlpha = 0.26f,
                creaseShadowAlpha = 0.22f,
                backPageShadeAlpha = 0.18f,
                contactShadowAlpha = 0.20f
            )
            ReaderTheme.DUSK -> PaperCurlVisualConfig(
                backPageColor = Color(0xFF27222C),
                backPageContentAlpha = 0.08f,
                shadowAlpha = 0.30f,
                shadowRadius = 24.dp,
                edgeHighlight = Color(0xFFE8DFF0),
                creaseHighlightAlpha = 0.18f,
                creaseShadowAlpha = 0.18f,
                backPageShadeAlpha = 0.12f,
                contactShadowAlpha = 0.14f
            )
            ReaderTheme.OLED -> PaperCurlVisualConfig(
                backPageColor = Color(0xFF111111),
                backPageContentAlpha = 0.06f,
                shadowAlpha = 0.24f,
                shadowRadius = 20.dp,
                edgeHighlight = Color(0xFFD8D8D8),
                creaseHighlightAlpha = 0.14f,
                creaseShadowAlpha = 0.16f,
                backPageShadeAlpha = 0.10f,
                contactShadowAlpha = 0.12f
            )
        }
    }
    var showNotebook by remember { mutableStateOf(false) }

    LaunchedEffect(readerAppearance, opened.book.id) {
        ReaderTrace.event(
            "appearance_observed",
            bookId = opened.book.id,
            sessionId = readerViewModel.traceSessionId(),
            details = "theme=${readerAppearance.theme} scroll=${readerAppearance.scroll} pageTurn=${readerAppearance.pageTurnStyle} brightness=${readerAppearance.screenBrightness ?: "system"}"
        )
    }
    ReaderBrightnessEffect(activity, readerAppearance.screenBrightness)

    LaunchedEffect(
        controlsVisible,
        showNotebook,
        showAppearance,
        showPdfZoom,
        selectionModeActive,
        touchExplorationEnabled
    ) {
        if (
            shouldAutoHideReaderChrome(
                controlsVisible = controlsVisible,
                showNotebook = showNotebook,
                showAppearance = showAppearance,
                showPdfZoom = showPdfZoom,
                selectionModeActive = selectionModeActive,
                touchExplorationEnabled = touchExplorationEnabled
            )
        ) {
            delay(VeilSanctuary.chromeAutoHideMillis)
            controlsVisible = false
        }
    }

    LaunchedEffect(readerAppearance.scroll, readerAppearance.pageTurnStyle) {
        if (
            readerAppearance.scroll ||
            readerAppearance.pageTurnStyle != PageTurnStyle.PAPER
        ) {
            if (paperCurlState.active) {
                paperCurlState.clear()
            }
        }
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
    val passageVisits by library.passageVisits.collectAsStateWithLifecycle()
    val bookHighlights by bookHighlightsFlow.collectAsStateWithLifecycle(
        initialValue = library.highlightsFor(opened.book.id)
    )
    val bookBookmarks by bookBookmarksFlow.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )
    val bookPassageVisits = remember(passageVisits, opened.book.id) {
        passageVisits.filter { it.bookId == opened.book.id }
    }
    var readerMessage by remember { mutableStateOf<String?>(null) }
    var closeInFlight by remember(opened.book.id) { mutableStateOf(false) }
    var pendingNoteHighlightId by remember { mutableStateOf<String?>(null) }
    var pendingNoteText by remember { mutableStateOf("") }
    var noteSaving by remember { mutableStateOf(false) }
    var locationTitle by remember(opened.book.id) {
        mutableStateOf(opened.book.currentChapter.takeUnless { it == "Not started" }.orEmpty())
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val quickSettingsHandleVisible = shouldShowReaderQuickSettingsHandle(
        navigatorReady = navigator != null,
        controlsVisible = controlsVisible,
        showNotebook = showNotebook,
        showAppearance = showAppearance,
        showPdfZoom = showPdfZoom,
        selectionModeActive = selectionModeActive,
        closeInFlight = closeInFlight,
        paperCurlActive = paperCurlState.active
    )
    val snackbarBottom by animateDpAsState(
        targetValue = when {
            controlsVisible -> 104.dp
            quickSettingsHandleVisible -> 68.dp
            else -> 16.dp
        },
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            tween(VeilMotion.READER_SNACKBAR_SHIFT_MS)
        },
        label = "reader-snackbar-offset"
    )

    val selectionActionModeCallback = remember(opened.book.id, library, readerViewModel, scope) {
        ReaderSelectionActionModeCallback(
            coroutineScope = scope,
            navigatorProvider = { navigator as? SelectableNavigator },
            onModeChanged = { active ->
                selectionModeActive = active
                if (active) controlsVisible = true
            },
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
                        onSensoryEvent(VeilSensoryEvent.MARK)
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

    fun recordLocator(locator: Locator, event: ReaderLocatorEvent) {
        val json = locator.toVeilPersistedJson(opened.format)
        readerViewModel.onLocatorUpdate(
            bookId = opened.book.id,
            progression = locator.locations.totalProgression
                ?: readerViewModel.uiState.value.progress.toDouble(),
            locatorJson = json,
            locationKey = "${opened.book.id}:$json",
            event = event
        )?.let { commit ->
            onLocatorCheckpoint(commit.locatorJson)
        }
    }

    fun currentLocatorJson(): String? =
        navigator?.currentLocator?.value?.toVeilPersistedJson(opened.format)

    fun returnToPreviousLocation() {
        val targetJson = previousLocationJson ?: return
        val locator = runCatching {
            Locator.fromJSON(JSONObject(targetJson))
        }.getOrNull()
        val currentJson = currentLocatorJson()

        readerViewModel.onUserInteraction()
        game.rebasePagePacing()

        if (locator != null && navigator?.go(locator, animated = true) == true) {
            previousLocationJson = currentJson?.takeIf { it != targetJson }
            controlsVisible = false
        } else {
            readerMessage = "The previous reading location could not be restored."
        }
    }

    fun closeReader() {
        if (closeInFlight) return
        latestNavigator.value?.currentLocator?.value?.let { locator ->
            recordLocator(locator, ReaderLocatorEvent.FINAL_SNAPSHOT)
        }
        closeInFlight = true
        scope.launch {
            try {
                ReaderTrace.event(
                    "reader_close_durability_wait",
                    bookId = opened.book.id,
                    sessionId = readerViewModel.traceSessionId()
                )
                awaitDurableReaderClose(
                    finalizeSession = readerViewModel::closeBook,
                    awaitDurability = library::flushWrites,
                    clearRoute = onClose
                )
                ReaderTrace.event(
                    "reader_close_durable",
                    bookId = opened.book.id,
                    sessionId = readerViewModel.traceSessionId()
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                closeInFlight = false
                ReaderTrace.event(
                    "reader_close_durability_failed",
                    bookId = opened.book.id,
                    sessionId = readerViewModel.traceSessionId(),
                    details = "error=${error::class.java.simpleName}"
                )
                readerMessage = "Could not safely close this book because the latest reading position was not confirmed in storage."
            }
        }
    }

    BackHandler(
        enabled = !closeInFlight && !showNotebook && !showAppearance && !showPdfZoom && !paperCurlState.active
    ) { closeReader() }

    val fragmentFactory = remember(opened.book.id, selectionActionModeCallback) {
        createReaderFactory(opened, readerAppearance, selectionActionModeCallback)
    }
    val onNavigatorReady = remember<(Navigator) -> Unit>(opened.book.id) {
        { ready ->
            navigator = ready
            navigatorAttached = true
            ReaderTrace.event(
                "navigator_attached",
                bookId = opened.book.id,
                sessionId = readerViewModel.traceSessionId(),
                details = "type=${ready::class.java.simpleName}"
            )
        }
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
                Lifecycle.Event.ON_DESTROY -> {
                    latestNavigator.value?.currentLocator?.value?.let { locator ->
                        recordLocator(locator, ReaderLocatorEvent.FINAL_SNAPSHOT)
                    }
                    readerViewModel.onPause()
                }
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
                ReaderTrace.event(
                    "locator_observed",
                    bookId = opened.book.id,
                    sessionId = readerViewModel.traceSessionId(),
                    details = "progress=${locator.locations.totalProgression}"
                )
                val continuousScroll =
                    (nav as? OverflowableNavigator)?.overflow?.value?.scroll == true
                val event = when {
                    continuousScroll -> ReaderLocatorEvent.NAVIGATOR_SCROLL_COMMIT
                    opened.format != BookFormat.EPUB ||
                        latestAppearance.value.pageTurnStyle != PageTurnStyle.PAPER ->
                        ReaderLocatorEvent.NAVIGATOR_PAGE_TURN
                    else -> ReaderLocatorEvent.NAVIGATOR_POSITION
                }
                readerViewModel.onLocatorUpdate(
                    bookId = opened.book.id,
                    progression = locator.locations.totalProgression
                        ?: readerViewModel.uiState.value.progress.toDouble(),
                    locatorJson = json,
                    locationKey = "${opened.book.id}:$json",
                    event = event
                )?.let { commit ->
                    onLocatorCheckpoint(commit.locatorJson)
                }
            }
    }

    DisposableEffect(navigator, opened.book.id) {
        val nav = navigator as? OverflowableNavigator
        if (nav == null) {
            onDispose { }
        } else {
            val paperListener = if (navigator is EpubNavigatorFragment) {
                PaperCurlInputListener(
                    navigator = nav,
                    state = paperCurlState,
                    isEnabled = {
                        !latestAppearance.value.scroll &&
                            latestAppearance.value.pageTurnStyle == PageTurnStyle.PAPER
                    },
                    scope = scope,
                    isReducedMotion = { reducedMotion },
                    onInteraction = {
                        readerViewModel.onUserInteraction()
                        controlsVisible = false
                    },
                    onCommittedTurn = {
                        onSensoryEvent(VeilSensoryEvent.PAGE_TURN)
                        val locator = nav.currentLocator.value
                        val json = locator.toVeilPersistedJson(opened.format)
                        recordLocator(locator, ReaderLocatorEvent.PAPER_COMMIT)
                    }
                )
            } else {
                null
            }

            val directionalListener = VeilDirectionalNavigationInputListener(
                navigator = nav,
                isAnimated = {
                    !reducedMotion &&
                        shouldAnimateDirectionalNavigation(
                            format = opened.format,
                            pageTurnStyle = latestAppearance.value.pageTurnStyle
                        )
                },
                isTapNavigationEnabled = {
                    shouldUseDirectionalTapNavigation(
                        format = opened.format,
                        scroll = nav.overflow.value.scroll,
                        pageTurnStyle = latestAppearance.value.pageTurnStyle
                    )
                },
                onNavigationCommitted = {
                    onSensoryEvent(VeilSensoryEvent.PAGE_TURN)
                }
            )

            val inputArbiter = ReaderInputArbiter(
                paper = paperListener,
                directional = directionalListener,
                chromeTap = {
                    readerViewModel.onUserInteraction()
                    controlsVisible = !controlsVisible
                    true
                },
                onTapOwner = { owner ->
                    ReaderTrace.event(
                        "gesture_owned",
                        bookId = opened.book.id,
                        sessionId = readerViewModel.traceSessionId(),
                        details = "gesture=tap owner=${owner.name.lowercase()}"
                    )
                }
            )

            nav.addInputListener(inputArbiter)
            onDispose {
                nav.removeInputListener(inputArbiter)
            }
        }
    }

    LaunchedEffect(navigator, readerAppearance, opened.format) {
        game.rebasePagePacing()
        readerViewModel.onUserInteraction()
        val traceDetails = "format=${opened.format} theme=${readerAppearance.theme} publisherStyles=${readerAppearance.publisherStyles} scroll=${readerAppearance.scroll} pageTurn=${readerAppearance.pageTurnStyle}"
        ReaderTrace.event(
            "appearance_submit_requested",
            bookId = opened.book.id,
            sessionId = readerViewModel.traceSessionId(),
            details = traceDetails
        )
        when (opened.format) {
            BookFormat.EPUB ->
                (navigator as? EpubNavigatorFragment)
                    ?.submitPreferences(readerAppearance.toEpubPreferences())

            BookFormat.PDF -> {
                @Suppress("UNCHECKED_CAST")
                val pdfNavigator = navigator as? PdfiumNavigatorFragment
                pdfNavigator?.submitPreferences(readerAppearance.toPdfiumPreferences())
            }

            else -> Unit
        }
        ReaderTrace.event(
            "appearance_submit_returned",
            bookId = opened.book.id,
            sessionId = readerViewModel.traceSessionId(),
            details = traceDetails
        )
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

    val readerCanvas = readerCanvasColor(readerAppearance.theme)

    Box(
        Modifier
            .fillMaxSize()
            .background(readerCanvas)
            .semantics {
                contentDescription = "Reader surface"
                onClick(label = "Toggle reader controls") {
                    readerViewModel.onUserInteraction()
                    controlsVisible = !controlsVisible
                    true
                }
            }
    ) {
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

        if (
            opened.format == BookFormat.EPUB &&
            !readerAppearance.scroll &&
            readerAppearance.pageTurnStyle == PageTurnStyle.PAPER
        ) {
            PaperCurlOverlay(
                state = paperCurlState,
                config = paperCurlConfig,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (opened.format == BookFormat.EPUB) {
            ReaderPageAtmosphere(
                theme = readerAppearance.theme,
                navigationMode = readerAppearance.navigationMode,
                progress = progress,
                progression = (navigator as? OverflowableNavigator)
                    ?.overflow
                    ?.value
                    ?.readingProgression
                    ?: ReadingProgression.LTR,
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = if (reducedMotion) {
                fadeIn(tween(VeilMotion.REDUCED_MOTION_FADE_MS))
            } else {
                fadeIn(tween(VeilMotion.MICRO_FAST_MS)) +
                    slideInVertically(tween(VeilMotion.FUNCTIONAL_ENTER_MS)) { -it / 4 }
            },
            exit = if (reducedMotion) {
                fadeOut(tween(VeilMotion.REDUCED_MOTION_FADE_MS))
            } else {
                fadeOut(tween(VeilMotion.MICRO_FAST_MS)) +
                    slideOutVertically(tween(VeilMotion.FUNCTIONAL_EXIT_MS)) { -it / 4 }
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = VeilPalette.Ink,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReaderChromeButton(
                            ReaderAction.BACK,
                            "Close reader"
                        ) { closeReader() }

                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                opened.book.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = VeilPalette.Moon,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                locationTitle.ifBlank {
                                    opened.book.author.ifBlank { opened.format.name }
                                },
                                color = VeilPalette.Moon.copy(alpha = 0.54f),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                            modifier = Modifier.semantics {
                                contentDescription =
                                    "${(progress.coerceIn(0f, 1f) * 100).toInt()} percent read"
                            },
                            color = VeilPalette.Brass,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(1.dp),
                        color = VeilPalette.Brass,
                        trackColor = VeilPalette.Moon.copy(alpha = 0.10f),
                        drawStopIndicator = {}
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = quickSettingsHandleVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 6.dp),
            enter = fadeIn(
                tween(
                    if (reducedMotion) VeilMotion.REDUCED_MOTION_FADE_MS
                    else VeilMotion.MICRO_FAST_MS
                )
            ),
            exit = fadeOut(
                tween(
                    if (reducedMotion) VeilMotion.REDUCED_MOTION_FADE_MS
                    else VeilMotion.MICRO_FAST_MS
                )
            )
        ) {
            ReaderQuickSettingsHandle(
                epub = opened.format == BookFormat.EPUB,
                onClick = {
                    readerViewModel.onUserInteraction()
                    controlsVisible = false
                    if (opened.format == BookFormat.EPUB) {
                        showAppearance = true
                    } else {
                        showPdfZoom = true
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = if (reducedMotion) {
                fadeIn(tween(VeilMotion.REDUCED_MOTION_FADE_MS))
            } else {
                fadeIn(tween(VeilMotion.MICRO_FAST_MS)) +
                    slideInVertically(tween(VeilMotion.FUNCTIONAL_ENTER_MS)) { it / 4 }
            },
            exit = if (reducedMotion) {
                fadeOut(tween(VeilMotion.REDUCED_MOTION_FADE_MS))
            } else {
                fadeOut(tween(VeilMotion.MICRO_FAST_MS)) +
                    slideOutVertically(tween(VeilMotion.FUNCTIONAL_EXIT_MS)) { it / 4 }
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = VeilPalette.Ink,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        VeilPalette.Brass.copy(alpha = 0.42f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReaderControl(
                            action = ReaderAction.NOTEBOOK,
                            label = "Notes",
                            modifier = Modifier.weight(1f)
                        ) {
                            readerViewModel.onUserInteraction()
                            showNotebook = true
                        }

                        ReaderControl(
                            action = ReaderAction.BOOKMARK,
                            label = "Mark",
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
                                if (added) {
                                    onSensoryEvent(VeilSensoryEvent.MARK)
                                }
                                readerMessage = if (added) {
                                    "Bookmark saved"
                                } else {
                                    "This location is already bookmarked"
                                }
                            }
                        }

                        ReaderControl(
                            action = if (opened.format == BookFormat.EPUB) {
                                ReaderAction.APPEARANCE
                            } else {
                                ReaderAction.ZOOM
                            },
                            label = if (opened.format == BookFormat.EPUB) "Type" else "Zoom",
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
        }

        AnimatedVisibility(
            visible = controlsVisible && previousLocationJson != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 56.dp),
            enter = fadeIn(
                tween(
                    if (reducedMotion) VeilMotion.REDUCED_MOTION_FADE_MS
                    else VeilMotion.TAP_MS
                )
            ),
            exit = fadeOut(
                tween(
                    if (reducedMotion) VeilMotion.REDUCED_MOTION_FADE_MS
                    else VeilMotion.MICRO_FAST_MS
                )
            )
        ) {
            OutlinedButton(
                onClick = ::returnToPreviousLocation,
                shape = MaterialTheme.shapes.extraSmall,
                border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.34f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = VeilPalette.Ink.copy(alpha = 0.96f),
                    contentColor = VeilPalette.Moon
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        "RETURN",
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Brass.copy(alpha = 0.84f)
                    )
                    Text(
                        "Previous location",
                        style = MaterialTheme.typography.labelMedium
                    )
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

        BookThresholdTransitionOverlay(
            book = opened.book,
            stage = BookEntryStage.HANDOFF,
            visible = entryVisible,
            continuity = entryContinuity,
            returnRitual = returnRitual,
            modifier = Modifier.fillMaxSize()
        )
    }

    pendingNoteHighlightId?.let { highlightId ->
        val pendingHighlight = bookHighlights.firstOrNull { it.id == highlightId }

        AlertDialog(
            onDismissRequest = {
                if (!noteSaving) {
                    pendingNoteHighlightId = null
                    pendingNoteText = ""
                }
            },
            shape = MaterialTheme.shapes.small,
            containerColor = VeilPalette.Archive,
            titleContentColor = VeilPalette.Moon,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = 0.dp,
            title = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "HIDDEN ARCHIVE · PASSAGE NOTE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.25.sp
                        ),
                        color = VeilPalette.Brass
                    )
                    Text(
                        "Note on this passage",
                        style = MaterialTheme.typography.titleLarge,
                        color = VeilPalette.Moon
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pendingHighlight?.quote
                        ?.takeIf { it.isNotBlank() }
                        ?.let { quote ->
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = VeilPalette.Ink.copy(alpha = 0.54f),
                                border = BorderStroke(
                                    1.dp,
                                    VeilPalette.Brass.copy(alpha = 0.28f)
                                ),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "SELECTED PASSAGE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VeilPalette.Brass.copy(alpha = 0.82f)
                                    )
                                    Text(
                                        "“$quote”",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = VeilPalette.Moon.copy(alpha = 0.78f),
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                    OutlinedTextField(
                        value = pendingNoteText,
                        onValueChange = { pendingNoteText = it },
                        enabled = !noteSaving,
                        placeholder = { Text("Write what you want to remember…") },
                        minLines = 4,
                        maxLines = 8,
                        shape = MaterialTheme.shapes.extraSmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VeilPalette.Brass.copy(alpha = 0.84f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
                            focusedContainerColor = VeilPalette.Ink.copy(alpha = 0.36f),
                            unfocusedContainerColor = VeilPalette.Ink.copy(alpha = 0.24f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            "${pendingNoteText.length} characters",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !noteSaving,
                    onClick = {
                        scope.launch {
                            noteSaving = true
                            try {
                                library.updateHighlightNote(highlightId, pendingNoteText)
                                library.flushWrites()
                                readerViewModel.onNoteSaved(highlightId, pendingNoteText)
                                onSensoryEvent(VeilSensoryEvent.NOTE)
                                pendingNoteHighlightId = null
                                pendingNoteText = ""
                                readerMessage = "Note saved"
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                readerMessage = error.message ?: "The note could not be saved."
                            } finally {
                                noteSaving = false
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VeilPalette.Brass,
                        contentColor = Color(0xFF17120A)
                    )
                ) {
                    Text(if (noteSaving) "Saving…" else "Save note")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !noteSaving,
                    onClick = {
                        pendingNoteHighlightId = null
                        pendingNoteText = ""
                    }
                ) {
                    Text(
                        "Cancel",
                        color = VeilPalette.Moon.copy(alpha = 0.72f)
                    )
                }
            }
        )
    }

    if (showNotebook) {
        ReaderNotebook(
            opened = opened,
            highlights = bookHighlights,
            bookmarks = bookBookmarks,
            passageVisits = bookPassageVisits,
            onDismiss = { showNotebook = false },
            onGo = { json ->
                readerViewModel.onUserInteraction()
                game.rebasePagePacing()
                val origin = currentLocatorJson()
                val locator = runCatching { Locator.fromJSON(JSONObject(json)) }.getOrNull()
                if (locator != null && navigator?.go(locator, animated = true) == true) {
                    library.recordPassageVisitForLocator(
                        bookId = opened.book.id,
                        locatorJson = json
                    )
                    previousLocationJson = origin?.takeIf { it != json }
                    showNotebook = false
                } else {
                    showNotebook = false
                    readerMessage = "That saved location could not be opened."
                }
            },
            onChapter = { link ->
                readerViewModel.onUserInteraction()
                game.rebasePagePacing()
                val origin = currentLocatorJson()
                if (navigator?.go(link, animated = true) == true) {
                    previousLocationJson = origin
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
                onSensoryEvent(VeilSensoryEvent.NOTE)
                readerMessage = "Note saved"
            },
            onDeleteHighlight = library::deleteHighlight,
            onDeleteBookmark = library::deleteBookmark
        )
    }

    if (showAppearance) {
        Dialog(
            onDismissRequest = { showAppearance = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = VeilPalette.Ink,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF141821),
                                    VeilPalette.Ink,
                                    Color(0xFF080A0E)
                                )
                            )
                        )
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    contentAlignment = Alignment.TopCenter
                ) {
                    GrayfogOrnamentFrame(
                        modifier = Modifier.matchParentSize(),
                        strength = 0.38f
                    )
                    EpubAppearancePanel(
                        appearance = readerAppearance,
                        onChange = {
                            readerViewModel.onUserInteraction()
                            onReaderAppearanceChange(it)
                        },
                        onDone = { showAppearance = false },
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp)
                    )
                }
            }
        }
    }

    if (showPdfZoom) {
        ModalBottomSheet(
            onDismissRequest = { showPdfZoom = false },
            containerColor = VeilPalette.Ink,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = VeilPalette.Brass.copy(alpha = 0.48f)
                )
            }
        ) {
            PdfZoomControls(
                navigator = navigator,
                appearance = readerAppearance,
                onAppearanceChange = { updated ->
                    readerViewModel.onUserInteraction()
                    onReaderAppearanceChange(updated)
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

internal fun shouldShowReaderQuickSettingsHandle(
    navigatorReady: Boolean,
    controlsVisible: Boolean,
    showNotebook: Boolean,
    showAppearance: Boolean,
    showPdfZoom: Boolean,
    selectionModeActive: Boolean,
    closeInFlight: Boolean,
    paperCurlActive: Boolean
): Boolean =
    navigatorReady &&
        !controlsVisible &&
        !showNotebook &&
        !showAppearance &&
        !showPdfZoom &&
        !selectionModeActive &&
        !closeInFlight &&
        !paperCurlActive

internal fun shouldAutoHideReaderChrome(
    controlsVisible: Boolean,
    showNotebook: Boolean,
    showAppearance: Boolean,
    showPdfZoom: Boolean,
    selectionModeActive: Boolean,
    touchExplorationEnabled: Boolean
): Boolean =
    controlsVisible &&
        !showNotebook &&
        !showAppearance &&
        !showPdfZoom &&
        !selectionModeActive &&
        !touchExplorationEnabled

private fun readerCanvasColor(theme: ReaderTheme): Color = when (theme) {
    ReaderTheme.PAPER -> Color(0xFFE9DEC5)
    ReaderTheme.SEPIA -> Color(0xFFE2D0AA)
    ReaderTheme.DUSK -> Color(0xFF18151D)
    ReaderTheme.OLED -> Color.Black
}

@Composable
private fun ReaderPageAtmosphere(
    theme: ReaderTheme,
    navigationMode: ReaderNavigationMode,
    progress: Float,
    progression: ReadingProgression,
    modifier: Modifier = Modifier
) {
    val dark = theme == ReaderTheme.DUSK || theme == ReaderTheme.OLED
    val material = sanctuaryPageMaterialFor(navigationMode)
    val stack = paperPageStackDepth(progress, progression)

    Canvas(modifier) {
        val edge = if (dark) {
            Color.Black.copy(alpha = 0.20f)
        } else {
            Color(0xFF4A3923).copy(alpha = 0.085f)
        }
        val highlight = if (dark) {
            Color.White.copy(alpha = 0.020f)
        } else {
            Color.White.copy(alpha = 0.135f)
        }
        val leftStackWidth = stack.leftDp.dp.toPx()
        val rightStackWidth = stack.rightDp.dp.toPx()

        // Physical page stack belongs only to paginated modes. Scroll remains a continuous
        // paper field and must not visually imply a detachable sheet at either edge.
        if (material.showPhysicalPageStack) {
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(edge, Color.Transparent),
                    startX = 0f,
                    endX = leftStackWidth
                ),
                size = Size(leftStackWidth, size.height)
            )
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, edge),
                    startX = size.width - rightStackWidth,
                    endX = size.width
                ),
                topLeft = Offset(size.width - rightStackWidth, 0f),
                size = Size(rightStackWidth, size.height)
            )

            val sheetLine = if (dark) {
                Color.White.copy(alpha = 0.018f)
            } else {
                Color(0xFF4A3923).copy(alpha = 0.035f)
            }
            repeat(3) { index ->
                val fraction = (index + 1) / 4f
                drawLine(
                    color = sheetLine,
                    start = Offset(leftStackWidth * fraction, 0f),
                    end = Offset(leftStackWidth * fraction, size.height),
                    strokeWidth = 0.45.dp.toPx()
                )
                drawLine(
                    color = sheetLine,
                    start = Offset(size.width - rightStackWidth * fraction, 0f),
                    end = Offset(size.width - rightStackWidth * fraction, size.height),
                    strokeWidth = 0.45.dp.toPx()
                )
            }
        }

        // Very soft top/bottom page falloff. Keep it below the threshold where it
        // competes with body text.
        val falloff = if (dark) {
            Color.Black.copy(alpha = 0.075f)
        } else {
            Color(0xFF7C6544).copy(alpha = 0.040f)
        }
        val band = 28.dp.toPx()
        if (material.showEdgeFalloff) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(falloff, Color.Transparent),
                    startY = 0f,
                    endY = band
                ),
                size = Size(size.width, band)
            )
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, falloff),
                    startY = size.height - band,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height - band),
                size = Size(size.width, band)
            )
        }

        // Deterministic micro-fibres: deliberately sparse and nearly invisible.
        // They add material character without turning the page into a texture image.
        if (!dark && material.showMicroFibres) {
            val fibre = Color(0xFF6F5A3D).copy(alpha = 0.012f)
            repeat(18) { index ->
                val y = ((index * 71f + 29f) % size.height)
                val x = ((index * 43f + 17f) % (size.width * 0.55f))
                val length = 24.dp.toPx() + (index % 4) * 9.dp.toPx()
                drawLine(
                    color = fibre,
                    start = Offset(x, y),
                    end = Offset((x + length).coerceAtMost(size.width), y + (index % 3 - 1) * 0.6f),
                    strokeWidth = 0.55.dp.toPx()
                )
            }

            val speck = Color(0xFF59462F).copy(alpha = 0.015f)
            repeat(24) { index ->
                val x = ((index * 97f + 31f) % size.width)
                val y = ((index * 137f + 47f) % size.height)
                drawCircle(
                    color = speck,
                    radius = if (index % 5 == 0) 0.75.dp.toPx() else 0.45.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        drawLine(
            color = highlight,
            start = Offset(0f, 1.dp.toPx()),
            end = Offset(size.width, 1.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
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
            defaults = PdfiumDefaults()
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
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = accessibilityLabel }
    ) {
        ReaderActionIcon(
            action = action,
            modifier = Modifier.size(21.dp),
            tint = VeilPalette.Brass
        )
    }
}

@Composable
private fun ReaderQuickSettingsHandle(
    epub: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .semantics {
                contentDescription =
                    if (epub) "Open reading appearance and page movement controls"
                    else "Open PDF reading and zoom controls"
            }
            .clickable(role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Ink.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.38f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (epub) "Aa" else "±",
                style = MaterialTheme.typography.titleSmall,
                color = VeilPalette.Brass
            )
            Text(
                "READING",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.15.sp),
                color = VeilPalette.Moon.copy(alpha = 0.86f)
            )
        }
    }
}

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
        modifier = modifier.defaultMinSize(minWidth = 0.dp, minHeight = 48.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 5.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = VeilPalette.Moon,
            disabledContentColor = VeilPalette.Moon.copy(alpha = 0.28f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            ReaderActionIcon(
                action = action,
                modifier = Modifier.size(
                    if (action == ReaderAction.APPEARANCE) 24.dp else 18.dp
                ),
                tint = if (enabled) VeilPalette.Brass else VeilPalette.Moon.copy(alpha = 0.28f)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (enabled) VeilPalette.Moon.copy(alpha = 0.78f)
                    else VeilPalette.Moon.copy(alpha = 0.28f),
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
                val tipX = if (layoutDirection == LayoutDirection.Rtl) {
                    w * .66f
                } else {
                    w * .34f
                }
                val tailX = if (layoutDirection == LayoutDirection.Rtl) {
                    w * .28f
                } else {
                    w * .72f
                }
                drawLine(
                    tint,
                    Offset(tailX, h * .20f),
                    Offset(tipX, h * .50f),
                    stroke.width,
                    StrokeCap.Round
                )
                drawLine(
                    tint,
                    Offset(tipX, h * .50f),
                    Offset(tailX, h * .80f),
                    stroke.width,
                    StrokeCap.Round
                )
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
private fun EpubAppearancePanel(
    appearance: ReaderAppearance,
    onChange: (ReaderAppearance) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember { mutableStateOf(appearance) }
    var hasPendingDraft by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }

    LaunchedEffect(appearance) {
        when {
            !hasPendingDraft -> draft = appearance
            appearance == draft -> hasPendingDraft = false
        }
    }

    fun updateDraft(value: ReaderAppearance) {
        draft = value
        hasPendingDraft = true
        onChange(value)
    }

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    "READING INSTRUMENTS",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
                    color = VeilPalette.Brass
                )
                BrassRule(Modifier.width(76.dp))
                Text(
                    "Appearance",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Changes apply live to the open publication.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            TextButton(
                onClick = onDone,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = VeilPalette.Moon
                )
            ) {
                Text(
                    "DONE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.15.sp)
                )
            }
        }

        // The preview is the focal instrument; controls stay secondary and compact.
        ReaderAppearancePreview(
            appearance = draft,
            modifier = Modifier.fillMaxWidth()
        )

        /* header consumed above */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf(false to "QUICK", true to "ADVANCED").forEach { (advanced, label) ->
                val selected = showAdvanced == advanced
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = selected,
                            role = Role.Tab
                        ) { showAdvanced = advanced },
                    shape = MaterialTheme.shapes.extraSmall,
                    color = if (selected) {
                        VeilPalette.DeepBrass.copy(alpha = 0.78f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.52f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (selected) VeilPalette.Brass.copy(alpha = 0.78f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.0.sp),
                            color = if (selected) VeilPalette.Moon
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (!showAdvanced) {
            Text(
                "THEME",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.3.sp),
                color = VeilPalette.Brass
            )

            listOf(
                listOf(ReaderTheme.PAPER to "Paper", ReaderTheme.SEPIA to "Sepia"),
                listOf(ReaderTheme.DUSK to "Dusk", ReaderTheme.OLED to "Night")
            ).forEach { presets ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (theme, label) ->
                        AppearancePreset(
                            label = label,
                            theme = theme,
                            selected = draft.theme == theme,
                            modifier = Modifier.weight(1f)
                        ) {
                            updateDraft(draft.withTheme(theme))
                        }
                    }
                }
            }

            BrassRule(Modifier.fillMaxWidth())

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "TEXT SIZE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = VeilPalette.Brass,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${(draft.fontScale * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = draft.fontScale.toFloat(),
                onValueChange = { updateDraft(draft.withFontScale(it.toDouble())) },
                valueRange = .75f..1.8f
            )

            BrassRule(Modifier.fillMaxWidth())

            Text(
                "PAGE MOVEMENT",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                color = VeilPalette.Brass
            )
            ReaderMotionSelector(
                selected = draft.navigationMode,
                onSelect = { updateDraft(draft.withNavigationMode(it)) }
            )
            Text(
                readerNavigationModeDescription(draft.navigationMode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "TYPOGRAPHY & LAYOUT",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                color = VeilPalette.Brass
            )

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Line spacing",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${"%.2f".format(draft.lineHeight)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = draft.lineHeight.toFloat(),
                onValueChange = { updateDraft(draft.withLineHeight(it.toDouble())) },
                valueRange = 1.1f..2.0f
            )

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Page margins",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${(draft.pageMargins * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = draft.pageMargins.toFloat(),
                onValueChange = { updateDraft(draft.withPageMargins(it.toDouble())) },
                valueRange = .5f..2.0f
            )

            BrassRule(Modifier.fillMaxWidth())

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "Publisher styling",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "Preserve the book's own typography when available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = draft.publisherStyles,
                    onCheckedChange = {
                        updateDraft(draft.copy(publisherStyles = it))
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Publisher styling"
                    }
                )
            }

            OutlinedButton(
                onClick = { updateDraft(ReaderAppearance()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.extraSmall,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)
                )
            ) {
                Text("Reset appearance")
            }
        }

        BrassRule(Modifier.fillMaxWidth())

        ReaderBrightnessControls(
            appearance = draft,
            onChange = ::updateDraft
        )

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = MaterialTheme.shapes.extraSmall,
            colors = ButtonDefaults.buttonColors(
                containerColor = VeilPalette.Brass,
                contentColor = Color(0xFF17120A)
            )
        ) {
            Text("Back to reading")
        }
    }
}

@Composable
private fun ReaderAppearancePreview(
    appearance: ReaderAppearance,
    modifier: Modifier = Modifier
) {
    val (paperArgb, inkArgb) = readiumThemeColors(appearance.theme)
    val paper = Color(paperArgb)
    val ink = Color(inkArgb)
    val margin = (14f + 12f * appearance.pageMargins.toFloat()).dp
    val sampleSize = (15f * appearance.fontScale.toFloat()).coerceIn(11f, 23f).sp
    val sampleLineHeight =
        (sampleSize.value * appearance.lineHeight.toFloat()).coerceIn(15f, 38f).sp

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.52f),
        border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.38f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LIVE PAGE PREVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.25.sp),
                    color = VeilPalette.Brass,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    when (appearance.navigationMode) {
                        ReaderNavigationMode.PAPER_CURL -> "CURL"
                        ReaderNavigationMode.SLIDE -> "SLIDE"
                        ReaderNavigationMode.PAGED -> "PAGED"
                        ReaderNavigationMode.SCROLL -> "SCROLL"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp)
                    .background(paper, MaterialTheme.shapes.extraSmall)
                    .border(
                        1.dp,
                        if (appearance.theme == ReaderTheme.OLED) {
                            Color.White.copy(alpha = 0.08f)
                        } else {
                            Color(0xFF6E5D42).copy(alpha = 0.24f)
                        },
                        MaterialTheme.shapes.extraSmall
                    )
            ) {
                ReaderPageAtmosphere(
                    theme = appearance.theme,
                    navigationMode = appearance.navigationMode,
                    progress = 0.42f,
                    progression = ReadingProgression.LTR,
                    modifier = Modifier.matchParentSize()
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = margin, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "CHAPTER VII",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                        color = ink.copy(alpha = 0.58f)
                    )
                    Text(
                        "Beyond the Veil",
                        style = MaterialTheme.typography.titleLarge,
                        color = ink
                    )
                    Text(
                        "The page should disappear beneath the story. Type, spacing, and motion remain present only when they help the eye move forward.",
                        fontSize = sampleSize,
                        lineHeight = sampleLineHeight,
                        color = ink.copy(alpha = 0.92f)
                    )
                }
            }
        }
    }
}

internal fun readerNavigationModeDescription(mode: ReaderNavigationMode): String =
    when (mode) {
        ReaderNavigationMode.PAPER_CURL ->
            "Physical page curl with weighted drag, release velocity, and page-stack depth."
        ReaderNavigationMode.SLIDE ->
            "Paginated reading with a lightweight horizontal transition and no paper deformation."
        ReaderNavigationMode.PAGED ->
            "Static pagination with no decorative page transition."
        ReaderNavigationMode.SCROLL ->
            "Continuous vertical reading when the publication format supports it."
    }

@Composable
internal fun ReaderMotionSelector(
    selected: ReaderNavigationMode,
    onSelect: (ReaderNavigationMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ReaderNavigationMode.entries.forEach { mode ->
            val active = selected == mode
            val label = when (mode) {
                ReaderNavigationMode.PAPER_CURL -> "Curl"
                ReaderNavigationMode.SLIDE -> "Slide"
                ReaderNavigationMode.PAGED -> "Paged"
                ReaderNavigationMode.SCROLL -> "Scroll"
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .selectable(
                        selected = active,
                        role = Role.RadioButton
                    ) { onSelect(mode) },
                shape = MaterialTheme.shapes.extraSmall,
                color = if (active) {
                    VeilPalette.DeepBrass.copy(alpha = 0.76f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.46f)
                },
                border = BorderStroke(
                    1.dp,
                    if (active) VeilPalette.Brass.copy(alpha = 0.82f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.46f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    ReaderMotionPreview(
                        mode = mode,
                        active = active,
                        modifier = Modifier
                            .width(44.dp)
                            .height(28.dp)
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) VeilPalette.Moon
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun ReaderMotionPreview(
    mode: ReaderNavigationMode,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = if (active) {
        VeilPalette.Brass
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
    }
    val faint = lineColor.copy(alpha = 0.38f)

    Canvas(modifier) {
        val stroke = 1.15.dp.toPx()
        val radius = 1.5.dp.toPx()
        val inset = 1.5.dp.toPx()
        val pageTop = size.height * 0.08f
        val pageHeight = size.height * 0.84f

        when (mode) {
            ReaderNavigationMode.PAPER_CURL -> {
                drawRoundRect(
                    color = faint,
                    topLeft = Offset(inset, pageTop),
                    size = Size(size.width - inset * 2f, pageHeight),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(stroke)
                )
                val fold = Path().apply {
                    moveTo(size.width * 0.67f, pageTop)
                    lineTo(size.width * 0.60f, size.height * 0.48f)
                    lineTo(size.width * 0.78f, size.height * 0.92f)
                }
                drawPath(
                    path = fold,
                    color = lineColor,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                drawLine(
                    color = lineColor.copy(alpha = 0.54f),
                    start = Offset(size.width * 0.67f, pageTop),
                    end = Offset(size.width * 0.92f, size.height * 0.25f),
                    strokeWidth = stroke
                )
            }

            ReaderNavigationMode.SLIDE -> {
                val pageWidth = size.width * 0.46f
                drawRoundRect(
                    color = faint,
                    topLeft = Offset(-pageWidth * 0.28f, pageTop),
                    size = Size(pageWidth, pageHeight),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(stroke)
                )
                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset(size.width * 0.42f, pageTop),
                    size = Size(pageWidth, pageHeight),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(stroke)
                )
                drawLine(
                    color = lineColor.copy(alpha = 0.55f),
                    start = Offset(size.width * 0.35f, size.height * 0.50f),
                    end = Offset(size.width * 0.55f, size.height * 0.50f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }

            ReaderNavigationMode.PAGED -> {
                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset(size.width * 0.18f, pageTop),
                    size = Size(size.width * 0.64f, pageHeight),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(stroke)
                )
                repeat(3) { index ->
                    val y = size.height * (0.34f + index * 0.15f)
                    drawLine(
                        color = faint,
                        start = Offset(size.width * 0.31f, y),
                        end = Offset(size.width * 0.69f, y),
                        strokeWidth = stroke * 0.75f,
                        cap = StrokeCap.Round
                    )
                }
            }

            ReaderNavigationMode.SCROLL -> {
                drawRoundRect(
                    color = faint,
                    topLeft = Offset(size.width * 0.20f, pageTop),
                    size = Size(size.width * 0.60f, pageHeight),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(stroke)
                )
                repeat(4) { index ->
                    val y = size.height * (0.24f + index * 0.16f)
                    val shift = if (index % 2 == 0) 0f else size.width * 0.06f
                    drawLine(
                        color = if (index == 2) lineColor else faint,
                        start = Offset(size.width * 0.30f + shift, y),
                        end = Offset(size.width * 0.70f, y),
                        strokeWidth = stroke * 0.8f,
                        cap = StrokeCap.Round
                    )
                }
                drawLine(
                    color = lineColor,
                    start = Offset(size.width * 0.82f, size.height * 0.30f),
                    end = Offset(size.width * 0.82f, size.height * 0.70f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = lineColor,
                    start = Offset(size.width * 0.76f, size.height * 0.64f),
                    end = Offset(size.width * 0.82f, size.height * 0.70f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = lineColor,
                    start = Offset(size.width * 0.88f, size.height * 0.64f),
                    end = Offset(size.width * 0.82f, size.height * 0.70f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun AppearancePreset(
    label: String,
    theme: ReaderTheme,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (paperArgb, inkArgb) = readiumThemeColors(theme)
    val paper = Color(paperArgb)
    val ink = Color(inkArgb)

    Surface(
        modifier = modifier
            .heightIn(min = 86.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.extraSmall,
        color = if (selected) {
            VeilPalette.DeepBrass.copy(alpha = 0.34f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.42f)
        },
        border = BorderStroke(
            1.dp,
            if (selected) VeilPalette.Brass.copy(alpha = 0.88f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(paper, MaterialTheme.shapes.extraSmall)
                    .border(
                        1.dp,
                        ink.copy(alpha = 0.18f),
                        MaterialTheme.shapes.extraSmall
                    )
            ) {
                Text(
                    "Aa",
                    modifier = Modifier.align(Alignment.Center),
                    color = ink,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) VeilPalette.Moon
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Text(
                        "●",
                        color = VeilPalette.Brass,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
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
        fontFamily = when (fontFamily) {
            ReaderFontFamily.PUBLISHER -> null
            ReaderFontFamily.SERIF -> FontFamily.SERIF
            ReaderFontFamily.SANS_SERIF -> FontFamily.SANS_SERIF
            ReaderFontFamily.MONOSPACE -> FontFamily.MONOSPACE
            ReaderFontFamily.OPEN_DYSLEXIC -> FontFamily.OPEN_DYSLEXIC
            ReaderFontFamily.ACCESSIBLE_DFA -> FontFamily.ACCESSIBLE_DFA
            ReaderFontFamily.IA_WRITER_DUOSPACE -> FontFamily.IA_WRITER_DUOSPACE
        },
        fontSize = readiumFontSizeRatio(fontScale),
        lineHeight = lineHeight.coerceIn(1.1, 2.0),
        pageMargins = pageMargins.coerceIn(0.5, 2.0),
        paragraphSpacing = paragraphSpacing?.coerceIn(0.0, 2.0),
        paragraphIndent = paragraphIndent?.coerceIn(0.0, 3.0),
        letterSpacing = letterSpacing?.coerceIn(0.0, 0.2),
        wordSpacing = wordSpacing?.coerceIn(0.0, 1.0),
        typeScale = typeScale?.coerceIn(1.0, 2.0),
        textAlign = when (textAlignment) {
            ReaderTextAlignment.PUBLISHER -> null
            ReaderTextAlignment.START -> ReadiumTextAlign.START
            ReaderTextAlignment.JUSTIFY -> ReadiumTextAlign.JUSTIFY
            ReaderTextAlignment.CENTER -> ReadiumTextAlign.CENTER
        },
        columnCount = when (columnMode) {
            ReaderColumnMode.AUTO -> ColumnCount.AUTO
            ReaderColumnMode.ONE -> ColumnCount.ONE
            ReaderColumnMode.TWO -> ColumnCount.TWO
        },
        hyphens = hyphenation.toNullableBoolean(),
        ligatures = ligatures.toNullableBoolean(),
        textNormalization = textNormalization.toNullableBoolean(),
        scroll = scroll,
        publisherStyles = publisherStyles
    )
}

private fun ReaderPreferenceToggle.toNullableBoolean(): Boolean? =
    when (this) {
        ReaderPreferenceToggle.DEFAULT -> null
        ReaderPreferenceToggle.ON -> true
        ReaderPreferenceToggle.OFF -> false
    }

internal fun readiumFontSizeRatio(scale: Double): Double =
    (if (scale.isFinite()) scale else 1.0).coerceIn(0.75, 1.8)

internal fun readiumThemeColors(theme: ReaderTheme): Pair<Int, Int> = when (theme) {
    ReaderTheme.PAPER -> 0xFFE9DEC5.toInt() to 0xFF2A251F.toInt()
    ReaderTheme.SEPIA -> 0xFFE2D0AA.toInt() to 0xFF362E24.toInt()
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

