package sample;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static sample.ApiJson.getCities;
import static sample.Indexer.dictionary;
import static sample.Indexer.docsCoprus;
import static sample.ReadFile.mySplit;
import static sample.ViewController.chosenCities;

public class Searcher {
    private Parse parse;
    private HashMap<String, ArrayList<String>> wordAndLocationsQuery;
    private HashMap<String, ArrayList<String>> wordAndLocationsSemantic;
    private HashMap<String, ArrayList<String>> wordAndLocationsDesc;
    private HashMap<String, ArrayList<String[]>> docsContainsQuery;
    private HashMap<String, ArrayList<String[]>> docsContainsSemantic;
    private HashMap<String, ArrayList<String[]>> docsContainsDesc;
    private HashMap<String, Integer> countWordsQuery;
    private HashMap<String, Integer> countWordsSemantic;
    private HashMap<String, Integer> countWordsDesc;
    private HashSet<String> combinedFilesWithCity;
    private TreeMap<String, Double> rankedFiles;
    private String postPath;
    private boolean stem;
    private boolean semantic;
    private int numOfDocs;

    public Searcher(HashSet<String> stopwords, String stopwordsPath, String postPath, boolean stem, ApiJson apiJson, boolean semantic) {
        this.stem = stem;
        this.semantic = semantic;
        HashSet<String> stopWords = stopwords;
        if (stopwords == null) {
            stopWords = createHashStopWords(stopwordsPath);
        }
        parse = new Parse(stopWords, stem, apiJson);
        this.postPath = postPath;
        this.numOfDocs = docsCoprus.size();
        // this.numOfDocs=472525;
        wordAndLocationsQuery = new HashMap<>();
        docsContainsQuery = new HashMap<>();
        countWordsQuery = new HashMap<>();
        combinedFilesWithCity = new HashSet<>();

    }

    private void parseTheQuery(String query) {
        // createCountWordsQuery(query);
        HashMap<String, Integer[]> queryParsed = parse.parsing(query, "");
        if (semantic) {
            wordAndLocationsSemantic = doSemantic(queryParsed);
        }
        wordAndLocationsQuery = runQuery(queryParsed, 1);
//        for (Map.Entry<String, Integer[]> entry : queryParsed.entrySet()) {
//            String key = entry.getKey();
//            String keyLower = key.toLowerCase();
//            String keyUpper = key.toUpperCase();
//            if (dictionary.containsKey(keyLower)) {
//                if (!wordAndLocationsQuery.containsKey(key)) {
//                    Integer[] fromDictionary = dictionary.get(keyLower);
//                    wordAndLocationsQuery.put(key, getAllPostings(fromDictionary[0]));
//                }
//            } else if (dictionary.containsKey(keyUpper)) {
//                if (!wordAndLocationsQuery.containsKey(key)) {
//                    Integer[] fromDictionary = dictionary.get(keyUpper);
//                    wordAndLocationsQuery.put(key, getAllPostings(fromDictionary[0]));
//                }
//            } else {
//                System.out.println("we don't have this word in our dictionary");
//            }
//        }
    }

    private void parseTheDesc(String query){
        HashMap<String, Integer[]> queryParsed = parse.parsing(query, "");
        wordAndLocationsDesc=runQuery(queryParsed, 2);
    }

    public void doQuery(String query) {
        parseTheQuery(query);
        //parseTheDesc(query); //NEW!!!!!
        // createCountWordsQuery(query);
        docsContainsQuery=createDocsContainsQuery(wordAndLocationsQuery);
        docsContainsDesc=createDocsContainsQuery(wordAndLocationsDesc);
        if(semantic)
            docsContainsSemantic=createDocsContainsQuery(wordAndLocationsSemantic);

        sendToRanker();
    }

