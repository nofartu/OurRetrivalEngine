package sample;


import javafx.util.Pair;

import java.util.ArrayList;


import static sample.ReadFile.mySplit;

public class TermPost implements Comparable<TermPost> {

    private String termName;
    private String post;
    private int numOfDocuments;

    public TermPost(String line) {
        numOfDocuments = 0;
        int charStar = line.indexOf("*;~");
        String nameTerm = line.substring(0, charStar);
        String itsPost = line.substring(charStar + 4);
        this.termName = nameTerm;
        this.post = itsPost;
        if (!post.equals("")) {
            updateDf();
        }
    }

    public TermPost(String name, String post) {
        numOfDocuments = 0;
        this.termName = name;
        this.post = post;
        if (!post.equals("")) {
            updateDf();
        }
    }

    public String getName() {
        return termName;
    }

    public void setName(String name) {
        this.termName = name;
    }

    public void addPost(TermPost termPost) {
        post = post + termPost.post;
        //setNumOfOccure(termPost.getNumOfOccure());
        updateDf();
    }

    public String convertToLine() {
        return this.termName + "*;~ " + this.post + " df:" + numOfDocuments;
    }

    public void updateDf() {
        int charStar = post.indexOf("*;~");
        String itsPost = post.substring(charStar + 4);
        ArrayList<String> posts = mySplit(itsPost, " ");
        numOfDocuments = posts.size();
    }
    public String getPost(){
        return this.post;
    }

    public int hashCode() {
        return TermPost.class.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;

        TermPost term = (TermPost) obj;
        return (term.termName.equals(this.termName));
    }


    public int compareTo(TermPost second) {
        return this.termName.toLowerCase().compareTo(second.termName.toLowerCase());
    }

    public ArrayList<Pair<String, Integer>> docsApear(){
        ArrayList<Pair<String, Integer>> docs=new ArrayList<>();
        ArrayList<String> posts = mySplit(post, " ");
        for(String s: posts){
            ArrayList<String> doc=mySplit(s, ":");
            int numOfOcur=Integer.parseInt(doc.get(1));
            docs.add(new Pair<>(doc.get(0),numOfOcur));
        }
        return docs;
    }

//    public void setNumOfOccure(int num) {
//        this.numOfOccure = numOfOccure + num;
//    }
//
//    public int getNumOfOccure() {
//        return numOfOccure;
//    }

//    private static String[] mySplit(String str, String regex) {
//        Vector<String> result = new Vector<String>();
//        int start = 0;
//        int pos = str.indexOf(regex);
//        while (pos >= start) {
//            if (pos > start) {
//                result.add(str.substring(start, pos));
//            }
//            start = pos + regex.length();
//            //result.add(regex);
//            pos = str.indexOf(regex, start);
//        }
//        if (start < str.length()) {
//            result.add(str.substring(start));
//        }
//        return result.toArray(new String[0]);
//    }
}
