package edu.mines.kkincade.looneytunes.extended;

import java.util.List;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


/**
 * This class is an implementation of the Bridge View between OpenCV and Java Camera.
 * This class relies on the functionality available in base class and only implements required functions:
 * connectCamera - opens Java camera and sets the PreviewCallback to be delivered.
 * disconnectCamera - closes the camera and stops preview.
 * 
 * When frame is delivered via callback from Camera - it processed via OpenCV to be
 * converted to RGBA32 and then passed to the external callback for modifications if required.
 */
public class JavaCameraViewExtended extends CameraBridgeViewBaseExtended implements PreviewCallback {

    private static final int MAGIC_TEXTURE_ID = 10;
    private static final String TAG = "JavaCameraView";

    private byte buffer[];
    private Mat[] frameChain;
    private int chainID = 0;
    private Thread thread;
    private boolean stopThread;

    protected Camera camera;
    protected JavaCameraFrame cameraFrame;
    private SurfaceTexture surfaceTexture;
    
    // Constructors
    public JavaCameraViewExtended(Context context, int cameraID) { super(context, cameraID); }
    public JavaCameraViewExtended(Context context, AttributeSet attrs) { super(context, attrs); }

    
    protected boolean initializeCamera(int width, int height) {
        Log.d(TAG, "Initialize Java Camera");
        boolean result = true;
        
        // Synchronize requires a lock for our JavaCameraViewExtended
        synchronized (this) {
            camera = null;

            if (cameraIndex == -1) {
                Log.d(TAG, "Trying to open camera with old open()");
                try {
                    camera = Camera.open();
                }
                catch (Exception e){
                    Log.e(TAG, "Camera is not available (in use or does not exist): " + e.getLocalizedMessage());
                }

                if (camera == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    boolean connected = false;
                    for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                        Log.d(TAG, "Trying to open camera with new open(" + Integer.valueOf(camIdx) + ")");
                        try {
                            camera = Camera.open(camIdx);
                            connected = true;
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Camera #" + camIdx + "failed to open: " + e.getLocalizedMessage());
                        }
                        if (connected) break;
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    Log.d(TAG, "Trying to open camera with new open(" + Integer.valueOf(cameraIndex) + ")");
                    try {
                        camera = Camera.open(cameraIndex);
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Camera #" + cameraIndex + "failed to open: " + e.getLocalizedMessage());
                    }
                }
            }

            if (camera == null) {
                return false;
            }

            /* Now set camera parameters */
            try {
                Camera.Parameters params = camera.getParameters();
                Log.d(TAG, "getSupportedPreviewSizes()");
                List<android.hardware.Camera.Size> sizes = params.getSupportedPreviewSizes();
                if (sizes != null) {
                    // Select the size that fits surface considering maximum size allowed
                    Size frameSize = calculateCameraFrameSize(sizes, new JavaCameraSizeAccessor(), width, height);

                    params.setPreviewFormat(ImageFormat.NV21);
                    Log.d(TAG, "Set preview size to " + Integer.valueOf((int)frameSize.width) + "x" + Integer.valueOf((int)frameSize.height));
                    params.setPreviewSize((int)frameSize.width, (int)frameSize.height);

                    List<String> FocusModes = params.getSupportedFocusModes();
                    if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    }
                    
                    
                    camera.setParameters(params);
                    params = camera.getParameters();
                    
                    frameWidth = params.getPreviewSize().width;
                    frameHeight = params.getPreviewSize().height;

                    if (fpsMeter != null) {
                        fpsMeter.setResolution(frameWidth, frameHeight);
                    }

                    int size = frameWidth * frameHeight;
                    size  = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
                    buffer = new byte[size];

                    camera.addCallbackBuffer(buffer);
                    camera.setPreviewCallbackWithBuffer(this);

                    frameChain = new Mat[2];
                    frameChain[0] = new Mat(frameHeight + (frameHeight/2), frameWidth, CvType.CV_8UC1);
                    frameChain[1] = new Mat(frameHeight + (frameHeight/2), frameWidth, CvType.CV_8UC1);

                    AllocateCache();

                    cameraFrame = new JavaCameraFrame(frameChain[chainID], frameWidth, frameHeight);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        surfaceTexture = new SurfaceTexture(MAGIC_TEXTURE_ID);
                        camera.setPreviewTexture(surfaceTexture);
                    } else
                       camera.setPreviewDisplay(null);

                    /* Finally we are ready to start the preview */
                    Log.d(TAG, "startPreview");
                    camera.startPreview();
                }
                else
                    result = false;
            } catch (Exception e) {
                result = false;
                e.printStackTrace();
            }
        }

        return result;
    }

    
    protected void releaseCamera() {
        synchronized (this) {
            if (camera != null) {
                camera.stopPreview();
                camera.release();
            }
            camera = null;
            if (frameChain != null) {
                frameChain[0].release();
                frameChain[1].release();
            }
            if (cameraFrame != null) {
                cameraFrame.release();
            }
        }
    }
    

    @Override
    /** Instantiates a camera, as well as a thread for that camera to receive frames on **/
    protected boolean connectCamera(int width, int height) {
        // First step - initialize camera connection
        Log.d(TAG, "Connecting to camera");
        if (!initializeCamera(width, height)) {
            return false;
        }

        // Update thread
        Log.d(TAG, "Starting processing thread");
        stopThread = false;
        thread = new Thread(new CameraWorker());
        thread.start();

        return true;
    }

    
    /** Notifies and stops the thread. Then stops and releases the camera **/
    protected void disconnectCamera() {
        Log.d(TAG, "Disconnecting from camera");
        try {
            stopThread = true;
            Log.d(TAG, "Notify thread");
            synchronized (this) {
                this.notify();
            }
            Log.d(TAG, "Wating for thread");
            if (thread != null) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            thread =  null;
        }

        /* Now release camera */
        releaseCamera();
    }

    
    public void onPreviewFrame(byte[] frame, Camera arg1) {
        synchronized (this) {
            frameChain[1 - chainID].put(0, 0, frame);
            this.notify();
        }
        
        if (camera != null) {
            camera.addCallbackBuffer(buffer);
        }
    }

    
    private class JavaCameraFrame implements CvCameraViewFrame {
        private Mat yuvFrameData;
        private Mat rgbaFrameData;
        private int cameraFrameWidth;
        private int cameraFrameHeight;
        
        public Mat gray() {
            return yuvFrameData.submat(0, cameraFrameHeight, 0, cameraFrameWidth);
        }

        public Mat rgba() {
            Imgproc.cvtColor(yuvFrameData, rgbaFrameData, Imgproc.COLOR_YUV2BGR_NV12, 4);
            return rgbaFrameData;
        }

        public JavaCameraFrame(Mat Yuv420sp, int width, int height) {
            super();
            cameraFrameWidth = width;
            cameraFrameHeight = height;
            yuvFrameData = Yuv420sp;
            rgbaFrameData = new Mat();
        }

        public void release() {
            rgbaFrameData.release();
        }
    };

    
    private class CameraWorker implements Runnable {
        public void run() {
            while (!stopThread) {
                synchronized (JavaCameraViewExtended.this) {
                    try {
                        JavaCameraViewExtended.this.wait();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (!stopThread) {
                    if (!frameChain[chainID].empty()) {
                        deliverAndDrawFrame(cameraFrame);
                    }
                    chainID = 1 - chainID;
                }
            }
        }
    }
    
    
    /** Class that allows us to get the width and height of the camera **/
    public static class JavaCameraSizeAccessor implements ListItemAccessor {
        public int getWidth(Object obj) {
            Camera.Size size = (Camera.Size) obj;
            return size.width;
        }

        public int getHeight(Object obj) {
            Camera.Size size = (Camera.Size) obj;
            return size.height;
        }
    }
}
