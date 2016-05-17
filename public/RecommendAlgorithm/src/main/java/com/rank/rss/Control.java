package com.rank.rss;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import groovy.json.JsonBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import javax.json.Json;
import javax.json.JsonBuilderFactory;

import org.elasticsearch.client.Client;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.parser.Parser;
import com.google.common.io.Files;
import com.google.gson.JsonObject;
import com.neveni.algorithm.ConnectionGraph;

public class Control {

	private final int rssNumber; // rss feeder number
	private final static int PAIRSNUMBER = 10; // search the top 10 keywords
												// with most connections
	private final static int SEARCHDATERANGE = 7; // search keywords within a
													// week

	private final static String FILEPATH = "C:\\Users\\ACA4\\Documents\\Output.json"; // output
																						// to
																						// frontend
	private final static String WEBJSON = "C:\\Users\\ACA4\\Desktop\\jsonfile.json"; // web
																						// crawler
	private final static String serializedClassifier = "classifiers_/english.all.3class.distsim.crf.ser.gz";

	private ElasticSearch elasticSearchEngine;
	private RssReader[] rssArray;
	private HashMap<String, String> urlSourcePairs;

	private String[] xmlAddress = { "http://www.rssmix.com/u/8182252/rss.xml", // Movie
			"http://www.rssmix.com/u/8182265/rss.xml", // Fitness
			"http://www.rssmix.com/u/8182268/rss.xml", // Meal
			"http://www.rssmix.com/u/8182269/rss.xml", // Tour
			"http://www.rssmix.com/u/8182271/rss.xml", // Mall
			"http://www.rssmix.com/u/8182274/rss.xml", // Drink
	};

	private String[] xmlIndex = { "Movie", "Fitness", "Meal", "Tour", "Mall", "Drink" };

	public Control() {
		this.elasticSearchEngine = new ElasticSearch();
		urlSourcePairs = readUrlSourcePairs();
		rssNumber = xmlAddress.length;
		if (xmlAddress.length != xmlIndex.length) {
			System.out.println("No pair xml address and index");
			return;
		}

		this.rssArray = new RssReader[rssNumber];
		for (int i = 0; i < rssNumber; i++) {
			try {
				this.rssArray[i] = new RssReader(xmlAddress[i], xmlIndex[i], urlSourcePairs, serializedClassifier);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public RssReader getRssReader(int index) {
		return rssArray[index];
	}

	public int getRssNumber() {
		return rssNumber;
	}

	public HashMap<String, String> readUrlSourcePairs() {

		String filename = this.getClass().getClassLoader().getResource("resources/urlpair.txt").getPath();

		String line = null;
		HashMap<String, String> urlSourcePairs = new HashMap<String, String>();
		try {
			FileReader fileReader = new FileReader(filename);

			// Always wrap FileReader in BufferReader
			BufferedReader br = new BufferedReader(fileReader);
			while ((line = br.readLine()) != null) {
				String[] pair = line.split(",");
				urlSourcePairs.put(pair[0], pair[1]);
			}
			br.close();

		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + filename + "'");
		} catch (IOException ex) {
			System.out.println("Error reading file '" + filename + "'");
			// Or we could just do this:
			// ex.printStackTrace();
		}
		return urlSourcePairs;
	}

	public ElasticSearch getEngine() {
		return this.elasticSearchEngine;
	}

	public void execute(RssReader rssReader) {
		String feedItemXPath = "//item";
		String dateFormat = "E, dd MMM yyyy HH:mm:ss z";
		String publicationDateTag = "pubDate";

		// read from xml file and output a list with corresponding node set
		List<RssItem> rssItemList;
		try {
			// read rss from xml
			rssItemList = rssReader.read(feedItemXPath, dateFormat, publicationDateTag);

			StringBuilder resultedJson = new StringBuilder();
			resultedJson.append("{\"RssFeeds\":[\n");

			// date as index when put data into elasticsearch
			DateFormat inputFormat = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy", Locale.US);
			DateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

			for (RssItem ri : rssItemList) {

				String dateType = outputFormat.format(inputFormat.parse(ri.getPublicationDate().toString()))
						.substring(0, 10).replace("-", "");
				// String testString = ri.toJson();
				// @argument: content, Index, Type(date), id(link address)
				elasticSearchEngine.bulkIndexDocument(ri.toJson(), rssReader.getXmlIndex(), dateType, ri.getLink());
				resultedJson.append(ri.toJson());
				if (ri != rssItemList.get(rssItemList.size() - 1)) {
					resultedJson.append(",");
				}

				resultedJson.append("\n");
			}
			resultedJson.append("]}");

			elasticSearchEngine.excuteBulkIndex();
			System.out.println(resultedJson.toString());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// extract the keyword section from json file
	public ArrayList<ArrayList<String>> parseJsonToKeywords(String resultJson) {
		ArrayList<ArrayList<String>> keyWordList = new ArrayList<ArrayList<String>>();
		try {
			JSONObject obj = new JSONObject(resultJson);
			JSONObject hitsObj = obj.getJSONObject("hits");
			JSONArray hitsArr = hitsObj.getJSONArray("hits");
			int length = hitsArr.length();
			if (length < 1) {
				System.out.println("can't find requested data");
				return keyWordList;
			}

			for (int i = 0; i < length; i++) {
				String keyWordListString = hitsArr.getJSONObject(i).getJSONObject("_source").getString("keywords");
				ArrayList<String> aList = new ArrayList<String>(Arrays.asList(keyWordListString.split(";")));
				keyWordList.add(aList);
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ConnectionGraph CG = new ConnectionGraph(keyWordList, PAIRSNUMBER);
		return CG.CalculateKeywords();
	}

	// assemble each Search Json files
	public String assembleSearchResult(ArrayList<ArrayList<String>> topKeywordList, Map<String, Integer> keyCount) {
		JSONArray keywordArray = new JSONArray();
		for (int i = 0; i < topKeywordList.size(); i++) {
			// put every keyword pair into elastic search engine and obtain the
			// search result
			ArrayList<String> singleKeyList = topKeywordList.get(i);
			String singleJson = this.getEngine().searchByKeywordList(singleKeyList, "news");
			try {
				JSONObject json = new JSONObject(singleJson);
				JSONArray arr = json.getJSONObject("hits").getJSONArray("hits");
				int size = 0;
				for (String key : singleKeyList) {
					size += keyCount.get(key);
				}
				size *= 100;
				for (int j = 0; j < arr.length(); j++) {
					JSONObject o = arr.getJSONObject(j).getJSONObject("_source");
					// calculate the total weight of each keyword list

					o.append("size", Integer.toString(size));
					keywordArray.put(o);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return keywordArray.toString();
	}

	public boolean outputJson(String content) {
		try {
			File file = new File(FILEPATH);

			if (file.exists()) {
				file.delete();
			}
			file.createNewFile();
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

}
