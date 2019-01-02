package sample;

import java.util.*;

import static sample.ApiJson.getCities;
import static sample.Indexer.docsCoprus;
import static sample.ReadFile.mySplit;
import static sample.ViewController.chosenCities;

public class Ranker {
    private HashMap<String, Double[]> allDocs; //all the documents by the query
    private HashMap<String, Double[]> allDocsDesc; //all the documents by the description
    private HashMap<String, Double[]> allDocsSemantic; //all the documents by semantic
    private HashMap<String, Double> finalScore; //calculated all- final rank
    private double avdl;
    private int numOfDocs;
    private boolean isCity;

    public Ranker(int numOfDocs, boolean isCity) {
        this.numOfDocs = numOfDocs;
        allDocs = new HashMap<>();
        allDocsDesc = new HashMap<>();
        allDocsSemantic = new HashMap<>();
        finalScore = new HashMap<>();
        avdl = getAvdl();
        this.isCity = isCity;
    }

    //ranking all the documents by the BM25
    private void rankBM25(HashMap<String, ArrayList<String[]>> docsContains, HashMap<String, Integer> countWords, int whichOne) {
        double sum = 0;
        double k = 1.2, b = 0.75;
        for (Map.Entry<String, ArrayList<String[]>> entry : docsContains.entrySet()) { //runs by document
            for (String[] info : entry.getValue()) {
                String key = info[0]; //word name
                int cWQ = countWords.get(key);
                int cWD = Integer.parseInt(info[1]); //word tf
                int dLength = Integer.parseInt(info[2]); // |d| - length of a document
                int df = Integer.parseInt(info[3]); //df of word
                double tmp = Math.log10((numOfDocs - df + 0.5) / (df + 0.5)) * ((cWD * (k + 1)) / (cWD + k * (1 - b + b * (dLength / avdl))));
                sum = sum + tmp; //summing all the words of the query that is in the document
            }

            Double[] d = {sum, 0.0, 0.0, 0.0};   //{BM25, TFIDF , LOCATION , COSSIM}
            if (whichOne == 0)
                allDocs.put(entry.getKey(), d);
            if (whichOne == 1)
                allDocsDesc.put(entry.getKey(), d);
            if (whichOne == 2)
                allDocsSemantic.put(entry.getKey(), d);
            sum = 0;
        }
    }



    /*
    GATHERS ALL THE RANKING FOR ALL THE DOCUMENTS AND RETURNS THE TOP 50 OF THE RANKED FILE RELATED TO THE QUERY
     */
    public TreeMap<String, Double> rankAll(HashMap<String, ArrayList<String[]>> docsContainsQuery, HashMap<String, Integer> countWordsQuery, HashMap<String, ArrayList<String>> wordAndLocations,
                                           HashMap<String, ArrayList<String[]>> docsContainsDesc, HashMap<String, Integer> countWordsDesc, HashMap<String, ArrayList<String>> wordAndLocationsDesc,
                                           HashMap<String, ArrayList<String[]>> docsContainsSemantic, HashMap<String, Integer> countWordsSemantic, HashMap<String, ArrayList<String>> wordAndLocationsSemantic, boolean ifSemantic) {
        //sends the query to get ranking
        rankBM25(docsContainsQuery, countWordsQuery, 0);
        rankTfIdfAndLocation(wordAndLocations, 0);
        //sends the description to  get ranking
        rankBM25(docsContainsDesc, countWordsDesc, 1);
        rankTfIdfAndLocation(wordAndLocationsDesc, 1);
        //in case semantic selected sends the semantic to get ranking
        if (ifSemantic) {
            rankBM25(docsContainsSemantic, countWordsSemantic, 2);
            rankTfIdfAndLocation(wordAndLocationsSemantic, 2);
        }

        //calculates for each document the final rank by whights and the all ranking
        for (Map.Entry<String, Double[]> entry : allDocs.entrySet()) {
            Double[] ranking = entry.getValue();
            if (ifSemantic) { //calculate the ranking with semantic
                //in try and catch to a case where there are docs that related to the query and doesn't find it
                try {
                    Double[] semanticRank = allDocsSemantic.get(entry.getKey());
                    try {
                        Double[] descRank = allDocsDesc.get(entry.getKey());
                        finalScore.put(entry.getKey(), ((ranking[0] * 0.98 + (ranking[1] / ranking[3]) * 0.01 + ranking[2] * 0.01) * 0.8 + (descRank[0] * 0.99 + descRank[2] * 0.01) * 0.2) * 0.8 + (semanticRank[0] * 0.99 + semanticRank[2] * 0.01) * 0.2);
                    } catch (Exception e1) {
                        finalScore.put(entry.getKey(), (ranking[0] * 0.98 + (ranking[1] / ranking[3]) * 0.01 + ranking[2] * 0.01) * 0.8 + (semanticRank[0] * 0.99 + semanticRank[2] * 0.01) * 0.2);
                    }
                } catch (Exception e) {
                    try {
                        Double[] descRank = allDocsDesc.get(entry.getKey());
                        finalScore.put(entry.getKey(), (ranking[0] * 0.98 + (ranking[1] / ranking[3]) * 0.01 + ranking[2] * 0.01) * 0.8 + (descRank[0] * 0.99 + descRank[2] * 0.01) * 0.2);
                    } catch (Exception e1) {
                        finalScore.put(entry.getKey(), (ranking[0] * 0.98 + (ranking[1] / ranking[3]) * 0.01 + ranking[2] * 0.01));
                    }
                }
            } else {
                try {
                    Double[] descRank = allDocsDesc.get(entry.getKey());
                    finalScore.put(entry.getKey(), (ranking[0] * 0.98 + (ranking[1] / ranking[3]) * 0.01 + ranking[2] * 0.01 ) * 0.8 + (descRank[0] * 0.99 + descRank[2] * 0.01 ) * 0.2);
                } catch (Exception e1) {
                    finalScore.put(entry.getKey(), (ranking[0] * 0.98 + (ranking[1] / ranking[3]) * 0.01 + ranking[2] * 0.01));
                }
            }
        }
        TreeMap<String, Double> sorted = new TreeMap<>(new ValueComparator(finalScore));
        sorted.putAll(getTop50()); //get the top 50 sorted by decreasing order(where the first one is the higher)
        return sorted;
    }

