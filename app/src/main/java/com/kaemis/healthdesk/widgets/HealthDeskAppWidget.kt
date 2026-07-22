package com.kaemis.healthdesk.widgets

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kaemis.healthdesk.HealthDeskApplication
import com.kaemis.healthdesk.MainActivity
import com.kaemis.healthdesk.data.datastore.SettingsSnapshot
import com.kaemis.healthdesk.data.entity.DEFAULT_PROFILE_ID
import com.kaemis.healthdesk.platform.service.FocusActionCommand
import kotlinx.coroutines.flow.first

class HealthDeskAppWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as? HealthDeskApplication
        val settings = app?.appContainer?.settingsDataStore?.settings?.first() ?: SettingsSnapshot()
        val profile = app?.appContainer?.database?.userProfileDao()?.getProfile(DEFAULT_PROFILE_ID)
        val focusState = FocusWidgetStateStore.load(context)
        val snapshot = HealthDeskWidgetSnapshotBuilder.build(profile, settings, focusState)
        val accent = widgetAccent(snapshot.accentHex)
        val accentSoft = ColorProvider(accent.copy(alpha = 0.18f))

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFFFFFDF8)))
                    .clickable(actionStartActivity<MainActivity>())
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.Start,
            ) {
                Text(
                    text = snapshot.title,
                    style = TextStyle(fontWeight = FontWeight.Bold, color = ColorProvider(Color(0xFF24312F))),
                    maxLines = 1,
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = snapshot.subtitle,
                    style = TextStyle(color = ColorProvider(Color(0xFF5D6762))),
                    maxLines = 2,
                )
                Spacer(modifier = GlanceModifier.height(12.dp))
                Row(horizontalAlignment = Alignment.Horizontal.Start) {
                    Text(
                        text = snapshot.primaryAction,
                        modifier = GlanceModifier
                            .background(ColorProvider(accent))
                            .clickable(widgetFocusAction(snapshot.primaryActionCommand))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        style = TextStyle(color = ColorProvider(Color.White)),
                        maxLines = 1,
                    )
                    snapshot.secondaryAction?.let { secondary ->
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = secondary,
                            modifier = GlanceModifier
                                .background(accentSoft)
                            .clickable(widgetFocusAction(snapshot.secondaryActionCommand!!))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            style = TextStyle(color = ColorProvider(Color(0xFF24312F))),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

class HealthDeskAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HealthDeskAppWidget()
}

object HealthDeskWidgetUpdater {
    suspend fun updateAll(context: Context) {
        HealthDeskAppWidget().updateAll(context)
    }
}

private fun widgetFocusAction(command: String) = actionRunCallback<WidgetFocusActionCallback>(
    actionParametersOf(WidgetFocusActionCallback.commandKey to command),
)

class WidgetFocusActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val command = when (parameters[commandKey]) {
            "com.kaemis.healthdesk.focus.action.STOP" -> FocusActionCommand.Stop
            "com.kaemis.healthdesk.focus.action.PAUSE" -> FocusActionCommand.Pause
            "com.kaemis.healthdesk.focus.action.RESUME" -> FocusActionCommand.Resume
            "com.kaemis.healthdesk.focus.action.STOP_ALARM" -> FocusActionCommand.StopAlarm
            "com.kaemis.healthdesk.focus.action.SNOOZE" -> FocusActionCommand.Snooze
            else -> FocusActionCommand.StartFocus
        }
        (context.applicationContext as? HealthDeskApplication)?.appContainer?.focusActionBus?.send(command)
        HealthDeskWidgetUpdater.updateAll(context)
    }

    companion object {
        val commandKey = ActionParameters.Key<String>("focus_command")
    }
}

private fun widgetAccent(accentKey: String): Color = if (accentKey.startsWith("#")) {
    runCatching { Color(android.graphics.Color.parseColor(accentKey)) }.getOrDefault(Color(0xFF6697FF))
} else {
    when (accentKey) {
        "mint" -> Color(0xFF43A779)
        "amber" -> Color(0xFFE6A93E)
        "clay" -> Color(0xFFC98261)
        "sky" -> Color(0xFF5C8DB8)
        "lavender" -> Color(0xFF8A7CCF)
        else -> Color(0xFF6697FF)
    }
}
