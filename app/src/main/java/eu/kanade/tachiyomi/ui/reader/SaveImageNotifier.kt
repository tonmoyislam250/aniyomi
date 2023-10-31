package eu.kanade.tachiyomi.ui.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.app.NotificationCompat
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notify

/**
 * Class used to show BigPictureStyle notifications
 */
class SaveImageNotifier(private val context: Context) {

    private val notificationBuilder = context.notificationBuilder(Notifications.CHANNEL_COMMON)
    private val notificationId: Int = Notifications.ID_DOWNLOAD_IMAGE

    /**
     * Called when image download/copy is complete.
     *
     * @param uri image file containing downloaded page image.
     */
    fun onComplete(uri: Uri) {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .size(720, 1280)
            .target(
                onSuccess = { result ->
                    showCompleteNotification(uri, (result as BitmapDrawable).bitmap)
                },
                onError = {
                    onError(null)
                },
            )
            .build()
        context.imageLoader.enqueue(request)
    }

    /**
     * Clears the notification message.
     */
    fun onClear() {
        context.cancelNotification(notificationId)
    }

    /**
     * Called on error while downloading image.
     * @param error string containing error information.
     */
    fun onError(error: String?) {
        // Create notification
        with(notificationBuilder) {
            setContentTitle(context.getString(R.string.download_notifier_title_error))
            setContentText(error ?: context.getString(R.string.unknown_error))
            setSmallIcon(android.R.drawable.ic_menu_report_image)
        }
        updateNotification()
    }

    private fun showCompleteNotification(uri: Uri, image: Bitmap) {
        with(notificationBuilder) {
            setContentTitle(context.getString(R.string.picture_saved))
            setSmallIcon(R.drawable.ic_photo_24dp)
            setStyle(NotificationCompat.BigPictureStyle().bigPicture(image))
            setLargeIcon(image)
            setAutoCancel(true)

            // Clear old actions if they exist
            clearActions()

            setContentIntent(NotificationHandler.openImagePendingActivity(context, uri))
            // Share action
            addAction(
                R.drawable.ic_share_24dp,
                context.getString(R.string.action_share),
                NotificationReceiver.shareImagePendingBroadcast(context, uri.path!!, notificationId),
            )
            // Delete action
            addAction(
                R.drawable.ic_delete_24dp,
                context.getString(R.string.action_delete),
                NotificationReceiver.deleteImagePendingBroadcast(context, uri.path!!, notificationId),
            )

            updateNotification()
        }
    }

    private fun updateNotification() {
        // Displays the progress bar on notification
        context.notify(notificationId, notificationBuilder.build())
    }
}
