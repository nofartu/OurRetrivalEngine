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
    private TreeMap<String, Double> combinedFilesWithCity;
    private TreeMap<String, Double> rankedFiles;
    private String postPath;
    private String description;
    private boolean stem;
    private boolean semantic;
    private boolean isCity;
    private int numOfDocs;

    /*
    constructor
     */
    public Searcher(String stopwordsPath, String postPath, boolean stem, ApiJson apiJson, boolean semantic, String description, boolean isCity) {
        this.stem = stem;
        this.semantic = semantic;
        HashSet<String> stopWords = createHashStopWords(stopwordsPath);
       // if (stopwords == null) {
       //     stopWords = createHashStopWords(stopwordsPath);
       // }
        parse = new Parse(stopWords, stem, apiJson);
        this.postPath = postPath;
        this.numOfDocs = docsCoprus.size();
        wordAndLocationsQuery = new HashMap<>();
        wordAndLocationsSemantic = new HashMap<>();
        wordAndLocationsDesc = new HashMap<>();
        docsContainsQuery = new HashMap<>();
        docsContainsSemantic = new HashMap<>();
        docsContainsDesc = new HashMap<>();
        countWordsQuery = new HashMap<>();
        countWordsSemantic = new HashMap<>();
        countWordsDesc = new HashMap<>();
        rankedFiles = new TreeMap<>();
        combinedFilesWithCity = new TreeMap<>();
        this.description = description;
        this.isCity = isCity;
    }

    /*
    parsing the query in our parser to be consistent with the postings and dictionaey
     */
    private void parseTheQuery(String query) {

        HashMap<String, Integer[]> queryParsed = parse.parsing(query, "");
        if (semantic) {
            wordAndLocationsSemantic = doSemantic(queryParsed);
        }
        wordAndLocationsQuery = runQuery(queryParsed, 1);
    }
    /*
       parsing the description in our parser to be consistent with the postings and dictionaey
        */
    private void parseTheDesc(String desc) {
        HashMap<String, Integer[]> queryParsed = parse.parsing(desc, "");
        wordAndLocationsDesc = runQuery(queryParsed, 2);
    }

    /*
    the main function that operates all the class
    it is parsing all the information then sends to create the data structures that is needed to rank
     */
    public void doQuery(String query) {
        parseTheQuery(query);
        docsContainsQuery = createDocsContainsQuery(wordAndLocationsQuery);
        parse.resetParse(); //reset so it wouldn't add it to the same data structure
        parseTheDesc(description);
        docsContainsDesc = createDocsContainsQuery(wordAndLocationsDesc);
        parse.resetParse();
        if (semantic)
            docsContainsSemantic = createDocsContainsQuery(wordAndLocationsSemantic);
        sendToRanker();  //sends to the function that do the proper sending to the ranker
    }

    /*
       create a data structure for each word in the dictionary it gets the postings
     */
    private HashMap<String, ArrayList<String>> runQuery(HashMap<String, Integer[]> queryParsed, int who) { //who: 1 - query, 2 - description, 3 - semantic
        HashMap<String, ArrayList<String>> wordAndLocationsTmp = new HashMap<>();
        TreeMap<Integer, String> invertedLoc = new TreeMap<>();
        for (Map.Entry<String, Integer[]> entry : queryParsed.entrySet()) {
            String key = entry.getKey();
            String keyLower = key.toLowerCase();
            String keyUpper = key.toUpperCase();
            if (dictionary.containsKey(keyLower)) {
                Integer[] fromDictionary = dictionary.get(keyLower);
                invertedLoc.put(fromDictionary[0], keyLower);
                insertToCountWords(who, keyLower, fromDictionary[0]);
            } else if (dictionary.containsKey(keyUpper)) {
                Integer[] fromDictionary = dictionary.get(keyUpper);
                invertedLoc.put(fromDictionary[0], keyUpper);
                insertToCountWords(who, keyUpper, fromDictionary[0]);
            } else {
            }
        }
        wordAndLocationsTmp = getAllPostings(wordAndLocationsTmp, invertedLoc);
        return wordAndLocationsTmp;
    }

    /*
    inserts into the data structure the count of the ords in the query, description, semantic depanding on the number sent in who
     */
    private void insertToCountWords(int who, String key, int value) {
        if (who == 1)    // of the query
            countWordsQuery.put(key, value);
        else if (who == 2)      // of the description
            countWordsDesc.put(key, value);
        else if (who == 3)  // of the semantic
            countWordsSemantic.put(key, value);
    }

    /*
    recives the semantic words related to the words in the query and parsing them and send it to the data structure of words from semantic with all the information related
     */
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


    /*
    create and returns a data structure with all the postings for each word
     */
    public HashMap<String, ArrayList<String>> getAllPostings(HashMap<String, ArrayList<String>> wordAndLocationsTmp, TreeMap<Integer, String> lineNumbers) {
        String line = "";
        String namePost = "";
        String post = "";
        if (stem)
            namePost = "Stem\\postFileWithStem";
        else
            namePost = "WithoutStem\\postFileWithoutStem";
        namePost = postPath + "\\" + namePost + ".txt";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(namePost), "UTF-8"));
            int count = 0;
            int next = 0;
            if (!lineNumbers.isEmpty())
                next = lineNumbers.firstKey();
            String name = lineNumbers.get(next);
            while (!lineNumbers.isEmpty()) {
                line = reader.readLine();
                while (count != next) {
                    line = reader.readLine();
                    count++;
                }
                if (next == count) {
                    TermPost term = new TermPost(line);
                    post = term.getPost();
                    if (!post.equals("")) {
                        ArrayList<String> arr = mySplit(post, " ");
                        wordAndLocationsTmp.put(name, arr);
                    } else {
                        wordAndLocationsTmp.put(name, null);
                    }
                }
                lineNumbers.pollFirstEntry();
                count++;
                if (!lineNumbers.isEmpty()) {
                    next = lineNumbers.firstKey();
                    name = lineNumbers.get(next);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return wordAndLocationsTmp;
    }

    /*
    sends all the information to the ranker to get the top 50 ranked documents
     */
    public void sendToRanker() {
        Ranker ranker = new Ranker(numOfDocs, isCity);
        rankedFiles = ranker.rankAll(docsContainsQuery, countWordsQuery, wordAndLocationsQuery, docsContainsDesc, countWordsDesc, wordAndLocationsDesc, docsContainsSemantic, countWordsSemantic, wordAndLocationsSemantic, semantic);
    }


    /*
    create a data structure that the key is document and each line in the arraylist contains a word in the query and the document
     */
    public HashMap<String, ArrayList<String[]>> createDocsContainsQuery(HashMap<String, ArrayList<String>> wordAndLocations) {
        HashMap<String, ArrayList<String[]>> docsContainsTmp = new HashMap<>();
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

    /*
    reades and parse the words from the API of the semantic words
     */
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
            for (int i = 0; i < 5; i++) {
                JsonObject o = (JsonObject) (arr.get(i));
                String str = o.get("word").getAsString();
                semanticWords.add(str);
            }
            return semanticWords;
        } catch (Exception e) {

        }
        return null;
    }

    /*
    create an Hash with all the stop words
     */
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
