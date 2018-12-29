package sample;

import java.util.*;

import static sample.Indexer.docsCoprus;
import static sample.ReadFile.mySplit;

public class Ranker {
    HashMap<String, Double[]> allDocs;
    HashMap<String, Double[]> allDocsDesc;
    HashMap<String, Double[]> allDocsSemantic;
    HashMap<String, Double> finalScore;
    double avdl;

    int numOfDocs;

    public Ranker(int numOfDocs) {
        this.numOfDocs = numOfDocs;
        allDocs = new HashMap<>();
        allDocsDesc=new HashMap<>();
        allDocsSemantic=new HashMap<>();
        finalScore = new HashMap<>();
        avdl=getAvdl();
    }


    public void rankBM25(HashMap<String, ArrayList<String[]>> docsContains, HashMap<String, Integer> countWords, int whichOne) {
        double sum = 0;
        double k = 0.6, b = 0.75;
        //double avdl = getAvdl();
        for (Map.Entry<String, ArrayList<String[]>> entry : docsContains.entrySet()) {
            for (String[] info : entry.getValue()) {
                String key = info[0]; //word name
                int cWQ = countWords.get(key);
                int cWD = Integer.parseInt(info[1]); //word tf
                int dLength = Integer.parseInt(info[2]); // |d|
                int df = Integer.parseInt(info[3]); //df of word
                // double tmp = (double) cWQ * (((k + 1) * cWD) / (cWD + k * (1 - b + b * (dLength / avdl)))) * Math.log10((numOfDocs + 1) / df);
                double tmp = Math.log10((numOfDocs - df + 0.5) / (df + 0.5)) * ((cWD * (k + 1)) / (cWD + k * (1 - b + b * (dLength / avdl))));
                sum = sum + tmp;
            }

            Double[] d = {sum, 0.0, 0.0};
            if (whichOne == 0)
                allDocs.put(entry.getKey(), d);
            if (whichOne == 1)
                allDocsDesc.put(entry.getKey(), d);
            if (whichOne == 2)
                allDocsSemantic.put(entry.getKey(), d);
            sum = 0;
        }
    }


    public TreeMap<String, Double> rankAll(HashMap<String, ArrayList<String[]>> docsContainsQuery, HashMap<String, Integer> countWordsQuery, HashMap<String, ArrayList<String>> wordAndLocations,
                                           HashMap<String, ArrayList<String[]>> docsContainsDesc, HashMap<String, Integer> countWordsDesc, HashMap<String, ArrayList<String>> wordAndLocationsDesc,
                                           HashMap<String, ArrayList<String[]>> docsContainsSemantic, HashMap<String, Integer> countWordsSemantic, HashMap<String, ArrayList<String>> wordAndLocationsSemantic,boolean ifSemantic) {
        rankBM25(docsContainsQuery, countWordsQuery, 0);
        rankTfIdfAndLocation(wordAndLocations,0);
        if(ifSemantic){
            rankBM25(docsContainsSemantic, countWordsSemantic, 2);
            rankTfIdfAndLocation(wordAndLocationsSemantic,2);
        }
        for (Map.Entry<String, Double[]> entry : allDocs.entrySet()) {
            Double[] ranking = entry.getValue();
            if(ifSemantic){ //calculate with semantic
                try{
                    Double[] semanticRank=allDocsSemantic.get(entry.getKey());
                    finalScore.put(entry.getKey(), ranking[0]*0.7+semanticRank[0]*0.3 /** 0.05 + ranking[1] * 0.05 + ranking[2] * 0.98*/);//0- bm25, 1- tfidf, 2- location in doc
                }catch(Exception e){
                    finalScore.put(entry.getKey(), ranking[0] /** 0.05 + ranking[1] * 0.05 + ranking[2] * 0.98*/);//0- bm25, 1- tfidf, 2- location in doc
                }




            }
            else{
                finalScore.put(entry.getKey(), ranking[0] /** 0.05 + ranking[1] * 0.05 + ranking[2] * 0.98*/);//0- bm25, 1- tfidf, 2- location in doc
            }

        }


        TreeMap<String, Double> sorted = getTop50();
//        for (Map.Entry<String, Double> entry : sorted.entrySet()) {
//            System.out.println(entry.getKey()+" and the rank is: "+entry.getValue());
//        }
        return sorted;
    }

    public void rankTfIdfAndLocation(HashMap<String, ArrayList<String>> wordAndLocations, int whichOne) {
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
                int location = Integer.parseInt(mySplit(key.get(2), "/").get(0));
                double tf = (double) cWD / dLength;
                double tfIdf = tf * idf;
                if (whichOne == 0) {
                    Double[] d = allDocs.get(key.get(0));
                    d[1] = tfIdf;
                    d[2] = (double) dLength - location; //location ranking
                    allDocs.put(name, d);
                }
                if (whichOne == 1) {
                    Double[] d = allDocsDesc.get(key.get(0));
                    d[1] = tfIdf;
                    d[2] = (double) dLength - location; //location ranking
                    allDocsDesc.put(name, d);
                }
                if (whichOne == 2) {
                    Double[] d = allDocsSemantic.get(key.get(0));
                    d[1] = tfIdf;
                    d[2] = (double) dLength - location; //location ranking
                    allDocsSemantic.put(name, d);
                }

            }
        }
    }

    private double getAvdl() {
        int sum = 0;
        for (Map.Entry<String, Documents> entry : docsCoprus.entrySet()) {
            sum += entry.getValue().getSize();
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
        TreeMap<String, Double> finalsorted = new TreeMap<>();
        int i = 0;
        for (Map.Entry<String, Double> entry : sorted.entrySet()) {
            if (i >= 50)
                break;
            else {
                finalsorted.put(entry.getKey(), entry.getValue());
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
