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
	private static final double MIN_RATIO = (1.0 / 1.5);
	
	
	
	private Mat currentFrame;
	private List<Mat> trainingImages;
	private List<Point> usedKP;
	private List<KeyPoint> keypoints;
	private boolean[] flags; // Flags for drawing matches, square, etc.
	
	private int numberOfMatchesNeeded = 6;
	private int goodDetections; // If this is above our criteria for the minimum number of points, then we detected our Looney Tune
	
	// Keypoints
	private MatOfKeyPoint currentFrameKeypoints;
	private MatOfKeyPoint trainingImageKeypoints;
	
	private Mat currentFrameDescriptors;
	private List<Mat> trainingImagesDescriptorsList;
	private Mat trainingImageDescriptors;
	private List<MatOfDMatch> currentFrameMatches = new ArrayList<MatOfDMatch>();
	
	// Feature Detection Components
	private FeatureDetector detector;
	private DescriptorExtractor extractor;
	private DescriptorMatcher matcher;
	
	
	// Default Constructor
	LooneyTunesDetector() {
		flags = new boolean[3];
		flags[0] = false; // Draw Matches
		flags[1] = false; // Draw Squares
		flags[2] = false;
				
		currentFrameKeypoints = new MatOfKeyPoint();
		currentFrameDescriptors = new Mat();
		
		usedKP = new ArrayList<Point>();

		goodDetections = 0;
		detector = FeatureDetector.create(FeatureDetector.ORB);
		extractor = DescriptorExtractor.create(FeatureDetector.ORB);
		matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
	}
	

	// Custom constructor that accepts a list of training images
	LooneyTunesDetector(List<Mat> images) {
		trainingImages = images;
		trainingImageKeypoints = new MatOfKeyPoint();
		trainingImagesDescriptorsList = new ArrayList<Mat>();
		detector = FeatureDetector.create(FeatureDetector.ORB);
		extractor = DescriptorExtractor.create(FeatureDetector.ORB);
		matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
	}
	

	public void processFrame(){
		detector.detect(currentFrame, currentFrameKeypoints);
		extractor.compute(currentFrame, currentFrameKeypoints, currentFrameDescriptors);
		keypoints = currentFrameKeypoints.toList();
	}

	
	public boolean process(Mat previousDescriptors, String name) {
		trainingImageDescriptors = previousDescriptors;
		matcher.knnMatch(currentFrameDescriptors, trainingImageDescriptors, currentFrameMatches, 2);
//		double minY = 9999, maxY = 0, minX = 9999, maxX = 0;
		
		for (int i = 0; i < currentFrameMatches.size(); i++) {
	    	DMatch[] matches = currentFrameMatches.get(i).toArray();
	    	
	    	DMatch bestMatch = matches[0];
	    	DMatch secondBestMatch = matches[1];
	    	
	    	for (int j = 0; j < currentFrameMatches.get(i).rows(); j++) {
	    		if ((bestMatch.distance / secondBestMatch.distance) < MIN_RATIO) {
//	    		if (matches[0] * 2.0 < matches[1].distance) {
    				Point matchedPoint = keypoints.get(matches[0].queryIdx).pt;
					goodDetections++;
					usedKP.add(matchedPoint);
//					if (flags[1]) { // Draw Square
//						double x = matchedPoint.x;
//    					double y = matchedPoint.y;
//    				
//	    				if (x < minX) {
//	    					minX = x;
//	    				} else if (x > maxX ) {
//	    					maxX = x;
//	    				}
//	    				
//	    				if (y < minY) {
//	    					minY = y;
//	    				} else if (y > maxY ) {
//	    					maxY = y;
//	    				}
//    				}
					
    				if (flags[0]) { // Draw Matches
    					Core.circle(currentFrame, keypoints.get(matches[0].queryIdx).pt, 6, new Scalar(255, 0, 255));
    				}	
	    		}
	    	}
	    }
		
		
//		if (currentFrameKeypoints.size().height > 5000) {
//			numberOfMatchesNeeded = 17;
//		} else if (currentFrameKeypoints.size().height > 9000) {
//			numberOfMatchesNeeded = 20;
//		}
		
		if (goodDetections >= numberOfMatchesNeeded) {
			return true;
		} else {
			goodDetections = 0;
		}
		
		return false;
	}


	public void drawMatches() { flags[0] = true; }
	public void drawSquare() { flags[1] = true;	}
	
	
	public boolean clear() {
		goodDetections = 0;
		return true;
	}

	
	public void drawKeypoints() {
		Features2d.drawKeypoints(currentFrame, currentFrameKeypoints, currentFrame, new Scalar(255, 255, 255), Features2d.DRAW_RICH_KEYPOINTS);
	}

	
	public void debug() {
		Log.d(TAG, "Total Keypoints"+ currentFrameKeypoints.size().height);
		Core.putText(currentFrame, "Total Keypoints: " + currentFrameKeypoints.size(), new Point(10, 100), 5, 1.8, new Scalar(255, 255, 255));
	}


	/** Computes keypoints and descriptors for all training images to compare to our input frame **/
	public void analyzeTrainingImages() {
		for (int c = 0; c < trainingImages.size(); c++) {
			Mat trainingImage = trainingImages.get(c);
			Mat trainingImageDescriptors = new Mat();
			detector.detect(trainingImage, trainingImageKeypoints);
			extractor.compute(trainingImage, trainingImageKeypoints, trainingImageDescriptors);
			trainingImagesDescriptorsList.add(trainingImageDescriptors);
		}	
	}
	
	
	/** -------------------------------------------- Getters and Setters -------------------------------------------- **/
	
	public int getNumberOfGoodDetections() {
		int x = goodDetections;
		goodDetections = 0;
		return x;
	}
	public Mat getKeypoints() { return trainingImageKeypoints; }
	public Mat getCurrentFrame() { return currentFrame; }
	public void setCurrentFrame(Mat frame) { currentFrame = frame; }
	public List<Mat> getDescriptors() { return trainingImagesDescriptorsList; }

}