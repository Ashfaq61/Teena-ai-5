package com.teena.assistant.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.ui.graphics.vector.ImageVector

enum class DestinationGroup(val title: String) {
    CORE("Core Assistant"),
    AGENTS("Agents & Tools"),
    RESEARCH("Research & Evolution"),
    SYSTEM("System & Operations")
}

enum class TeenaDestination(
    val route: String,
    val title: String,
    val shortTitle: String,
    val icon: ImageVector,
    val group: DestinationGroup,
    val targetPhase: Int,
    val summary: String
) {
    DASHBOARD(
        route = "dashboard",
        title = "Home Dashboard",
        shortTitle = "Home",
        icon = Icons.Rounded.Dashboard,
        group = DestinationGroup.CORE,
        targetPhase = 1,
        summary = "Real-time system health, active models, working agents, and build telemetry."
    ),
    CHAT(
        route = "chat",
        title = "AI Chat",
        shortTitle = "Chat",
        icon = Icons.Rounded.ChatBubbleOutline,
        group = DestinationGroup.CORE,
        targetPhase = 2,
        summary = "Context-aware multilingual assistant (Urdu, Roman Urdu, Hinglish, English) with failover."
    ),
    LIVE_VOICE(
        route = "live_voice",
        title = "Live Voice",
        shortTitle = "Voice",
        icon = Icons.Rounded.GraphicEq,
        group = DestinationGroup.CORE,
        targetPhase = 3,
        summary = "Gemini Live bidirectional audio-to-audio engine over WebSocket BidiGenerateContent."
    ),
    SCREEN_AUTOMATION(
        route = "screen_automation",
        title = "Screen Automation",
        shortTitle = "Screen",
        icon = Icons.Rounded.TouchApp,
        group = DestinationGroup.CORE,
        targetPhase = 5,
        summary = "Verified AccessibilityService screen vision, UI automation, and social workflows."
    ),
    AGENTS(
        route = "agents",
        title = "AI Agents",
        shortTitle = "Agents",
        icon = Icons.Rounded.Psychology,
        group = DestinationGroup.AGENTS,
        targetPhase = 7,
        summary = "17 autonomous agents with heartbeats, least-privilege scopes, and Room state."
    ),
    AGENT_COLLABORATION(
        route = "agent_collaboration",
        title = "Agent Collaboration",
        shortTitle = "Collab",
        icon = Icons.Rounded.Hub,
        group = DestinationGroup.AGENTS,
        targetPhase = 7,
        summary = "Typed internal message bus history, task assignments, and verified knowledge exchange."
    ),
    MARKETPLACE(
        route = "marketplace",
        title = "Tool Marketplace",
        shortTitle = "Market",
        icon = Icons.Rounded.Extension,
        group = DestinationGroup.AGENTS,
        targetPhase = 7,
        summary = "Internal verified tool and agent catalog with compatibility, license, and security checks."
    ),
    RESEARCH(
        route = "research",
        title = "Research Center",
        shortTitle = "Research",
        icon = Icons.Rounded.Science,
        group = DestinationGroup.RESEARCH,
        targetPhase = 8,
        summary = "Autonomous GitHub, Hugging Face, web, and document technical evaluations."
    ),
    SELF_IMPROVEMENT(
        route = "self_improvement",
        title = "Self-Improvement Center",
        shortTitle = "Evolve",
        icon = Icons.Rounded.AutoFixHigh,
        group = DestinationGroup.RESEARCH,
        targetPhase = 9,
        summary = "Sandbox self-coding pipeline, automated testing, security scan, and user-approved upgrades."
    ),
    API_MANAGER(
        route = "api_manager",
        title = "API Manager",
        shortTitle = "APIs",
        icon = Icons.Rounded.Key,
        group = DestinationGroup.SYSTEM,
        targetPhase = 2,
        summary = "Encrypted multi-key provider slots (Gemini, Groq, OpenRouter) and ModelRegistry auto-detect."
    ),
    MEMORY_HISTORY(
        route = "memory_history",
        title = "Memory & History",
        shortTitle = "Memory",
        icon = Icons.Rounded.History,
        group = DestinationGroup.RESEARCH,
        targetPhase = 8,
        summary = "Long-term embeddings memory, searchable logs, SAF files, and encrypted backup/restore."
    ),
    APK_BUILD(
        route = "apk_build",
        title = "APK Build Center",
        shortTitle = "Builds",
        icon = Icons.Rounded.Build,
        group = DestinationGroup.SYSTEM,
        targetPhase = 1,
        summary = "GitHub Actions signed APK pipeline, SHA-256 verification, release history, and updates."
    ),
    NOTIFICATIONS(
        route = "notifications",
        title = "Notifications",
        shortTitle = "Alerts",
        icon = Icons.Rounded.Notifications,
        group = DestinationGroup.SYSTEM,
        targetPhase = 6,
        summary = "NotificationListenerService triage, reminders, scheduled jobs, and build alerts."
    ),
    PERMISSIONS(
        route = "permissions",
        title = "Permissions",
        shortTitle = "Perms",
        icon = Icons.Rounded.Security,
        group = DestinationGroup.SYSTEM,
        targetPhase = 4,
        summary = "Runtime grants, Android 13+ restricted settings guide, and ColorOS battery/autostart links."
    ),
    SETTINGS(
        route = "settings",
        title = "Settings",
        shortTitle = "Settings",
        icon = Icons.Rounded.Settings,
        group = DestinationGroup.SYSTEM,
        targetPhase = 4,
        summary = "Voice preferences, confirmation tiers, privacy controls, and background limits."
    ),
    ABOUT(
        route = "about",
        title = "About",
        shortTitle = "About",
        icon = Icons.Rounded.Info,
        group = DestinationGroup.SYSTEM,
        targetPhase = 1,
        summary = "Application identity, build metadata, signing policy, and Oppo F15 environment details."
    );

    companion object {
        val bottomNavDestinations: List<TeenaDestination> = listOf(
            DASHBOARD,
            CHAT,
            LIVE_VOICE,
            AGENTS,
            API_MANAGER
        )

        fun fromRoute(route: String?): TeenaDestination =
            entries.firstOrNull { it.route == route } ?: DASHBOARD
    }
}
