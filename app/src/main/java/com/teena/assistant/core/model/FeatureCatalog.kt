package com.teena.assistant.core.model

object FeatureCatalog {

    val initialFeatures: List<FeatureSpec> = listOf(
        FeatureSpec(
            id = "scaffold_shell",
            title = "Compose App Shell & 16-Section Navigation",
            sectionRoute = "dashboard",
            phase = 1,
            status = FeatureStatus.IMPLEMENTED_NOT_DEVICE_VERIFIED,
            details = "Gradle Kotlin DSL scaffold, Material 3 dark RTL-aware UI, and 16 section routes built. Awaiting real-device verification on Oppo F15."
        ),
        FeatureSpec(
            id = "core_persistence_di",
            title = "Hilt DI, Room v2 Database & WorkManager",
            sectionRoute = "memory_history",
            phase = 1,
            status = FeatureStatus.IMPLEMENTED_NOT_DEVICE_VERIFIED,
            details = "Room v2 schema (MIGRATION_1_2) storing keys metadata, discovered models, chat history, embeddings memory, voice transcripts, and usage."
        ),
        FeatureSpec(
            id = "ai_brain_model_registry",
            title = "AI Brain, Encrypted API Manager & ModelRegistry Auto-Detect",
            sectionRoute = "api_manager",
            phase = 2,
            status = FeatureStatus.IMPLEMENTED_NOT_DEVICE_VERIFIED,
            details = "EncryptedSharedPreferences key slots (Gemini, Groq, OpenRouter), live model discovery, capability probing, ranking, 24h WorkManager refresh, manual overrides, and failover."
        ),
        FeatureSpec(
            id = "ai_chat_multilang",
            title = "Multi-Language Streaming AI Chat & Long-Term Memory",
            sectionRoute = "chat",
            phase = 2,
            status = FeatureStatus.IMPLEMENTED_NOT_DEVICE_VERIFIED,
            details = "SSE streaming chat across Gemini/Groq/OpenRouter with automatic failover, Urdu RTL rendering, Roman Urdu/Hinglish/English support, and Room embeddings memory."
        ),
        FeatureSpec(
            id = "gemini_live_voice",
            title = "Gemini Live Bidirectional Voice (BidiGenerateContent)",
            sectionRoute = "live_voice",
            phase = 3,
            status = FeatureStatus.IMPLEMENTED_NOT_DEVICE_VERIFIED,
            details = "OkHttp WebSocket BidiGenerateContent, 16kHz AudioRecord + AEC/NS/AGC, 24kHz AudioTrack + barge-in flush, prebuilt voices, captions, tool calling, resumption/goAway, and mic foreground service."
        ),
        FeatureSpec(
            id = "permissions_onboarding_coloros",
            title = "First-Run Onboarding, Permissions, ColorOS Links & Emergency Stop",
            sectionRoute = "permissions",
            phase = 4,
            status = FeatureStatus.IMPLEMENTED_NOT_DEVICE_VERIFIED,
            details = "Interactive permission walkthrough, live runtime checks, Battery Optimization & ColorOS Auto-Start deep links, Android 13+ Restricted Settings steps, and global Emergency Stop."
        ),
        FeatureSpec(
            id = "screen_vision_automation",
            title = "Accessibility Screen Vision & Social Automation",
            sectionRoute = "screen_automation",
            phase = 5,
            status = FeatureStatus.NOT_IMPLEMENTED,
            details = "AccessibilityService hierarchy/screenshot verification loop, FLAG_SECURE respect, and stop controls scheduled for Phase 5."
        ),
        FeatureSpec(
            id = "phone_camera_notifications",
            title = "Phone/Telecom, CameraX & Notification Listener",
            sectionRoute = "notifications",
            phase = 6,
            status = FeatureStatus.NOT_IMPLEMENTED,
            details = "TelecomManager/InCallService/CallScreeningService, CameraX, MediaSession, and NotificationListenerService scheduled for Phase 6."
        ),
        FeatureSpec(
            id = "multi_agent_network",
            title = "17-Agent Autonomous Network & Collaboration Bus",
            sectionRoute = "agents",
            phase = 7,
            status = FeatureStatus.NOT_IMPLEMENTED,
            details = "Typed internal message bus, heartbeats, least-privilege scopes, and Room persistence scheduled for Phase 7."
        ),
        FeatureSpec(
            id = "agent_collaboration_view",
            title = "Agent Collaboration Monitor",
            sectionRoute = "agent_collaboration",
            phase = 7,
            status = FeatureStatus.NOT_IMPLEMENTED,
            details = "Live task-assignment graph and verified inter-agent logs scheduled for Phase 7."
        ),
        FeatureSpec(
            id = "tool_marketplace",
            title = "Internal Tool & Agent Marketplace",
            sectionRoute = "marketplace",
            phase = 7,
            status = FeatureStatus.NOT_IMPLEMENTED,
            details = "Compatibility checks, license/security scan metadata, and verified tool registry scheduled for Phase 7."
        ),
        FeatureSpec(
            id = "research_center",
            title = "GitHub, Hugging Face & Web Research Center",
            sectionRoute = "research",
            phase = 8,
            status = FeatureStatus.NOT_IMPLEMENTED,
            details = "Real API repo/model technical evaluation, web/PDF extraction, and citations scheduled for Phase 8."
        ),
        FeatureSpec(
            id = "files_knowledge_maps",
            title = "SAF Files, Knowledge Base, Encrypted Backup & Maps",
            sectionRoute = "settings",
            phase = 8,
            status = FeatureStatus.NOT_IMPLEMENTED,
            details = "Storage Access Framework, OCR, encrypted export/import, and location/geofencing scheduled for Phase 8."
        ),
        FeatureSpec(
            id = "self_improvement_engine",
            title = "Self-Coding & Self-Improvement Center",
            sectionRoute = "self_improvement",
            phase = 9,
            status = FeatureStatus.NOT_IMPLEMENTED,
            details = "Sandbox PR workflow, automated test/security gates, and user-approved APK rollout scheduled for Phase 9."
        ),
        FeatureSpec(
            id = "apk_build_cicd",
            title = "GitHub Actions Signed APK Build & Release Center",
            sectionRoute = "apk_build",
            phase = 1,
            status = FeatureStatus.PARTIAL,
            details = "CI/CD workflow for assembleDebug, unit tests, lint, signed/temp-signed APK, SHA-256 checksums, and GitHub Release active; in-app release tracker scheduled for Phase 9."
        )
    )
}
