package mwdb.phase1.task3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
//import java.util.logging.Level;
//import java.util.logging.Logger;
import mwdb.phase1.task1.CreateGestureVector;

/**
 * This class is used to identify the 10 most similar multivariate data series for a given input data file 
 * @author Akshay
 *
 */
public class IdentifySimilarDataFiles {
	private Map<String,Integer> globalDict;
	private String queryDocInputDir;
	private List<Map<String,List<Double>>> queryFileGestureWords; 
	private List<List<Map<String,List<Double>>>> databaseAllValues;
	private List<List<Map<String,List<Double>>>> inputDictionary;
	private List<Map<String,Double>> dictionaryIDF2; 
	private CreateGestureVector CreateGestureVector;
	private Double maxTermFrequency;
	enum Statistic{TF,IDF,IDF2,TFIDF,TFIDF2;}
	
	
	/**
	 * Constructor to initialize the data members and member functions
	 * @param globalDict
	 * @param databaseAllValues
	 * @param dictionaryIDF2
	 * @param wordLength
	 * @param shiftLength
	 * @param queryDocInputDir
	 * @param constructGestureWords
	 * @throws IOException
	 */
	public IdentifySimilarDataFiles(Map<String,Integer> globalDict,List<List<Map<String,List<Double>>>> databaseAllValues,List<Map<String,Double>> dictionaryIDF2,Integer wordLength,Integer shiftLength,String queryDocInputDir,CreateGestureVector constructGestureWords) throws IOException{
		this.globalDict = globalDict;
		this.databaseAllValues=databaseAllValues;
		this.inputDictionary = new ArrayList<List<Map<String,List<Double>>>>();
		this.dictionaryIDF2=dictionaryIDF2;
		this.queryDocInputDir = queryDocInputDir;
		this.CreateGestureVector = constructGestureWords;
		queryFileGestureWords = new ArrayList<Map<String,List<Double>>>();
		createWordsAndInitializeDictionary(wordLength,shiftLength,queryDocInputDir); //generated tf,idf,idf2 save to file

	
		//Query for TF
		HashMap<Integer, Double> tfSimilarScores = extractRequiredStatistic(inputDictionary,this.databaseAllValues,Statistic.TF);
		//Query for TF-IDF
		HashMap<Integer, Double> tfidfSimilarScores = extractRequiredStatistic(inputDictionary,this.databaseAllValues, Statistic.TFIDF);
		//Query for TF-IDF2
		HashMap<Integer, Double> tfidf2SimilarScores = extractRequiredStatistic(inputDictionary,this.databaseAllValues,Statistic.TFIDF2);
		
		System.out.println("-------------------------------------------------------------------------------------");
		System.out.println("The TOP 10 Matching multivariate time series data files according to TF values are :");
		displaySimilarDocs(tfSimilarScores);
		System.out.println("-------------------------------------------------------------------------------------");
		System.out.println("The TOP 10 Matching multivariate time series data files according to TFIDF values are :");
		displaySimilarDocs(tfidfSimilarScores);
		System.out.println("-------------------------------------------------------------------------------------");
		System.out.println("The TOP 10 Matching multivariate time series data files according to TFIDF2 values are :");
		displaySimilarDocs(tfidf2SimilarScores);
		System.out.println("-------------------------------------------------------------------------------------");
		
		
	}

	/**
	 * Function to display similar Docs based on the value of scores
	 * @param tfidfSimilarScores
	 */
	private void displaySimilarDocs(HashMap<Integer, Double> tfidfSimilarScores) {
		int i=0;
		for (Entry<Integer, Double> entry : tfidfSimilarScores.entrySet()) { 
		    Integer key = entry.getKey();
		    Double value = entry.getValue();
		    File fileName = CreateGestureVector.getListOfFiles()[key];
		    String path = fileName.getPath();
		    path = path.replace("bands" + File.separator, "");
		    System.out.print(path +"\n");
		    i++;
		    if(i == 10)
		    	break;
		}
		System.out.println("\n");
	}

