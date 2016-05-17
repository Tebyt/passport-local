package com.rank.algorithm;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

class ValueComparator implements Comparator<String> {
    Map<String,Integer> base ;

    public ValueComparator(Map<String, Integer> base) {
        this.base=base;
    }

    @Override
    public int compare(String a, String b) {

        if (((Integer) base.get(a)).intValue() < ((Integer) base.get(b)).intValue()) {
            return 1;
        } else if ( ((Integer) base.get(a)).intValue() == ((Integer) base.get(b)).intValue()) {
            return ((String)a).compareTo(((String)b));
        } else {
            return -1;
        }
    }
}