    private HashMap<String, ArrayList<String>> runQuery(HashMap<String, Integer[]> queryParsed, int who) { //who: 1 - query, 2 - description, 3 - semantic
        HashMap<String, ArrayList<String>> wordAndLocationsTmp = new HashMap<>();
        for (Map.Entry<String, Integer[]> entry : queryParsed.entrySet()) {
            String key = entry.getKey();
            String keyLower = key.toLowerCase();
            String keyUpper = key.toUpperCase();
            if (dictionary.containsKey(keyLower)) {
                if (!wordAndLocationsTmp.containsKey(key)) {
                    Integer[] fromDictionary = dictionary.get(keyLower);
                    wordAndLocationsTmp.put(keyLower, getAllPostings(fromDictionary[0]));
                    insertToCountWords(who, keyLower, fromDictionary[0]);
                    //countWordsQuery.put(keyLower, fromDictionary[0]);
                }
            } else if (dictionary.containsKey(keyUpper)) {
                if (!wordAndLocationsTmp.containsKey(key)) {
                    Integer[] fromDictionary = dictionary.get(keyUpper);
                    wordAndLocationsTmp.put(keyUpper, getAllPostings(fromDictionary[0]));
                    insertToCountWords(who, keyUpper, fromDictionary[0]);
                    //countWordsQuery.put(keyUpper, fromDictionary[0]);
                }
            } else {
                System.out.println("2 we don't have this word in our dictionary " + key);
            }
        }
        return wordAndLocationsTmp;
    }

    private void insertToCountWords(int who, String key, int value) {
        if (who == 1)
            countWordsQuery.put(key, value);
        else if (who == 2)
            countWordsDesc.put(key, value);
        else if (who == 3)
            countWordsSemantic.put(key, value);
    }

    private HashMap<String, ArrayList<String>> doSemantic(HashMap<String, Integer[]> queryParsed) {
        HashMap<String, Integer[]> separateQuery = new HashMap<>();
        parse.resetParse();
        StringBuilder query = new StringBuilder("");
        for (Map.Entry<String, Integer[]> entry : queryParsed.entrySet()) {
            String key = entry.getKey();
            ArrayList<String> fromSemantic = semantic(key);
            if (fromSemantic != null) {
                for (String s : fromSemantic) {
                    separateQuery.put(s, null);
                    query.append(s + " ");
                }
            }
        }
        HashMap<String, Integer[]> query2Parsed = parse.parsing(query.toString(), "");
        return runQuery(query2Parsed, 3);
    }

    public ArrayList<String> getAllPostings(int lineNumber) {
        String line = "";
        String namePost = "";
        String post = "";
        if (stem)
            namePost = "Stem\\postFileWithStem";
        else
            namePost = "WithoutStem\\postFileWithoutStem";
        try (Stream<String> lines = Files.lines(Paths.get(postPath + "\\" + namePost + ".txt"))) {
            line = lines.skip(lineNumber).findFirst().get();
            TermPost term = new TermPost(line);
            post = term.getPost();

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!post.equals("")) {
            ArrayList<String> arr = mySplit(post, " ");
            return arr;

        }
        return null;
    }

//    // TODO: 12/22/2018 we have a word that is not in the dictionary what to do"?
//    public void createCountWordsQuery(String query) {
//        query=OurReplace(query,"-"," ");
//        ArrayList<String> tmp = mySplit(query, " ");
//        for (int i = 0; i < tmp.size(); i++) {
//            String key = tmp.get(i);
//            String keyUp=key.toUpperCase();
//            String keyLower=key.toLowerCase();
//            if (dictionary.containsKey(keyLower)) {
//                if (!countWordsQuery.containsKey(keyLower)) {
//                    countWordsQuery.put(keyLower, 1);
//                } else {
//                    countWordsQuery.put(keyLower, countWordsQuery.get(keyLower) + 1);
//                }
//            }
//            else if(dictionary.containsKey(keyUp)){
//                if (!countWordsQuery.containsKey(keyUp)) {
//                    countWordsQuery.put(keyUp, 1);
//                } else {
//                    countWordsQuery.put(keyUp, countWordsQuery.get(keyUp) + 1);
//                }
//            }
//            else
//                System.out.println("1 we have a word in the query that is not in the dictionary "+key );
//
//        }
//    }

