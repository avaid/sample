
package mwdb.phase1.task1;

import java.io.File;
import java.io.IOException;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
/**
 * This class is used to normalize the values in input data file between 1 and -1
 * @author Akshay
 *
 */
public class NormalizeInputData{


	public static void NormalizeTask1Data(MatlabProxy proxy,String dataDir) throws IOException, MatlabInvocationException {
		File file = new File(dataDir);
		String[] filesList = file.list();
		String normDir = dataDir + File.separator + "normalizedData";
		File normDirObj = new File(normDir);
		if (normDirObj.exists()) {
			CreateGestureVector.delete(normDirObj);
		}
		if (normDirObj.mkdir()) {
			for (int i = 0; i < filesList.length; i++) {
				if (filesList[i].contains("csv")) {
					String normalizedFile = normDir + File.separator + filesList[i];
					String inputFileName = dataDir + File.separator + filesList[i];
					proxy.eval("normalize('" + inputFileName + "','"
							+ normalizedFile + "')");
				}
			}
		} else {
			System.out.println("Unable to create Directory for normalized files");
		}
	}
	
	public static void NormalizeTask3Data(MatlabProxy proxy,
			String inputFileLocation) throws MatlabInvocationException {
		int index = inputFileLocation.lastIndexOf(File.separator);
		if (index > 0) {
			String currentDir = inputFileLocation.substring(0, index);
			String normOutFilePath = currentDir + File.separator
					+ "queryDataNormalized" + ".csv";
			proxy.eval("normalize('" + inputFileLocation + "','"
					+ normOutFilePath + "')");
		} else {
			System.out.println("Error in the provided path of the input data files");
		}
	}
}
