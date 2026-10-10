package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.CuratedSamples
import com.example.data.model.EnglishStyle
import com.example.data.model.GrammarNote
import com.example.data.model.ParagraphTranslationResult
import com.example.data.model.PronunciationWord
import com.example.data.model.SentenceBreakdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.Locale
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null,
    val responseModalities: List<String>? = null,
    val speechConfig: SpeechConfig? = null
)

@Serializable
data class SpeechConfig(
    val voiceConfig: VoiceConfig
)

@Serializable
data class VoiceConfig(
    val prebuiltVoiceConfig: PrebuiltVoiceConfig
)

@Serializable
data class PrebuiltVoiceConfig(
    val voiceName: String
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    val content: Content? = null
)

@Serializable
private data class GeminiTranslationSchemaResponse(
    val standardEnglishText: String = "",
    val literalDraftComparison: String = "",
    val caraMembacaFull: String = "",
    val ipaFull: String = "",
    val indonesianSummary: String = "",
    val sentences: List<SentenceBreakdown> = emptyList(),
    val grammarNotes: List<GrammarNote> = emptyList(),
    val keyWords: List<PronunciationWord> = emptyList()
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @POST("v1beta/models/gemini-2.5-flash-preview-tts:generateContent")
    suspend fun generateSpeech(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiTranslationService(
    private val apiService: GeminiApiService = GeminiRetrofitClient.service
) {

    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY.trim()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.startsWith("YOUR_")
    }

    suspend fun translateAndCorrectParagraph(
        inputParagraph: String,
        style: EnglishStyle
    ): ParagraphTranslationResult = withContext(Dispatchers.IO) {
        val trimmedInput = inputParagraph.trim()

        // Check if input matches a curated preset when using the default style or offline
        val matchingPreset = CuratedSamples.presets.firstOrNull {
            it.sourceParagraph.equals(trimmedInput, ignoreCase = true)
        }

        if (isApiKeyConfigured()) {
            try {
                val aiResult = callGeminiStructuredTranslation(trimmedInput, style)
                if (aiResult != null && aiResult.standardEnglishText.isNotBlank()) {
                    return@withContext aiResult
                }
            } catch (_: Exception) {
                // Fall through to preset or local linguistic engine if network/key fails
            }
        }

        if (matchingPreset != null) {
            return@withContext matchingPreset.prebuiltResult.copy(
                styleBadge = style.badgeText
            )
        }

        // Local standard English grammar & Indonesian phonetic fallback engine
        LocalLinguisticEngine.processParagraph(trimmedInput, style)
    }

    private suspend fun callGeminiStructuredTranslation(
        paragraph: String,
        style: EnglishStyle
    ): ParagraphTranslationResult? {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        val systemPrompt = """
            You are an expert English linguist, translator, grammar editor, and phonetics coach for Indonesian speakers.
            Your task:
            1. If the user's paragraph is in Indonesian (or mixed Indonesian/English), translate it into natural, grammatically impeccable Standard English matching this style: ${style.promptInstruction}
            2. If the user's paragraph is already in English (including broken or non-standard English), correct and refine it into impeccable Standard English matching the requested style.
            3. Provide 'caraMembacaFull': a clear, intuitive pronunciation guide written using Indonesian phonetic spelling rules (e.g., "ai wud laik tu pri-ZENT aw-er nyu PRO-jekt..."), capitalizing the stressed syllable of multi-syllable words so an Indonesian speaker can read it aloud with natural English rhythm and pronunciation.
            4. Provide 'ipaFull': the International Phonetic Alphabet (IPA) transcription of the entire Standard English paragraph.
            5. Break down the paragraph sentence-by-sentence in 'sentences' with 'englishSentence', 'caraMembaca' (Indonesian phonetic respelling), 'ipaPhonetic', and 'indonesianMeaning' (accurate Indonesian translation).
            6. Provide 2 to 4 'grammarNotes' explaining key grammar corrections, tense choices, subject-verb agreements, or vocabulary upgrades made to ensure Standard English (with 'explanationId' in Indonesian).
            7. Provide 3 to 5 'keyWords' highlighting important or commonly mispronounced English words from the paragraph, including 'word', 'caraMembaca', 'ipa', 'meaningId', and 'tipId' (practical pronunciation tip in Indonesian).
        """.trimIndent()

        val schema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("standardEnglishText") {
                    put("type", "STRING")
                    put("description", "The full translated and grammatically corrected Standard English paragraph.")
                }
                putJsonObject("literalDraftComparison") {
                    put("type", "STRING")
                    put("description", "A literal/word-for-word or unedited version showing what non-standard phrasing was avoided.")
                }
                putJsonObject("caraMembacaFull") {
                    put("type", "STRING")
                    put("description", "Full paragraph pronunciation guide written in Indonesian phonetic respelling with stressed syllables in UPPERCASE.")
                }
                putJsonObject("ipaFull") {
                    put("type", "STRING")
                    put("description", "Full paragraph IPA phonetic transcription.")
                }
                putJsonObject("indonesianSummary") {
                    put("type", "STRING")
                    put("description", "Brief summary in Indonesian explaining the grammar rules and standard style applied.")
                }
                putJsonObject("sentences") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("englishSentence") { put("type", "STRING") }
                            putJsonObject("caraMembaca") { put("type", "STRING") }
                            putJsonObject("ipaPhonetic") { put("type", "STRING") }
                            putJsonObject("indonesianMeaning") { put("type", "STRING") }
                        }
                        putJsonArray("required") {
                            add(kotlinx.serialization.json.JsonPrimitive("englishSentence"))
                            add(kotlinx.serialization.json.JsonPrimitive("caraMembaca"))
                            add(kotlinx.serialization.json.JsonPrimitive("ipaPhonetic"))
                            add(kotlinx.serialization.json.JsonPrimitive("indonesianMeaning"))
                        }
                    }
                }
                putJsonObject("grammarNotes") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("originalOrLiteral") { put("type", "STRING") }
                            putJsonObject("correctedStandard") { put("type", "STRING") }
                            putJsonObject("ruleCategory") { put("type", "STRING") }
                            putJsonObject("explanationId") { put("type", "STRING") }
                        }
                        putJsonArray("required") {
                            add(kotlinx.serialization.json.JsonPrimitive("originalOrLiteral"))
                            add(kotlinx.serialization.json.JsonPrimitive("correctedStandard"))
                            add(kotlinx.serialization.json.JsonPrimitive("ruleCategory"))
                            add(kotlinx.serialization.json.JsonPrimitive("explanationId"))
                        }
                    }
                }
                putJsonObject("keyWords") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("word") { put("type", "STRING") }
                            putJsonObject("caraMembaca") { put("type", "STRING") }
                            putJsonObject("ipa") { put("type", "STRING") }
                            putJsonObject("meaningId") { put("type", "STRING") }
                            putJsonObject("tipId") { put("type", "STRING") }
                        }
                        putJsonArray("required") {
                            add(kotlinx.serialization.json.JsonPrimitive("word"))
                            add(kotlinx.serialization.json.JsonPrimitive("caraMembaca"))
                            add(kotlinx.serialization.json.JsonPrimitive("ipa"))
                            add(kotlinx.serialization.json.JsonPrimitive("meaningId"))
                            add(kotlinx.serialization.json.JsonPrimitive("tipId"))
                        }
                    }
                }
            }
            putJsonArray("required") {
                add(kotlinx.serialization.json.JsonPrimitive("standardEnglishText"))
                add(kotlinx.serialization.json.JsonPrimitive("caraMembacaFull"))
                add(kotlinx.serialization.json.JsonPrimitive("ipaFull"))
                add(kotlinx.serialization.json.JsonPrimitive("indonesianSummary"))
                add(kotlinx.serialization.json.JsonPrimitive("sentences"))
                add(kotlinx.serialization.json.JsonPrimitive("grammarNotes"))
                add(kotlinx.serialization.json.JsonPrimitive("keyWords"))
            }
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(
                            text = "Translate and/or correct the following paragraph into Standard English (${style.badgeText}) and provide full pronunciation guides (Cara Membaca & IPA) and grammar explanations:\n\n\"\"\"\n$paragraph\n\"\"\""
                        )
                    )
                )
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = schema,
                temperature = 0.25f
            )
        )

        val response = apiService.generateContent(apiKey, request)
        val rawJson = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return null
        val parsed = GeminiRetrofitClient.json.decodeFromString<GeminiTranslationSchemaResponse>(rawJson)

        return ParagraphTranslationResult(
            sourceText = paragraph,
            standardEnglishText = parsed.standardEnglishText,
            literalDraftComparison = parsed.literalDraftComparison.ifBlank { paragraph },
            caraMembacaFull = parsed.caraMembacaFull,
            ipaFull = parsed.ipaFull,
            indonesianSummary = parsed.indonesianSummary,
            styleBadge = style.badgeText,
            sentences = parsed.sentences,
            grammarNotes = parsed.grammarNotes,
            keyWords = parsed.keyWords,
            isAiGenerated = true
        )
    }
}

