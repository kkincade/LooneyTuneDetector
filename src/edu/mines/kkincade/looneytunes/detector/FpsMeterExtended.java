package edu.mines.kkincade.looneytunes.detector;

import java.text.DecimalFormat;

import org.opencv.core.Core;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class FpsMeterExtended {
    private static final String TAG = "FpsMeter";
    private static final int STEP = 20;
    private static final DecimalFormat FPS_FORMAT = new DecimalFormat("0.00");

    private int framesCounter;
    private double frequency;
    private long prevFrameTime;
    private String fpsString;
    Paint paint;
    boolean isInitialized = false;
    int width = 0;
    int height = 0;

    public void init() {
        framesCounter = 0;
        frequency = Core.getTickFrequency();
        prevFrameTime = Core.getTickCount();
        fpsString = "";

        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setTextSize(40);
    }

    public void measure() {
        if (!isInitialized) {
            init();
            isInitialized = true;
        } else {
            framesCounter++;
            if (framesCounter % STEP == 0) {
                long time = Core.getTickCount();
                double fps = STEP * frequency / (time - prevFrameTime);
                prevFrameTime = time;
                if (width != 0 && height != 0)
                    fpsString = FPS_FORMAT.format(fps) + " FPS@" + Integer.valueOf(width) + "x" + Integer.valueOf(height);
                else
                    fpsString = FPS_FORMAT.format(fps) + " FPS";
                Log.i(TAG, fpsString);
            }
        }
    }

    public void setResolution(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void draw(Canvas canvas, float offsetx, float offsety) {
        Log.d(TAG, fpsString);
        canvas.drawText(fpsString, offsetx, offsety, paint);
    }

}
