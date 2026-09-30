package com.veilreader.app.manga.reader.screen

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import java.io.File

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

    BackHandler {
        readerViewModel.onBackgrounded()
        onClose()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message.toUiText())
        readerViewModel.dismissMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                },
                errorContent = { error, retry ->
                    MangaChapterError(error, retry)
                },
                partialOfflineContent = {
                    Text(
                        "Offline preview · reconnect to load the rest",
                        modifier = Modifier.padding(12.dp)
                    )
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
                    onClose = {
                        readerViewModel.onBackgrounded()
                        onClose()
                    },
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
                AdaptivePageState.Error(resolved.message)

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

        is AdaptivePageState.Error -> {
            Box(modifier, contentAlignment = Alignment.Center) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(current.message)
                    Button(onClick = { retryKey += 1 }) {
                        Text(stringResource(R.string.manga_reader_retry))
                    }
                }
            }
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
    data class Error(val message: String) : AdaptivePageState
}

@Composable
private fun MangaReaderChrome(
    mode: MangaReaderMode,
    direction: MangaPageDirection,
    onClose: () -> Unit,
    onIntent: (MangaReaderUiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onClose) {
            Text(stringResource(R.string.manga_reader_back))
        }
        Button(
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
            }
        ) {
            Text(\n                stringResource(\n                    if (mode == MangaReaderMode.PAGED) {\n                        R.string.manga_reader_mode_webtoon\n                    } else {\n                        R.string.manga_reader_mode_paged\n                    }\n                )\n            )
        }
        Button(
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
            }
        ) {
            Text(\n                stringResource(\n                    if (direction == MangaPageDirection.RIGHT_TO_LEFT) {\n                        R.string.manga_reader_direction_ltr\n                    } else {\n                        R.string.manga_reader_direction_rtl\n                    }\n                )\n            )
        }
    }
}

@Composable
private fun MangaChapterError(
    error: MangaPresentationError,
    retry: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(error.kind.toUiMessageRes()))
            if (error.retryable) {
                Button(onClick = retry) {
                    Text("Retry")
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