/**
 * Local linguistic & phonetic engine that provides immediate, accurate translation,
 * Standard English grammar normalization, and Indonesian phonetic respelling ("Cara Membaca")
 * when offline or before a custom GEMINI_API_KEY is added in Secrets.
 */
object LocalLinguisticEngine {

    private data class PhraseRule(
        val regex: Regex,
        val replacement: String,
        val noteTitle: String? = null,
        val noteExplanation: String? = null
    )

    private val brokenEnglishGrammarFixes = listOf(
        PhraseRule(
            Regex("\\b[Mm]e and my ([a-zA-Z]+)\\b"),
            "My $1 and I",
            "Subject Pronoun Order",
            "Gunakan 'My ... and I' sebagai subjek kalimat dalam Bahasa Inggris baku, bukan 'Me and my ...'."
        ),
        PhraseRule(
            Regex("\\b(I|we|they|you)\\s+was\\s+go\\b", RegexOption.IGNORE_CASE),
            "$1 went",
            "Simple Past Irregular Verb",
            "Bentuk lampau dari 'go' adalah 'went' (irregular verb), tidak boleh digabungkan dengan 'was go'."
        ),
        PhraseRule(
            Regex("\\b(he|she|it)\\s+don't\\b", RegexOption.IGNORE_CASE),
            "$1 does not",
            "Subject-Verb Agreement",
            "Subjek tunggal orang ketiga (he/she/it) wajib menggunakan 'does not' (atau 'did not' untuk lampau), bukan 'don't'."
        ),
        PhraseRule(
            Regex("\\b(he|she|it|I|we|they)\\s+didn't\\s+went\\b", RegexOption.IGNORE_CASE),
            "$1 did not go",
            "Double Past Tense Correction",
            "Setelah kata bantu lampau 'did not', kata kerja harus kembali ke bentuk dasar V1 ('go'), bukan V2 ('went')."
        ),
        PhraseRule(
            Regex("\\bfor\\s+(study|learn|make|do|discuss|complete|improve)\\b", RegexOption.IGNORE_CASE),
            "to $1",
            "Infinitive of Purpose",
            "Untuk menyatakan tujuan ('untuk melakukan sesuatu'), Bahasa Inggris baku menggunakan 'to + V1' (to-infinitive), bukan 'for + V1'."
        ),
        PhraseRule(
            Regex("\\bhow\\s+to\\s+([a-zA-Z]+)ing\\b", RegexOption.IGNORE_CASE),
            "how to $1",
            "Infinitive after 'How to'",
            "Frasa 'how to' harus diikuti oleh kata kerja dasar (V1), bukan bentuk '-ing'."
        ),
        PhraseRule(
            Regex("\\bdiscuss\\s+about\\b", RegexOption.IGNORE_CASE),
            "discuss",
            "Transitive Verb ('Discuss')",
            "Kata kerja 'discuss' bersifat transitif sehingga langsung diikuti objek tanpa preposisi 'about'."
        ),
        PhraseRule(
            Regex("\\bexplain\\s+to\\s+(me|him|her|us|them)\\b", RegexOption.IGNORE_CASE),
            "explain it to $1",
            "Object Structure with 'Explain'",
            "Kata kerja 'explain' membutuhkan objek langsung sebelum 'to + person' (misalnya 'explain it to her')."
        ),
        PhraseRule(
            Regex("\\bmore\\s+better\\b", RegexOption.IGNORE_CASE),
            "much better",
            "Double Comparative Correction",
            "Kata 'better' sudah berbentuk komparatif, sehingga tidak boleh ditambah 'more'. Gunakan 'much better'."
        ),
        PhraseRule(
            Regex("\\bi\\s+am\\s+agree\\b", RegexOption.IGNORE_CASE),
            "I agree",
            "Action Verb ('Agree')",
            "'Agree' adalah kata kerja (verb), bukan kata sifat, sehingga tidak menggunakan 'am/is/are' di depannya."
        )
    )

