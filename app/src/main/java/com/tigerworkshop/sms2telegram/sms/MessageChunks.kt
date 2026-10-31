package com.tigerworkshop.sms2telegram.sms

/** Keep every UTF-16 character, without splitting a surrogate pair. */
fun messageChunks(text: String, limit: Int = 3900): List<String> {
    require(limit >= 2)
    if (text.isEmpty()) return listOf("")
    val chunks = mutableListOf<String>()
    var start = 0
    while (start < text.length) {
        var end = minOf(start + limit, text.length)
        if (end < text.length && text[end - 1].isHighSurrogate() && text[end].isLowSurrogate()) end--
        chunks.add(text.substring(start, end))
        start = end
    }
    return chunks
}
