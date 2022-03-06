import dev.limebeck.templateEngine.KoTeRenderer
import dev.limebeck.templateEngine.runtime.defaultLib.kote
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(CIO, port = 8080) {
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
            }
        }
        routing {
            get("/") {
                try{
                    val renderer = KoTeRenderer {
                        mapOf(kote)
                    }
                    val result = renderer.render("""{{ kote() }}""", null, mapOf()).getValueOrNull() ?: "No answer :("
                    call.respondText(result)
                } catch (e: Throwable){
                    call.respondText(e.stackTraceToString())
                }
            }
            get("/error") {
                throw RuntimeException("<417cf101> Error")
            }
        }
    }.start(wait = true)
}