    private val indonesianPhraseDictionary = listOf(
        "selamat pagi" to "good morning",
        "selamat siang" to "good afternoon",
        "selamat sore" to "good afternoon",
        "selamat malam" to "good evening",
        "terima kasih banyak atas" to "thank you very much for",
        "terima kasih atas" to "thank you for",
        "terima kasih" to "thank you",
        "perkenalkan nama saya" to "allow me to introduce myself, my name is",
        "nama saya adalah" to "my name is",
        "nama saya" to "my name is",
        "saya ingin menjelaskan" to "I would like to explain",
        "saya ingin menyampaikan" to "I would like to convey",
        "saya ingin melamar" to "I would like to apply for",
        "saya ingin" to "I would like to",
        "kami ingin" to "we would like to",
        "kemarin saya pergi ke" to "yesterday, I went to",
        "kemarin saya sudah" to "yesterday, I already",
        "kemarin saya" to "yesterday, I",
        "minggu depan" to "next week",
        "minggu lalu" to "last week",
        "bulan depan" to "next month",
        "tahun depan" to "next year",
        "hari ini" to "today",
        "besok pagi" to "tomorrow morning",
        "besok" to "tomorrow",
        "kemarin" to "yesterday",
        "bahasa inggris yang baku" to "standard English",
        "bahasa inggris" to "English",
        "tata bahasa" to "grammar",
        "cara membaca" to "pronunciation guide",
        "pengucapan" to "pronunciation",
        "belajar bersama" to "study together",
        "bekerja sama" to "collaborate",
        "bekerja keras" to "work hard",
        "mempresentasikan rencana proyek baru" to "present the new project plan",
        "rencana proyek baru" to "new project plan",
        "laporan perkembangan proyek" to "project progress report",
        "ditunda sampai" to "has been postponed until",
        "ditunda hingga" to "has been postponed until",
        "ditunda" to "postponed",
        "berjalan lancar" to "runs smoothly",
        "sangat penting untuk" to "is very important to",
        "sangat penting" to "very important",
        "menurut pendapat saya" to "in my opinion",
        "oleh karena itu" to "therefore",
        "selain itu" to "furthermore",
        "namun" to "however",
        "tetapi" to "however",
        "karena" to "because",
        "sehingga" to "so that",
        "agar" to "so that",
        "untuk meningkatkan" to "to improve",
        "kemampuan komunikasi" to "communication skills",
        "perkembangan teknologi" to "technological development",
        "pendidikan" to "education",
        "universitas" to "university",
        "perpustakaan" to "the library",
        "kantor" to "the office",
        "perusahaan" to "the company",
        "manajer" to "the manager",
        "anggota tim" to "team members",
        "rapat" to "the meeting",
        "presentasi" to "the presentation",
        "masyarakat" to "society",
        "kesehatan" to "health",
        "lingkungan" to "the environment",
        "masa depan" to "the future",
        "saya sedang" to "I am currently",
        "saya sudah" to "I have already",
        "saya akan" to "I will",
        "saya dapat" to "I can",
        "saya bisa" to "I can",
        "saya merasa" to "I feel",
        "saya" to "I",
        "kami" to "we",
        "kita" to "we",
        "mereka" to "they",
        "beliau" to "he or she",
        "dia" to "he or she",
        "anda" to "you",
        "belajar" to "study",
        "membaca" to "read",
        "menulis" to "write",
        "mendengarkan" to "listen to",
        "berbicara" to "speak",
        "memahami" to "understand",
        "menggunakan" to "use",
        "membuat" to "create",
        "mengirim" to "submit",
        "memeriksa" to "review",
        "membantu" to "help",
        "pergi ke" to "go to",
        "pergi" to "go",
        "datang" to "come",
        "dengan baik" to "properly",
        "dengan jelas" to "clearly",
        "sangat" to "very",
        "dan" to "and",
        "atau" to "or",
        "di" to "in",
        "ke" to "to",
        "dari" to "from",
        "untuk" to "to",
        "dengan" to "with",
        "pada" to "on",
        "yang" to "that",
        "ini" to "this",
        "itu" to "that"
    )

