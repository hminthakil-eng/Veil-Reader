package com.veilreader.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.text.Html
import android.os.SystemClock
import android.view.ActionMode
import android.view.accessibility.AccessibilityManager
import android.view.View
import com.github.barteksc.pdfviewer.link.LinkHandler
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
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
import com.veilreader.app.BuildConfig
import com.veilreader.app.R
import com.veilreader.app.data.GameRepository
import com.veilreader.app.data.LocalLibraryRepository
import com.veilreader.app.data.OpenedPublication
import com.veilreader.app.data.toVeilPersistedJson
import com.veilreader.app.diagnostics.ReaderPerformanceMetrics
import com.veilreader.app.diagnostics.ReaderTrace
import com.veilreader.app.feature.VeilFeatureGates
import com.veilreader.app.feature.VeilRiskyFeature
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.BookReturnRitual
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderColumnMode
import com.veilreader.app.domain.ReaderDarkImageTreatment
import com.veilreader.app.domain.ReaderFontFamily
import com.veilreader.app.domain.ReaderFixedLayoutSpread
import com.veilreader.app.domain.ReaderFocusGuideMode
import com.veilreader.app.domain.ReaderFocusGuideSettings
import com.veilreader.app.domain.ReaderHardwareKeyMap
import com.veilreader.app.domain.ReaderPreferenceToggle
import com.veilreader.app.domain.ReaderTtsSettings
import com.veilreader.app.domain.deriveReadingPace
import com.veilreader.app.domain.estimateBookTimeRemaining
import com.veilreader.app.ui.reader.tts.ReaderTtsPreferences
import com.veilreader.app.ui.reader.tts.readerCanPlayForegroundTts
import com.veilreader.app.ui.reader.tts.readerCanCompleteTtsStart
import com.veilreader.app.ui.reader.tts.ReaderTtsState
import com.veilreader.app.ui.reader.tts.ReaderForegroundTtsCheckpointController
import com.veilreader.app.ui.reader.tts.ReaderTtsCheckpointStore
import com.veilreader.app.ui.reader.tts.ReaderTtsCheckpoint
import com.veilreader.app.ui.reader.tts.ReaderTtsServiceController
import com.veilreader.app.ui.reader.tts.ReaderTtsProblem
import com.veilreader.app.ui.reader.tts.ReaderTtsVoice
import com.veilreader.app.domain.ReaderReadingMode
import com.veilreader.app.domain.ReaderTapGrid
import com.veilreader.app.domain.ReaderTextAlignment
import com.veilreader.app.domain.ReadingContinuitySummary
import com.veilreader.app.domain.ReaderNavigationMode
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.domain.readerFocusGuideBand
import com.veilreader.app.ui.reader.ReaderHardwareKeyController
import com.veilreader.app.ui.reader.awaitReaderVisualNavigationDeparture
import com.veilreader.app.ui.reader.ReaderHardwareKeyHost
import com.veilreader.app.ui.reader.readerHardwareAccessibilityActive
import com.veilreader.app.ui.reader.ReaderLocatorEvent
import com.veilreader.app.ui.reader.ReaderNavigationIdentity
import com.veilreader.app.ui.reader.hasReachedObservedDestination
import com.veilreader.app.ui.reader.passageVisitAfterSettlement
import com.veilreader.app.ui.reader.ReaderNavigationTransactionGate
import com.veilreader.app.ui.reader.ReaderNavigationReason
import com.veilreader.app.ui.reader.ReaderNavigationCommitPolicy
import com.veilreader.app.ui.reader.ReaderNavigationSessionStateMachine
import com.veilreader.app.ui.reader.ReaderViewModel
import com.veilreader.app.ui.reader.shouldStartReaderIdentityJump
import com.veilreader.app.ui.reader.shouldStartReaderLinkJump
import com.veilreader.app.ui.reader.toReaderNavigationIdentity
import com.veilreader.app.ui.reader.readerEffectiveTargetHref
import com.veilreader.app.ui.reader.readerObservedLocatorEvent
import com.veilreader.app.ui.reader.tts.createReadiumReaderTtsSession
import com.veilreader.app.ui.reader.shouldCollectReaderLocator
import com.veilreader.app.ui.reader.shouldFlushStartupLocatorInBackground
import com.veilreader.app.ui.reader.shouldResumeReaderAfterOpen
import com.veilreader.app.ui.reader.awaitDurableReaderClose
import com.veilreader.app.ui.reader.material.MaterialPageEngineRollout
import com.veilreader.app.ui.reader.material.MaterialPageSensoryAction
import com.veilreader.app.ui.reader.material.MaterialPageSensorySink
import com.veilreader.app.ui.reader.material.MaterialPageTone
import com.veilreader.app.ui.reader.material.toVeilSensoryCue
import com.veilreader.app.ui.sensory.VeilMaterialPageSensoryCue
import com.veilreader.app.ui.sensory.VeilSensoryEvent
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilMotion
import com.veilreader.app.ui.theme.VeilMaterials
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSanctuary
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.withVeilTracking
import com.veilreader.app.ui.theme.sanctuaryPageMaterialFor
import com.veilreader.app.ui.theme.sanctuarySurfaceProfileFor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import org.readium.r2.shared.util.use
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.veilreader.app.ui.reader.runReaderRequestWithDeadline
import com.veilreader.app.ui.reader.deliverReaderResource
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.adapter.pdfium.navigator.PdfiumEngineProvider
import org.readium.adapter.pdfium.navigator.PdfiumDefaults
import org.readium.adapter.pdfium.navigator.PdfiumNavigatorFragment
import org.readium.adapter.pdfium.navigator.PdfiumPreferences
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.HyperlinkNavigator
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
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.ImageFilter
import org.readium.r2.navigator.preferences.Color as ReadiumColor
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.Spread
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.navigator.preferences.TextAlign as ReadiumTextAlign
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Layout
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.util.AbsoluteUrl

