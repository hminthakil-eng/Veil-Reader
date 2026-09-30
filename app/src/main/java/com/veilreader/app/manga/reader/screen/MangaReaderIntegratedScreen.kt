package com.veilreader.app.manga.reader.screen

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.ImageLoader
import com.veilreader.app.R
import com.veilreader.app.manga.library.MangaProgressStore
import com.veilreader.app.manga.reader.MangaOrientationPolicy
import com.veilreader.app.manga.reader.MangaPageDirection
import com.veilreader.app.manga.reader.MangaReaderMode
import com.veilreader.app.manga.reader.image.AndroidMangaImageDimensionProbe
import com.veilreader.app.manga.reader.image.MangaCoilPage
import com.veilreader.app.manga.reader.image.MangaImageDeliveryPlan
import com.veilreader.app.manga.reader.image.MangaImageDeliveryPlanner
import com.veilreader.app.manga.reader.image.MangaImageDeliveryStrategy
import com.veilreader.app.manga.reader.image.MangaImageLoaderFactory
import com.veilreader.app.manga.reader.image.MangaLocalPageVerifier
import com.veilreader.app.manga.reader.image.MangaLocalSubsamplingPage
import com.veilreader.app.manga.reader.image.MangaPageAssetResolver
import com.veilreader.app.manga.reader.image.MangaPageResolveResult
import com.veilreader.app.manga.reader.presentation.MangaChapterPresentationLoader
import com.veilreader.app.manga.reader.presentation.MangaPageAsset
import com.veilreader.app.manga.reader.presentation.MangaPresentationError
import com.veilreader.app.manga.reader.presentation.MangaPresentationErrorKind
import com.veilreader.app.manga.reader.presentation.MangaReaderPresentationState
import com.veilreader.app.manga.reader.presentation.MangaReaderPresentationSurface
import com.veilreader.app.manga.reader.ui.MangaReaderGestureOwner
import com.veilreader.app.manga.reader.ui.MangaReaderUiIntent
import com.veilreader.app.ui.screens.rememberVeilIntegerFormatter
import com.veilreader.app.ui.theme.LocalVeilHighContrast
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun MangaReaderIntegratedScreen(
    session: MangaReaderSession,
    loader: MangaChapterPresentationLoader,
    progressStore: MangaProgressStore,
    cacheRoot: File,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val factory = remember(session, loader, progressStore) {
        MangaReaderScreenViewModel.factory(
            session = session,
            loader = loader,
            progressStore = progressStore
        )
    }
    val readerViewModel: MangaReaderScreenViewModel = viewModel(
        key = session.sessionKey,
        factory = factory
    )
    val state by readerViewModel.state.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val imageLoader = remember(context) { MangaImageLoaderFactory.create(context) }
    val resolver = remember(cacheRoot) {
        MangaPageAssetResolver(MangaLocalPageVerifier(cacheRoot))
    }
    val planner = remember {
        MangaImageDeliveryPlanner(AndroidMangaImageDimensionProbe())
    }

    var gestureOwner by remember(
        state.entry.route.readerChapter,
        state.readerUi.reader.position.itemIndex
    ) {
        mutableStateOf(MangaReaderGestureOwner.VEIL_READER)
    }

    MangaReaderOrientationEffect(state.readerUi.reader.orientationPolicy)
    MangaReaderLifecyclePersistence(readerViewModel)

    val closeScope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    fun requestDurableClose() {
        if (closing) return
        closing = true
        closeScope.launch {
            if (readerViewModel.persistForClose()) {
                onClose()
            } else {
                closing = false
            }
        }
    }

    BackHandler(enabled = !closing) {
        requestDurableClose()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = state.message?.toUiText()
    LaunchedEffect(state.message, snackbarMessage) {
        if (state.message == null || snackbarMessage == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(snackbarMessage)
        readerViewModel.dismissMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MangaReaderPresentationSurface(
                presentation = state.presentation,
                uiState = state.readerUi,
                onIntent = readerViewModel::onIntent,
                onRetry = readerViewModel::retry,
                gestureOwner = gestureOwner,
                modifier = Modifier.fillMaxSize(),
                loadingContent = {
                    MangaReaderLoading()
                },
                errorContent = { error, retry ->
                    MangaChapterError(error, retry)
                },
                partialOfflineContent = {
                    MangaPartialOfflineNotice()
                },
                pageContent = { asset, pageModifier ->
                    val isCurrent =
                        asset.index == state.readerUi.reader.position.itemIndex
                    MangaAdaptivePage(
                        asset = asset,
                        mode = state.readerUi.reader.mode,
                        resolver = resolver,
                        planner = planner,
                        imageLoader = imageLoader,
                        isCurrent = isCurrent,
                        onCurrentGestureOwner = { owner ->
                            if (isCurrent && gestureOwner != owner) {
                                gestureOwner = owner
                            }
                        },
                        onRendererTap = {
                            readerViewModel.onIntent(MangaReaderUiIntent.ToggleControls)
                        },
                        modifier = pageModifier
                    )
                }
            )

            if (state.readerUi.controlsVisible) {
                MangaReaderChrome(
                    mode = state.readerUi.reader.mode,
                    direction = state.readerUi.reader.direction,
                    itemIndex = state.readerUi.reader.position.itemIndex,
                    pageCount = state.readerUi.reader.pageCount,
                    closing = closing,
                    onClose = ::requestDurableClose,
                    onIntent = readerViewModel::onIntent,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
private fun MangaAdaptivePage(
    asset: MangaPageAsset,
    mode: MangaReaderMode,
    resolver: MangaPageAssetResolver,
    planner: MangaImageDeliveryPlanner,
    imageLoader: ImageLoader,
    isCurrent: Boolean,
    onCurrentGestureOwner: (MangaReaderGestureOwner) -> Unit,
    onRendererTap: () -> Unit,
    modifier: Modifier
) {
    var retryKey by remember(asset) { mutableIntStateOf(0) }

    val planState by produceState<AdaptivePageState>(
        initialValue = AdaptivePageState.Loading,
        asset,
        retryKey,
        resolver,
        planner
    ) {
        value = when (val resolved = resolver.resolve(asset)) {
            is MangaPageResolveResult.Error ->
                AdaptivePageState.Error

            is MangaPageResolveResult.Ready -> {
                val plan = planner.plan(resolved.page)
                AdaptivePageState.Ready(plan)
            }
        }
    }

    val desiredOwner = when (val current = planState) {
        is AdaptivePageState.Ready ->
            MangaReaderGestureOwnershipPolicy.ownerFor(current.plan.decision.strategy)
        AdaptivePageState.Loading,
        is AdaptivePageState.Error -> MangaReaderGestureOwner.VEIL_READER
    }

    LaunchedEffect(isCurrent, desiredOwner) {
        if (isCurrent) onCurrentGestureOwner(desiredOwner)
    }

    when (val current = planState) {
        AdaptivePageState.Loading -> {
            Box(modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        AdaptivePageState.Error -> {
            MangaPageError(
                onRetry = { retryKey += 1 },
                modifier = modifier
            )
        }

        is AdaptivePageState.Ready -> {
            val plan = current.plan
            if (
                plan.decision.strategy == MangaImageDeliveryStrategy.LOCAL_SUBSAMPLING &&
                plan.page is com.veilreader.app.manga.reader.image.MangaResolvedPage.Local &&
                plan.dimensions != null
            ) {
                MangaLocalSubsamplingPage(
                    page = plan.page,
                    dimensions = plan.dimensions,
                    mode = mode,
                    imageLoader = imageLoader,
                    modifier = modifier,
                    onTap = onRendererTap
                )
            } else {
                MangaCoilPage(
                    asset = asset,
                    mode = mode,
                    resolver = resolver,
                    imageLoader = imageLoader,
                    modifier = modifier
                )
            }
        }
    }
}

private sealed interface AdaptivePageState {
    data object Loading : AdaptivePageState
    data class Ready(val plan: MangaImageDeliveryPlan) : AdaptivePageState
    data object Error : AdaptivePageState
}

@Composable
private fun MangaReaderChrome(
    mode: MangaReaderMode,
    direction: MangaPageDirection,
    itemIndex: Int,
    pageCount: Int?,
    closing: Boolean,
    onClose: () -> Unit,
    onIntent: (MangaReaderUiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val highContrast = LocalVeilHighContrast.current
    val integer = rememberVeilIntegerFormatter()
    val progress = pageCount
        ?.takeIf { it > 0 }
        ?.let { ((itemIndex + 1).toFloat() / it.toFloat()).coerceIn(0f, 1f) }
        ?: 0f
    val positionLabel = if (closing) {
        stringResource(R.string.manga_reader_saving_position)
    } else pageCount
        ?.takeIf { it > 0 }
        ?.let {
            stringResource(
                R.string.manga_reader_page_of,
                integer((itemIndex + 1).coerceAtMost(it)),
                integer(it)
            )
        }
        ?: stringResource(
            R.string.manga_reader_page_unknown,
            integer(itemIndex + 1)
        )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .widthIn(max = 760.dp),
        shape = MaterialTheme.shapes.small,
        color = VeilPalette.Archive.copy(alpha = if (highContrast) 0.98f else 0.94f),
        border = BorderStroke(
            1.dp,
            if (highContrast) {
                MaterialTheme.colorScheme.outline
            } else {
                VeilPalette.Brass.copy(alpha = 0.54f)
            }
        ),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = VeilSpacing.md,
                vertical = VeilSpacing.sm
            ),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        stringResource(R.string.manga_reader_eyebrow),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (highContrast) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            VeilPalette.Brass
                        }
                    )
                    Text(
                        positionLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                OutlinedButton(
                    onClick = onClose,
                    enabled = !closing,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(stringResource(R.string.manga_reader_back))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.56f))
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(2.dp)
                            .background(
                                if (highContrast) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    VeilPalette.Brass
                                }
                            )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
            ) {
                OutlinedButton(
                    onClick = {
                        onIntent(
                            MangaReaderUiIntent.SetMode(
                                if (mode == MangaReaderMode.PAGED) {
                                    MangaReaderMode.WEBTOON
                                } else {
                                    MangaReaderMode.PAGED
                                }
                            )
                        )
                    },
                    enabled = !closing,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        stringResource(
                            if (mode == MangaReaderMode.PAGED) {
                                R.string.manga_reader_mode_webtoon
                            } else {
                                R.string.manga_reader_mode_paged
                            }
                        )
                    )
                }
                OutlinedButton(
                    onClick = {
                        onIntent(
                            MangaReaderUiIntent.SetDirection(
                                if (direction == MangaPageDirection.RIGHT_TO_LEFT) {
                                    MangaPageDirection.LEFT_TO_RIGHT
                                } else {
                                    MangaPageDirection.RIGHT_TO_LEFT
                                }
                            )
                        )
                    },
                    enabled = !closing,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        stringResource(
                            if (direction == MangaPageDirection.RIGHT_TO_LEFT) {
                                R.string.manga_reader_direction_ltr
                            } else {
                                R.string.manga_reader_direction_rtl
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MangaReaderLoading() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            CircularProgressIndicator()
            Text(
                stringResource(R.string.manga_reader_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MangaPartialOfflineNotice() {
    Surface(
        modifier = Modifier.padding(12.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Archive.copy(alpha = 0.94f),
        border = BorderStroke(
            1.dp,
            VeilPalette.Brass.copy(alpha = 0.46f)
        )
    ) {
        Text(
            stringResource(R.string.manga_reader_partial_offline_notice),
            modifier = Modifier.padding(
                horizontal = VeilSpacing.md,
                vertical = VeilSpacing.sm
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MangaPageError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        MangaReaderErrorPanel(
            message = stringResource(R.string.manga_reader_page_failed),
            retry = onRetry
        )
    }
}

@Composable
private fun MangaChapterError(
    error: MangaPresentationError,
    retry: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        MangaReaderErrorPanel(
            message = stringResource(error.kind.toUiMessageRes()),
            retry = retry.takeIf { error.retryable }
        )
    }
}

@Composable
private fun MangaReaderErrorPanel(
    message: String,
    retry: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .padding(VeilSpacing.lg)
            .widthIn(max = 520.dp),
        shape = MaterialTheme.shapes.small,
        color = VeilPalette.Archive,
        border = BorderStroke(
            1.dp,
            VeilPalette.Brass.copy(alpha = 0.46f)
        )
    ) {
        Column(
            modifier = Modifier.padding(VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
        ) {
            Text(
                stringResource(R.string.manga_reader_error_eyebrow),
                style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Brass
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            retry?.let {
                Button(
                    onClick = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(stringResource(R.string.manga_reader_retry))
                }
            }
        }
    }
}

@Composable
private fun MangaReaderOrientationEffect(policy: MangaOrientationPolicy) {
    val activity = LocalActivity.current
    val previousOrientation = remember(activity) {
        activity?.requestedOrientation
    }

    DisposableEffect(activity) {
        onDispose {
            if (activity != null && previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    LaunchedEffect(activity, policy) {
        activity ?: return@LaunchedEffect
        activity.requestedOrientation = when (policy) {
            MangaOrientationPolicy.FOLLOW_SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            MangaOrientationPolicy.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            MangaOrientationPolicy.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }
}

@Composable
private fun MangaReaderLifecyclePersistence(
    viewModel: MangaReaderScreenViewModel
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> viewModel.onBackgrounded()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            viewModel.onBackgrounded()
            lifecycle.removeObserver(observer)
        }
    }
}

@Composable
private fun MangaReaderScreenMessage.toUiText(): String = when (this) {
    MangaReaderScreenMessage.PartialOfflineBoundary ->
        stringResource(R.string.manga_reader_message_partial_offline_boundary)
    MangaReaderScreenMessage.SeriesBoundary ->
        stringResource(R.string.manga_reader_message_series_boundary)
    MangaReaderScreenMessage.ChapterRouteUnavailable ->
        stringResource(R.string.manga_reader_message_route_unavailable)
    MangaReaderScreenMessage.ProgressSaveFailed ->
        stringResource(R.string.manga_reader_message_progress_save_failed)
}

private fun MangaPresentationErrorKind.toUiMessageRes(): Int = when (this) {
    MangaPresentationErrorKind.OFFLINE_UNAVAILABLE ->
        R.string.manga_reader_error_offline_unavailable
    MangaPresentationErrorKind.SOURCE_FAILURE ->
        R.string.manga_reader_error_source_failure
    MangaPresentationErrorKind.EMPTY_CHAPTER ->
        R.string.manga_reader_error_empty_chapter
    MangaPresentationErrorKind.INVALID_PAGE_SET ->
        R.string.manga_reader_error_invalid_page_set
    MangaPresentationErrorKind.REQUEST_MISMATCH ->
        R.string.manga_reader_error_request_mismatch
}
