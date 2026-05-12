package com.fountainsim;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class FountainGLSurfaceView extends GLSurfaceView {
    private FountainRenderer renderer;
    private float prevX, prevY;
    private boolean isTouching = false;
    private ScaleGestureDetector scaleDetector;
    private long lastTouchTime = 0;

    public FountainGLSurfaceView(Context context, FountainRenderer renderer) {
        super(context);
        this.renderer = renderer;
        setEGLContextClientVersion(2);
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        lastTouchTime = System.currentTimeMillis();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                prevX = event.getX();
                prevY = event.getY();
                isTouching = true;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isTouching && event.getPointerCount() == 1) {
                    float dx = event.getX() - prevX;
                    float dy = event.getY() - prevY;
                    renderer.getScene().getCamera().rotate(dx, dy);
                    prevX = event.getX();
                    prevY = event.getY();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTouching = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            renderer.getScene().getCamera().zoom(detector.getScaleFactor());
            return true;
        }
    }

    public long getLastTouchTime() { return lastTouchTime; }
}
