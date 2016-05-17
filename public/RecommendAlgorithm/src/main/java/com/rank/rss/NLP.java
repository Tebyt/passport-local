package com.rank.rss;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

public class NLP {
	private static final float SIMLARITY_THRESHOLD = (float) 0.9;
	private final AbstractSequenceClassifier<CoreLabel> classifier;

	public NLP(AbstractSequenceClassifier<CoreLabel> classifier) {
		this.classifier = classifier;
	}

	public List<String> process(String titleString, String text) {
		List<String> wordsinarticle = null;

		try {
			wordsinarticle = extractEntities(titleString + " ; " + text, this.classifier);

			/*****************
			 * NLP extract nouns from sentences
			 ****************/
			Properties props = new Properties();
			props.put("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment");
			// create a StanfordCoreNLP object
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
			// run all Annotators on this text
			Annotation annotation = new Annotation(text);
			long start = System.currentTimeMillis();
			pipeline.annotate(annotation);
			System.out.printf("%d microsec pass!!!!!%n", System.currentTimeMillis() - start);

			// a CoreMap is essentially a Map that uses class objects as keys
			// and has values with custom types
			List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
			System.out.println("-----------");
			/*
			 * Iterator<CoreMap> iter1 = sentences.iterator();
			 * while(iter1.hasNext())
			 * System.out.println(iter1.next().toString());
			 * System.out.println("-----------");
			 */
			for (CoreMap sentence : sentences) {
				// sentiment hasn't used ???
				String sentiment = sentence.get(SentimentCoreAnnotations.SentimentClass.class);
				// traversing the words in the current sentence
				// a CoreLabel is a CoreMap with additional
				// token-specific
				// methods
				for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
					// this is the text of the token
					String word = token.get(TextAnnotation.class);
					// this is the POS tag of the token
					String pos = token.get(PartOfSpeechAnnotation.class);
					// System.out.println("Part of Speech: "+pos + " ;Word: "+
					// word);
					if (pos.length() >= 3 && pos.substring(0, 3).equals("NNP")) {
						// Count word occurrence number

						if (!existSimilarWordCheck(wordsinarticle, word)) {
							wordsinarticle.add(word);
						}
						// this is the NER label of the token
						// String ne =
						// token.get(NamedEntityTagAnnotation.class);
					}
				}

				/*****************************************************************/
			}
		} catch (ClassCastException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return wordsinarticle;
	}

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

}