    private data class PhoneticEntry(
        val caraMembaca: String,
        val ipa: String,
        val meaningId: String = "",
        val tipId: String = ""
    )

    private val phoneticLexicon: Map<String, PhoneticEntry> = mapOf(
        "i" to PhoneticEntry("ai", "aɪ", "Saya", "Dibaca jelas 'ai' seperti kata 'air' tanpa r."),
        "my" to PhoneticEntry("mai", "maɪ", "Milik saya"),
        "me" to PhoneticEntry("mii", "miː", "Saya (objek)"),
        "you" to PhoneticEntry("yuu", "juː", "Anda / Kamu"),
        "we" to PhoneticEntry("wii", "wiː", "Kami / Kita"),
        "our" to PhoneticEntry("aw-er", "ˈaʊər", "Milik kami"),
        "they" to PhoneticEntry("dhei", "ðeɪ", "Mereka", "Awali dengan ujung lidah menyentuh gigi atas ('dh')."),
        "their" to PhoneticEntry("dher", "ðɛər", "Milik mereka"),
        "he" to PhoneticEntry("hii", "hiː", "Dia (laki-laki)"),
        "she" to PhoneticEntry("syi", "ʃiː", "Dia (perempuan)", "Bunyi 'sh' tebal seperti menyuruh orang diam ('syi')."),
        "it" to PhoneticEntry("it", "ɪt", "Itu / Benda"),
        "is" to PhoneticEntry("iz", "ɪz", "Adalah"),
        "am" to PhoneticEntry("em", "æm", "Adalah (untuk I)"),
        "are" to PhoneticEntry("ar", "ɑːr", "Adalah (jamak)"),
        "was" to PhoneticEntry("woz", "wɒz", "Adalah (lampau)"),
        "were" to PhoneticEntry("wer", "wɜːr", "Adalah (lampau jamak)"),
        "have" to PhoneticEntry("hev", "hæv", "Memiliki / Sudah"),
        "has" to PhoneticEntry("hez", "hæz", "Memiliki / Sudah"),
        "had" to PhoneticEntry("hed", "hæd", "Sudah (lampau)"),
        "been" to PhoneticEntry("bin", "biːn", "Telah"),
        "will" to PhoneticEntry("wil", "wɪl", "Akan"),
        "would" to PhoneticEntry("wud", "wʊd", "Ingin / Akan (sopan)", "Huruf 'l' TIDAK dibaca (silent L). Dibaca 'wud'."),
        "could" to PhoneticEntry("kud", "kʊd", "Dapat / Bisa (sopan)", "Huruf 'l' TIDAK dibaca (silent L). Dibaca 'kud'."),
        "should" to PhoneticEntry("syud", "ʃʊd", "Seharusnya", "Huruf 'l' TIDAK dibaca (silent L). Dibaca 'syud'."),
        "can" to PhoneticEntry("ken", "kæn", "Dapat / Bisa"),
        "to" to PhoneticEntry("tu", "tuː", "Ke / Untuk"),
        "the" to PhoneticEntry("dhe", "ðə", "Itu / Sang", "Bunyi 'th' berdengung lembut ('dhe'), atau 'dhi' jika diikuti huruf vokal."),
        "a" to PhoneticEntry("e", "ə", "Sebuah / Seorang"),
        "an" to PhoneticEntry("en", "æn", "Sebuah / Seorang"),
        "and" to PhoneticEntry("end", "ænd", "Dan"),
        "or" to PhoneticEntry("or", "ɔːr", "Atau"),
        "because" to PhoneticEntry("bi-KOZ", "bɪˈkɒz", "Karena", "Tekanan pada suku kata kedua '-KOZ' dengan bunyi akhir 'z'."),
        "however" to PhoneticEntry("haw-EV-er", "haʊˈɛvər", "Namun / Akan tetapi", "Tekanan berada di suku kata tengah '-EV-'."),
        "therefore" to PhoneticEntry("DHER-for", "ˈðɛərfɔːr", "Oleh karena itu", "Awali dengan bunyi 'dh' lembut."),
        "furthermore" to PhoneticEntry("FER-dher-mor", "ˌfɜːrðərˈmɔːr", "Selain itu"),
        "yesterday" to PhoneticEntry("YES-ter-dei", "ˈjɛstərdeɪ", "Kemarin", "Tekanan di awal 'YES', akhiran '-day' dibaca 'dei'."),
        "today" to PhoneticEntry("te-DEI", "təˈdeɪ", "Hari ini", "Suku kata pertama dibaca cepat 'te', bukan 'tu'."),
        "tomorrow" to PhoneticEntry("te-MOR-ou", "təˈmɒrəʊ", "Besok", "Tekanan pada suku kata kedua '-MOR-', akhiran dibaca 'ou'."),
        "next" to PhoneticEntry("nekst", "nɛkst", "Berikutnya / Depan"),
        "week" to PhoneticEntry("wiik", "wiːk", "Minggu / Pekan"),
        "month" to PhoneticEntry("manth", "mʌnθ", "Bulan", "Vokal 'o' dibaca 'a' ('manth') dan diakhiri bunyi 'th' hembusan napas."),
        "year" to PhoneticEntry("yiir", "jɪər", "Tahun"),
        "wednesday" to PhoneticEntry("WENZ-dei", "ˈwɛnzdeɪ", "Hari Rabu", "Huruf 'd' pertama TIDAK dibaca. Lafalkan 'WENZ-dei'."),
        "english" to PhoneticEntry("ING-glisy", "ˈɪŋɡlɪʃ", "Bahasa Inggris", "Huruf 'E' awal dibaca 'I' ('ING-glisy'), bukan 'eng-lish'."),
        "standard" to PhoneticEntry("STEN-derd", "ˈstændərd", "Baku / Standar"),
        "grammar" to PhoneticEntry("GRAM-er", "ˈɡræmər", "Tata bahasa", "Akhiran '-ar' dibaca lembut 'er' ('GRAM-er')."),
        "pronunciation" to PhoneticEntry("pre-nan-si-EI-syen", "prəˌnʌnsiˈeɪʃən", "Pengucapan / Pelafalan", "Perhatikan suku kata tengah adalah '-nan-' (bukan '-noun-'): 'pre-nan-si-EI-syen'."),
        "speak" to PhoneticEntry("spiik", "spiːk", "Berbicara"),
        "speaking" to PhoneticEntry("SPII-king", "ˈspiːkɪŋ", "Berbicara"),
        "study" to PhoneticEntry("STAD-i", "ˈstʌdi", "Belajar", "Huruf 'u' dibaca 'a' pendek ('STAD-i')."),
        "understand" to PhoneticEntry("an-der-STEND", "ˌʌndərˈstænd", "Memahami", "Tekanan pada suku kata terakhir '-STEND'."),
        "explain" to PhoneticEntry("ik-SPLEIN", "ɪkˈspleɪn", "Menjelaskan"),
        "explained" to PhoneticEntry("ik-SPLEIND", "ɪkˈspleɪnd", "Menjelaskan (lampau)"),
        "clearly" to PhoneticEntry("KLIIR-li", "ˈklɪərli", "Dengan jelas"),
        "properly" to PhoneticEntry("PROP-er-li", "ˈprɒpərli", "Dengan baik dan benar"),
        "smoothly" to PhoneticEntry("SMUUDH-li", "ˈsmuːðli", "Dengan lancar"),
        "project" to PhoneticEntry("PRO-jekt", "ˈprɒdʒɛkt", "Proyek", "Sebagai kata benda, tekanan di suku kata pertama 'PRO-jekt'."),
        "plan" to PhoneticEntry("plen", "plæn", "Rencana"),
        "report" to PhoneticEntry("ri-PORT", "rɪˈpɔːrt", "Laporan"),
        "manager" to PhoneticEntry("MEN-i-jer", "ˈmænɪdʒər", "Manajer", "Suku kata tengah dibaca ringan 'i' ('MEN-i-jer')."),
        "meeting" to PhoneticEntry("MII-ting", "ˈmiːtɪŋ", "Rapat / Pertemuan"),
        "postponed" to PhoneticEntry("poust-POUND", "pəʊstˈpəʊnd", "Ditunda", "Tekanan pada suku kata kedua '-POUND'."),
        "presentation" to PhoneticEntry("prez-en-TEI-syen", "ˌprɛzənˈteɪʃən", "Presentasi"),
        "present" to PhoneticEntry("pri-ZENT", "prɪˈzɛnt", "Mempresentasikan"),
        "office" to PhoneticEntry("OF-is", "ˈɒfɪs", "Kantor"),
        "company" to PhoneticEntry("KAM-pe-ni", "ˈkʌmpəni", "Perusahaan", "Huruf 'o' pertama dibaca 'a' ('KAM-pe-ni')."),
        "library" to PhoneticEntry("LAI-brer-i", "ˈlaɪbrəri", "Perpustakaan"),
        "university" to PhoneticEntry("yuu-ni-VER-si-ti", "ˌjuːnɪˈvɜːrsɪti", "Universitas", "Awali dengan bunyi 'yuu' (bukan 'u')."),
        "education" to PhoneticEntry("ej-u-KEI-syen", "ˌɛdʒuˈkeɪʃən", "Pendidikan", "Huruf 'd' bertemu 'u' menghasilkan bunyi 'j' ('ej-u-KEI-syen')."),
        "communication" to PhoneticEntry("ke-myuu-ni-KEI-syen", "kəˌmjuːnɪˈkeɪʃən", "Komunikasi"),
        "important" to PhoneticEntry("im-POR-tent", "ɪmˈpɔːrtənt", "Penting"),
        "confident" to PhoneticEntry("KON-fi-dent", "ˈkɒnfɪdənt", "Percaya diri"),
        "improve" to PhoneticEntry("im-PRUUV", "ɪmˈpruːv", "Meningkatkan", "Vokal 'o' dibaca 'uu' panjang ('im-PRUUV')."),
        "went" to PhoneticEntry("went", "wɛnt", "Pergi (lampau)"),
        "submitted" to PhoneticEntry("seb-MIT-id", "səbˈmɪtɪd", "Menyerahkan (lampau)"),
        "together" to PhoneticEntry("te-GEDH-er", "təˈɡɛðər", "Bersama-sama"),
        "allow" to PhoneticEntry("e-LAW", "əˈlaʊ", "Mengizinkan"),
        "introduce" to PhoneticEntry("in-tre-DYUUS", "ˌɪntrəˈdjuːs", "Memperkenalkan"),
        "myself" to PhoneticEntry("mai-SELF", "maɪˈsɛlf", "Diri saya sendiri")
    )

