package templating

import com.benasher44.uuid.uuid4
import dev.limebeck.sse.utils.SseEvent
import dev.limebeck.sse.utils.respondSse
import dev.limebeck.templateEngine.KoTeRenderer
import dev.limebeck.templateEngine.runtime.Resource
import dev.limebeck.templateEngine.runtime.ResourceLoader
import dev.limebeck.templateEngine.runtime.defaultLib.kote
import dev.limebeck.utils.appendSseReloadScript
import dev.limebeck.utils.asHtml
import dev.limebeck.utils.json.convertJson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okio.FileSystem
import okio.Path.Companion.toPath
import utils.*

fun Application.templatingModule() {
    val rootPath = "/home/lime/work/opensource/ktor-native-experiments/src/nativeMain/resources" //TODO: Change me

    val filesWatcher = FilesWatcher(rootPath)
    val renderer = KoTeRenderer(NativeResourceLoader(rootPath)) {
        mapOf(
            kote
        )
    }

    routing {
        route("static") {
            files(rootPath)
        }

        get("/kote") {
            val result = renderer.render(
                readFileToString(rootPath.toPath() / "index.kote"),
                mapOf()
            ).getValueOrNull() ?: "No answer :("
            call.respondText(result.appendSseReloadScript(), contentType = ContentType.Text.Html)
        }

        get("/sse") {
            val events = filesWatcher.asFlow().map {
                println("Got event $it")
                SseEvent(
                    data = renderer.render(
                        readFileToString(rootPath.toPath() / "index.kote"),
                        mapOf()
                    ).getValueOrNull() ?: "No answer :(",
                    event = "PageUpdated",
                    id = uuid4().toString()
                )
            }.produceIn(this)
            call.respondSse(events)
        }
    }
}