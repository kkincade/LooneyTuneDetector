package edu.mines.kkincade.looneytunes.extended;

import java.util.List;

import org.opencv.R;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;

import edu.mines.kkincade.looneytunes.detector.FpsMeterExtended;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * This is a basic class, implementing the interaction with Camera and OpenCV library.
 * The main responsibility of it is to control when camera can be enabled, process the frame,
 * call external listener to make any adjustments to the frame, and then draw the resulting
 * frame to the screen.
 * 
 * SurfaceView provides a dedicated drawing surface embedded inside of a view hierarchy. 
 * You can control the format of this surface and, if you like, its size. The SurfaceView 
 * takes care of placing the surface at the correct location on the screen.
 */
public abstract class CameraBridgeViewBaseExtended extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "CameraBridge";
    private static final int MAX_UNSPECIFIED = -1;
    private static final int STOPPED = 0;
    private static final int STARTED = 1;

    private int state = STOPPED;
    private Bitmap cacheBitmap;
    private CvCameraViewListener2 cameraListener;
    private boolean surfaceExist;
    private Object syncObject = new Object();

    protected int frameWidth;
    protected int frameHeight;
    protected int maxHeight;
    protected int maxWidth;
    protected int previewFormat = Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA;
    protected int cameraIndex = -1;
    protected boolean enabled;
    protected FpsMeterExtended fpsMeter = null;

    public CameraBridgeViewBaseExtended(Context context, int cameraId) {
        super(context);
        cameraIndex = cameraId;
    }
    
    
    public CameraBridgeViewBaseExtended(Context context, AttributeSet attrs) {
        super(context, attrs);

        int count = attrs.getAttributeCount();
        Log.d(TAG, "Attr count: " + Integer.valueOf(count));

        // Get "show_fps" and "camera_index" attributes
        TypedArray styledAttrs = getContext().obtainStyledAttributes(attrs, R.styleable.CameraBridgeViewBase);
        if (styledAttrs.getBoolean(R.styleable.CameraBridgeViewBase_show_fps, false)) { enableFpsMeter(); }
        cameraIndex = styledAttrs.getInt(R.styleable.CameraBridgeViewBase_camera_id, -1);
        styledAttrs.recycle();
        
        getHolder().addCallback(this);
        maxWidth = MAX_UNSPECIFIED;
        maxHeight = MAX_UNSPECIFIED;
    }

    
    /** ----------------------------- SurfaceHolder Callback Methods ----------------------------- **/
    
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        Log.d(TAG, "call surfaceChanged event");
        synchronized(syncObject) {
            if (!surfaceExist) {
                surfaceExist = true;
                checkCurrentState();
            } else {
                /** Surface changed. We need to stop camera and restart with new parameters */
                /* Pretend that old surface has been destroyed */
                surfaceExist = false;
                checkCurrentState();
                /* Now use new surface. Say we have it now */
                surfaceExist = true;
                checkCurrentState();
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) { }
    public void surfaceDestroyed(SurfaceHolder holder) {
        synchronized(syncObject) {
            surfaceExist = false;
            checkCurrentState();
        }
    }
    
    
    /** ------------------------------------ CvCameraView Classes and Interfaces ------------------------------------ **/
    
    public interface CvCameraViewListener {
        public void onCameraViewStarted(int width, int height);
        public void onCameraViewStopped();
        public Mat onCameraFrame(Mat inputFrame);
    }

    public interface CvCameraViewListener2 {
        public void onCameraViewStarted(int width, int height);
        public void onCameraViewStopped();
        public Mat onCameraFrame(CvCameraViewFrame inputFrame);
    };

    protected class CvCameraViewListenerAdapter implements CvCameraViewListener2  {
    	
    	// Variables
    	private int listenerPreviewFormat = Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA;
        private CvCameraViewListener oldStyleListener;
        
        // Constructor
        public CvCameraViewListenerAdapter(CvCameraViewListener oldStypeListener) { 
        	oldStyleListener = oldStypeListener; 
        }
        
        // Camera callback methods
        public void onCameraViewStarted(int width, int height) { oldStyleListener.onCameraViewStarted(width, height); }
        public void onCameraViewStopped() { oldStyleListener.onCameraViewStopped(); }

        public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
             Mat result = null;
             switch (listenerPreviewFormat) {
                case Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA:
                    result = oldStyleListener.onCameraFrame(inputFrame.rgba());
                    break;
                case Highgui.CV_CAP_ANDROID_GREY_FRAME:
                    result = oldStyleListener.onCameraFrame(inputFrame.gray());
                    break;
                default:
                    Log.e(TAG, "Invalid frame format! Only RGBA and Gray Scale are supported!");
            };

            return result;
        }

        // Getters and setters
        public void setFrameFormat(int format) { listenerPreviewFormat = format; }
    };
    
    /** This class interface is abstract representation of single frame from camera for onCameraFrame callback
     * Attention: Do not use objects, that represents this interface out of onCameraFrame callback! **/
    public interface CvCameraViewFrame {
        public Mat rgba(); // Returns RGBA Mat with frame
        public Mat gray(); // Returns Gray Mat with frame
    };


    /** ----------------------------- Enable/Disable Methods ---------------------------- **/
    
    /** This method is provided for clients, so they can enable the camera connection. The actual 
     * onCameraViewStarted callback will be delivered only after both this method is called and surface is available **/
    public void enableView() {
        synchronized(syncObject) {
            enabled = true;
            checkCurrentState();
        }
    }

    /** This method is provided for clients, so they can disable camera connection and stop the delivery
     *  of frames even though the surface view itself is not destroyed and still stays on the screen **/
    public void disableView() {
        synchronized(syncObject) {
            enabled = false;
            checkCurrentState();
        }
    }

    /** This method enables the fps label on the screen **/
    public void enableFpsMeter() {
        if (fpsMeter == null) {
            fpsMeter = new FpsMeterExtended();
            fpsMeter.setResolution(frameWidth, frameHeight);
        }
    }

    public void disableFpsMeter() { 
    	fpsMeter = null;
    }

    
    /** ------------------------------------ Getters and Setters --------------------------------------**/
    
    public void setCvCameraViewListener(CvCameraViewListener2 listener) {
        cameraListener = listener;
    }

    public void setCvCameraViewListener(CvCameraViewListener listener) {
        CvCameraViewListenerAdapter adapter = new CvCameraViewListenerAdapter(listener);
        adapter.setFrameFormat(previewFormat);
        cameraListener = adapter;
    }

    /**
     * This method sets the maximum size that camera frame is allowed to be. When selecting
     * size - the biggest size which less or equal the size set will be selected.
     * As an example - we set setMaxFrameSize(200,200) and we have 176x152 and 320x240 sizes. The
     * preview frame will be selected with 176x152 size.
     * This method is useful when need to restrict the size of preview frame for some reason (for example for video recording)
     * @param maxWidth - the maximum width allowed for camera frame.
     * @param maxHeight - the maximum height allowed for camera frame
     */
    public void setMaxFrameSize(int maxWidth, int maxHeight) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    public void SetCaptureFormat(int format) {
        previewFormat = format;
        if (cameraListener instanceof CvCameraViewListenerAdapter) {
            CvCameraViewListenerAdapter adapter = (CvCameraViewListenerAdapter) cameraListener;
            adapter.setFrameFormat(previewFormat);
        }
    }

    /** Called when syncObject lock is held **/
    private void checkCurrentState() {
        int targetState;

        if (enabled && surfaceExist && getVisibility() == VISIBLE) {
            targetState = STARTED;
        } else {
            targetState = STOPPED;
        }

        if (targetState != state) {
            // The state change detected. Need to exit the current state and enter target state
            processExitState(state);
            state = targetState;
            processEnterState(state);
        }
    }

    private void processEnterState(int state) {
        switch(state) {
        case STARTED:
            onEnterStartedState();
            if (cameraListener != null) {
                cameraListener.onCameraViewStarted(frameWidth, frameHeight);
            }
            break;
        case STOPPED:
            onEnterStoppedState();
            if (cameraListener != null) {
                cameraListener.onCameraViewStopped();
            }
            break;
        };
    }

    private void processExitState(int state) {
        switch(state) {
        case STARTED:
            onExitStartedState();
            break;
        case STOPPED:
            onExitStoppedState();
            break;
        };
    }

    private void onEnterStoppedState() { }
    private void onExitStoppedState() { }

    // NOTE: The order of bitmap constructor and camera connection is important for android 4.1.x
    // Bitmap must be constructed before surface
    private void onEnterStartedState() {
        /* Connect camera */
        if (!connectCamera(getWidth(), getHeight())) {
            AlertDialog ad = new AlertDialog.Builder(getContext()).create();
            ad.setCancelable(false); // This blocks the 'BACK' button
            ad.setMessage("It seems that you device does not support camera (or it is locked). Application will be closed.");
            ad.setButton(DialogInterface.BUTTON_NEUTRAL,  "OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    ((Activity) getContext()).finish();
                }
            });
            ad.show();

        }
    }

    private void onExitStartedState() {
        disconnectCamera();
        if (cacheBitmap != null) {
            cacheBitmap.recycle();
        }
    }

    /**
     * This method shall be called by the subclasses when they have valid
     * object and want it to be delivered to external client (via callback) and
     * then displayed on the screen.
     * @param frame - the current frame to be delivered
     */
    protected void deliverAndDrawFrame(CvCameraViewFrame frame) {
        Mat modified;

        if (cameraListener != null) {
            modified = cameraListener.onCameraFrame(frame);
        } else {
            modified = frame.rgba();
        }

        boolean bmpValid = true;
        if (modified != null) {
            try {
                Utils.matToBitmap(modified, cacheBitmap);
            } catch(Exception e) {
                Log.e(TAG, "Mat type: " + modified);
                Log.e(TAG, "Bitmap type: " + cacheBitmap.getWidth() + "*" + cacheBitmap.getHeight());
                Log.e(TAG, "Utils.matToBitmap() throws an exception: " + e.getMessage());
                bmpValid = false;
            }
        }

        if (bmpValid && cacheBitmap != null) {
            Canvas canvas = getHolder().lockCanvas();
            if (canvas != null) {
                canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
                canvas.drawBitmap(cacheBitmap, (canvas.getWidth() - cacheBitmap.getWidth()) / 2, (canvas.getHeight() - cacheBitmap.getHeight()) / 2, null);
                if (fpsMeter != null) {
                    fpsMeter.measure();
                    fpsMeter.draw(canvas, 20, 40);
                }
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    /**
     * This method is invoked shall perform concrete operation to initialize the camera.
     * CONTRACT: as a result of this method variables mFrameWidth and mFrameHeight MUST be
     * initialized with the size of the Camera frames that will be delivered to external processor.
     * @param width - the width of this SurfaceView
     * @param height - the height of this SurfaceView
     */
    protected abstract boolean connectCamera(int width, int height);

    /** Disconnects and release the particular camera object being connected to this surface view.
     * Called when syncObject lock is held **/
    protected abstract void disconnectCamera();

    // NOTE: On Android 4.1.x the function must be called before SurfaceTextre constructor!
    protected void AllocateCache() {
        cacheBitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888);
    }

    public interface ListItemAccessor {
        public int getWidth(Object obj);
        public int getHeight(Object obj);
    };

    /** This helper method can be called by subclasses to select camera preview size.
     * It goes over the list of the supported preview sizes and selects the maximum one which
     * fits both values set via setMaxFrameSize() and surface frame allocated for this view **/
    protected Size calculateCameraFrameSize(List<?> supportedSizes, ListItemAccessor accessor, int surfaceWidth, int surfaceHeight) {
        int calculatedWidth = 0;
        int calculatedHeight = 0;

        int maxAllowedWidth = (maxWidth != MAX_UNSPECIFIED && maxWidth < surfaceWidth)? maxWidth : surfaceWidth;
        int maxAllowedHeight = (maxHeight != MAX_UNSPECIFIED && maxHeight < surfaceHeight)? maxHeight : surfaceHeight;

        for (Object size : supportedSizes) {
            int width = accessor.getWidth(size);
            int height = accessor.getHeight(size);

            if (width <= maxAllowedWidth && height <= maxAllowedHeight) {
                if (width >= calculatedWidth && height >= calculatedHeight) {
                    calculatedWidth = (int) width;
                    calculatedHeight = (int) height;
                }
            }
        }

        return new Size(calculatedWidth, calculatedHeight);
    }
}
