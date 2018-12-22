package sample;


import javafx.util.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static sample.ReadFile.mySplit;


public class Indexer {
    private ArrayList<Documents> listDocs;
    private int max_tf = 0;
    //the list is the name of the files where int the posting is located (line number)
    public static HashMap<String, Integer[]> dictionary; //{line number, num of occurrence}
    private int postingNumber;
    private TreeMap<String, List<String>> posting;
    private boolean stemming;
    private String pathPost;
    private File directory;
    public static HashMap<String, Documents> docsCoprus;
    private HashMap<TermPost, List<Integer>> hashTermToPost;
    private PriorityQueue<TermPost> terms;
    private int numOfDoc;
    private int uniques;


    public Indexer(boolean isStem, String pathPost) {
        listDocs = new ArrayList<>();
        dictionary = new HashMap<>();
        postingNumber = 0;
        posting = new TreeMap<>(new Indexer.TreeCompare());
        stemming = isStem;
        this.pathPost = pathPost;
        docsCoprus = new HashMap<>();
        uniques = 0;
    }

    /**
     * Adding to lhe list of documents in the class and sends to the temp posting.
     *
     * @param listDocs - the list that contain the docs that we are currently work on
     */
    public void addDocument(ArrayList<Documents> listDocs) {
        this.listDocs = listDocs;
        tmpPosting();
        this.listDocs = new ArrayList<>();    //nofar add
    }

    /**
     * This will do the tmp posting.
     * It will send to writeToDisc.
     */
    private void tmpPosting() {
        int countDocNumber = 0;
        for (Iterator docs = listDocs.iterator(); docs.hasNext(); ) { //go through list of docs
            Documents doc = (Documents) docs.next();
            int max = 0;
            int min = 0;
            countDocNumber++;
            Integer[] getSize = doc.getTerms().remove("***+++***+++***");
            int size = getSize[0];
            for (Map.Entry<String, Integer[]> entry : doc.getTerms().entrySet()) { //go through the docs terms
                String key = entry.getKey();
                int value = entry.getValue()[0]; //num of occurrence
                int location = entry.getValue()[1]; //first location in text
                String keyUpper = key.toUpperCase();
                String keyLower = key.toLowerCase();
                if (!key.equals("")) {
                    //checking the tf of doc
                    if (value > max)
                        max = value;
                    if (value == 1)
                        min++;
                    // addToCities2( key, doc.getIdDoc(), location, "L"); //adding to cities
                    if (!dictionary.containsKey(key)) {
                        //checking for lower/upper case problem
                        //exist in lower case - but key is in upper
                        if (dictionary.containsKey(keyLower)) {
                            key = keyLower;
                            Integer newVal = value + dictionary.get(key)[1];
                            Integer[] arr = {0, newVal};
                            dictionary.put(key, arr);
                        }
                        //exist in upper case - but key is in lower.
                        //change the word to lower case because it came in lower so it should be in the dictionary in lower.
                        else if (dictionary.containsKey(keyUpper)) {
                            Integer[] tmp = dictionary.remove(keyUpper);
                            Integer newVal = value + tmp[1];
                            Integer[] arr = {0, newVal};
                            //dictionary.remove(keyUpper);
                            dictionary.put(keyLower, arr);
                            key = keyLower;
                        }
                        //does'nt exist
                        else {
                            //checking situation where in one word there is a upper and lower case
                            //like oPen
                            if (!key.equals(keyLower) && !key.equals(keyUpper) && !Character.isDigit(key.charAt(0))) {
                                int count = 0;
                                for (int i = 0; i < key.length(); i++) {
                                    if (Character.isUpperCase(key.charAt(i)))
                                        count++;
                                }
                                int percent = count / key.length();
                                if (percent >= (3 / 4))
                                    key = keyUpper;
                                else
                                    key = keyLower;
                            }
                            Integer[] arr = {0, value};
                            dictionary.put(key, arr);
                        }
                    } else {
                        Integer newVal = value + dictionary.get(key)[1];
                        Integer[] arr = {0, newVal};
                        dictionary.put(key, arr);

                    }
                    if (!posting.containsKey(key)) {
                        //checking for lower/upper case problem
                        //exist in lower case - but key is in upper
                        if (posting.containsKey(keyLower)) {
                            key = keyLower;
                        }
                        //exist in upper case - but key is in lower.
                        //change the word to lower case because it came in lower so it should be in the posting in lower.
                        else if (posting.containsKey(keyUpper)) {
                            List<String> list = posting.get(keyUpper);
                            posting.remove(keyUpper);
                            posting.put(keyLower, list);
                            key = keyLower;
                        }
                        //does'nt exist
                        else {
                            posting.put(key, new LinkedList<>());
                        }
                    }
                    String insert = doc.getIdDoc() + ":" + value + ":" + location + "/" + size; //saves the post in form of docId:tf:location/total doc size
                    posting.get(key).add(insert);
                }
            }
            doc.resetTerms(); //saving some memory
            addToCorpus(doc, max, min, size); //adding to the corpus of docs

        }
        //   writeToDisk();
    }


