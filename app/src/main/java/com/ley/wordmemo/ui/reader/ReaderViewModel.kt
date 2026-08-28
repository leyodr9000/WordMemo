package com.ley.wordmemo.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ley.wordmemo.data.api.AiClient
import com.ley.wordmemo.data.api.ChatMessage
import com.ley.wordmemo.data.model.Article
import com.ley.wordmemo.data.model.WordTranslation
import com.ley.wordmemo.data.repository.ArticleRepository
import com.ley.wordmemo.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** 阅读条目：句子 + 翻译状态 */
data class ReaderSentence(
    val original: String,
    val translation: String = "",
    var translating: Boolean = false,
)

data class ReaderUiState(
    val sentenceList: List<ReaderSentence> = emptyList(),
    val wholeTranslated: Boolean = false,        // 全文翻译开关
    val translatingAll: Boolean = false,
    val tappedWord: String = "",
    val wordMeaning: String = "",
    val wordTipAnchor: Pair<Float, Float>? = null,
    val error: String? = null,
    val configured: Boolean = false,
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val aiClient: AiClient,
    private val settingsRepository: SettingsRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state

    private var currentArticleId: Long = 0L
    private val wordCache = mutableMapOf<String, String>()  // 词 -> 释义 (本次会话缓存)

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                configured = settingsRepository.settings.first().isApiConfigured,
            )
            // 内置文章 + 首次可用
            val all = articleRepository.articles.first()
            if (all.isEmpty()) {
                seedArticles()
            }
        }
    }

    /** 无参进入: 自动加载最新一篇文章 */
    fun loadLatest() {
        viewModelScope.launch {
            val all = articleRepository.articles.first()
            if (all.isEmpty()) {
                seedArticles()
                val seeded = articleRepository.articles.first()
                seeded.firstOrNull()?.let { loadArticle(it) }
            } else {
                loadArticle(all.first())
            }
        }
    }

    /** 把一段原文解析为句子列表 */
    fun loadArticle(article: Article) {
        currentArticleId = article.id
        val sentences = splitSentences(article.content)
        _state.value = ReaderUiState(sentenceList = sentences)
    }

    private fun splitSentences(content: String): List<ReaderSentence> {
        return content
            .replace("\n", " ")
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { ReaderSentence(original = it) }
    }

    /** 点击单词 -> 查询/翻译, 显示气泡 */
    fun onWordTap(word: String, anchorX: Float, anchorY: Float) {
        val clean = word.trim().trim(',', '.', ';', ':', '!', '?', '"', '(', ')', '”', '“', '，', '。', '；', '：', '！', '？')
        if (clean.isEmpty()) return
        val lower = clean.lowercase()
        _state.value = _state.value.copy(
            tappedWord = clean,
            wordTipAnchor = anchorX to anchorY,
        )
        wordCache[lower]?.let {
            _state.value = _state.value.copy(wordMeaning = it)
            return
        }
        viewModelScope.launch {
            // 1) 离线词典优先 (无需 API)
            com.ley.wordmemo.data.reader.OfflineDict.ensureLoaded(context)
            com.ley.wordmemo.data.reader.OfflineDict.lookup(clean)?.let { off ->
                wordCache[lower] = off
                _state.value = _state.value.copy(wordMeaning = "📖 $off")
                return@launch
            }
            // 2) 数据库缓存 (之前 AI 翻译过的词)
            val cached = articleRepository.findTranslation(lower)
            if (cached != null) {
                wordCache[lower] = cached.meaning
                _state.value = _state.value.copy(wordMeaning = cached.meaning)
                return@launch
            }
            // 3) 翻译源=ai 且有配置时才走 AI
            val settings = settingsRepository.settings.first()
            if (settings.translationSource != "ai" || !settings.isApiConfigured) {
                _state.value = _state.value.copy(wordMeaning = "(词典未收录，可切换 AI 翻译)")
                return@launch
            }
            val meaning = withContext(Dispatchers.IO) {
                aiClient.chat(
                    settings = settings,
                    history = listOf(
                        ChatMessage("system", "你是英文词典。只输出中文释义与词性，10字以内，不要解释。"),
                        ChatMessage("user", clean),
                    ),
                )
            }.trim().take(40)
            wordCache[lower] = meaning
            articleRepository.cacheTranslation(WordTranslation(word = lower, meaning = meaning))
            _state.value = _state.value.copy(wordMeaning = meaning)
        }
    }

    fun dismissWordTip() {
        _state.value = _state.value.copy(tappedWord = "", wordMeaning = "")
    }

    /** 翻译第 i 句 */
    fun translateSentence(index: Int) {
        val st = _state.value
        val s = st.sentenceList.getOrNull(index) ?: return
        if (s.translating) return
        _state.value = st.copy(
            sentenceList = st.sentenceList.mapIndexed { i, it ->
                if (i == index) it.copy(translating = true) else it
            },
        )
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            // 离线词典优先: 无需 API
            if (settings.translationSource != "ai" || !settings.isApiConfigured) {
                val off = withContext(Dispatchers.IO) {
                    com.ley.wordmemo.data.reader.OfflineDict.translateSentenceOffline(context, s.original)
                }
                _state.value = applyTranslation(index, if (off.isBlank()) "（未收录）" else off)
                return@launch
            }
            val tr = withContext(Dispatchers.IO) {
                aiClient.chat(
                    settings = settings,
                    history = listOf(
                        ChatMessage("system", "你是专业翻译。把这句英文翻成通顺地道的中文，只输出译文。"),
                        ChatMessage("user", s.original),
                    ),
                )
            }.trim().take(200)
            _state.value = applyTranslation(index, tr)
        }
    }

    private fun applyTranslation(index: Int, tr: String): ReaderUiState {
        val st = _state.value
        return st.copy(
            sentenceList = st.sentenceList.mapIndexed { i, it ->
                if (i == index) it.copy(translation = tr, translating = false) else it
            },
        )
    }

    /** 全文翻译开关: 逐句翻译所有未翻译的句子 */
    fun toggleWholeTranslation() {
        val st = _state.value
        val nowOn = !st.wholeTranslated
        _state.value = st.copy(
            wholeTranslated = nowOn,
            translatingAll = nowOn,
        )
        if (!nowOn) return  // 关闭 -> 保留已翻译内容
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val useOffline = settings.translationSource != "ai" || !settings.isApiConfigured
            if (useOffline) {
                com.ley.wordmemo.data.reader.OfflineDict.ensureLoaded(context)
            }
            // 批量: 全部未翻译的句子
            val idxList = _state.value.sentenceList.indices.toList()
            for (idx in idxList) {
                val cur = _state.value
                val s = cur.sentenceList.getOrNull(idx) ?: continue
                if (s.translation.isNotBlank() || s.translating) continue
                _state.value = cur.copy(
                    sentenceList = cur.sentenceList.mapIndexed { i, it ->
                        if (i == idx) it.copy(translating = true) else it
                    },
                )
                val tr = withContext(Dispatchers.IO) {
                    if (useOffline) {
                        com.ley.wordmemo.data.reader.OfflineDict.translateSentenceOffline(context, s.original)
                    } else {
                        aiClient.chat(
                            settings = settings,
                            history = listOf(
                                ChatMessage("system", "你是专业翻译。把这句英文翻成通顺地道的中文，只输出译文。"),
                                ChatMessage("user", s.original),
                            ),
                        ).trim().take(200)
                    }
                }
                _state.value = _state.value.copy(
                    sentenceList = _state.value.sentenceList.mapIndexed { i, it ->
                        if (i == idx) it.copy(translation = tr, translating = false) else it
                    },
                )
            }
            _state.value = _state.value.copy(translatingAll = false)
        }
    }

    /** 根据拍照/粘贴的英汉资料识别文章（占位：先支持粘贴文本） */
    fun importText(title: String, content: String) {
        viewModelScope.launch {
            val article = Article(title = title, content = content, source = "手动粘贴")
            val id = articleRepository.insert(article)
            articleRepository.article(id).first()?.let { loadArticle(it) }
        }
    }

    private suspend fun seedArticles() {
        val sample = Article(
            title = "考研阅读·示例",
            source = "内置",
            content = BUILT_IN_ARTICLE,
        )
        articleRepository.insert(sample)
    }

    companion object {
        /** 内置一篇考研风格阅读短文 */
        val BUILT_IN_ARTICLE = """
            The rapid pace of technological change has transformed the way we live and work. It has reshaped industries, created new forms of employment, and changed the nature of education.
            Yet this progress has not been without cost. Many workers find their skills outdated within a few years, and the gap between the digitally skilled and the rest of society continues to widen.
            Some economists argue that the solution lies in lifelong learning, while others insist that governments must take a more active role in protecting those who are left behind.
            The debate is unlikely to be settled soon. What is clear, however, is that no country can afford to ignore the social consequences of technological advancement.
        """.trimIndent()
    }
}