    public void sendToRanker() {
        Ranker ranker = new Ranker(numOfDocs);
        rankedFiles = ranker.rankAll(docsContainsQuery, countWordsQuery, wordAndLocationsQuery); //change
        withCities();
    }

    public HashMap<String, ArrayList<String>> getAllCityPostings(String word) {
        HashMap<String, City> c = getCities();
        return c.get(word).getLocations();
    }

    public void withCities() {
        HashMap<String, ArrayList<String>> files;
        HashMap<String, Double> tmp = new HashMap<>();
        tmp.putAll(rankedFiles);
        // int size = chosenCities.size();
        for (String city : chosenCities) {
            //for (String city : cities.keySet()) {
            files = getAllCityPostings(city);
            for (Map.Entry<String, ArrayList<String>> entry : files.entrySet()) {
                if (tmp.containsKey(entry.getKey())) {
                    combinedFilesWithCity.add(entry.getKey());
                }
            }
        }
        for (String entry : combinedFilesWithCity) {
            System.out.println(entry);
        }

    }


    public HashMap<String, ArrayList<String[]>> createDocsContainsQuery(HashMap<String, ArrayList<String>> wordAndLocations) {
        HashMap<String, ArrayList<String[]>> docsContainsTmp=new HashMap<>();
        for (Map.Entry<String, ArrayList<String>> entry : wordAndLocations.entrySet()) {
            ArrayList<String> tmp = entry.getValue();
            String keyWord = entry.getKey();
            for (int i = 0; i < tmp.size() - 1; i++) {
                String dfToAdd = tmp.get(tmp.size() - 1);
                String df = mySplit(dfToAdd, ":").get(1);
                ArrayList<String> docAndTf = mySplit(tmp.get(i), ":");
                String key = docAndTf.get(0);
                ArrayList<String> tmp1 = mySplit(docAndTf.get(2), "/");
                if (docsContainsTmp.containsKey(key)) {
                    String[] strings = {keyWord, docAndTf.get(1), tmp1.get(1), df, tmp1.get(0)};
                    docsContainsTmp.get(key).add(strings);
                } else {
                    docsContainsTmp.put(key, new ArrayList<>());
                    String[] strings = {keyWord, docAndTf.get(1), tmp1.get(1), df, tmp1.get(0)};
                    docsContainsTmp.get(key).add(strings);
                }
            }
        }
        return docsContainsTmp;
    }


    private ArrayList<String> semantic(String word) {
        ArrayList<String> semanticWords = new ArrayList<>();
        try {
            String urlLink = "https://api.datamuse.com/words?ml=" + word;
            OkHttpClient client = new OkHttpClient();
            HttpUrl.Builder urlBuilder = HttpUrl.parse(urlLink).newBuilder();
            String url = urlBuilder.build().toString();
            Request request = new Request.Builder().url(url).build();
            Call call = client.newCall(request);
            Response response = call.execute();
            JsonParser parser = new JsonParser();
            JsonElement ele = parser.parse(response.body().string());
            JsonArray arr = ele.getAsJsonArray();
            for (int i = 0; i < 12; i++) {
                JsonObject o = (JsonObject) (arr.get(i));
                String str = o.get("word").getAsString();
                semanticWords.add(str);
            }
            return semanticWords;
        } catch (Exception e) {

        }
        return null;
    }

    //create an Hash with all the stop words
    private HashSet<String> createHashStopWords(String path) {
        HashSet<String> stopwords = new HashSet<>();
        String line;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(path + "\\stop_words.txt"), StandardCharsets.UTF_8));
            while ((line = br.readLine()) != null) {
                stopwords.add(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stopwords;
    }

    public TreeMap<String, Double> getDocs() {
        return rankedFiles;
    }


}
