package sample;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Stream;

import static sample.Indexer.dictionary;
import static sample.Indexer.docsCoprus;
import static sample.ReadFile.mySplit;

public class Searcher {
    private Parse parse;
    private HashMap<String, ArrayList<String>> wordAndLocations;
    private HashMap<String, ArrayList<String>> wordAndLocationsSemantic;
    private HashMap<String, ArrayList<String[]>> docsContainsQuery;
    private HashMap<String, Integer> countWordsQuery;
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
        wordAndLocations = new HashMap<>();
        docsContainsQuery = new HashMap<>();
        countWordsQuery = new HashMap<>();
    }

    public void parseTheQuery(String query) {
        createCountWordsQuery(query);
        HashMap<String, Integer[]> queryParsed = parse.parsing(query, "");
        if (semantic) {
            wordAndLocationsSemantic = doSemantic(queryParsed);
        }
        wordAndLocations = runQuery(queryParsed);
//        for (Map.Entry<String, Integer[]> entry : queryParsed.entrySet()) {
//            String key = entry.getKey();
//            String keyLower = key.toLowerCase();
//            String keyUpper = key.toUpperCase();
//            if (dictionary.containsKey(keyLower)) {
//                if (!wordAndLocations.containsKey(key)) {
//                    Integer[] fromDictionary = dictionary.get(keyLower);
//                    wordAndLocations.put(key, getAllPostings(fromDictionary[0]));
//                }
//            } else if (dictionary.containsKey(keyUpper)) {
//                if (!wordAndLocations.containsKey(key)) {
//                    Integer[] fromDictionary = dictionary.get(keyUpper);
//                    wordAndLocations.put(key, getAllPostings(fromDictionary[0]));
//                }
//            } else {
//                System.out.println("we don't have this word in our dictionary");
//            }
//        }

    }


    private HashMap<String, ArrayList<String>> runQuery(HashMap<String, Integer[]> queryParsed) {
        HashMap<String, ArrayList<String>> wordAndLocationsTmp = new HashMap<>();
        for (Map.Entry<String, Integer[]> entry : queryParsed.entrySet()) {
            String key = entry.getKey();
            String keyLower = key.toLowerCase();
            String keyUpper = key.toUpperCase();
            if (dictionary.containsKey(keyLower)) {
                if (!wordAndLocationsTmp.containsKey(key)) {
                    Integer[] fromDictionary = dictionary.get(keyLower);
                    wordAndLocationsTmp.put(key, getAllPostings(fromDictionary[0]));
                }
            } else if (dictionary.containsKey(keyUpper)) {
                if (!wordAndLocationsTmp.containsKey(key)) {
                    Integer[] fromDictionary = dictionary.get(keyUpper);
                    wordAndLocationsTmp.put(key, getAllPostings(fromDictionary[0]));
                }
            } else {
                System.out.println("we don't have this word in our dictionary");
            }
        }
        return wordAndLocationsTmp;
    }

    private HashMap<String, ArrayList<String>> doSemantic(HashMap<String, Integer[]> queryParsed) {
        HashMap<String, Integer[]> separateQuery = new HashMap<>();
        for (Map.Entry<String, Integer[]> entry : queryParsed.entrySet()) {
            String key = entry.getKey();
            ArrayList<String> fromSemantic = semantic(key);
            if (fromSemantic != null) {
                for (String s : fromSemantic) {
                    separateQuery.put(s, null);
                }
            }
        }
        return runQuery(separateQuery);
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

    public void createCountWordsQuery(String query) {
        ArrayList<String> tmp = mySplit(query, " ");
        for (int i = 0; i < tmp.size(); i++) {
            String key = tmp.get(i);
            if (!countWordsQuery.containsKey(key)) {
                countWordsQuery.put(key, 1);
            } else {
                countWordsQuery.put(key, countWordsQuery.get(key) + 1);
            }
        }
    }

    public void sendToRanker() {
        Ranker ranker = new Ranker();
        //ranker.rankBM25(docsContainsQuery, countWordsQuery, numOfDocs);
    }


    public void createDocsContainsQuery() {
        for (Map.Entry<String, ArrayList<String>> entry : wordAndLocations.entrySet()) {
            ArrayList<String> tmp = entry.getValue();
            String keyWord = entry.getKey();
            for (int i = 0; i < tmp.size() - 1; i++) {
                String dfToAdd = tmp.get(tmp.size() - 1);
                String df = mySplit(dfToAdd, ":").get(1);
                ArrayList<String> docAndTf = mySplit(tmp.get(i), ":");
                String key = docAndTf.get(0);
                ArrayList<String> tmp1 = mySplit(docAndTf.get(2), "/");
                if (docsContainsQuery.containsKey(key)) {
                    String[] strings = {keyWord, docAndTf.get(1), tmp1.get(1), df, tmp1.get(0)};
                    docsContainsQuery.get(key).add(strings);
                } else {
                    docsContainsQuery.put(key, new ArrayList<>());
                    String[] strings = {keyWord, docAndTf.get(1), tmp1.get(1), df, tmp1.get(0)};
                    docsContainsQuery.get(key).add(strings);
                }
            }
        }

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

}
