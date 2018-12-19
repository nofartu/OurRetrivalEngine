package sample;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader;
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
import static sample.ReadFile.mySplit;

public class Searcher {
    private Parse parse;
    private HashMap<String, ArrayList<String>> wordAndLocations;
    private HashMap<String,ArrayList<String[]>> docsContainsQuery;
    private HashMap<String,Integer> countWordsQuery;
    private String postPath;
    private boolean stem;
    private int numOfDocs;

    public Searcher(HashSet<String> stopwords, String stopwordsPath, String postPath, boolean stem, ApiJson apiJson,int numOfDoc) {
        this.stem = stem;
        HashSet<String> stopWords = stopwords;
        if (stopwords.size() <= 0) {
            stopWords = createHashStopWords(stopwordsPath);
        }
        parse = new Parse(stopWords, stem, apiJson);
        this.postPath = postPath;
        this.numOfDocs=numOfDoc;
        wordAndLocations=new HashMap<>();
        docsContainsQuery=new HashMap<>();
        countWordsQuery=new HashMap<>();
    }

    public void parseTheQuery(String query) {
        createCountWordsQuery(query);
        HashMap<String, Integer[]> queryParsed = parse.parsing(query, "");
        Integer[] dictionaryLoc;
        for (Map.Entry<String, Integer[]> entry : queryParsed.entrySet()) {
            String key = entry.getKey();
            if (dictionary.containsKey(key.toLowerCase())) {
                if (!wordAndLocations.containsKey(key)){
                    dictionaryLoc = entry.getValue();
                    wordAndLocations.put(key, getAllPostings(dictionaryLoc[0]));
                }
            } else if (dictionary.containsKey(key.toUpperCase())) {
                if(!wordAndLocations.containsKey(key)){
                    dictionaryLoc = entry.getValue();
                    wordAndLocations.put(key,getAllPostings(dictionaryLoc[0]));
                }
            } else {
                System.out.println("we don't have this word in our dictionary");
            }
        }
    }


    public ArrayList<String> getAllPostings(int lineNumber) {
        String line = "";
        String namePost = "";
        String post="";
        if (stem)
            namePost = "postFileWithoutStem";
        else
            namePost = "postFileWithStem";
        try (Stream<String> lines = Files.lines(Paths.get(postPath + "\\" + namePost + ".txt"))) {
            line = lines.skip(lineNumber).findFirst().get();
            TermPost term=new TermPost(line);
            post=term.getPost();

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!post.equals("")){
            ArrayList<String> arr= mySplit(post," ");
        }
        return null;
    }

    public void createCountWordsQuery(String query) {
        ArrayList<String> tmp=mySplit(query," ");
        for(int i=0;i<tmp.size();i++){
            String key=tmp.get(i);
            if(!countWordsQuery.containsKey(key)){
                countWordsQuery.put(key,1);
            }else {
                countWordsQuery.put(key,countWordsQuery.get(key)+1);
            }
        }
    }

//    public HashMap<String, ArrayList<String>> createit(){
//        HashMap<String, ArrayList<String>> arr=new HashMap<>();
//        ArrayList<String> tmp=mySplit("LA100590-0148:1:207/1362 LA041689-0056:2:334/659 LA042190-0155:1:981/1037 LA050690-0199:1:964/1066 FT921-8666:1:271/471 bla"," ");
//        arr.put("valances",tmp);
//        tmp=mySplit("LA100590-0148:1:207/1362 LA041689-0056:2:334/659 LA042190-0155:1:981/1037 LA050690-0199:1:964/1066 FT921-8666:1:271/471 bla"," ");
//        arr.put("valances1",tmp);
//        tmp=mySplit("LA100590-0148:1:207/1362 LA041689-0056:2:334/659 LA042190-0155:1:981/1037 LA050690-0199:1:964/1066 FT921-8666:1:271/471 bla"," ");
//        arr.put("valances2",tmp);
//        tmp=mySplit("LA100590-0148:1:207/1362 LA041689-0056:2:334/659 LA042190-0155:1:981/1037 LA050690-0199:1:964/1066 FT921-8666:1:271/471 bla"," ");
//        arr.put("valances3",tmp);
//        return arr;
//    }


    public void createDocsContainsQuery() {
        for (Map.Entry<String, ArrayList<String>> entry : wordAndLocations.entrySet()){
            ArrayList<String> tmp=entry.getValue();
            String keyWord=entry.getKey();
            for (int i=0;i<tmp.size()-1;i++){
                String dfToAdd=tmp.get(tmp.size()-1);
                String df=mySplit(dfToAdd,";").get(1);
                ArrayList<String> docAndTf=mySplit(tmp.get(i),":");
                String key=docAndTf.get(0);
                ArrayList<String> tmp1=mySplit(docAndTf.get(2),"/");
                if(docsContainsQuery.containsKey(key)){
                    String[] strings={keyWord,docAndTf.get(1)+"",tmp1.get(1)+"",df};
                    docsContainsQuery.get(key).add(strings);
                }else {
                    docsContainsQuery.put(key,new ArrayList<>());
                    String[] strings={keyWord,docAndTf.get(1)+"",tmp1.get(1)+"",df};
                    docsContainsQuery.get(key).add(strings);
                }
            }
        }
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


    // TODO: 19/12/2018 add it to the view controller 
    public String getTheQuery(String path) {
        File file = new File(path);
        if (file.isFile()) {
            try {
                Document doc = Jsoup.parse(file, "UTF-8");
                Elements docs = doc.select("top");
                for (Element e : docs) {
                    String query = e.getElementsByTag("title").text();
                    return query;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }

}
