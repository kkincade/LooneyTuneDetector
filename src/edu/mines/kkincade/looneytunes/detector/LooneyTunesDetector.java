package edu.mines.kkincade.looneytunes.detector;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;

import android.util.Log;

public class LooneyTunesDetector {
	
	private static final String TAG = "LOONEY_TUNES_DETECTOR";

	private Mat currentFrame;
	private List<Mat> trainingImages;
	private List<Point> usedKP;
	private List<KeyPoint> keypoints;
	private boolean[] flags;
	
	private int goodDetections; // If this is above our criteria for the minimum number of points, then we detected our Looney Tune
	
	// Keypoints
	private MatOfKeyPoint FrameKeypoints;
	private MatOfKeyPoint inputFrameKeypoints;
	
	private Mat FrameDescriptors;
	private List<Mat> inputFrameDescriptors;
	private Mat previousDescriptors;
	private List<MatOfDMatch> inputFrameMatches = new ArrayList<MatOfDMatch>();
	
	// Feature Detection Components
	private FeatureDetector detector;
	private DescriptorExtractor extractor;
	private DescriptorMatcher matcher;
	
	
	// Default Constructor
	LooneyTunesDetector() {
		flags = new boolean[3];
		flags[0] = false;
		flags[1] = false;
		flags[2] = false;
				
		FrameKeypoints = new MatOfKeyPoint();
		FrameDescriptors = new Mat();
		
		usedKP = new ArrayList<Point>();

		goodDetections = 0;
		detector = FeatureDetector.create(FeatureDetector.FAST);
		extractor = DescriptorExtractor.create(FeatureDetector.SURF);
		matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
	}
	

	// Custom constructor
	LooneyTunesDetector(List<Mat> images) {
		trainingImages = images;
		inputFrameKeypoints = new MatOfKeyPoint();
		inputFrameDescriptors = new ArrayList<Mat>();
		detector = FeatureDetector.create(FeatureDetector.FAST);
		extractor = DescriptorExtractor.create(FeatureDetector.SURF);
		matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
	}
	

	public void ProcessFrame(){
		detector.detect(currentFrame, FrameKeypoints);
		extractor.compute(currentFrame, FrameKeypoints, FrameDescriptors);
		keypoints = FrameKeypoints.toList();
	}

	
	public boolean Process(Mat PreviousDescriptors, String name) {
		previousDescriptors = PreviousDescriptors;
		matcher.knnMatch(FrameDescriptors, previousDescriptors, inputFrameMatches, 2);
		
		double minY = 9999, maxY = 0, minX = 9999, maxX = 0;
		
		for (int i = 0; i < inputFrameMatches.size(); i++) {
	    	DMatch[] atual = inputFrameMatches.get(i).toArray();
	    	
	    	for (int j = 0; j < inputFrameMatches.get(i).rows(); j++) {
	    		if (atual[0].distance * 2.0 < atual[1].distance) {
	    			
    				Point matchedPoint = keypoints.get(atual[0].queryIdx).pt;
    		
					goodDetections++;
					usedKP.add(matchedPoint);
					if (flags[1]) {
						double x = matchedPoint.x;
    					double y = matchedPoint.y;
    				
	    				if (x < minX) {
	    					minX = x;
	    				} else if (x > maxX ) {
	    					maxX = x;
	    				}
	    				
	    				if (y < minY) {
	    					minY = y;
	    				} else if (y > maxY ) {
	    					maxY = y;
	    				}
    				}
					
    				if (flags[0]) { //DrawMatches
    					Core.circle(currentFrame, keypoints.get(atual[0].queryIdx).pt ,6, new Scalar(255, 0, 255));
    				}	
	    		}
	    	}
	    }
		
		int pts = 9;
		
		if (FrameKeypoints.size().height > 5000) {
			pts = 17;
		} else if (FrameKeypoints.size().height > 9000) {
			pts = 30;
		}
		
		if (goodDetections >= pts) {
			return true;
		} else {
			goodDetections = 0;
		}
		
		return false;
	}


	public void drawMatches() { flags[0] = true; }
	public void drawSquare() { flags[1] = true;	}
	
	
	public boolean clean() {
		goodDetections = 0;
		return true;
	}

	
	public void drawKeypoints(){
		Features2d.drawKeypoints(currentFrame, FrameKeypoints, currentFrame);
		//Features2d.drawKeypoints(Frame, FrameKeypoints, Frame, new Scalar(255, 255, 255), Features2d.DRAW_RICH_KEYPOINTS);
	}

	
	public void Debug() {
		Log.d(TAG, "Total Keypoints"+ FrameKeypoints.size().height);
		Core.putText(currentFrame, "Total Keypoints: " + FrameKeypoints.size(), new Point(10, 100), 5, 1.8, new Scalar(255, 255, 255));
	}


	/** Computes keypoints and descriptors for all training images to compare to our input frame **/
	public void analyzeTrainingImages() {
		for (int c = 0; c < trainingImages.size(); c++) {
			Mat trainingImage = trainingImages.get(c);
			Mat trainingImageDescriptors = new Mat();
			detector.detect(trainingImage, inputFrameKeypoints);
			extractor.compute(trainingImage, inputFrameKeypoints, trainingImageDescriptors);
			inputFrameDescriptors.add(trainingImageDescriptors);
		}	
	}
	
	
	/** -------------------------------------------- Getters and Setters -------------------------------------------- **/
	
	public int getNumberOfGoodDetections() {
		int x = goodDetections;
		goodDetections = 0;
		return x;
	}
	public Mat getKeypoints() { return inputFrameKeypoints; }
	public Mat getCurrentFrame() { return currentFrame; }
	public void setCurrentFrame(Mat frame) { currentFrame = frame; }
	public List<Mat> getDescriptors() { return inputFrameDescriptors; }

}