	/**
	 * Function to initialize a dictionary for the input query file
	 * @param wordLength
	 * @param shiftLength
	 * @param inputDirectory
	 * @throws IOException
	 */
	private void createWordsAndInitializeDictionary(Integer wordLength,Integer shiftLength, String inputDirectory) throws IOException { 
		// TODO Auto-generated method stub
		File inputFilePath = new File(inputDirectory);
		int index = inputDirectory.lastIndexOf(File.separator);
		if(index > 0){
			String currentDir = inputDirectory.substring(0, index);
			File file1 = new File(currentDir + File.separator +"queryBands.csv");
		
			BufferedReader in = new BufferedReader(new FileReader(file1));
			
			List<Map<String,List<Double>>> mapPerGestureFile = new ArrayList<Map<String,List<Double>>>(); 
			
			Map<String,List<Double>> wordMap = null;  //per row 
			while(in.ready())
			{
				wordMap = new HashMap<String, List<Double>>();  //per rows
				String series = in.readLine();
				
				String letters[]= series.split(",");
				double totalWordCountPerDocument = 0.0; 
				for (int j = 0; j < letters.length-wordLength+1 ; j=j+shiftLength) {
					
					String word="";
					for (int k = j; k < wordLength+j; k++) {
						word+=letters[k];
					}
					
					if(wordMap.containsKey(word))
					{	
						List<Double> list = wordMap.get(word);
						list.set(0,list.get(0)+1.0F); //0 index for TF
						wordMap.put(word, list); 
					}
					else
					{
						List<Double> list = new ArrayList<Double>();
						list.add(0, 1.0);
						wordMap.put(word, list);					
					}			
					++totalWordCountPerDocument;
				}

			
				wordMap = updateTF(wordMap,totalWordCountPerDocument);
				mapPerGestureFile.add(wordMap);	
				queryFileGestureWords.add(wordMap);
				
			} 
			inputDictionary.add(mapPerGestureFile);
			
			
			maxTermFrequency = getMaxTF(queryFileGestureWords);
			
			processIDF(databaseAllValues,inputDictionary,globalDict);
			
			processIDF2(inputDictionary,mapPerGestureFile);
		
			writeToFile(inputDictionary.get(0),currentDir);
			
			
		}
		
	}
	
	//This function updates the TF value by dividing it with the total word count in a document
	private  Map<String, List<Double>> updateTF(
			Map<String, List<Double>> wordMap, double totalWordCountPerDocument) {
		Iterator iterator = wordMap.entrySet().iterator();
		while(iterator.hasNext()){
			 Map.Entry<String, List<Double>> entry = (Map.Entry<String, List<Double>>) iterator.next();
			 if(totalWordCountPerDocument> 0.0)
				 entry.getValue().set(0,entry.getValue().get(0)/totalWordCountPerDocument); //add all tf for total words
		}
	
		return wordMap;
	}
	
	/**
	 * This function is used to get the max TF value to calculate the TF-IDF,TF-IDF2 as per the salton and Buckley Formula
	 * @param inputDictionary2
	 * @return
	 */
	private Double getMaxTF(List<Map<String, List<Double>>> inputDictionary2) {
		List<Double> value = new ArrayList<Double>();
		for (int i = 0; i < inputDictionary2.size(); i++) {
			 Map<String, List<Double>> listMap = inputDictionary2.get(i);
			 Iterator iterator = listMap.entrySet().iterator();
			 while(iterator.hasNext()){
				 Entry<String, List<Double>> entry = (Entry<String,List<Double>>)iterator.next();
				 value.add(entry.getValue().get(0));
			 }
		}
		return Collections.max(value);
	}

