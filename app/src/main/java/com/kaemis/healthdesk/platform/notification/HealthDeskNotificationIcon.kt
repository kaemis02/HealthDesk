package com.kaemis.healthdesk.platform.notification

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.kaemis.healthdesk.R

/** Supplies a transparent, circular large icon instead of the adaptive launcher icon. */
object HealthDeskNotificationIcon {
    fun large(context: Context): Bitmap = requireNotNull(
        ContextCompat.getDrawable(context, R.drawable.ic_notification_large),
    ).toBitmap()
}
