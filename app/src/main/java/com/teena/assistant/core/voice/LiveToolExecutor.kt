package com.teena.assistant.core.voice

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.teena.assistant.core.ai.memory.MemoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveToolExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memoryRepository: MemoryRepository,
    private val json: Json
) {

    fun declarationsJson(): JsonArray = buildJsonArray {
        add(buildJsonObject {
            put("name", "get_current_time")
            put("description", "Returns the current local date and time on the user's device.")
        })
        add(buildJsonObject {
            put("name", "get_device_battery")
            put("description", "Returns the current device battery percentage and charging status.")
        })
        add(buildJsonObject {
            put("name", "search_user_memory")
            put("description", "Searches TEENA's long-term Room memory for user facts or preferences.")
            put("parameters", buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("query", buildJsonObject {
                        put("type", "string")
                        put("description", "Search query")
                    })
                })
                put("required", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive("query")) })
            })
        })
        add(buildJsonObject {
            put("name", "save_user_memory")
            put("description", "Saves a short fact the user explicitly asked TEENA to remember.")
            put("parameters", buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("fact", buildJsonObject {
                        put("type", "string")
                        put("description", "The fact to remember")
                    })
                })
                put("required", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive("fact")) })
            })
        })
        add(buildJsonObject {
            put("name", "open_installed_app")
            put("description", "Opens an installed Android app by package name (requires user confirmation).")
            put("parameters", buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("package_name", buildJsonObject {
                        put("type", "string")
                        put("description", "Android package name, e.g. com.whatsapp")
                    })
                })
                put("required", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive("package_name")) })
            })
        })
    }

    fun requiresUserConfirmation(toolName: String): Boolean =
        toolName == "open_installed_app"

    suspend fun execute(toolName: String, rawArgsJson: String): JsonObject {
        val args = runCatching { json.parseToJsonElement(rawArgsJson).jsonObject }.getOrNull()
        return when (toolName) {
            "get_current_time" -> {
                val formatted = SimpleDateFormat("EEEE, yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
                    .format(Date())
                buildJsonObject { put("current_time", formatted) }
            }
            "get_device_battery" -> {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                buildJsonObject {
                    put("battery_percent", pct)
                    put("is_charging", bm.isCharging)
                }
            }
            "search_user_memory" -> {
                val query = args?.get("query")?.jsonPrimitive?.contentOrNull.orEmpty()
                val matches = memoryRepository.retrieveRelevantMemories(query, limit = 5)
                buildJsonObject {
                    put("matches", buildJsonArray {
                        matches.forEach { add(kotlinx.serialization.json.JsonPrimitive(it.content)) }
                    })
                }
            }
            "save_user_memory" -> {
                val fact = args?.get("fact")?.jsonPrimitive?.contentOrNull.orEmpty()
                if (fact.isBlank()) {
                    buildJsonObject { put("status", "error"); put("reason", "Empty fact") }
                } else {
                    memoryRepository.saveMemory(fact, category = "LIVE_VOICE")
                    buildJsonObject { put("status", "saved"); put("fact", fact) }
                }
            }
            "open_installed_app" -> {
                val pkg = args?.get("package_name")?.jsonPrimitive?.contentOrNull.orEmpty()
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent == null) {
                    buildJsonObject {
                        put("status", "failed")
                        put("reason", "App $pkg is not installed or has no launchable activity.")
                    }
                } else {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    buildJsonObject { put("status", "opened"); put("package_name", pkg) }
                }
            }
            else -> buildJsonObject {
                put("status", "error")
                put("reason", "Unknown tool: $toolName")
            }
        }
    }
}
