package com.ley.wordmemo.data.model

/** 单词书统计（DAO 查询结果，非实体） */
data class BookStat(
    val book: String = "默认词库",
    val total: Int = 0,
    val newCount: Int = 0,
    val masteredCount: Int = 0,
    val forgottenCount: Int = 0,
    val lastUpdated: Long = 0,
) {
    /** 掌握进度：非生词（熟练+忘记）占比 */
    val progress: Float
        get() = if (total == 0) 0f else (masteredCount + forgottenCount).toFloat() / total
}

object BookNames {
    const val DEFAULT = "默认词库"
    fun normalize(sourceBook: String): String =
        if (sourceBook.isBlank()) DEFAULT else sourceBook
}