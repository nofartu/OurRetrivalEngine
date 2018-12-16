package sample;

import javafx.util.Pair;

import java.util.HashMap;
import java.util.TreeMap;


public class Documents {

    private String idDoc;
    private HashMap<String, Integer[]> terms;
    private int max_tf;
    private int numOfUniqe;
    private String origin;
    private int size;

    public Documents(String id, HashMap<String, Integer[]> termsMap) {
        idDoc = id;
        terms = termsMap;
        max_tf = 0;
        numOfUniqe = 0;
        origin = "";
        size = 0;
    }


    public String getIdDoc() {
        return idDoc;
    }

    public HashMap<String, Integer[]> getTerms() {
        return terms;
    }

    public int getMax_tf() {
        return max_tf;
    }

    public int getNumOfUniqe() {
        return numOfUniqe;
    }

    public String getOrigin() {
        return origin;
    }

    public void setIdDoc(String idDoc) {
        this.idDoc = idDoc;
    }

    public void setTerms(HashMap<String, Integer[]> terms) {
        this.terms = terms;
    }

    public void setMax_tf(int max_tf) {
        this.max_tf = max_tf;
    }

    public void setNumOfUniqe(int numOfUniqe) {
        this.numOfUniqe = numOfUniqe;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
