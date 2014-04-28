package edu.mines.kkincade.looneytunes.extended;

import java.io.FileOutputStream;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

public class ViewExtended extends JavaCameraViewExtended {
	
    private static final String TAG = "Debug: ";
    Paint paintBlack;
    
    public ViewExtended(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        paintBlack = new Paint();
        paintBlack.setStyle(Paint.Style.FILL);
        paintBlack.setColor(Color.BLACK);
        paintBlack.setTextSize(40);
    }
    
    
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    	Log.d(TAG, "Surface Created");
    	invalidate();
    }

    public void takePicture(final String fileName) {
    	Log.d(TAG, "Taking picture");
    	Camera.Parameters params = camera.getParameters();
    	params.setPictureSize(3264, 2448);
		camera.setParameters(params);
		
        PictureCallback callback = new PictureCallback() {
            private String mPictureFileName = fileName;

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.i(TAG, "Saving a bitmap to file");
                Bitmap picture = BitmapFactory.decodeByteArray(data, 0, data.length);
                try {
                    FileOutputStream out = new FileOutputStream(mPictureFileName);
                    picture.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    picture.recycle();
                    camera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        
        camera.takePicture(null, null, callback);
    }
    
    
    /** -------------------------------- Getters and Setters ---------------------------------- **/
    
    public String FPS() { return fpsMeter.toString(); }
    public List<String> getEffectList() { return camera.getParameters().getSupportedColorEffects(); }
    public boolean isEffectSupported() { return (camera.getParameters().getColorEffect() != null); }
    public String getEffect() { return camera.getParameters().getColorEffect(); }
    public List<Size> getResolutionList() { return camera.getParameters().getSupportedPreviewSizes(); }
    public Size getResolution() { return camera.getParameters().getPreviewSize(); }
    
    public void setEffect(String effect) {
        Camera.Parameters params = camera.getParameters();
        params.setColorEffect(effect);
        camera.setParameters(params);
    }

    public void setResolution(Size resolution) {
        disconnectCamera();
        maxHeight = resolution.height;
        maxWidth = resolution.width;
        String res = Integer.toString(getHeight()) + " x " + Integer.toString(getWidth());
        Toast.makeText(getContext(), res, Toast.LENGTH_SHORT).show();
        connectCamera(getWidth(), getHeight());
    }
}