    //calculates all the ranking for location and cossim
    private void rankTfIdfAndLocation(HashMap<String, ArrayList<String>> wordAndLocations, int whichOne) {
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
                int dLength = Integer.parseInt(mySplit(key.get(2), "/").get(1)); // |d| - length of a document
                int location = Integer.parseInt(mySplit(key.get(2), "/").get(0)); //location of the word in the document
                double tf = (double) cWD / dLength;
                double tfIdf = tf * idf;
                if (whichOne == 0) {
                    Double[] d = allDocs.get(key.get(0));
                    d[1] = d[1] + tfIdf; //tfidf ranking
                    d[3] = d[3] + Math.sqrt(Math.pow(tfIdf, 2) * wordAndLocations.size());
                    double calc = (double) (dLength - location) / avdl;
                    d[2] = d[2] + (double) (calc / wordAndLocations.size()); //location ranking
                    allDocs.put(key.get(0), d);
                }
                if (whichOne == 1) {
                    Double[] d = allDocsDesc.get(key.get(0));
                    d[1] = d[1] + tfIdf; //tfidf ranking
                    double calc = (double) (dLength - location) / avdl;
                    d[2] = d[2] + (double) (calc / wordAndLocations.size()); //location ranking
                    allDocsDesc.put(key.get(0), d);
                }
                if (whichOne == 2) {
                    Double[] d = allDocsSemantic.get(key.get(0));
                    d[1] = d[1] + tfIdf; //tfidf ranking
                    double calc = (double) (dLength - location) / avdl;
                    d[2] = d[2] + (double) (calc / wordAndLocations.size()); //location ranking
                    allDocsSemantic.put(key.get(0), d);
                }

            }
        }
    }

    /*
    returns the average length of the document
     */
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

    /*
    returns the top 50 documents sorted in decreasing order where the first one is the highest
     */
    private TreeMap<String, Double> getTop50() {

        TreeMap<String, Double> sorted = new TreeMap<>(new ValueComparator(finalScore));
        if (isCity) {
            sorted.putAll(withCities());
        } else {
            sorted.putAll(finalScore);
        }
        TreeMap<String, Double> finalsorted = new TreeMap<>(new ValueComparator(sorted));
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

    //returns the joint documents that contains the city and ranked high by the ranking algorithem
    private TreeMap<String, Double> withCities() {
        TreeMap<String, Double> combinedFilesWithCity = new TreeMap<>();
        HashMap<String, ArrayList<String>> files;
        HashMap<String, Double> tmp = new HashMap<>();
        tmp.putAll(finalScore);
        for (String city : chosenCities) {
            files = getAllCityPostings(city);
            for (Map.Entry<String, ArrayList<String>> entry : files.entrySet()) {
                if (tmp.containsKey(entry.getKey())) {
                    combinedFilesWithCity.put(entry.getKey(), tmp.get(entry.getKey()));
                }
            }
        }
        return combinedFilesWithCity;
    }

    private HashMap<String, ArrayList<String>> getAllCityPostings(String word) {
        HashMap<String, City> c = getCities();
        return c.get(word).getLocations();
    }

    //comperator for using the tree map
    class ValueComparator implements Comparator<String> {

        TreeMap<String, Double> map = new TreeMap<>();

        public ValueComparator(HashMap<String, Double> map) {
            this.map.putAll(map);
        }

        public ValueComparator(TreeMap<String, Double> map) {
            this.map.putAll(map);
        }

        @Override
        public int compare(String s1, String s2) {
            if (map.get(s1) >= map.get(s2)) {
                return -1;
            } else if (map.get(s1).equals(map.get(s2))) {
                return 0;
            } else {
                return 1;
            }
        }
    }
}
