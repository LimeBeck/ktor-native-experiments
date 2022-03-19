import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import platform.posix.closedir
import platform.posix.opendir
import platform.posix.readdir
import utils.FilesWatcher
import utils.listDir
import utils.listDirRecursively
import kotlin.test.Test

class InotifyTest {

    @Test
    fun dirAccessTest() {
        val path = "/home/lime/work/tests/ktor-native/src/nativeMain/resources"
        val files = listDirRecursively(path)
        println(files.filter { it.name !in listOf(".", "..") })
    }

    @Test
    fun inotifyTest() {
        runBlocking {
            val watcher = FilesWatcher("/home/lime/work/tests/ktor-native/src/nativeMain/resources")
            try {
                println("Init watcher")
                watcher.asFlow().collect {
                    println("COLLECT: $it")
                }
            }
            finally {
                watcher.close()
            }
        }
    }
}