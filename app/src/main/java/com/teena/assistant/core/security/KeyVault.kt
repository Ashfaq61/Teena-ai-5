package com.teena.assistant.core.security

interface KeyVault {
    fun saveKey(slotId: String, rawKey: String)
    fun getKey(slotId: String): String?
    fun deleteKey(slotId: String)
}

fun maskApiKey(rawKey: String): String {
    val trimmed = rawKey.trim()
    if (trimmed.isEmpty()) return "Empty"
    if (trimmed.length <= 8) return "••••${trimmed.takeLast(2)}"
    return "${trimmed.take(4)}••••${trimmed.takeLast(4)}"
}
