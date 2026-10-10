package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AccentLocale
import com.example.data.model.CuratedSamples
import com.example.data.model.EnglishStyle
import com.example.data.model.GrammarNote
import com.example.data.model.ParagraphTranslationResult
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LoraFontFamily
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.TranslatorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuturInggrisApp(
    viewModel: TranslatorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedList by viewModel.savedTranslations.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.speechManager.isSpeaking.collectAsStateWithLifecycle()
    val activeTag by viewModel.speechManager.activeUtteranceTag.collectAsStateWithLifecycle()
    val selectedAccent by viewModel.speechManager.selectedAccent.collectAsStateWithLifecycle()
    val speechBanner by viewModel.speechManager.statusBanner.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (uiState.currentTab != AppTab.TRANSLATOR) {
        BackHandler {
            viewModel.selectTab(AppTab.TRANSLATOR)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSnackbar()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = LoraFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Terjemahan Baku • Cara Baca • Suara",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // Quick Accent Toggle in TopAppBar
                        FilledTonalButton(
                            onClick = {
                                val next = if (selectedAccent == AccentLocale.US) {
                                    AccentLocale.UK
                                } else {
                                    AccentLocale.US
                                }
                                viewModel.setAccent(next)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .testTag("top_accent_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Ganti Aksen Suara",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedAccent.shortCode,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        NavigationBarItem(
                            selected = uiState.currentTab == AppTab.TRANSLATOR,
                            onClick = { viewModel.selectTab(AppTab.TRANSLATOR) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.currentTab == AppTab.TRANSLATOR) {
                                        Icons.Filled.Translate
                                    } else {
                                        Icons.Outlined.Translate
                                    },
                                    contentDescription = stringResource(R.string.nav_translator)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_translator)) },
                            modifier = Modifier.testTag("nav_tab_translator")
                        )
                        NavigationBarItem(
                            selected = uiState.currentTab == AppTab.PRACTICE,
                            onClick = { viewModel.selectTab(AppTab.PRACTICE) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (uiState.currentResult.sentences.isNotEmpty()) {
                                            Badge {
                                                Text(uiState.currentResult.sentences.size.toString())
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (uiState.currentTab == AppTab.PRACTICE) {
                                            Icons.Filled.RecordVoiceOver
                                        } else {
                                            Icons.Outlined.RecordVoiceOver
                                        },
                                        contentDescription = stringResource(R.string.nav_practice)
                                    )
                                }
                            },
                            label = { Text(stringResource(R.string.nav_practice)) },
                            modifier = Modifier.testTag("nav_tab_practice")
                        )
                        NavigationBarItem(
                            selected = uiState.currentTab == AppTab.SAVED,
                            onClick = { viewModel.selectTab(AppTab.SAVED) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (savedList.isNotEmpty()) {
                                            Badge { Text(savedList.size.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (uiState.currentTab == AppTab.SAVED) {
                                            Icons.Filled.History
                                        } else {
                                            Icons.Outlined.History
                                        },
                                        contentDescription = stringResource(R.string.nav_saved)
                                    )
                                }
                            },
                            label = { Text(stringResource(R.string.nav_saved)) },
                            modifier = Modifier.testTag("nav_tab_saved")
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("side_navigation_rail")
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        NavigationRailItem(
                            selected = uiState.currentTab == AppTab.TRANSLATOR,
                            onClick = { viewModel.selectTab(AppTab.TRANSLATOR) },
                            icon = {
                                Icon(Icons.Default.Translate, contentDescription = null)
                            },
                            label = { Text(stringResource(R.string.nav_translator)) },
                            modifier = Modifier.testTag("rail_tab_translator")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NavigationRailItem(
                            selected = uiState.currentTab == AppTab.PRACTICE,
                            onClick = { viewModel.selectTab(AppTab.PRACTICE) },
                            icon = {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                            },
                            label = { Text(stringResource(R.string.nav_practice)) },
                            modifier = Modifier.testTag("rail_tab_practice")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NavigationRailItem(
                            selected = uiState.currentTab == AppTab.SAVED,
                            onClick = { viewModel.selectTab(AppTab.SAVED) },
                            icon = {
                                Icon(Icons.Default.History, contentDescription = null)
                            },
                            label = { Text(stringResource(R.string.nav_saved)) },
                            modifier = Modifier.testTag("rail_tab_saved")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                    ) {
                        // Active Voice Status Bar when speaking
                        AnimatedVisibility(
                            visible = isSpeaking && speechBanner != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("active_speech_banner"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = speechBanner ?: "Memutar suara Bahasa Inggris...",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    TextButton(
                                        onClick = { viewModel.stopSpeaking() },
                                        modifier = Modifier.testTag("banner_stop_speech_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.StopCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Stop")
                                    }
                                }
                            }
                        }

                        when (uiState.currentTab) {
                            AppTab.TRANSLATOR -> {
                                TranslatorWorkspaceContent(
                                    inputParagraph = uiState.inputParagraph,
                                    selectedStyle = uiState.selectedStyle,
                                    isLoading = uiState.isLoading,
                                    result = uiState.currentResult,
                                    showIpa = uiState.showIpaAlongsideIndonesianPhonetic,
                                    isCurrentSaved = uiState.isCurrentSaved,
                                    isSpeaking = isSpeaking,
                                    activeUtteranceTag = activeTag,
                                    selectedAccent = selectedAccent,
                                    onInputChange = viewModel::updateInputParagraph,
                                    onClearInput = viewModel::clearInput,
                                    onPasteInput = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val pasted = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                                        if (!pasted.isNullOrBlank()) {
                                            viewModel.updateInputParagraph(pasted)
                                        }
                                    },
                                    onSelectStyle = viewModel::selectStyle,
                                    onSelectPreset = viewModel::applySamplePreset,
                                    onTranslateClick = viewModel::translateAndCorrect,
                                    onSpeakNormal = { viewModel.speakFullParagraph(slowMode = false) },
                                    onSpeakSlow = { viewModel.speakFullParagraph(slowMode = true) },
                                    onStopSpeaking = viewModel::stopSpeaking,
                                    onSelectAccent = viewModel::setAccent,
                                    onCopyText = { label, textToCopy ->
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText(label, textToCopy))
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("$label berhasil disalin")
                                        }
                                    },
                                    onSaveResult = viewModel::saveCurrentTranslation,
                                    onToggleIpa = viewModel::toggleIpaVisibility,
                                    onOpenSentencePractice = { viewModel.selectTab(AppTab.PRACTICE) }
                                )
                            }

                            AppTab.PRACTICE -> {
                                PracticeBreakdownScreen(
                                    result = uiState.currentResult,
                                    isSpeaking = isSpeaking,
                                    activeUtteranceTag = activeTag,
                                    selectedAccent = selectedAccent,
                                    onSelectAccent = viewModel::setAccent,
                                    onSpeakFullNormal = { viewModel.speakFullParagraph(false) },
                                    onSpeakFullSlow = { viewModel.speakFullParagraph(true) },
                                    onSpeakSentence = viewModel::speakSentence,
                                    onSpeakWord = viewModel::speakWord,
                                    onStopSpeaking = viewModel::stopSpeaking
                                )
                            }

                            AppTab.SAVED -> {
                                SavedHistoryScreen(
                                    savedItems = savedList,
                                    onLoadItem = viewModel::loadSavedEntity,
                                    onSpeakItem = { text ->
                                        viewModel.speechManager.speakEnglish(text, 1.0f, "saved_item")
                                    },
                                    onToggleFavorite = viewModel::toggleFavoriteSaved,
                                    onDeleteItem = viewModel::deleteSaved
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranslatorWorkspaceContent(
    inputParagraph: String,
    selectedStyle: EnglishStyle,
    isLoading: Boolean,
    result: ParagraphTranslationResult,
    showIpa: Boolean,
    isCurrentSaved: Boolean,
    isSpeaking: Boolean,
    activeUtteranceTag: String?,
    selectedAccent: AccentLocale,
    onInputChange: (String) -> Unit,
    onClearInput: () -> Unit,
    onPasteInput: () -> Unit,
    onSelectStyle: (EnglishStyle) -> Unit,
    onSelectPreset: (com.example.data.model.SampleParagraphPreset) -> Unit,
    onTranslateClick: () -> Unit,
    onSpeakNormal: () -> Unit,
    onSpeakSlow: () -> Unit,
    onStopSpeaking: () -> Unit,
    onSelectAccent: (AccentLocale) -> Unit,
    onCopyText: (String, String) -> Unit,
    onSaveResult: () -> Unit,
    onToggleIpa: () -> Unit,
    onOpenSentencePractice: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("translator_main_scroll"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Editorial Hero Banner
        item {
            HeroBannerCard()
        }

        // 2. Sample Presets for Instant Exploration
        item {
            Column {
                Text(
                    text = stringResource(R.string.sample_paragraphs_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CuratedSamples.presets.forEach { preset ->
                        AssistChip(
                            onClick = { onSelectPreset(preset) },
                            label = { Text(preset.title) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.testTag("preset_chip_${preset.id}")
                        )
                    }
                }
            }
        }

        // 3. Source Paragraph Input & Style Selection Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.input_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row {
                            TextButton(
                                onClick = onPasteInput,
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier.testTag("paste_paragraph_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = stringResource(R.string.btn_paste),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.btn_paste))
                            }
                            if (inputParagraph.isNotEmpty()) {
                                TextButton(
                                    onClick = onClearInput,
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = Modifier.testTag("clear_paragraph_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.btn_clear),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_clear))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = inputParagraph,
                        onValueChange = onInputChange,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.input_placeholder),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        minLines = 4,
                        maxLines = 8,
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("paragraph_input_field")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.style_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EnglishStyle.entries.forEach { styleOption ->
                            val isSelected = styleOption == selectedStyle
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectStyle(styleOption) },
                                label = { Text(styleOption.labelId) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("style_chip_${styleOption.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onTranslateClick,
                        enabled = !isLoading && inputParagraph.isNotBlank(),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("translate_and_correct_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.btn_translating),
                                style = MaterialTheme.typography.titleMedium
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Spellcheck,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.btn_translate_correct),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }

        // 4. Primary Output Card: Standard English Text + Prominent Voice Pronunciation Controls
        item {
            StandardEnglishOutputCard(
                result = result,
                isSpeaking = isSpeaking,
                activeUtteranceTag = activeUtteranceTag,
                selectedAccent = selectedAccent,
                isCurrentSaved = isCurrentSaved,
                onSpeakNormal = onSpeakNormal,
                onSpeakSlow = onSpeakSlow,
                onStopSpeaking = onStopSpeaking,
                onSelectAccent = onSelectAccent,
                onCopyEnglish = {
                    onCopyText("Teks Bahasa Inggris Baku", result.standardEnglishText)
                },
                onSave = onSaveResult
            )
        }

        // 5. Cara Membaca (Phonetic Indonesian Respelling + IPA Transcription) Card
        item {
            CaraMembacaCard(
                result = result,
                showIpa = showIpa,
                isSpeaking = isSpeaking,
                onToggleIpa = onToggleIpa,
                onSpeakWhileReading = onSpeakSlow,
                onStopSpeaking = onStopSpeaking,
                onCopyReadingGuide = {
                    onCopyText("Panduan Cara Membaca", result.caraMembacaFull)
                },
                onOpenSentencePractice = onOpenSentencePractice
            )
        }

        // 6. Standard English Grammar Corrections & Explanation Card
        item {
            GrammarCorrectionsCard(result = result)
        }

        // 7. Prototype Security Notice
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.security_prototype_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeroBannerCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_banner_card"),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner_1791636576732),
                contentDescription = stringResource(R.string.hero_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F172A).copy(alpha = 0.45f),
                                Color(0xFF0F172A).copy(alpha = 0.88f)
                            )
                        )
                    )
            )
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Surface(
                    color = Color(0xFFF59E0B),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "STANDARD ENGLISH & PHONETICS",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.hero_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = LoraFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.hero_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 2
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StandardEnglishOutputCard(
    result: ParagraphTranslationResult,
    isSpeaking: Boolean,
    activeUtteranceTag: String?,
    selectedAccent: AccentLocale,
    isCurrentSaved: Boolean,
    onSpeakNormal: () -> Unit,
    onSpeakSlow: () -> Unit,
    onStopSpeaking: () -> Unit,
    onSelectAccent: (AccentLocale) -> Unit,
    onCopyEnglish: () -> Unit,
    onSave: () -> Unit
) {
    val isSpeakingNormal = isSpeaking && activeUtteranceTag == "full_normal"
    val isSpeakingSlow = isSpeaking && activeUtteranceTag == "full_slow"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("standard_english_result_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSpeakingNormal || isSpeakingSlow) 2.dp else 1.5.dp,
            color = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.section_english_result),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = result.styleBadge,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Full Standard English Paragraph
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = result.standardEnglishText,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = LoraFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag("english_result_text")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Prominent Audio Pronunciation Buttons ("Dengarkan Suara Pengucapan")
            Text(
                text = "Dengarkan Suara Pengucapan Bahasa Inggris:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = if (isSpeakingNormal) onStopSpeaking else onSpeakNormal,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSpeakingNormal) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("speak_normal_button")
                ) {
                    Icon(
                        imageVector = if (isSpeakingNormal) {
                            Icons.Default.StopCircle
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = stringResource(R.string.btn_speak_normal),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSpeakingNormal) {
                            stringResource(R.string.btn_stop_speaking)
                        } else {
                            stringResource(R.string.btn_speak_normal)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                FilledTonalButton(
                    onClick = if (isSpeakingSlow) onStopSpeaking else onSpeakSlow,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("speak_slow_button")
                ) {
                    Icon(
                        imageVector = if (isSpeakingSlow) {
                            Icons.Default.StopCircle
                        } else {
                            Icons.Default.SlowMotionVideo
                        },
                        contentDescription = stringResource(R.string.btn_speak_slow),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSpeakingSlow) "Stop" else stringResource(R.string.btn_speak_slow),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Row: Accent Selector + Copy + Save
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AccentLocale.entries.forEach { accent ->
                    val isSelected = accent == selectedAccent
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectAccent(accent) },
                        label = { Text(accent.labelId) },
                        modifier = Modifier.testTag("accent_chip_${accent.name.lowercase()}")
                    )
                }

                OutlinedButton(
                    onClick = onCopyEnglish,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("copy_english_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.btn_copy),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_copy))
                }

                OutlinedButton(
                    onClick = onSave,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("save_translation_button")
                ) {
                    Icon(
                        imageVector = if (isCurrentSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = stringResource(R.string.btn_save),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isCurrentSaved) {
                            stringResource(R.string.btn_saved)
                        } else {
                            stringResource(R.string.btn_save)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CaraMembacaCard(
    result: ParagraphTranslationResult,
    showIpa: Boolean,
    isSpeaking: Boolean,
    onToggleIpa: () -> Unit,
    onSpeakWhileReading: () -> Unit,
    onStopSpeaking: () -> Unit,
    onCopyReadingGuide: () -> Unit,
    onOpenSentencePractice: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reading_guide_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.section_reading_guide),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "HURUF KAPITAL = Suku kata yang ditekan (word stress)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Indonesian Phonetic Reading Text Box
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = result.caraMembacaFull,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("cara_membaca_text")
                    )

                    AnimatedVisibility(visible = showIpa) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.section_ipa_guide),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.ipaFull,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = JetBrainsMonoFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("ipa_phonetic_text")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = showIpa,
                        onCheckedChange = { onToggleIpa() },
                        modifier = Modifier.testTag("toggle_ipa_switch")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tampilkan Simbol IPA",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onCopyReadingGuide,
                        modifier = Modifier.testTag("copy_reading_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salin Cara Baca")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    onClick = if (isSpeaking) onStopSpeaking else onSpeakWhileReading,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("listen_while_reading_button")
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.StopCircle else Icons.Default.SlowMotionVideo,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSpeaking) "Hentikan" else "Dengarkan Pelan Sambil Baca")
                }

                OutlinedButton(
                    onClick = onOpenSentencePractice,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_sentence_breakdown_button")
                ) {
                    Text("Latihan Per Kalimat (${result.sentences.size})")
                }
            }
        }
    }
}

@Composable
private fun GrammarCorrectionsCard(
    result: ParagraphTranslationResult
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("grammar_notes_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Spellcheck,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.section_grammar_improvements),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = result.indonesianSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            result.grammarNotes.forEachIndexed { idx, note ->
                GrammarNoteRow(note = note, index = idx)
                if (idx < result.grammarNotes.lastIndex) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun GrammarNoteRow(
    note: GrammarNote,
    index: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("grammar_note_item_$index")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = note.ruleCategory,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (note.originalOrLiteral.isNotBlank()) {
                Text(
                    text = "Harfiah / Belum Baku: \"${note.originalOrLiteral}\"",
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.LineThrough
                    ),
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = "Bentuk Baku: \"${note.correctedStandard}\"",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = note.explanationId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
