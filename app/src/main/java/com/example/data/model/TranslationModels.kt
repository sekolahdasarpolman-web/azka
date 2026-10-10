package com.example.data.model

import kotlinx.serialization.Serializable

enum class EnglishStyle(val labelId: String, val badgeText: String, val promptInstruction: String) {
    STANDARD(
        labelId = "Baku Umum",
        badgeText = "Standard English",
        promptInstruction = "Standard, natural, grammatically impeccable modern English suitable for general formal and everyday communication."
    ),
    BUSINESS(
        labelId = "Bisnis & Kerja",
        badgeText = "Business Formal",
        promptInstruction = "Polished, professional Business English suitable for workplace emails, presentations, and executive communication."
    ),
    ACADEMIC(
        labelId = "Akademik",
        badgeText = "Academic Standard",
        promptInstruction = "Formal Academic English with precise vocabulary, objective tone, and standard scholarly grammar."
    ),
    CONVERSATIONAL(
        labelId = "Sopan Sehari-hari",
        badgeText = "Polite Spoken",
        promptInstruction = "Polite, fluent, grammatically correct spoken Standard English that sounds natural when spoken aloud."
    )
}

enum class AccentLocale(val labelId: String, val shortCode: String, val languageTag: String) {
    US("US (Amerika)", "EN-US", "en-US"),
    UK("UK (Inggris)", "EN-GB", "en-GB")
}

@Serializable
data class SentenceBreakdown(
    val englishSentence: String,
    val caraMembaca: String,
    val ipaPhonetic: String,
    val indonesianMeaning: String
)

@Serializable
data class GrammarNote(
    val originalOrLiteral: String,
    val correctedStandard: String,
    val ruleCategory: String,
    val explanationId: String
)

@Serializable
data class PronunciationWord(
    val word: String,
    val caraMembaca: String,
    val ipa: String,
    val meaningId: String,
    val tipId: String
)

@Serializable
data class ParagraphTranslationResult(
    val sourceText: String,
    val standardEnglishText: String,
    val literalDraftComparison: String,
    val caraMembacaFull: String,
    val ipaFull: String,
    val indonesianSummary: String,
    val styleBadge: String,
    val sentences: List<SentenceBreakdown>,
    val grammarNotes: List<GrammarNote>,
    val keyWords: List<PronunciationWord>,
    val isAiGenerated: Boolean = true
)

data class SampleParagraphPreset(
    val id: String,
    val title: String,
    val category: String,
    val sourceParagraph: String,
    val preferredStyle: EnglishStyle,
    val prebuiltResult: ParagraphTranslationResult
)

