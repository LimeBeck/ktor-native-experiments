package sse

import com.benasher44.uuid.uuid4
import dev.limebeck.sse.utils.SseEvent
import dev.limebeck.sse.utils.respondSse
import dev.limebeck.templateEngine.KoTeRenderer
import dev.limebeck.templateEngine.runtime.defaultLib.kote
import dev.limebeck.utils.appendSseReloadScript
import dev.limebeck.utils.asHtml
import dev.limebeck.utils.tickerFlow
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(DelicateCoroutinesApi::class, FlowPreview::class, ExperimentalTime::class, InternalCoroutinesApi::class)
fun Application.sseModule() {
    routing {
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
            val counter = atomic(1)
            val events = updatedStateFlow
                .drop(1)
                .map {
                    println("Got event $it")
                    SseEvent(
                        data = counter.getAndIncrement().toString(),
                        event = "PageUpdated",
                        id = it
                    )
                }
                .produceIn(this)

            call.respondSse(events)
        }
    }
}