	/**
	 * This function is used to extract the required statistic such as TF,IDF based on the value passed to the function(entity)
	 * @param inputDictionary
	 * @param dictionary
	 * @param entity
	 * @return
	 */
	private LinkedHashMap<Integer,Double> extractRequiredStatistic(List<List<Map<String, List<Double>>>> inputDictionary,List<List<Map<String, List<Double>>>> dictionary, Statistic entity) {
		
		HashMap<Integer,Double> scores = new HashMap<Integer,Double>();
			
			if(inputDictionary.get(0).size()!=dictionary.get(0).size())
			{
				System.out.println("Number of sensors in input file dont match withe number of sensors in the database files");
			}
			for (int i = 0; i < dictionary.size(); i++) {
				List<Map<String, List<Double>>> entry = dictionary.get(i);
				List<Map<String,List<Double>>> inputEntry = inputDictionary.get(0);// always 0
				double sum = 0.0d;
				try{
				for (int k = 0; k < inputEntry.size(); k++) {
					Map<String, Double> inputMap = convertMap(inputEntry.get(k),entity); 
					Map<String, Double> dictMap = convertMap(entry.get(k),entity);
					sum+=calculateCosineSimilarity(inputMap, dictMap);
				}}catch (Exception e) {
					e.printStackTrace();
				}
					scores.put(i,sum);
				}
			
			
			return  sortHashMapByValuesD(scores);
		}

	/**
	 * This function is used to convert the map which contains all the 5 statistics values to map which contains only the required statistic
	 * @param map
	 * @param entity
	 * @return
	 */
	private Map<String, Double> convertMap(Map<String, List<Double>> map,Statistic entity) {
			Map<String,Double> values = new HashMap<String, Double>(); 
			Iterator<Entry<String, List<Double>>> iterator = map.entrySet().iterator();
			while(iterator.hasNext()){
				Entry<String, List<Double>> entry = iterator.next();
				values.put(entry.getKey(), entry.getValue().get(entity.ordinal()));
			}
			
		return values;
	}

	
	/**
	 * This function is used to write the statistics values for the input query document to a file
	 * @param gestureDocument
	 * @param inputFolder
	 * @throws IOException
	 */
	private void writeToFile(List<Map<String, List<Double>>> gestureDocument,String inputFolder) throws IOException {
		
		File task3OutputFileObj = new File(inputFolder + File.separator
				+ "QueryData3Values");
		if (task3OutputFileObj.exists()) {
			mwdb.phase1.task1.CreateGestureVector.delete(task3OutputFileObj);
		}
		File task3OutputFolderAll = new File(inputFolder + File.separator
				+ "QueryData5Values");
		if (task3OutputFolderAll.exists()) {
			mwdb.phase1.task1.CreateGestureVector.delete(task3OutputFolderAll);
		}
		if (task3OutputFileObj.mkdir() && task3OutputFolderAll.mkdir()) {
		
		
		FileWriter fileWriter3 = new FileWriter(task3OutputFileObj +File.separator+"task3.csv",true);
		FileWriter fileWriter5 = new FileWriter(task3OutputFolderAll+File.separator+"task3.csv",true);
		BufferedWriter bufferWriter3 = new BufferedWriter(fileWriter3); 
		BufferedWriter bufferWriter5 = new BufferedWriter(fileWriter5);
		
		
		for (int j = 0; j < gestureDocument.size(); j++) {
			Map<String,List<Double>> tempMap = gestureDocument.get(j);
			Iterator it = tempMap.entrySet().iterator();				
			  while (it.hasNext()) {
			        Map.Entry<String,List<Double>> pairs = (Map.Entry)it.next();
			        List<Double> idf = pairs.getValue();
			        bufferWriter3.write(pairs.getKey()+":"+pairs.getValue().get(0)+","+pairs.getValue().get(1)+","+pairs.getValue().get(2)+";");
			        bufferWriter5.write(pairs.getKey()+":"+pairs.getValue().get(0)+","+pairs.getValue().get(1)+","+pairs.getValue().get(2)+","+pairs.getValue().get(3)+","+pairs.getValue().get(4)+";");		    
			    }
		      bufferWriter3.write("\r\n");
		      bufferWriter5.write("\r\n");
		}
		
		  bufferWriter3.close();
		  bufferWriter5.close();
		} else {
			System.out.println("Could not create output directories for Task 3");
		}
	}

