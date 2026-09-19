/*
 * Copyright 2012 Harri Smatt
 * Licensed under the Apache License, Version 2.0.
 *
 * Modified for Veil Reader:
 * - caller-owned bitmaps are never recycled by the GPU curl layer
 * - modern NPOT textures are uploaded directly without power-of-two copies
 * - explicit upload state avoids per-turn bitmap duplication
 */
package com.veilreader.app.ui.paperturn.gpu;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;

public final class CurlPage {
    public static final int SIDE_FRONT = 1;
    public static final int SIDE_BACK = 2;
    public static final int SIDE_BOTH = 3;

    private final Bitmap fallback;
    private int colorFront = Color.WHITE;
    private int colorBack = Color.WHITE;
    private Bitmap textureFront;
    private Bitmap textureBack;
    private boolean texturesChanged = true;
    public CurlPage() {
        fallback = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        fallback.eraseColor(Color.WHITE);
        textureFront = fallback;
        textureBack = fallback;
    }

    public synchronized int getColor(int side) {
        return side == SIDE_FRONT ? colorFront : colorBack;
    }

    public synchronized Bitmap getTexture(RectF textureRect, int side) {
        textureRect.set(0f, 0f, 1f, 1f);
        Bitmap bitmap = side == SIDE_FRONT ? textureFront : textureBack;
        return bitmap != null && !bitmap.isRecycled() ? bitmap : fallback;
    }

    public synchronized boolean getTexturesChanged() {
        return texturesChanged;
    }

    public synchronized boolean hasBackTexture() {
        return textureBack != textureFront;
    }

    public synchronized void markTexturesUploaded() {
        texturesChanged = false;
    }
    public synchronized void reset() {
        colorFront = Color.WHITE;
        colorBack = Color.WHITE;
        textureFront = fallback;
        textureBack = fallback;
        texturesChanged = true;
    }

    public synchronized void setColor(int color, int side) {
        switch (side) {
            case SIDE_FRONT:
                colorFront = color;
                break;
            case SIDE_BACK:
                colorBack = color;
                break;
            default:
                colorFront = color;
                colorBack = color;
                break;
        }
    }

    public synchronized void setTexture(Bitmap texture, int side) {
        Bitmap safe = texture != null && !texture.isRecycled()
                ? texture
                : fallback;
        switch (side) {
            case SIDE_FRONT:
                textureFront = safe;
                break;
            case SIDE_BACK:
                textureBack = safe;
                break;
            default:
                textureFront = safe;
                textureBack = safe;
                break;
        }
        texturesChanged = true;
    }

    public synchronized void release() {
        // Keep the 1x1 fallback alive across detach/reattach. GLSurfaceView
        // can recreate its EGL context while the Java view instance survives.
        textureFront = fallback;
        textureBack = fallback;
        texturesChanged = true;
    }
}
