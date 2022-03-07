import com.benasher44.uuid.uuid4
import dev.limebeck.json.convertJson
import dev.limebeck.templateEngine.KoTeRenderer
import dev.limebeck.templateEngine.runtime.defaultLib.kote
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import platform.posix.SIGINT
import platform.posix.SIGPIPE
import platform.posix.signal
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(DelicateCoroutinesApi::class, FlowPreview::class, ExperimentalTime::class, InternalCoroutinesApi::class)
fun main() {
    signal(SIGPIPE, staticCFunction<Int, Unit> {
        println("Interrupt: $it")
    })
    try {
        embeddedServer(CIO, port = 8080) {
            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    call.respondText(
                        text = cause.asHtml(),
                        contentType = ContentType.Text.Html,
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
            routing {
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
                get("/") {
                    val params = call.parameters.entries().associate { it.key to it.value }
                    val renderer = KoTeRenderer {
                        mapOf(
                            kote,
                        )
                    }
                    println(params)
                    val result = renderer.render(
                        template = """
                            {{ params }}
                        """.trimIndent(),
                        resources = null,
                        data = mapOf("params" to params)
                    ).getValueOrNull() ?: "No answer :("
                    call.respondText(result.appendSseReloadScript(), contentType = ContentType.Text.Html)
                }

                get("/sse") {
                    val updatedStateFlow = MutableStateFlow<String>("Initial")
                    launch {
                        val ticker = tickerFlow(1.seconds)
                        ticker.collect {
                            val uuid = uuid4().toString()
                            println("Produced $uuid")
                            try {
                                updatedStateFlow.emit(uuid)
                            } catch (e: Throwable) {
                                updatedStateFlow.emit(e.asHtml())
                            }
                        }
                    }

                    val events = updatedStateFlow
                        .drop(1)
                        .map {
                            println("Got event $it")
                            SseEvent(data = it, event = "PageUpdated", id = it)
                        }
                        .produceIn(this)

                    call.respondSse(events)
                }

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
            }
        }.start(wait = true).addShutdownHook {
            println("Shutting down")
        }
    } catch (e: Throwable) {
        e.printStackTrace()
    }
    println("Error")
}