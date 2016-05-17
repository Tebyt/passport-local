package com.rank.rss;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.codehaus.groovy.tools.shell.completion.KeywordSyntaxCompletor;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import info.debatty.java.stringsimilarity.*;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.util.*;

public class RssReader {

	public static final String TITLE_TAG = "title";
	public static final String LINK_TAG = "link";
	public static final String DESCRIPTION_TAG = "description";
	public static final String CONTENT_TAG = "enclosure";
	public static final String HREF_ATTR = "href";
	public static final String IMAGE_URL = "enclosure";
	private static final float SIMLARITY_THRESHOLD = (float) 0.9;
	private final String serializedClassifier;

	private HashMap<String, String> urlSourcePairs;
	private String xmlIndex;
	private Map<String, Integer> wordCount = new HashMap<String, Integer>();
	private String feedUrlPrefix;

	@SuppressWarnings("unchecked")
	public RssReader(String xmlAddress, String xmlIndex, HashMap<String, String> urlSourcePairs,
			String serializedClassifier) throws IOException, Throwable {
		super();
		this.xmlIndex = xmlIndex;
		this.urlSourcePairs = urlSourcePairs;
		this.feedUrlPrefix = xmlAddress;
		this.serializedClassifier = serializedClassifier;

	}

	public List<RssItem> extractKeyPair(List<List<String>> rawKeyGroup) {
		return null;
	}

	public String getXmlIndex() {
		return xmlIndex;
	}

