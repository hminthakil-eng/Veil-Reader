package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderColumnMode
import com.veilreader.app.domain.ReaderDarkImageTreatment
import com.veilreader.app.domain.ReaderFontFamily
import com.veilreader.app.domain.ReaderFixedLayoutSpread
import com.veilreader.app.domain.ReaderPreferenceToggle
import com.veilreader.app.domain.ReaderReadingMode
import com.veilreader.app.domain.ReaderTextAlignment
import com.veilreader.app.domain.ReaderNavigationMode
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.ui.reader.material.MaterialPageEngineRollout
import com.veilreader.app.ui.theme.VeilMaterials
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing
import org.readium.r2.navigator.preferences.ReadingProgression

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
