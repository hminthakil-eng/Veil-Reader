package com.veilreader.app.ui.reader.material

import androidx.core.graphics.createBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color

/** Same white backing as CurlMesh's blank pass, composed once instead of every frame. */
internal fun nativePaperOwnedOpaqueTexture(source: Bitmap): Bitmap =
    createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888).also { owned ->
        val canvas = Canvas(owned)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(source, 0f, 0f, null)
        owned.setHasAlpha(false)
    }
