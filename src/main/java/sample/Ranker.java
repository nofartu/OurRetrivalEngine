package sample;

import java.util.*;

import static sample.Indexer.docsCoprus;

public class Ranker {
    HashMap<String, Double> allDocs;

    public Ranker() {
        allDocs = new HashMap<>();
    }


    public void rankAll(HashMap<String, ArrayList<String[]>> docsContainsQuery, HashMap<String, Integer> countWordsQuery, int numOfDocs) {
        double sum = 0;
        double k = 1.2, b = 0.75;
        double avdl = getAvdl();
        for (Map.Entry<String, ArrayList<String[]>> entry : docsContainsQuery.entrySet()) {
            for (String[] info : entry.getValue()) {
                String key = info[0]; //word name
                int cWQ = countWordsQuery.get(key);
                int cWD = Integer.parseInt(info[1]); //word tf
                int dLength = Integer.parseInt(info[2]); // |d|
                int df = Integer.parseInt(info[3]); //df of word
                double tmp = cWQ * (((k + 1) * cWD) / (cWD + k * (1 - b + b * (dLength / avdl)))) * Math.log10((numOfDocs + 1) / df);
                sum = sum + tmp;
            }
            allDocs.put(entry.getKey(), sum);
            sum = 0;
        }
        getTop50();
    }

    public void rankBM25(){

    }
    public void rankTfIdf(){

    }

    private double getAvdl() {
        int sum = 0;
        for (int i = 0; i < docsCoprus.size(); i++) {
            sum += docsCoprus.get(i).getSize();
        }
        int total = docsCoprus.size();
        if (total == 0) {
            total = 1;
        }
        return sum / total;
    }

    public TreeMap<String, Double> getTop50() {
        TreeMap<String, Double> sorted = new TreeMap<>(new ValueComparator(allDocs));
        sorted.putAll(allDocs);
        System.out.println("hey i'm here");
        return sorted;

    }

    class ValueComparator implements Comparator<String> {

        HashMap<String, Double> map = new HashMap<String, Double>();

        public ValueComparator(HashMap<String, Double> map) {
            this.map.putAll(map);
        }

        @Override
        public int compare(String s1, String s2) {
            if (map.get(s1) >= map.get(s2)) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
