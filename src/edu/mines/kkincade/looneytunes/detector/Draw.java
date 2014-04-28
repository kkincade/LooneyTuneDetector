package edu.mines.kkincade.looneytunes.detector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;


public class Draw extends View {
	Bitmap bitmap;
	
	Paint paintBlack;
	Paint paintYellow;
	Paint paintRed;
	Paint paintGreen;
	Paint paintBlue;
	
//	byte[] mYUVData;
//	int[] mRGBData;
//	int mImageWidth, mImageHeight;
//	int[] mRedHistogram;
//	int[] mGreenHistogram;
//	int[] mBlueHistogram;
//	double[] mBinSquared;
	long time_start;
		
    public Draw(Context context) {
        super(context);
        
        paintBlack = new Paint();
        paintBlack.setStyle(Paint.Style.FILL);
        paintBlack.setColor(Color.BLACK);
        paintBlack.setTextSize(40);
        
        paintYellow = new Paint();
        paintYellow.setStyle(Paint.Style.FILL);
        paintYellow.setColor(Color.YELLOW);
        paintYellow.setTextSize(25);
        
        paintRed = new Paint();
        paintRed.setStyle(Paint.Style.FILL);
        paintRed.setColor(Color.RED);
        paintRed.setTextSize(40);
        
        paintGreen = new Paint();
        paintGreen.setStyle(Paint.Style.FILL);
        paintGreen.setColor(Color.GREEN);
        paintGreen.setTextSize(25);
        
        paintBlue = new Paint();
        paintBlue.setStyle(Paint.Style.FILL);
        paintBlue.setColor(Color.BLUE);
        paintBlue.setTextSize(25);
    }
    
    
    @Override
    protected void onDraw(Canvas canvas) {
    	time_start = SystemClock.currentThreadTimeMillis();
    	if (bitmap != null) {
	    	String tag = "onDraw";
			Log.v(tag, "Called");
    	}
    	
    	long delta_time = SystemClock.currentThreadTimeMillis() - time_start;
    	
    	String toPrint = delta_time + " ms" + " - " ;
    	canvas.drawText( toPrint, 11, 700, paintBlack);
    	
        super.onDraw(canvas);
    }
} 