	private static String nodeToString(Node node) {
		StringWriter sw = new StringWriter();
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.transform(new DOMSource(node), new StreamResult(sw));
		} catch (TransformerException te) {
			System.out.println("nodeToString Transformer Exception");
		}
		return sw.toString();
	}

	public List<RssItem> read(String feedItemXPath, String dateFormat, String publicationDateTag)
			throws InterruptedException, ClassCastException, ClassNotFoundException {
		List<RssItem> rssItemList = new ArrayList<RssItem>();
		String url = feedUrlPrefix;

		try {
			// Load CRF classifier
			// String serializedClassifier =
			// "classifiers_/english.all.3class.distsim.crf.ser.gz";
			AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier.getClassifier(serializedClassifier);
			// Create DOM model to parse things in url
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url);
			Element root = doc.getDocumentElement();

			// Catch all the 'feedItemXpath' element in XML file
			XPath xPath = XPathFactory.newInstance().newXPath();
			XPathExpression expression = xPath.compile(feedItemXPath);
			NodeList nodeList = (NodeList) expression.evaluate(root, XPathConstants.NODESET);

			// words used to construct the graph
			List<List<String>> totalKeywordList = new ArrayList<List<String>>();
			for (int index = 0; index < nodeList.getLength(); index++) {
				Node node = nodeList.item(index);

				Node titleChild = (Node) xPath.compile(TITLE_TAG).evaluate(node, XPathConstants.NODE);
				String titleString = titleChild.getTextContent().replaceAll("\"", "&quote;");

				// check if title contains Chinese
				boolean containChinese = false;
				for (int countChinese = 0; countChinese < titleString.length(); countChinese++) {
					if (Character.UnicodeScript
							.of(titleString.codePointAt(countChinese)) == Character.UnicodeScript.HAN) {
						containChinese = true;
						break;
					}
				}
				if (containChinese)
					continue; // skip when encounter Chinese

				Node linkChild = (Node) xPath.compile(LINK_TAG).evaluate(node, XPathConstants.NODE);
				String linkString = extractLinkString(linkChild);

				// Get domain part of a URI
				int slashslash = linkString.indexOf("//") + 2;
				String domain = linkString.substring(slashslash, linkString.indexOf('/', slashslash));

				// Initialization of urlSourcePairs has been commented??
				String sourceString = urlSourcePairs.get(domain);

				Node descriptionChild = (Node) xPath.compile(DESCRIPTION_TAG).evaluate(node, XPathConstants.NODE);
				String shortDescription = Jsoup.parse(descriptionChild.getTextContent()).text()
						.replaceAll("\"", "&quote;").replaceAll("\\[…\\]", "");

				System.out.printf("!!!!!!!!%s%n", shortDescription);

				Node contentChild = (Node) xPath.compile(CONTENT_TAG).evaluate(node, XPathConstants.NODE);
				String contentString = descriptionChild.getTextContent();
				String imageUrl = "";
				if (contentChild != null) {
					imageUrl = contentChild.getAttributes().getNamedItem("url").getNodeValue();// "http:"
																								// +
																								// descriptionParser.imageUrl(contentString);
				}
				Node publicationDateChild = (Node) xPath.compile(publicationDateTag).evaluate(node,
						XPathConstants.NODE);
				Date publicationDate = null;
				if (publicationDateChild == null) {
					System.out.println("no publication Date, assign it with today's date");
					publicationDate = Calendar.getInstance().getTime();

				} else {
					publicationDate = getPublicationDateContent(publicationDateChild, dateFormat);
				}

				/* Extract keywords(NN) from feed text using coreNPL library */
				String text = shortDescription + ";" + titleString; // "I am
																	// feeling
																	// very sad
																	// and
																	// frustrated.";
				StringBuilder keywords = new StringBuilder("");

				// NLP part
				NLP nlp = new NLP(classifier);
				List<String> wordsinarticle = nlp.process(titleString, text);

				totalKeywordList.add(wordsinarticle);
				for (Iterator iterator = wordsinarticle.iterator(); iterator.hasNext();) {
					String s = (String) iterator.next();
					// System.out.print(s+"#");
					keywords.append(s + "; ");

				}

				if (keywords.toString().length() - 2 < 0) {
					continue;
				}
				System.out.println("KK " + keywords);
				rssItemList.add(new RssItem(titleString, linkString, shortDescription, publicationDate, // relatedArticles,
						sourceString, imageUrl, keywords.toString().substring(0, keywords.toString().length() - 2)));

				wordCount.clear();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		return rssItemList;
	}

	private void sentenceList(List<String> sentence) {
		// TODO Auto-generated method stub

	}

	/**
	 * Extract Organization, Location, Person name from text
	 * 
	 * @param text
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ClassCastException
	 */
	public List<String> extractEntities(String text, AbstractSequenceClassifier<CoreLabel> classifier)
			throws ClassCastException, ClassNotFoundException, IOException {
		StringBuilder resultEntities = new StringBuilder("");

		HashMap<String, HashMap<String, Integer>> entities = new HashMap<String, HashMap<String, Integer>>();
		List<String> entitieNames = new ArrayList<String>();
		String inlineXMLAnnotations = classifier.classifyWithInlineXML(text);
		System.out.println("<Org><Per><Loc> String: " + inlineXMLAnnotations);
		// extract content between <ORGANIZATION> tag
		Pattern p = Pattern.compile("<ORGANIZATION>(.*?)</ORGANIZATION>");
		if (inlineXMLAnnotations.indexOf("<ORGANIZATION>") != -1) {
			Matcher m = p.matcher(inlineXMLAnnotations);
			while (m.find()) {
				entitieNames = existSimilarWordInSameArticle(entitieNames, m.group(1));
				/*
				 * if (existSimilarWordInSameArticle(entitieNames, m.group(1)))
				 * continue; entitieNames.add(m.group(1));
				 * System.out.println(m.group(1));
				 */
			}
		}
		if (inlineXMLAnnotations.indexOf("<PERSON>") != -1) {
			p = Pattern.compile("<PERSON>(.*?)</PERSON>");
			Matcher m = p.matcher(inlineXMLAnnotations);
			while (m.find()) {
				entitieNames = existSimilarWordInSameArticle(entitieNames, m.group(1));
				/*
				 * if (existSimilarWordInSameArticle(entitieNames, m.group(1)))
				 * continue; entitieNames.add(m.group(1));
				 * System.out.println(m.group(1));
				 */
			}
		}
		if (inlineXMLAnnotations.indexOf("<LOCATION>") != -1) {
			p = Pattern.compile("<LOCATION>(.*?)</LOCATION>");
			Matcher m = p.matcher(inlineXMLAnnotations);
			while (m.find()) {
				entitieNames = existSimilarWordInSameArticle(entitieNames, m.group(1));
				/*
				 * if (existSimilarWordInSameArticle(entitieNames, m.group(1)))
				 * continue; entitieNames.add(m.group(1));
				 * System.out.println(m.group(1));
				 */
			}
		}

		// assemble adjacent non-O things
		for (List<CoreLabel> lcl : classifier.classify(text)) {

			Iterator<CoreLabel> iterator = lcl.iterator();

			if (!iterator.hasNext())
				continue;

			CoreLabel cl = iterator.next();

			while (iterator.hasNext()) {
				String answer = cl.getString(CoreAnnotations.AnswerAnnotation.class);

				if (answer.equals("O")) {
					cl = iterator.next();
					continue;
				}

				if (!entities.containsKey(answer))
					entities.put(answer, new HashMap<String, Integer>());

				String value = cl.getString(CoreAnnotations.ValueAnnotation.class);

				while (iterator.hasNext()) {
					cl = iterator.next();
					if (answer.equals(cl.getString(CoreAnnotations.AnswerAnnotation.class)))
						value = value + " " + cl.getString(CoreAnnotations.ValueAnnotation.class);
					else {
						if (!entities.get(answer).containsKey(value))
							entities.get(answer).put(value, 0);

						entities.get(answer).put(value, entities.get(answer).get(value) + 1);

						break;
					}
				}

				if (!iterator.hasNext())
					break;
			}
		}
		return entitieNames;
	}

	/**
	 * Check if there already exist similar word/sentence
	 * 
	 * @param words
	 * @param pattern
	 * @return
	 */

	private boolean existSimilarWordCheck(List<String> words, String pattern) {
		// This distance is computed as levenshtein distance divided by the
		// length of the longest string
		NormalizedLevenshtein l = new NormalizedLevenshtein();
		boolean isExist = false;
		for (String word : words) {
			// If they are similar words
			if (l.distance(word, pattern) < SIMLARITY_THRESHOLD || word.indexOf(pattern) != -1
					|| pattern.indexOf(word) != -1) {
				isExist = true;
				break;
			}
		}
		return isExist;
	}

	private boolean existSimilarWord(List<String> words, String pattern) {
		// This distance is computed as levenshtein distance divided by the
		// length of the longest string
		NormalizedLevenshtein l = new NormalizedLevenshtein();
		boolean isExist = false;
		for (String word : words) {
			// If they are similar words
			if (l.distance(word, pattern) < SIMLARITY_THRESHOLD || word.indexOf(pattern) != -1
					|| pattern.indexOf(word) != -1) {
				wordCount.put(word, (Integer) wordCount.get(word) + 1);
				isExist = true;
				break;
			}
		}
		if (!isExist) {
			wordCount.put(pattern, 1);
		}
		return isExist;
	}

	private List<String> existSimilarWordInSameArticle(List<String> words, String pattern) {
		// This distance is computed as levenshtein distance divided by the
		// length of the longest string
		NormalizedLevenshtein l = new NormalizedLevenshtein();
		boolean isExist = false;
		for (String word : words) {
			// If they are similar words
			if (l.distance(word, pattern) < SIMLARITY_THRESHOLD || word.indexOf(pattern) != -1
					|| pattern.indexOf(word) != -1) {
				isExist = true;
				break;
			}
		}
		if (!isExist) {
			words.add(pattern);
		}
		return words;
	}

	private Date getPublicationDateContent(Node descriptionChild, String dateFormat) {
		SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
		Date date = null;
		try {
			date = formatter.parse(descriptionChild.getTextContent());

		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}

	/**
	 * Handle two types of links in node: 1) <link>www.example.com</link> 2)
	 * <link href="www.example.com" />
	 */
	private String extractLinkString(Node linkChild) {
		String url = linkChild.getTextContent();
		if (url == null || url.isEmpty()) {
			// try to find url among tag attributes
			url = linkChild.getAttributes().getNamedItem(HREF_ATTR).getNodeValue();
		}
		return url;
	}

	/**
	 * Sorted map for sorting word by their occurrence number
	 * 
	 * @param wordCount
	 * @return
	 */
	public TreeMap<String, Integer> SortByValue(Map<String, Integer> wordCount) {
		ValComparator vc = new ValComparator(wordCount);
		TreeMap<String, Integer> sortedMap = new TreeMap<String, Integer>(vc);
		sortedMap.putAll(wordCount);
		return sortedMap;
	}

}

class ValComparator implements Comparator<String> {

	Map<String, Integer> map;

	public ValComparator(Map<String, Integer> base) {
		this.map = base;
	}

	public int compare(String a, String b) {
		if (map.get(a) >= map.get(b)) {
			return -1;
		} else {
			return 1;
		} // returning 0 would merge keys
	}
}
