package com.veilreader.app.manga.debug

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.File

object MangaDebugFixtureAssets {

    fun ensurePng(
        root: File,
        name: String,
        width: Int,
        height: Int,
        seed: Int
    ): File {
        require(width > 0 && height > 0)
        root.mkdirs()
        val file = File(root, name)

        if (isValidFixture(file, width, height)) return file
        file.delete()

        val safeSeed = seed and Int.MAX_VALUE
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            val background = Paint().apply {
                color = Color.rgb(
                    40 + (safeSeed * 37L % 160L).toInt(),
                    40 + (safeSeed * 53L % 160L).toInt(),
                    40 + (safeSeed * 71L % 160L).toInt()
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), background)

            val marker = Paint().apply {
                color = Color.WHITE
                strokeWidth = 8f
                textSize = 42f
            }
            var y = 80
            var markerIndex = 0
            while (y < height) {
                canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), marker)
                canvas.drawText(
                    "Veil " + seed + " / " + markerIndex,
                    24f,
                    (y - 16).coerceAtLeast(48).toFloat(),
                    marker
                )
                y += 512
                markerIndex += 1
            }

            val temporary = File(root, name + ".tmp")
            temporary.delete()
            temporary.outputStream().buffered().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    "Fixture image compression failed"
                }
            }
            if (!temporary.renameTo(file)) {
                file.delete()
                check(temporary.renameTo(file)) {
                    "Fixture image atomic replace failed"
                }
            }
        } finally {
            bitmap.recycle()
        }

        check(isValidFixture(file, width, height)) {
            "Fixture image dimensions are invalid after write"
        }
        return file
    }

    private fun isValidFixture(
        file: File,
        expectedWidth: Int,
        expectedHeight: Int
    ): Boolean {
        if (!file.isFile || file.length() <= 0L) return false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth == expectedWidth && options.outHeight == expectedHeight
    }
}
