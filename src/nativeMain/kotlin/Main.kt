import dev.limebeck.utils.asHtml
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.cinterop.staticCFunction
import platform.posix.SIGPIPE
import platform.posix.SIGTERM
import platform.posix.signal
import sse.sseModule
import templating.templatingModule

fun main() {
    signal(SIGPIPE, staticCFunction<Int, Unit> {
        println("Interrupt: $it")
    })
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
//        sseModule()
        templatingModule()
    }.start(wait = true).apply {
//        signal(SIGTERM, staticCFunction<Int, Unit> {
//            stop()
//        })
    } .addShutdownHook {
        println("Shutting down")
    }
}