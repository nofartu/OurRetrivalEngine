package sample;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.util.*;

public class Stemmer {

    private SnowballStemmer snowballStemmer = new englishStemmer();

    public HashMap<String, Integer[]> stemming(HashMap<String, Integer[]> tokens) {
        HashMap<String, Integer[]> allStems = new HashMap<>();
        for (String s : tokens.keySet()) {
            snowballStemmer.setCurrent(s);
            snowballStemmer.stem();
            String newStem = snowballStemmer.getCurrent();
            //only for words with upper case - the stemmer returns the letter in lowercase, so it will change back to upper cse
            if(!newStem.equals("")&&Character.isUpperCase(newStem.charAt(0))){
                newStem=newStem.toUpperCase();
            }
            if (allStems.containsKey(newStem)) {
                int counter = allStems.get(newStem)[0];
                Integer[] tmp = tokens.get(s);
                Integer[] add = {counter + tmp[0], tmp[1]};
                allStems.put(newStem, add);
                //stems.add(newStem);
            } else {
                Integer[] tmp = tokens.get(s);
                Integer[] add = {tmp[0], tmp[1]};
                allStems.put(newStem, add);
            }
        }
        return allStems;
    }
}