	/**
	 * This function is used to calculate the IDF2 values for the input query file
	 * @param inputDictionary
	 * @param mapPerGestureFile
	 */
	private void processIDF2(List<List<Map<String, List<Double>>>> inputDictionary, List<Map<String, List<Double>>> mapPerGestureFile) {  
		
		Map<String,Double> idf2PerDocument = new HashMap<String, Double>(); // per univariate series
		
		for (int i = 0; i < mapPerGestureFile.size(); i++) {
			Map<String,List<Double>> tmpMap = mapPerGestureFile.get(i);
			Iterator iterator = tmpMap.entrySet().iterator();
			while(iterator.hasNext()){
				Map.Entry<String, List<Double>> pairs = (Entry<String, List<Double>>) iterator.next();
				if(idf2PerDocument.containsKey(pairs.getKey()))
						idf2PerDocument.put(pairs.getKey(), idf2PerDocument.get(pairs.getKey())+1.0F); 
				else
						idf2PerDocument.put(pairs.getKey(), 1.0);
			}
		}
		dictionaryIDF2.add(idf2PerDocument);
		
		List<Map<String,List<Double>>> oneFile = inputDictionary.get(0); // only single element in list 
		for (int i = 0; i < oneFile.size(); i++) {
			Map<String,List<Double>> mapForRow = oneFile.get(i);
			Iterator<Entry<String, List<Double>>> iterator = mapForRow.entrySet().iterator();
			while(iterator.hasNext()){
				Map.Entry<String, List<Double>> pair = iterator.next();
				Double inverse = new Double(( oneFile.size())/ idf2PerDocument.get(pair.getKey())) ; // tf value at 0
				
				//Calculating the value of TF-IDF2 using the Sallen & Buckley formula
				Double expression = 0.5 * (pair.getValue().get(0));
				if(maxTermFrequency>0.0)
				{
					expression=0.5 + expression/maxTermFrequency;
				}
				else
				{
					expression = 0.0;
				}
				
				
				pair.getValue().set(2,Math.log(inverse)); // IDF2 will go to second index
				
				//add TF-IDF2
				pair.getValue().add(pair.getValue().get(0)*pair.getValue().get(2));
				
			}
		}
		
		
	}
	/**
	 * This function is used to calculate the IDF values for the input query file
	 * @param dictionary
	 * @param inputDictionary
	 * @param tfGlobalMap2
	 */
	private void processIDF(List<List<Map<String, List<Double>>>> dictionary,List<List<Map<String, List<Double>>>> inputDictionary,
			Map<String, Integer> tfGlobalMap2) {
		// TODO Auto-generated method stub
		List<Map<String,List<Double>>> oneFile = inputDictionary.get(0); //since single file input, only one element in list 
		for (int i = 0; i < oneFile.size(); i++) {
			Map<String,List<Double>> mapForRow = oneFile.get(i);
			Iterator<Entry<String, List<Double>>> iterator = mapForRow.entrySet().iterator();
			while(iterator.hasNext()){
				Map.Entry<String, List<Double>> pair = iterator.next();
								
				Integer globalMapCount = tfGlobalMap2.get(pair.getKey());
				if(globalMapCount==null)
					globalMapCount = 1;
				
				Double inverse = new Double(( oneFile.size() * dictionary.size())/ globalMapCount ) ; // tf value at 0 , size is now N, because pushing input tfidfs to new dictionary
				
				
				//Using the Sallen & Buckley Formula for calculating TF_IDF
				Double expression = 0.5 * (pair.getValue().get(0));
				if(maxTermFrequency>0.0)
				{
					expression=0.5 + expression/maxTermFrequency;
				}
				else
				{
					expression = 0.0;
				}
				pair.getValue().add(Math.log(inverse)); // idf
				pair.getValue().add(0.0); // temporary value idf2,  will be added by processIDF2
				pair.getValue().add(Math.log(inverse)*expression); // tf-idf
			}
		}
	}

	
	
	
	/**
	 * This function is used to update the global map with the gesture words created for the input query file
	 * @param inputFileGesturewords2
	 */
	private void updateGlobalMap(List<Map<String, List<Double>>> inputFileGesturewords2) {
		for (int i = 0; i < inputFileGesturewords2.size(); i++) {
			Map<String, List<Double>> tmpMap = inputFileGesturewords2.get(i); 
			Iterator<Entry<String, List<Double>>> mapIterator = tmpMap.entrySet().iterator();
			while(mapIterator.hasNext()){
				Map.Entry<String, List<Double>> pairs = mapIterator.next();
				if(globalDict.containsKey(pairs.getKey())){
					globalDict.put(pairs.getKey(), globalDict.get(pairs.getKey())+1);
				}else
					globalDict.put(pairs.getKey(),1);
			}
		}
	}

