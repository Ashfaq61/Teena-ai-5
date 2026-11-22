package com.teena.assistant.core.ai.provider

import com.teena.assistant.core.ai.model.AiProviderType
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiProviderModule {

    @Provides
    @Singleton
    fun provideProvidersMap(
        geminiProvider: GeminiProvider,
        groqProvider: GroqProvider,
        openRouterProvider: OpenRouterProvider
    ): Map<AiProviderType, AiProvider> = mapOf(
        AiProviderType.GEMINI to geminiProvider,
        AiProviderType.GROQ to groqProvider,
        AiProviderType.OPENROUTER to openRouterProvider
    )
}
