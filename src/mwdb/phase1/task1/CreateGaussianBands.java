package mwdb.phase1.task1;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.math3.distribution.NormalDistribution;


/**
 * This class is used to create Guassian Band ranges for the provided resolution r
 * @author Akshay
 *
 */
public class CreateGaussianBands {


	/**
	 * Function to create the range of bands for the user provided resolution r
	 * @param inputDir
	 * @param resolution
	 * @param mean
	 * @param std
	 * @return
	 * @throws IOException
	 */
	public double[][] getGaussianBands(String inputDir, double resolution,double mean, double std) throws IOException {
		
		NormalDistribution nd = new NormalDistribution(mean, std);
		double bandValues[] = new double[(int) (resolution)];
		for (int i = 0; i < resolution; i++) {
			bandValues[i] = (nd.probability((double) -(i + 1) * (1 / resolution),
					(double) (i + 1) * (1 / resolution)));
		}

			
		double bandRanges[][] = new double[(int) (2 * resolution)][3];
		int j = 0;
		double current = -1.1;
		for (int i = bandValues.length - 2; i >= 0; i--) {
			bandRanges[j][0] = current;
			bandRanges[j][1] = -1 * bandValues[i];
			bandRanges[j][2] = j + 1;
			j++;
			current = -1 * bandValues[i];
		}

		bandRanges[j][0] = current;
		bandRanges[j][1] = 0;
		bandRanges[j][2] = j + 1;
		j++;
		current = 0;
		double prev=0;
		for (int i = 0; i < bandValues.length; i++) {
			bandRanges[j][0] = current;
			bandRanges[j][1] = bandValues[i];
			bandRanges[j][2] = j + 1;
			j++;
			prev=current;
			current = bandValues[i];
		}

		bandRanges[j-1][0] = prev;
		bandRanges[j-1][1] = 1.1;
		bandRanges[j-1][2] = j;
		BufferedWriter br = new BufferedWriter(new FileWriter(new File(
				inputDir + File.separator + "bandRanges.csv")));
		for (int i = 0; i < bandRanges.length; i++) {
			br.write(Double.toString(bandRanges[i][0]) + ","
					+ Double.toString(bandRanges[i][1]) + ","
					+ Double.toString(bandRanges[i][2]));
			br.write("\r\n");
		}
		br.close();
		return bandRanges;
	}
}