    //adding to list of documents and for each doc the max tf, origin, num of unique
    private void addToCorpus(Documents doc, int max, int min, int size) {
//        Documents d = new Documents(doc.getIdDoc(), null);
//        d.setMax_tf(max);
//        d.setOrigin(doc.getOrigin());
//        d.setNumOfUniqe(min);
//        d.setSize(size);
//        docsCoprus.add(d);
        doc.setMax_tf(max);
        doc.setOrigin(doc.getOrigin());
        doc.setNumOfUniqe(min);
        doc.setSize(size);
        docsCoprus.put(doc.getIdDoc(), doc);
    }

    /**
     * writes to the disc the tmp posting
     */
    public void writeToDisk() {
        StringBuilder toWrite = new StringBuilder("");
        BufferedWriter bw = null;
        directory = new File(pathPost + "\\PostingDirectory");
        if (!directory.exists()) {
            directory.mkdir();
        }
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathPost + "\\PostingDirectory\\postFile" + postingNumber + ".txt", false), StandardCharsets.UTF_8));
            postingNumber++;
            int count = 0;
            //for (Map.Entry<String, List<String>> entry : posting.entrySet()) { //go through the docs terms
            while (posting.size() > 0) {
                String key = posting.firstKey();
                List<String> value = posting.remove(key);
                count++;
                //String key = entry.getKey();
                String keyUpper = key.toUpperCase();
                String keyLower = key.toLowerCase();
                if (!dictionary.containsKey(key)) {
                    if (dictionary.containsKey(keyLower))
                        key = keyLower;
                    else if (dictionary.containsKey(keyUpper))
                        key = keyUpper;
                }
                toWrite.append(key + "*;~ ");
                for (Iterator post = value.iterator(); post.hasNext(); ) {
                    toWrite.append(post.next() + " ");

                }
                toWrite.append("\n");
                if (count % 100 == 0) {
                    bw.write(toWrite.toString());
                    bw.flush();
                    toWrite = new StringBuilder("");
                }

            }
            //}
            bw.write(toWrite.toString());
            bw.flush();
            bw.close();
        } catch (IOException e) {
            System.out.println("not good writing");
        }
        posting = new TreeMap<>(new Indexer.TreeCompare());
    }

    /**
     * writes doc to the disc
     */
    public void writeDocsToDisk() {
        try {
            String namedir = "";
            if (stemming) {
                namedir = "Stem";
            } else {
                namedir = "WithoutStem";
            }

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathPost + "\\" + namedir + "\\Documents.txt", false), StandardCharsets.UTF_8));
            int count = 0;
            StringBuilder writeIt = new StringBuilder("");
            for (Map.Entry<String, Documents> entry : docsCoprus.entrySet()) {
                Documents doc = entry.getValue();
                int maxTf = doc.getMax_tf();
                int unique = doc.getNumOfUniqe();
                String origin = doc.getOrigin();
                String name = doc.getIdDoc();
                int size = doc.getSize();
                writeIt.append(name + ": " + maxTf + ";" + unique + ";" + size + ";" + origin + ";Entities");
                Set<String> set = doc.getEntities();
                if (set != null) {
                    for (String entity : set) {
                        writeIt.append(";" + entity);
                    }
                    writeIt.append("\n");
                }
                if (count % 10000 == 0) {
                    bw.write(writeIt.toString());
                    bw.flush();
                    writeIt = new StringBuilder("");
                }
            }
