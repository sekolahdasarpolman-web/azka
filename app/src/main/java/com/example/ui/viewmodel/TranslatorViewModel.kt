package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SavedTranslationEntity
import com.example.data.local.TranslationRepository
import com.example.data.model.AccentLocale
import com.example.data.model.CuratedSamples
import com.example.data.model.EnglishStyle
import com.example.data.model.ParagraphTranslationResult
import com.example.data.model.SampleParagraphPreset
import com.example.data.remote.GeminiTranslationService
import com.example.tts.EnglishSpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab {
    TRANSLATOR,
    PRACTICE,
    SAVED
}

data class TranslatorUiState(
    val currentTab: AppTab = AppTab.TRANSLATOR,
    val inputParagraph: String = CuratedSamples.presets.first().sourceParagraph,
    val selectedStyle: EnglishStyle = EnglishStyle.BUSINESS,
    val isLoading: Boolean = false,
    val currentResult: ParagraphTranslationResult = CuratedSamples.presets.first().prebuiltResult,
    val showIpaAlongsideIndonesianPhonetic: Boolean = true,
    val showGrammarComparison: Boolean = true,
    val isCurrentSaved: Boolean = false,
    val snackbarMessage: String? = null,
    val isApiKeyConfigured: Boolean = false
)

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TranslationRepository
    private val geminiService = GeminiTranslationService()
    val speechManager = EnglishSpeechManager(application)

    private val _uiState = MutableStateFlow(
        TranslatorUiState(
            isApiKeyConfigured = geminiService.isApiKeyConfigured()
        )
    )
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    val savedTranslations: StateFlow<List<SavedTranslationEntity>>

    init {
        val db = AppDatabase.getInstance(application)
        repository = TranslationRepository(db.savedTranslationDao())
        savedTranslations = repository.allSavedTranslations.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun updateInputParagraph(text: String) {
        _uiState.update {
            it.copy(
                inputParagraph = text,
                isCurrentSaved = false
            )
        }
    }

    fun clearInput() {
        speechManager.stop()
        _uiState.update {
            it.copy(
                inputParagraph = "",
                isCurrentSaved = false
            )
        }
    }

    fun selectStyle(style: EnglishStyle) {
        _uiState.update { it.copy(selectedStyle = style) }
        if (_uiState.value.inputParagraph.isNotBlank()) {
            translateAndCorrect()
        }
    }

    fun applySamplePreset(preset: SampleParagraphPreset) {
        speechManager.stop()
        _uiState.update {
            it.copy(
                inputParagraph = preset.sourceParagraph,
                selectedStyle = preset.preferredStyle,
                currentResult = preset.prebuiltResult,
                isCurrentSaved = false,
                snackbarMessage = "Contoh \"${preset.title}\" dimuat"
            )
        }
    }

    fun translateAndCorrect() {
        val paragraph = _uiState.value.inputParagraph.trim()
        if (paragraph.isEmpty()) {
            _uiState.update {
                it.copy(snackbarMessage = "Silakan masukkan paragraf terlebih dahulu.")
            }
            return
        }

        speechManager.stop()
        val style = _uiState.value.selectedStyle

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = geminiService.translateAndCorrectParagraph(paragraph, style)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentResult = result,
                    isCurrentSaved = false
                )
            }
        }
    }

    fun toggleIpaVisibility() {
        _uiState.update {
            it.copy(showIpaAlongsideIndonesianPhonetic = !it.showIpaAlongsideIndonesianPhonetic)
        }
    }

    fun setAccent(accent: AccentLocale) {
        speechManager.setAccent(accent)
    }

    fun speakFullParagraph(slowMode: Boolean = false) {
        val text = _uiState.value.currentResult.standardEnglishText
        val rate = if (slowMode) 0.70f else 1.0f
        val tag = if (slowMode) "full_slow" else "full_normal"
        speechManager.speakEnglish(text, rate, tag)
    }

    fun speakSentence(sentence: String, index: Int, slowMode: Boolean = false) {
        val rate = if (slowMode) 0.70f else 0.95f
        val tag = if (slowMode) "sentence_${index}_slow" else "sentence_${index}_normal"
        speechManager.speakEnglish(sentence, rate, tag)
    }

    fun speakWord(word: String) {
        speechManager.speakEnglish(word, 0.80f, "word_${word.lowercase()}")
    }

    fun stopSpeaking() {
        speechManager.stop()
    }

    fun saveCurrentTranslation() {
        val current = _uiState.value.currentResult
        if (current.standardEnglishText.isBlank()) return
        viewModelScope.launch {
            repository.saveResult(current)
            _uiState.update {
                it.copy(
                    isCurrentSaved = true,
                    snackbarMessage = "Tersimpan ke Riwayat Terjemahan"
                )
            }
        }
    }

    fun loadSavedEntity(entity: SavedTranslationEntity) {
        speechManager.stop()
        val decoded = repository.decodeFullResult(entity)
        if (decoded != null) {
            _uiState.update {
                it.copy(
                    currentTab = AppTab.TRANSLATOR,
                    inputParagraph = decoded.sourceText,
                    currentResult = decoded,
                    isCurrentSaved = true,
                    snackbarMessage = "Memuat paragraf dari Riwayat"
                )
            }
        }
    }

    fun toggleFavoriteSaved(entity: SavedTranslationEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(entity.id, entity.isFavorite)
        }
    }

    fun deleteSaved(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
            _uiState.update {
                it.copy(snackbarMessage = "Item riwayat dihapus")
            }
        }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.shutdown()
    }
}
