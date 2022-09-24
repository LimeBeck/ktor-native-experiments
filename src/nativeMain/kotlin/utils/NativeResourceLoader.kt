package utils

import dev.limebeck.templateEngine.runtime.Resource
import dev.limebeck.templateEngine.runtime.ResourceLoader
import io.ktor.http.*
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class NativeResourceLoader(private val rootPath: String) : ResourceLoader {
    override fun loadResource(identifier: String): Resource {
        val filePath = rootPath.toPath() / identifier
        if (!FileSystem.SYSTEM.exists(filePath)) {
            throw RuntimeException("<87ce8bff> Resource $identifier was not found")
        }
        println("<c596907d> Find resource $identifier by path $filePath")
        return NativeFileResource(identifier, filePath)
    }
}

class NativeFileResource(
    override val identifier: String,
    private val filePath: Path
) : Resource {
    override val content: ByteArray
        get() = FileSystem.SYSTEM.read(filePath) {
            readByteArray()
        }
    override val contentType: String
        get() = ContentType.fromFileExtension(
            identifier.split(".").last()
        ).selectDefault().contentType
}