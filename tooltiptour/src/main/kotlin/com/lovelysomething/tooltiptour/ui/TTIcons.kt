package com.lovelysomething.tooltiptour.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color

/**
 * 24 stroke icons — identical paths to the iOS TTIcons.swift and the web SVGs.
 * Drawn via Canvas.drawPath with round caps and joins on a 24×24 unit grid.
 */
enum class TTIcon(val key: String) {
    QUESTION("question"), COMPASS("compass"), MAP("map"), LIGHTBULB("lightbulb"),
    SPARKLE("sparkle"), SEARCH("search"), BOOK("book"), ROCKET("rocket"),
    CHAT("chat"), INFO("info"), PLAY("play"), GUIDE("guide"),
    FLAG("flag"), BELL("bell"), GIFT("gift"), CHECK("check"),
    HEART("heart"), LOCK("lock"), SETTINGS("settings"), TROPHY("trophy"),
    ZAP("zap"), EYE("eye"), CURSOR("cursor"), CHART("chart");

    companion object {
        fun from(key: String?): TTIcon = values().firstOrNull { it.key == key } ?: QUESTION
    }
}

/** Draw [icon] centred in the current DrawScope at [size] × [size] dp. */
fun DrawScope.drawTTIcon(icon: TTIcon, color: Color, sizePx: Float) {
    val s = sizePx / 24f
    fun p(x: Float, y: Float) = Offset(x * s, y * s)

    val path = Path()

    when (icon) {
        TTIcon.QUESTION -> {
            path.addOval(Rect(Offset(2 * s, 2 * s), Size(20 * s, 20 * s)))
            path.moveTo(9.09f * s, 9f * s)
            path.cubicTo(9.09f * s, 5.8f * s, 14.92f * s, 5.8f * s, 14.92f * s, 10f * s)
            path.cubicTo(14.92f * s, 12f * s, 12f * s, 13f * s, 12f * s, 13f * s)
            path.moveTo(12f * s, 17f * s); path.lineTo(12f * s, 17.01f * s)
        }
        TTIcon.COMPASS -> {
            path.addOval(Rect(Offset(2 * s, 2 * s), Size(20 * s, 20 * s)))
            path.moveTo(16.24f * s, 7.76f * s)
            path.lineTo(14.12f * s, 14.12f * s)
            path.lineTo(7.76f * s, 16.24f * s)
            path.lineTo(9.88f * s, 9.88f * s)
            path.close()
        }
        TTIcon.MAP -> {
            path.moveTo(3 * s, 6 * s)
            path.lineTo(9 * s, 3 * s); path.lineTo(15 * s, 6 * s); path.lineTo(21 * s, 3 * s)
            path.lineTo(21 * s, 18 * s); path.lineTo(15 * s, 21 * s)
            path.lineTo(9 * s, 18 * s); path.lineTo(3 * s, 21 * s)
            path.close()
            path.moveTo(9 * s, 3 * s);  path.lineTo(9 * s, 18 * s)
            path.moveTo(15 * s, 6 * s); path.lineTo(15 * s, 21 * s)
        }
        TTIcon.LIGHTBULB -> {
            path.moveTo(9 * s, 18 * s);  path.lineTo(15 * s, 18 * s)
            path.moveTo(10 * s, 22 * s); path.lineTo(14 * s, 22 * s)
            path.moveTo(15.09f * s, 14 * s)
            path.cubicTo(16.7f * s, 12.5f * s, 18 * s, 10.5f * s, 18 * s, 8 * s)
            path.arcTo(Rect(Offset(6 * s, 2 * s), Size(12 * s, 12 * s)), 0f, 180f, false)
            path.cubicTo(6 * s, 10.5f * s, 7.3f * s, 12.5f * s, 8.91f * s, 14 * s)
        }
        TTIcon.SPARKLE -> {
            val pts = listOf(
                12f to 2f, 15.09f to 8.26f, 22f to 9.27f, 17f to 14.14f, 18.18f to 21.02f,
                12f to 17.77f, 5.82f to 21.02f, 7f to 14.14f, 2f to 9.27f, 8.91f to 8.26f,
            )
            path.moveTo(pts[0].first * s, pts[0].second * s)
            pts.drop(1).forEach { path.lineTo(it.first * s, it.second * s) }
            path.close()
        }
        TTIcon.SEARCH -> {
            path.addOval(Rect(Offset(3 * s, 3 * s), Size(16 * s, 16 * s)))
            path.moveTo(21 * s, 21 * s); path.lineTo(16.65f * s, 16.65f * s)
        }
        TTIcon.BOOK -> {
            path.moveTo(4 * s, 19.5f * s)
            path.quadraticBezierTo(4 * s, 17 * s, 6.5f * s, 17 * s)
            path.lineTo(20 * s, 17 * s)
            path.moveTo(6.5f * s, 2 * s)
            path.lineTo(20 * s, 2 * s); path.lineTo(20 * s, 22 * s); path.lineTo(6.5f * s, 22 * s)
            path.quadraticBezierTo(4 * s, 22 * s, 4 * s, 19.5f * s)
            path.lineTo(4 * s, 4.5f * s)
            path.quadraticBezierTo(4 * s, 2 * s, 6.5f * s, 2 * s)
            path.close()
        }
        TTIcon.ROCKET -> {
            path.moveTo(22 * s, 2 * s); path.lineTo(11 * s, 13 * s)
            path.moveTo(22 * s, 2 * s)
            path.lineTo(15 * s, 22 * s); path.lineTo(11 * s, 13 * s)
            path.lineTo(2 * s, 9 * s)
            path.close()
        }
        TTIcon.CHAT -> {
            path.moveTo(21 * s, 15 * s)
            path.quadraticBezierTo(21 * s, 17 * s, 19 * s, 17 * s)
            path.lineTo(7 * s, 17 * s); path.lineTo(3 * s, 21 * s); path.lineTo(3 * s, 5 * s)
            path.quadraticBezierTo(3 * s, 3 * s, 5 * s, 3 * s)
            path.lineTo(19 * s, 3 * s)
            path.quadraticBezierTo(21 * s, 3 * s, 21 * s, 5 * s)
            path.close()
        }
        TTIcon.INFO -> {
            path.addOval(Rect(Offset(2 * s, 2 * s), Size(20 * s, 20 * s)))
            path.moveTo(12 * s, 16 * s); path.lineTo(12 * s, 12 * s)
            path.moveTo(12 * s, 8 * s);  path.lineTo(12 * s, 8.01f * s)
        }
        TTIcon.PLAY -> {
            path.addOval(Rect(Offset(2 * s, 2 * s), Size(20 * s, 20 * s)))
            path.moveTo(10 * s, 8 * s); path.lineTo(16 * s, 12 * s); path.lineTo(10 * s, 16 * s)
            path.close()
        }
        TTIcon.GUIDE -> {
            path.addOval(Rect(Offset(2 * s, 2 * s), Size(20 * s, 20 * s)))
            path.moveTo(12 * s, 8 * s); path.lineTo(16 * s, 12 * s); path.lineTo(12 * s, 16 * s)
            path.moveTo(8 * s, 12 * s);  path.lineTo(16 * s, 12 * s)
        }
        TTIcon.FLAG -> {
            path.moveTo(4 * s, 22 * s); path.lineTo(4 * s, 3 * s)
            path.lineTo(19 * s, 3 * s); path.lineTo(19 * s, 13 * s); path.lineTo(4 * s, 13 * s)
        }
        TTIcon.BELL -> {
            path.moveTo(18 * s, 8 * s)
            path.arcTo(Rect(Offset(6 * s, 2 * s), Size(12 * s, 12 * s)), 0f, -180f, false)
            path.cubicTo(6 * s, 15 * s, 3 * s, 17 * s, 3 * s, 17 * s)
            path.lineTo(21 * s, 17 * s)
            path.cubicTo(21 * s, 15 * s, 18 * s, 15 * s, 18 * s, 8 * s)
            path.moveTo(10.73f * s, 21 * s)
            path.quadraticBezierTo(12 * s, 23 * s, 13.27f * s, 21 * s)
        }
        TTIcon.GIFT -> {
            path.addRect(Rect(Offset(2 * s, 7 * s), Size(20 * s, 2 * s)))
            path.addRect(Rect(Offset(3 * s, 9 * s), Size(18 * s, 13 * s)))
            path.moveTo(12 * s, 7 * s);  path.lineTo(12 * s, 22 * s)
            path.moveTo(12 * s, 7 * s)
            path.cubicTo(10 * s, 7 * s, 8 * s, 5.5f * s, 8 * s, 3 * s)
            path.cubicTo(8 * s, 1.5f * s, 11 * s, 4 * s, 12 * s, 7 * s)
            path.moveTo(12 * s, 7 * s)
            path.cubicTo(14 * s, 7 * s, 16 * s, 5.5f * s, 16 * s, 3 * s)
            path.cubicTo(16 * s, 1.5f * s, 13 * s, 4 * s, 12 * s, 7 * s)
        }
        TTIcon.CHECK -> {
            path.addOval(Rect(Offset(2 * s, 2 * s), Size(20 * s, 20 * s)))
            path.moveTo(8 * s, 12 * s); path.lineTo(11 * s, 15 * s); path.lineTo(16 * s, 9.5f * s)
        }
        TTIcon.HEART -> {
            path.moveTo(12 * s, 21 * s)
            path.cubicTo(7 * s, 21 * s, 2 * s, 17 * s, 3 * s, 12 * s)
            path.cubicTo(2 * s, 7 * s, 4.5f * s, 5.5f * s, 7.5f * s, 5.5f * s)
            path.cubicTo(10 * s, 5.5f * s, 11 * s, 6.5f * s, 12 * s, 9.5f * s)
            path.cubicTo(13 * s, 6.5f * s, 14 * s, 5.5f * s, 16.5f * s, 5.5f * s)
            path.cubicTo(19.5f * s, 5.5f * s, 22 * s, 7 * s, 21 * s, 12 * s)
            path.cubicTo(22 * s, 17 * s, 17 * s, 21 * s, 12 * s, 21 * s)
            path.close()
        }
        TTIcon.LOCK -> {
            path.addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    Rect(Offset(3 * s, 11 * s), Size(18 * s, 11 * s)),
                    androidx.compose.ui.geometry.CornerRadius(2 * s)
                )
            )
            path.moveTo(7 * s, 11 * s); path.lineTo(7 * s, 7 * s)
            path.arcTo(Rect(Offset(7 * s, 2 * s), Size(10 * s, 10 * s)), 180f, -180f, false)
            path.lineTo(17 * s, 11 * s)
        }
        TTIcon.SETTINGS -> {
            path.moveTo(4 * s, 21 * s);  path.lineTo(4 * s, 14 * s)
            path.moveTo(4 * s, 10 * s);  path.lineTo(4 * s, 3 * s)
            path.moveTo(12 * s, 21 * s); path.lineTo(12 * s, 12 * s)
            path.moveTo(12 * s, 8 * s);  path.lineTo(12 * s, 3 * s)
            path.moveTo(20 * s, 21 * s); path.lineTo(20 * s, 16 * s)
            path.moveTo(20 * s, 12 * s); path.lineTo(20 * s, 3 * s)
            path.addOval(Rect(Offset(2 * s, 10 * s),  Size(4 * s, 4 * s)))
            path.addOval(Rect(Offset(10 * s, 6 * s),  Size(4 * s, 4 * s)))
            path.addOval(Rect(Offset(18 * s, 12 * s), Size(4 * s, 4 * s)))
        }
        TTIcon.TROPHY -> {
            path.moveTo(6 * s, 3 * s); path.lineTo(18 * s, 3 * s)
            path.lineTo(18 * s, 10 * s)
            path.cubicTo(18 * s, 14 * s, 15 * s, 17 * s, 12 * s, 17 * s)
            path.cubicTo(9 * s, 17 * s, 6 * s, 14 * s, 6 * s, 10 * s)
            path.close()
            path.moveTo(6 * s, 5 * s)
            path.cubicTo(2 * s, 5 * s, 2 * s, 9 * s, 3 * s, 9 * s)
            path.lineTo(6 * s, 9 * s)
            path.moveTo(18 * s, 5 * s)
            path.cubicTo(22 * s, 5 * s, 22 * s, 9 * s, 21 * s, 9 * s)
            path.lineTo(18 * s, 9 * s)
            path.moveTo(12 * s, 17 * s); path.lineTo(12 * s, 21 * s)
            path.moveTo(8 * s, 21 * s);  path.lineTo(16 * s, 21 * s)
        }
        TTIcon.ZAP -> {
            val pts = listOf(13f to 2f, 3f to 14f, 12f to 14f, 11f to 22f, 21f to 10f, 12f to 10f)
            path.moveTo(pts[0].first * s, pts[0].second * s)
            pts.drop(1).forEach { path.lineTo(it.first * s, it.second * s) }
            path.close()
        }
        TTIcon.EYE -> {
            path.moveTo(1 * s, 12 * s)
            path.cubicTo(1 * s, 7.5f * s, 6 * s, 5 * s, 12 * s, 5 * s)
            path.cubicTo(18 * s, 5 * s, 23 * s, 7.5f * s, 23 * s, 12 * s)
            path.cubicTo(23 * s, 16.5f * s, 18 * s, 19 * s, 12 * s, 19 * s)
            path.cubicTo(6 * s, 19 * s, 1 * s, 16.5f * s, 1 * s, 12 * s)
            path.addOval(Rect(Offset(9 * s, 9 * s), Size(6 * s, 6 * s)))
        }
        TTIcon.CURSOR -> {
            path.moveTo(5 * s, 3 * s)
            path.lineTo(19 * s, 12 * s); path.lineTo(11 * s, 14 * s)
            path.lineTo(9 * s, 22 * s)
            path.close()
        }
        TTIcon.CHART -> {
            path.moveTo(18 * s, 20 * s); path.lineTo(18 * s, 10 * s)
            path.moveTo(12 * s, 20 * s); path.lineTo(12 * s, 4 * s)
            path.moveTo(6 * s, 20 * s);  path.lineTo(6 * s, 14 * s)
            path.moveTo(2 * s, 20 * s);  path.lineTo(22 * s, 20 * s)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = 2f * (sizePx / 24f),
            cap   = StrokeCap.Round,
            join  = StrokeJoin.Round,
        ),
    )
}
