package edu.mines.kkincade.looneytunes.detector;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import edu.mines.kkincade.looneytunes.detector.R;
import edu.mines.kkincade.looneytunes.extended.AdapterTwoLinesImage;
import edu.mines.kkincade.looneytunes.extended.ViewExtended;
import edu.mines.kkincade.looneytunes.extended.CameraBridgeViewBaseExtended.CvCameraViewFrame;
import edu.mines.kkincade.looneytunes.extended.CameraBridgeViewBaseExtended.CvCameraViewListener2;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity implements CvCameraViewListener2, OnTouchListener, OnClickListener, OnItemClickListener {

	private static final String TAG = "LOONEY_TUNES_DETECTOR";
	private static final int BUGS_BUNNY_ID = 0;
	private static final int LOLA_BUNNY_ID = 1;
//	private static final int FOGHORN_LEGHORN_ID = 2;
	private static final int YOSEMITE_SAM_ID = 2;
	private static final int TASMANIAN_DEVIL_ID = 3;
	
	private static final String BUGS_BUNNY = "Bugs Bunny";
	private static final String LOLA_BUNNY = "Lola Bunny";
//	private static final String FOGHORN_LEGHORN = "Foghorn Leghorn";
	private static final String YOSEMITE_SAM = "Yosemite Sam";
	private static final String TASMANIAN_DEVIL = "Tasmanian Devil";
	
	private static final int FULL_DETECTION_VISIBLE = 100;
	private static final int FULL_DETECTION_GONE = 101;
	
	private ViewExtended cameraView;
	private Handler mainHandler; // This Handler answers messages from other threads

	// Full Detection
	private LinearLayout Loading;

	// Buttons
	private Button fullDetectionButton;
	private Button clearListButton;

	// Options Menu
	private SubMenu colorEffectsMenu;
	private SubMenu resolutionMenu;
//	 private SubMenu detectionModeMenu;
	private SubMenu debugMenu;
	private MenuItem[] colorEffectMenuItems;
	private MenuItem[] resolutionMenuItems;
//	 private MenuItem[] detectionModeMenuItems;
	private MenuItem[] debugMenuItems;
	private List<Size> resolutionMenuChoices;

	private LooneyTunesDetector trainingImagesDetector;
	
	// Training Images
	private List<Mat> trainingImages;
	private Mat bugsBunny;
	private Mat lolaBunny;
	private Mat yosemiteSam;
//	private Mat foghornLeghorn;
	private Mat tasmanianDevil;
	
	private boolean[] detectedCharacters; // An array of boolean that keep track of which characters have been detected
	private boolean[] threadControl; // An array of booleans that keep track of which threads are running?

	private AdapterTwoLinesImage adapter;
	private TextView statusTextView;
	private String detectionMode;
//	private int test1 = 1;
	private boolean debug;

	private ListView detectedCharactersListView;

	private ArrayList<ObjectList> detectedCharactersList;
	private boolean[] reportedCharacterAsFound;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "ON_CREATE");
		
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);

		// Variables
		detectedCharactersList = new ArrayList<ObjectList>();
		adapter = new AdapterTwoLinesImage(getApplicationContext(), detectedCharactersList);
		reportedCharacterAsFound = new boolean[30];
		debug = false;
		detectionMode = "Parallel";
		
		// Initialize our CameraView
		cameraView = (ViewExtended) findViewById(R.id.ExtendedSurfaceView);
		cameraView.setVisibility(SurfaceView.VISIBLE);
		cameraView.setCvCameraViewListener(this);

		// Initialize Layout Objects
		Loading = (LinearLayout) findViewById(R.id.Loading);
		Loading.setVisibility(View.GONE);
		statusTextView = (TextView) findViewById(R.id.text_status);
		statusTextView.setOnClickListener(this);
		fullDetectionButton = (Button) findViewById(R.id.fullDetection);
		fullDetectionButton.setOnClickListener(this);
		clearListButton = (Button) findViewById(R.id.CleanList);
		clearListButton.setOnClickListener(this);
		detectedCharactersListView = (ListView) findViewById(R.id.DetectedObjects);
		detectedCharactersListView.setOnItemClickListener(this);
		detectedCharactersListView.setAdapter(adapter);
		
		// Message handler for threads
		mainHandler = new MainHandler(this);
	}

	
	/** This handler receives messages from other threads. It is notified with a message when a thread has
	 * detected a Looney Tunes character. Based on which thread sent the message, it creates an appropriate
	 * CharacterDialog. **/
	private static class MainHandler extends Handler {
		private final WeakReference<MainActivity> mainActivityContext;
		private MainActivity mainActivity;
		
		public MainHandler(MainActivity context) {
			mainActivityContext = new WeakReference<MainActivity>((MainActivity) context);
			mainActivity = mainActivityContext.get();
		}
		
		@Override
		public void handleMessage(Message message) {
			// Detected Bugs Bunny
			if (message.what == BUGS_BUNNY_ID && !mainActivity.reportedCharacterAsFound[message.what]) {
				mainActivity.detectedCharactersList.add(new ObjectList(R.drawable.bugs_bunny, BUGS_BUNNY, message.obj.toString(),"file:///android_asset/Homer.html"));
				mainActivity.reportedCharacterAsFound[BUGS_BUNNY_ID] = true;
				AdapterTwoLinesImage adapter = new AdapterTwoLinesImage(mainActivity.getApplicationContext(), mainActivity.detectedCharactersList);
				mainActivity.detectedCharactersListView.setAdapter(adapter);
				
			// Detected Lola Bunny
			} else if (message.what == LOLA_BUNNY_ID && !mainActivity.reportedCharacterAsFound[message.what]) {
				mainActivity.detectedCharactersList.add(new ObjectList(R.drawable.lola_bunny, LOLA_BUNNY, message.obj.toString(),"file:///android_asset/Homer.html"));
				mainActivity.reportedCharacterAsFound[LOLA_BUNNY_ID] = true;
				AdapterTwoLinesImage adapter = new AdapterTwoLinesImage(mainActivity.getApplicationContext(), mainActivity.detectedCharactersList);
				mainActivity.detectedCharactersListView.setAdapter(adapter);
				
			// Detected Foghorn Leghorn
//			} else if (message.what == FOGHORN_LEGHORN_ID && !mainActivity.objControl[message.what]) {
//				mainActivity.detectedCharactersList.add(new ObjectList(R.drawable.foghorn_leghorn, FOGHORN_LEGHORN, message.obj.toString(),"file:///android_asset/Homer.html"));
//				mainActivity.reportedCharacterAsFound[FOGHORN_LEGHORN_ID] = true;
//				AdapterTwoLinesImage adapter = new AdapterTwoLinesImage(mainActivity.getApplicationContext(), mainActivity.detectedCharactersList);
//				mainActivity.detectedCharactersListView.setAdapter(adapter);
				
			// Detected Yosemite Sam
			} else if (message.what == YOSEMITE_SAM_ID && !mainActivity.reportedCharacterAsFound[message.what]) {
				mainActivity.detectedCharactersList.add(new ObjectList(R.drawable.yosemite_sam, YOSEMITE_SAM, message.obj.toString(),"file:///android_asset/Homer.html"));
				mainActivity.reportedCharacterAsFound[YOSEMITE_SAM_ID] = true;
				AdapterTwoLinesImage adapter = new AdapterTwoLinesImage(mainActivity.getApplicationContext(), mainActivity.detectedCharactersList);
				mainActivity.detectedCharactersListView.setAdapter(adapter);
				
			// Detected Tasmanian Devil
			} else if (message.what == TASMANIAN_DEVIL_ID && !mainActivity.reportedCharacterAsFound[message.what]) {
				mainActivity.detectedCharactersList.add(new ObjectList(R.drawable.tasmanian_devil, TASMANIAN_DEVIL, message.obj.toString(),"file:///android_asset/Homer.html"));
				mainActivity.reportedCharacterAsFound[TASMANIAN_DEVIL_ID] = true;
				AdapterTwoLinesImage adapter = new AdapterTwoLinesImage(mainActivity.getApplicationContext(), mainActivity.detectedCharactersList);
				mainActivity.detectedCharactersListView.setAdapter(adapter);
				
			} else if(message.what == FULL_DETECTION_VISIBLE) {
				mainActivity.Loading.setVisibility(View.VISIBLE);
			} else if(message.what == FULL_DETECTION_GONE) {
				mainActivity.Loading.setVisibility(View.GONE);
			} else if(message.what == 200) {
				if (!mainActivity.threadControl[Integer.parseInt(message.obj.toString())]) {
					mainActivity.threadControl[Integer.parseInt(message.obj.toString())] = true;
				} else {
					mainActivity.threadControl[Integer.parseInt(message.obj.toString())] = false;
				}
			}
		}
	};

	
	/** ---------------------------------------- CvCameraViewListener Methods ------------------------------------------- **/
	
	/** Loads and initializes the training images used to identify the Looney Tunes **/
	public void onCameraViewStarted(int width, int height) {
		Log.d(TAG, "ON_CAMERA_VIEW_STARTED");

		// Load training images
		trainingImages = new ArrayList<Mat>();
		bugsBunny = new Mat();
		lolaBunny = new Mat();
//		foghornLeghorn = new Mat();
		yosemiteSam = new Mat();
		tasmanianDevil = new Mat();

		try {
			// Bugs Bunny
			Mat rawBugsBunny = Utils.loadResource(this, R.drawable.bugs_bunny);
			Imgproc.cvtColor(rawBugsBunny, bugsBunny, Imgproc.COLOR_RGB2BGR);
			trainingImages.add(bugsBunny); // Index: 0
			
			// Lola Bunny
			Mat rawLolaBunny = Utils.loadResource(this, R.drawable.lola_bunny);
			Imgproc.cvtColor(rawLolaBunny, lolaBunny, Imgproc.COLOR_RGB2BGR);
			trainingImages.add(lolaBunny); // Index: 1
			
			// Foghorn Leghorn
//			Mat rawFoghornLeghorn = Utils.loadResource(this, R.drawable.foghorn_leghorn);
//			Imgproc.cvtColor(rawFoghornLeghorn, foghornLeghorn, Imgproc.COLOR_RGB2BGR);
//			trainingImages.add(foghornLeghorn); // Index: 2
			
			// Yosemite Sam
			Mat rawYosemiteSam = Utils.loadResource(this, R.drawable.yosemite_sam);
			Imgproc.cvtColor(rawYosemiteSam, yosemiteSam, Imgproc.COLOR_RGB2BGR);
			trainingImages.add(yosemiteSam); // Index: 3
			
			// Tasmanian Devil
			Mat rawTasmanianDevil = Utils.loadResource(this, R.drawable.tasmanian_devil);
			Imgproc.cvtColor(rawTasmanianDevil, tasmanianDevil, Imgproc.COLOR_RGB2BGR);
			trainingImages.add(tasmanianDevil); // Index: 4		
			
			trainingImagesDetector = new LooneyTunesDetector(trainingImages);
			trainingImagesDetector.analyzeTrainingImages();

			detectedCharacters = new boolean[trainingImages.size()];
			threadControl = new boolean[trainingImages.size()];
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void onCameraViewStopped() { }


	@Override
	/** This method is called every time our camera receives an input frame **/
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		final Mat rgbInputFrame = inputFrame.rgba();

		if (detectionMode.equals("Parallel")) {
			
			// --------------------- Bugs Bunny Thread --------------------- 
			Thread bugsBunnyThread = new Thread(new Runnable() {
				@Override
				public void run() {
					LooneyTunesDetector looneyTuneDetector = new LooneyTunesDetector();
					looneyTuneDetector.setCurrentFrame(rgbInputFrame); // Make this input frame the current frame
					looneyTuneDetector.processFrame(); // Process the new frame
					
					looneyTuneDetector.drawMatches();
					looneyTuneDetector.drawSquare();
					
					// If Process() returned true, we found a character
					if (looneyTuneDetector.process(trainingImagesDetector.getDescriptors().get(BUGS_BUNNY_ID), BUGS_BUNNY)) {
						detectedCharacters[BUGS_BUNNY_ID] = true;
						
						// Send message to handler to let it know we found a match
						Message message = new Message();
						message.what = BUGS_BUNNY_ID;
						message.obj = looneyTuneDetector.getNumberOfGoodDetections();
						mainHandler.sendMessage(message);
					}
				}
			}, BUGS_BUNNY);

			if ((getThreadByName(BUGS_BUNNY) == null) && !detectedCharacters[BUGS_BUNNY_ID] ) {
				bugsBunnyThread.start();
			}
			
			// --------------------- Lola Bunny Thread ---------------------
			Thread lolaBunnyThread = new Thread(new Runnable() {
				@Override
				public void run() {
					LooneyTunesDetector looneyTuneDetector = new LooneyTunesDetector();
					looneyTuneDetector.setCurrentFrame(rgbInputFrame); // Make this input frame the current frame
					looneyTuneDetector.processFrame(); // Process the new frame
					
					looneyTuneDetector.drawMatches();
					looneyTuneDetector.drawSquare();
					
					// If Process() returned true, we found a character
					if (looneyTuneDetector.process(trainingImagesDetector.getDescriptors().get(LOLA_BUNNY_ID), LOLA_BUNNY)) {
						detectedCharacters[LOLA_BUNNY_ID] = true;
						
						// Send message to handler to let it know we found a match
						Message message = new Message();
						message.what = LOLA_BUNNY_ID;
						message.obj = looneyTuneDetector.getNumberOfGoodDetections();
						mainHandler.sendMessage(message);
					}
				}
			}, LOLA_BUNNY);

			if ((getThreadByName(LOLA_BUNNY) == null) && !detectedCharacters[LOLA_BUNNY_ID] ) {
				lolaBunnyThread.start();
			}

			// --------------------- Foghorn Leghorn Thread ---------------------
//			Thread foghornLeghornThread = new Thread(new Runnable() {
//				@Override
//				public void run() {
//					LooneyTunesDetector looneyTuneDetector = new LooneyTunesDetector();
//					looneyTuneDetector.setCurrentFrame(rgbInputFrame); // Make this input frame the current frame
//					looneyTuneDetector.processFrame(); // Process the new frame
//					
//					looneyTuneDetector.drawMatches();
//					looneyTuneDetector.drawSquare();
//					
//					// If Process() returned true, we found a character
//					if (looneyTuneDetector.process(trainingImagesDetector.getDescriptors().get(FOGHORN_LEGHORN_ID), FOGHORN_LEGHORN)) {
//						detectedCharacters[FOGHORN_LEGHORN_ID] = true;
//						
//						// Send message to handler to let it know we found a match
//						Message message = new Message();
//						message.what = FOGHORN_LEGHORN_ID;
//						message.obj = looneyTuneDetector.getNumberOfGoodDetections();
//						mainHandler.sendMessage(message);
//					}
//				}
//			}, FOGHORN_LEGHORN);
//
//			if ((getThreadByName(FOGHORN_LEGHORN) == null) && !detectedCharacters[FOGHORN_LEGHORN_ID] ) {
//				foghornLeghornThread.start();
//			}

			// --------------------- Yosemite Sam Thread ---------------------
			Thread yosemiteSamThread = new Thread(new Runnable() {
				@Override
				public void run() {
					LooneyTunesDetector looneyTuneDetector = new LooneyTunesDetector();
					looneyTuneDetector.setCurrentFrame(rgbInputFrame); // Make this input frame the current frame
					looneyTuneDetector.processFrame(); // Process the new frame
					
					looneyTuneDetector.drawMatches();
					looneyTuneDetector.drawSquare();
					
					// If Process() returned true, we found a character
					if (looneyTuneDetector.process(trainingImagesDetector.getDescriptors().get(YOSEMITE_SAM_ID), YOSEMITE_SAM)) {
						detectedCharacters[YOSEMITE_SAM_ID] = true;
						
						// Send message to handler to let it know we found a match
						Message message = new Message();
						message.what = YOSEMITE_SAM_ID;
						message.obj = looneyTuneDetector.getNumberOfGoodDetections();
						mainHandler.sendMessage(message);
					}
				}
			}, YOSEMITE_SAM);

			if ((getThreadByName(YOSEMITE_SAM) == null) && !detectedCharacters[YOSEMITE_SAM_ID] ) {
				yosemiteSamThread.start();
			}

			// --------------------- Tasmanian Devil Thread ---------------------
			Thread tasmanianDevilThread = new Thread(new Runnable() {
				@Override
				public void run() {
					LooneyTunesDetector looneyTuneDetector = new LooneyTunesDetector();
					looneyTuneDetector.setCurrentFrame(rgbInputFrame); // Make this input frame the current frame
					looneyTuneDetector.processFrame(); // Process the new frame
					
					looneyTuneDetector.drawMatches();
					looneyTuneDetector.drawSquare();
					
					// If Process() returned true, we found a character
					if (looneyTuneDetector.process(trainingImagesDetector.getDescriptors().get(TASMANIAN_DEVIL_ID), TASMANIAN_DEVIL)) {
						detectedCharacters[TASMANIAN_DEVIL_ID] = true;
						
						// Send message to handler to let it know we found a match
						Message message = new Message();
						message.what = TASMANIAN_DEVIL_ID;
						message.obj = looneyTuneDetector.getNumberOfGoodDetections();
						mainHandler.sendMessage(message);
					}
				}
			}, TASMANIAN_DEVIL);

			if ((getThreadByName(TASMANIAN_DEVIL) == null) && !detectedCharacters[TASMANIAN_DEVIL_ID] ) {
				tasmanianDevilThread.start();
			}
			
		} else if (detectionMode.equals("Sequential")) {

			LooneyTunesDetector looneyTunesDetector = new LooneyTunesDetector();
			looneyTunesDetector.setCurrentFrame(rgbInputFrame);
			looneyTunesDetector.processFrame();
			
			if (debug) { looneyTunesDetector.drawMatches(); }

			if (looneyTunesDetector.process(trainingImagesDetector.getDescriptors().get(BUGS_BUNNY_ID), BUGS_BUNNY)) {
				detectedCharacters[BUGS_BUNNY_ID] = true;
				Message message = new Message();
				message.what = BUGS_BUNNY_ID;
				message.obj=looneyTunesDetector.getNumberOfGoodDetections();
				mainHandler.sendMessage(message);
			}
			
			if (debug) { looneyTunesDetector.Debug(); }
			
			looneyTunesDetector.getCurrentFrame();
			
		} else if(detectionMode.equals("Circle Detection")) {
			Process.CircleDetection(inputFrame, rgbInputFrame);
		}

		return rgbInputFrame;
	}
	

	/** ------------------------------------------ Helper Methods ------------------------------------------ **/

	private Thread getThreadByName(String threadName) {
		Thread __tmp = null;
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
		for (int i = 0; i < threadArray.length; i++)
			if (threadArray[i].getName().equals(threadName))
				__tmp =  threadArray[i];
		return __tmp;
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(TAG, "ON_CREATE_OPTIONS_MENU");
		
		List<String> effects = cameraView.getEffectList();
		
		// Setup Options Menu
		debugMenu = menu.addSubMenu("Debug");
		debugMenuItems = new MenuItem[4];
		debugMenuItems[0] = debugMenu.add(4, 0, Menu.NONE,"Enable");
		debugMenuItems[1] = debugMenu.add(4, 0, Menu.NONE,"Disable");
		debugMenuItems[2] = debugMenu.add(4, 0, Menu.NONE,"Show FPS");
		debugMenuItems[3] = debugMenu.add(4, 0, Menu.NONE,"Hide FPS");

//		detectionModeMenu = menu.addSubMenu("Detection Mode");
//		detectionModeMenuItems = new MenuItem[3];
//		detectionModeMenuItems[0] = detectionModeMenu.add(3, 0, Menu.NONE,"Parallel");
//		detectionModeMenuItems[1] = detectionModeMenu.add(3, 1, Menu.NONE,"Sequential");
//		detectionModeMenuItems[2] = detectionModeMenu.add(3, 2, Menu.NONE,"Circle Detection");

		if (effects == null) {
			Log.e(TAG, "Color effects are not supported by device!");
			return true;
		}

		colorEffectsMenu = menu.addSubMenu("Color Effect");
		colorEffectMenuItems = new MenuItem[effects.size()];

		int index = 0;
		ListIterator<String> effectItr = effects.listIterator();
		while (effectItr.hasNext()) {
			String element = effectItr.next();
			colorEffectMenuItems[index] = colorEffectsMenu.add(1, index, Menu.NONE, element);
			index++;
		}

		resolutionMenu = menu.addSubMenu("Resolution");
		resolutionMenuChoices = cameraView.getResolutionList();
		resolutionMenuItems = new MenuItem[resolutionMenuChoices.size()];

		ListIterator<Size> resolutionItr = resolutionMenuChoices.listIterator();
		index = 0;
		while (resolutionItr.hasNext()) {
			Size element = resolutionItr.next();
			resolutionMenuItems[index] = resolutionMenu.add(2, index, Menu.NONE, Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
			index++;
		}

		return true;
	}

	
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
		
		// Color effects item selected
		if (item.getGroupId() == 1) {
			cameraView.setEffect((String) item.getTitle());
			Toast.makeText(this, cameraView.getEffect(), Toast.LENGTH_SHORT).show();
		
		// Resolution item selected
		} else if (item.getGroupId() == 2) {
			int id = item.getItemId();
			Size resolution = resolutionMenuChoices.get(id);
			String res = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
			Toast.makeText(this, res, Toast.LENGTH_SHORT).show();
			cameraView.setResolution(resolution);
			resolution = cameraView.getResolution();
			String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
			Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
			
		// Detection mode item selected
		} else if(item.getGroupId() == 3) {
			detectionMode = (String) item.getTitle();
		
		// Debug item selected
		} else if(item.getGroupId() == 4) {
			if ((String) item.getTitle() == "Disable") {
				debug = false;
			} else if((String) item.getTitle() == "Show FPS") {
				Toast.makeText(this, "Showing FPS...", Toast.LENGTH_SHORT).show();
				cameraView.enableFpsMeter();
			} else if((String) item.getTitle() == "Hide FPS") {
				cameraView.disableFpsMeter();
			} else {
				debug = true;
			}
		}

		return true;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {

//		final int action = event.getAction();
//		switch (action & MotionEvent.ACTION_MASK) {
//			case MotionEvent.ACTION_DOWN: {
//				//Toast.makeText(this,"Touch", Toast.LENGTH_SHORT).show();
//				if (test1 == 1) {
//					test1 = 2 ;
//				} else if (test1 == 2) {
//					test1 = 0;
//				} else {
//					test1 = 1;
//				}
//				break;
//			} case MotionEvent.ACTION_MOVE: {
//				//teste1 = (int) event.getX();
//				//teste2 = (int) event.getY();
//				//break;
//			}
//		}
		Log.d(TAG,"onTouch event");

		return true;
	}


	@Override
	/** Callback method for our "Clear List" and "Full Detection" buttons **/
	public void onClick(View v) {
		if (v == statusTextView) {
			Toast.makeText(this,"Click on " + statusTextView.getText(), Toast.LENGTH_SHORT).show();
		} else if (v == fullDetectionButton) {
			Toast.makeText(this,"Take a picture and do a Full Detection", Toast.LENGTH_SHORT).show();

			//SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
			//String currentDateandTime = sdf.format(new Date());
			//String fileName = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Photo/temp_picture_" + currentDateandTime + ".jpg";
			//String fileName = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Photo/temp_picture_" + "processing" + ".jpg";

			//CameraView.takePicture(fileName);
			//Toast.makeText(this, fileName + " saved", Toast.LENGTH_SHORT).show();

			doFullDetection();

			//File file = new File(fileName);
			//boolean deleted = file.delete();

		} else if (v == clearListButton) {
			Toast.makeText(this, "List Cleared", Toast.LENGTH_SHORT).show();
			
			// Clear our detected characters
			reportedCharacterAsFound = new boolean[30];
			detectedCharactersList = new ArrayList<ObjectList>();
			AdapterTwoLinesImage adapter = new AdapterTwoLinesImage(getApplicationContext(), detectedCharactersList);
			detectedCharactersListView.setAdapter(adapter);
			for (int c = 0; c < detectedCharacters.length; c++) {
				detectedCharacters[c] = false;
			}
		}
	}

	
	// Full Detection allows you to take a picture to use as your own reference image
	private void doFullDetection() {

		Thread fullDetectionThread = new Thread(new Runnable() {

			@Override
			public void run() {
				Message VISIBLE = new Message();
				VISIBLE.what = FULL_DETECTION_VISIBLE;
				mainHandler.sendMessage(VISIBLE);

				String fileName = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Photo/temp_picture_" + "processing" + ".jpg";

				cameraView.takePicture(fileName);
				
				File File = new File(fileName);
				Mat BaseImage = new Mat();

				BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
				bitmapOptions.inPreferredConfig = Config.RGB_565;
				bitmapOptions.inSampleSize = 2;
				Bitmap bitmap;
				bitmap = BitmapFactory.decodeFile(File.getAbsolutePath(), bitmapOptions);
				
				if (bitmap != null) {
					Log.d(TAG, File.getAbsolutePath());
					Utils.bitmapToMat(bitmap, BaseImage);

					LooneyTunesDetector looneyTunesDetector = new LooneyTunesDetector();
					looneyTunesDetector.setCurrentFrame(BaseImage);
					looneyTunesDetector.drawSquare();
					looneyTunesDetector.drawMatches();
					looneyTunesDetector.processFrame();
					
					for (int c = 0; c < trainingImages.size(); c++) {
						if (!detectedCharacters[0] && (looneyTunesDetector.process( trainingImagesDetector.getDescriptors().get(0), "Bugs") && looneyTunesDetector.clear())) {
							detectedCharacters[0] = true;
							Message message = new Message();
							message.what = BUGS_BUNNY_ID;
							mainHandler.sendMessage(message);
						}
					}
					BaseImage.release();
				}
				
				bitmap.recycle();
				
				Message GONE = new Message();
				GONE.what = FULL_DETECTION_GONE;
				mainHandler.sendMessage(GONE);
			}
		}, "FullDetection Thread");

		fullDetectionThread.start();
	}

	
	@Override
	public void onItemClick(AdapterView<?> av, View arg1, int position, long arg3) {
		//AdapterTwoLinesImage atualAdapter =  (AdapterTwoLinesImage)av.getAdapter();
		//String atualAdress = atualAdapter.personal.get(position).getAdress();
		//String atualName = atualAdapter.personal.get(position).getTitle();
		//CharDialog atual = new CharDialog(this, atualName, atualAdress);
		//atual.show();
		//Toast.makeText(this, atualAdapter.personal.get(position).getAdress() + "", Toast.LENGTH_SHORT).show();

	}


	/** ------------------------------------------- Activity Methods ------------------------------------------- **/

	@Override
	public void onPause() {
		super.onPause();
		if (cameraView != null) {
			cameraView.disableView();
		}
	}

	@Override
	public void onResume() {
		super.onResume();        
		if (OpenCVLoader.initDebug()) {
			cameraView.enableView();
			cameraView.setOnTouchListener(MainActivity.this);
		}
	}

	public void onDestroy() {
		super.onDestroy();
		if (cameraView != null) {
			cameraView.disableView();
		}
	}

}
