package com.example.bnki.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import com.example.bnki.MainActivity
import com.example.bnki.data.BnkiRepository

class BnkiWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val due = BnkiRepository.from(context).countAllDue()
        provideContent {
            WidgetContent(due)
        }
    }
}

@Composable
private fun WidgetContent(due: Int) {
    val context = LocalContext.current
    val openApp = actionStartActivity(Intent(context, MainActivity::class.java))
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(12.dp)
            .clickable(openApp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "$due",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                color = ColorProvider(Color(0xFF6200EE)),
            ),
        )
        Text(
            text = if (due == 1) "Karte fällig" else "Karten fällig",
            style = TextStyle(color = GlanceTheme.colors.onSurface),
        )
        Text(
            text = "Jetzt lernen ▶",
            style = TextStyle(color = ColorProvider(Color(0xFF6200EE))),
        )
    }
}

class BnkiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BnkiWidget()
}
