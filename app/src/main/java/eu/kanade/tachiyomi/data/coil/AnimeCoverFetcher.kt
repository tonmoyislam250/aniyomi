package eu.kanade.tachiyomi.data.coil

import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.network.HttpException
import coil.request.Options
import coil.request.Parameters
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.coil.AnimeCoverFetcher.Companion.USE_CUSTOM_COVER
import eu.kanade.tachiyomi.network.await
import logcat.LogPriority
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.http.HTTP_NOT_MODIFIED
import okio.Path.Companion.toOkioPath
import okio.Source
import okio.buffer
import okio.sink
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeCover
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import uy.kohesive.injekt.injectLazy
import java.io.File

/**
 * A [Fetcher] that fetches cover image for [Anime] object.
 *
 * It uses [Anime.thumbnail_url] if custom cover is not set by the user.
 * Disk caching for library items is handled by [AnimeCoverCache], otherwise
 * handled by Coil's [DiskCache].
 *
 * Available request parameter:
 * - [USE_CUSTOM_COVER]: Use custom cover if set by user, default is true
 */
class AnimeCoverFetcher(
    private val url: String?,
    private val isLibraryAnime: Boolean,
    private val options: Options,
    private val coverFileLazy: Lazy<File?>,
    private val customCoverFileLazy: Lazy<File>,
    private val diskCacheKeyLazy: Lazy<String>,
    private val sourceLazy: Lazy<AnimeHttpSource?>,
    private val callFactoryLazy: Lazy<Call.Factory>,
    private val diskCacheLazy: Lazy<DiskCache>,
) : Fetcher {

    private val diskCacheKey: String
        get() = diskCacheKeyLazy.value

    override suspend fun fetch(): FetchResult {
        // Use custom cover if exists
        val useCustomCover = options.parameters.value(USE_CUSTOM_COVER) ?: true
        if (useCustomCover) {
            val customCoverFile = customCoverFileLazy.value
            if (customCoverFile.exists()) {
                return fileLoader(customCoverFile)
            }
        }

        // diskCacheKey is thumbnail_url
        if (url == null) error("No cover specified")
        return when (getResourceType(url)) {
            Type.URL -> httpLoader()
            Type.File -> fileLoader(File(url.substringAfter("file://")))
            null -> error("Invalid image")
        }
    }

    private fun fileLoader(file: File): FetchResult {
        return SourceResult(
            source = ImageSource(file = file.toOkioPath(), diskCacheKey = diskCacheKey),
            mimeType = "image/*",
            dataSource = DataSource.DISK,
        )
    }

    private suspend fun httpLoader(): FetchResult {
        // Only cache separately if it's a library item
        val libraryCoverCacheFile = if (isLibraryAnime) {
            coverFileLazy.value ?: error("No cover specified")
        } else {
            null
        }
        if (libraryCoverCacheFile?.exists() == true && options.diskCachePolicy.readEnabled) {
            return fileLoader(libraryCoverCacheFile)
        }

        var snapshot = readFromDiskCache()
        try {
            // Fetch from disk cache
            if (snapshot != null) {
                val snapshotCoverCache = moveSnapshotToCoverCache(snapshot, libraryCoverCacheFile)
                if (snapshotCoverCache != null) {
                    // Read from cover cache after added to library
                    return fileLoader(snapshotCoverCache)
                }

                // Read from snapshot
                return SourceResult(
                    source = snapshot.toImageSource(),
                    mimeType = "image/*",
                    dataSource = DataSource.DISK,
                )
            }

            // Fetch from network
            val response = executeNetworkRequest()
            val responseBody = checkNotNull(response.body) { "Null response source" }
            try {
                // Read from cover cache after library manga cover updated
                val responseCoverCache = writeResponseToCoverCache(response, libraryCoverCacheFile)
                if (responseCoverCache != null) {
                    return fileLoader(responseCoverCache)
                }

                // Read from disk cache
                snapshot = writeToDiskCache(response)
                if (snapshot != null) {
                    return SourceResult(
                        source = snapshot.toImageSource(),
                        mimeType = "image/*",
                        dataSource = DataSource.NETWORK,
                    )
                }

                // Read from response if cache is unused or unusable
                return SourceResult(
                    source = ImageSource(source = responseBody.source(), context = options.context),
                    mimeType = "image/*",
                    dataSource = if (response.cacheResponse != null) DataSource.DISK else DataSource.NETWORK,
                )
            } catch (e: Exception) {
                responseBody.close()
                throw e
            }
        } catch (e: Exception) {
            snapshot?.close()
            throw e
        }
    }

    private suspend fun executeNetworkRequest(): Response {
        val client = sourceLazy.value?.client ?: callFactoryLazy.value
        val response = client.newCall(newRequest()).await()
        if (!response.isSuccessful && response.code != HTTP_NOT_MODIFIED) {
            response.close()
            throw HttpException(response)
        }
        return response
    }

    private fun newRequest(): Request {
        val request = Request.Builder()
            .url(url!!)
            .headers(sourceLazy.value?.headers ?: options.headers)
            // Support attaching custom data to the network request.
            .tag(Parameters::class.java, options.parameters)

        when {
            options.networkCachePolicy.readEnabled -> {
                // don't take up okhttp cache
                request.cacheControl(CACHE_CONTROL_NO_STORE)
            }
            else -> {
                // This causes the request to fail with a 504 Unsatisfiable Request.
                request.cacheControl(CACHE_CONTROL_NO_NETWORK_NO_CACHE)
            }
        }

        return request.build()
    }

    private fun moveSnapshotToCoverCache(snapshot: DiskCache.Snapshot, cacheFile: File?): File? {
        if (cacheFile == null) return null
        return try {
            diskCacheLazy.value.run {
                fileSystem.source(snapshot.data).use { input ->
                    writeSourceToCoverCache(input, cacheFile)
                }
                remove(diskCacheKey)
            }
            cacheFile.takeIf { it.exists() }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to write snapshot data to cover cache ${cacheFile.name}" }
            null
        }
    }

    private fun writeResponseToCoverCache(response: Response, cacheFile: File?): File? {
        if (cacheFile == null || !options.diskCachePolicy.writeEnabled) return null
        return try {
            response.peekBody(Long.MAX_VALUE).source().use { input ->
                writeSourceToCoverCache(input, cacheFile)
            }
            cacheFile.takeIf { it.exists() }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to write response data to cover cache ${cacheFile.name}" }
            null
        }
    }

    private fun writeSourceToCoverCache(input: Source, cacheFile: File) {
        cacheFile.parentFile?.mkdirs()
        cacheFile.delete()
        try {
            cacheFile.sink().buffer().use { output ->
                output.writeAll(input)
            }
        } catch (e: Exception) {
            cacheFile.delete()
            throw e
        }
    }

    private fun readFromDiskCache(): DiskCache.Snapshot? {
        return if (options.diskCachePolicy.readEnabled) diskCacheLazy.value[diskCacheKey] else null
    }

    private fun writeToDiskCache(
        response: Response,
    ): DiskCache.Snapshot? {
        val editor = diskCacheLazy.value.edit(diskCacheKey) ?: return null
        try {
            diskCacheLazy.value.fileSystem.write(editor.data) {
                response.body.source().readAll(this)
            }
            return editor.commitAndGet()
        } catch (e: Exception) {
            try {
                editor.abort()
            } catch (ignored: Exception) {
            }
            throw e
        }
    }

    private fun DiskCache.Snapshot.toImageSource(): ImageSource {
        return ImageSource(file = data, diskCacheKey = diskCacheKey, closeable = this)
    }

    private fun getResourceType(cover: String?): Type? {
        return when {
            cover.isNullOrEmpty() -> null
            cover.startsWith("http", true) || cover.startsWith("Custom-", true) -> Type.URL
            cover.startsWith("/") || cover.startsWith("file://") -> Type.File
            else -> null
        }
    }

    private enum class Type {
        File, URL
    }

    class AnimeFactory(
        private val callFactoryLazy: Lazy<Call.Factory>,
        private val diskCacheLazy: Lazy<DiskCache>,
    ) : Fetcher.Factory<Anime> {

        private val coverCache: AnimeCoverCache by injectLazy()
        private val sourceManager: AnimeSourceManager by injectLazy()

        override fun create(data: Anime, options: Options, imageLoader: ImageLoader): Fetcher {
            return AnimeCoverFetcher(
                url = data.thumbnailUrl,
                isLibraryAnime = data.favorite,
                options = options,
                coverFileLazy = lazy { coverCache.getCoverFile(data.thumbnailUrl) },
                customCoverFileLazy = lazy { coverCache.getCustomCoverFile(data.id) },
                diskCacheKeyLazy = lazy { AnimeKeyer().key(data, options) },
                sourceLazy = lazy { sourceManager.get(data.source) as? AnimeHttpSource },
                callFactoryLazy = callFactoryLazy,
                diskCacheLazy = diskCacheLazy,
            )
        }
    }

    class AnimeCoverFactory(
        private val callFactoryLazy: Lazy<Call.Factory>,
        private val diskCacheLazy: Lazy<DiskCache>,
    ) : Fetcher.Factory<AnimeCover> {

        private val coverCache: AnimeCoverCache by injectLazy()
        private val sourceManager: AnimeSourceManager by injectLazy()

        override fun create(data: AnimeCover, options: Options, imageLoader: ImageLoader): Fetcher {
            return AnimeCoverFetcher(
                url = data.url,
                isLibraryAnime = data.isAnimeFavorite,
                options = options,
                coverFileLazy = lazy { coverCache.getCoverFile(data.url) },
                customCoverFileLazy = lazy { coverCache.getCustomCoverFile(data.animeId) },
                diskCacheKeyLazy = lazy { AnimeCoverKeyer().key(data, options) },
                sourceLazy = lazy { sourceManager.get(data.sourceId) as? AnimeHttpSource },
                callFactoryLazy = callFactoryLazy,
                diskCacheLazy = diskCacheLazy,
            )
        }
    }

    companion object {
        const val USE_CUSTOM_COVER = "use_custom_cover"

        private val CACHE_CONTROL_NO_STORE = CacheControl.Builder().noStore().build()
        private val CACHE_CONTROL_NO_NETWORK_NO_CACHE = CacheControl.Builder().noCache().onlyIfCached().build()
    }
}
