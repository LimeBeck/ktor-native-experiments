package utils

sealed interface FileChangeEvent {
    val filepath: String
    val isDir: Boolean

    data class Created(
        override val filepath: String,
        override val isDir: Boolean
    ): FileChangeEvent

    data class Deleted(
        override val filepath: String,
        override val isDir: Boolean
    ): FileChangeEvent

    data class Updated(
        override val filepath: String,
        override val isDir: Boolean
    ): FileChangeEvent

    data class MovedInside(
        override val filepath: String,
        override val isDir: Boolean,
        val oldFilePath: String
    ): FileChangeEvent

    data class MovedTo(
        override val filepath: String,
        override val isDir: Boolean,
    ): FileChangeEvent

    data class MovedFrom(
        override val filepath: String,
        override val isDir: Boolean,
    ): FileChangeEvent
}