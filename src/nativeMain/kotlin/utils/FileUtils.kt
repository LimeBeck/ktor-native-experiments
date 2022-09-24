package utils

import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKStringFromUtf8
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.closedir
import platform.posix.opendir
import platform.posix.readdir

data class File(
    val name: String,
    val path: String,
    val isDir: Boolean
)

fun listDir(path: String): List<File> {
    val files = mutableListOf<File>()
    memScoped {
        val directory = opendir(path)
        try {
            if (directory != null) {
                var ep = readdir(directory)
                while (ep != null) {
                    val file = ep.pointed
                    val name = file.d_name.toKStringFromUtf8()
                    files.add(File(name, "$path/$name", file.d_type == 4.toUByte()))
                    ep = readdir(directory)
                }
            }
        } finally {
            closedir(directory)
        }
    }
    return files
}

fun listDirRecursively(path: String): List<File> {
    val dirs = listDir(path).filter { it.name !in listOf(".", "..") }
    return dirs + dirs.filter { it.isDir }.flatMap { listDirRecursively(it.path) }
}

fun readFileToString(path: Path) = FileSystem.SYSTEM.read(path) {
    readUtf8()
}