@OptIn(ExperimentalReadiumApi::class, ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ReaderScreen(
    opened: OpenedPublication,
    readerSessionInstanceId: String,
    library: LocalLibraryRepository,
    game: GameRepository,
    readerAppearance: ReaderAppearance,
    readerChromeAutoHideEnabled: Boolean = true,
    onReaderChromeAutoHideChange: (Boolean) -> Unit = {},
    readerTapGrid: ReaderTapGrid = ReaderTapGrid(),
    readerHardwareKeys: ReaderHardwareKeyMap = ReaderHardwareKeyMap(),
    focusGuide: ReaderFocusGuideSettings = ReaderFocusGuideSettings(),
    ttsSettings: ReaderTtsSettings = ReaderTtsSettings(),
    onTtsSettingsChange: (ReaderTtsSettings) -> Unit = {},
    onFocusGuideChange: (ReaderFocusGuideSettings) -> Unit = {},
    fixedLayoutSpread: ReaderFixedLayoutSpread = ReaderFixedLayoutSpread.AUTO,
    onReaderAppearanceChange: (ReaderAppearance) -> Unit,
    onFixedLayoutSpreadChange: (ReaderFixedLayoutSpread) -> Unit = {},
    entryContinuity: ReadingContinuitySummary? = null,
    returnRitual: BookReturnRitual? = null,
    initialReturnLocatorJson: String? = null,
    onSensoryEvent: (VeilSensoryEvent) -> Unit = {},
    onMaterialPageSensoryCue: (VeilMaterialPageSensoryCue) -> Unit = {},
    onClose: () -> Unit,
    onLocatorCheckpoint: (String) -> Unit = {}
) {
    val activity = requireNotNull(LocalActivity.current as? FragmentActivity) {
        "Veil Reader requires a FragmentActivity host."
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var readerLifecycleResumed by remember(lifecycle, readerSessionInstanceId) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    val scope = rememberCoroutineScope()
    val latestReaderSessionInstanceId = rememberUpdatedState(readerSessionInstanceId)
    val formatPercent = rememberVeilPercentFormatter()
    val focusGuideState = remember(opened.book.id, readerSessionInstanceId) {
        ReaderFocusGuideState(focusGuide)
    }
    LaunchedEffect(focusGuide, focusGuideState) {
        focusGuideState.acceptPersisted(focusGuide)
    }
    val activeFocusGuide = focusGuideState.value
    var entryVisible by rememberSaveable(opened.book.id, readerSessionInstanceId) { mutableStateOf(true) }
    var navigatorAttached by remember(opened.book.id, readerSessionInstanceId) { mutableStateOf(false) }
    var previousLocationJson by rememberSaveable(opened.book.id, readerSessionInstanceId) {
        mutableStateOf(initialReturnLocatorJson)
    }
    var readerSessionReady by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf(false)
    }
    val latestReaderSessionReady = rememberUpdatedState(readerSessionReady)

    LaunchedEffect(initialReturnLocatorJson, opened.book.id, readerSessionInstanceId) {
        if (previousLocationJson == null && !initialReturnLocatorJson.isNullOrBlank()) {
            previousLocationJson = initialReturnLocatorJson
        }
    }

    LaunchedEffect(opened.book.id, readerSessionInstanceId) {
        // Safety ceiling: a Reader failure must never leave an opaque transition permanently stuck.
        delay(2400)
        entryVisible = false
    }

    LaunchedEffect(
        navigatorAttached,
        readerSessionReady,
        opened.book.id,
        readerSessionInstanceId
    ) {
        if (navigatorAttached && readerSessionReady) {
            // Rare return rituals get only a slightly longer handoff; they never block reading.
            delay(if (returnRitual != null) 760 else 520)
            entryVisible = false
        }
    }
    val readerViewModel: ReaderViewModel = viewModel(
        key = "veil-reader-state",
        factory = remember(library, game) { ReaderViewModel.factory(library, game) }
    )
    val progressFlow = remember(readerViewModel, opened.book.id, opened.book.progress, readerSessionInstanceId) {
        readerViewModel.uiState
            .map { state ->
                if (state.bookId == opened.book.id) state.progress else opened.book.progress
            }
            .distinctUntilChanged()
    }
    val progress by progressFlow.collectAsStateWithLifecycle(initialValue = opened.book.progress)
    val progressFailureFlow = remember(readerViewModel, opened.book.id, readerSessionInstanceId) {
        readerViewModel.uiState.map { state ->
            state.bookId == opened.book.id && state.progressSaveFailed
        }.distinctUntilChanged()
    }
    val progressSaveFailed by progressFailureFlow.collectAsStateWithLifecycle(initialValue = false)

    var navigator by remember(opened.book.id, readerSessionInstanceId) { mutableStateOf<Navigator?>(null) }
    val latestNavigator = rememberUpdatedState(navigator)
    var pdfTapArbiter by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf<ReaderPdfTapArbiter?>(null)
    }
    var controlsVisible by rememberSaveable(opened.book.id, readerSessionInstanceId) { mutableStateOf(false) }
    var selectionModeActive by remember(opened.book.id, readerSessionInstanceId) { mutableStateOf(false) }
    val accessibilityManager = remember(activity) {
        activity.getSystemService(AccessibilityManager::class.java)
    }
    var touchExplorationEnabled by remember(accessibilityManager) {
        mutableStateOf(accessibilityManager?.isTouchExplorationEnabled == true)
    }
    DisposableEffect(accessibilityManager) {
        val manager = accessibilityManager
        if (manager == null) {
            onDispose { }
        } else {
            val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled ->
                touchExplorationEnabled = enabled
            }
            manager.addTouchExplorationStateChangeListener(listener)
            touchExplorationEnabled = manager.isTouchExplorationEnabled
            onDispose {
                manager.removeTouchExplorationStateChangeListener(listener)
            }
        }
    }
    val reducedMotion = LocalVeilReducedMotion.current
    val fixedLayoutPublication = remember(opened.book.id, opened.format, readerSessionInstanceId) {
        opened.format == BookFormat.EPUB &&
            opened.publication.metadata.layout == Layout.FIXED
    }
    var activeFixedLayoutSpread by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf(fixedLayoutSpread)
    }
    var presentedFixedLayoutSpread by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf(fixedLayoutSpread)
    }
    LaunchedEffect(fixedLayoutSpread, opened.book.id, readerSessionInstanceId) {
        if (activeFixedLayoutSpread != fixedLayoutSpread) {
            activeFixedLayoutSpread = fixedLayoutSpread
        }
    }
    val typesettingViewportWidth = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp().value.toDouble()
    }
    val typesettingAccessibilityScale = LocalDensity.current.fontScale.toDouble()
    fun effectiveAppearance(requested: ReaderAppearance): ReaderAppearance =
        applyMaterialPageRolloutToAppearance(
            appearance = AdaptiveTypesettingPolicy.resolve(
                appearance = effectiveReaderAppearanceForPublication(requested, fixedLayoutPublication),
                format = opened.format,
                fixedLayout = fixedLayoutPublication,
                viewportWidthDp = typesettingViewportWidth,
                accessibilityFontScale = typesettingAccessibilityScale
            ),
            format = opened.format,
            debugReview = BuildConfig.DEBUG,
            materialPageEnabled = MaterialPageEngineRollout.isEnabled()
        )
    val effectiveReaderAppearance = remember(
        readerAppearance,
        fixedLayoutPublication,
        typesettingViewportWidth,
        typesettingAccessibilityScale
    ) {
        effectiveAppearance(readerAppearance)
    }
    var presentedReaderAppearance by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf(effectiveReaderAppearance)
    }
    var acceptedReaderAppearance by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf(readerAppearance)
    }
    var rendererPreferencesSettling by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf(false)
    }
    var pendingEpubRelayoutSourceJson by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf<String?>(null)
    }
    var pendingEpubRelayoutAnchor by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf<Locator?>(null)
    }
    val readerModeHandoffState = remember(opened.book.id, readerSessionInstanceId) {
        ReaderModeHandoffState()
    }
    val publicationLanguage = remember(opened.book.id, opened.book.language, readerSessionInstanceId) {
        opened.book.language
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: opened.publication.metadata.languages.firstOrNull()
    }

    val latestReducedMotion = rememberUpdatedState(reducedMotion)
    val selectionHighlightLabel =
        stringResource(R.string.reader_selection_highlight)
    val selectionNoteLabel =
        stringResource(R.string.reader_selection_note)
    val selectionLookupLabel = stringResource(R.string.reader_selection_lookup)
    val lookupFailedMessage = stringResource(R.string.reader_lookup_failed)
    val highlightedMessage = stringResource(R.string.reader_highlighted)
    val alreadyHighlightedMessage = stringResource(R.string.reader_already_highlighted)
    val passageSaveFailedMessage = stringResource(R.string.reader_passage_save_failed)
    val previousLocationFailedMessage = stringResource(R.string.reader_previous_location_failed)
    val closeStorageFailedMessage = stringResource(R.string.reader_close_storage_failed)
    val readerOpenFailedMessage = stringResource(R.string.notice_open_failed)
    val bookmarkSavedMessage = stringResource(R.string.reader_bookmark_saved)
    val bookmarkSaveFailedMessage = stringResource(R.string.reader_bookmark_save_failed)
    val bookmarkDeleteFailedMessage = stringResource(R.string.reader_bookmark_delete_failed)
    val bookmarkDuplicateMessage = stringResource(R.string.reader_bookmark_duplicate)
    val noteSavedMessage = stringResource(R.string.reader_note_saved)
    val noteSaveFailedMessage = stringResource(R.string.reader_note_save_failed)
    val savedLocationFailedMessage = stringResource(R.string.reader_saved_location_failed)
    val chapterFailedMessage = stringResource(R.string.reader_chapter_failed)
    val externalLinkFailedMessage =
        stringResource(R.string.reader_external_link_failed)
    val focusGuideOnMessage = stringResource(R.string.reader_focus_on)
    val focusGuideOffMessage = stringResource(R.string.reader_focus_off)
    val focusGuideStateDescription = stringResource(
        when (activeFocusGuide.mode) {
            ReaderFocusGuideMode.OFF -> R.string.settings_focus_guide_off
            ReaderFocusGuideMode.WINDOW -> R.string.settings_focus_guide_window
            ReaderFocusGuideMode.LINE -> R.string.settings_focus_guide_line
        }
    )
    val imageViewerFailedMessage =
        stringResource(R.string.reader_image_viewer_failed)
    val imageViewerTimeoutMessage = stringResource(R.string.reader_image_viewer_timeout)
    val imageViewerTooLargeMessage = stringResource(R.string.reader_image_viewer_too_large)
    val appearanceApplyFailedMessage =
        stringResource(R.string.reader_appearance_apply_failed)
    val boundaryBeginningMessage =
        stringResource(R.string.reader_boundary_beginning)
    val boundaryEndMessage =
        stringResource(R.string.reader_boundary_end)
    var readerMessage by remember(readerSessionInstanceId) { mutableStateOf<String?>(null) }
    val progressSaveFailedMessage = stringResource(R.string.reader_progress_save_failed)
    LaunchedEffect(progressSaveFailed, opened.book.id, readerSessionInstanceId) {
        if (progressSaveFailed) {
            readerMessage = progressSaveFailedMessage
        }
    }
    var paperFailureNotice by remember(readerSessionInstanceId) {
        mutableStateOf<ReaderPaperFailureNotice?>(null)
    }
    val paperSnapshotFailedMessage = stringResource(R.string.reader_paper_snapshot_failed)
    val paperPresentationFailedMessage = stringResource(R.string.reader_paper_presentation_failed)
    val paperRendererUnavailableMessage = stringResource(R.string.reader_paper_renderer_unavailable)
    val paperUseSlideLabel = stringResource(R.string.reader_paper_use_slide)
    val paperCurlState = remember(opened.book.id, readerSessionInstanceId) { PaperCurlState() }
    val performanceRootView = activity.window.decorView
    LaunchedEffect(
        performanceRootView,
        presentedReaderAppearance.navigationMode,
        paperCurlState.performancePhase,
        paperCurlState.rendererStatus
    ) {
        ReaderPerformanceMetrics.putState(
            root = performanceRootView,
            key = ReaderPerformanceMetrics.READER_MODE_KEY,
            value = presentedReaderAppearance.navigationMode.name
        )
        if (presentedReaderAppearance.navigationMode == ReaderNavigationMode.PAPER_CURL) {
            ReaderPerformanceMetrics.putState(
                root = performanceRootView,
                key = ReaderPerformanceMetrics.PAPER_PHASE_KEY,
                value = paperCurlState.performancePhase.name
            )
            ReaderPerformanceMetrics.putState(
                root = performanceRootView,
                key = ReaderPerformanceMetrics.PAPER_GPU_KEY,
                value = paperCurlState.rendererStatus.name
            )
        } else {
            ReaderPerformanceMetrics.removeState(
                root = performanceRootView,
                key = ReaderPerformanceMetrics.PAPER_PHASE_KEY
            )
            ReaderPerformanceMetrics.removeState(
                root = performanceRootView,
                key = ReaderPerformanceMetrics.PAPER_GPU_KEY
            )
        }
    }
    DisposableEffect(performanceRootView, readerSessionInstanceId) {
        onDispose {
            ReaderPerformanceMetrics.removeState(
                root = performanceRootView,
                key = ReaderPerformanceMetrics.READER_MODE_KEY
            )
            ReaderPerformanceMetrics.removeState(
                root = performanceRootView,
                key = ReaderPerformanceMetrics.PAPER_PHASE_KEY
            )
            ReaderPerformanceMetrics.removeState(
                root = performanceRootView,
                key = ReaderPerformanceMetrics.PAPER_GPU_KEY
            )
            ReaderPerformanceMetrics.removeState(
                root = performanceRootView,
                key = ReaderPerformanceMetrics.PAPER_WORK_KEY
            )
        }
    }
    val latestMaterialPageSensoryCue =
        rememberUpdatedState(onMaterialPageSensoryCue)
    DisposableEffect(paperCurlState) {
        val sink = MaterialPageSensorySink { cue ->
            // Reader already owns the generic terminal-boundary feedback path.
            // Suppress only that duplicate; lift/cancel/complete stay material-specific.
            if (cue.action != MaterialPageSensoryAction.BOUNDARY) {
                latestMaterialPageSensoryCue.value(cue.toVeilSensoryCue())
            }
        }
        paperCurlState.materialEngine.setSensorySink(sink)
        onDispose {
            paperCurlState.materialEngine.setSensorySink(null)
        }
    }
    var paperInputListener by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf<PaperCurlInputListener?>(null)
    }
    val slidePageState = remember(opened.book.id, readerSessionInstanceId) { SlidePageState() }
    var slideInputListener by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf<SlideNavigationInputListener?>(null)
    }
    LaunchedEffect(paperInputListener, paperCurlState.rendererStatus, paperCurlState.active) {
        if (paperCurlState.active &&
            paperCurlState.rendererStatus != com.veilreader.app.ui.reader.material.GpuMaterialPageRendererStatus.READY) {
            // Context loss/failure must retire an uncommitted preview before a replacement
            // renderer or subsequent input can acquire its locator/visual ownership.
            paperInputListener?.forceCancelPendingTurn()
        }
    }
    val navigationTransactionGate = remember(opened.book.id, readerSessionInstanceId) {
        ReaderNavigationTransactionGate()
    }
    val navigationSessionState = remember(opened.book.id, readerSessionInstanceId) {
        ReaderNavigationSessionStateMachine(
            opened.initialLocator?.toVeilPersistedJson(opened.format)
        )
    }
    DisposableEffect(navigationTransactionGate) {
        onDispose { navigationTransactionGate.reset() }
    }

    suspend fun settlePagePreviewsBeforeProgrammaticNavigation(): Boolean {
        val paperHadPendingTurn =
            paperInputListener?.hasPendingTurn() == true
        val slideHadPendingTurn =
            slideInputListener?.hasPendingTurn() == true

        // Cancellation acknowledgement alone is insufficient: a listener may still own
        // a preview when it reports completion. Recheck both listeners after cancellation.
        if (paperHadPendingTurn) paperInputListener?.cancelPendingTurnAndAwait()
        if (slideHadPendingTurn) slideInputListener?.cancelPendingTurnAndAwait()
        val paperSettled = paperInputListener?.hasPendingTurn() != true
        val slideSettled = slideInputListener?.hasPendingTurn() != true

        if (!paperSettled || !slideSettled) {
            paperInputListener?.forceCancelPendingTurn()
            slideInputListener?.forceCancelPendingTurn()
            ReaderTrace.event(
                "navigation_jump_blocked_unsettled_preview",
                bookId = opened.book.id,
                sessionId = readerSessionInstanceId
            )
            return false
        }
        return true
    }

    fun beginProgrammaticNavigation(
        originLocatorJson: String?,
        targetIdentity: ReaderNavigationIdentity? = null,
        targetHref: String? = null,
        passageVisitLocatorJson: String? = null,
        reason: ReaderNavigationReason,
        expectedPdfPage: Int? = null
    ): Long {
        val transaction = navigationTransactionGate.begin(
            originLocatorJson = originLocatorJson,
            nowElapsedMs = SystemClock.elapsedRealtime(),
            targetIdentity = targetIdentity,
            targetHref = targetHref,
            passageVisitLocatorJson = passageVisitLocatorJson,
            reason = reason,
            expectedPdfPage = expectedPdfPage,
            originPdfPage = if (expectedPdfPage != null) {
                latestNavigator.value?.currentLocator?.value?.let(::pdfPageNumber)
            } else null
        )
        ReaderTrace.event(
            "navigation_jump_requested",
            bookId = opened.book.id,
            sessionId = readerSessionInstanceId,
            details = "token=${transaction.token}"
        )
        return transaction.token
    }

    fun cancelProgrammaticNavigation(token: Long) {
        navigationTransactionGate.cancel(token)
        ReaderTrace.event(
            "navigation_jump_cancelled",
            bookId = opened.book.id,
            sessionId = readerSessionInstanceId,
            details = "token=$token"
        )
    }

    var boundaryPulseSide by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf<PaperCurlSide?>(null)
    }
    var boundaryPulseSerial by remember(opened.book.id, readerSessionInstanceId) { mutableIntStateOf(0) }
    var lastBoundaryFeedbackAtMillis by remember(opened.book.id, readerSessionInstanceId) {
        mutableLongStateOf(0L)
    }
    val boundaryPulseAlpha = remember(opened.book.id, readerSessionInstanceId) { Animatable(0f) }

    fun emitBoundaryFeedback(side: PaperCurlSide) {
        val now = SystemClock.uptimeMillis()
        if (
            !shouldEmitReaderBoundaryFeedback(
                nowMillis = now,
                lastEmissionMillis = lastBoundaryFeedbackAtMillis
            )
        ) {
            return
        }
        lastBoundaryFeedbackAtMillis = now
        boundaryPulseSide = side
        boundaryPulseSerial += 1
        onSensoryEvent(VeilSensoryEvent.BOUNDARY)
        if (touchExplorationEnabled) {
            val progression =
                (navigator as? OverflowableNavigator)
                    ?.overflow
                    ?.value
                    ?.readingProgression
                    ?: ReadingProgression.LTR
            readerMessage = when (readerBoundaryKind(side, progression)) {
                ReaderBoundaryKind.BEGINNING -> boundaryBeginningMessage
                ReaderBoundaryKind.END -> boundaryEndMessage
            }
        }
    }

    LaunchedEffect(boundaryPulseSerial, opened.book.id, readerSessionInstanceId) {
        if (boundaryPulseSerial <= 0) return@LaunchedEffect
        boundaryPulseAlpha.snapTo(0f)
        boundaryPulseAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(if (reducedMotion) 1 else 52)
        )
        boundaryPulseAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(if (reducedMotion) 70 else 170)
        )
        boundaryPulseSide = null
    }
    LaunchedEffect(touchExplorationEnabled, opened.book.id, readerSessionInstanceId) {
        if (touchExplorationEnabled) {
            controlsVisible = true
            val restoredPaper =
                paperInputListener?.cancelPendingTurnAndAwait() == true
            if (!restoredPaper && paperCurlState.active) paperCurlState.clearImmediately()
            val restoredSlide =
                slideInputListener?.cancelPendingTurnAndAwait() == true
            if (!restoredSlide && slidePageState.active) slidePageState.clearImmediately()
        }
    }
    var showAppearance by rememberSaveable(opened.book.id, readerSessionInstanceId) { mutableStateOf(false) }
    var appearanceCloseJob by remember(opened.book.id, readerSessionInstanceId) { mutableStateOf<Job?>(null) }
    var showPdfZoom by rememberSaveable(opened.book.id, readerSessionInstanceId) { mutableStateOf(false) }

    fun closeAppearanceAfterRendererSettles(
        expectedAppearance: ReaderAppearance? = null
    ) {
        appearanceCloseJob?.cancel()
        val normalizedExpected = expectedAppearance?.let(::effectiveAppearance)
        appearanceCloseJob = scope.launch {
            // Give a final slider commit one frame to propagate into the parent state before
            // deciding whether the renderer is settled. Then keep the chamber above the renderer
            // until the exact requested appearance is presented, with a defensive ceiling so a
            // renderer failure can never trap the user inside Settings.
            delay(VeilMotion.FRAME_SETTLE_MS)
            val startedAt = SystemClock.elapsedRealtime()
            while (
                shouldAwaitReaderAppearanceClose(
                    rendererPreferencesSettling = rendererPreferencesSettling,
                    presented = presentedReaderAppearance,
                    expected = normalizedExpected
                ) &&
                SystemClock.elapsedRealtime() - startedAt <
                    READER_APPEARANCE_CLOSE_TIMEOUT_MS
            ) {
                delay(VeilMotion.FRAME_SETTLE_MS)
            }
            delay(VeilMotion.FRAME_SETTLE_MS)
            if (
                !readerAsyncResultBelongsToSession(
                    currentSessionInstanceId = latestReaderSessionInstanceId.value,
                    expectedSessionInstanceId = readerSessionInstanceId
                )
            ) {
                return@launch
            }
            showAppearance = false
            appearanceCloseJob = null
        }
    }
    val latestAppearance = rememberUpdatedState(presentedReaderAppearance)
    LaunchedEffect(
        navigator,
        readerSessionReady,
        presentedReaderAppearance,
        reducedMotion,
        paperCurlState.active,
        paperCurlState.snapshotSourceRevision,
        readerLifecycleResumed,
        readerSessionInstanceId
    ) {
        if (
            !readerSessionReady || !readerLifecycleResumed ||
            opened.format != BookFormat.EPUB ||
            presentedReaderAppearance.navigationMode != ReaderNavigationMode.PAPER_CURL ||
            !shouldCapturePaperTurnSnapshot(reducedMotion)
        ) {
            paperCurlState.releaseBufferIfIdle()
            return@LaunchedEffect
        }
        if (paperCurlState.active) return@LaunchedEffect

        val nav = navigator as? OverflowableNavigator ?: return@LaunchedEffect
        // Prepare an exact Readium source snapshot while the reader is idle, after
        // the visible WebView reports visual readiness. begin() consumes it only if
        // revision + viewport + ping-pong buffer ownership still match; otherwise
        // the gesture falls back to a fresh immediate capture.
        delay(VeilMotion.FRAME_SETTLE_MS * 2)
        if (
            !paperCurlState.active && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
            readerAsyncResultBelongsToSession(
                currentSessionInstanceId = latestReaderSessionInstanceId.value,
                expectedSessionInstanceId = readerSessionInstanceId
            )
        ) {
            paperCurlState.prepareSnapshot(nav.publicationView) {
                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                    readerAsyncResultBelongsToSession(
                        currentSessionInstanceId = latestReaderSessionInstanceId.value,
                        expectedSessionInstanceId = readerSessionInstanceId
                    )
            }
        }
    }
    LaunchedEffect(
        navigator,
        readerSessionReady,
        presentedReaderAppearance,
        reducedMotion,
        slidePageState.active,
        readerLifecycleResumed,
        readerSessionInstanceId
    ) {
        if (
            !readerSessionReady || !readerLifecycleResumed ||
            opened.format != BookFormat.EPUB ||
            presentedReaderAppearance.navigationMode != ReaderNavigationMode.SLIDE ||
            reducedMotion
        ) {
            slidePageState.releaseBufferIfIdle()
            return@LaunchedEffect
        }
        if (slidePageState.active) return@LaunchedEffect

        val nav = navigator as? OverflowableNavigator ?: return@LaunchedEffect
        delay(VeilMotion.FRAME_SETTLE_MS * 2)
        if (
            !slidePageState.active && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
            readerAsyncResultBelongsToSession(
                currentSessionInstanceId = latestReaderSessionInstanceId.value,
                expectedSessionInstanceId = readerSessionInstanceId
            )
        ) {
            slidePageState.prepareBuffer(nav.publicationView)
        }
    }

    val latestTapGrid = rememberUpdatedState(readerTapGrid)
    val latestHardwareKeys = rememberUpdatedState(readerHardwareKeys)
    var showTts by rememberSaveable(opened.book.id, readerSessionInstanceId) { mutableStateOf(false) }
    var ttsStartJob by remember(readerSessionInstanceId) { mutableStateOf<Job?>(null) }
    var ttsStartSerial by remember(readerSessionInstanceId) { mutableIntStateOf(0) }
    var ttsStartPending by remember(readerSessionInstanceId) { mutableStateOf(false) }
    var ttsStartFailed by remember(readerSessionInstanceId) { mutableStateOf(false) }
    val speechSettingsState = remember(readerSessionInstanceId) { ReaderTtsSettingsState(ttsSettings) }
    LaunchedEffect(ttsSettings) { speechSettingsState.acceptPersisted(ttsSettings) }
    val latestTtsSettings = rememberUpdatedState(speechSettingsState.value)
    val readingPaceProfiles by library.readingPaceProfiles.collectAsStateWithLifecycle()
    val readingPace = remember(readingPaceProfiles, opened.book.id) {
        deriveReadingPace(readingPaceProfiles[opened.book.id])
    }
    val timeRemainingEstimate = remember(opened.book.totalPages, progress, readingPace) {
        estimateBookTimeRemaining(opened.book.totalPages, progress, readingPace)
    }
    var showNotebook by rememberSaveable(opened.book.id, readerSessionInstanceId) { mutableStateOf(false) }
    var showReaderSearch by remember(opened.book.id, readerSessionInstanceId) { mutableStateOf(false) }

    LaunchedEffect(readerAppearance, opened.book.id, readerSessionInstanceId) {
        ReaderTrace.event(
            "appearance_observed",
            bookId = opened.book.id,
            sessionId = readerViewModel.traceSessionId(),
            details = "theme=${readerAppearance.theme} scroll=${readerAppearance.scroll} pageTurn=${readerAppearance.pageTurnStyle} brightness=${readerAppearance.screenBrightness ?: "system"}"
        )
    }
    ReaderBrightnessEffect(activity, readerAppearance.screenBrightness)

    LaunchedEffect(
        readerSessionInstanceId,
        controlsVisible,
        showNotebook,
        showAppearance,
        showPdfZoom,
        selectionModeActive,
        touchExplorationEnabled,
        readerChromeAutoHideEnabled
    ) {
        if (
            shouldAutoHideReaderChrome(
                controlsVisible = controlsVisible,
                showNotebook = showNotebook,
                showAppearance = showAppearance,
                showPdfZoom = showPdfZoom,
                selectionModeActive = selectionModeActive,
                touchExplorationEnabled = touchExplorationEnabled,
                autoHideEnabled = readerChromeAutoHideEnabled
            )
        ) {
            delay(VeilSanctuary.chromeAutoHideMillis)
            controlsVisible = false
        }
    }

    LaunchedEffect(
        readerSessionInstanceId,
        effectiveReaderAppearance.scroll,
        effectiveReaderAppearance.pageTurnStyle
    ) {
        if (
            effectiveReaderAppearance.scroll ||
            effectiveReaderAppearance.pageTurnStyle != PageTurnStyle.PAPER
        ) {
            val cancelingDrag =
                paperInputListener?.cancelPendingTurnAndAwait() == true
            if (!cancelingDrag && paperCurlState.active) {
                paperCurlState.clear()
            }
            paperCurlState.releaseBufferIfIdle()
        }
        if (
            effectiveReaderAppearance.scroll ||
            effectiveReaderAppearance.pageTurnStyle != PageTurnStyle.SLIDE
        ) {
            val cancelingSlide =
                slideInputListener?.cancelPendingTurnAndAwait() == true
            if (!cancelingSlide && slidePageState.active) {
                slidePageState.clear()
            }
            slidePageState.releaseBufferIfIdle()
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
    val bookPassageVisitsFlow = remember(library, opened.book.id) {
        library.passageVisits
            .map { items -> items.filter { it.bookId == opened.book.id } }
            .distinctUntilChanged()
    }
    val bookHighlights by bookHighlightsFlow.collectAsStateWithLifecycle(
        initialValue = library.highlightsFor(opened.book.id)
    )
    val bookBookmarks by bookBookmarksFlow.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )
    val bookPassageVisits by bookPassageVisitsFlow.collectAsStateWithLifecycle(
        initialValue = library.passageVisitsFor(opened.book.id)
    )
    var footnote by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf<ReaderFootnote?>(null)
    }
    var imageViewer by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf<ReaderImageContent?>(null)
    }
    var imageLoading by remember(opened.book.id, readerSessionInstanceId) { mutableStateOf(false) }
    var imageLoadJob by remember(opened.book.id, readerSessionInstanceId) { mutableStateOf<Job?>(null) }
    var imageLoadSerial by remember(opened.book.id, readerSessionInstanceId) { mutableIntStateOf(0) }
    fun cancelImageLoad() {
        imageLoadSerial += 1
        imageLoadJob?.cancel()
        imageLoadJob = null
        imageLoading = false
    }
    var closeInFlight by remember(opened.book.id, readerSessionInstanceId) { mutableStateOf(false) }
    var readerViewportSize by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf(IntSize.Zero)
    }
    var viewportRelayoutPending by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf(false)
    }
    var viewportRelayoutJob by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf<Job?>(null)
    }

    DisposableEffect(imageViewer) {
        val ownedImage = imageViewer
        onDispose { ownedImage?.close() }
    }
    DisposableEffect(readerSessionInstanceId) {
        onDispose {
            appearanceCloseJob?.cancel()
            viewportRelayoutJob?.cancel()
            imageLoadSerial += 1
            imageLoadJob?.cancel()
        }
    }

    var pendingNoteHighlightId by rememberSaveable(opened.book.id, readerSessionInstanceId) {
        mutableStateOf<String?>(null)
    }
    var pendingNoteLocatorJson by rememberSaveable(opened.book.id, readerSessionInstanceId) {
        mutableStateOf<String?>(null)
    }
    var pendingNoteQuote by rememberSaveable(opened.book.id, readerSessionInstanceId) {
        mutableStateOf("")
    }
    var pendingNoteText by rememberSaveable(opened.book.id, readerSessionInstanceId) {
        mutableStateOf("")
    }
    var noteSaving by remember(readerSessionInstanceId) { mutableStateOf(false) }

    fun clearPendingSelectionNoteDraft() {
        pendingNoteHighlightId = null
        pendingNoteLocatorJson = null
        pendingNoteQuote = ""
        pendingNoteText = ""
    }

    fun dismissPendingSelectionNote() {
        if (!noteSaving) clearPendingSelectionNoteDraft()
    }

    var locationTitle by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf(opened.book.currentChapter.takeUnless { it == "Not started" }.orEmpty())
    }
    var currentLocationHref by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf(opened.initialLocator?.href?.toString())
    }
    val snackbarHostState = remember(readerSessionInstanceId) { SnackbarHostState() }
    val snackbarBottom by animateDpAsState(
        targetValue = if (controlsVisible) 104.dp else 16.dp,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            tween(VeilMotion.READER_SNACKBAR_SHIFT_MS)
        },
        label = "reader-snackbar-offset"
    )

    val selectionActionModeCallback = remember(
        opened.book.id,
        readerSessionInstanceId,
        library,
        readerViewModel,
        scope,
        selectionHighlightLabel,
        selectionNoteLabel,
        selectionLookupLabel,
        lookupFailedMessage,
        highlightedMessage,
        alreadyHighlightedMessage,
        passageSaveFailedMessage
    ) {
        ReaderSelectionActionModeCallback(
            coroutineScope = scope,
            navigatorProvider = { navigator as? SelectableNavigator },
            highlightLabel = selectionHighlightLabel,
            noteLabel = selectionNoteLabel,
            lookupLabel = selectionLookupLabel,
            onModeChanged = { active ->
                selectionModeActive = active
                if (active) controlsVisible = true
            },
            onAction = onAction@{ action, locator, quote ->
                if (!readerAsyncResultBelongsToSession(
                        currentSessionInstanceId = latestReaderSessionInstanceId.value,
                        expectedSessionInstanceId = readerSessionInstanceId
                    ) || closeInFlight || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                ) return@onAction
                if (action == ReaderSelectionAction.LOOKUP) {
                    readerViewModel.onUserInteraction(readerSessionInstanceId)
                    if (!launchReaderLookup(activity, quote, selectionLookupLabel)) {
                        readerMessage = lookupFailedMessage
                    }
                    return@onAction
                }
                try {
                    val locatorJson = locator.toVeilPersistedJson(opened.format)
                    val existing = library.highlightsFor(opened.book.id).firstOrNull {
                        it.locatorJson == locatorJson && it.quote == quote
                    }
                    when (action) {
                        ReaderSelectionAction.LOOKUP -> Unit
                        ReaderSelectionAction.HIGHLIGHT -> {
                            val committed = library.commitSelectionHighlight(
                                bookId = opened.book.id, quote = quote, locatorJson = locatorJson
                            )
                            if (!readerAsyncResultBelongsToSession(
                                    currentSessionInstanceId = latestReaderSessionInstanceId.value,
                                    expectedSessionInstanceId = readerSessionInstanceId
                                )) return@onAction
                            val isNew = committed.created
                            if (isNew) {
                                readerViewModel.onHighlightAdded(readerSessionInstanceId)
                                onSensoryEvent(VeilSensoryEvent.MARK)
                            } else {
                                readerViewModel.onUserInteraction(readerSessionInstanceId)
                            }
                            readerMessage =
                                if (isNew) highlightedMessage else alreadyHighlightedMessage
                        }
                        ReaderSelectionAction.NOTE -> {
                            readerViewModel.onUserInteraction(readerSessionInstanceId)
                            pendingNoteHighlightId = existing?.id
                            pendingNoteLocatorJson = locatorJson
                            pendingNoteQuote = quote
                            pendingNoteText = existing?.note.orEmpty()
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    readerMessage = passageSaveFailedMessage
                }
            }
        )
    }

    LaunchedEffect(opened.book.id, readerSessionInstanceId) {
        readerSessionReady = false
        var handedOff = false
        ReaderTrace.event(
            "reader_startup_handshake_started",
            bookId = opened.book.id,
            sessionId = readerSessionInstanceId
        )
        try {
            val prepared = readerViewModel.openBook(
                bookId = opened.book.id,
                initialProgress = opened.book.progress,
                openInstanceId = readerSessionInstanceId
            )
            if (!prepared) return@LaunchedEffect

            if (
                !readerAsyncResultBelongsToSession(
                    currentSessionInstanceId = latestReaderSessionInstanceId.value,
                    expectedSessionInstanceId = readerSessionInstanceId
                )
            ) {
                return@LaunchedEffect
            }

            if (!readerViewModel.confirmOpen(readerSessionInstanceId)) {
                return@LaunchedEffect
            }

            handedOff = true
            readerSessionReady = true
            val lifecycleResumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            ReaderTrace.event(
                "reader_startup_handshake_ready",
                bookId = opened.book.id,
                sessionId = readerSessionInstanceId,
                details = "lifecycleResumed=$lifecycleResumed navigatorAttached=$navigatorAttached"
            )
            if (
                shouldResumeReaderAfterOpen(
                    sessionReady = true,
                    lifecycleResumed = lifecycleResumed
                )
            ) {
                readerViewModel.onResume(readerSessionInstanceId)
            } else {
                // The app may have backgrounded while Room hydration was suspended. Reconcile the
                // durable session immediately; the first locator commit performs a second barrier.
                readerViewModel.onPause(readerSessionInstanceId)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            ReaderTrace.event(
                "reader_startup_handshake_failed",
                bookId = opened.book.id,
                sessionId = readerSessionInstanceId,
                details = "error=${error::class.java.simpleName}"
            )
            readerMessage = readerOpenFailedMessage
        } finally {
            if (!handedOff) {
                readerViewModel.cancelOpen(readerSessionInstanceId)
            }
        }
    }

    LaunchedEffect(readerMessage, readerSessionInstanceId) {
        val message = readerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = message,
            withDismissAction = true,
            duration = SnackbarDuration.Short
        )
        if (
            readerAsyncResultBelongsToSession(
                currentSessionInstanceId = latestReaderSessionInstanceId.value,
                expectedSessionInstanceId = readerSessionInstanceId
            ) &&
            readerMessage == message
        ) {
            readerMessage = null
        }
    }

    val transitionUnavailableReason = readerTransitionUnavailableReason(
        appearance = readerAppearance,
        format = opened.format,
        fixedLayout = fixedLayoutPublication,
        materialPageEnabled = MaterialPageEngineRollout.isEnabled()
    )
    val transitionUnavailableMessage = when (transitionUnavailableReason) {
        ReaderTransitionUnavailableReason.PAPER_ROLLOUT_DISABLED ->
            stringResource(R.string.reader_paper_rollout_unavailable)
        ReaderTransitionUnavailableReason.FIXED_LAYOUT_LEAF_UNSUPPORTED ->
            stringResource(R.string.reader_fixed_layout_transition_unavailable)
        null -> null
    }
    var announcedTransitionReason by remember(opened.book.id, readerSessionInstanceId) {
        mutableStateOf<ReaderTransitionUnavailableReason?>(null)
    }
    LaunchedEffect(transitionUnavailableReason, readerSessionReady, closeInFlight,
        readerSessionInstanceId) {
        val reason = transitionUnavailableReason ?: return@LaunchedEffect
        if (!readerSessionReady || closeInFlight || announcedTransitionReason == reason) {
            return@LaunchedEffect
        }
        announcedTransitionReason = reason
        snackbarHostState.showSnackbar(
            message = transitionUnavailableMessage ?: return@LaunchedEffect,
            withDismissAction = true,
            duration = SnackbarDuration.Long
        )
    }

    val latestRequestedReaderAppearance = rememberUpdatedState(readerAppearance)
    val latestReaderAppearanceChange = rememberUpdatedState(onReaderAppearanceChange)

    fun reportPaperVisualFailure(failure: PaperTurnVisualFailure) {
        if (closeInFlight || !latestReaderSessionReady.value ||
            !readerAsyncResultBelongsToSession(
                currentSessionInstanceId = latestReaderSessionInstanceId.value,
                expectedSessionInstanceId = readerSessionInstanceId
            ) || !shouldUsePaperCurlNavigation(opened.format,
                latestAppearance.value.scroll, latestAppearance.value.pageTurnStyle) ||
            latestReducedMotion.value) return
        // Repeated failures of the same kind coalesce while their notice is displayed/queued.
        if (paperFailureNotice?.failure != failure) {
            paperFailureNotice = ReaderPaperFailureNotice(failure)
        }
    }

    LaunchedEffect(paperCurlState.rendererStatus, presentedReaderAppearance.navigationMode,
        reducedMotion, readerSessionReady, closeInFlight) {
        val failure = paperRendererFailureNotice(paperCurlState.rendererStatus)
        if (failure == null) {
            if (paperCurlState.rendererStatus ==
                com.veilreader.app.ui.reader.material.GpuMaterialPageRendererStatus.READY &&
                paperFailureNotice?.failure == PaperTurnVisualFailure.RENDERER_UNAVAILABLE) {
                paperFailureNotice = null
            }
            return@LaunchedEffect
        }
        if (!MaterialPageEngineRollout.isEnabled() || reducedMotion || closeInFlight ||
            presentedReaderAppearance.navigationMode != ReaderNavigationMode.PAPER_CURL) {
            return@LaunchedEffect
        }
        // Ignore transient recovery states; a status/mode change cancels this delay.
        delay(700L)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            reportPaperVisualFailure(failure)
        }
    }

    LaunchedEffect(paperFailureNotice, presentedReaderAppearance.navigationMode,
        reducedMotion, readerSessionReady, closeInFlight) {
        val notice = paperFailureNotice ?: return@LaunchedEffect
        try {
            if (closeInFlight || !readerSessionReady || reducedMotion ||
                presentedReaderAppearance.navigationMode != ReaderNavigationMode.PAPER_CURL) {
                return@LaunchedEffect
            }
            val message = when (notice.failure) {
                PaperTurnVisualFailure.SNAPSHOT -> paperSnapshotFailedMessage
                PaperTurnVisualFailure.PRESENTATION -> paperPresentationFailedMessage
                PaperTurnVisualFailure.RENDERER_UNAVAILABLE -> paperRendererUnavailableMessage
            }
            val result = snackbarHostState.showSnackbar(message = message,
                actionLabel = paperUseSlideLabel, withDismissAction = true,
                duration = SnackbarDuration.Long)
            val currentAppearance = latestRequestedReaderAppearance.value
            if (result == SnackbarResult.ActionPerformed && !closeInFlight &&
                latestReaderSessionReady.value && !latestReducedMotion.value &&
                readerAsyncResultBelongsToSession(
                    currentSessionInstanceId = latestReaderSessionInstanceId.value,
                    expectedSessionInstanceId = readerSessionInstanceId
                ) && shouldUsePaperCurlNavigation(opened.format,
                    currentAppearance.scroll, currentAppearance.pageTurnStyle)) {
                readerViewModel.onUserInteraction(readerSessionInstanceId)
                latestReaderAppearanceChange.value(currentAppearance.withPageTurnStyle(PageTurnStyle.SLIDE))
            }
        } finally {
            if (paperFailureNotice === notice) paperFailureNotice = null
        }
    }

    val latestTtsCanPlay = rememberUpdatedState {
        readerCanPlayForegroundTts(
            readerReady = latestReaderSessionReady.value,
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
            closeInFlight = closeInFlight,
            preferencesSettling = rendererPreferencesSettling,
            selectionActive = selectionModeActive,
            blockingOverlayVisible = showNotebook || showAppearance || showPdfZoom ||
                footnote != null || imageLoading || imageViewer != null,
            noteLocatorJson = pendingNoteLocatorJson,
            accessibilityActive = readerHardwareAccessibilityActive(accessibilityManager)
        )
    }
    val backgroundTtsEnabled = remember {
        VeilFeatureGates.enabled(
            VeilRiskyFeature.BACKGROUND_TTS,
            debugReview = BuildConfig.DEBUG && !BuildConfig.FORGE_QA
        )
    }
    val foregroundTtsCheckpoint = remember(opened.book.id, readerSessionInstanceId, backgroundTtsEnabled) {
        if (backgroundTtsEnabled) null else {
            val store = ReaderTtsCheckpointStore(activity.applicationContext)
            ReaderForegroundTtsCheckpointController(opened.book.id, store::read, store::save)
        }
    }
    val ttsSession = remember(
        opened.book.id,
        readerSessionInstanceId,
        lifecycle,
        backgroundTtsEnabled
    ) {
        if (backgroundTtsEnabled) {
            null
        } else {
            val ownerId = readerSessionInstanceId
            createReadiumReaderTtsSession(
                activity.applicationContext, opened,
                commitCheckpoint = { locator, preferences ->
                    requireNotNull(foregroundTtsCheckpoint).commit(locator, preferences)
                }
            ) {
                latestReaderSessionInstanceId.value == ownerId && latestTtsCanPlay.value()
            }
        }
    }
    LaunchedEffect(ttsSession, foregroundTtsCheckpoint) {
        val session = ttsSession ?: return@LaunchedEffect
        try {
            foregroundTtsCheckpoint?.restore(
                session,
                ReaderTtsPreferences(
                    speed = latestTtsSettings.value.speed.toFloat(),
                    pitch = latestTtsSettings.value.pitch.toFloat(),
                    preferredVoiceIds = latestTtsSettings.value.preferredVoiceIds
                )
            ) { latestReaderSessionInstanceId.value == readerSessionInstanceId && !closeInFlight }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            readerMessage = activity.getString(R.string.tts_checkpoint_failed)
        }
    }
    val ttsServiceController = remember(
        opened.book.id,
        readerSessionInstanceId,
        backgroundTtsEnabled
    ) {
        if (backgroundTtsEnabled && opened.format == BookFormat.EPUB) {
            ReaderTtsServiceController(activity.applicationContext)
        } else {
            null
        }
    }
    DisposableEffect(ttsServiceController) {
        onDispose {
            ttsServiceController?.close()
        }
    }
    DisposableEffect(ttsSession, lifecycle, backgroundTtsEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (
                !backgroundTtsEnabled &&
                (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP)
            ) {
                ttsStartSerial += 1
                ttsStartJob?.cancel()
                ttsStartPending = false
                ttsSession?.pause()
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            ttsStartSerial += 1
            ttsStartJob?.cancel()
            ttsSession?.close()
        }
    }

    fun recordLocator(locator: Locator, event: ReaderLocatorEvent) {
        val json = locator.toVeilPersistedJson(opened.format)
        if (!navigationSessionState.mayCommitObservedEvent(event, json)) {
            ReaderTrace.event(
                "exploration_checkpoint_suppressed",
                bookId = opened.book.id,
                sessionId = readerSessionInstanceId,
                details = "event=${event.name}"
            )
            return
        }
        readerViewModel.onLocatorUpdate(
            bookId = opened.book.id,
            expectedOpenInstanceId = readerSessionInstanceId,
            progression = locator.locations.totalProgression
                ?: readerViewModel.uiState.value.progress.toDouble(),
            locatorJson = json,
            locationKey = "${opened.book.id}:$json",
            event = event
        )?.let { commit ->
            navigationSessionState.onDurableReadingCommit(commit.locatorJson)
            onLocatorCheckpoint(commit.locatorJson)
        }
    }

    fun currentLocatorJson(): String? =
        navigator?.currentLocator?.value?.toVeilPersistedJson(opened.format)

    fun markReaderNavigationInteraction() {
        viewportRelayoutPending = false
        viewportRelayoutJob?.cancel()
        viewportRelayoutJob = null
        navigationTransactionGate.reset()
        readerViewModel.onUserInteraction(readerSessionInstanceId)
        controlsVisible = false
    }

    fun returnToPreviousLocation() {
        val targetJson = previousLocationJson ?: return
        val locator = runCatching {
            Locator.fromJSON(JSONObject(targetJson))
        }.getOrNull()
        if (locator == null || navigator == null) {
            readerMessage = previousLocationFailedMessage
            return
        }

        val expectedSessionId = readerSessionInstanceId
        scope.launch {
            if (!settlePagePreviewsBeforeProgrammaticNavigation()) {
                readerMessage = previousLocationFailedMessage
                return@launch
            }
            if (
                !readerAsyncResultBelongsToSession(
                    currentSessionInstanceId = latestReaderSessionInstanceId.value,
                    expectedSessionInstanceId = expectedSessionId
                )
            ) {
                return@launch
            }

            val nav = latestNavigator.value ?: return@launch
            val originLocator = nav.currentLocator.value
            val targetIdentity = locator.toReaderNavigationIdentity()
            if (
                !shouldStartReaderIdentityJump(
                    origin = originLocator.toReaderNavigationIdentity(),
                    target = targetIdentity
                )
            ) {
                previousLocationJson = null
                controlsVisible = false
                return@launch
            }

            readerViewModel.onUserInteraction(expectedSessionId)
            game.rebasePagePacing()
            val transactionToken = beginProgrammaticNavigation(
                originLocatorJson =
                    originLocator.toVeilPersistedJson(opened.format),
                targetIdentity = targetIdentity,
                reason = ReaderNavigationReason.RETURN_PREVIOUS,
                expectedPdfPage =
                    if (opened.format == BookFormat.PDF) {
                        pdfPageNumber(locator)
                    } else {
                        null
                    }
            )

            if (
                nav.go(
                    locator,
                    animated = shouldAnimateReaderJump(latestReducedMotion.value)
                )
            ) {
                controlsVisible = false
            } else {
                cancelProgrammaticNavigation(transactionToken)
                readerMessage = previousLocationFailedMessage
            }
        }
    }

    fun settleReachedPdfNavigation(locator: Locator? = latestNavigator.value?.currentLocator?.value) {
        if (opened.format != BookFormat.PDF || locator == null) return
        val settled = navigationTransactionGate.consumeReachedPdfDestination(
            nowElapsedMs = SystemClock.elapsedRealtime(),
            observedPdfPage = pdfPageNumber(locator)
        ) ?: return
        val json = locator.toVeilPersistedJson(opened.format)
        previousLocationJson = settled.originLocatorJson?.takeIf { it != json }
        navigationSessionState.onProgrammaticSettlement(settled, json)
        if (settled.commitPolicy == ReaderNavigationCommitPolicy.COMMIT_ON_SETTLEMENT) {
            recordLocator(locator, ReaderLocatorEvent.NAVIGATION_JUMP_COMMIT)
        }
        settled.passageVisitAfterSettlement(pdfPageNumber(locator))?.let {
            library.recordPassageVisitForLocator(bookId = opened.book.id, locatorJson = it)
        }
    }

    fun closeReader() {
        if (closeInFlight) return
        closeInFlight = true
        ttsStartSerial += 1
        ttsStartJob?.cancel()
        ttsSession?.close()

        val expectedSessionId = readerSessionInstanceId
        val paperHadPendingTurn =
            paperInputListener?.hasPendingTurn() == true
        val slideHadPendingTurn =
            slideInputListener?.hasPendingTurn() == true

        scope.launch {
            try {
                // Normal user-initiated Close has a live composition scope, so unlike
                // emergency lifecycle teardown we can wait for preview restoration to
                // become authoritative in Readium's currentLocator before snapshotting.
                val paperPreviewSettled =
                    !paperHadPendingTurn ||
                        paperInputListener?.cancelPendingTurnAndAwait() == true ||
                        paperInputListener?.hasPendingTurn() != true
                val slidePreviewSettled =
                    !slideHadPendingTurn ||
                        slideInputListener?.cancelPendingTurnAndAwait() == true ||
                        slideInputListener?.hasPendingTurn() != true
                val unresolvedPreview =
                    !paperPreviewSettled || !slidePreviewSettled

                if (unresolvedPreview) {
                    // A timed-out navigator restoration must never be persisted as a
                    // committed destination. Emergency cleanup is visual/resource-only;
                    // the previously durable locator remains the safe fallback.
                    paperInputListener?.forceCancelPendingTurn()
                    slideInputListener?.forceCancelPendingTurn()
                    ReaderTrace.event(
                        "reader_close_preview_restore_unsettled",
                        bookId = opened.book.id,
                        sessionId = expectedSessionId
                    )
                }

                settleReachedPdfNavigation()
                val currentLocator =
                    latestNavigator.value?.currentLocator?.value
                val cancelledNavigation =
                    navigationTransactionGate
                        .cancelActive(SystemClock.elapsedRealtime())
                val reachedCancelledNavigation =
                    cancelledNavigation != null &&
                        currentLocator != null &&
                        cancelledNavigation.hasReachedObservedDestination(
                            observedLocatorJson =
                                currentLocator.toVeilPersistedJson(opened.format),
                            observedIdentity =
                                currentLocator.toReaderNavigationIdentity(),
                            observedPdfPage =
                                if (opened.format == BookFormat.PDF) {
                                    pdfPageNumber(currentLocator)
                                } else {
                                    null
                                }
                        )

                if (reachedCancelledNavigation) {
                    val json =
                        currentLocator.toVeilPersistedJson(opened.format)
                    previousLocationJson =
                        cancelledNavigation.originLocatorJson
                            ?.takeIf { it != json }
                    navigationSessionState.onProgrammaticSettlement(
                        cancelledNavigation,
                        json
                    )
                    if (cancelledNavigation.commitPolicy ==
                        ReaderNavigationCommitPolicy.COMMIT_ON_SETTLEMENT
                    ) {
                        recordLocator(currentLocator, ReaderLocatorEvent.NAVIGATION_JUMP_COMMIT)
                    }
                    cancelledNavigation
                        .passageVisitAfterSettlement(
                            if (opened.format == BookFormat.PDF) {
                                pdfPageNumber(currentLocator)
                            } else {
                                null
                            }
                        )
                        ?.let { visitedLocatorJson ->
                            library.recordPassageVisitForLocator(
                                bookId = opened.book.id,
                                locatorJson = visitedLocatorJson
                            )
                        }
                }

                val unresolvedProgrammaticNavigation =
                    cancelledNavigation != null &&
                        !reachedCancelledNavigation
                if (
                    pendingEpubRelayoutSourceJson == null &&
                    shouldTakeFinalNavigatorSnapshot(
                        format = opened.format,
                        paperPreviewActive = paperCurlState.active,
                        slidePreviewActive = slidePageState.active,
                        previewCancelled = unresolvedPreview,
                        programmaticNavigationInFlight =
                            unresolvedProgrammaticNavigation
                    )
                ) {
                    currentLocator?.let { locator ->
                        recordLocator(locator, ReaderLocatorEvent.FINAL_SNAPSHOT)
                    }
                }

                check(!readerViewModel.uiState.value.progressSaveFailed) {
                    "Reader progress checkpoint is not durable."
                }
                ReaderTrace.event(
                    "reader_close_durability_wait",
                    bookId = opened.book.id,
                    sessionId = expectedSessionId
                )
                awaitDurableReaderClose(
                    finalizeSession = { readerViewModel.closeBook(expectedSessionId) },
                    awaitDurability = library::flushWrites,
                    clearRoute = onClose,
                    awaitOwnerRelease = { ttsSession?.awaitClosed() }
                )
                ReaderTrace.event(
                    "reader_close_durable",
                    bookId = opened.book.id,
                    sessionId = expectedSessionId
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (
                    !readerAsyncResultBelongsToSession(
                        currentSessionInstanceId = latestReaderSessionInstanceId.value,
                        expectedSessionInstanceId = expectedSessionId
                    )
                ) {
                    return@launch
                }
                closeInFlight = false
                ReaderTrace.event(
                    "reader_close_durability_failed",
                    bookId = opened.book.id,
                    sessionId = expectedSessionId,
                    details = "error=${error::class.java.simpleName}"
                )
                readerMessage = closeStorageFailedMessage
            }
        }
    }

    BackHandler(enabled = imageLoading) { cancelImageLoad() }

    BackHandler(
        enabled =
            !showNotebook &&
            !showReaderSearch &&
            !showAppearance &&
            !showPdfZoom &&
            !showTts &&
            !imageLoading &&
            imageViewer == null &&
            footnote == null &&
            pendingNoteLocatorJson == null
    ) {
        when (
            readerBackDisposition(
                closeInFlight = closeInFlight,
                paperPreviewActive = paperCurlState.active,
                slidePreviewActive = slidePageState.active
            )
        ) {
            ReaderBackDisposition.SWALLOW -> Unit
            ReaderBackDisposition.CANCEL_PAPER -> {
                val restored = paperInputListener?.cancelPendingTurn() == true
                if (!restored && paperCurlState.active) {
                    paperCurlState.clearImmediately()
                }
            }
            ReaderBackDisposition.CANCEL_SLIDE -> {
                val restored = slideInputListener?.cancelPendingTurn() == true
                if (!restored && slidePageState.active) {
                    slidePageState.clearImmediately()
                }
            }
            ReaderBackDisposition.CLOSE -> closeReader()
        }
    }

    val epubNavigatorListener = remember(
        opened.book.id,
        readerSessionInstanceId,
        activity,
        externalLinkFailedMessage
    ) {
        object : EpubNavigatorFragment.Listener {
            override fun shouldFollowInternalLink(
                link: Link,
                context: HyperlinkNavigator.LinkContext?
            ): Boolean =
                when (context) {
                    is HyperlinkNavigator.FootnoteContext -> {
                        val text = if (link.mediaType?.isHtml == true) {
                            Html.fromHtml(
                                context.noteContent,
                                Html.FROM_HTML_MODE_COMPACT
                            ).toString()
                        } else {
                            context.noteContent
                        }.trim()

                        activity.runOnUiThread {
                            footnote = ReaderFootnote(
                                title = link.title?.trim()
                                    ?.takeIf { it.isNotEmpty() },
                                text = text
                            )
                        }
                        false
                    }
                    else -> {
                        val hasPendingPreview =
                            paperInputListener?.hasPendingTurn() == true ||
                                slideInputListener?.hasPendingTurn() == true

                        if (hasPendingPreview) {
                            val expectedSessionId = readerSessionInstanceId
                            // Consume Readium's automatic link navigation now. Veil will
                            // replay the exact link after the preview origin is authoritative.
                            scope.launch {
                                if (!settlePagePreviewsBeforeProgrammaticNavigation()) {
                                    readerMessage = chapterFailedMessage
                                    return@launch
                                }
                                if (
                                    !readerAsyncResultBelongsToSession(
                                        currentSessionInstanceId =
                                            latestReaderSessionInstanceId.value,
                                        expectedSessionInstanceId =
                                            expectedSessionId
                                    )
                                ) {
                                    return@launch
                                }

                                val nav = latestNavigator.value ?: return@launch
                                val currentLocator = nav.currentLocator.value
                                val targetHref = readerEffectiveTargetHref(
                                    currentHref = currentLocator.href.toString(),
                                    targetHref = link.href.toString()
                                )
                                if (
                                    shouldStartReaderLinkJump(
                                        currentHref = currentLocator.href.toString(),
                                        targetHref = targetHref
                                    )
                                ) {
                                    val token = beginProgrammaticNavigation(
                                        originLocatorJson =
                                            currentLocator.toVeilPersistedJson(opened.format),
                                        targetHref = targetHref,
                                        reason = ReaderNavigationReason.INTERNAL_LINK
                                    )
                                    game.rebasePagePacing()
                                    if (
                                        !nav.go(
                                            link,
                                            animated =
                                                shouldAnimateReaderJump(
                                                    latestReducedMotion.value
                                                )
                                        )
                                    ) {
                                        cancelProgrammaticNavigation(token)
                                        readerMessage = chapterFailedMessage
                                        return@launch
                                    }
                                }
                                controlsVisible = false
                                readerViewModel.onUserInteraction(expectedSessionId)
                            }
                            false
                        } else {
                            val currentLocator = latestNavigator.value
                                ?.currentLocator
                                ?.value
                            val origin = currentLocator
                                ?.toVeilPersistedJson(opened.format)
                            val targetHref = readerEffectiveTargetHref(
                                currentHref = currentLocator?.href?.toString(),
                                targetHref = link.href.toString()
                            )
                            val trackJump = shouldStartReaderLinkJump(
                                currentHref = currentLocator?.href?.toString(),
                                targetHref = targetHref
                            )
                            if (trackJump) {
                                val transaction = navigationTransactionGate.begin(
                                    originLocatorJson = origin,
                                    nowElapsedMs = SystemClock.elapsedRealtime(),
                                    targetHref = targetHref,
                                    reason = ReaderNavigationReason.INTERNAL_LINK
                                )
                                ReaderTrace.event(
                                    "navigation_jump_requested",
                                    bookId = opened.book.id,
                                    sessionId = readerSessionInstanceId,
                                    details =
                                        "token=${transaction.token} source=internal_link"
                                )
                                game.rebasePagePacing()
                            }
                            activity.runOnUiThread {
                                controlsVisible = false
                            }
                            readerViewModel.onUserInteraction(readerSessionInstanceId)
                            true
                        }
                    }
                }

            override fun onExternalLinkActivated(url: AbsoluteUrl) {
                if (!url.isHttp) return
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    url.toString().toUri()
                ).addCategory(Intent.CATEGORY_BROWSABLE)
                val launched = runCatching {
                    activity.startActivity(intent)
                }.isSuccess
                if (!launched) {
                    activity.runOnUiThread {
                        readerMessage = externalLinkFailedMessage
                    }
                }
            }
        }
    }

    val latestPdfLinkAction = rememberUpdatedState<(com.github.barteksc.pdfviewer.model.LinkTapEvent) -> Unit> { event ->
        // Native link hit testing happens after onTap; it always wins over pending Veil chrome.
        pdfTapArbiter?.cancelPendingTap()
        val nav = latestNavigator.value
        val owned = readerAsyncResultBelongsToSession(
            currentSessionInstanceId = latestReaderSessionInstanceId.value,
            expectedSessionInstanceId = readerSessionInstanceId
        )
        if (
            owned && latestReaderSessionReady.value && nav != null &&
            lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
            !closeInFlight && !rendererPreferencesSettling &&
            !showNotebook && !showAppearance && !showPdfZoom &&
            pendingNoteLocatorJson == null && footnote == null &&
            !imageLoading && imageViewer == null
        ) {
            val uri = event.link.uri
            if (!uri.isNullOrBlank()) {
                val safeUri = safePdfExternalLink(uri)
                if (safeUri == null || runCatching {
                    activity.startActivity(
                        Intent(Intent.ACTION_VIEW, safeUri)
                            .addCategory(Intent.CATEGORY_BROWSABLE)
                    )
                }.isFailure) {
                    readerMessage = externalLinkFailedMessage
                }
            } else {
                val pageIndex = event.link.destPageIdx
                val pageCount = nav.findPdfView()?.pageCount ?: 0
                val target = pageIndex?.let {
                    pdfInternalLinkLocator(nav.currentLocator.value, it, pageCount)
                }
                if (target == null) {
                    readerMessage = previousLocationFailedMessage
                } else if (pdfPageNumber(nav.currentLocator.value) != pdfPageNumber(target)) {
                    val token = beginProgrammaticNavigation(
                        currentLocatorJson(),
                        reason = ReaderNavigationReason.PDF_INTERNAL_LINK,
                        expectedPdfPage = pdfPageNumber(target)
                    )
                    readerViewModel.onUserInteraction(readerSessionInstanceId)
                    game.rebasePagePacing()
                    if (nav.go(target, animated = false)) {
                        controlsVisible = false
                    } else {
                        cancelProgrammaticNavigation(token)
                        readerMessage = previousLocationFailedMessage
                    }
                }
            }
        }
    }
    val pdfLinkHandler = remember(opened.book.id, readerSessionInstanceId) {
        val ownerSessionId = readerSessionInstanceId
        LinkHandler { event ->
            if (latestReaderSessionInstanceId.value == ownerSessionId) {
                latestPdfLinkAction.value(event)
            }
        }
    }

    val fragmentFactory = remember(
        opened.book.id,
        readerSessionInstanceId,
        selectionActionModeCallback,
        epubNavigatorListener,
        pdfLinkHandler
    ) {
        createReaderFactory(
            opened = opened,
            appearance = effectiveReaderAppearance,
            fixedLayoutSpread = activeFixedLayoutSpread,
            selectionActionModeCallback = selectionActionModeCallback,
            epubNavigatorListener = epubNavigatorListener,
            pdfLinkHandler = pdfLinkHandler
        )
    }
    val onNavigatorReady = remember<(Navigator) -> Unit>(opened.book.id, readerSessionInstanceId) {
        { ready ->
            navigator = ready
            navigatorAttached = true
            ReaderTrace.event(
                "navigator_attached",
                bookId = opened.book.id,
                sessionId = readerSessionInstanceId,
                details = "type=${ready::class.java.simpleName}"
            )
        }
    }
    val onDisposePublication = remember(opened.book.id, readerSessionInstanceId) {
        { opened.close() }
    }

    DisposableEffect(paperCurlState) {
        onDispose { paperCurlState.dispose() }
    }
    DisposableEffect(slidePageState) {
        onDispose { slidePageState.dispose() }
    }
    DisposableEffect(readerModeHandoffState) {
        onDispose { readerModeHandoffState.dispose() }
    }

    DisposableEffect(lifecycle, readerViewModel, readerSessionInstanceId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    readerLifecycleResumed = true
                    if (latestReaderSessionReady.value) {
                        readerViewModel.onResume(readerSessionInstanceId)
                    }
                }
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> {
                    readerLifecycleResumed = false
                    if (latestReaderSessionReady.value) {
                        // Lifecycle teardown may cancel the composition scope immediately. Restore an
                        // uncommitted preview synchronously before any final locator can be flushed.
                        val cancelledPaperPreview =
                            paperInputListener?.forceCancelPendingTurn() == true
                        val cancelledSlidePreview =
                            slideInputListener?.forceCancelPendingTurn() == true
                        val cancelledPreview =
                            cancelledPaperPreview || cancelledSlidePreview
                        settleReachedPdfNavigation()
                        val navigationJumpInFlight =
                            navigationTransactionGate.isActive(SystemClock.elapsedRealtime())
                        if (
                            pendingEpubRelayoutSourceJson == null &&
                            shouldTakeFinalNavigatorSnapshot(
                                format = opened.format,
                                paperPreviewActive = paperCurlState.active,
                                slidePreviewActive = slidePageState.active,
                                previewCancelled = cancelledPreview,
                                programmaticNavigationInFlight = navigationJumpInFlight
                            )
                        ) {
                            latestNavigator.value?.currentLocator?.value?.let { locator ->
                                recordLocator(locator, ReaderLocatorEvent.FINAL_SNAPSHOT)
                            }
                        }
                        readerViewModel.onPause(readerSessionInstanceId)
                        if (
                            event == Lifecycle.Event.ON_STOP ||
                            event == Lifecycle.Event.ON_DESTROY
                        ) {
                            // Hidden Readers should not retain warm CPU page captures.
                            // Active previews keep ownership until the cancellation path
                            // above clears them; idle caches are immediately expendable.
                            paperCurlState.releaseBufferIfIdle()
                            slidePageState.releaseBufferIfIdle()
                            readerModeHandoffState.releaseBufferIfIdle()
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            paperInputListener?.forceCancelPendingTurn()
            slideInputListener?.forceCancelPendingTurn()
            if (latestReaderSessionReady.value) {
                readerViewModel.onPause(readerSessionInstanceId)
            } else {
                readerViewModel.cancelOpen(readerSessionInstanceId)
            }
            lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(
        navigator,
        navigatorAttached,
        readerSessionReady,
        opened.book.id,
        readerSessionInstanceId
    ) {
        val nav = navigator ?: return@LaunchedEffect
        if (
            !shouldCollectReaderLocator(
                sessionReady = readerSessionReady,
                navigatorAttached = navigatorAttached
            )
        ) {
            return@LaunchedEffect
        }
        var initialLocatorPending = true
        var lastPaperVisualLocatorJson: String? = null
        nav.currentLocator
            .onEach { locator ->
                // Paper pixels follow the visual navigator, not only durable commits. Invalidate
                // immediately for TOC jumps, previous-location returns and preview navigation so
                // an old warm snapshot cannot survive until the next gesture.
                if (opened.format == BookFormat.EPUB) {
                    val visualLocatorJson =
                        locator.toVeilPersistedJson(opened.format)
                    if (visualLocatorJson != lastPaperVisualLocatorJson) {
                        lastPaperVisualLocatorJson = visualLocatorJson
                        paperCurlState.invalidateSnapshotSource()
                    }
                }
                // A PDF link destination is already authoritative before the UI debounce. Capture
                // it once so a rapid subsequent native swipe or lifecycle pause cannot erase it.
                if (opened.format == BookFormat.PDF) settleReachedPdfNavigation(locator)
            }
            .debounce(500)
            .collect { locator ->
                locationTitle = locator.title?.trim().orEmpty()
                currentLocationHref = locator.href.toString()

                val pagePreviewActive =
                    shouldSuppressNavigatorLocatorDuringPagePreview(
                        format = opened.format,
                        paperPreviewActive = paperCurlState.active,
                        slidePreviewActive = slidePageState.active
                    )
                if (pagePreviewActive) return@collect

                val json = locator.toVeilPersistedJson(opened.format)
                pendingEpubRelayoutSourceJson?.let { staleSourceJson ->
                    if (json == staleSourceJson) {
                        ReaderTrace.event(
                            "locator_relayout_stale_suppressed",
                            bookId = opened.book.id,
                            sessionId = readerSessionInstanceId
                        )
                        return@collect
                    }
                    pendingEpubRelayoutSourceJson = null
                    pendingEpubRelayoutAnchor = null
                    ReaderTrace.event(
                        "locator_relayout_fresh_observed",
                        bookId = opened.book.id,
                        sessionId = readerSessionInstanceId
                    )
                }
                ReaderTrace.event(
                    "locator_observed",
                    bookId = opened.book.id,
                    sessionId = readerViewModel.traceSessionId(),
                    details = "progress=${locator.locations.totalProgression}"
                )
                val settledNavigation = navigationTransactionGate.consumeSettled(
                    observedLocatorJson = json,
                    nowElapsedMs = SystemClock.elapsedRealtime(),
                    observedIdentity = locator.toReaderNavigationIdentity(),
                    observedPdfPage = if (opened.format == BookFormat.PDF) pdfPageNumber(locator) else null
                )
                if (settledNavigation != null) {
                    navigationSessionState.onProgrammaticSettlement(settledNavigation, json)
                    previousLocationJson = settledNavigation.originLocatorJson
                        ?.takeIf { origin -> origin != json }
                    settledNavigation.passageVisitAfterSettlement(
                        if (opened.format == BookFormat.PDF) pdfPageNumber(locator) else null
                    )
                        ?.takeIf { it.isNotBlank() }
                        ?.let { visitedLocatorJson ->
                            library.recordPassageVisitForLocator(
                                bookId = opened.book.id,
                                locatorJson = visitedLocatorJson
                            )
                        }
                    ReaderTrace.event(
                        "navigation_jump_settled",
                        bookId = opened.book.id,
                        sessionId = readerSessionInstanceId,
                        details = "token=${settledNavigation.token} progress=${locator.locations.totalProgression}"
                    )
                }

                val continuousScroll =
                    (nav as? OverflowableNavigator)?.overflow?.value?.scroll == true
                val event = readerObservedLocatorEvent(
                    programmaticNavigationSettled = settledNavigation != null,
                    viewportRelayoutPending = viewportRelayoutPending,
                    isInitialEmission = initialLocatorPending,
                    isContinuousScroll = continuousScroll,
                    isPaperMode =
                        opened.format == BookFormat.EPUB &&
                            latestAppearance.value.navigationMode ==
                                ReaderNavigationMode.PAPER_CURL,
                    isSlidePreviewActive =
                        opened.format == BookFormat.EPUB &&
                            slidePageState.active
                )
                val wasInitialLocator = initialLocatorPending
                initialLocatorPending = false
                val commit = if (
                    settledNavigation?.commitPolicy ==
                        ReaderNavigationCommitPolicy.PRESERVE_READING_ANCHOR ||
                    !navigationSessionState.mayCommitObservedEvent(event, json)
                ) null else readerViewModel.onLocatorUpdate(
                    bookId = opened.book.id,
                    expectedOpenInstanceId = readerSessionInstanceId,
                    progression = locator.locations.totalProgression
                        ?: readerViewModel.uiState.value.progress.toDouble(),
                    locatorJson = json,
                    locationKey = "${opened.book.id}:$json",
                    event = event
                )
                commit?.let { accepted ->
                    navigationSessionState.onDurableReadingCommit(accepted.locatorJson)
                    onLocatorCheckpoint(accepted.locatorJson)
                }

                val lifecycleResumed =
                    lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                if (
                    shouldFlushStartupLocatorInBackground(
                        sessionReady = readerSessionReady,
                        lifecycleResumed = lifecycleResumed,
                        initialLocatorCommitAccepted = wasInitialLocator && commit != null
                    )
                ) {
                    ReaderTrace.event(
                        "reader_startup_locator_background_flush",
                        bookId = opened.book.id,
                        sessionId = readerSessionInstanceId
                    )
                    readerViewModel.onPause(readerSessionInstanceId)
                }
            }
    }

    DisposableEffect(navigator, opened.book.id, readerSessionInstanceId) {
        val nav = navigator as? OverflowableNavigator
        if (nav == null) {
            onDispose { }
        } else {
            val imageTapListener = if (navigator is EpubNavigatorFragment) {
                ReaderImageTapInputListener { image ->
                    readerViewModel.onUserInteraction(readerSessionInstanceId)
                    controlsVisible = false
                    imageLoadSerial += 1
                    val requestSerial = imageLoadSerial
                    val expectedSessionId = readerSessionInstanceId
                    imageLoadJob?.cancel()
                    imageLoading = true
                    imageLoadJob = scope.launch {
                        try {
                            runReaderRequestWithDeadline(
                                timeoutMs = READER_IMAGE_LOAD_TIMEOUT_MS,
                                onTimeout = {
                                    if (
                                        requestSerial == imageLoadSerial &&
                                        readerAsyncResultBelongsToSession(
                                            currentSessionInstanceId = latestReaderSessionInstanceId.value,
                                            expectedSessionInstanceId = expectedSessionId
                                        )
                                    ) {
                                        imageLoadSerial += 1
                                        imageLoading = false
                                        imageLoadJob = null
                                        readerMessage = imageViewerTimeoutMessage
                                    }
                                }
                            ) {
                                val bytes = withContext(Dispatchers.IO) {
                                    opened.publication.get(image.embeddedLink)
                                        ?.use { resource -> readReaderImageBytes(resource) }
                                }
                                val caption = image.text?.trim()?.takeIf { it.isNotEmpty() }
                                deliverReaderResource(
                                    dispatcher = Dispatchers.Default,
                                    load = {
                                        bytes?.let { payload ->
                                            decodeReaderImage(payload)?.let { bitmap ->
                                                ReaderImageContent(
                                                    bitmap = bitmap,
                                                    caption = caption
                                                )
                                            }
                                        }
                                    },
                                    accept = { content ->
                                        when {
                                            requestSerial != imageLoadSerial ||
                                                !readerAsyncResultBelongsToSession(
                                                    currentSessionInstanceId = latestReaderSessionInstanceId.value,
                                                    expectedSessionInstanceId = expectedSessionId
                                                ) -> false
                                            content == null -> {
                                                readerMessage = imageViewerFailedMessage
                                                false
                                            }
                                            else -> {
                                                imageViewer = content
                                                true
                                            }
                                        }
                                    }
                                )
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            if (
                                requestSerial == imageLoadSerial &&
                                readerAsyncResultBelongsToSession(
                                    currentSessionInstanceId = latestReaderSessionInstanceId.value,
                                    expectedSessionInstanceId = expectedSessionId
                                )
                            ) {
                                readerMessage = if (error is ReaderImageTooLargeException) {
                                    imageViewerTooLargeMessage
                                } else {
                                    imageViewerFailedMessage
                                }
                            }
                        } finally {
                            if (
                                requestSerial == imageLoadSerial &&
                                readerAsyncResultBelongsToSession(
                                    currentSessionInstanceId = latestReaderSessionInstanceId.value,
                                    expectedSessionInstanceId = expectedSessionId
                                )
                            ) {
                                imageLoading = false
                                imageLoadJob = null
                            }
                        }
                    }
                }
            } else {
                null
            }

            fun paperModeSelected(): Boolean =
                shouldUsePaperCurlNavigation(
                    format = opened.format,
                    scroll = latestAppearance.value.scroll,
                    pageTurnStyle = latestAppearance.value.pageTurnStyle
                )

            val paperListener = if (navigator is EpubNavigatorFragment) {
                PaperCurlInputListener(
                    navigator = nav,
                    state = paperCurlState,
                    onVisualFailure = ::reportPaperVisualFailure,
                    isDragEnabled = {
                        latestReaderSessionReady.value && paperModeSelected() &&
                            paperRendererCanReserveDrag(
                                reducedMotion = latestReducedMotion.value,
                                rendererStatus = paperCurlState.rendererStatus
                            )
                    },
                    isEnabled = {
                        latestReaderSessionReady.value && paperModeSelected()
                    },
                    scope = scope,
                    isReducedMotion = { latestReducedMotion.value },
                    onInteraction = ::markReaderNavigationInteraction,
                    onCommittedTurn = {
                        if (!paperCurlState.usingMaterialEngine()) {
                            onSensoryEvent(VeilSensoryEvent.PAGE_TURN)
                        }
                        val locator = nav.currentLocator.value
                        recordLocator(locator, ReaderLocatorEvent.PAPER_COMMIT)
                    },
                    onBoundaryHit = { side ->
                        navigationTransactionGate.reset()
                        emitBoundaryFeedback(side)
                    }
                )
            } else {
                null
            }

            val slideListener = if (navigator is EpubNavigatorFragment) {
                SlideNavigationInputListener(
                    navigator = nav,
                    state = slidePageState,
                    isEnabled = {
                        latestReaderSessionReady.value &&
                            shouldUseVeilSlideNavigation(
                                format = opened.format,
                                scroll = latestAppearance.value.scroll,
                                pageTurnStyle = latestAppearance.value.pageTurnStyle
                            )
                    },
                    scope = scope,
                    isReducedMotion = { latestReducedMotion.value },
                    onInteraction = ::markReaderNavigationInteraction,
                    onCommittedTurn = {
                        onSensoryEvent(VeilSensoryEvent.SLIDE_TURN)
                        nav.currentLocator.value.let { locator ->
                            recordLocator(
                                locator,
                                ReaderLocatorEvent.NAVIGATOR_PAGE_TURN
                            )
                        }
                    },
                    onBoundaryHit = { side ->
                        navigationTransactionGate.reset()
                        emitBoundaryFeedback(side)
                    }
                )
            } else {
                null
            }

            val staticPagedListener = if (navigator is EpubNavigatorFragment) {
                StaticPagedNavigationInputListener(
                    navigator = nav,
                    isEnabled = {
                        latestReaderSessionReady.value &&
                            shouldUseStaticPagedDragNavigation(
                                format = opened.format,
                                scroll = latestAppearance.value.scroll,
                                pageTurnStyle = latestAppearance.value.pageTurnStyle
                            )
                    },
                    scope = scope,
                    onInteraction = ::markReaderNavigationInteraction,
                    onNavigationCommitted = {
                        onSensoryEvent(VeilSensoryEvent.PAGED_TURN)
                        nav.currentLocator.value.let { locator ->
                            recordLocator(
                                locator,
                                ReaderLocatorEvent.NAVIGATOR_PAGE_TURN
                            )
                        }
                    },
                    onBoundaryHit = { side ->
                        navigationTransactionGate.reset()
                        emitBoundaryFeedback(side)
                    }
                )
            } else {
                null
            }

            val directionalListener = VeilDirectionalNavigationInputListener(
                navigator = nav,
                scope = scope,
                isAnimated = {
                    !latestReducedMotion.value &&
                        shouldAnimateDirectionalNavigation(
                            format = opened.format,
                            scroll = if (opened.format == BookFormat.EPUB) {
                                latestAppearance.value.scroll
                            } else {
                                nav.overflow.value.scroll
                            },
                            pageTurnStyle = latestAppearance.value.pageTurnStyle
                        )
                },
                isEnabled = {
                    latestReaderSessionReady.value &&
                        (
                            opened.format != BookFormat.EPUB ||
                                !latestAppearance.value.scroll
                            )
                },
                isTapNavigationEnabled = {
                    shouldUseDirectionalTapNavigation(
                        format = opened.format,
                        scroll = if (opened.format == BookFormat.EPUB) {
                            latestAppearance.value.scroll
                        } else {
                            nav.overflow.value.scroll
                        },
                        pageTurnStyle = latestAppearance.value.pageTurnStyle
                    )
                },
                onInteraction = ::markReaderNavigationInteraction,
                onNavigationCommitted = {
                    navigationTransactionGate.reset()
                    val navigationMode = latestAppearance.value.navigationMode
                    val sensoryEvent = when {
                        opened.format != BookFormat.EPUB ->
                            VeilSensoryEvent.PAGED_TURN
                        navigationMode == ReaderNavigationMode.SLIDE ->
                            VeilSensoryEvent.SLIDE_TURN
                        navigationMode == ReaderNavigationMode.PAPER_CURL ->
                            VeilSensoryEvent.PAGE_TURN
                        else ->
                            VeilSensoryEvent.PAGED_TURN
                    }
                    onSensoryEvent(sensoryEvent)
                },
                onBoundaryHit = { side ->
                    navigationTransactionGate.reset()
                    emitBoundaryFeedback(side)
                }
            )

            fun performSemanticReaderTurn(direction: PaperTurnDirection): Boolean {
                val intendedScroll =
                    if (opened.format == BookFormat.EPUB) {
                        latestAppearance.value.scroll
                    } else {
                        nav.overflow.value.scroll
                    }
                if (
                    intendedScroll ||
                    !latestReaderSessionReady.value
                ) {
                    return false
                }

                fun performDirectPagedTurn(): Boolean {
                    navigationTransactionGate.reset()
                    readerViewModel.onUserInteraction(readerSessionInstanceId)
                    controlsVisible = false
                    val side = paperTurnSideFor(
                        direction,
                        nav.overflow.value.readingProgression
                    )
                    val origin = nav.currentLocator.value
                    val accepted = when (direction) {
                        PaperTurnDirection.FORWARD ->
                            nav.goForward(animated = false)
                        PaperTurnDirection.BACKWARD ->
                            nav.goBackward(animated = false)
                    }
                    if (!accepted) {
                        emitBoundaryFeedback(side)
                        return true
                    }

                    if (opened.format == BookFormat.EPUB) {
                        // Readium accepts the command before currentLocator reflects
                        // the new viewport. Let the authoritative locator collector
                        // persist/count the destination instead of writing the origin.
                        scope.launch {
                            val moved =
                                awaitReaderVisualNavigationDeparture(
                                    currentLocator = nav.currentLocator,
                                    origin = origin
                                )
                            if (moved) {
                                onSensoryEvent(VeilSensoryEvent.PAGED_TURN)
                            } else {
                                nav.go(origin, animated = false)
                                emitBoundaryFeedback(side)
                            }
                        }
                    } else {
                        // PDF has its own page-settlement/durability plumbing.
                        onSensoryEvent(VeilSensoryEvent.PAGED_TURN)
                    }
                    return true
                }

                return when (opened.format) {
                    BookFormat.EPUB ->
                        when (latestAppearance.value.navigationMode) {
                            ReaderNavigationMode.PAPER_CURL ->
                                paperListener?.performDiscreteTurn(direction) == true

                            ReaderNavigationMode.SLIDE ->
                                slideListener?.performDiscreteTurn(direction) == true

                            ReaderNavigationMode.PAGED ->
                                performDirectPagedTurn()

                            ReaderNavigationMode.SCROLL -> false
                        }

                    BookFormat.PDF ->
                        performDirectPagedTurn()

                    else -> false
                }
            }

            val tapZoneListener = ReaderTapZoneInputListener(
                navigator = nav,
                grid = { latestTapGrid.value },
                isEnabled = {
                    latestReaderSessionReady.value &&
                        opened.format == BookFormat.EPUB
                },
                canTurnPages = {
                    !latestAppearance.value.scroll
                },
                onPreviousPage = {
                    performSemanticReaderTurn(PaperTurnDirection.BACKWARD)
                },
                onNextPage = {
                    performSemanticReaderTurn(PaperTurnDirection.FORWARD)
                },
                onToggleControls = {
                    readerViewModel.onUserInteraction(readerSessionInstanceId)
                    controlsVisible = !controlsVisible
                    true
                }
            )

            fun currentInteractionMode(): ReaderInteractionMode =
                readerInteractionMode(
                    selectionModeActive = selectionModeActive,
                    overlayVisible =
                        !latestReaderSessionReady.value ||
                            rendererPreferencesSettling ||
                            showNotebook ||
                            // Modal search must own hardware keys/drag/tap while visible.
                            // Dialog touch interception alone does not fence Readium input.
                            showReaderSearch ||
                            showAppearance ||
                            showPdfZoom ||
                            showTts ||
                            pendingNoteLocatorJson != null ||
                            footnote != null ||
                            imageLoading ||
                            imageViewer != null,
                    closeInFlight = closeInFlight,
                    controlsVisible = controlsVisible,
                    touchExplorationEnabled = touchExplorationEnabled
                )

            val hardwareKeyController = ReaderHardwareKeyController(
                mapping = { latestHardwareKeys.value },
                isEnabled = {
                    latestReaderSessionReady.value &&
                        (opened.format == BookFormat.EPUB || opened.format == BookFormat.PDF) &&
                        !readerHardwareAccessibilityActive(accessibilityManager) &&
                        when (currentInteractionMode()) {
                            ReaderInteractionMode.NAVIGATION,
                            ReaderInteractionMode.CHROME_PRIORITY -> true
                            else -> false
                        }
                },
                onPreviousPage = {
                    performSemanticReaderTurn(PaperTurnDirection.BACKWARD)
                },
                onNextPage = {
                    performSemanticReaderTurn(PaperTurnDirection.FORWARD)
                },
                onToggleControls = {
                    readerViewModel.onUserInteraction(readerSessionInstanceId)
                    controlsVisible = !controlsVisible
                    true
                }
            )
            val hardwareKeyHost = activity as? ReaderHardwareKeyHost
            hardwareKeyHost?.installReaderHardwareKeyHandler(
                ownerId = readerSessionInstanceId,
                handler = hardwareKeyController::handle
            )

            val inputArbiter = ReaderInputArbiter(
                contentTarget = imageTapListener,
                tapZones = tapZoneListener,
                paper = paperListener,
                slide = slideListener,
                staticPaged = staticPagedListener,
                directional = directionalListener,
                chromeTap = {
                    readerViewModel.onUserInteraction(readerSessionInstanceId)
                    controlsVisible = !controlsVisible
                    true
                },
                interactionMode = ::currentInteractionMode,
                onTapOwner = { owner ->
                    ReaderTrace.event(
                        "gesture_owned",
                        bookId = opened.book.id,
                        sessionId = readerViewModel.traceSessionId(),
                        details = "gesture=tap owner=${owner.name.lowercase()}"
                    )
                }
            )

            paperInputListener = paperListener
            slideInputListener = slideListener
            val registeredInput = if (opened.format == BookFormat.PDF) {
                ReaderPdfTapArbiter(inputArbiter, isEnabled = {
                    lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                        latestReaderSessionInstanceId.value == readerSessionInstanceId
                }).also { pdfTapArbiter = it }
            } else inputArbiter
            nav.addInputListener(registeredInput)
            onDispose {
                imageLoadSerial += 1
                imageLoadJob?.cancel()
                imageLoadJob = null
                imageLoading = false
                paperListener?.forceCancelPendingTurn()
                slideListener?.forceCancelPendingTurn()
                hardwareKeyHost?.clearReaderHardwareKeyHandler(readerSessionInstanceId)
                nav.removeInputListener(registeredInput)
                (registeredInput as? ReaderPdfTapArbiter)?.let { owner ->
                    owner.dispose()
                    if (pdfTapArbiter === owner) pdfTapArbiter = null
                }
                if (paperInputListener === paperListener) paperInputListener = null
                if (slideInputListener === slideListener) slideInputListener = null
            }
        }
    }

    LaunchedEffect(
        readerSessionInstanceId,
        navigator,
        effectiveReaderAppearance,
        activeFixedLayoutSpread,
        opened.format
    ) {
        val nav = navigator
        if (nav == null) {
            presentedReaderAppearance = effectiveReaderAppearance
            acceptedReaderAppearance = readerAppearance
            presentedFixedLayoutSpread = activeFixedLayoutSpread
            rendererPreferencesSettling = false
            readerModeHandoffState.clearImmediately()
            return@LaunchedEffect
        }

        val previousPresented = presentedReaderAppearance
        val previousAccepted = acceptedReaderAppearance
        val previousPresentedSpread = presentedFixedLayoutSpread
        val requestedSource = readerAppearance
        val requested = effectiveReaderAppearance
        val requestedSpread = activeFixedLayoutSpread

        if (
            !readerPreferencesNeedSubmission(
                previousPresented = previousPresented,
                previousAccepted = previousAccepted,
                previousSpread = previousPresentedSpread,
                requestedPresented = requested,
                requestedSource = requestedSource,
                requestedSpread = requestedSpread
            )
        ) {
            // The FragmentFactory already supplied these exact preferences to Readium.
            // Re-submitting them during the first navigator attach can leave the
            // renderer preference coroutine waiting while Veil blocks every input.
            rendererPreferencesSettling = false
            readerModeHandoffState.clearImmediately()
            return@LaunchedEffect
        }

        val fixedLayoutSpreadChanged =
            fixedLayoutPublication && previousPresentedSpread != requestedSpread
        val captureModeHandoff = shouldCaptureReaderModeHandoff(
            format = opened.format,
            previousMode = previousPresented.navigationMode,
            requestedMode = requested.navigationMode,
            fixedLayoutSpreadChanged = fixedLayoutSpreadChanged
        )
        val epubRelayoutRisk =
            opened.format == BookFormat.EPUB &&
                !fixedLayoutPublication &&
                epubPreferencesMayRelayout(previousPresented, requested)

        // Preference changes must never race a half-committed page gesture.
        // This effect is already suspend-capable, so require Readium's currentLocator
        // to settle back to the preview origin before capturing a handoff or reflowing.
        val traceDetails =
            "format=${opened.format} theme=${requested.theme} " +
                "publisherStyles=${requested.publisherStyles} " +
                "scroll=${requested.scroll} " +
                "pageTurn=${requested.pageTurnStyle} " +
                "spread=$activeFixedLayoutSpread"
        rendererPreferencesSettling = true
        try {
            val paperHadPendingTurn =
                paperInputListener?.hasPendingTurn() == true
            val slideHadPendingTurn =
                slideInputListener?.hasPendingTurn() == true
            val paperSettled =
                !paperHadPendingTurn ||
                    withTimeoutOrNull(READER_PREVIEW_SETTLE_TIMEOUT_MS) {
                        paperInputListener?.cancelPendingTurnAndAwait()
                    } == true ||
                    paperInputListener?.hasPendingTurn() != true
            val slideSettled =
                !slideHadPendingTurn ||
                    withTimeoutOrNull(READER_PREVIEW_SETTLE_TIMEOUT_MS) {
                        slideInputListener?.cancelPendingTurnAndAwait()
                    } == true ||
                    slideInputListener?.hasPendingTurn() != true
            if (!paperSettled || !slideSettled) {
                paperInputListener?.forceCancelPendingTurn()
                slideInputListener?.forceCancelPendingTurn()
                rendererPreferencesSettling = false
                readerMessage = appearanceApplyFailedMessage
                ReaderTrace.event(
                    "appearance_submit_blocked_unsettled_preview",
                    bookId = opened.book.id,
                    sessionId = readerSessionInstanceId
                )
                if (requestedSource != previousAccepted) {
                    onReaderAppearanceChange(previousAccepted)
                }
                if (requestedSpread != previousPresentedSpread) {
                    activeFixedLayoutSpread = previousPresentedSpread
                    onFixedLayoutSpreadChange(previousPresentedSpread)
                }
                return@LaunchedEffect
            }

            if (!readerRendererPreferencesChanged(
                    format = opened.format,
                    previous = previousPresented,
                    requested = requested,
                    previousSpread = previousPresentedSpread,
                    requestedSpread = requestedSpread
                )
            ) {
                // Paper/Slide/None share Readium's paged preferences. Applying the same
                // renderer preferences can wait for a layout that will never change.
                // Finish any preview first, then switch the Veil input and GPU together.
                presentedReaderAppearance = requested
                acceptedReaderAppearance = requestedSource
                presentedFixedLayoutSpread = requestedSpread
                paperCurlState.invalidateSnapshotSource()
                readerModeHandoffState.clearImmediately()
                rendererPreferencesSettling = false
                ReaderTrace.event(
                    "appearance_veil_only_applied",
                    bookId = opened.book.id,
                    sessionId = readerSessionInstanceId,
                    details = "pageTurn=${requested.pageTurnStyle}"
                )
                return@LaunchedEffect
            }

            val captured = if (captureModeHandoff) {
                (nav as? OverflowableNavigator)
                    ?.publicationView
                    ?.let(readerModeHandoffState::capture) == true
            } else {
                readerModeHandoffState.clearImmediately()
                false
            }

            game.rebasePagePacing()
            readerViewModel.onUserInteraction(readerSessionInstanceId)
            ReaderTrace.event(
                "appearance_submit_requested",
                bookId = opened.book.id,
                sessionId = readerViewModel.traceSessionId(),
                details = traceDetails
            )

            withTimeoutOrNull(READER_APPEARANCE_APPLY_TIMEOUT_MS) {
                if (epubRelayoutRisk) {
                    val epubNavigator = nav as? EpubNavigatorFragment
                    val rendererLocator = nav.currentLocator.value
                    val staleSourceJson = rendererLocator.toVeilPersistedJson(opened.format)
                    val preciseSourceLocator = try {
                        withTimeoutOrNull(READER_LOCATOR_QUERY_TIMEOUT_MS) {
                            epubNavigator?.firstVisibleElementLocator()
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                    val anchor = try {
                        val positions = withContext(Dispatchers.IO) {
                            opened.publication.positions()
                        }
                        stableEpubPositionAnchor(rendererLocator, positions)
                            ?.withEpubCssSelectorFrom(preciseSourceLocator)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                    if (anchor != null) {
                        recordLocator(anchor, ReaderLocatorEvent.RELAYOUT_CHECKPOINT)
                        pendingEpubRelayoutSourceJson = staleSourceJson
                        pendingEpubRelayoutAnchor = anchor
                        ReaderTrace.event(
                            "locator_relayout_anchor_committed",
                            bookId = opened.book.id,
                            sessionId = readerSessionInstanceId,
                            details = "position=${anchor.locations.position}"
                        )
                    }
                }

                if (fixedLayoutSpreadChanged) {
                    recordLocator(
                        nav.currentLocator.value,
                        ReaderLocatorEvent.RELAYOUT_CHECKPOINT
                    )
                }

                when (opened.format) {
                    BookFormat.EPUB ->
                        (nav as? EpubNavigatorFragment)
                            ?.submitPreferences(
                                requested.toEpubPreferences(
                                    fixedLayoutSpread = requestedSpread
                                )
                            )

                    BookFormat.PDF -> {
                        @Suppress("UNCHECKED_CAST")
                        val pdfNavigator = nav as? PdfiumNavigatorFragment
                        pdfNavigator?.submitPreferences(requested.toPdfiumPreferences())
                    }

                    else -> Unit
                }

                val settleFrames = readerPreferenceSettleFrames(
                    previousMode = previousPresented.navigationMode,
                    requestedMode = requested.navigationMode,
                    fixedLayoutSpreadChanged = fixedLayoutSpreadChanged
                )
                repeat(settleFrames) {
                    delay(VeilMotion.FRAME_SETTLE_MS)
                }

                if (
                    shouldRefreshPendingEpubRelayout(
                        format = opened.format,
                        hasPendingAnchor = pendingEpubRelayoutSourceJson != null
                    )
                ) {
                    val refreshed = try {
                        withTimeoutOrNull(READER_LOCATOR_QUERY_TIMEOUT_MS) {
                            (nav as? EpubNavigatorFragment)?.firstVisibleElementLocator()
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                    if (refreshed != null) {
                        val refreshedCheckpoint =
                            pendingEpubRelayoutAnchor?.withEpubCssSelectorFrom(refreshed)
                                ?: refreshed
                        recordLocator(refreshedCheckpoint, ReaderLocatorEvent.RELAYOUT_CHECKPOINT)
                        pendingEpubRelayoutSourceJson = null
                        pendingEpubRelayoutAnchor = null
                        ReaderTrace.event(
                            "locator_relayout_refreshed",
                            bookId = opened.book.id,
                            sessionId = readerSessionInstanceId,
                            details =
                                "position=${refreshedCheckpoint.locations.position} " +
                                    "progress=${refreshedCheckpoint.locations.totalProgression}"
                        )
                    }
                }

                if (fixedLayoutSpreadChanged) {
                    recordLocator(
                        nav.currentLocator.value,
                        ReaderLocatorEvent.RELAYOUT_CHECKPOINT
                    )
                }

                // Switch Veil-owned visuals/input only after the renderer has had time to paint.
                presentedReaderAppearance = requested
                acceptedReaderAppearance = requestedSource
                presentedFixedLayoutSpread = requestedSpread
                paperCurlState.invalidateSnapshotSource()
                if (captured) {
                    readerModeHandoffState.release(reducedMotion)
                }

                ReaderTrace.event(
                    "appearance_submit_returned",
                    bookId = opened.book.id,
                    sessionId = readerViewModel.traceSessionId(),
                    details = traceDetails
                )
                true
            } ?: error("Reader appearance application timed out")
        } catch (error: Exception) {
            if (error is CancellationException) {
                readerModeHandoffState.clearImmediately()
                throw error
            }
            pendingEpubRelayoutSourceJson = null
            pendingEpubRelayoutAnchor = null
            readerModeHandoffState.clearImmediately()
            presentedReaderAppearance = previousPresented
            acceptedReaderAppearance = previousAccepted
            presentedFixedLayoutSpread = previousPresentedSpread
            if (requestedSource != previousAccepted) {
                onReaderAppearanceChange(previousAccepted)
            }
            if (requestedSpread != previousPresentedSpread) {
                activeFixedLayoutSpread = previousPresentedSpread
                onFixedLayoutSpreadChange(previousPresentedSpread)
            }
            readerMessage = appearanceApplyFailedMessage
            ReaderTrace.event(
                "appearance_submit_failed",
                bookId = opened.book.id,
                sessionId = readerViewModel.traceSessionId(),
                details = "error=${error::class.java.simpleName} ${traceDetails}"
            )
        } finally {
            rendererPreferencesSettling = false
        }
    }

    LaunchedEffect(
        readerSessionInstanceId,
        navigator,
        opened.book.id,
        bookHighlights,
        presentedReaderAppearance.theme
    ) {
        val decorable = navigator as? DecorableNavigator ?: return@LaunchedEffect
        val highlightTint = readerHighlightTint(presentedReaderAppearance.theme)
        val decorations = bookHighlights.mapNotNull { item ->
            val locator = runCatching { Locator.fromJSON(JSONObject(item.locatorJson)) }.getOrNull()
                ?: return@mapNotNull null
            Decoration(
                id = item.id,
                locator = locator,
                style = Decoration.Style.Highlight(tint = highlightTint)
            )
        }
        decorable.applyDecorations(decorations, HIGHLIGHT_GROUP)
        if (opened.format == BookFormat.EPUB) {
            paperCurlState.invalidateSnapshotSource()
        }
    }

    val readerCanvas = readerCanvasColor(presentedReaderAppearance.theme)
    val chromeColors = remember(presentedReaderAppearance.theme) {
        readerAccessColors(presentedReaderAppearance.theme)
    }
    val readerChromeBackground = chromeColors.background
    val readerChromeForeground = chromeColors.foreground
    val readerChromeMuted = readerChromeForeground.copy(alpha = 0.56f)
    val readerChromeAccent = chromeColors.accent
    val readerBookTitle = opened.book.title.ifBlank {
        stringResource(R.string.common_untitled_book)
    }
    val readerSurfaceLabel = stringResource(R.string.reader_surface_label)
    val controlsActionLabel = stringResource(
        if (touchExplorationEnabled) {
            R.string.reader_show_controls
        } else if (controlsVisible) {
            R.string.reader_hide_controls
        } else {
            R.string.reader_show_controls
        }
    )
    val contextControl = readerContextControlFor(opened.format)
    val progressLabel = formatPercent(progress.coerceIn(0f, 1f))
    val progressDescription = stringResource(R.string.reader_percent_read_text, progressLabel)
    val etaLabel = timeRemainingEstimate?.takeIf { it.remainingPages > 0 }?.let {
        stringResource(R.string.reader_eta_remaining, formatReaderEtaDuration(it.centerMillis))
    }
    val etaDescription = timeRemainingEstimate?.takeIf { it.remainingPages > 0 }?.let {
        stringResource(R.string.reader_eta_range_description,
            formatReaderEtaDuration(it.lowMillis), formatReaderEtaDuration(it.highMillis),
            readingPace?.observedIntervals ?: 0)
    }
    val focusGuideVisible =
        activeFocusGuide.mode != ReaderFocusGuideMode.OFF &&
            readerSessionReady &&
            !rendererPreferencesSettling &&
            !closeInFlight &&
            !selectionModeActive &&
            !entryVisible &&
            !showNotebook &&
            !showAppearance &&
            !showPdfZoom &&
            !showTts &&
            pendingNoteLocatorJson == null &&
            footnote == null &&
            !imageLoading &&
            imageViewer == null

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { newSize ->
                val previousSize = readerViewportSize
                if (
                    shouldCancelReaderPreviewForViewportChange(
                        previousSize = previousSize,
                        newSize = newSize,
                        paperPreviewActive = paperCurlState.active,
                        slidePreviewActive = slidePageState.active
                    )
                ) {
                    val paperCancelled =
                        paperInputListener?.forceCancelPendingTurn() == true
                    val slideCancelled =
                        slideInputListener?.forceCancelPendingTurn() == true
                    if (!paperCancelled && paperCurlState.active) {
                        paperCurlState.clearImmediately()
                    }
                    if (!slideCancelled && slidePageState.active) {
                        slidePageState.clearImmediately()
                    }
                    ReaderTrace.event(
                        "reader_preview_cancelled_for_resize",
                        bookId = opened.book.id,
                        sessionId = readerSessionInstanceId,
                        details =
                            "from=${previousSize.width}x${previousSize.height} " +
                                "to=${newSize.width}x${newSize.height}"
                    )
                }
                if (
                    previousSize != IntSize.Zero &&
                    previousSize != newSize
                ) {
                    paperCurlState.invalidateSnapshotSource()
                    if (readerModeHandoffState.snapshot != null) {
                        readerModeHandoffState.clearImmediately()
                        scope.launch {
                            delay(VeilMotion.FRAME_SETTLE_MS)
                            readerModeHandoffState.releaseBufferIfIdle()
                        }
                    }
                    viewportRelayoutPending = true
                    viewportRelayoutJob?.cancel()
                    val expectedSessionId = readerSessionInstanceId
                    viewportRelayoutJob = scope.launch {
                        delay(READER_VIEWPORT_REFLOW_QUIET_MS)
                        if (
                            readerAsyncResultBelongsToSession(
                                currentSessionInstanceId = latestReaderSessionInstanceId.value,
                                expectedSessionInstanceId = expectedSessionId
                            )
                        ) {
                            latestNavigator.value?.currentLocator?.value?.let { locator ->
                                recordLocator(locator, ReaderLocatorEvent.RELAYOUT_CHECKPOINT)
                            }
                            viewportRelayoutPending = false
                            viewportRelayoutJob = null
                        }
                    }
                }
                readerViewportSize = newSize
            }
            .background(readerCanvas)
            .semantics {
                contentDescription = readerSurfaceLabel
                onClick(label = controlsActionLabel) {
                    readerViewModel.onUserInteraction(readerSessionInstanceId)
                    controlsVisible = if (touchExplorationEnabled) true else !controlsVisible
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
            tag = readerFragmentTag(
                bookId = opened.book.id,
                readerSessionInstanceId = readerSessionInstanceId
            ),
            onNavigatorReady = onNavigatorReady,
            onDisposePublication = onDisposePublication,
            modifier = Modifier.fillMaxSize()
        )

        if (
            opened.format == BookFormat.EPUB &&
            !fixedLayoutPublication
        ) {
            ReaderPageAtmosphere(
                theme = presentedReaderAppearance.theme,
                navigationMode = presentedReaderAppearance.navigationMode,
                paperPatina = presentedReaderAppearance.paperPatina.toFloat(),
                progress = progress,
                progression = (navigator as? OverflowableNavigator)
                    ?.overflow
                    ?.value
                    ?.readingProgression
                    ?: ReadingProgression.LTR,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (
            opened.format == BookFormat.EPUB &&
            !presentedReaderAppearance.scroll &&
            presentedReaderAppearance.pageTurnStyle == PageTurnStyle.PAPER
        ) {
            PaperCurlOverlay(
                state = paperCurlState,
                patina = presentedReaderAppearance.paperPatina.toFloat(),
                tone = when (presentedReaderAppearance.theme) {
                    ReaderTheme.PAPER -> MaterialPageTone.LIGHT
                    ReaderTheme.SEPIA -> MaterialPageTone.SEPIA
                    ReaderTheme.DUSK,
                    ReaderTheme.OLED -> MaterialPageTone.DARK
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (
            opened.format == BookFormat.EPUB &&
            !presentedReaderAppearance.scroll &&
            presentedReaderAppearance.pageTurnStyle == PageTurnStyle.SLIDE
        ) {
            SlidePageOverlay(
                state = slidePageState,
                modifier = Modifier.fillMaxSize()
            )
        }

        ReaderModeHandoffOverlay(
            state = readerModeHandoffState,
            modifier = Modifier.fillMaxSize()
        )

        // No exit animation: OFF and blocking UI remove the guide immediately.
        // The Canvas has no input or semantics modifiers; Readium retains ownership.
        if (focusGuideVisible) {
            ReaderFocusGuideOverlay(
                settings = activeFocusGuide,
                theme = presentedReaderAppearance.theme,
                modifier = Modifier.fillMaxSize()
            )
        }

        boundaryPulseSide?.let { side ->
            ReaderBoundaryPulse(
                side = side,
                theme = presentedReaderAppearance.theme,
                alpha = boundaryPulseAlpha.value,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (imageLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VeilPalette.Ink.copy(alpha = 0.42f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = VeilPalette.Brass, strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.reader_image_viewer_loading),
                        color = VeilPalette.Moon,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { cancelImageLoad() }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible =
                !controlsVisible &&
                    readerSessionReady &&
                    !showNotebook &&
                    !showAppearance &&
                    !showPdfZoom &&
            !showTts &&
                    !selectionModeActive &&
                    !touchExplorationEnabled,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            enter = fadeIn(tween(if (reducedMotion) VeilMotion.REDUCED_MOTION_FADE_MS else VeilMotion.MICRO_FAST_MS)),
            exit = fadeOut(tween(if (reducedMotion) VeilMotion.REDUCED_MOTION_FADE_MS else VeilMotion.MICRO_FAST_MS))
        ) {
            ReaderAccessDock(
                settingsAction = when (contextControl) {
                    ReaderContextControl.APPEARANCE -> ReaderAction.APPEARANCE
                    ReaderContextControl.PDF_VIEW -> ReaderAction.ZOOM
                },
                settingsLabel = stringResource(
                    when (contextControl) {
                        ReaderContextControl.APPEARANCE -> R.string.reader_chrome_appearance
                        ReaderContextControl.PDF_VIEW -> R.string.reader_chrome_pdf_view
                    }
                ),
                background = readerChromeBackground,
                foreground = readerChromeForeground,
                accent = readerChromeAccent,
                onMenu = {
                    readerViewModel.onUserInteraction(readerSessionInstanceId)
                    controlsVisible = true
                },
                onSettings = {
                    readerViewModel.onUserInteraction(readerSessionInstanceId)
                    selectionActionModeCallback.dismissSelection()
                    when (contextControl) {
                        ReaderContextControl.APPEARANCE -> {
                            appearanceCloseJob?.cancel()
                            appearanceCloseJob = null
                            showAppearance = true
                        }
                        ReaderContextControl.PDF_VIEW -> showPdfZoom = true
                    }
                }
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
                    .statusBarsPadding()
                    .padding(start = 6.dp, top = 2.dp, end = 6.dp),
                shape = RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomEnd = 10.dp,
                    bottomStart = 10.dp
                ),
                color = readerChromeBackground,
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
                            stringResource(R.string.reader_close),
                            tint = readerChromeAccent
                        ) { closeReader() }

                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                readerBookTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = readerChromeForeground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                locationTitle.ifBlank {
                                    opened.book.author.trim().ifBlank {
                                        localizedBookFormatLabel(opened.format)
                                    }
                                },
                                color = readerChromeMuted,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (
                            opened.format == BookFormat.EPUB &&
                            navigator != null &&
                            VeilFeatureGates.enabled(
                                VeilRiskyFeature.IN_BOOK_SEARCH,
                                debugReview = BuildConfig.DEBUG
                            )
                        ) {
                            ReaderChromeButton(
                                ReaderAction.SEARCH,
                                stringResource(R.string.reader_chrome_search),
                                tint = readerChromeAccent
                            ) {
                                readerViewModel.onUserInteraction(readerSessionInstanceId)
                                selectionActionModeCallback.dismissSelection()
                                showReaderSearch = true
                            }
                        }

                        Text(
                            progressLabel,
                            modifier = Modifier.semantics {
                                contentDescription =
                                    listOfNotNull(progressDescription, etaDescription).joinToString(". ")
                            },
                            color = readerChromeAccent,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(1.dp),
                        color = readerChromeAccent,
                        trackColor = readerChromeForeground.copy(alpha = 0.10f),
                        drawStopIndicator = {}
                    )
                }
            }
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
                    .navigationBarsPadding()
                    .padding(start = 6.dp, end = 6.dp, bottom = 4.dp),
                shape = RoundedCornerShape(
                    topStart = 10.dp,
                    topEnd = 10.dp,
                    bottomEnd = 0.dp,
                    bottomStart = 0.dp
                ),
                color = readerChromeBackground,
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
                                        readerChromeAccent.copy(alpha = 0.42f),
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
                            label = stringResource(R.string.reader_chrome_notebook),
                            modifier = Modifier.weight(1f),
                            accent = readerChromeAccent,
                            foreground = readerChromeForeground
                        ) {
                            readerViewModel.onUserInteraction(readerSessionInstanceId)
                            selectionActionModeCallback.dismissSelection()
                            showNotebook = true
                        }

                        ReaderControl(
                            action = ReaderAction.BOOKMARK,
                            label = stringResource(R.string.reader_chrome_bookmark),
                            modifier = Modifier.weight(1f),
                            enabled = navigator != null,
                            accent = readerChromeAccent,
                            foreground = readerChromeForeground
                        ) {
                            readerViewModel.onUserInteraction(readerSessionInstanceId)
                            selectionActionModeCallback.dismissSelection()
                            val locator = navigator?.currentLocator?.value
                            if (locator != null) {
                                val expectedSessionId = readerSessionInstanceId
                                val bookId = opened.book.id
                                val locatorJson = locator.toVeilPersistedJson(opened.format)
                                val label =
                                    if (opened.format == BookFormat.PDF && pdfPageNumber(locator) != null) {
                                        activity.getString(R.string.pdf_bookmark_page, pdfPageNumber(locator))
                                    } else {
                                        "${formatPercent(progress)} · ${locator.title?.takeIf { it.isNotBlank() } ?: readerBookTitle}"
                                    }
                                scope.launch {
                                    try {
                                        val added = library.addBookmark(bookId, label, locatorJson)
                                        if (!readerAsyncResultBelongsToSession(
                                                currentSessionInstanceId = latestReaderSessionInstanceId.value,
                                                expectedSessionInstanceId = expectedSessionId
                                            )
                                        ) return@launch
                                        if (added) onSensoryEvent(VeilSensoryEvent.MARK)
                                        readerMessage = if (added) bookmarkSavedMessage else bookmarkDuplicateMessage
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        if (readerAsyncResultBelongsToSession(
                                                currentSessionInstanceId = latestReaderSessionInstanceId.value,
                                                expectedSessionInstanceId = expectedSessionId
                                            )
                                        ) readerMessage = bookmarkSaveFailedMessage
                                    }
                                }
                            }
                        }

                        ReaderControl(
                            action = ReaderAction.FOCUS,
                            label = stringResource(R.string.reader_focus),
                            modifier = Modifier.weight(1f).semantics {
                                stateDescription = focusGuideStateDescription
                            },
                            accent = if (activeFocusGuide.mode == ReaderFocusGuideMode.OFF) {
                                readerChromeAccent.copy(alpha = 0.58f)
                            } else {
                                readerChromeAccent
                            },
                            foreground = readerChromeForeground
                        ) {
                            readerViewModel.onUserInteraction(readerSessionInstanceId)
                            val updated = focusGuideState.toggle()
                            onFocusGuideChange(updated)
                            readerMessage = if (updated.mode == ReaderFocusGuideMode.OFF) {
                                focusGuideOffMessage
                            } else {
                                focusGuideOnMessage
                            }
                        }

                        ReaderControl(
                            action = when (contextControl) {
                                ReaderContextControl.APPEARANCE -> ReaderAction.APPEARANCE
                                ReaderContextControl.PDF_VIEW -> ReaderAction.ZOOM
                            },
                            label = stringResource(
                                when (contextControl) {
                                    ReaderContextControl.APPEARANCE -> R.string.reader_chrome_appearance
                                    ReaderContextControl.PDF_VIEW -> R.string.reader_chrome_pdf_view
                                }
                            ),
                            modifier = Modifier.weight(1f),
                            enabled = navigator != null,
                            accent = readerChromeAccent,
                            foreground = readerChromeForeground
                        ) {
                            readerViewModel.onUserInteraction(readerSessionInstanceId)
                            selectionActionModeCallback.dismissSelection()
                            when (contextControl) {
                                ReaderContextControl.APPEARANCE -> {
                                    appearanceCloseJob?.cancel()
                                    appearanceCloseJob = null
                                    showAppearance = true
                                }
                                ReaderContextControl.PDF_VIEW -> {
                                    showPdfZoom = true
                                }
                            }
                        }
                    }

                    if (ttsSession != null || ttsServiceController != null) {
                        TextButton(
                            onClick = {
                                readerViewModel.onUserInteraction(readerSessionInstanceId)
                                selectionActionModeCallback.dismissSelection()
                                showTts = true
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) { Text(stringResource(R.string.tts_title), color = readerChromeAccent) }
                    }
                    etaLabel?.let { label ->
                        Text(label, color = readerChromeMuted, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 14.dp).semantics {
                                contentDescription = etaDescription.orEmpty()
                            })
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
                border = BorderStroke(1.dp, readerChromeAccent.copy(alpha = 0.34f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = readerChromeBackground,
                    contentColor = readerChromeForeground
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        stringResource(R.string.reader_return),
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Brass.copy(alpha = 0.84f)
                    )
                    Text(
                        stringResource(R.string.reader_previous_location),
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

    imageViewer?.let { content ->
        ReaderImageViewer(
            content = content,
            title = stringResource(R.string.reader_image_viewer_title),
            closeLabel = stringResource(R.string.reader_image_viewer_close),
            zoomHint = stringResource(R.string.reader_image_viewer_hint),
            onDismiss = { imageViewer = null }
        )
    }

    footnote?.let { currentFootnote ->
        Dialog(
            onDismissRequest = { footnote = null },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 620.dp)
                        .fillMaxWidth()
                        .heightIn(max = 680.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = VeilPalette.Archive,
                    border = BorderStroke(
                        1.dp,
                        VeilPalette.Brass.copy(alpha = 0.42f)
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                stringResource(R.string.reader_footnote_eyebrow),
                                style = MaterialTheme.typography.labelSmall,
                                color = VeilPalette.Brass
                            )
                            Text(
                                currentFootnote.title
                                    ?: stringResource(R.string.reader_footnote_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = VeilPalette.Moon
                            )
                            BrassRule(Modifier.fillMaxWidth())
                            Text(
                                currentFootnote.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = VeilPalette.Moon.copy(alpha = 0.90f)
                            )
                            Button(
                                onClick = { footnote = null },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                shape = MaterialTheme.shapes.extraSmall,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VeilPalette.Brass,
                                    contentColor = Color(0xFF17120A)
                                )
                            ) {
                                Text(
                                    stringResource(
                                        R.string.reader_footnote_close
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingNoteLocatorJson?.let { pendingLocatorJson ->
        val existingHighlightId = pendingNoteHighlightId
        val isNewNoteDraft = existingHighlightId == null

        Dialog(
            onDismissRequest = {
                if (!noteSaving) {
                    dismissPendingSelectionNote()
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = !noteSaving,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .imePadding()
                    .padding(VeilSpacing.lg),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = VeilPalette.Archive,
                    border = BorderStroke(
                        1.dp,
                        VeilPalette.Brass.copy(alpha = 0.48f)
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box {
                        Column(
                            modifier = Modifier
                                .padding(VeilSpacing.lg)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                        ) {
                            VeilMicroLabel(
                                text = stringResource(R.string.reader_note_eyebrow),
                                strong = true
                            )
                            Text(
                                stringResource(R.string.notebook_note_dialog_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = VeilPalette.Moon
                            )
                            BrassRule(Modifier.fillMaxWidth())

                            pendingNoteQuote
                                .takeIf { it.isNotBlank() }
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
                                                stringResource(R.string.reader_selected_passage),
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
                                placeholder = {
                                    Text(stringResource(R.string.reader_note_hint))
                                },
                                minLines = 4,
                                maxLines = 8,
                                shape = MaterialTheme.shapes.extraSmall,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VeilPalette.Brass.copy(alpha = 0.84f),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(
                                        alpha = 0.72f
                                    ),
                                    focusedContainerColor = VeilPalette.Ink.copy(alpha = 0.36f),
                                    unfocusedContainerColor = VeilPalette.Ink.copy(alpha = 0.24f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                stringResource(
                                    R.string.reader_characters_count,
                                    pendingNoteText.length
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.70f
                                ),
                                modifier = Modifier.align(Alignment.End)
                            )

                            VeilAdaptiveDialogActions(
                                spacing = VeilSpacing.xs,
                                first = { actionModifier ->
                                    OutlinedButton(
                                        enabled = !noteSaving,
                                        onClick = ::dismissPendingSelectionNote,
                                        modifier = actionModifier.heightIn(min = 48.dp),
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Text(stringResource(R.string.common_cancel))
                                    }
                                },
                                second = { actionModifier ->
                                    Button(
                                        enabled = !noteSaving &&
                                            canSavePendingSelectionNote(
                                                isNewNote = isNewNoteDraft,
                                                note = pendingNoteText
                                            ),
                                        onClick = {
                                            val expectedSessionId = readerSessionInstanceId
                                            val noteToSave = pendingNoteText
                                            val quoteToSave = pendingNoteQuote
                                            val existingId = existingHighlightId
                                            scope.launch {
                                                noteSaving = true
                                                try {
                                                    val committed =
                                                        library.commitSelectionNote(
                                                            bookId = opened.book.id,
                                                            quote = quoteToSave,
                                                            locatorJson = pendingLocatorJson,
                                                            existingHighlightId = existingId,
                                                            note = noteToSave
                                                        ) ?: error(
                                                            "Selection note target is no longer available."
                                                        )
                                                    library.flushWrites()
                                                    if (
                                                        !readerAsyncResultBelongsToSession(
                                                            currentSessionInstanceId =
                                                                latestReaderSessionInstanceId.value,
                                                            expectedSessionInstanceId =
                                                                expectedSessionId
                                                        )
                                                    ) {
                                                        return@launch
                                                    }
                                                    if (committed.created) {
                                                        readerViewModel.onHighlightAdded(
                                                            expectedSessionId
                                                        )
                                                    }
                                                    readerViewModel.onNoteSaved(
                                                        expectedSessionId,
                                                        committed.highlight.id,
                                                        noteToSave
                                                    )
                                                    onSensoryEvent(VeilSensoryEvent.NOTE)
                                                    clearPendingSelectionNoteDraft()
                                                    readerMessage = noteSavedMessage
                                                } catch (cancelled: CancellationException) {
                                                    throw cancelled
                                                } catch (error: Exception) {
                                                    if (
                                                        readerAsyncResultBelongsToSession(
                                                            currentSessionInstanceId =
                                                                latestReaderSessionInstanceId.value,
                                                            expectedSessionInstanceId =
                                                                expectedSessionId
                                                        )
                                                    ) {
                                                        readerMessage = noteSaveFailedMessage
                                                    }
                                                } finally {
                                                    if (
                                                        readerAsyncResultBelongsToSession(
                                                            currentSessionInstanceId =
                                                                latestReaderSessionInstanceId.value,
                                                            expectedSessionInstanceId =
                                                                expectedSessionId
                                                        )
                                                    ) {
                                                        noteSaving = false
                                                    }
                                                }
                                            }
                                        },
                                        modifier = actionModifier.heightIn(min = 48.dp),
                                        shape = MaterialTheme.shapes.extraSmall,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = VeilPalette.Brass,
                                            contentColor = Color(0xFF17120A)
                                        )
                                    ) {
                                        Text(
                                            stringResource(
                                                if (noteSaving) {
                                                    R.string.reader_saving_note
                                                } else {
                                                    R.string.reader_save_note
                                                }
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReaderSearch) {
        key(readerSessionInstanceId) {
            ReaderBookSearchDialog(
                publication = opened.publication,
                onDismiss = { showReaderSearch = false },
                onResult = { result ->
                    showReaderSearch = false
                    val expectedSessionId = readerSessionInstanceId
                    scope.launch {
                        if (!settlePagePreviewsBeforeProgrammaticNavigation()) {
                            readerMessage = savedLocationFailedMessage
                            return@launch
                        }
                        if (
                            closeInFlight ||
                            !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) ||
                            !readerAsyncResultBelongsToSession(
                                currentSessionInstanceId = latestReaderSessionInstanceId.value,
                                expectedSessionInstanceId = expectedSessionId
                            )
                        ) return@launch
                        val nav = latestNavigator.value ?: return@launch
                        val origin = nav.currentLocator.value
                        val targetIdentity = result.toReaderNavigationIdentity()
                        if (!shouldStartReaderIdentityJump(
                                origin = origin.toReaderNavigationIdentity(),
                                target = targetIdentity
                            )
                        ) {
                            controlsVisible = false
                            return@launch
                        }
                        readerViewModel.onUserInteraction(expectedSessionId)
                        game.rebasePagePacing()
                        val token = beginProgrammaticNavigation(
                            originLocatorJson = origin.toVeilPersistedJson(opened.format),
                            targetIdentity = targetIdentity,
                            // A search hit is exploration, not a bookmark/note visit.
                            // The existing SEARCH_RESULT policy preserves the saved
                            // reading anchor until the user actually reads onward.
                            reason = ReaderNavigationReason.SEARCH_RESULT
                        )
                        if (nav.go(
                                result,
                                animated = shouldAnimateReaderJump(latestReducedMotion.value)
                            )
                        ) {
                            controlsVisible = false
                        } else {
                            cancelProgrammaticNavigation(token)
                            readerMessage = savedLocationFailedMessage
                        }
                    }
                }
            )
        }
    }

    if (showNotebook) {
        key(readerSessionInstanceId) {
            ReaderNotebook(
            opened = opened,
            readerSessionInstanceId = readerSessionInstanceId,
            currentHref = currentLocationHref,
            highlights = bookHighlights,
            bookmarks = bookBookmarks,
            passageVisits = bookPassageVisits,
            onDismiss = { showNotebook = false },
            onGo = { json ->
                val locator =
                    runCatching { Locator.fromJSON(JSONObject(json)) }.getOrNull()
                if (locator == null || navigator == null) {
                    showNotebook = false
                    readerMessage = savedLocationFailedMessage
                } else {
                    val expectedSessionId = readerSessionInstanceId
                    scope.launch {
                        if (!settlePagePreviewsBeforeProgrammaticNavigation()) {
                            showNotebook = false
                            readerMessage = savedLocationFailedMessage
                            return@launch
                        }
                        if (
                            !readerAsyncResultBelongsToSession(
                                currentSessionInstanceId =
                                    latestReaderSessionInstanceId.value,
                                expectedSessionInstanceId = expectedSessionId
                            )
                        ) {
                            return@launch
                        }

                        val nav = latestNavigator.value ?: return@launch
                        val originLocator = nav.currentLocator.value
                        val targetIdentity = locator.toReaderNavigationIdentity()
                        if (
                            !shouldStartReaderIdentityJump(
                                origin = originLocator.toReaderNavigationIdentity(),
                                target = targetIdentity
                            )
                        ) {
                            showNotebook = false
                            return@launch
                        }

                        readerViewModel.onUserInteraction(expectedSessionId)
                        game.rebasePagePacing()
                        val transactionToken = beginProgrammaticNavigation(
                            originLocatorJson =
                                originLocator.toVeilPersistedJson(opened.format),
                            targetIdentity = targetIdentity,
                            passageVisitLocatorJson = json,
                            reason = ReaderNavigationReason.SAVED_PASSAGE,
                            expectedPdfPage =
                                if (opened.format == BookFormat.PDF) {
                                    pdfPageNumber(locator)
                                } else {
                                    null
                                }
                        )
                        if (
                            nav.go(
                                locator,
                                animated =
                                    shouldAnimateReaderJump(latestReducedMotion.value)
                            )
                        ) {
                            showNotebook = false
                        } else {
                            cancelProgrammaticNavigation(transactionToken)
                            showNotebook = false
                            readerMessage = savedLocationFailedMessage
                        }
                    }
                }
            },
            onChapter = { link ->
                if (navigator == null) {
                    showNotebook = false
                    readerMessage = chapterFailedMessage
                } else {
                    val expectedSessionId = readerSessionInstanceId
                    scope.launch {
                        if (!settlePagePreviewsBeforeProgrammaticNavigation()) {
                            showNotebook = false
                            readerMessage = chapterFailedMessage
                            return@launch
                        }
                        if (
                            !readerAsyncResultBelongsToSession(
                                currentSessionInstanceId =
                                    latestReaderSessionInstanceId.value,
                                expectedSessionInstanceId = expectedSessionId
                            )
                        ) {
                            return@launch
                        }

                        val nav = latestNavigator.value ?: return@launch
                        val current = nav.currentLocator.value
                        val targetHref = readerEffectiveTargetHref(
                            currentHref = current.href.toString(),
                            targetHref = link.href.toString()
                        )
                        if (
                            !shouldStartReaderLinkJump(
                                currentHref = current.href.toString(),
                                targetHref = targetHref
                            )
                        ) {
                            showNotebook = false
                            return@launch
                        }

                        readerViewModel.onUserInteraction(expectedSessionId)
                        game.rebasePagePacing()
                        val transactionToken = beginProgrammaticNavigation(
                            originLocatorJson =
                                current.toVeilPersistedJson(opened.format),
                            targetHref = targetHref,
                            reason = ReaderNavigationReason.TABLE_OF_CONTENTS
                        )
                        if (
                            nav.go(
                                link,
                                animated =
                                    shouldAnimateReaderJump(latestReducedMotion.value)
                            )
                        ) {
                            showNotebook = false
                        } else {
                            cancelProgrammaticNavigation(transactionToken)
                            showNotebook = false
                            readerMessage = chapterFailedMessage
                        }
                    }
                }
            },
            onSaveNote = onSaveNote@{ id, note ->
                val expectedSessionId = readerSessionInstanceId
                check(library.updateHighlightNote(id, note)) { "The saved passage was removed." }
                if (
                    !readerAsyncResultBelongsToSession(
                        currentSessionInstanceId = latestReaderSessionInstanceId.value,
                        expectedSessionInstanceId = expectedSessionId
                    )
                ) {
                    return@onSaveNote
                }
                readerViewModel.onNoteSaved(expectedSessionId, id, note)
                onSensoryEvent(VeilSensoryEvent.NOTE)
                readerMessage = noteSavedMessage
            },
            onDeleteHighlight = { id ->
                scope.launch {
                    try { library.deleteHighlight(id) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) {
                        if (latestReaderSessionInstanceId.value == readerSessionInstanceId) {
                            readerMessage = activity.getString(R.string.notice_highlight_delete_failed)
                        }
                    }
                }
            },
            onDeleteBookmark = { id ->
                val expectedSessionId = readerSessionInstanceId
                scope.launch {
                    try {
                        library.deleteBookmark(id)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        if (readerAsyncResultBelongsToSession(
                                currentSessionInstanceId = latestReaderSessionInstanceId.value,
                                expectedSessionInstanceId = expectedSessionId
                            )
                        ) readerMessage = bookmarkDeleteFailedMessage
                    }
                }
            }
            )
        }
    }

    if (showAppearance) {
        Dialog(
            onDismissRequest = { closeAppearanceAfterRendererSettles() },
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
                    EpubAppearancePanel(
                        appearance = readerAppearance,
                        readerChromeAutoHideEnabled = readerChromeAutoHideEnabled,
                        onReaderChromeAutoHideChange = onReaderChromeAutoHideChange,
                        fixedLayout = fixedLayoutPublication,
                        fixedLayoutSpread = activeFixedLayoutSpread,
                        publicationLanguage = publicationLanguage,
                        onSpreadChange = { mode ->
                            readerViewModel.onUserInteraction(readerSessionInstanceId)
                            activeFixedLayoutSpread = mode
                            onFixedLayoutSpreadChange(mode)
                        },
                        onChange = {
                            readerViewModel.onUserInteraction(readerSessionInstanceId)
                            onReaderAppearanceChange(it)
                        },
                        onDone = { finalAppearance ->
                            closeAppearanceAfterRendererSettles(finalAppearance)
                        },
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxSize()
                    )
                }
            }
        }
    }

    if (showTts) {
        val speechStateSource = ttsServiceController?.state
            ?: ttsSession?.state
            ?: remember { kotlinx.coroutines.flow.MutableStateFlow(ReaderTtsState()) }
        val speechState by speechStateSource.collectAsStateWithLifecycle()
        val voiceCatalogSource = ttsServiceController?.voices
            ?: remember {
                kotlinx.coroutines.flow.MutableStateFlow<List<ReaderTtsVoice>>(emptyList())
            }
        val voiceCatalog by voiceCatalogSource.collectAsStateWithLifecycle()
        val voiceCatalogLoadingSource = ttsServiceController?.voiceCatalogLoading
            ?: remember { kotlinx.coroutines.flow.MutableStateFlow(false) }
        val voiceCatalogLoading by voiceCatalogLoadingSource.collectAsStateWithLifecycle()
        val voiceCatalogProblemSource = ttsServiceController?.voiceCatalogProblem
            ?: remember {
                kotlinx.coroutines.flow.MutableStateFlow<ReaderTtsProblem?>(null)
            }
        val voiceCatalogProblem by voiceCatalogProblemSource.collectAsStateWithLifecycle()
        val previewProblemSource = ttsServiceController?.previewProblem
            ?: remember {
                kotlinx.coroutines.flow.MutableStateFlow<ReaderTtsProblem?>(null)
            }
        val previewProblem by previewProblemSource.collectAsStateWithLifecycle()
        val checkpointSource = ttsServiceController?.checkpoint
            ?: foregroundTtsCheckpoint?.checkpoint
            ?: remember {
                kotlinx.coroutines.flow.MutableStateFlow<ReaderTtsCheckpoint?>(null)
            }
        val listeningCheckpoint by checkpointSource.collectAsStateWithLifecycle()
        val activeListeningCheckpoint = listeningCheckpoint?.takeIf {
            it.request.bookId == opened.book.id
        }
        val sleepDeadlineSource = ttsServiceController?.sleepDeadlineEpochMs
            ?: ttsSession?.sleepDeadlineEpochMs
            ?: remember { kotlinx.coroutines.flow.MutableStateFlow<Long?>(null) }
        val sleepDeadlineEpochMs by sleepDeadlineSource.collectAsStateWithLifecycle()

        val activeSegmentSource = ttsServiceController?.activeSegmentText
            ?: remember { kotlinx.coroutines.flow.MutableStateFlow<String?>(null) }
        val activeSegmentText by activeSegmentSource.collectAsStateWithLifecycle()

        fun dismissSpeechControls() {
            ttsStartSerial += 1
            ttsStartJob?.cancel()
            ttsStartPending = false
            showTts = false
        }
        Dialog(
            onDismissRequest = ::dismissSpeechControls,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ReaderListeningMode(
                book = opened.book,
                state = speechState,
                activeText = activeSegmentText ?: speechState.activeText,
                supported = ttsSession != null || ttsServiceController != null,
                settings = speechSettingsState.value,
                startPending = ttsStartPending,
                startFailed = ttsStartFailed,
                publicationLanguage = publicationLanguage,
                voiceCatalogSupported = ttsServiceController != null,
                voices = voiceCatalog,
                voiceCatalogLoading = voiceCatalogLoading,
                voiceCatalogProblem = voiceCatalogProblem,
                previewProblem = previewProblem,
                sleepDeadlineEpochMs = sleepDeadlineEpochMs,
                onSetSleepTimer = { minutes ->
                    if (ttsServiceController != null) {
                        ttsServiceController.setSleepTimer(minutes)
                    } else {
                        ttsSession?.setSleepTimer(minutes)
                    }
                },
                listeningPositionAvailable = activeListeningCheckpoint != null,
                onSyncListeningPosition = {
                    val checkpoint = activeListeningCheckpoint
                    if (checkpoint != null) {
                        scope.launch syncListeningPosition@{
                            val nav = latestNavigator.value ?: return@syncListeningPosition
                            if (!settlePagePreviewsBeforeProgrammaticNavigation()) {
                                return@syncListeningPosition
                            }
                            val target = runCatching {
                                Locator.fromJSON(JSONObject(checkpoint.locatorJson))
                            }.getOrNull() ?: return@syncListeningPosition
                            val origin = nav.currentLocator.value
                            val targetIdentity = target.toReaderNavigationIdentity()
                            if (
                                !shouldStartReaderIdentityJump(
                                    origin = origin.toReaderNavigationIdentity(),
                                    target = targetIdentity
                                )
                            ) {
                                showTts = false
                                return@syncListeningPosition
                            }
                            readerViewModel.onUserInteraction(readerSessionInstanceId)
                            game.rebasePagePacing()
                            val token = beginProgrammaticNavigation(
                                originLocatorJson =
                                    origin.toVeilPersistedJson(opened.format),
                                targetIdentity = targetIdentity,
                                reason = ReaderNavigationReason.LISTENING_POSITION
                            )
                            if (
                                nav.go(
                                    target,
                                    animated = shouldAnimateReaderJump(
                                        latestReducedMotion.value
                                    )
                                )
                            ) {
                                showTts = false
                            } else {
                                cancelProgrammaticNavigation(token)
                                readerMessage = savedLocationFailedMessage
                            }
                        }
                    }
                },
                onRefreshVoices = { ttsServiceController?.refreshVoiceCatalog() },
                onPreviewVoice = { languageTag, voiceId, sample ->
                    ttsServiceController?.previewVoice(
                        languageTag = languageTag,
                        voiceId = voiceId,
                        sample = sample,
                        settings = speechSettingsState.value
                    )
                },
                onStart = {
                    val nav = latestNavigator.value as? EpubNavigatorFragment
                    if (
                        nav != null &&
                        (ttsSession != null || ttsServiceController != null) &&
                        !ttsStartPending
                    ) {
                        val requestSerial = ++ttsStartSerial
                        ttsStartPending = true
                        ttsStartFailed = false
                        ttsStartJob = scope.launch {
                            try {
                                val locator = kotlinx.coroutines.withTimeout(5_000L) {
                                    nav.firstVisibleElementLocator()
                                }
                                if (readerCanCompleteTtsStart(
                                    expectedOwnerId = readerSessionInstanceId,
                                    currentOwnerId = latestReaderSessionInstanceId.value,
                                    navigatorStillOwned = latestNavigator.value === nav,
                                    controlsVisible = showTts,
                                    playbackAllowed = latestTtsCanPlay.value(),
                                    requestSerial = requestSerial,
                                    currentSerial = ttsStartSerial
                                )) {
                                    if (locator == null) {
                                        ttsStartFailed = true
                                    } else if (ttsServiceController != null) {
                                        ttsServiceController.start(
                                            bookId = opened.book.id,
                                            locatorJson = locator.toJSON().toString(),
                                            settings = latestTtsSettings.value
                                        )
                                    } else {
                                        ttsSession?.start(
                                            locator,
                                            ReaderTtsPreferences(
                                                speed = latestTtsSettings.value.speed.toFloat(),
                                                pitch = latestTtsSettings.value.pitch.toFloat(),
                                                preferredVoiceIds =
                                                    latestTtsSettings.value.preferredVoiceIds
                                            )
                                        )
                                    }
                                }
                            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                                if (requestSerial == ttsStartSerial) ttsStartFailed = true
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                if (requestSerial == ttsStartSerial) ttsStartFailed = true
                            } finally {
                                if (requestSerial == ttsStartSerial) ttsStartPending = false
                            }
                        }
                    }
                },
                onResume = { ttsServiceController?.resume() ?: ttsSession?.resume() },
                onPause = { ttsServiceController?.pause() ?: ttsSession?.pause() },
                onPrevious = {
                    ttsServiceController?.previous() ?: ttsSession?.previous()
                },
                onNext = {
                    ttsServiceController?.next() ?: ttsSession?.next()
                },
                onStop = {
                    ttsStartSerial += 1
                    ttsStartJob?.cancel()
                    ttsStartPending = false
                    ttsServiceController?.stop() ?: ttsSession?.stop()
                },
                onSettingsChange = { updated ->
                    speechSettingsState.update(updated)
                    onTtsSettingsChange(updated)
                    ttsServiceController?.updateSettings(updated)
                        ?: ttsSession?.updatePreferences(
                            ReaderTtsPreferences(
                                speed = updated.speed.toFloat(),
                                pitch = updated.pitch.toFloat(),
                                preferredVoiceIds = updated.preferredVoiceIds
                            )
                        )
                },
                onDone = ::dismissSpeechControls
            )
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
                reducedMotion = reducedMotion,
                onAppearanceChange = { updated ->
                    readerViewModel.onUserInteraction(readerSessionInstanceId)
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

internal fun shouldAnimateReaderJump(reducedMotion: Boolean): Boolean =
    !reducedMotion

internal enum class ReaderBoundaryKind {
    BEGINNING,
    END
}

internal fun readerBoundaryKind(
    side: PaperCurlSide,
    progression: ReadingProgression
): ReaderBoundaryKind =
    when (paperTurnDirectionFor(side, progression)) {
        PaperTurnDirection.BACKWARD -> ReaderBoundaryKind.BEGINNING
        PaperTurnDirection.FORWARD -> ReaderBoundaryKind.END
    }



internal fun shouldAwaitReaderAppearanceClose(
    rendererPreferencesSettling: Boolean,
    presented: ReaderAppearance,
    expected: ReaderAppearance?
): Boolean =
    rendererPreferencesSettling ||
        (expected != null && presented != expected)

internal fun readerPreferencesNeedSubmission(
    previousPresented: ReaderAppearance,
    previousAccepted: ReaderAppearance,
    previousSpread: ReaderFixedLayoutSpread,
    requestedPresented: ReaderAppearance,
    requestedSource: ReaderAppearance,
    requestedSpread: ReaderFixedLayoutSpread
): Boolean =
    previousPresented != requestedPresented ||
        previousAccepted != requestedSource ||
        previousSpread != requestedSpread

// Compare what Readium receives, rather than Veil-only animation/material options.
@OptIn(ExperimentalReadiumApi::class)
internal fun readerRendererPreferencesChanged(
    format: BookFormat,
    previous: ReaderAppearance,
    requested: ReaderAppearance,
    previousSpread: ReaderFixedLayoutSpread,
    requestedSpread: ReaderFixedLayoutSpread
): Boolean = when (format) {
    BookFormat.EPUB -> previous.toEpubPreferences(previousSpread) !=
        requested.toEpubPreferences(requestedSpread)
    BookFormat.PDF -> previous.toPdfiumPreferences() != requested.toPdfiumPreferences()
    else -> false
}

private const val READER_APPEARANCE_APPLY_TIMEOUT_MS = 6_000L
private const val READER_LOCATOR_QUERY_TIMEOUT_MS = 1_000L
private const val READER_PREVIEW_SETTLE_TIMEOUT_MS = 2_000L
private const val READER_APPEARANCE_CLOSE_TIMEOUT_MS = 2_000L
private const val READER_VIEWPORT_REFLOW_QUIET_MS = 650L

@OptIn(ExperimentalReadiumApi::class, DelicateReadiumApi::class)
private fun createReaderFactory(
    opened: OpenedPublication,
    appearance: ReaderAppearance,
    fixedLayoutSpread: ReaderFixedLayoutSpread,
    selectionActionModeCallback: ActionMode.Callback,
    epubNavigatorListener: EpubNavigatorFragment.Listener,
    pdfLinkHandler: LinkHandler
): FragmentFactory = when (opened.format) {
    BookFormat.EPUB -> EpubNavigatorFactory(opened.publication)
        .createFragmentFactory(
            initialLocator = opened.initialLocator,
            listener = epubNavigatorListener,
            initialPreferences = appearance.toEpubPreferences(
                fixedLayoutSpread = fixedLayoutSpread
            ),
            configuration = EpubNavigatorFragment.Configuration {
                useReadiumCssFontSize = false
                disablePageTurnsWhileScrolling = false
                this.selectionActionModeCallback = selectionActionModeCallback
                decorationTemplates = HtmlDecorationTemplates.defaultTemplates(
                    alpha = READER_HIGHLIGHT_ALPHA,
                    experimentalPositioning = true
                )
            }
        )

    BookFormat.PDF -> PdfNavigatorFactory(
        publication = opened.publication,
        pdfEngineProvider = PdfiumEngineProvider(
            defaults = PdfiumDefaults(),
            listener = object : PdfiumEngineProvider.Listener {
                override fun onConfigurePdfView(configurator: com.github.barteksc.pdfviewer.PDFView.Configurator) {
                    // Public adapter hook; Readium installs its own page/tap/render listeners after it.
                    configurator.enableAnnotationRendering(true).linkHandler(pdfLinkHandler)
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

internal fun readerFragmentTag(
    bookId: String,
    readerSessionInstanceId: String
): String =
    "reader-${bookId.trim()}-${readerSessionInstanceId.trim()}"

@Composable
internal fun ReaderFocusGuideOverlay(
    settings: ReaderFocusGuideSettings,
    theme: ReaderTheme,
    modifier: Modifier = Modifier
) {
    val normalized = settings.normalized()
    val dimBase = when (theme) {
        ReaderTheme.PAPER,
        ReaderTheme.SEPIA -> Color(0xFF18130F)
        ReaderTheme.DUSK,
        ReaderTheme.OLED -> Color.Black
    }
    val edge = when (theme) {
        ReaderTheme.PAPER,
        ReaderTheme.SEPIA -> Color(0xFF765C36)
        ReaderTheme.DUSK,
        ReaderTheme.OLED -> VeilPalette.Brass
    }

    Canvas(modifier) {
        val band = readerFocusGuideBand(size.height, normalized) ?: return@Canvas
        val alpha = normalized.dimStrength.toFloat()

        if (band.top > 0f) {
            drawRect(
                color = dimBase.copy(alpha = alpha),
                topLeft = Offset.Zero,
                size = Size(size.width, band.top)
            )
        }
        if (band.bottom < size.height) {
            drawRect(
                color = dimBase.copy(alpha = alpha),
                topLeft = Offset(0f, band.bottom),
                size = Size(size.width, size.height - band.bottom)
            )
        }

        val edgeAlpha =
            if (normalized.mode == ReaderFocusGuideMode.LINE) 0.34f else 0.16f
        val edgeWidth =
            if (normalized.mode == ReaderFocusGuideMode.LINE) 1.25.dp.toPx()
            else 0.75.dp.toPx()

        drawLine(
            color = edge.copy(alpha = edgeAlpha),
            start = Offset(0f, band.top),
            end = Offset(size.width, band.top),
            strokeWidth = edgeWidth
        )
        drawLine(
            color = edge.copy(alpha = edgeAlpha),
            start = Offset(0f, band.bottom),
            end = Offset(size.width, band.bottom),
            strokeWidth = edgeWidth
        )
    }
}

private data class ReaderFootnote(
    val title: String?,
    val text: String
)

internal enum class ReaderAction { BACK, NOTEBOOK, BOOKMARK, FOCUS, APPEARANCE, ZOOM, SEARCH }

@Composable
internal fun ReaderChromeButton(
    action: ReaderAction,
    accessibilityLabel: String,
    tint: Color = VeilPalette.Brass,
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
            tint = tint
        )
    }
}

@Composable
private fun ReaderControl(
    action: ReaderAction,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = VeilPalette.Brass,
    foreground: Color = VeilPalette.Moon,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minWidth = 0.dp, minHeight = 56.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 5.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = foreground,
            disabledContentColor = foreground.copy(alpha = 0.28f)
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
                tint = if (enabled) accent else foreground.copy(alpha = 0.28f)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (enabled) foreground.copy(alpha = 0.78f)
                    else foreground.copy(alpha = 0.28f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
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
            ReaderAction.SEARCH -> {
                drawCircle(color = tint, radius = w * 0.27f,
                    center = Offset(w * 0.43f, h * 0.42f), style = stroke)
                drawLine(color = tint,
                    start = Offset(w * 0.64f, h * 0.64f),
                    end = Offset(w * 0.84f, h * 0.84f),
                    strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
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
            ReaderAction.FOCUS -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * .14f, h * .22f),
                    size = Size(w * .72f, h * .56f),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    style = stroke
                )
                drawLine(
                    tint,
                    Offset(w * .18f, h * .50f),
                    Offset(w * .82f, h * .50f),
                    stroke.width,
                    StrokeCap.Round
                )
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
internal fun EpubAppearancePanel(
    appearance: ReaderAppearance,
    fixedLayout: Boolean,
    fixedLayoutSpread: ReaderFixedLayoutSpread,
    publicationLanguage: String?,
    onSpreadChange: (ReaderFixedLayoutSpread) -> Unit,
    onChange: (ReaderAppearance) -> Unit,
    onDone: (ReaderAppearance) -> Unit,
    modifier: Modifier = Modifier,
    initiallyAdvanced: Boolean = false,
    readerChromeAutoHideEnabled: Boolean = true,
    onReaderChromeAutoHideChange: (Boolean) -> Unit = {}
) {
    com.veilreader.app.ui.VeilSystemBars(lightBackground = false)
    val formatPercent = rememberVeilPercentFormatter()
    val formatNumber = rememberVeilNumberFormatter()
    var draft by remember { mutableStateOf(appearance) }
    var hasPendingDraft by remember { mutableStateOf(false) }
    var sliderPending by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(initiallyAdvanced) }
    val condensedApproach = com.veilreader.app.ui.theme.condenseRealmApproach(
        LocalDensity.current.fontScale, with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height.toDp().value.toInt()
        })
    var showPreview by remember(condensedApproach) { mutableStateOf(!condensedApproach) }
    val capabilities = readerAppearanceCapabilities(
        fixedLayout = fixedLayout,
        languageTag = publicationLanguage,
        continuousScroll = draft.navigationMode == ReaderNavigationMode.SCROLL
    )
    val publisherStyleLabel = stringResource(R.string.reader_publisher_styling)
    val textSizeLabel = stringResource(R.string.settings_text_size)
    val quickReadingMode = if (capabilities.continuousScrollEditable) {
        draft.readingMode
    } else {
        ReaderReadingMode.PAGED
    }

    LaunchedEffect(appearance) {
        when {
            !hasPendingDraft -> draft = appearance
            appearance == draft -> hasPendingDraft = false
        }
    }

    fun updateDraft(value: ReaderAppearance) {
        draft = value
        hasPendingDraft = true
        sliderPending = false
        onChange(value)
    }

    fun previewDraft(value: ReaderAppearance) {
        draft = value
        hasPendingDraft = true
        sliderPending = true
    }

    fun previewTypography(value: ReaderAppearance) {
        previewDraft(value.copy(publisherStyles = false))
    }

    fun commitDraft() {
        if (sliderPending) {
            sliderPending = false
            onChange(draft)
        }
    }

    val latestDraftForDispose by rememberUpdatedState(draft)
    val latestSliderPendingForDispose by rememberUpdatedState(sliderPending)
    val latestOnChangeForDispose by rememberUpdatedState(onChange)
    DisposableEffect(Unit) {
        onDispose {
            if (latestSliderPendingForDispose) {
                latestOnChangeForDispose(latestDraftForDispose)
            }
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = VeilSpacing.lg, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (!condensedApproach) {
                VeilMicroLabel(
                    text = stringResource(R.string.reader_instruments),
                    strong = true
                )
                BrassRule(Modifier.width(76.dp))
            }
            Text(
                stringResource(R.string.settings_appearance_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                stringResource(R.string.reader_changes_live),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (!capabilities.fixedLayout) {
            if (condensedApproach) {
                TextButton(
                    onClick = { showPreview = !showPreview },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text(
                        stringResource(
                            if (showPreview) R.string.reader_hide_reading_preview
                            else R.string.reader_show_reading_preview
                        )
                    )
                }
            }
            if (showPreview) {
                ReaderAppearancePreview(
                    appearance = draft,
                    typographyEnabled = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (capabilities.fixedLayout) {
            ReaderCapabilityNotice(
                text = stringResource(R.string.reader_fixed_layout_notice)
            )
        }

        if (capabilities.fixedLayout) {
            VeilMicroLabel(
                text = stringResource(R.string.reader_fixed_spread_title),
                strong = true
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stackedSpreadChoices = shouldStackDenseChoices(
                    widthDp = maxWidth.value.toInt(),
                    fontScale = LocalDensity.current.fontScale,
                    optionCount = ReaderFixedLayoutSpread.entries.size
                )
                if (stackedSpreadChoices) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReaderFixedLayoutSpread.entries.forEach { mode ->
                            ReaderAppearanceChoice(
                                label = when (mode) {
                                    ReaderFixedLayoutSpread.AUTO ->
                                        stringResource(R.string.reader_fixed_spread_auto)
                                    ReaderFixedLayoutSpread.SINGLE ->
                                        stringResource(R.string.reader_fixed_spread_single)
                                    ReaderFixedLayoutSpread.DUAL ->
                                        stringResource(R.string.reader_fixed_spread_dual)
                                },
                                selected = fixedLayoutSpread == mode,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onSpreadChange(mode) }
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReaderFixedLayoutSpread.entries.forEach { mode ->
                            ReaderAppearanceChoice(
                                label = when (mode) {
                                    ReaderFixedLayoutSpread.AUTO ->
                                        stringResource(R.string.reader_fixed_spread_auto)
                                    ReaderFixedLayoutSpread.SINGLE ->
                                        stringResource(R.string.reader_fixed_spread_single)
                                    ReaderFixedLayoutSpread.DUAL ->
                                        stringResource(R.string.reader_fixed_spread_dual)
                                },
                                selected = fixedLayoutSpread == mode,
                                modifier = Modifier.weight(1f),
                                onClick = { onSpreadChange(mode) }
                            )
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.reader_fixed_spread_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BrassRule(Modifier.fillMaxWidth())
        }

        if (!capabilities.fixedLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf(
                    false to stringResource(R.string.reader_quick),
                    true to stringResource(R.string.reader_advanced)
                ).forEach { (advanced, label) ->
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
                        VeilMaterials.ElevatedSurface
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
                        VeilMicroLabel(
                            text = label,
                            color = if (selected) {
                                VeilPalette.Moon
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
        }

        if (!showAdvanced) {
            if (!capabilities.fixedLayout) {
            VeilMicroLabel(
                text = stringResource(R.string.settings_publication_theme),
                strong = true
            )

            listOf(
                listOf(
                    ReaderTheme.PAPER to stringResource(R.string.settings_reader_paper),
                    ReaderTheme.SEPIA to stringResource(R.string.settings_reader_sepia)
                ),
                listOf(
                    ReaderTheme.DUSK to stringResource(R.string.settings_reader_dusk),
                    ReaderTheme.OLED to stringResource(R.string.settings_reader_oled)
                )
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
                VeilMicroLabel(
                    text = stringResource(R.string.settings_text_size),
                    modifier = Modifier.weight(1f),
                    strong = true
                )
                Text(
                    formatPercent(draft.fontScale.toFloat()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = draft.fontScale.toFloat(),
                onValueChange = { previewDraft(draft.withFontScale(it.toDouble())) },
                onValueChangeFinished = ::commitDraft,
                valueRange = .75f..1.8f,
                enabled = capabilities.typographyEditable,
                modifier = Modifier.semantics {
                    contentDescription = textSizeLabel
                    stateDescription = formatPercent(draft.fontScale.toFloat())
                }
            )

            BrassRule(Modifier.fillMaxWidth())
            }

            VeilMicroLabel(
                text = stringResource(R.string.settings_reading_mode_title),
                strong = true
            )
            ReaderReadingModeSelector(
                selected = quickReadingMode,
                scrollEnabled = capabilities.continuousScrollEditable,
                onSelect = { mode ->
                    updateDraft(draft.withReadingMode(mode))
                }
            )
            Text(
                stringResource(
                    if (quickReadingMode == ReaderReadingMode.SCROLL) {
                        R.string.settings_mode_scroll_description
                    } else {
                        R.string.settings_reading_mode_paged_description
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            BrassRule(Modifier.fillMaxWidth())

            VeilMicroLabel(
                text = stringResource(R.string.settings_page_turn_title),
                strong = true
            )
            if (quickReadingMode == ReaderReadingMode.SCROLL) {
                ReaderCapabilityNotice(
                    text = stringResource(R.string.settings_page_turn_scroll_hint)
                )
            } else {
                val paperTurnAvailable = MaterialPageEngineRollout.isEnabled()
                ReaderPageTurnSelector(
                    selected = draft.pageTurnStyle,
                    paperEnabled = paperTurnAvailable,
                    onSelect = { style ->
                        updateDraft(draft.withPageTurnStyle(style))
                    }
                )
                if (!paperTurnAvailable) {
                    ReaderCapabilityNotice(
                        text = stringResource(R.string.settings_mode_curl_unavailable)
                    )
                }
                Text(
                    localizedPageTurnStyleDescription(draft.pageTurnStyle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BrassRule(Modifier.fillMaxWidth())
            ReaderMenuAutoHideControl(readerChromeAutoHideEnabled, onReaderChromeAutoHideChange)
        } else {
            VeilMicroLabel(
                text = stringResource(R.string.reader_typography_layout),
                strong = true
            )

            if (draft.publisherStyles) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = VeilPalette.Archive.copy(alpha = 0.60f),
                    border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.28f))
                ) {
                    Text(
                        stringResource(R.string.reader_publisher_override_note),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(stringResource(R.string.settings_font_family), style = MaterialTheme.typography.titleSmall)
            ReaderAppearanceChoiceGroup(
                entries = ReaderFontFamily.entries.toList(),
                selected = draft.fontFamily,
                label = { family ->
                    when (family) {
                        ReaderFontFamily.PUBLISHER -> stringResource(R.string.settings_book_default)
                        ReaderFontFamily.SERIF -> stringResource(R.string.settings_font_serif)
                        ReaderFontFamily.SANS_SERIF -> stringResource(R.string.settings_font_sans)
                        ReaderFontFamily.MONOSPACE -> stringResource(R.string.settings_font_mono)
                        ReaderFontFamily.OPEN_DYSLEXIC -> stringResource(R.string.settings_font_opendyslexic)
                        ReaderFontFamily.ACCESSIBLE_DFA -> stringResource(R.string.settings_font_accessible)
                        ReaderFontFamily.IA_WRITER_DUOSPACE -> stringResource(R.string.settings_font_duospace)
                    }
                },
                enabled = { capabilities.typographyEditable },
                scrollWhenInline = true,
                onSelect = { family -> updateDraft(draft.withFontFamily(family)) }
            )

            ReaderAppearanceNullableSlider(
                label = stringResource(R.string.settings_font_weight),
                value = draft.fontWeight,
                valueRange = 0f..2.5f,
                nullPreviewValue = 1f,
                valueLabel = { formatPercent(it) },
                onValueChange = {
                    previewTypography(draft.withFontWeight(it.toDouble()))
                },
                onValueChangeFinished = ::commitDraft,
                onReset = { updateDraft(draft.copy(fontWeight = null)) },
                enabled = capabilities.typographyEditable
            )

            ReaderAppearanceSlider(
                label = stringResource(R.string.settings_line_height),
                value = draft.lineHeight.toFloat(),
                valueRange = 1.1f..2.0f,
                valueLabel = { "${formatNumber(it)}×" },
                onValueChange = { previewTypography(draft.withLineHeight(it.toDouble())) },
                onValueChangeFinished = ::commitDraft,
                enabled = capabilities.typographyEditable
            )

            ReaderAppearanceSlider(
                label = stringResource(R.string.settings_page_margins),
                value = draft.pageMargins.toFloat(),
                valueRange = 0.5f..2.0f,
                valueLabel = { formatPercent(it) },
                onValueChange = { previewTypography(draft.withPageMargins(it.toDouble())) },
                onValueChangeFinished = ::commitDraft,
                enabled = capabilities.typographyEditable
            )

            Text(stringResource(R.string.settings_text_alignment), style = MaterialTheme.typography.titleSmall)
            ReaderAppearanceChoiceGroup(
                entries = ReaderTextAlignment.entries.toList(),
                selected = draft.textAlignment,
                label = { alignment ->
                    when (alignment) {
                        ReaderTextAlignment.PUBLISHER -> stringResource(R.string.settings_book_default)
                        ReaderTextAlignment.START -> stringResource(R.string.settings_align_start)
                        ReaderTextAlignment.JUSTIFY -> stringResource(R.string.settings_align_justify)
                        ReaderTextAlignment.CENTER -> stringResource(R.string.settings_align_center)
                    }
                },
                enabled = { capabilities.textAlignmentEditable },
                onSelect = { alignment -> updateDraft(draft.withTextAlignment(alignment)) }
            )

            Text(stringResource(R.string.settings_columns), style = MaterialTheme.typography.titleSmall)
            ReaderAppearanceChoiceGroup(
                entries = ReaderColumnMode.entries.toList(),
                selected = draft.columnMode,
                label = { mode ->
                    when (mode) {
                        ReaderColumnMode.AUTO -> stringResource(R.string.settings_column_auto)
                        ReaderColumnMode.ONE -> stringResource(R.string.settings_column_one)
                        ReaderColumnMode.TWO -> stringResource(R.string.settings_column_two)
                    }
                },
                enabled = { capabilities.columnsEditable },
                onSelect = { mode ->
                    updateDraft(
                        draft.copy(
                            columnMode = mode,
                            publisherStyles = if (mode == ReaderColumnMode.AUTO) {
                                draft.publisherStyles
                            } else {
                                false
                            }
                        )
                    )
                }
            )
            if (capabilities.columnsEditable) {
                Text(
                    stringResource(R.string.reader_columns_adaptive_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!capabilities.columnsEditable && !capabilities.fixedLayout) {
                Text(
                    stringResource(R.string.reader_columns_paged_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
            }

            BrassRule(Modifier.fillMaxWidth())

            VeilMicroLabel(
                text = stringResource(R.string.reader_spacing_shaping),
                strong = true
            )
            if (capabilities.rtlPublication && !capabilities.fixedLayout) {
                ReaderCapabilityNotice(
                    text = stringResource(R.string.reader_rtl_typography_notice)
                )
            } else if (capabilities.cjkPublication && !capabilities.fixedLayout) {
                ReaderCapabilityNotice(
                    text = stringResource(R.string.reader_cjk_typography_notice)
                )
            }

            ReaderAppearanceTriState(
                title = stringResource(R.string.reader_hyphenation),
                value = draft.hyphenation,
                onChange = { option ->
                    updateDraft(
                        draft.copy(
                            hyphenation = option,
                            publisherStyles = if (option == ReaderPreferenceToggle.DEFAULT) {
                                draft.publisherStyles
                            } else {
                                false
                            }
                        )
                    )
                },
                enabled = capabilities.hyphenationEditable
            )
            ReaderAppearanceTriState(
                title = stringResource(R.string.reader_ligatures),
                value = draft.ligatures,
                onChange = { option ->
                    updateDraft(
                        draft.copy(
                            ligatures = option,
                            publisherStyles = if (option == ReaderPreferenceToggle.DEFAULT) {
                                draft.publisherStyles
                            } else {
                                false
                            }
                        )
                    )
                },
                enabled = capabilities.ligaturesEditable
            )
            ReaderAppearanceTriState(
                title = stringResource(R.string.reader_text_normalization),
                value = draft.textNormalization,
                onChange = { option ->
                    updateDraft(
                        draft.copy(
                            textNormalization = option,
                            publisherStyles = if (option == ReaderPreferenceToggle.DEFAULT) {
                                draft.publisherStyles
                            } else {
                                false
                            }
                        )
                    )
                },
                enabled = capabilities.typographyEditable
            )

            ReaderAppearanceNullableSlider(
                label = stringResource(R.string.reader_paragraph_spacing),
                value = draft.paragraphSpacing,
                valueRange = 0f..2f,
                nullPreviewValue = 0f,
                valueLabel = { "${formatNumber(it)}×" },
                onValueChange = { previewDraft(draft.withParagraphSpacing(it.toDouble())) },
                onValueChangeFinished = ::commitDraft,
                onReset = { updateDraft(draft.copy(paragraphSpacing = null)) },
                enabled = capabilities.typographyEditable
            )
            ReaderAppearanceNullableSlider(
                label = stringResource(R.string.reader_paragraph_indent),
                value = draft.paragraphIndent,
                valueRange = 0f..3f,
                nullPreviewValue = 0f,
                valueLabel = { "${formatNumber(it)}×" },
                onValueChange = { previewDraft(draft.withParagraphIndent(it.toDouble())) },
                onValueChangeFinished = ::commitDraft,
                onReset = { updateDraft(draft.copy(paragraphIndent = null)) },
                enabled = capabilities.paragraphIndentEditable
            )
            ReaderAppearanceNullableSlider(
                label = stringResource(R.string.reader_letter_spacing),
                value = draft.letterSpacing,
                valueRange = 0f..0.2f,
                nullPreviewValue = 0f,
                valueLabel = { formatNumber(it) },
                onValueChange = { previewDraft(draft.withLetterSpacing(it.toDouble())) },
                onValueChangeFinished = ::commitDraft,
                onReset = { updateDraft(draft.copy(letterSpacing = null)) },
                enabled = capabilities.letterSpacingEditable
            )
            ReaderAppearanceNullableSlider(
                label = stringResource(R.string.reader_word_spacing),
                value = draft.wordSpacing,
                valueRange = 0f..1f,
                nullPreviewValue = 0f,
                valueLabel = { formatNumber(it) },
                onValueChange = { previewDraft(draft.withWordSpacing(it.toDouble())) },
                onValueChangeFinished = ::commitDraft,
                onReset = { updateDraft(draft.copy(wordSpacing = null)) },
                enabled = capabilities.wordSpacingEditable
            )
            ReaderAppearanceNullableSlider(
                label = stringResource(R.string.reader_type_scale),
                value = draft.typeScale,
                valueRange = 1f..2f,
                nullPreviewValue = 1f,
                valueLabel = { "${formatNumber(it)}×" },
                onValueChange = { previewDraft(draft.withTypeScale(it.toDouble())) },
                onValueChangeFinished = ::commitDraft,
                onReset = { updateDraft(draft.copy(typeScale = null)) },
                enabled = capabilities.typographyEditable
            )

            BrassRule(Modifier.fillMaxWidth())

            if (draft.theme == ReaderTheme.PAPER || draft.theme == ReaderTheme.SEPIA) {
                ReaderAppearanceSlider(
                    label = stringResource(R.string.settings_paper_age),
                    value = draft.paperPatina.toFloat(),
                    valueRange = 0f..1f,
                    valueLabel = { formatPercent(it) },
                    onValueChange = { previewDraft(draft.withPaperPatina(it.toDouble())) },
                    onValueChangeFinished = ::commitDraft
                )
                Text(
                    stringResource(R.string.settings_paper_age_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val darkTheme = draft.theme == ReaderTheme.DUSK || draft.theme == ReaderTheme.OLED
            Text(stringResource(R.string.settings_dark_images), style = MaterialTheme.typography.titleSmall)
            ReaderAppearanceChoiceGroup(
                entries = ReaderDarkImageTreatment.entries.toList(),
                selected = draft.darkImageTreatment,
                label = { treatment ->
                    when (treatment) {
                        ReaderDarkImageTreatment.NONE -> stringResource(R.string.settings_dark_images_original)
                        ReaderDarkImageTreatment.DARKEN -> stringResource(R.string.settings_dark_images_darken)
                        ReaderDarkImageTreatment.INVERT -> stringResource(R.string.settings_dark_images_invert)
                    }
                },
                enabled = { darkTheme && !capabilities.fixedLayout },
                onSelect = { treatment -> updateDraft(draft.withDarkImageTreatment(treatment)) }
            )
            Text(
                stringResource(
                    if (darkTheme) R.string.settings_dark_images_hint
                    else R.string.settings_dark_images_unavailable
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        stringResource(R.string.reader_publisher_styling),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        stringResource(R.string.reader_publisher_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = draft.publisherStyles,
                    enabled = capabilities.typographyEditable,
                    onCheckedChange = {
                        updateDraft(draft.copy(publisherStyles = it))
                    },
                    modifier = Modifier.semantics {
                        contentDescription = publisherStyleLabel
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    updateDraft(
                        ReaderAppearance().copy(
                            scroll = draft.scroll,
                            pageTurnStyle = draft.pageTurnStyle,
                            screenBrightness = draft.screenBrightness
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.extraSmall,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)
                )
            ) {
                Text(stringResource(R.string.reader_reset_appearance))
            }
        }

        BrassRule(Modifier.fillMaxWidth())

        ReaderBrightnessControls(
            appearance = draft,
            onChange = ::updateDraft
        )

        Button(
            onClick = {
                commitDraft()
                onDone(draft)
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = MaterialTheme.shapes.extraSmall,
            colors = ButtonDefaults.buttonColors(
                containerColor = VeilPalette.DeepBrass,
                contentColor = VeilPalette.Moon
            )
        ) {
            Text(stringResource(R.string.reader_back_to_reading))
        }
    }
}

@Composable
internal fun ReaderCapabilityNotice(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Archive.copy(alpha = 0.62f),
        border = BorderStroke(
            1.dp,
            VeilPalette.Brass.copy(alpha = 0.26f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun <T> ReaderAppearanceChoiceGroup(
    entries: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    enabled: (T) -> Boolean = { true },
    scrollWhenInline: Boolean = false,
    onSelect: (T) -> Unit
) {
    if (entries.isEmpty()) return

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = shouldStackDenseChoices(
            widthDp = maxWidth.value.toInt(),
            fontScale = LocalDensity.current.fontScale,
            optionCount = entries.size
        )

        when {
            stacked -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    entries.forEach { entry ->
                        ReaderAppearanceChoice(
                            label = label(entry),
                            selected = selected == entry,
                            enabled = enabled(entry),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onSelect(entry) }
                        )
                    }
                }
            }

            scrollWhenInline -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    entries.forEach { entry ->
                        ReaderAppearanceChoice(
                            label = label(entry),
                            selected = selected == entry,
                            enabled = enabled(entry),
                            onClick = { onSelect(entry) }
                        )
                    }
                }
            }

            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    entries.forEach { entry ->
                        ReaderAppearanceChoice(
                            label = label(entry),
                            selected = selected == entry,
                            enabled = enabled(entry),
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderAppearanceChoice(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val foreground = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        selected -> VeilPalette.Moon
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.extraSmall,
        color = if (selected) {
            VeilMaterials.ElevatedSurface
        } else {
            VeilPalette.Archive.copy(alpha = 0.66f)
        },
        contentColor = foreground,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) {
                VeilPalette.Brass.copy(alpha = 0.76f)
            } else {
                VeilPalette.Brass.copy(alpha = 0.22f)
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = foreground,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReaderAppearanceSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: (Float) -> String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
    enabled: Boolean = true
) {
    val safeValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val valueDescription = valueLabel(safeValue)

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stackedHeader = shouldStackDenseChoices(
            widthDp = maxWidth.value.toInt(),
            fontScale = LocalDensity.current.fontScale,
            optionCount = 2
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (stackedHeader) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(label, style = MaterialTheme.typography.titleSmall)
                    Text(
                        valueDescription,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (enabled) 1f else 0.48f
                        )
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(
                        valueDescription,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (enabled) 1f else 0.48f
                        )
                    )
                }
            }
            Slider(
                value = safeValue,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                enabled = enabled,
                modifier = Modifier.semantics {
                    contentDescription = label
                    stateDescription = valueDescription
                }
            )
        }
    }
}

@Composable
private fun ReaderAppearanceNullableSlider(
    label: String,
    value: Double?,
    valueRange: ClosedFloatingPointRange<Float>,
    nullPreviewValue: Float,
    valueLabel: (Float) -> String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
    onReset: () -> Unit,
    enabled: Boolean = true
) {
    val safeValue = (value?.toFloat() ?: nullPreviewValue)
        .coerceIn(valueRange.start, valueRange.endInclusive)
    val valueDescription = if (value == null) {
        stringResource(R.string.settings_book_default)
    } else {
        valueLabel(safeValue)
    }

    @Composable
    fun ValueAndReset() {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                valueDescription,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onReset,
                enabled = enabled && value != null,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.reader_value_reset))
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stackedHeader = shouldStackDenseChoices(
            widthDp = maxWidth.value.toInt(),
            fontScale = LocalDensity.current.fontScale,
            optionCount = 3
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (stackedHeader) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(label, style = MaterialTheme.typography.titleSmall)
                    ValueAndReset()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    ValueAndReset()
                }
            }
            Slider(
                value = safeValue,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                enabled = enabled,
                modifier = Modifier.semantics {
                    contentDescription = label
                    stateDescription = valueDescription
                }
            )
        }
    }
}

@Composable
private fun ReaderAppearanceTriState(
    title: String,
    value: ReaderPreferenceToggle,
    onChange: (ReaderPreferenceToggle) -> Unit,
    enabled: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = shouldStackDenseChoices(
                widthDp = maxWidth.value.toInt(),
                fontScale = LocalDensity.current.fontScale,
                optionCount = ReaderPreferenceToggle.entries.size
            )

            if (stacked) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReaderPreferenceToggle.entries.forEach { option ->
                        ReaderAppearanceChoice(
                            label = when (option) {
                                ReaderPreferenceToggle.DEFAULT -> stringResource(R.string.settings_book_default)
                                ReaderPreferenceToggle.ON -> stringResource(R.string.reader_value_on)
                                ReaderPreferenceToggle.OFF -> stringResource(R.string.reader_value_off)
                            },
                            selected = value == option,
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onChange(option) }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReaderPreferenceToggle.entries.forEach { option ->
                        ReaderAppearanceChoice(
                            label = when (option) {
                                ReaderPreferenceToggle.DEFAULT -> stringResource(R.string.settings_book_default)
                                ReaderPreferenceToggle.ON -> stringResource(R.string.reader_value_on)
                                ReaderPreferenceToggle.OFF -> stringResource(R.string.reader_value_off)
                            },
                            selected = value == option,
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            onClick = { onChange(option) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderAppearancePreview(
    appearance: ReaderAppearance,
    typographyEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val (paperArgb, inkArgb) = readiumThemeColors(appearance.theme)
    val paper = Color(paperArgb)
    val ink = Color(inkArgb)
    val previewMargins = if (typographyEnabled) appearance.pageMargins.toFloat() else 1f
    val previewScale = if (typographyEnabled) appearance.fontScale.toFloat() else 1f
    val previewLineHeight = if (typographyEnabled) appearance.lineHeight.toFloat() else 1.45f
    val margin = (14f + 12f * previewMargins).dp
    val sampleSize = (15f * previewScale).coerceIn(11f, 23f).sp
    val sampleLineHeight =
        (sampleSize.value * previewLineHeight).coerceIn(15f, 38f).sp
    val sampleProgression = if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
        ReadingProgression.RTL
    } else {
        ReadingProgression.LTR
    }

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
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stackPreviewHeader = shouldStackDenseChoices(
                    widthDp = maxWidth.value.toInt(),
                    fontScale = LocalDensity.current.fontScale,
                    optionCount = 2
                )
                if (stackPreviewHeader) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        VeilMicroLabel(
                            text = stringResource(R.string.reader_sample_preview),
                            strong = true
                        )
                        Text(
                            localizedReaderMotionSummary(appearance),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VeilMicroLabel(
                            text = stringResource(R.string.reader_sample_preview),
                            modifier = Modifier.weight(1f),
                            strong = true
                        )
                        Text(
                            localizedReaderMotionSummary(appearance),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End
                        )
                    }
                }
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
                    paperPatina = appearance.paperPatina.toFloat(),
                    progress = 0.42f,
                    progression = sampleProgression,
                    modifier = Modifier.matchParentSize()
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = margin, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.reader_sample_chapter),
                        style = MaterialTheme.typography.labelSmall,
                        color = ink.copy(alpha = 0.58f)
                    )
                    Text(
                        stringResource(R.string.reader_sample_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = ink
                    )
                    Text(
                        stringResource(R.string.reader_sample_body),
                        fontSize = sampleSize,
                        lineHeight = sampleLineHeight,
                        color = ink.copy(alpha = 0.92f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun localizedReaderMotionSummary(appearance: ReaderAppearance): String {
    if (appearance.readingMode == ReaderReadingMode.SCROLL) {
        return stringResource(R.string.settings_mode_scroll)
    }

    val readingMode = stringResource(R.string.settings_mode_paged)
    val pageTurn = stringResource(
        when (appearance.pageTurnStyle) {
            PageTurnStyle.PAPER -> R.string.settings_mode_curl
            PageTurnStyle.SLIDE -> R.string.settings_mode_slide
            PageTurnStyle.NONE -> R.string.settings_page_turn_none
        }
    )
    return "$readingMode · $pageTurn"
}

@Composable
internal fun localizedPageTurnStyleDescription(style: PageTurnStyle): String =
    stringResource(
        when (style) {
            PageTurnStyle.PAPER -> R.string.settings_mode_curl_description
            PageTurnStyle.SLIDE -> R.string.settings_mode_slide_description
            PageTurnStyle.NONE -> R.string.settings_mode_paged_description
        }
    )

@Composable
internal fun ReaderReadingModeSelector(
    selected: ReaderReadingMode,
    scrollEnabled: Boolean,
    onSelect: (ReaderReadingMode) -> Unit
) {
    val choices = listOf(
        ReaderReadingMode.PAGED to ReaderNavigationMode.PAGED,
        ReaderReadingMode.SCROLL to ReaderNavigationMode.SCROLL
    )

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = shouldStackDenseChoices(
            widthDp = maxWidth.value.toInt(),
            fontScale = LocalDensity.current.fontScale,
            optionCount = choices.size
        )

        if (stacked) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                choices.forEach { (mode, previewMode) ->
                    val active = selected == mode
                    val enabled = mode != ReaderReadingMode.SCROLL || scrollEnabled
                    ReaderModeChoice(
                        label = stringResource(
                            if (mode == ReaderReadingMode.SCROLL) {
                                R.string.settings_mode_scroll
                            } else {
                                R.string.settings_mode_paged
                            }
                        ),
                        previewMode = previewMode,
                        active = active,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelect(mode) }
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                choices.forEach { (mode, previewMode) ->
                    val active = selected == mode
                    val enabled = mode != ReaderReadingMode.SCROLL || scrollEnabled
                    ReaderModeChoice(
                        label = stringResource(
                            if (mode == ReaderReadingMode.SCROLL) {
                                R.string.settings_mode_scroll
                            } else {
                                R.string.settings_mode_paged
                            }
                        ),
                        previewMode = previewMode,
                        active = active,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(mode) }
                    )
                }
            }
        }
    }
}

internal fun readerPageTurnStyleEnabled(
    style: PageTurnStyle,
    selectorEnabled: Boolean,
    materialPageEnabled: Boolean
): Boolean =
    selectorEnabled &&
        (style != PageTurnStyle.PAPER || materialPageEnabled)

@Composable
internal fun ReaderPageTurnSelector(
    selected: PageTurnStyle,
    enabled: Boolean = true,
    paperEnabled: Boolean = MaterialPageEngineRollout.isEnabled(),
    onSelect: (PageTurnStyle) -> Unit
) {
    val choices = listOf(
        Triple(
            PageTurnStyle.PAPER,
            ReaderNavigationMode.PAPER_CURL,
            stringResource(R.string.settings_mode_curl)
        ),
        Triple(
            PageTurnStyle.SLIDE,
            ReaderNavigationMode.SLIDE,
            stringResource(R.string.settings_mode_slide)
        ),
        Triple(
            PageTurnStyle.NONE,
            ReaderNavigationMode.PAGED,
            stringResource(R.string.settings_page_turn_none)
        )
    )

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = shouldStackDenseChoices(
            widthDp = maxWidth.value.toInt(),
            fontScale = LocalDensity.current.fontScale,
            optionCount = choices.size
        )

        if (stacked) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                choices.forEach { (style, previewMode, label) ->
                    val choiceEnabled = readerPageTurnStyleEnabled(
                        style = style,
                        selectorEnabled = enabled,
                        materialPageEnabled = paperEnabled
                    )
                    ReaderModeChoice(
                        label = label,
                        previewMode = previewMode,
                        active = selected == style,
                        enabled = choiceEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { if (choiceEnabled) onSelect(style) }
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                choices.forEach { (style, previewMode, label) ->
                    val choiceEnabled = readerPageTurnStyleEnabled(
                        style = style,
                        selectorEnabled = enabled,
                        materialPageEnabled = paperEnabled
                    )
                    ReaderModeChoice(
                        label = label,
                        previewMode = previewMode,
                        active = selected == style,
                        enabled = choiceEnabled,
                        modifier = Modifier.weight(1f),
                        onClick = { if (choiceEnabled) onSelect(style) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderModeChoice(
    label: String,
    previewMode: ReaderNavigationMode,
    active: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 58.dp)
            .selectable(
                selected = active,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.extraSmall,
        color = if (active) {
            VeilMaterials.ElevatedSurface
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.46f)
        },
        border = BorderStroke(
            1.dp,
            if (active) {
                VeilPalette.Brass.copy(alpha = 0.82f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.46f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            ReaderMotionPreview(
                mode = previewMode,
                active = active,
                modifier = Modifier
                    .width(44.dp)
                    .height(28.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    !enabled ->
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    active -> VeilPalette.Moon
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
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
            VeilMaterials.ElevatedSurface
        } else {
            Color.Transparent
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
                    .heightIn(min = 64.dp)
                    .background(paper, MaterialTheme.shapes.extraSmall)
                    .border(
                        1.dp,
                        ink.copy(alpha = 0.18f),
                        MaterialTheme.shapes.extraSmall
                    )
            ) {
                Text(
                    stringResource(R.string.reader_theme_specimen),
                    modifier = Modifier.align(Alignment.Center).padding(VeilSpacing.sm),
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
internal fun ReaderAppearance.toEpubPreferences(
    fixedLayoutSpread: ReaderFixedLayoutSpread = ReaderFixedLayoutSpread.AUTO
): EpubPreferences {
    val safe = normalized()
    val colors = if (safe.publisherStyles) null else readiumThemeColors(safe.theme)
    return EpubPreferences(
        spread = fixedLayoutSpread.toReadiumSpread(),
        theme = when (safe.theme) {
            ReaderTheme.PAPER -> Theme.LIGHT
            ReaderTheme.SEPIA -> Theme.SEPIA
            ReaderTheme.DUSK, ReaderTheme.OLED -> Theme.DARK
        },
        imageFilter = when (safe.darkImageTreatment) {
            ReaderDarkImageTreatment.NONE -> null
            ReaderDarkImageTreatment.DARKEN -> ImageFilter.DARKEN
            ReaderDarkImageTreatment.INVERT -> ImageFilter.INVERT
        },
        backgroundColor = colors?.first?.let(::ReadiumColor),
        textColor = colors?.second?.let(::ReadiumColor),
        fontFamily = when (safe.fontFamily) {
            ReaderFontFamily.PUBLISHER -> null
            ReaderFontFamily.SERIF -> FontFamily.SERIF
            ReaderFontFamily.SANS_SERIF -> FontFamily.SANS_SERIF
            ReaderFontFamily.MONOSPACE -> FontFamily.MONOSPACE
            ReaderFontFamily.OPEN_DYSLEXIC -> FontFamily.OPEN_DYSLEXIC
            ReaderFontFamily.ACCESSIBLE_DFA -> FontFamily.ACCESSIBLE_DFA
            ReaderFontFamily.IA_WRITER_DUOSPACE -> FontFamily.IA_WRITER_DUOSPACE
        },
        fontSize = readiumFontSizeRatio(safe.fontScale),
        fontWeight = safe.fontWeight,
        lineHeight = safe.lineHeight,
        pageMargins = safe.pageMargins,
        paragraphSpacing = safe.paragraphSpacing,
        paragraphIndent = safe.paragraphIndent,
        letterSpacing = safe.letterSpacing,
        wordSpacing = safe.wordSpacing,
        typeScale = safe.typeScale,
        textAlign = when (safe.textAlignment) {
            ReaderTextAlignment.PUBLISHER -> null
            ReaderTextAlignment.START -> ReadiumTextAlign.START
            ReaderTextAlignment.JUSTIFY -> ReadiumTextAlign.JUSTIFY
            ReaderTextAlignment.CENTER -> ReadiumTextAlign.CENTER
        },
        columnCount = when (safe.columnMode) {
            ReaderColumnMode.AUTO -> ColumnCount.AUTO
            ReaderColumnMode.ONE -> ColumnCount.ONE
            ReaderColumnMode.TWO -> ColumnCount.TWO
        },
        hyphens = safe.hyphenation.toNullableBoolean(),
        ligatures = safe.ligatures.toNullableBoolean(),
        textNormalization = safe.textNormalization.toNullableBoolean(),
        scroll = safe.scroll,
        publisherStyles = safe.publisherStyles
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

internal fun ReaderFixedLayoutSpread.toReadiumSpread(): Spread? =
    when (this) {
        ReaderFixedLayoutSpread.AUTO -> null
        ReaderFixedLayoutSpread.SINGLE -> Spread.NEVER
        ReaderFixedLayoutSpread.DUAL -> Spread.ALWAYS
    }

internal fun readerHighlightTint(theme: ReaderTheme): Int =
    when (theme) {
        ReaderTheme.PAPER -> 0xFFB58A34.toInt()
        ReaderTheme.SEPIA -> 0xFFA8732E.toInt()
        ReaderTheme.DUSK -> 0xFFC7A253.toInt()
        ReaderTheme.OLED -> 0xFFD1B15B.toInt()
    }

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
private const val READER_HIGHLIGHT_ALPHA = 0.34


internal fun epubPreferencesMayRelayout(
    previous: ReaderAppearance,
    requested: ReaderAppearance
): Boolean =
    previous.scroll != requested.scroll ||
        previous.publisherStyles != requested.publisherStyles ||
        previous.fontFamily != requested.fontFamily ||
        previous.fontScale != requested.fontScale ||
        previous.fontWeight != requested.fontWeight ||
        previous.lineHeight != requested.lineHeight ||
        previous.pageMargins != requested.pageMargins ||
        previous.paragraphSpacing != requested.paragraphSpacing ||
        previous.paragraphIndent != requested.paragraphIndent ||
        previous.letterSpacing != requested.letterSpacing ||
        previous.wordSpacing != requested.wordSpacing ||
        previous.typeScale != requested.typeScale ||
        previous.textAlignment != requested.textAlignment ||
        previous.columnMode != requested.columnMode ||
        previous.hyphenation != requested.hyphenation ||
        previous.ligatures != requested.ligatures ||
        previous.textNormalization != requested.textNormalization

internal data class EpubPositionAnchorSample(
    val position: Int?,
    val totalProgression: Double?
)

internal fun stableEpubPositionAnchorIndex(
    currentPosition: Int?,
    currentTotalProgression: Double?,
    positions: List<EpubPositionAnchorSample>
): Int? {
    if (positions.isEmpty()) return null

    currentPosition?.let { position ->
        positions.indexOfFirst { it.position == position }
            .takeIf { it >= 0 }
            ?.let { return it }
    }

    val progression = currentTotalProgression ?: return null
    var bestIndex = -1
    var bestProgression = Double.NEGATIVE_INFINITY
    positions.forEachIndexed { index, candidate ->
        val candidateProgression = candidate.totalProgression ?: return@forEachIndexed
        if (candidateProgression <= progression && candidateProgression > bestProgression) {
            bestIndex = index
            bestProgression = candidateProgression
        }
    }
    return bestIndex.takeIf { it >= 0 } ?: 0
}

internal fun stableEpubPositionAnchor(
    current: Locator,
    positions: List<Locator>
): Locator? {
    val index = stableEpubPositionAnchorIndex(
        currentPosition = current.locations.position,
        currentTotalProgression = current.locations.totalProgression,
        positions = positions.map { candidate ->
            EpubPositionAnchorSample(
                position = candidate.locations.position,
                totalProgression = candidate.locations.totalProgression
            )
        }
    ) ?: return null
    return positions.getOrNull(index)
}

private fun Locator.withEpubCssSelectorFrom(precise: Locator?): Locator {
    val selector = precise?.locations?.get("cssSelector") as? String
    if (selector.isNullOrBlank()) return this
    return copy(
        locations = locations.copy(
            otherLocations = locations.otherLocations + ("cssSelector" to selector)
        )
    )
}


internal fun shouldRefreshPendingEpubRelayout(
    format: BookFormat,
    hasPendingAnchor: Boolean
): Boolean =
    format == BookFormat.EPUB && hasPendingAnchor

@Composable
private fun formatReaderEtaDuration(millis: Long): String {
    if (millis <= 0L) {
        return stringResource(R.string.reader_eta_minutes, 0)
    }

    val roundedMinutes = ((millis + 30_000L) / 60_000L)
        .coerceAtLeast(1L)
    val hours = roundedMinutes / 60L
    val minutes = roundedMinutes % 60L

    return if (hours > 0L) {
        stringResource(
            R.string.reader_eta_hours_minutes,
            hours,
            minutes
        )
    } else {
        stringResource(
            R.string.reader_eta_minutes,
            minutes
        )
    }
}
