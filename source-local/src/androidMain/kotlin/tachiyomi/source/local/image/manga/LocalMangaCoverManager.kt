package tachiyomi.source.local.image.manga

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.storage.DiskUtil
import tachiyomi.core.util.system.ImageUtil
import tachiyomi.source.local.io.manga.LocalMangaSourceFileSystem
import java.io.File
import java.io.InputStream

private const val DEFAULT_COVER_NAME = "cover.jpg"

actual class LocalMangaCoverManager(
    private val context: Context,
    private val fileSystem: LocalMangaSourceFileSystem,
) {

    actual fun find(mangaUrl: String): File? {
        return fileSystem.getFilesInMangaDirectory(mangaUrl)
            // Get all file whose names start with 'cover'
            .filter { it.isFile && it.nameWithoutExtension.equals("cover", ignoreCase = true) }
            // Get the first actual image
            .firstOrNull {
                ImageUtil.isImage(it.name) { it.inputStream() }
            }
    }

    actual fun update(manga: SManga, inputStream: InputStream): File? {
        val directory = fileSystem.getMangaDirectory(manga.url)
        if (directory == null) {
            inputStream.close()
            return null
        }

        var targetFile = find(manga.url)
        if (targetFile == null) {
            targetFile = File(directory.absolutePath, DEFAULT_COVER_NAME)
            targetFile.createNewFile()
        }

        // It might not exist at this point
        targetFile.parentFile?.mkdirs()
        inputStream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        DiskUtil.createNoMediaFile(UniFile.fromFile(directory), context)

        manga.thumbnail_url = targetFile.absolutePath
        return targetFile
    }
}
