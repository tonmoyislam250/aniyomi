package tachiyomi.domain.library.manga.model

import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.FlagWithMask
import tachiyomi.domain.library.model.plus

data class MangaLibrarySort(
    val type: Type,
    val direction: Direction,
) : FlagWithMask {

    override val flag: Long
        get() = type + direction

    override val mask: Long
        get() = type.mask or direction.mask

    val isAscending: Boolean
        get() = direction == Direction.Ascending

    sealed class Type(
        override val flag: Long,
    ) : FlagWithMask {

        override val mask: Long = 0b00111100L

        object Alphabetical : Type(0b00000000)
        object LastRead : Type(0b00000100)
        object LastUpdate : Type(0b00001000)
        object UnreadCount : Type(0b00001100)
        object TotalChapters : Type(0b00010000)
        object LatestChapter : Type(0b00010100)
        object ChapterFetchDate : Type(0b00011000)
        object DateAdded : Type(0b00011100)

        companion object {
            fun valueOf(flag: Long): Type {
                return types.find { type -> type.flag == flag and type.mask } ?: default.type
            }
        }
    }

    sealed class Direction(
        override val flag: Long,
    ) : FlagWithMask {

        override val mask: Long = 0b01000000L

        object Ascending : Direction(0b01000000)
        object Descending : Direction(0b00000000)

        companion object {
            fun valueOf(flag: Long): Direction {
                return directions.find { direction -> direction.flag == flag and direction.mask } ?: default.direction
            }
        }
    }

    object Serializer {
        fun deserialize(serialized: String): MangaLibrarySort {
            return Companion.deserialize(serialized)
        }

        fun serialize(value: MangaLibrarySort): String {
            return value.serialize()
        }
    }

    companion object {
        val types by lazy {
            setOf(
                Type.Alphabetical,
                Type.LastRead,
                Type.LastUpdate,
                Type.UnreadCount,
                Type.TotalChapters,
                Type.LatestChapter,
                Type.ChapterFetchDate,
                Type.DateAdded,
            )
        }
        val directions by lazy { setOf(Direction.Ascending, Direction.Descending) }
        val default = MangaLibrarySort(Type.Alphabetical, Direction.Ascending)

        fun valueOf(flag: Long?): MangaLibrarySort {
            if (flag == null) return default
            return MangaLibrarySort(
                Type.valueOf(flag),
                Direction.valueOf(flag),
            )
        }

        fun deserialize(serialized: String): MangaLibrarySort {
            if (serialized.isEmpty()) return default
            return try {
                val values = serialized.split(",")
                val type = when (values[0]) {
                    "ALPHABETICAL" -> Type.Alphabetical
                    "LAST_READ" -> Type.LastRead
                    "LAST_MANGA_UPDATE" -> Type.LastUpdate
                    "UNREAD_COUNT" -> Type.UnreadCount
                    "TOTAL_CHAPTERS" -> Type.TotalChapters
                    "LATEST_CHAPTER" -> Type.LatestChapter
                    "CHAPTER_FETCH_DATE" -> Type.ChapterFetchDate
                    "DATE_ADDED" -> Type.DateAdded
                    else -> Type.Alphabetical
                }
                val ascending = if (values[1] == "ASCENDING") Direction.Ascending else Direction.Descending
                MangaLibrarySort(type, ascending)
            } catch (e: Exception) {
                default
            }
        }
    }

    fun serialize(): String {
        val type = when (type) {
            Type.Alphabetical -> "ALPHABETICAL"
            Type.LastRead -> "LAST_READ"
            Type.LastUpdate -> "LAST_MANGA_UPDATE"
            Type.UnreadCount -> "UNREAD_COUNT"
            Type.TotalChapters -> "TOTAL_CHAPTERS"
            Type.LatestChapter -> "LATEST_CHAPTER"
            Type.ChapterFetchDate -> "CHAPTER_FETCH_DATE"
            Type.DateAdded -> "DATE_ADDED"
        }
        val direction = if (direction == Direction.Ascending) "ASCENDING" else "DESCENDING"
        return "$type,$direction"
    }
}

val Category?.sort: MangaLibrarySort
    get() = MangaLibrarySort.valueOf(this?.flags)
