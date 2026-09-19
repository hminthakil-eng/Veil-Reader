/*
 * Veil Reader GPU paper curl overlay.
 * Uses the cylinder mesh algorithm from Harri Smatt's android-pagecurl
 * (Apache-2.0) with Veil-specific lifecycle, texture ownership and
 * external Readium gesture driving.
 */
package com.veilreader.app.ui.paperturn.gpu;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

public final class GpuPaperCurlView extends GLSurfaceView
        implements CurlRenderer.Observer {

    public static final int SIDE_LEFT = -1;
    public static final int SIDE_RIGHT = 1;

    public interface SupportListener {
        void onSupportChanged(boolean supported);
    }
    private final CurlRenderer renderer;
    private final CurlMesh mesh;
    private final PointF dragStart = new PointF();
    private final PointF curlPos = new PointF();
    private final PointF curlDir = new PointF();
    private final PointF translatedStart = new PointF();
    private final PointF translatedPointer = new PointF();
    private final PointF canonicalStart = new PointF();
    private final PointF canonicalPointer = new PointF();

    private volatile boolean surfaceReady;
    private volatile boolean npotSupported;
    private volatile boolean active;
    private int side = SIDE_RIGHT;
    private float startYPx;
    private double curlRadius = 0.0001d;
    private int pagePixelWidth;
    private int pagePixelHeight;
    private SupportListener supportListener;

    public GpuPaperCurlView(Context context) {
        this(context, null);
    }

    public GpuPaperCurlView(Context context, AttributeSet attrs) {
        super(context, attrs);

        SurfaceHolder holder = getHolder();
        holder.setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        setPreserveEGLContextOnPause(true);
        renderer = new CurlRenderer(this);
        renderer.setBackgroundColor(Color.TRANSPARENT);
        mesh = new CurlMesh(20);

        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        setClickable(false);
        setFocusable(false);
        setFocusableInTouchMode(false);
    }

    public void setSupportListener(SupportListener listener) {
        supportListener = listener;
        notifySupport();
    }

    public boolean isGpuReady() {
        return surfaceReady && npotSupported
                && pagePixelWidth > 0 && pagePixelHeight > 0;
    }

    public boolean isActiveCurl() {
        return active;
    }

    public boolean begin(
            Bitmap snapshot,
            int side,
            float startYPx,
            int backColor
    ) {
        if (!isGpuReady() || snapshot == null || snapshot.isRecycled()) {
            return false;
        }
        this.side = side == SIDE_LEFT ? SIDE_LEFT : SIDE_RIGHT;
        this.startYPx = startYPx;
        this.active = true;

        CurlPage page = mesh.getTexturePage();
        page.setTexture(snapshot, CurlPage.SIDE_BOTH);
        page.setColor(Color.WHITE, CurlPage.SIDE_FRONT);
        page.setColor(backColor, CurlPage.SIDE_BACK);

        RectF rect = renderer.getPageRect(CurlRenderer.PAGE_RIGHT);
        mesh.setRect(rect);
        mesh.setFlipTexture(false);
        mesh.reset();
        renderer.addCurlMesh(mesh);

        translatedStart.set(
                this.side == SIDE_RIGHT ? pagePixelWidth : 0f,
                clamp(startYPx, 0f, pagePixelHeight)
        );
        renderer.translate(translatedStart);
        dragStart.set(
                this.side == SIDE_RIGHT ? rect.right : rect.left,
                clamp(translatedStart.y, rect.bottom, rect.top)
        );

        requestRender();
        return true;
    }
    public void updateDrag(
            float startX,
            float startY,
            float offsetX,
            float offsetY
    ) {
        if (!active) {
            return;
        }

        // Keep the physical grab point stable across noisy JS drag starts.
        this.startYPx = startY;
        translatedStart.set(startX, startY);
        renderer.translate(translatedStart);
        RectF rect = renderer.getPageRect(CurlRenderer.PAGE_RIGHT);
        dragStart.set(
                side == SIDE_RIGHT ? rect.right : rect.left,
                clamp(translatedStart.y, rect.bottom, rect.top)
        );

        updatePointer(startX + offsetX, startY + offsetY);
    }

    public void setProgress(float progress) {
        if (!active) {
            return;
        }

        float p = clamp(progress, 0f, 1f);
        float x = side == SIDE_RIGHT
                ? pagePixelWidth * (1f - 1.10f * p)
                : pagePixelWidth * (1.10f * p);
        float lift = (float) Math.sin(Math.PI * p)
                * pagePixelHeight * 0.055f;
        float yDirection = startYPx >= pagePixelHeight * 0.5f
                ? -1f
                : 1f;
        float y = startYPx + yDirection * lift;
        updatePointer(x, y);
    }

    public void endCurl() {
        active = false;
        renderer.removeCurlMesh(mesh);
        mesh.reset();
        requestRender();
    }

    public void releaseGpu() {
        active = false;
        renderer.removeCurlMesh(mesh);
        mesh.getTexturePage().release();
        queueEvent(mesh::resetTexture);
        requestRender();
    }

    private void updatePointer(float xPx, float yPx) {
        if (!active || !isGpuReady()) {
            return;
        }

        translatedPointer.set(xPx, yPx);
        renderer.translate(translatedPointer);

        RectF page = renderer.getPageRect(CurlRenderer.PAGE_RIGHT);
        float mirrorSum = page.left + page.right;
        canonicalPointer.set(translatedPointer);
        canonicalStart.set(dragStart);

        if (side == SIDE_LEFT) {
            canonicalPointer.x = mirrorSum - canonicalPointer.x;
            canonicalStart.x = page.right;
        } else {
            canonicalStart.x = page.right;
        }

        computeRightCurl(page, canonicalStart, canonicalPointer);

        if (side == SIDE_LEFT) {
            curlPos.x = mirrorSum - curlPos.x;
            curlDir.x = -curlDir.x;
        }

        constrainAndRender(page);
    }

    private void computeRightCurl(
            RectF page,
            PointF start,
            PointF pointer
    ) {
        double radius = page.width() * 0.095d;
        curlPos.set(pointer);
        curlDir.set(
                curlPos.x - start.x,
                curlPos.y - start.y
        );

        float dist = length(curlDir.x, curlDir.y);
        if (dist < 0.0001f) {
            curlRadius = 0.0001d;
            mesh.reset();
            requestRender();
            return;
        }
        float pageWidth = page.width();
        double curlLength = radius * Math.PI;

        if (dist > (pageWidth * 2f) - curlLength) {
            curlLength = Math.max((pageWidth * 2f) - dist, 0d);
            radius = curlLength / Math.PI;
        }

        if (curlLength <= 0.0001d) {
            radius = 0.0001d;
            curlLength = radius * Math.PI;
        }

        if (dist >= curlLength) {
            double translate = (dist - curlLength) * 0.5d;
            radius = Math.max(
                    Math.min(curlPos.x - page.left, radius),
                    0.0001d
            );
            curlPos.x -= curlDir.x * translate / dist;
            curlPos.y -= curlDir.y * translate / dist;
        } else {
            double angle = Math.PI * Math.sqrt(dist / curlLength);
            double translate = radius * Math.sin(angle);
            curlPos.x += curlDir.x * translate / dist;
            curlPos.y += curlDir.y * translate / dist;
        }

        curlRadius = Math.max(radius, 0.0001d);
    }

    private void constrainAndRender(RectF page) {
        if (curlPos.x >= page.right) {
            mesh.reset();
            requestRender();
            return;
        }

        if (curlPos.x < page.left) {
            curlPos.x = page.left;
        }

        if (curlDir.y != 0f) {
            float diffX = curlPos.x - page.left;
            float leftY = curlPos.y + diffX * curlDir.x / curlDir.y;
            if (curlDir.y < 0f && leftY < page.top) {
                curlDir.x = curlPos.y - page.top;
                curlDir.y = page.left - curlPos.x;
            } else if (curlDir.y > 0f && leftY > page.bottom) {
                curlDir.x = page.bottom - curlPos.y;
                curlDir.y = curlPos.x - page.left;
            }
        }

        float distance = length(curlDir.x, curlDir.y);
        if (distance <= 0.0001f) {
            mesh.reset();
            requestRender();
            return;
        }
        curlDir.x /= distance;
        curlDir.y /= distance;

        mesh.curl(
                curlPos,
                curlDir,
                Math.max(curlRadius, 0.0001d)
        );
        requestRender();
    }

    @Override
    public void onDrawFrame() {
        // Render-on-demand: no per-frame Java animation loop.
    }

    @Override
    public void onPageSizeChanged(int width, int height) {
        pagePixelWidth = width;
        pagePixelHeight = height;
        RectF rect = renderer.getPageRect(CurlRenderer.PAGE_RIGHT);
        mesh.setRect(rect);
        mesh.reset();
        notifySupport();
    }

    @Override
    public void onSurfaceCreated() {
        surfaceReady = true;
        npotSupported = renderer.supportsNpotTextures();
        mesh.resetTexture();
        notifySupport();
    }

    @Override
    protected void onDetachedFromWindow() {
        releaseGpu();
        surfaceReady = false;
        notifySupport();
        super.onDetachedFromWindow();
    }

    private void notifySupport() {
        SupportListener listener = supportListener;
        if (listener == null) {
            return;
        }
        boolean supported = isGpuReady();
        post(() -> listener.onSupportChanged(supported));
    }

    private static float length(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