//            for (int i = 0; i < docsCoprus.size(); i++) {
//                int maxTf = docsCoprus.get(i).getMax_tf();
//                int unique = docsCoprus.get(i).getNumOfUniqe();
//                String origin = docsCoprus.get(i).getOrigin();
//                String name = docsCoprus.get(i).getIdDoc();
//                int size = docsCoprus.get(i).getSize();
//                writeIt.append(name + ": " + maxTf + ";" + unique + ";" + size + ";" + origin + "\n");
//                if (count % 10000 == 0) {
//                    bw.write(writeIt.toString());
//                    bw.flush();
//                    writeIt = new StringBuilder("");
//                }
//            }
            //old(Down)
//            for (Documents doc : docsCoprus) {//go through list of docs
//                int maxTf = doc.getMax_tf();
//                int unique = doc.getNumOfUniqe();
//                String origin = doc.getOrigin();
//                String name = doc.getIdDoc();
//                writeIt.append(name + ": " + maxTf + ";" + unique + ";" + origin + "\n");
//                if (count % 10000 == 0) {
//                    bw.write(writeIt.toString());
//                    bw.flush();
//                    writeIt = new StringBuilder("");
//                }
//            }

            bw.write(writeIt.toString());
            bw.flush();
            bw.close();
            numOfDoc = docsCoprus.size();
            docsCoprus = new HashMap<>();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The External merge!!!
     */
    public void mergePost() {
        int lineNumber = 0;
        StringBuilder writing = new StringBuilder("");
        terms = new PriorityQueue<>(); //maybe term not string
        BufferedReader[] brs = new BufferedReader[postingNumber];
        hashTermToPost = new HashMap<>(); //each term and its post with list of post file
        BufferedWriter bw = null;
        String name = "";
        String namedir = "";
        if (stemming) {
            name = "postFileWithStem";
            namedir = "Stem";
        } else {
            name = "postFileWithoutStem";
            namedir = "WithoutStem";
        }
        try {
            File dir = new File(pathPost + "\\" + namedir);
            if (!dir.exists())
                dir.mkdir();
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathPost + "\\" + namedir + "\\" + name + ".txt", false), StandardCharsets.UTF_8));
            for (int i = 0; i < postingNumber; i++) {
                //each file reader
                brs[i] = new BufferedReader(new InputStreamReader(new FileInputStream(pathPost + "\\PostingDirectory\\postFile" + i + ".txt"), StandardCharsets.UTF_8));
                int j = 0;
                String line;
                //getting 3 first line of each file
                while (j < 3 && (line = brs[i].readLine()) != null) {
                    TermPost term = new TermPost(line);
                    //                    terms.add(term);
                    addToQueue(term);
                    addingHash(term, i);
                    j++;
                }
            }
            int round = 0;
            while (terms.size() != 0 && hashTermToPost.size() != 0) {
                TermPost termToAdd = terms.poll(); //get my best term that i can add to file
                int numOfTimeInPosts = 0;
                if (!hashTermToPost.containsKey(termToAdd)) {
                    //System.out.println(termToAdd.getName());
                } else {
                    numOfTimeInPosts = hashTermToPost.get(termToAdd).size();
                    if (numOfTimeInPosts == 1) { // accrue once in the queue or we already merged him to one term
                        int index = hashTermToPost.get(termToAdd).get(0);
                        String line = brs[index].readLine();
                        if (line != null) {
                            TermPost term = new TermPost(line);
//                        terms.add(term);
                            addToQueue(term);
                            addingHash(term, index);
                        }

                        String toWrite = termToAdd.convertToLine(); //ready to be written to disk
                        writing.append(toWrite + " \n");

                        if (dictionary.containsKey(termToAdd.getName())) {
                            int occurrence = dictionary.get(termToAdd.getName())[1];
                            //int occurrence = termToAdd.getNumOfOccure();
                            Integer[] arr = {lineNumber, occurrence};
                            //dictionary.remove(termToAdd.getName());
                            dictionary.put(termToAdd.getName(), arr); //updating the line number in the total post file and num of occurrence
                            lineNumber++;
                        }
                        if (termToAdd.getName().equals(termToAdd.getName().toUpperCase()) && !isNum(termToAdd.getName()) && !Character.isDigit(termToAdd.getName().charAt(0))) {
                            ArrayList<String> docs = termToAdd.docsApear();
                            for (String doc : docs) {
                                Documents doctmps = docsCoprus.get(doc);
                                doctmps.addEnity(termToAdd.getName());
                                docsCoprus.put(doc, doctmps);
                            }
                        }
                        hashTermToPost.remove(termToAdd);
                    }
                    //if there is more than one term of the same kind in the queue
                    else if (numOfTimeInPosts > 1) {
                        TermPost tmpTerm = terms.poll();
                        tmpTerm.addPost(termToAdd); //merged 2 posts
                        terms.add(tmpTerm);

                        int index = hashTermToPost.get(termToAdd).get(0);
                        List<Integer> listTmp = hashTermToPost.get(termToAdd);
                        listTmp.remove(0);
                        hashTermToPost.remove(termToAdd);
                        hashTermToPost.put(tmpTerm, listTmp);

                        //getting new line from file instead of the file we took from him the term
                        String line = brs[index].readLine();
                        if (line != null) {
                            TermPost term = new TermPost(line);
//                        terms.add(term);
                            addToQueue(term);
                            addingHash(term, index);
                        }

                    }
                }
                if (round % 10 == 0 && round != 0) {
                    //write to disk
                    bw.write(writing.toString());
                    bw.flush();
                    writing = new StringBuilder("");
                }
                round++;
            }
            //write rest to disk
            if (terms.size() == 0 || hashTermToPost.size() == 0) {
                bw.write(writing.toString());
                bw.flush();
                writing = null;
            }
            //closing all the buffer reader
            for (BufferedReader br : brs) {
                br.close();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //handle erasing temp file
        for (int i = 0; i < postingNumber; i++) {
            File currentFile = new File(pathPost + "\\PostingDirectory\\postFile" + i + ".txt");
            currentFile.delete();
        }
        if (directory.exists())
            directory.delete();

        //System.out.println("The dictionary size is: " + dictionary.size());
        writeDictionaryToDisk();

    }

    /**
     * Writes all the dictionary to disk [Key*;~line number,number of appearance
     */
    private void writeDictionaryToDisk() {
        String name = "";
        String namedir = "";
        if (stemming) {
            name = "dictionaryStem";
            namedir = "Stem";
        } else {
            name = "dictionaryNoStem";
            namedir = "WithoutStem";
        }
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathPost + "\\" + namedir + "\\" + name + ".txt", false), StandardCharsets.UTF_8));
            int count = 0;
            StringBuilder writeIt = new StringBuilder("");
            for (Map.Entry<String, Integer[]> entry : dictionary.entrySet()) {
                count++;
                String key = entry.getKey();
                int location = entry.getValue()[0];
                int value = entry.getValue()[1];
                writeIt.append(key + "*;~" + location + "," + value + "\n");
                if (count % 10000 == 0) {
                    bw.write(writeIt.toString());
                    bw.flush();
                    writeIt = new StringBuilder("");
                }
            }
            bw.write(writeIt.toString());
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //handle the insertion to queue with more checks in the dictionary
    private void addToQueue(TermPost term) {
        String keyLower = term.getName().toLowerCase();
        if (!dictionary.containsKey(term.getName())) {
            if (dictionary.containsKey(keyLower))
                term.setName(keyLower);
        }
        terms.add(term);
    }

    //handling duplicate, will append a list of number of post file in which the terms appear
    //controlling the lower case - upper case
    private void addingHash(TermPost term, int i) {
        if (!hashTermToPost.containsKey(term)) {
            TermPost tmpLow = new TermPost(term.getName().toLowerCase(), "");
            TermPost tmpUp = new TermPost(term.getName().toUpperCase(), "");
            String termTmp = term.getName().toLowerCase();
            //int num=term.getNumOfOccure();
            //exist in lower case
            if (hashTermToPost.containsKey(tmpLow)) {
                term.setName(termTmp);
            }
            //exist in upper case
            else if (hashTermToPost.containsKey(tmpUp)) {
                List<Integer> tmps = hashTermToPost.get(tmpUp); //getting the value
//                for (Map.Entry<TermPost, List<Integer>> entry : hashTermToPost.entrySet()) {
//                    if (entry.getKey().getName().equals(tmpUp.getName())) {
//                        tmpUp = entry.getKey(); //getting the key (in is object)
//                        break;
//                    }
//                }
//                tmpUp.addPost(term);
                hashTermToPost.remove(tmpUp);
                tmpUp.setName(termTmp);
                //tmpUp.setNumOfOccure(num);
                hashTermToPost.put(tmpUp, tmps); //put in lower
            } else {
                hashTermToPost.put(term, new LinkedList<>());
            }
        }

        hashTermToPost.get(term).add(i);


    }

    public void addToDict() {
        try {
            BufferedReader br = null;
            String namedir = "";
            if (stemming)
                br = new BufferedReader(new InputStreamReader(new FileInputStream(pathPost + "\\Stem\\dictionaryStem.txt"), StandardCharsets.UTF_8));
            else
                br = new BufferedReader(new InputStreamReader(new FileInputStream(pathPost + "\\WithoutStem\\dictionaryNoStem.txt"), StandardCharsets.UTF_8));
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                count++;
                ArrayList<String> arrayList = mySplit(line, "*;~");
                String term = arrayList.get(0);
                String information = arrayList.get(1);
                ArrayList<String> arrayList1 = mySplit(information, ",");
                String linenumber = arrayList1.get(0);
                String numOfOcccur = arrayList1.get(1);
                int num1 = Integer.parseInt(linenumber);
                int num2 = Integer.parseInt(numOfOcccur);
                Integer[] arr = {num1, num2};
                dictionary.put(term, arr);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadDocuments() {
        try {
            BufferedReader br = null;
            String namedir = "";
            if (stemming)
                br = new BufferedReader(new InputStreamReader(new FileInputStream(pathPost + "\\Stem\\Documents.txt"), StandardCharsets.UTF_8));
            else
                br = new BufferedReader(new InputStreamReader(new FileInputStream(pathPost + "\\WithoutStem\\Documents.txt"), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                ArrayList<String> arrayList = mySplit(line, ":");
                String docName = arrayList.get(0);
                String information = arrayList.get(1);
                ArrayList<String> arrayList1 = mySplit(information, ";");
                String maxTf = arrayList1.get(0);
                maxTf = maxTf.substring(1);
                String unique = arrayList1.get(1);
                String size = arrayList1.get(2);
                String origin = "";
                if (arrayList1.size() > 3) {
                    origin = arrayList1.get(3);
                }

                int maxTfNum = Integer.parseInt(maxTf);
                int uniqueNum = Integer.parseInt(unique);
                int sizeNum = Integer.parseInt(size);
                Documents doc = new Documents(docName, null);
                doc.setMax_tf(maxTfNum);
                doc.setNumOfUniqe(uniqueNum);
                doc.setOrigin(origin);
                doc.setSize(sizeNum);
                docsCoprus.put(doc.getIdDoc(), doc);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    static class TreeCompare implements Comparator<String> {
        /* Compares keys based on the
           last word's natural ordering */
        public int compare(String a, String b) {
            return a.compareToIgnoreCase(b);
        }

    }


    public static TreeMap<String, Integer[]> sort() {
        TreeMap<String, Integer[]> sort = new TreeMap<>(new Indexer.TreeCompare());
        sort.putAll(dictionary);
        return sort;
    }


    //The information on the docs and uniques
    public String information() {
        int numOfUnique = dictionary.size();
        return "Number of docs: " + numOfDoc + "\n" + "Number of unique: " + numOfUnique + "\n";
    }

    private static boolean isNum(String s) {
        if (s.contains(",")) {
            s = s.replace(",", "");
        }
        try {
            double d = Double.parseDouble(s);
        } catch (NumberFormatException ex) {
            return false;
        }
        return true;
    }
}