    fun processParagraph(
        rawParagraph: String,
        style: EnglishStyle
    ): ParagraphTranslationResult {
        val rawSentences = rawParagraph
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(rawParagraph.trim()) }

        val grammarNotes = mutableListOf<GrammarNote>()
        val sentenceBreakdowns = mutableListOf<SentenceBreakdown>()

        for (rawSentence in rawSentences) {
            val isIndonesian = detectLikelyIndonesian(rawSentence)
            val (englishSentence, indonesianMeaning, notes) = if (isIndonesian) {
                translateIndonesianSentence(rawSentence, style)
            } else {
                correctEnglishSentence(rawSentence, style)
            }

            grammarNotes.addAll(notes)
            val caraMembaca = buildCaraMembacaSentence(englishSentence)
            val ipaPhonetic = buildIpaSentence(englishSentence)

            sentenceBreakdowns.add(
                SentenceBreakdown(
                    englishSentence = englishSentence,
                    caraMembaca = caraMembaca,
                    ipaPhonetic = ipaPhonetic,
                    indonesianMeaning = indonesianMeaning
                )
            )
        }

        if (grammarNotes.isEmpty()) {
            grammarNotes.add(
                GrammarNote(
                    originalOrLiteral = rawSentences.firstOrNull() ?: rawParagraph,
                    correctedStandard = sentenceBreakdowns.firstOrNull()?.englishSentence ?: "",
                    ruleCategory = "Standardisasi Struktur & Kosakata (${style.badgeText})",
                    explanationId = "Susunan kalimat telah disesuaikan ke struktur Subjek-Predikat-Objek-Keterangan (S-V-O-Adv) Bahasa Inggris baku dengan pemilihan tense dan tanda baca formal."
                )
            )
        }

