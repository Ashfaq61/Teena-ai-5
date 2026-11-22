package com.teena.assistant.core.ai.model

object LanguageDetector {

    private val romanUrduTokens = setOf(
        "kya", "hai", "hain", "nahi", "nhi", "mujhe", "mera", "meri", "aap",
        "tum", "kaise", "kaisa", "karo", "karna", "batao", "samjhao", "acha",
        "theek", "shukriya", "bhai", "yaar", "zaroor", "abhi", "lekin", "kyun",
        "kahan", "kitna", "haan", "ji", "chalo", "sunao", "likho", "dekho"
    )

    private val hinglishTokens = setOf(
        "bhai", "yaar", "karo", "batao", "samajh", "kuch", "matlab", "wala",
        "wali", "se", "ko", "mein", "pe", "par", "toh", "aur", "kyunki"
    )

    fun detect(text: String): LanguageHint {
        if (text.isBlank()) return LanguageHint.ENGLISH

        val arabicScriptCount = text.count { ch ->
            ch in '\u0600'..'\u06FF' || ch in '\u0750'..'\u077F' || ch in '\uFB50'..'\uFDFF'
        }
        if (arabicScriptCount >= 2) {
            return LanguageHint.URDU
        }

        val tokens = text.lowercase()
            .split(Regex("[^a-z]+"))
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return LanguageHint.ENGLISH

        val romanHits = tokens.count { it in romanUrduTokens }
        if (romanHits >= 2 || (tokens.size <= 4 && romanHits >= 1)) {
            return LanguageHint.ROMAN_URDU
        }

        val hinglishHits = tokens.count { it in hinglishTokens }
        if (hinglishHits >= 2) {
            return LanguageHint.HINGLISH
        }

        return LanguageHint.ENGLISH
    }
}
