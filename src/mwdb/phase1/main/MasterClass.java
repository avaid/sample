package mwdb.phase1.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import mwdb.phase1.task1.AssignBandsToNormValues;
import mwdb.phase1.task1.CreateGaussianBands;
import mwdb.phase1.task1.CreateGestureVector;
import mwdb.phase1.task1.NormalizeInputData;
import mwdb.phase1.task2.createHeatMap;
import mwdb.phase1.task3.IdentifySimilarDataFiles;
/**
 * Master Class which executes task1, task2 and task3
 * @author Akshay
 *
 */

public class MasterClass {

	private static MatlabProxy proxy;
	private static Integer windowLength = 0;
	private static Integer shiftLength = 0;
	private static Double resolution = 0.0;
	private static Double mean = 0.0;
	private static Double stdDeviation = 0.25;
	private static String matLabScriptsPath = null;
	
	
	
	private static String greyScaleHeatMapPath = null;
	private static String normFilePath = null;
	private static String letterFilePath = null;
	public static CreateGestureVector GestureVector = null;
	
	
	/**
	 * Function to clean up all the temporary files and directories created in the previous run
	 * @param inputDirectory
	 * @throws IOException
	 */
	
	private static void cleanUp(String inputDirectory) throws IOException {
        File listFiles = new File(inputDirectory);
        File[] allFiles =     listFiles.listFiles();
        for(File file : allFiles) {
            String fileName = file.getName();
            if(fileName.contains("queryBands") || fileName.contains("normalizedData") || fileName.contains("bands") 
            || fileName.contains("InputData3Values") || fileName.contains("InputData5Values") 
            || fileName.contains("QueryData3Values") || fileName.contains("QueryData5Values")||fileName.contains("queryDataNormalized")
            || fileName.contains("bandRanges")) {
                CreateGestureVector.delete(file);
            }
        }
    }
		

	/**
	 * Function to create gesture words dictionary from the given multivariate dataset ,based on user provided 
	 * values of window length, shift length and resolution 
	 * @throws IOException
	 * @throws MatlabInvocationException
	 */
	private static void processTask1() throws IOException,
			MatlabInvocationException {

		
		System.out.println("Enter the path of the directory for sample database");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String inputDirectory = br.readLine();

		System.out.println("Enter the value of window length(w)");
		windowLength = Integer.parseInt(br.readLine());

		System.out.println("Enter the value of shift length(s)");
		shiftLength = Integer.parseInt(br.readLine());

		System.out.println("Enter the value of resolution(r)");
		resolution = Double.parseDouble(br.readLine());

		
		cleanUp(inputDirectory);
		
		NormalizeInputData.NormalizeTask1Data(proxy, inputDirectory);
		CreateGaussianBands gb = new CreateGaussianBands();
		double rBandValueRange[][] = gb.getGaussianBands(inputDirectory,
				resolution, mean, stdDeviation);
		AssignBandsToNormValues.task1AssignBands(proxy, matLabScriptsPath,
				inputDirectory, rBandValueRange);
		//
		// TODO - handle in case of nested folders
		GestureVector = new CreateGestureVector();
		GestureVector.CreateGestureVector(inputDirectory,windowLength, shiftLength);
		System.out.println("Task 1 Execution Done :");
		System.out.println("Normalized Data files are present \"normalizedData\" Folder");
		System.out.println("Discretized Data files are present in \"Bands\" Folder");
		System.out.println("Statistics(TF,IDF,IDF2,TF-IDF,TF-IDF2) are present in \"InputData5Values\" Folder");

	}
	/**
	 * Function to view the data file in the form of gray scale heat map
	 * @throws IOException
	 */
	private static void processTask2() throws IOException {
		System.out.println("Enter the path for gesture file to be viewed in the form of grey scale heat map");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String inputGestureFile = br.readLine();
		int index = inputGestureFile.lastIndexOf(File.separator);
		if (index > 0) {
			String gestureFileDir = inputGestureFile.substring(0, index);
			String gestFileName = inputGestureFile.substring(index + 1); 
			normFilePath = gestureFileDir + File.separator + "normalizedData" +File.separator + gestFileName;
			letterFilePath = gestureFileDir + File.separator + "bands" +File.separator +gestFileName;
			String gestureVectorFilePath = gestureFileDir + File.separator  + "InputData5Values"+File.separator + gestFileName;
			
			System.out.println("Select the statistic based on which you want to visualise the data? (TF/IDF/IDF2/TF-IDF/TF-IDF2");
			String heatMapStatistic = br.readLine();
			
			System.out.println("Enter the output path for the grey scale heat map(Extension should be .png)");
			greyScaleHeatMapPath = br.readLine();
			
			createHeatMap.drawHeatMap(normFilePath, letterFilePath, gestureVectorFilePath ,windowLength, shiftLength, greyScaleHeatMapPath, heatMapStatistic);
		} else {
			//logger.info("Error in File I/O for Task 2");
			System.out.println("Error in File I/O for Task 2");
		}
	}
	
	
/**Function to select the 10 most similar multi-variate time series data files in the database 
 * based in TF,TF-IDF and TF-IDF2 values
 * @throws IOException
 * @throws MatlabInvocationException
 */
	private static void processTask3() throws IOException,MatlabInvocationException {
		System.out.println("Enter the path of the input data file for which similar multivariate files are to be to be queried from the database");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String inputFilePath = br.readLine();
		NormalizeInputData.NormalizeTask3Data(proxy, inputFilePath);
		CreateGaussianBands gb = new CreateGaussianBands();
		int index = inputFilePath.lastIndexOf(File.separator);
		String currentDir = null;
		if (index > 0) {
			currentDir = inputFilePath.substring(0, index);
		}
		double gaussianBandRanges[][] = gb.getGaussianBands(currentDir,
				resolution, mean, stdDeviation);
		AssignBandsToNormValues.task3AssignBands(proxy, matLabScriptsPath,
				inputFilePath, gaussianBandRanges);
		IdentifySimilarDataFiles IdentifySimilarFiles= new IdentifySimilarDataFiles(GestureVector.getGlobalDict(),GestureVector.getAllValues(),GestureVector.getMapIDF2(),windowLength,shiftLength,inputFilePath,GestureVector);
	}
	
	/**
	 * Main function which executes all the tasks
	 * @param args
	 */
	
	public static void main(String args[]) {

		try {
			 //Java Code to initialize connection to matlab and execute the code located in matlab scripts
			MatlabProxyFactory factory = new MatlabProxyFactory();
			proxy = factory.getProxy();
			matLabScriptsPath = "." + File.separator + "MatlabScripts";
			String path = "cd(\'" + matLabScriptsPath + "')";
			proxy.eval(path);
	
			System.out.println("*********************************");
			//Execute task1
			System.out.println("********Executing Task 1 ********");
			System.out.println("*********************************");
			processTask1();
			System.out.println("*********************************");
			//Execute task2
			System.out.println("********Executing Task 2 ********");
			System.out.println("*********************************");
			processTask2();
			System.out.println("*********************************");
			//Execute task3
			System.out.println("********Executing Task 3 ********");
			System.out.println("*********************************");
			processTask3();
			System.out.println("*********************************");
	
			// Disconnect Matlab
			proxy.exit();
			proxy.disconnect();
		} catch (Exception e) {
			System.out.println("Unable to process the tasks");
		}

	}
}
