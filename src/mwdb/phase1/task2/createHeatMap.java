package mwdb.phase1.task2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.tc33.jheatchart.HeatChart;

//import asu.edu.loggers.MyLogger;
import au.com.bytecode.opencsv.CSVReader;

public class createHeatMap {


	public static void drawHeatMap(String normalizeFileName,String letterFileName, String task1OutputFile, int w, int s,String outFileName, String type) throws IOException {
		CSVReader readCSV = new CSVReader(new FileReader(normalizeFileName));
		List<String[]> matrixStr = readCSV.readAll();
		readCSV.close();

		Iterator<String[]> I = matrixStr.iterator();
		String temp[] = I.next();
		List<String[]> topTen = parseTask2File(task1OutputFile, type);

		String topTenWordListDetails[][] = new String[topTen.size()][3];
		int k = 0;
		for (String temp1[] : topTen) {
			topTenWordListDetails[k] = temp1;
			k++;
		}

		// Parse Top ten word List Details
		Arrays.sort(topTenWordListDetails, new Comparator<String[]>() {
			@Override
			public int compare(final String[] entry1, final String[] entry2) {
				final double value1 = Double.parseDouble(entry1[1]);
				final double value2 = Double.parseDouble(entry2[1]);
				if (value1 > value2) {
					return -1;
				} else if (value1 < value2) {
					return 1;
				} else {
					return 0;
				}
			}
		});

		double matrix[][] = new double[matrixStr.size()][temp.length];

		k = 0;
		for (String item[] : matrixStr) {
			for (int i = 0; i < item.length; i++) {
				matrix[k][i] = Double.parseDouble(item[i]);
			}
			k++;
		}

		double highlightValue = 2.5;
		System.out.println("Top ten " + type + " values");
		// Prints top ten
		for (int i = 0; i < 10; i++) {
			String temp2[] = topTenWordListDetails[i];
			int lineNum = Integer.parseInt(temp2[2]);
			System.out.println("Word : " + temp2[0] + " " + type + " value :"
					+ temp2[1] + " Sensor number : " + temp2[2]);
			int t[] = getRangeFromMatrix(letterFileName, w, s, temp2[0],
					lineNum);
			int index = t[0];
			int len = t[1] - t[0] + 1;
			for (int j = 0; j < len; j++) {
				matrix[lineNum - 1][index] = highlightValue;
				index++;
			}
			highlightValue = highlightValue - 0.15;
		}
		// Step 1: Create our heat map chart using our data.
		HeatChart map = new HeatChart(matrix);

		// Step 2: Customise the chart.
		map.setTitle(" Heat Map");
		map.setXAxisLabel("X Axis");
		map.setYAxisLabel("Y Axis");

		String xAxisLabels[] = new String[matrix[0].length];

		for (int i = 0; i < xAxisLabels.length; i++) {
			xAxisLabels[i] = "Time" + (i + 1);
		}
		String yAxisLabels[] = new String[matrix.length];

		for (int i = 0; i < yAxisLabels.length; i++) {
			yAxisLabels[i] = "Sensor" + (i + 1);
		}
		map.setXValues(xAxisLabels);
		map.setYValues(yAxisLabels);

		// Step 3: Output the chart to a file.
		map.saveToFile(new File(outFileName));
	}

	public static List<String[]> parseTask2File(String inFileName, String type)
			throws IOException {
		List<String[]> topTen = new ArrayList<String[]>();

		CSVReader csvReader = new CSVReader(new FileReader(inFileName),
				(char) ';');
		List<String[]> fileDataList = csvReader.readAll();
		csvReader.close();
		// Initializing line number to 1,
		// when you use this arrays, please do line-1
		int line = 1;
		for (String temp[] : fileDataList) {
			String temp1[][] = getWordCount(temp, line, type);
			for (String temp2[] : temp1) {
				topTen.add(temp2);
			}
			line++;
		}
		return topTen;

	}

	public static String[][] getWordCount(String words[], int lineNum,
			String type) {

		int pointer = getTypeNum(type);

		String finalizedWordCount[][] = new String[words.length - 1][3];
		int wordListCount = 0;
		for (int i = 0; i < words.length - 1; i++) {
			String temp1 = words[i];
			temp1 = temp1.replaceFirst(":", ",");
			String temp2[] = temp1.split(",");
			finalizedWordCount[wordListCount][0] = temp2[0];
			finalizedWordCount[wordListCount][1] = temp2[pointer];
			finalizedWordCount[wordListCount][2] = "" + lineNum;
			wordListCount++;
		}

		return finalizedWordCount;

	}

	public static int getTypeNum(String type) {
		if (type.equalsIgnoreCase("TF")) {
			return 1;
		} else if (type.equalsIgnoreCase("IDF")) {
			return 2;
		} else if (type.equalsIgnoreCase("IDF2")) {
			return 3;
		} else if (type.equalsIgnoreCase("TF-IDF")) {
			return 4;
		} else if (type.equalsIgnoreCase("TF-IDF2")) {
			return 5;
		}
		return 1;
	}

	public static int[] getRangeFromMatrix(String letterFileName, int w, int s,String word, int lineNumber) throws IOException {
		int range[] = new int[2];
		BufferedReader br = new BufferedReader(new FileReader(letterFileName));
		String line;
		String reqLine = null;
		int k = 1;
		while ((line = br.readLine()) != null) {
			if (k == lineNumber) {
				reqLine = line;
			}
			k++;
		}
		br.close();
		String letter[] = reqLine.split(",");
		word = word.replaceFirst("d", "");
		String context[] = word.split("d");
		int start = 0;
		int end = 0;
		int flag = 0;
		int count = 0;
		for (int i = 0; i < letter.length -w + 1; i=i+s) {
			count = 0;
			if (letter[i].equals("d" + context[count])) {
				int j = i + 1;
				if (flag == 0) {
					count++;
					start = i;

					while ((count < w)
							&& (letter[j].equals("d" + context[count]))) {
						j++;
						count++;

					}

					if (count == w) {
						end = j - 1;
						i = letter.length + 1;
					} else {
						flag = 0;
						count = 0;
					}
				}

			}
		}

		range[0] = start;
		range[1] = end;
		return range;

	}

}
