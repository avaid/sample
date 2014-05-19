package mwdb.phase1.task1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class CreateGestureVector  { 
	
	private   List<List<Map<String,List<Double>>>> allValues = new ArrayList<List<Map<String,List<Double>>>>();
	private  Map<String,Integer> globalDict = new HashMap<String,Integer>();  //global map 
	private  List<Map<String,Double>> mapIDF2 = new ArrayList<Map<String,Double>>(); 
	private  List<Double> allIDF = new ArrayList<Double>();
	private  List<Double> allIDF2 = new ArrayList<Double>();
	private  File[] listOfFiles;
	
	/**
	 * Function to delete file or a directory
	 * @param file
	 * @throws IOException
	 */
	public static void delete(File file) throws IOException {
		if (file.isDirectory()) {
			// directory is empty, then delete it
			if (file.list().length == 0) {
				file.delete();
			} else {
				// list all the directory contents
				String files[] = file.list();
				for (String temp : files) {
					// construct the file structure
					File fileDelete = new File(file, temp);

					// recursive delete
					delete(fileDelete);
				}
				// check the directory again, if empty then delete it
				if (file.list().length == 0) {
					file.delete();
				}
			}
		} else {
			// if file, then delete it
			file.delete();
		}
	}
	
	
	public void CreateGestureVector ( String inputFolder,int windowLength, int shiftLength) throws IOException{
		
		String lettersPath = inputFolder + File.separator + "bands";
		File dirLetters = new File(lettersPath);
		
		//Filter CSV files from the directory
		listOfFiles  = dirLetters.listFiles(new FileFilter() {
		    @Override
		    public boolean accept(File pathname) {
		        String name = pathname.getName().toLowerCase();
		        return name.endsWith(".csv") && pathname.isFile();
		    }
		});
		for (int i = 0; i < listOfFiles.length; i++) {
			BufferedReader in = new BufferedReader(new FileReader(listOfFiles[i]));
			
			List<Map<String,List<Double>>> multivariateGestureMap = new ArrayList<Map<String,List<Double>>>(); 
			
			Map<String,List<Double>> univariateGestureMap = null;  //per row 
			while(in.ready())
			{
				univariateGestureMap = new HashMap<String, List<Double>>();  //per rows
				String univariateSeries = in.readLine();
				String bands[]= univariateSeries.split(",");
				double wordCntUnivariateDoc = 0.0; 
				for (int j = 0; j < bands.length-windowLength+1 ; j=j+shiftLength) {
					
					String word="";
					for (int k = j; k < windowLength+j; k++) {
						word+=bands[k];
					}
					
					//if the univariate gesture map contains the word, update the count,
					//else add the word in the map
					
					if(univariateGestureMap.containsKey(word))
					{	
						List<Double> list = univariateGestureMap.get(word);
						list.set(0,list.get(0)+1.0F); 
						univariateGestureMap.put(word, list); 
					}
					else
					{
						List<Double> list = new ArrayList<Double>();
						list.add(0, 1.0);
						univariateGestureMap.put(word, list);					
					}			
					++wordCntUnivariateDoc;
				}
			
				//Calculate the TF using the formula n/k where n is the word frequency and k is the number of words in the document 
				univariateGestureMap = updateWordMapForTotalCountK(univariateGestureMap,wordCntUnivariateDoc); 
				multivariateGestureMap.add(univariateGestureMap);				
			}
			getAllValues().add(multivariateGestureMap);
			
			
			
			
			//Calculating the IDF2 for a single multivariate document
			calculateIDF2(multivariateGestureMap);
		}
		
		// Update the all values map for idf2 values
		createIDFMap(getAllValues());
		
		//Calculate the values of idf
		 generateIDFFiles();
		 
		 
		 
		 //Calculate the values of idf2
		 generateIDF2Files();
		 
		 
		 //normalize the idf and idf2 values in the dictionary using the max value calculated
		 normalizeIDFandIDF2();
		 
		 
		 //Write the gesture vector file the values of all the statistics calculated
		 writeToFile(getAllValues(),inputFolder);
		 
		 
	}


	/**
	 * This function calculates the value of TF using the formula n/K where n is the word frequency and K is the total word count
	 * @param wordMap
	 * @param wordCntUnivariateDoc
	 * @return
	 */
	private  Map<String, List<Double>> updateWordMapForTotalCountK(Map<String, List<Double>> wordMap, double wordCntUnivariateDoc) {
		
		Iterator iterator = wordMap.entrySet().iterator();
		while(iterator.hasNext()){
			 Map.Entry<String, List<Double>> entry = (Map.Entry<String, List<Double>>) iterator.next();
			 if(wordCntUnivariateDoc> 0.0)
				 entry.getValue().set(0,entry.getValue().get(0)/wordCntUnivariateDoc); //add all tf for total words
		}
	
		return wordMap;
	}
	
	/**
	 * This function normalizes the values of TF-IDF and TF-IDF2 by divinding it with the max value
	 */
	private void normalizeIDFandIDF2() {
		
		Collections.sort(allIDF, Collections.reverseOrder());
		Collections.sort(allIDF2, Collections.reverseOrder());
		 
		 
		 Double maxIDF=allIDF.get(0)==0.0?1.0:allIDF.get(0);
		 Double maxIDF2=allIDF2.get(0)==0.0?1.0:allIDF2.get(0);
		
		for (int i = 0; i < getAllValues().size(); i++) {
			List<Map<String,List<Double>>> gestureDocument = getAllValues().get(i);
			for (int j = 0; j < gestureDocument.size(); j++) {
				Map<String,List<Double>> tempMap = gestureDocument.get(j);
				Iterator it = tempMap.entrySet().iterator();
				  while (it.hasNext()) {
				        Map.Entry<String,List<Double>> pairs = (Map.Entry)it.next();
				     
				        	pairs.getValue().set(3, pairs.getValue().get(3)/maxIDF);
				   
				        	pairs.getValue().set(4, pairs.getValue().get(4)/maxIDF2);
				  }
			}
		}
	}
	
	//This Function writes the values of statistics calculated to the file
	
	private void writeToFile(
			List<List<Map<String, List<Double>>>> tfMapArrayIDF3, String task1OutputFolder) throws IOException {
		// TODO Auto-generated method stub
		File task1OutputFileObj = new File(task1OutputFolder + File.separator
				+ "InputData3Values");
		if (task1OutputFileObj.exists()) {
			delete(task1OutputFileObj);
		}
		File task1OutputFolderAll = new File(task1OutputFolder + File.separator
				+ "InputData5Values");
		if (task1OutputFolderAll.exists()) {
			delete(task1OutputFolderAll);
		}
		if (task1OutputFileObj.mkdir() && task1OutputFolderAll.mkdir()) {
			for (int i = 0; i < getAllValues().size(); i++) {
				List<Map<String, List<Double>>> gestureDocument = getAllValues()
						.get(i);

				FileWriter fileWriter3 = new FileWriter(task1OutputFileObj
						+ File.separator + (listOfFiles[i].getName()));
				FileWriter fileWriter5 = new FileWriter(task1OutputFolderAll
						+ File.separator + (listOfFiles[i].getName()));

				BufferedWriter bufferWriter3 = new BufferedWriter(fileWriter3);
				BufferedWriter bufferWriter5 = new BufferedWriter(fileWriter5);

				for (int j = 0; j < gestureDocument.size(); j++) {
					Map<String, List<Double>> tempMap = gestureDocument.get(j);
					Iterator it = tempMap.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry<String, List<Double>> pairs = (Map.Entry) it
								.next();
						List<Double> idf = pairs.getValue();
						bufferWriter3.write(pairs.getKey() + ":"
								+ pairs.getValue().get(0) + ","
								+ pairs.getValue().get(1) + ","
								+ pairs.getValue().get(2) + ";");
						bufferWriter5.write(pairs.getKey() + ":"
								+ pairs.getValue().get(0) + ","
								+ pairs.getValue().get(1) + ","
								+ pairs.getValue().get(2) + ","
								+ pairs.getValue().get(3) + ","
								+ pairs.getValue().get(4) + ";");

					}
					bufferWriter3.write("\r\n");
					bufferWriter5.write("\r\n");
				}
				bufferWriter3.close();
				bufferWriter5.close();
			}
		} else {
			System.out.println("Error while creating output folders");
		}

	}
	
	private void generateIDF2Files() throws IOException {

		for (int i = 0; i < getAllValues().size(); i++) {
			List<Map<String,List<Double>>> multivariateGesture = getAllValues().get(i);
			Map<String,Double> univariateGesture = getMapIDF2().get(i);
			
			for (int j = 0; j < multivariateGesture.size(); j++) {
				Map<String,List<Double>> tempMap = multivariateGesture.get(j);
				Iterator it = tempMap.entrySet().iterator();
				
				  while (it.hasNext()) {
				        Map.Entry<String,List<Double>> pairs = (Map.Entry)it.next();
				        Double inverse = (new Double(getAllValues().get(0).size())/univariateGesture.get(pairs.getKey())); //already inversse
				        List<Double> tf = pairs.getValue(); 
				        //tf.add(tf.get(0)*Math.log(inverse)); // 2 for idf2
				        tf.add(Math.log(inverse)); //tfidf2 
				        //calculate tf-idf and tf-idf2 by multipliication of tf,idf and tf,idf2
				        tf.add(tf.get(0)*tf.get(1));
				        //add the value of tf-idf and tf-idf2 to the set to calculate the max value
				        allIDF.add(tf.get(1));			
				        tf.add(tf.get(0)*tf.get(2)); 
				        allIDF2.add(tf.get(2));							        				        	
				    }
			}
		}
	}




	private void calculateIDF2(List<Map<String, List<Double>>> multivariateGestureMap) {
	
		
		Map<String,Double> univariateGestureMap = new HashMap<String, Double>(); 
		
		for (int i = 0; i < multivariateGestureMap.size(); i++) {
			Map<String,List<Double>> tmpMap = multivariateGestureMap.get(i);
			Iterator iterator = tmpMap.entrySet().iterator();
			while(iterator.hasNext()){
				Map.Entry<String, List<Double>> pairs = (Entry<String, List<Double>>) iterator.next();
				if(univariateGestureMap.containsKey(pairs.getKey()))
						univariateGestureMap.put(pairs.getKey(), univariateGestureMap.get(pairs.getKey())+1.0); 
				else
						univariateGestureMap.put(pairs.getKey(), 1.0);
			}
		}
		getMapIDF2().add(univariateGestureMap);
	}

	private void generateIDFFiles() throws IOException {
		for (int i = 0; i < getAllValues().size(); i++) {
			List<Map<String,List<Double>>> gestureDocument = getAllValues().get(i);
			for (int j = 0; j < gestureDocument.size(); j++) {
				Map<String,List<Double>> tempMap = gestureDocument.get(j);
				Iterator it = tempMap.entrySet().iterator();
				  while (it.hasNext()) {
				        Map.Entry<String,List<Double>> pairs = (Map.Entry)it.next();
				        Double inverse = (new Double(getAllValues().size()*getAllValues().get(0).size())/getGlobalDict().get(pairs.getKey())); //already inversse
				        Double idf = (Math.log(inverse));
				        pairs.getValue().add(idf);
				    
				    }

			}
		}		
		
	}

	private  void createIDFMap(List<List<Map<String,List<Double>>>> tfMap){
		
		for (int i = 0; i < tfMap.size(); i++) {
			List<Map<String,List<Double>>> gestureDocument = tfMap.get(i);
			for (int j = 0; j < gestureDocument.size(); j++) {
				Map<String,List<Double>> tempMap = gestureDocument.get(j);
				Iterator it = tempMap.entrySet().iterator();
				  while (it.hasNext()) {
				        Map.Entry<String,Integer> pairs = (Map.Entry)it.next();
				        
				        if(getGlobalDict().containsKey(pairs.getKey()))
				        	getGlobalDict().put(pairs.getKey(), getGlobalDict().get(pairs.getKey())+1);
				        else
				        	getGlobalDict().put(pairs.getKey(), 1);
				    
				    }
			}
		}
	}
	
	public void setGlobalDict(Map<String,Integer> globalDict) {
		this.globalDict = globalDict;
	}
	public  Map<String,Integer> getGlobalDict() {
		return globalDict;
	}
	public  void setMapIDF2(List<Map<String,Double>> mapIDF2) {
		this.mapIDF2 = mapIDF2;
	}
	public  List<Map<String,Double>> getMapIDF2() {
		return mapIDF2;
	}
	public  void setAllValues(List<List<Map<String,List<Double>>>> allValues) {
		this.allValues = allValues;
	}
	public  List<List<Map<String,List<Double>>>> getAllValues() {
		return allValues;
	}
	public void setListOfFiles(File[] listOfFiles) {
		this.listOfFiles = listOfFiles;
	}
	public File[] getListOfFiles() {
		return listOfFiles;
	}
}
