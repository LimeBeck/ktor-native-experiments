import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.utils.io.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

fun Throwable.asHtml(): String = """
    <html>
        <head>
            <link href="https://cdn.jsdelivr.net/npm/water.css@2/out/water.css" rel="stylesheet"/>
            <title>Rendering Error</title>
        </head>
        <body>
            <h1>ERROR</h1>
            <h3>${message ?: this.toString()}</h3>
            <p>Additional error info</p>
            <pre>
                <code>
                    ${stackTraceToString()}
                </code>
            </pre>
        </body>
    </html>
""".trimIndent()

fun String.appendSseReloadScript() = this + """
        <script type="text/javascript">
            var source = new EventSource('/sse');
            source.addEventListener('PageUpdated', function(e) {
                document.documentElement.innerHTML = e.data
            }, false);
        </script>
""".trimIndent()

data class SseEvent(val data: String, val event: String? = null, val id: String? = null)

@OptIn(ExperimentalTime::class)
fun tickerFlow(period: Duration, initialDelay: Duration = Duration.ZERO) = flow {
    delay(initialDelay)
    while (true) {
        emit(Unit)
        delay(period)
    }
}

suspend fun ApplicationCall.respondSse(events: ReceiveChannel<SseEvent>) =
    coroutineScope {
        response.cacheControl(CacheControl.NoCache(null))
        respondBytesWriter(contentType = ContentType.Text.EventStream) {
            events.consumeEach { event ->
                if (isClosedForWrite) {
                    println("<b785289> Closed socked. Cancel execution")
                    this@coroutineScope.cancel()
                } else {
                    println("<0c08a270> Send event $event")
                    try {
                        if (event.id != null) {
                            writeStringUtf8("id: ${event.id}\n")
                        }
                        if (event.event != null) {
                            writeStringUtf8("event: ${event.event}\n")
                        }
                        for (dataLine in event.data.lines()) {
                            writeStringUtf8("data: $dataLine\n")
                        }
                        writeStringUtf8("\n")
                        flush()
                    } catch (e: IOException) {
                        println("<b785289> Closed socked. Cancel execution")
                        this@coroutineScope.cancel()
                    }
                }
            }
        }
    }
