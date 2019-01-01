package sample;

import javafx.util.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


public class ReadFile {

    private ArrayList<Documents> dirDocuments;
    private Indexer indexer;
    private HashSet<String> stopwords;
    private HashMap<String, Integer[]> fromParse;
    private ApiJson apiJson;
    private String corpusPath;
    private boolean isStem;
    private static HashSet<String> languages;
    private String pathPost;
    private Parse parse;


    public ReadFile(String corpusPath, String stopWordsPath, String postingPath, boolean stem) throws IOException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        System.out.println("Start time:" + dateFormat.format(date));
        apiJson = new ApiJson();
        dirDocuments = new ArrayList<>();
        stopwords = new HashSet<>();
        createHashStopWords(stopWordsPath);
        indexer = new Indexer(stem, postingPath);
        this.corpusPath = corpusPath;
        isStem = stem;
        pathPost = postingPath;
        languages = new HashSet<>();
        parse = new Parse(stopwords, isStem, apiJson);
    }


    public String reading() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        System.out.println("Start time:" + dateFormat.format(date));
        int count = 0;
        String city = "";
        int size = 0;
        boolean isThere = false;
        File file = new File(corpusPath);
        for (File file2 : file.listFiles()) {
            size = file.listFiles().length;
            if (new File(corpusPath + "\\stop_words.txt").exists())
                isThere = true;
            if (isThere)
                size--;
            if (file2.isDirectory()) {
                for (File file3 : file2.listFiles()) {
                    if (file3.isFile()) {
                        try {
                            //Document doc = Jsoup.parse(new String(Files.readAllBytes(file3.toPath())), "", Parser.xmlParser());
                            Document doc = Jsoup.parse(file3, "UTF-8");
                            Elements docs = doc.select("DOC");
                            //Elements docs = doc.getElementsByTag("DOC");
                            for (Element e : docs) {
                                String toPars = e.getElementsByTag("TEXT").text();
                                String docName = e.getElementsByTag("DOCNO").text();
                                try {
                                    String s = e.getElementsByTag("F").toString();
                                    city = findAndAddCity(docName, s, apiJson);
                                    addLanguage(s);
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                                fromParse = parse.parsing(toPars, docName);
                                Documents d = new Documents(docName, fromParse);
                                HashMap<String, Double> entity = handleEntities();
                                d.setEntities(entity);
                                if (!city.equals("")) {
                                    d.setOrigin(city);
                                }
                                dirDocuments.add(d);
                                parse.resetParse();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    count++;
                }
                indexer.addDocument(dirDocuments);
                dirDocuments = new ArrayList<>();  //should delete all the dir array
                if (count % 33 == 0 || count == size) {
                    indexer.writeToDisk();
                }
            }
        }
        indexer.mergePost();
        apiJson.writeCityToDisk(isStem, pathPost);
        indexer.writeDocsToDisk();
        writeLanguagesToDisk();

        return indexer.information();

    }

    private void addLanguage(String text) {
        String ans = "";
        int i = 0;
        int location = text.indexOf("<f p=\"105\"");
        if (location != -1 && text.charAt(location + 11) != '<') {
            location = location + 11;
            if (text.charAt(location) == '\n') {
                //don't do anything
                location++;
            }
            while (text.charAt(location) == ' ') {
                location++;
            }
            while (text.charAt(location) != '<') {
                if (text.charAt(location) == ' ' || Character.isDigit(text.charAt(location)) || text.charAt(location) == ',' || text.charAt(location) == ';' || text.charAt(location) == '-') {
                    break;
                } else {
                    ans = ans + text.charAt(location);
                }
                location++;
            }
        }
        if (!ans.equals("")) {
            languages.add(ans);
        }
    }

    public static HashSet<String> getLanguages() {
        return languages;
    }

    private void writeLanguagesToDisk() {
        try {
            String namedir = "";
            if (isStem) {
                namedir = "Stem";
            } else {
                namedir = "WithoutStem";
            }
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathPost + "\\" + namedir + "\\Languages.txt", false), StandardCharsets.UTF_8));
            int count = 0;
            StringBuilder writeIt = new StringBuilder("");
            for (String lan : languages) {
                writeIt.append(lan + "\n");
                if (count % 50 == 0) {
                    bw.write(writeIt.toString());
                    bw.flush();
                    writeIt = new StringBuilder("");
                }
                count++;
            }
            bw.write(writeIt.toString());
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadLanguages(boolean isStem, String pathPost) {
        languages = new HashSet<>();
        try {
            BufferedReader br = null;
            if (isStem)
                br = new BufferedReader(new InputStreamReader(new FileInputStream(pathPost + "\\Stem\\Languages.txt"), StandardCharsets.UTF_8));
            else
                br = new BufferedReader(new InputStreamReader(new FileInputStream(pathPost + "\\WithoutStem\\Languages.txt"), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                languages.add(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String findAndAddCity(String docName, String text, ApiJson apiJson) throws Exception {

        String ans = "";
        int i = 0;
        int location = text.indexOf("<f p=\"104\"");
        if (location != -1 && (text.charAt(location + 11) != '<' && text.charAt(location + 11) != '-' && text.charAt(location + 11) != '\'' && text.charAt(location + 11) != '[' && text.charAt(location + 11) != '(' && !Character.isDigit(text.charAt(location + 11)))) {
            location = location + 12;
            int locationInDoc = location;
            //eliminate spaces
            while (text.charAt(location) == ' ') {
                location++;
            }
            while (text.charAt(location) != '<') {
                if (text.charAt(location) == '\n') {
                    //don't do anything
                }
                if (text.charAt(location) == ' ') {
                    if (i != 0)
                        break;
                    ans = ans + text.charAt(location);
                    i++;
                } else {
                    ans = ans + text.charAt(location);
                }
                location++;
            }

            ArrayList<String> cityName = mySplit(ans, " ");
            String tmp = cityName.get(0).toUpperCase(); //only first word
            String ans1 = ans.toUpperCase();
            //ans is the two words

            return apiJson.addToCities(tmp, ans1, docName, locationInDoc, cityName.get(0), "b");

        }
        return "";
    }

    public static ArrayList<String> mySplit(String str, String regex) {
        ArrayList<String> result = new ArrayList<>();
        int start = 0;
        int pos = str.indexOf(regex);
        while (pos >= start) {
            if (pos > start) {
                result.add(str.substring(start, pos));
            }
            start = pos + regex.length();
            pos = str.indexOf(regex, start);
        }
        if (start < str.length()) {
            result.add(str.substring(start));
        }
        return result;
    }

    //create an Hash with all the stop words
    private void createHashStopWords(String path) {
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
    }

    private HashMap<String, Double> handleEntities() {
        HashMap<String, Double> entities = new HashMap<>();
        int size = fromParse.get("***+++***+++***")[0];
        for (Map.Entry<String, Integer[]> entry : fromParse.entrySet()) {
            if (!entry.getKey().equals("") && Character.isUpperCase(entry.getKey().charAt(0)) && !entry.getKey().contains("-")) {
                if (entities.size() < 5) {
                    entities.put(entry.getKey(), calcDominance(size, entry.getValue()[1], entry.getValue()[0]));
                } else {
                    for (Map.Entry<String, Double> entryEnt : entities.entrySet()) {
                        double dom = calcDominance(size, entry.getValue()[1], entry.getValue()[0]);
                        if (entryEnt.getValue() < dom) {
                            entities.remove(entryEnt.getKey());
                            entities.put(entry.getKey(), dom);
                            break;
                        }
                    }
                }
            }
        }
        return entities;
    }

    private double calcDominance(int size, int location, int appear) {
        double relativeLocation = (double) (location / size);
        double rate = 0.6 * appear + 0.4 * relativeLocation;
        return rate;
    }
}
