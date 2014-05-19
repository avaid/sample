package mwdb.phase1.task1;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import au.com.bytecode.opencsv.CSVReader;

public class AssignBandsToNormValues {


	/**
	 * Function which assign Gaussian Bands to the input file in Task1 
	 * @param proxy
	 * @param matlabScriptLoc
	 * @param inputDirectory
	 * @param rBandValueRange
	 * @throws MatlabInvocationException
	 * @throws IOException
	 */
	public static void task1AssignBands(MatlabProxy proxy,String matlabScriptLoc, String inputDirectory,double rBandValueRange[][]) throws MatlabInvocationException,
			IOException {
		File inputFiles = new File(inputDirectory);
		String[] listOfFiles = inputFiles.list();
		
		String bandsOutputDirectory = inputDirectory + File.separator + "bands";
		File bandOutDirectoryObject = new File(bandsOutputDirectory);
		if (bandOutDirectoryObject.exists()) {
			CreateGestureVector.delete(bandOutDirectoryObject);
		}
		if(bandOutDirectoryObject.mkdir()) {
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].contains("csv") && !listOfFiles[i].contains("bandRanges")) {
					String inputFileLocation = inputDirectory + File.separator + "normalizedData" + File.separator +  listOfFiles[i];
					String gausianFileLocation = inputDirectory + File.separator + "bands" + File.separator + listOfFiles[i];
					assignLetters(inputFileLocation, gausianFileLocation, rBandValueRange);
				}
			}
		} else {
			System.out.println("Unable to create directory for Gaussian Bands Output");
		}
		
		
	}

	/**
	 * Function which assign Gaussian Bands to the input file in Task3 
	 * @param proxy
	 * @param matlabScriptLoc
	 * @param inputFileLocation
	 * @param rBandValueRange
	 * @throws IOException
	 */
	public static void task3AssignBands(MatlabProxy proxy,String matlabScriptLoc, String inputFileLocation,double rBandValueRange[][]) throws IOException {
		int index = inputFileLocation.lastIndexOf(File.separator);
		if (index > 0) {
			String currentDirectory = inputFileLocation.substring(0, index);
			String gaussianFileOutput = currentDirectory + File.separator + "queryBands.csv";
			String nomalizedFileLocation = currentDirectory + File.separator + "queryDataNormalized.csv";
			assignLetters(nomalizedFileLocation, gaussianFileOutput, rBandValueRange);
		} else {
			System.out.println("Error in assigning bands for task3");
		}
	}

	
	
	/**
	 * Function to assign Letters to the Normalized Data
	 * @param nomalizedFileLocation
	 * @param gaussianFileOutput
	 * @param rBandValueRange
	 * @throws IOException
	 */
	public static void assignLetters(String nomalizedFileLocation,String gaussianFileOutput, double rBandValueRange[][]) throws IOException {

		CSVReader csvReader = new CSVReader(new FileReader(nomalizedFileLocation));
		List<String[]> lines = csvReader.readAll();
		csvReader.close();
		List<String> writeLines = new ArrayList<String>();
		for (String[] line : lines) {
			String newLine = new String();
			int flag = 0;
			for (int i = 0; i < line.length; i++) {
				String sensorPoint = line[i];
				// binary search to assign the the band to the normalized data
				int value = (int) binarySearch(rBandValueRange,
						Double.parseDouble(sensorPoint),
						rBandValueRange.length - 1, 0);
				if (flag == 0) {
					newLine = "d" + value;
					flag = 1;
				} else {
					newLine = newLine + ",d" + value;
				}

			}
			writeLines.add(newLine);
		}

		BufferedWriter br = new BufferedWriter(new FileWriter(new File(
				gaussianFileOutput)));
		for (String line : writeLines) {
			br.write(line);
			br.write("\r\n"); // needed so that it can be read in Matlab
		}

		br.close();
	}

	/**
	 * Common binary search algorithm
	 * @param rBandValueRange
	 * @param value
	 * @param high
	 * @param low
	 * @return
	 */
	public static double binarySearch(double rBandValueRange[][], double value,
			int high, int low) {
		int mid = (high + low) / 2;
		double result =99;

		if (value < rBandValueRange[mid][0]) {
			result=binarySearch(rBandValueRange, value, mid - 1, low);
			
		} else if (value > rBandValueRange[mid][0]) {
			if (value <= rBandValueRange[mid][1]) {
				return rBandValueRange[mid][2];
			} else {
				result =binarySearch(rBandValueRange, value, high, mid + 1);
			}

		} 
		return result;
	}
}