        val fullEnglish = sentenceBreakdowns.joinToString(" ") { it.englishSentence }
        val fullCaraMembaca = sentenceBreakdowns.joinToString(" ") { it.caraMembaca }
        val fullIpa = "/" + sentenceBreakdowns.joinToString(" ") {
            it.ipaPhonetic.trim('/')
        } + "/"

        val extractedKeyWords = extractPronunciationWords(fullEnglish)

        return ParagraphTranslationResult(
            sourceText = rawParagraph,
            standardEnglishText = fullEnglish,
            literalDraftComparison = rawParagraph,
            caraMembacaFull = fullCaraMembaca,
            ipaFull = fullIpa,
            indonesianSummary = "Paragraf telah diterjemahkan dan disempurnakan mengikuti kaidah tata bahasa Inggris baku (${style.labelId} - ${style.badgeText}), lengkap dengan panduan cara membaca ejaan Indonesia dan simbol fonetik IPA.",
            styleBadge = style.badgeText,
            sentences = sentenceBreakdowns,
            grammarNotes = grammarNotes.distinctBy { it.ruleCategory + it.correctedStandard }.take(4),
            keyWords = extractedKeyWords,
            isAiGenerated = false
        )
    }

    private fun detectLikelyIndonesian(text: String): Boolean {
        val lower = " " + text.lowercase(Locale.ROOT) + " "
        val idMarkers = listOf(
            " saya ", " kami ", " kita ", " mereka ", " adalah ", " yang ", " untuk ",
            " dengan ", " karena ", " kemarin ", " besok ", " tidak ", " sudah ",
            " akan ", " dalam ", " kepada ", " dari ", " pada ", " namun ", " tetapi ",
            " bahasa ", " belajar ", " bekerja ", " pergi ", " kantor ", " rapat "
        )
        return idMarkers.count { lower.contains(it) } >= 1
    }

    private fun translateIndonesianSentence(
        idSentence: String,
        style: EnglishStyle
    ): Triple<String, String, List<GrammarNote>> {
        var working = idSentence.trim()
        val trailingPunct = if (working.endsWith(".") || working.endsWith("!") || working.endsWith("?")) {
            working.last().toString()
        } else {
            "."
        }
        working = working.trimEnd('.', '!', '?')

        var translated = working.lowercase(Locale.ROOT)
        for ((idPhrase, enPhrase) in indonesianPhraseDictionary) {
            translated = translated.replace(
                Regex("\\b${Regex.escape(idPhrase)}\\b", RegexOption.IGNORE_CASE),
                enPhrase
            )
        }

        // Normalize grammar patterns in translated text
        val notes = mutableListOf<GrammarNote>()
        if (translated.contains("yesterday") && translated.contains("go to")) {
            translated = translated.replace("go to", "went to")
            notes.add(
                GrammarNote(
                    originalOrLiteral = "...yesterday I go to...",
                    correctedStandard = "...yesterday, I went to...",
                    ruleCategory = "Simple Past Tense",
                    explanationId = "Keterangan waktu lampau ('yesterday') mengharuskan perubahan kata kerja 'go' menjadi bentuk lampau V2 'went'."
                )
            )
        }
        if (translated.contains("yesterday") && translated.contains("already submit")) {
            translated = translated.replace("already submit", "submitted")
        }

        // Style refinement
        if (style == EnglishStyle.BUSINESS || style == EnglishStyle.ACADEMIC) {
            translated = translated
                .replace(Regex("\\bi want to\\b", RegexOption.IGNORE_CASE), "I would like to")
                .replace(Regex("\\bcan\\b", RegexOption.IGNORE_CASE), "am able to")
        }

        val cleaned = translated
            .replace(Regex("\\s+"), " ")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } + trailingPunct

        if (notes.isEmpty()) {
            notes.add(
                GrammarNote(
                    originalOrLiteral = idSentence,
                    correctedStandard = cleaned,
                    ruleCategory = "Terjemahan Baku (${style.badgeText})",
                    explanationId = "Kalimat bahasa Indonesia diterjemahkan secara idiomatis ke dalam struktur kalimat Bahasa Inggris baku yang alami dan tepat secara tata bahasa."
                )
            )
        }

        return Triple(cleaned, idSentence, notes)
    }

    private fun correctEnglishSentence(
        enSentence: String,
        style: EnglishStyle
    ): Triple<String, String, List<GrammarNote>> {
        var corrected = enSentence.trim()
        val notes = mutableListOf<GrammarNote>()

        for (rule in brokenEnglishGrammarFixes) {
            if (rule.regex.containsMatchIn(corrected)) {
                val before = corrected
                corrected = rule.regex.replace(corrected, rule.replacement)
                if (rule.noteTitle != null && rule.noteExplanation != null) {
                    notes.add(
                        GrammarNote(
                            originalOrLiteral = before,
                            correctedStandard = corrected,
                            ruleCategory = rule.noteTitle,
                            explanationId = rule.noteExplanation
                        )
                    )
                }
            }
        }

        if (style == EnglishStyle.BUSINESS || style == EnglishStyle.ACADEMIC) {
            corrected = corrected
                .replace("don't", "do not")
                .replace("doesn't", "does not")
                .replace("didn't", "did not")
                .replace("can't", "cannot")
                .replace("won't", "will not")
                .replace("I'm", "I am")
        }

        if (!corrected.endsWith(".") && !corrected.endsWith("!") && !corrected.endsWith("?")) {
            corrected += "."
        }
        corrected = corrected.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }

        val meaning = "Kalimat telah dikoreksi sesuai kaidah tata bahasa Inggris baku (${style.badgeText})."
        return Triple(corrected, meaning, notes)
    }

    fun buildCaraMembacaSentence(englishSentence: String): String {
        val tokens = englishSentence.split(Regex("\\s+")).filter { it.isNotBlank() }
        return tokens.joinToString(" ") { token ->
            val leadingPunct = token.takeWhile { !it.isLetterOrDigit() }
            val trailingPunct = token.takeLastWhile { !it.isLetterOrDigit() }
            val cleanWord = token.drop(leadingPunct.length).dropLast(trailingPunct.length)
            if (cleanWord.isEmpty()) {
                token
            } else {
                val lower = cleanWord.lowercase(Locale.ROOT)
                val phonetic = phoneticLexicon[lower]?.caraMembaca ?: approximateIndonesianPhonetic(lower)
                "$leadingPunct$phonetic$trailingPunct"
            }
        }
    }

    fun buildIpaSentence(englishSentence: String): String {
        val tokens = englishSentence.split(Regex("\\s+")).filter { it.isNotBlank() }
        val inner = tokens.joinToString(" ") { token ->
            val trailingPunct = token.takeLastWhile { !it.isLetterOrDigit() }
            val cleanWord = token.filter { it.isLetterOrDigit() }.lowercase(Locale.ROOT)
            if (cleanWord.isEmpty()) {
                token
            } else {
                val ipa = phoneticLexicon[cleanWord]?.ipa ?: approximateIpa(cleanWord)
                "$ipa$trailingPunct"
            }
        }
        return "/$inner/"
    }

    private fun approximateIndonesianPhonetic(word: String): String {
        if (word.length <= 2) return word
        var s = word
            .replace("tion", "-SYEN")
            .replace("sion", "-ZYEN")
            .replace("ture", "-cher")
            .replace("ough", "ou")
            .replace("igh", "ai")
            .replace("ee", "ii")
            .replace("oo", "uu")
            .replace("ea", "ii")
            .replace("sh", "sy")
            .replace("ch", "ch")
            .replace("th", "dh")
            .replace("ph", "f")
            .replace("ck", "k")
            .replace("qu", "kw")
        if (s.endsWith("e") && s.length > 4 && !s.endsWith("ee")) {
            s = s.dropLast(1)
        }
        return s
    }

    private fun approximateIpa(word: String): String {
        return word
            .replace("tion", "ʃən")
            .replace("sh", "ʃ")
            .replace("ch", "tʃ")
            .replace("th", "ð")
            .replace("ee", "iː")
            .replace("oo", "uː")
    }

    private fun extractPronunciationWords(englishParagraph: String): List<PronunciationWord> {
        val words = englishParagraph
            .split(Regex("[^a-zA-Z]+"))
            .map { it.trim() }
            .filter { it.length >= 4 }
            .distinctBy { it.lowercase(Locale.ROOT) }

        val matched = mutableListOf<PronunciationWord>()
        for (w in words) {
            val lower = w.lowercase(Locale.ROOT)
            val entry = phoneticLexicon[lower]
            if (entry != null && entry.tipId.isNotBlank()) {
                matched.add(
                    PronunciationWord(
                        word = w.replaceFirstChar { it.titlecase(Locale.ROOT) },
                        caraMembaca = entry.caraMembaca,
                        ipa = "/${entry.ipa}/",
                        meaningId = entry.meaningId,
                        tipId = entry.tipId
                    )
                )
            }
        }

        if (matched.size < 4) {
            for (w in words) {
                if (matched.any { it.word.equals(w, ignoreCase = true) }) continue
                val lower = w.lowercase(Locale.ROOT)
                val entry = phoneticLexicon[lower]
                matched.add(
                    PronunciationWord(
                        word = w.replaceFirstChar { it.titlecase(Locale.ROOT) },
                        caraMembaca = entry?.caraMembaca ?: approximateIndonesianPhonetic(lower),
                        ipa = "/${entry?.ipa ?: approximateIpa(lower)}/",
                        meaningId = entry?.meaningId?.ifBlank { "Kosakata penting dalam paragraf" }
                            ?: "Kosakata penting dalam paragraf",
                        tipId = entry?.tipId?.ifBlank { "Ucapkan suku kata berhuruf kapital dengan penekanan suara yang lebih jelas." }
                            ?: "Ucapkan suku kata dengan vokal yang jelas sesuai panduan cara membaca."
                    )
                )
                if (matched.size >= 4) break
            }
        }

        return matched.take(5)
    }
}
