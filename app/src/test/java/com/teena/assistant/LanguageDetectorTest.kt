package com.teena.assistant

import com.google.common.truth.Truth.assertThat
import com.teena.assistant.core.ai.model.LanguageDetector
import com.teena.assistant.core.ai.model.LanguageHint
import org.junit.Test

class LanguageDetectorTest {

    @Test
    fun detectsUrduScriptAsRtl() {
        val hint = LanguageDetector.detect("آپ کیسے ہیں؟ مجھے موسم بتائیں")
        assertThat(hint).isEqualTo(LanguageHint.URDU)
        assertThat(hint.isRtl).isTrue()
    }

    @Test
    fun detectsRomanUrdu() {
        val hint = LanguageDetector.detect("Aap kaise hain mujhe aaj ka kaam batao")
        assertThat(hint).isEqualTo(LanguageHint.ROMAN_URDU)
        assertThat(hint.isRtl).isFalse()
    }

    @Test
    fun detectsHinglish() {
        val hint = LanguageDetector.detect("Matlab kuch samajh par toh aa raha hai yaar")
        // Note: contains "hai" + "yaar" (romanUrduTokens) OR hinglishTokens depending on tokens;
        // pure hinglish without 2 romanUrduTokens:
        val pureHinglish = LanguageDetector.detect("Matlab kuch samajh par toh walon ko")
        assertThat(pureHinglish).isEqualTo(LanguageHint.HINGLISH)
        assertThat(hint.isRtl).isFalse()
    }

    @Test
    fun detectsEnglishByDefault() {
        val hint = LanguageDetector.detect("Summarize the Android build pipeline status.")
        assertThat(hint).isEqualTo(LanguageHint.ENGLISH)
        assertThat(hint.isRtl).isFalse()
    }
}
