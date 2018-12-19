package sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import static sample.Indexer.docsCoprus;

public class Ranker {
    HashMap<String,Integer> allDocs;

    public Ranker() {
allDocs=new HashMap<>();
    }

    public void rankBM25(HashMap<String, ArrayList<String[]>> docsContainsQuery, HashMap<String, Integer> countWordsQuery, int numOfDocs) {
        double sum = 0;
        double k = 1.2, b = 0.75;
        double avdl = getAvdl();
        int count = 0;
        int count2 = 0;
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
            System.out.println("doc num: " + entry.getKey() + " and the bm25 is: " + sum);
            sum = 0;
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
}
