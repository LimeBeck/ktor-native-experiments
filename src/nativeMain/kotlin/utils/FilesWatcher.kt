package utils

import dev.limebeck.utils.allSame
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import platform.linux.inotify_add_watch
import platform.linux.inotify_event
import platform.linux.inotify_init
import platform.linux.inotify_rm_watch
import platform.posix.close
import platform.posix.read


class FilesWatcher(
    val path: String,
    private val isDebug: Boolean = false
) {
    private val channel = MutableSharedFlow<FileChangeEvent>()
    fun asFlow() = channel.asSharedFlow()

    private val coroutineScope = CoroutineScope(Job())

    init {
        coroutineScope.launch {
            watch()
        }
    }

    fun close() {
        coroutineScope.cancel()
    }

    private data class EventInternal(
        val name: String,
        val path: String,
        val type: EventType,
        val cookie: UInt,
        val isDir: Boolean
    )

    private fun inotify_event.debug() =
        "$cookie ${EventType.fromCode(mask)} ${if (isDir) "DIR" else ""} ${name.toKStringFromUtf8()}"

    private fun debug(block: () -> Unit) {
        if(isDebug){
            block()
        }
    }

    private suspend fun watch() {
        memScoped {
            val buffLen = 1 * sizeOf<inotify_event>() * 100
            val fd = inotify_init()
            val dirsInside = listDirRecursively(path).filter { it.isDir }
            val wds = (dirsInside.map { it.path } + path).associateBy {
                debug {
                    println("<2fb3891b> Init watch for $it")
                }
                inotify_add_watch(fd, it, EventType.ALL)
            }
            channel.onCompletion {
                debug {
                    println("closed")
                }
                wds.forEach { wd ->
                    inotify_rm_watch(fd, wd.key)
                }
                close(fd)
            }
            while (true) {
                memScoped {
                    val buffer = allocArray<inotify_event>(buffLen.toInt())
                    val len = read(fd, buffer, buffLen.toULong())
                    val events = (0 until len).mapNotNull {
                        val event = buffer[it]
                        if (event.len != 0.toUInt()) {
                            debug {
                                println("INTERNAL: ${event.debug()}")
                            }
                            EventInternal(
                                name = event.name.toKStringFromUtf8(),
                                path = wds[event.wd]!!,
                                cookie = event.cookie,
                                type = EventType.fromCode(event.mask),
                                isDir = event.isDir
                            )
                        } else {
                            null
                        }
                    }.fold(mutableMapOf<UInt, List<EventInternal>>()) { acc, event ->
                        acc[event.cookie] = (acc[event.cookie] ?: listOf()) + listOf(event)
                        acc
                    }.mapNotNull {
                        val events = it.value
                        when (events.size) {
                            2 -> {
                                when {
                                    events.all { it.type in listOf(EventType.MOVED_FROM, EventType.MOVED_TO) } -> {
                                        val from = events.find { it.type == EventType.MOVED_FROM }!!
                                        val to = events.find { it.type == EventType.MOVED_TO }!!
                                        FileChangeEvent.MovedInside(
                                            filepath = to.path,
                                            oldFilePath = from.path,
                                            isDir = to.isDir,
                                        )
                                    }
                                    events.allSame { it.type } && events.allSame { it.path } -> {
                                        val event = events.first()
                                        when (event.type) {
                                            EventType.CREATED -> FileChangeEvent.Created(
                                                filepath = event.path,
                                                isDir = event.isDir
                                            )
                                            EventType.UPDATED -> FileChangeEvent.Updated(
                                                filepath = event.path,
                                                isDir = event.isDir
                                            )
                                            EventType.DELETED -> FileChangeEvent.Deleted(
                                                filepath = event.path,
                                                isDir = event.isDir
                                            )
                                            EventType.MOVED_FROM -> FileChangeEvent.MovedFrom(
                                                filepath = event.path,
                                                isDir = event.isDir
                                            )
                                            EventType.MOVED_TO -> FileChangeEvent.MovedTo(
                                                filepath = event.path,
                                                isDir = event.isDir
                                            )
                                        }
                                    }
                                    else -> {
                                        println(events)
                                        throw RuntimeException("<532e03d8>")
                                    } //TODO Own exception
                                }
                            }
                            1 -> {
                                val event = events.first()
                                when (event.type) {
                                    EventType.CREATED -> FileChangeEvent.Created(
                                        filepath = event.path,
                                        isDir = event.isDir
                                    )
                                    EventType.UPDATED -> FileChangeEvent.Updated(
                                        filepath = event.path,
                                        isDir = event.isDir
                                    )
                                    EventType.DELETED -> FileChangeEvent.Deleted(
                                        filepath = event.path,
                                        isDir = event.isDir
                                    )
                                    EventType.MOVED_FROM -> FileChangeEvent.MovedFrom(
                                        filepath = event.path,
                                        isDir = event.isDir
                                    )
                                    EventType.MOVED_TO -> FileChangeEvent.MovedTo(
                                        filepath = event.path,
                                        isDir = event.isDir
                                    )
                                }
                            }
                            else -> {
                                null
                            }
                        }
                    }
                    events.forEach { channel.emit(it) }
                }
            }
        }
    }
}
