package utils

import platform.linux.*

enum class EventType(vararg codes: Int) {
    CREATED(IN_CREATE),
    UPDATED(IN_MODIFY),
    DELETED(IN_DELETE, IN_DELETE_SELF),
    MOVED_FROM(IN_MOVED_FROM),
    MOVED_TO(IN_MOVED_TO);

    val codes: List<UInt> = codes.map { it.toUInt() }
    internal val asUInt32: UInt
        get() = codes.reduce { l, r -> l.or(r) }.toUInt()

    companion object {
        fun fromCode(code: UInt): EventType =
            values().find { it.codes.any { code.and(it) != 0.toUInt() } }
                ?: throw RuntimeException("<cb78e435> EventType for code is not found $code")

        fun fromCode(code: Int) = fromCode(code.toUInt())
        val ALL = values().toList().asUInt32
    }
}

internal val Collection<EventType>.asUInt32: UInt
    get() = map { it.asUInt32 }.reduce { l, r -> l.or(r) }.toUInt()

val inotify_event.isDir: Boolean
    get() = mask.and(IN_ISDIR.toUInt()) != 0.toUInt()
