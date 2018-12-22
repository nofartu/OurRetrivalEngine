package sample;

import java.util.*;

import static sample.Indexer.docsCoprus;
import static sample.ReadFile.mySplit;

public class Ranker {
    HashMap<String, Double[]> allDocs;
    HashMap<String, Double> finalScore;
    int numOfDocs;

    public Ranker(int numOfDocs) {
        this.numOfDocs = numOfDocs;
        allDocs = new HashMap<>();
        finalScore = new HashMap<>();
    }


    public void rankBM25(HashMap<String, ArrayList<String[]>> docsContainsQuery, HashMap<String, Integer> countWordsQuery) {
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
                double tmp =(double) cWQ * (((k + 1) * cWD) / (cWD + k * (1 - b + b * (dLength / avdl)))) * Math.log10((numOfDocs + 1) / df);
                sum = sum + tmp;
            }
            Double[] d = {sum, 0.0};
            allDocs.put(entry.getKey(), d);
            sum = 0;
        }

    }

    public TreeMap <String, Double> rankAll(HashMap<String, ArrayList<String[]>> docsContainsQuery, HashMap<String, Integer> countWordsQuery, HashMap<String, ArrayList<String>> wordAndLocations) {
        rankBM25(docsContainsQuery, countWordsQuery);
        rankTfIdf(wordAndLocations);
        for (Map.Entry<String, Double[]> entry : allDocs.entrySet()) {
            Double[] ranking = entry.getValue();
            finalScore.put(entry.getKey(), ranking[0] * 0.75 + ranking[1] * 0.25);
        }
        TreeMap<String,Double> sorted=getTop50();
//        for (Map.Entry<String, Double> entry : sorted.entrySet()) {
//            System.out.println(entry.getKey()+" and the rank is: "+entry.getValue());
//        }
        return sorted;
    }

    public void rankTfIdf(HashMap<String, ArrayList<String>> wordAndLocations) {
        for (Map.Entry<String, ArrayList<String>> entry : wordAndLocations.entrySet()) {
            String name = entry.getKey();
            int size = entry.getValue().size();
            String info = entry.getValue().get(size - 1);
            int df = Integer.parseInt(mySplit(info, ":").get(1));//df of word
            double idf = Math.log10(numOfDocs / df);
            for (int i = 0; i < size - 1; i++) {
                info = entry.getValue().get(i);
                ArrayList<String> key = mySplit(info, ":"); //word name
                int cWD = Integer.parseInt(key.get(1)); //word tf
                int dLength = Integer.parseInt(mySplit(key.get(2), "/").get(1)); // |d|
                double tf = (double)cWD / dLength;
                double tfIdf = tf * idf;
                // double tmp = cWQ * (((k + 1) * cWD) / (cWD + k * (1 - b + b * (dLength / avdl)))) * Math.log10((numOfDocs + 1) / df);
                Double[] d = allDocs.get(key.get(0));
                d[1] = tfIdf;
                allDocs.put(name, d);
            }
        }
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
         TreeMap<String, Double> sorted = new TreeMap<>(new ValueComparator(finalScore));
         //finalScore = new TreeMap<>(new ValueComparator(finalScore));
         sorted.putAll(finalScore);
        TreeMap<String, Double> finalsorted=new TreeMap<>();
        int i=0;
        for (Map.Entry<String, Double> entry : sorted.entrySet()) {
            if(i>=50)
                break;
            else
            {
                finalsorted.put(entry.getKey(),entry.getValue());
                i++;
            }
        }
         return finalsorted;
    }

    class ValueComparator implements Comparator<String> {

        TreeMap<String, Double> map = new TreeMap<>();

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
