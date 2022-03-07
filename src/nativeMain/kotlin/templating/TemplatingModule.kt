package templating

import dev.limebeck.templateEngine.KoTeRenderer
import dev.limebeck.templateEngine.runtime.defaultLib.kote
import dev.limebeck.utils.asHtml
import dev.limebeck.utils.json.convertJson
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

fun Application.templatingModule() {
    routing {
        get("/kote") {
            val renderer = KoTeRenderer {
                mapOf(kote)
            }
            val result = renderer.render("""{{ kote() }}""", null, mapOf()).getValueOrNull() ?: "No answer :("
            call.respondText(result)
        }
        get("/error") {
            throw RuntimeException("<417cf101> Error")
        }
        post("/") {
            println(call.request.headers.toMap())
            try {
                val rawParams = call.receiveText()
                val params = Json.decodeFromString<JsonObject>(rawParams).convertJson()
                val renderer = KoTeRenderer {
                    mapOf(
                        kote,
                    )
                }
                println("<3426793f> $params")
                val result = renderer.render(
                    template = """
                            {{ params }}
                        """.trimIndent(),
                    resources = null,
                    data = mapOf("params" to params)
                ).getValueOrNull()
                    ?: "No answer :("
                call.respondText(result)
            } catch (e: Throwable) {
                call.respondText(e.asHtml())
            }
        }
    }
}