object CuratedSamples {
    val presets: List<SampleParagraphPreset> = listOf(
        SampleParagraphPreset(
            id = "work_meeting",
            title = "Jadwal Rapat & Proyek Kantor",
            category = "Bisnis & Kerja",
            sourceParagraph = "Kemarin saya sudah mengirim laporan perkembangan proyek kepada manajer. Namun karena beliau sedang dinas di luar kota, rapat evaluasi kita ditunda sampai hari Rabu minggu depan. Mohon seluruh anggota tim memeriksa kembali data masing-masing agar presentasi berjalan lancar.",
            preferredStyle = EnglishStyle.BUSINESS,
            prebuiltResult = ParagraphTranslationResult(
                sourceText = "Kemarin saya sudah mengirim laporan perkembangan proyek kepada manajer. Namun karena beliau sedang dinas di luar kota, rapat evaluasi kita ditunda sampai hari Rabu minggu depan. Mohon seluruh anggota tim memeriksa kembali data masing-masing agar presentasi berjalan lancar.",
                standardEnglishText = "Yesterday, I submitted the project progress report to the manager. However, because he or she is currently away on a business trip, our evaluation meeting has been postponed until next Wednesday. Please ask all team members to review their respective data so that the presentation runs smoothly.",
                literalDraftComparison = "Yesterday I already send the project progress report to manager. But because he is out of town on duty, our evaluation meeting is delay until Wednesday next week. Please all team members check again their data so presentation goes smooth.",
                caraMembacaFull = "YES-ter-dei, ai seb-MIT-id dhe PRO-jekt PROG-res ri-PORT tu dhe MEN-i-jer. haw-EV-er, bi-KOZ hi or syi iz KER-ent-li e-WEI on e BIZ-nes trip, aw-er i-vel-yu-EI-syen MII-ting hez bin poust-POUND en-TIL nekst WENZ-dei. pliiz ask ol tiim MEM-berz tu ri-VYU dher ri-SPEK-tiv DEI-te sou dhet dhe prez-en-TEI-syen ranz SMUUDH-li.",
                ipaFull = "/ˈjɛstərdeɪ, aɪ səbˈmɪtɪd ðə ˈprɒdʒɛkt ˈprəʊɡrɛs rɪˈpɔːrt tuː ðə ˈmænɪdʒər. haʊˈɛvər, bɪˈkɒz hiː ɔːr ʃiː ɪz ˈkʌrəntli əˈweɪ ɒn ə ˈbɪznəs trɪp, ˈaʊər ɪˌvæljuˈeɪʃən ˈmiːtɪŋ hæz biːn pəʊstˈpəʊnd ʌnˈtɪl nɛkst ˈwɛnzdeɪ. pliːz ɑːsk ɔːl tiːm ˈmɛmbərz tuː rɪˈvjuː ðɛər rɪˈspɛktɪv ˈdeɪtə səʊ ðæt ðə ˌprɛzənˈteɪʃən rʌnz ˈsmuːðli./",
                indonesianSummary = "Terjemahan disesuaikan ke dalam Bahasa Inggris Bisnis Baku menggunakan Simple Past Tense untuk tindakan di masa lampau ('Yesterday, I submitted...'), Present Perfect Passive ('has been postponed') untuk dampak jadwal yang masih berlaku, serta kosakata profesional ('respective data', 'runs smoothly').",
                styleBadge = "Business Formal",
                sentences = listOf(
                    SentenceBreakdown(
                        englishSentence = "Yesterday, I submitted the project progress report to the manager.",
                        caraMembaca = "YES-ter-dei, ai seb-MIT-id dhe PRO-jekt PROG-res ri-PORT tu dhe MEN-i-jer.",
                        ipaPhonetic = "/ˈjɛstərdeɪ, aɪ səbˈmɪtɪd ðə ˈprɒdʒɛkt ˈprəʊɡrɛs rɪˈpɔːrt tuː ðə ˈmænɪdʒər/",
                        indonesianMeaning = "Kemarin, saya telah menyerahkan laporan perkembangan proyek kepada manajer."
                    ),
                    SentenceBreakdown(
                        englishSentence = "However, because he or she is currently away on a business trip, our evaluation meeting has been postponed until next Wednesday.",
                        caraMembaca = "haw-EV-er, bi-KOZ hi or syi iz KER-ent-li e-WEI on e BIZ-nes trip, aw-er i-vel-yu-EI-syen MII-ting hez bin poust-POUND en-TIL nekst WENZ-dei.",
                        ipaPhonetic = "/haʊˈɛvər, bɪˈkɒz hiː ɔːr ʃiː ɪz ˈkʌrəntli əˈweɪ ɒn ə ˈbɪznəs trɪp, ˈaʊər ɪˌvæljuˈeɪʃən ˈmiːtɪŋ hæz biːn pəʊstˈpəʊnd ʌnˈtɪl nɛkst ˈwɛnzdeɪ/",
                        indonesianMeaning = "Namun, karena beliau saat ini sedang perjalanan dinas luar kota, rapat evaluasi kita ditunda hingga Rabu depan."
                    ),
                    SentenceBreakdown(
                        englishSentence = "Please ask all team members to review their respective data so that the presentation runs smoothly.",
                        caraMembaca = "pliiz ask ol tiim MEM-berz tu ri-VYU dher ri-SPEK-tiv DEI-te sou dhet dhe prez-en-TEI-syen ranz SMUUDH-li.",
                        ipaPhonetic = "/pliːz ɑːsk ɔːl tiːm ˈmɛmbərz tuː rɪˈvjuː ðɛər rɪˈspɛktɪv ˈdeɪtə səʊ ðæt ðə ˌprɛzənˈteɪʃən rʌnz ˈsmuːðli/",
                        indonesianMeaning = "Mohon minta seluruh anggota tim meninjau kembali data masing-masing agar presentasi berjalan lancar."
                    )
                ),
                grammarNotes = listOf(
                    GrammarNote(
                        originalOrLiteral = "Yesterday I already send...",
                        correctedStandard = "Yesterday, I submitted...",
                        ruleCategory = "Simple Past Tense & Diksi Formal",
                        explanationId = "Keterangan waktu lampau spesifik ('Yesterday') wajib diikuti kata kerja bentuk kedua (V2: 'submitted' / 'sent'), bukan V1. Kata 'submitted' lebih baku untuk laporan resmi dibanding 'sent'."
                    ),
                    GrammarNote(
                        originalOrLiteral = "...our evaluation meeting is delay until Wednesday next week.",
                        correctedStandard = "...our evaluation meeting has been postponed until next Wednesday.",
                        ruleCategory = "Passive Voice (Present Perfect)",
                        explanationId = "Penggunaan 'has been postponed' menyatakan keputusan penundaan yang baru terjadi dan hasilnya masih berlaku hingga sekarang. Selain itu, 'Rabu depan' dalam bahasa Inggris baku adalah 'next Wednesday'."
                    ),
                    GrammarNote(
                        originalOrLiteral = "...check again their data so presentation goes smooth.",
                        correctedStandard = "...review their respective data so that the presentation runs smoothly.",
                        ruleCategory = "Adverbia & Kosakata Baku",
                        explanationId = "Kata kerja 'runs' membutuhkan kata keterangan (adverb) 'smoothly' (dengan akhiran -ly), bukan kata sifat 'smooth'. Kata 'masing-masing' diterjemahkan secara baku menjadi 'their respective'."
                    )
                ),
                keyWords = listOf(
                    PronunciationWord(
                        word = "Wednesday",
                        caraMembaca = "WENZ-dei",
                        ipa = "/ˈwɛnzdeɪ/",
                        meaningId = "Hari Rabu",
                        tipId = "Huruf 'd' pertama dan 'e' tengah TIDAK dibaca (silent D). Jangan dibaca 'wed-nes-dei', melainkan 'WENZ-dei'."
                    ),
                    PronunciationWord(
                        word = "Postponed",
                        caraMembaca = "poust-POUND",
                        ipa = "/pəʊstˈpəʊnd/",
                        meaningId = "Ditunda / Diundur",
                        tipId = "Huruf 't' di tengah terdengar sangat ringan menyatu ke 'p', dengan tekanan kuat pada suku kata kedua '-POUND'."
                    ),
                    PronunciationWord(
                        word = "Respective",
                        caraMembaca = "ri-SPEK-tiv",
                        ipa = "/rɪˈspɛktɪv/",
                        meaningId = "Masing-masing / Terkait",
                        tipId = "Tekanan berada di tengah ('SPEK'), akhiran '-tive' dibaca pendek 'tiv' (bukan 'taif')."
                    ),
                    PronunciationWord(
                        word = "Smoothly",
                        caraMembaca = "SMUUDH-li",
                        ipa = "/ˈsmuːðli/",
                        meaningId = "Dengan lancar",
                        tipId = "Bunyi 'th' adalah bunyi berdengung lembut (voiced dental fricative) dengan ujung lidah sedikit menyentuh gigi atas."
                    )
                ),
                isAiGenerated = true
            )
        ),
        SampleParagraphPreset(
            id = "broken_english_correction",
            title = "Koreksi Paragraf Inggris Tidak Baku",
            category = "Koreksi Grammar",
            sourceParagraph = "Me and my friend was go to the library yesterday for study English grammar together. She don't understand about how to using present perfect tense, so I explain it to her very clear. After that, we feels more confidence for speaking English in class.",
            preferredStyle = EnglishStyle.STANDARD,
            prebuiltResult = ParagraphTranslationResult(
                sourceText = "Me and my friend was go to the library yesterday for study English grammar together. She don't understand about how to using present perfect tense, so I explain it to her very clear. After that, we feels more confidence for speaking English in class.",
                standardEnglishText = "My friend and I went to the library yesterday to study English grammar together. She did not understand how to use the present perfect tense, so I explained it to her very clearly. Afterward, we felt much more confident about speaking English in class.",
                literalDraftComparison = "Me and my friend was go to the library yesterday for study English grammar together. She don't understand about how to using present perfect tense...",
                caraMembacaFull = "mai frend end ai went tu dhe LAI-brer-i YES-ter-dei tu STAD-i ING-glisy GRAM-er te-GEDH-er. syi did not an-der-STEND haw tu yuuz dhe PREZ-ent PER-fikt tens, sou ai ik-SPLEIND it tu her VER-i KLIIR-li. AF-ter-werd, wii felt mach mor KON-fi-dent e-BAWT SPII-king ING-glisy in klas.",
                ipaFull = "/maɪ frɛnd ænd aɪ wɛnt tuː ðə ˈlaɪbrəri ˈjɛstərdeɪ tuː ˈstʌdi ˈɪŋɡlɪʃ ˈɡræmər təˈɡɛðər. ʃiː dɪd nɒt ˌʌndərˈstænd haʊ tuː juːz ðə ˈprɛzənt ˈpɜːrfɪkt tɛns, səʊ aɪ ɪkˈspleɪnd ɪt tuː hɜːr ˈvɛri ˈklɪərli. ˈɑːftərwərd, wiː fɛlt mʌtʃ mɔːr ˈkɒnfɪdənt əˈbaʊt ˈspiːkɪŋ ˈɪŋɡlɪʃ ɪn klɑːs./",
                indonesianSummary = "Paragraf Bahasa Inggris yang belum baku ini telah dikoreksi sepenuhnya menjadi Standard English: memperbaiki posisi subjek ('My friend and I'), konsistensi Past Tense ('went', 'did not understand', 'explained', 'felt'), infinitive ('to study', 'how to use'), serta bentuk kata sifat/keterangan ('clearly', 'confident').",
                styleBadge = "Standard English",
                sentences = listOf(
                    SentenceBreakdown(
                        englishSentence = "My friend and I went to the library yesterday to study English grammar together.",
                        caraMembaca = "mai frend end ai went tu dhe LAI-brer-i YES-ter-dei tu STAD-i ING-glisy GRAM-er te-GEDH-er.",
                        ipaPhonetic = "/maɪ frɛnd ænd aɪ wɛnt tuː ðə ˈlaɪbrəri ˈjɛstərdeɪ tuː ˈstʌdi ˈɪŋɡlɪʃ ˈɡræmər təˈɡɛðər/",
                        indonesianMeaning = "Teman saya dan saya pergi ke perpustakaan kemarin untuk belajar tata bahasa Inggris bersama-sama."
                    ),
                    SentenceBreakdown(
                        englishSentence = "She did not understand how to use the present perfect tense, so I explained it to her very clearly.",
                        caraMembaca = "syi did not an-der-STEND haw tu yuuz dhe PREZ-ent PER-fikt tens, sou ai ik-SPLEIND it tu her VER-i KLIIR-li.",
                        ipaPhonetic = "/ʃiː dɪd nɒt ˌʌndərˈstænd haʊ tuː juːz ðə ˈprɛzənt ˈpɜːrfɪkt tɛns, səʊ aɪ ɪkˈspleɪnd ɪt tuː hɜːr ˈvɛri ˈklɪərli/",
                        indonesianMeaning = "Dia tidak memahami cara menggunakan present perfect tense, jadi saya menjelaskannya kepadanya dengan sangat jelas."
                    ),
                    SentenceBreakdown(
                        englishSentence = "Afterward, we felt much more confident about speaking English in class.",
                        caraMembaca = "AF-ter-werd, wii felt mach mor KON-fi-dent e-BAWT SPII-king ING-glisy in klas.",
                        ipaPhonetic = "/ˈɑːftərwərd, wiː fɛlt mʌtʃ mɔːr ˈkɒnfɪdənt əˈbaʊt ˈspiːkɪŋ ˈɪŋɡlɪʃ ɪn klɑːs/",
                        indonesianMeaning = "Setelah itu, kami merasa jauh lebih percaya diri untuk berbicara bahasa Inggris di kelas."
                    )
                ),
                grammarNotes = listOf(
                    GrammarNote(
                        originalOrLiteral = "Me and my friend was go... for study...",
                        correctedStandard = "My friend and I went... to study...",
                        ruleCategory = "Subjek Kata Ganti & Infinitive Tujuan",
                        explanationId = "Sebagai subjek kalimat, gunakan 'My friend and I' (bukan 'Me and my friend'). Kata kerja lampau dari 'go' adalah 'went' (tanpa 'was'). Untuk menyatakan tujuan ('untuk belajar'), gunakan to-infinitive 'to study', bukan 'for study'."
                    ),
                    GrammarNote(
                        originalOrLiteral = "She don't understand about how to using... I explain it very clear.",
                        correctedStandard = "She did not understand how to use... I explained it very clearly.",
                        ruleCategory = "Konsistensi Past Tense & Struktur 'How to + V1'",
                        explanationId = "Karena peristiwa terjadi kemarin, gunakan 'did not understand' dan 'explained'. Setelah 'understand' tidak perlu preposisi 'about', dan setelah 'how to' wajib diikuti kata kerja dasar V1 ('use'). 'Clearly' (adverbia) menerangkan kata kerja 'explained'."
                    ),
                    GrammarNote(
                        originalOrLiteral = "...we feels more confidence for speaking...",
                        correctedStandard = "...we felt much more confident about speaking...",
                        ruleCategory = "Adjective vs Noun ('Confident' vs 'Confidence')",
                        explanationId = "Setelah linking verb 'felt' (bentuk lampau dari feel), gunakan kata sifat (adjective) 'confident' (percaya diri), bukan kata benda 'confidence' (kepercayaan diri)."
                    )
                ),
                keyWords = listOf(
                    PronunciationWord(
                        word = "Library",
                        caraMembaca = "LAI-brer-i",
                        ipa = "/ˈlaɪbrəri/",
                        meaningId = "Perpustakaan",
                        tipId = "Perhatikan bunyi 'brer' di tengah, jangan dilafalkan 'lai-ber-i'."
                    ),
                    PronunciationWord(
                        word = "Explained",
                        caraMembaca = "ik-SPLEIND",
                        ipa = "/ɪkˈspleɪnd/",
                        meaningId = "Menjelaskan (lampau)",
                        tipId = "Akhiran '-ed' setelah bunyi 'n' dibaca sebagai bunyi 'd' lembut ('ik-SPLEIND'), bukan 'ik-splein-id'."
                    ),
                    PronunciationWord(
                        word = "Confident",
                        caraMembaca = "KON-fi-dent",
                        ipa = "/ˈkɒnfɪdənt/",
                        meaningId = "Percaya diri (kata sifat)",
                        tipId = "Tekanan berada di suku kata pertama 'KON', sedangkan vokal kedua dan ketiga terdengar cepat dan ringan."
                    )
                ),
                isAiGenerated = true
            )
        ),
        SampleParagraphPreset(
            id = "self_intro_interview",
            title = "Perkenalan Diri & Wawancara Kerja",
            category = "Karir & Profesional",
            sourceParagraph = "Perkenalkan nama saya Budi dan saya lulusan baru jurusan Teknik Informatika. Selama kuliah, saya aktif mengembangkan berbagai aplikasi perangkat bergerak dan mengikuti program magang selama enam bulan. Saya sangat tertarik bergabung dengan perusahaan ini karena ingin berkontribusi melalui keahlian pemrograman saya.",
            preferredStyle = EnglishStyle.BUSINESS,
            prebuiltResult = ParagraphTranslationResult(
                sourceText = "Perkenalkan nama saya Budi dan saya lulusan baru jurusan Teknik Informatika. Selama kuliah, saya aktif mengembangkan berbagai aplikasi perangkat bergerak dan mengikuti program magang selama enam bulan. Saya sangat tertarik bergabung dengan perusahaan ini karena ingin berkontribusi melalui keahlian pemrograman saya.",
                standardEnglishText = "Allow me to introduce myself; my name is Budi, and I am a recent graduate in Informatics Engineering. During my university studies, I actively developed various mobile applications and completed a six-month internship program. I am eager to join this company because I want to contribute through my programming expertise.",
                literalDraftComparison = "Let me introduce my name is Budi and I am new graduate major Informatics Engineering. During college, I active develop many mobile application and join internship program for six months.",
                caraMembacaFull = "e-LAW mii tu in-tre-DYUUS mai-SELF; mai neim iz BU-di, end ai em e RII-sent GRAD-ju-et in in-for-MAT-iks en-ji-NIIR-ing. DYUR-ing mai yuu-ni-VER-si-ti STAD-iiz, ai AK-tiv-li di-VEL-ept VER-i-es MOU-bel ap-li-KEI-syenz end kem-PLII-tid e siks-manth IN-tern-syip PROU-gram. ai em II-ger tu join dhis KAM-pe-ni bi-KOZ ai wont tu ken-TRIB-yuut thruu mai PROU-gram-ing ek-sper-TIIZ.",
                ipaFull = "/əˈlaʊ miː tuː ˌɪntrəˈdjuːs maɪˈsɛlf; maɪ neɪm ɪz ˈbuːdi, ænd aɪ æm ə ˈriːsənt ˈɡrædʒuət ɪn ˌɪnfərˈmætɪks ˌɛndʒɪˈnɪərɪŋ. ˈdjʊərɪŋ maɪ ˌjuːnɪˈvɜːrsɪti ˈstʌdiz, aɪ ˈæktɪvli dɪˈvɛləpt ˈvɛəriəs ˈməʊbəl ˌæplɪˈkeɪʃənz ænd kəmˈpliːtɪd ə sɪks-mʌnθ ˈɪntɜːrnʃɪp ˈprəʊɡræm. aɪ æm ˈiːɡər tuː dʒɔɪn ðɪs ˈkʌmpəni bɪˈkɒz aɪ wɒnt tuː kənˈtrɪbjuːt θruː maɪ ˈprəʊɡræmɪŋ ˌɛkspɜːrˈtiːz./",
                indonesianSummary = "Menggunakan struktur perkenalan profesional yang baku ('Allow me to introduce myself', 'recent graduate in...'), kata sifat majemuk dengan tanda hubung ('six-month internship'), serta pilihan diksi wawancara kerja yang kuat ('eager to join', 'programming expertise').",
                styleBadge = "Business Formal",
                sentences = listOf(
                    SentenceBreakdown(
                        englishSentence = "Allow me to introduce myself; my name is Budi, and I am a recent graduate in Informatics Engineering.",
                        caraMembaca = "e-LAW mii tu in-tre-DYUUS mai-SELF; mai neim iz BU-di, end ai em e RII-sent GRAD-ju-et in in-for-MAT-iks en-ji-NIIR-ing.",
                        ipaPhonetic = "/əˈlaʊ miː tuː ˌɪntrəˈdjuːs maɪˈsɛlf; maɪ neɪm ɪz ˈbuːdi, ænd aɪ æm ə ˈriːsənt ˈɡrædʒuət ɪn ˌɪnfərˈmætɪks ˌɛndʒɪˈnɪərɪŋ/",
                        indonesianMeaning = "Izinkan saya memperkenalkan diri; nama saya Budi, dan saya merupakan lulusan baru di bidang Teknik Informatika."
                    ),
                    SentenceBreakdown(
                        englishSentence = "During my university studies, I actively developed various mobile applications and completed a six-month internship program.",
                        caraMembaca = "DYUR-ing mai yuu-ni-VER-si-ti STAD-iiz, ai AK-tiv-li di-VEL-ept VER-i-es MOU-bel ap-li-KEI-syenz end kem-PLII-tid e siks-manth IN-tern-syip PROU-gram.",
                        ipaPhonetic = "/ˈdjʊərɪŋ maɪ ˌjuːnɪˈvɜːrsɪti ˈstʌdiz, aɪ ˈæktɪvli dɪˈvɛləpt ˈvɛəriəs ˈməʊbəl ˌæplɪˈkeɪʃənz ænd kəmˈpliːtɪd ə sɪks-mʌnθ ˈɪntɜːrnʃɪp ˈprəʊɡræm/",
                        indonesianMeaning = "Selama masa perkuliahan saya, saya aktif mengembangkan berbagai aplikasi seluler dan menyelesaikan program magang selama enam bulan."
                    ),
                    SentenceBreakdown(
                        englishSentence = "I am eager to join this company because I want to contribute through my programming expertise.",
                        caraMembaca = "ai em II-ger tu join dhis KAM-pe-ni bi-KOZ ai wont tu ken-TRIB-yuut thruu mai PROU-gram-ing ek-sper-TIIZ.",
                        ipaPhonetic = "/aɪ æm ˈiːɡər tuː dʒɔɪn ðɪs ˈkʌmpəni bɪˈkɒz aɪ wɒnt tuː kənˈtrɪbjuːt θruː maɪ ˈprəʊɡræmɪŋ ˌɛkspɜːrˈtiːz/",
                        indonesianMeaning = "Saya sangat antusias bergabung dengan perusahaan ini karena saya ingin berkontribusi melalui keahlian pemrograman saya."
                    )
                ),
                grammarNotes = listOf(
                    GrammarNote(
                        originalOrLiteral = "Introduce my name is Budi and I am new graduate...",
                        correctedStandard = "Allow me to introduce myself; my name is Budi, and I am a recent graduate...",
                        ruleCategory = "Ungkapan Perkenalan Baku",
                        explanationId = "Hindari menggabungkan 'Introduce my name is...'. Gunakan 'Allow me to introduce myself' atau langsung 'My name is...'. Istilah baku untuk lulusan baru adalah 'a recent graduate' atau 'a fresh graduate'."
                    ),
                    GrammarNote(
                        originalOrLiteral = "...six months internship program.",
                        correctedStandard = "...a six-month internship program.",
                        ruleCategory = "Compound Adjective (Kata Sifat Majemuk)",
                        explanationId = "Ketika ukuran waktu diletakkan sebelum kata benda sebagai penjelas ('internship program'), gunakan tanda hubung dan bentuk tunggal: 'six-month' (tanpa huruf 's')."
                    )
                ),
                keyWords = listOf(
                    PronunciationWord(
                        word = "Graduate",
                        caraMembaca = "GRAD-ju-et",
                        ipa = "/ˈɡrædʒuət/",
                        meaningId = "Lulusan (kata benda)",
                        tipId = "Sebagai kata benda (lulusan), akhiran '-ate' dibaca pendek 'et' ('GRAD-ju-et'), sedangkan sebagai kata kerja (lulus) dibaca 'GRAD-ju-eit'."
                    ),
                    PronunciationWord(
                        word = "Developed",
                        caraMembaca = "di-VEL-ept",
                        ipa = "/dɪˈvɛləpt/",
                        meaningId = "Mengembangkan (lampau)",
                        tipId = "Akhiran '-ed' setelah bunyi 'p' dibaca seperti huruf 't' ('di-VEL-ept'), bukan 'di-vel-op-ed'."
                    ),
                    PronunciationWord(
                        word = "Expertise",
                        caraMembaca = "ek-sper-TIIZ",
                        ipa = "/ˌɛkspɜːrˈtiːz/",
                        meaningId = "Keahlian mendalam",
                        tipId = "Tekanan berada di suku kata terakhir '-TIIZ' dengan vokal 'i' panjang dan akhiran bunyi 'z'."
                    )
                ),
                isAiGenerated = true
            )
        )
    )
}
