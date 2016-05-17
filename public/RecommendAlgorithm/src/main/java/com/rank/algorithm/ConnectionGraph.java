package com.rank.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class ConnectionGraph {
	private final Integer pairsNumber;
	private ArrayList<ArrayList<String>> nodeSet;
	private Map<String, Integer> keyRelativesCount;

	public ConnectionGraph(ArrayList<ArrayList<String>> nodeSet, Integer pairsNumber) {
		this.nodeSet = nodeSet;
		this.pairsNumber = pairsNumber;
	}

	public Map<String, Integer> getKeyCount() {
		if (keyRelativesCount == null) {
			System.out.println("keyCount doesn't exist");
			return null;
		}
		return this.keyRelativesCount;
	}

	public ArrayList<ArrayList<String>> CalculateKeywords() throws IllegalArgumentException {
		if (nodeSet == null) {
			throw new IllegalArgumentException("nodeSet hasn't been initialized");
		}
		Map<String, HashMap<String, Integer>> keyMap = new HashMap<String, HashMap<String, Integer>>();
		Map<String, Integer> keyCount = new HashMap<String, Integer>();
		for (List<String> it : nodeSet) {
			for (String adjacentIndex : it) {
				for (String adjacentValue : it) {
					// no need to record node itself, only relatives
					if (adjacentIndex.equals(adjacentValue)) {
						continue;
					} else {
						// initialize the relative map
						if (!keyMap.containsKey(adjacentIndex)) {
							keyMap.put(adjacentIndex, new HashMap<String, Integer>());
						}
						// obtain the hashmap indexed by adjacentIndex and
						// update it(add 1)
						HashMap<String, Integer> certainMap = keyMap.get(adjacentIndex);
						Integer tempValue = certainMap.containsKey(adjacentValue) ? certainMap.get(adjacentValue) + 1
								: 1;
						certainMap.put(adjacentValue, tempValue);

						// update the number of relatives of adjacentIndex
						tempValue = keyCount.containsKey(adjacentIndex) ? keyCount.get(adjacentIndex) + 1 : 1;
						keyCount.put(adjacentIndex, tempValue);
					}
				}
			}
		}
		// sort the keyCount based on number of relatives
		ValueComparator VC = new ValueComparator(keyCount);
		TreeMap<String, Integer> sortMap = new TreeMap<String, Integer>(VC);
		sortMap.putAll(keyCount);
		this.keyRelativesCount = sortMap;
		// extract key pairs
		return extractKeys(keyMap, sortMap);
	}

	private ArrayList<ArrayList<String>> extractKeys(Map<String, HashMap<String, Integer>> keyMap,
			Map<String, Integer> keyCount) {
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		HashSet<String> resultSnapShot = new HashSet<String>();

		Iterator<Entry<String, Integer>> entries = keyCount.entrySet().iterator();
		while (resultSnapShot.size() < 2 * pairsNumber) {
			if (!entries.hasNext()) {
				break;
			}
			ArrayList<String> tempList = new ArrayList<String>();
			tempList.clear();
			String firstString = entries.next().getKey();
			if (resultSnapShot.contains(firstString))
				continue;
			resultSnapShot.add(firstString);
			tempList.add(firstString);
			String secondString = findMaxRelative(keyMap.get(firstString), resultSnapShot, keyCount);

			if (secondString != null) {
				resultSnapShot.add(secondString);
				tempList.add(secondString);
			}
			result.add(tempList);
		}
		return result;
	}

	private String findMaxRelative(HashMap<String, Integer> relatives, HashSet<String> resultSnapShot,
			Map<String, Integer> keyCount) {
		Iterator<Entry<String, Integer>> entry = relatives.entrySet().iterator();
		String result = null;
		int maxCount = 0;
		while (entry.hasNext()) {
			Entry<String, Integer> iteratorEntry = entry.next();
			if (result == null || maxCount <= iteratorEntry.getValue()) {
				// already output to final keywords list
				if (resultSnapShot.contains(iteratorEntry.getKey())) {
					continue;
				}
				// choose the one with more relatives when their number of
				// connections with firstString is the same
				if (result != null && maxCount == iteratorEntry.getValue()
						&& keyCount.get(result) > keyCount.get(iteratorEntry.getKey())) {
					continue;
				}
				result = iteratorEntry.getKey();
				maxCount = iteratorEntry.getValue();
			}
		}
		return result;
	}
}
