package sample;

import javafx.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;


public class Documents {

    private String idDoc;
    private HashMap<String, Integer[]> terms;
    private int max_tf;
    private int numOfUniqe;
    private String origin;
    private int size;
    private HashMap<String, Double> entities;

    public Documents(String id, HashMap<String, Integer[]> termsMap) {
        idDoc = id;
        terms = termsMap;
        max_tf = 0;
        numOfUniqe = 0;
        origin = "";
        size = 0;
        entities = new HashMap<>();
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

    public void resetTerms() {
        this.terms = new HashMap<>();
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

    public int getSize() {
        return size;
    }

    public void addEnity(String entity, double dominance) {
        entities.put(entity, dominance);
    }

    public void setEntities(HashMap<String, Double> entities) {
        this.entities = entities;
    }

    public HashMap<String, Double> getEntities() {
        return entities;
    }

    public int getSizeOfEntity() {
        return entities.size();
    }

    public void removeFromEntity(String name) {
        entities.remove(name);
    }
}