	/**
	 * This function is used to sort the scores of cosine similarity in descending order
	 * @param passedMap
	 * @return
	 */
	public static LinkedHashMap sortHashMapByValuesD(HashMap passedMap) {
		   List mapKeys = new ArrayList(passedMap.keySet());
		   List mapValues = new ArrayList(passedMap.values());
		   Collections.sort(mapValues,Collections.reverseOrder());
		   Collections.sort(mapKeys,Collections.reverseOrder());

		   LinkedHashMap sortedMap = 
		       new LinkedHashMap();

		   Iterator valueIt = mapValues.iterator();
		   while (valueIt.hasNext()) {
		       Object val = valueIt.next();
		    Iterator keyIt = mapKeys.iterator();

		    while (keyIt.hasNext()) {
		        Object key = keyIt.next();
		        String comp1 = passedMap.get(key).toString();
		        String comp2 = val.toString();

		        if (comp1.equals(comp2)){
		            passedMap.remove(key);
		            mapKeys.remove(key);
		            sortedMap.put(key, (Double)val);
		            break;
		        }

		    }

		}
		return sortedMap;
		}
	
	/**
	 * This function is used to calculate cosine similarity measure between a single univariate time series of input data and gesture words dictionary
	 * @param hashMap
	 * @param hashMap2
	 * @return
	 */
	
	public static double calculateCosineSimilarity(Map<String, Double> hashMap, Map<String, Double> hashMap2) {
		String[] keys = new String[hashMap.size()];
		hashMap.keySet().toArray(keys);
		float ans = 0;
		
		//Calculate the dot product
		for (int i = 0; i < keys.length; i++) {
			if (hashMap2.containsKey(keys[i])) {
				ans += hashMap.get(keys[i]) * hashMap2.get(keys[i]);
				
			}
		}
		float magnitude1 = 0;
		
		for (int i = 0; i < keys.length; i++) {
			magnitude1 += (hashMap.get(keys[i]) * hashMap.get(keys[i]));
		}
		magnitude1 = (float) Math.sqrt(magnitude1);
		String[] keys2 = new String[hashMap2.size()];
		hashMap2.keySet().toArray(keys2);
		float magnitude2 = 0;
		for (int i = 0; i < keys2.length; i++) {
			magnitude2 += hashMap2.get(keys2[i]) * hashMap2.get(keys2[i]);
		}
		magnitude2 = (float) Math.sqrt(magnitude2);				
		if(magnitude1 ==0 || magnitude2 == 0)
		return 0;
		//Formula to calculate cosine similarity
		return (float) (ans / (magnitude2 * magnitude1));
	}
}
