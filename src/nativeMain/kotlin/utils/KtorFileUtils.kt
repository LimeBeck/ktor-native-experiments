package utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.errors.*
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

private const val pathParameterName = "static-content-path-parameter"

fun Route.files(folder: String) {
    val path = folder.toPath()
    if (!FileSystem.SYSTEM.metadata(path).isDirectory) {
        throw PosixException.InvalidArgumentException("<965b1da5> \"$folder\" must point to a dir")
    }

    get("{$pathParameterName...}") {
        val relativePath = call.parameters
            .getAll(pathParameterName)
            ?.joinToString("/")
            ?.toPath(normalize = true) ?: return@get
        val file = path / relativePath
        if (FileSystem.SYSTEM.exists(file))
            call.respondStaticFile(file)
        else
            throw NotFoundException()
    }
}

internal fun List<ContentType>.selectDefault(): ContentType {
    val contentType = firstOrNull() ?: ContentType.Application.OctetStream
    return when {
        contentType.contentType == "text" && contentType.charset() == null -> contentType.withCharset(Charsets.UTF_8)
        else -> contentType
    }
}

suspend fun ApplicationCall.respondStaticFile(file: Path) {
    val contentType = ContentType.fromFileExtension(file.name.split(".").last()).selectDefault()
    respondBytes(contentType = contentType) { FileSystem.SYSTEM.read(file) { readByteArray() } }
}