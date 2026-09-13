package com.veilreader.app.ui.screens

import com.veilreader.app.ui.theme.LocalVeilMotion
import android.graphics.Color as AndroidColor
import android.animation.ValueAnimator
import android.view.ActionMode
import android.view.View
import android.widget.FrameLayout
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
import com.veilreader.app.data.GameRepository
import com.veilreader.app.data.LocalLibraryRepository
import com.veilreader.app.data.OpenedPublication
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderFont
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.domain.ReadingPolicy
import com.veilreader.app.ui.reader.PageCurlView
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
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.input.DragEvent
import org.readium.r2.navigator.input.Key
import org.readium.r2.navigator.input.KeyEvent
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.navigator.preferences.Color as ReadiumColor
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.FontFamily as ReadiumFontFamily
import org.readium.r2.navigator.preferences.TextAlign
import kotlin.math.abs
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
    val motion = LocalVeilMotion.current
    val activity = requireNotNull(LocalActivity.current as? FragmentActivity) {
        "Veil Reader requires a FragmentActivity host."
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val readerViewModel: ReaderViewModel = viewModel(
        key = "veil-reader-state",
        factory = remember(library, game) { ReaderViewModel.factory(library, game) }
    )
    val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
    val progress = if (readerState.bookId == opened.book.id) readerState.progress else opened.book.progress

    var navigator by remember(opened.book.id) { mutableStateOf<Navigator?>(null) }
    var curlView by remember(opened.book.id) { mutableStateOf<PageCurlView?>(null) }
    var controlsVisible by remember(opened.book.id) { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }
    val appearance by library.appearance.collectAsStateWithLifecycle()
    val latestAppearance by rememberUpdatedState(appearance)
    var showNotebook by remember { mutableStateOf(false) }

    DisposableEffect(activity, appearance.keepScreenOn) {
        val decor = activity.window.decorView
        val previous = decor.keepScreenOn
        decor.keepScreenOn = appearance.keepScreenOn
        onDispose { decor.keepScreenOn = previous }
    }
    DisposableEffect(curlView, lifecycle) {
        val view = curlView
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) view?.cancelTurn()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); view?.release() }
    }
    LaunchedEffect(appearance, showAppearance, showNotebook) { curlView?.cancelTurn() }
    LaunchedEffect(navigator, curlView) {
        navigator?.currentLocator?.collect { curlView?.onLocationChanged(it.toJSON().toString()) }
    }
    val highlights by library.highlights.collectAsState()
    val bookmarks by library.bookmarks.collectAsState()
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
        animationSpec = tween(motion.duration(220)),
        label = "reader-snackbar-offset"
    )

    val selectionActionModeCallback = remember(opened.book.id, library, readerViewModel, scope) {
        ReaderSelectionActionModeCallback(
            coroutineScope = scope,
            navigatorProvider = { navigator as? SelectableNavigator },
            onAction = { action, locator, quote ->
                try {
                    val locatorJson = locator.toJSON().toString()
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

    val fragmentFactory = remember(opened.book.id, selectionActionModeCallback) {
        createReaderFactory(opened, appearance, selectionActionModeCallback)
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

    DisposableEffect(navigator, opened.book.id, curlView) {
        val nav = navigator as? OverflowableNavigator
        val listener = object : InputListener {
            private fun turn(right: Boolean): Boolean {
                val reader = nav ?: return false
                if (curlView?.isTurning == true) return true
                readerViewModel.onUserInteraction()
                val prefs = latestAppearance
                val forward = right == (reader.overflow.value.readingProgression == ReadingProgression.LTR)
                val motion = !prefs.reduceMotion && ValueAnimator.areAnimatorsEnabled()
                val animate = motion && prefs.pageTurnStyle == PageTurnStyle.SLIDE
                val go = { if (forward) reader.goForward(animated = animate) else reader.goBackward(animated = animate) }
                val result = if (motion && prefs.pageTurnStyle == PageTurnStyle.CURL &&
                    opened.format == BookFormat.EPUB && !reader.overflow.value.scroll) {
                    curlView?.turn(reader.publicationView, reader.currentLocator.value.toJSON().toString(), right, go) ?: go()
                } else go()
                // Consume edge taps even at the beginning/end, so the chrome does not flash.
                if (!result) readerMessage = if (forward) "End of the book" else "Beginning of the book"
                return true
            }

            override fun onTap(event: TapEvent): Boolean {
                val reader = nav ?: return false
                readerViewModel.onUserInteraction()
                if (!reader.overflow.value.scroll) {
                    val width = reader.publicationView.width
                    if (event.point.x < width * .22f) return turn(false)
                    if (event.point.x > width * .78f) return turn(true)
                }
                controlsVisible = !controlsVisible
                return true
            }

            override fun onDrag(event: DragEvent): Boolean {
                val reader = nav ?: return false
                val prefs = latestAppearance
                if (opened.format != BookFormat.EPUB || reader.overflow.value.scroll ||
                    (prefs.pageTurnStyle == PageTurnStyle.SLIDE && !prefs.reduceMotion && ValueAnimator.areAnimatorsEnabled())) return false
                if (event.type == DragEvent.Type.End) {
                    val dx = event.offset.x
                    val threshold = 48 * activity.resources.displayMetrics.density
                    if (abs(dx) > threshold && abs(dx) > abs(event.offset.y) * 1.3f) turn(dx < 0)
                }
                return true
            }

            override fun onKey(event: KeyEvent): Boolean {
                if (event.type != KeyEvent.Type.Down || event.modifiers.isNotEmpty()) return false
                val rightIsForward = nav?.overflow?.value?.readingProgression == ReadingProgression.LTR
                return when (event.key) {
                    Key.ArrowRight -> turn(true)
                    Key.ArrowLeft -> turn(false)
                    Key.ArrowDown, Key.Space -> turn(rightIsForward)
                    Key.ArrowUp -> turn(!rightIsForward)
                    else -> false
                }
            }
        }
        nav?.addInputListener(listener)
        onDispose { nav?.removeInputListener(listener) }
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
            onNavigatorReady = { navigator = it },
            onCurlReady = { curlView = it },
            onDisposePublication = { opened.close() },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(tween(motion.duration(170))) + slideInVertically(tween(motion.duration(220))) { -it / 2 },
            exit = fadeOut(tween(motion.duration(120))) + slideOutVertically(tween(motion.duration(180))) { -it / 2 }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
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
            enter = fadeIn(tween(motion.duration(170))) + slideInVertically(tween(motion.duration(220))) { it / 2 },
            exit = fadeOut(tween(motion.duration(120))) + slideOutVertically(tween(motion.duration(180))) { it / 2 }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
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
                                locator.toJSON().toString()
                            )
                            readerMessage = if (added) "Bookmark saved" else "This location is already bookmarked"
                        }
                    }
                    ReaderControl(
                        action = ReaderAction.APPEARANCE,
                        label = "Appearance",
                        modifier = Modifier.weight(1f)
                    ) {
                        readerViewModel.onUserInteraction()
                        showAppearance = true
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
            highlights = highlights.filter { it.bookId == opened.book.id },
            bookmarks = bookmarks.filter { it.bookId == opened.book.id },
            onDismiss = { showNotebook = false },
            onGo = { json ->
                readerViewModel.onUserInteraction()
                game.pauseReading()
                val locator = runCatching { Locator.fromJSON(JSONObject(json)) }.getOrNull()
                if (locator != null && navigator?.go(locator, animated = !appearance.reduceMotion && ValueAnimator.areAnimatorsEnabled()) == true) {
                    showNotebook = false
                } else {
                    showNotebook = false
                    readerMessage = "That saved location could not be opened."
                }
            },
            onChapter = { link ->
                readerViewModel.onUserInteraction()
                game.pauseReading()
                if (navigator?.go(link, animated = !appearance.reduceMotion && ValueAnimator.areAnimatorsEnabled()) == true) {
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
            ReaderAppearancePanel(
                reflowable = opened.format == BookFormat.EPUB,
                appearance = appearance,
                onChange = {
                    readerViewModel.onUserInteraction()
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
    appearance: ReaderAppearance,
    selectionActionModeCallback: ActionMode.Callback
): FragmentFactory = when (opened.format) {
    BookFormat.EPUB -> EpubNavigatorFactory(opened.publication)
        .createFragmentFactory(
            initialLocator = opened.initialLocator,
            initialPreferences = appearance.toEpubPreferences(),
            configuration = EpubNavigatorFragment.Configuration {
                disablePageTurnsWhileScrolling = true
                this.selectionActionModeCallback = selectionActionModeCallback
                decorationTemplates = HtmlDecorationTemplates.defaultTemplates(
                    alpha = 1.0,
                    experimentalPositioning = true
                )
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
    onCurlReady: (PageCurlView) -> Unit,
    onDisposePublication: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerId = remember(tag) { View.generateViewId() }
    AndroidView(
        factory = { context ->
            // Keep the overlay in the same native ViewGroup as the publication. A separate
            // full-screen AndroidView above the host can steal Compose sibling hit testing.
            FrameLayout(context).apply {
                addView(FragmentContainerView(context).apply { id = containerId },
                    FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                addView(PageCurlView(context).also(onCurlReady),
                    FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            }
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

private enum class ReaderAction { BACK, NOTEBOOK, BOOKMARK, APPEARANCE }

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
                maxLines = 2,
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
        }
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
    fontFamily = when (font) {
        ReaderFont.ORIGINAL -> null
        ReaderFont.SERIF -> ReadiumFontFamily("serif")
        ReaderFont.SANS -> ReadiumFontFamily("sans-serif")
        ReaderFont.MONO -> ReadiumFontFamily("monospace")
    },
    textAlign = if (publisherStyles) null else if (justified) TextAlign.JUSTIFY else TextAlign.START,
    publisherStyles = publisherStyles
)

private const val HIGHLIGHT_GROUP = "veil-highlights"

