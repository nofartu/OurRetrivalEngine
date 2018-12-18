package sample;

import javafx.util.Pair;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static sample.ApiJson.cities;
import static sample.ReadFile.mySplit;


public class Parse {

    private Stemmer stemmer;//
    private HashSet<Character> delimiters; //
    private HashSet<String> stopwords; //
    private HashMap<String, String> months; //
    private ArrayList<String> allWords;
    private HashMap<String, Integer[]> terms; //pair: key ->number of occurrence. value -> location in text  {number of occurrence, location in text}
    private boolean isStem;
    private ApiJson apiJson;

    public Parse(HashSet<String> stop, boolean stem, ApiJson api) {
        delimiters = new HashSet<>();
        initialDelimiter();
        stopwords = stop;
        allWords = new ArrayList<>();
        months = new HashMap<>();
        initialMonths();
        terms = new HashMap<>();
        isStem = stem;
        apiJson = api;
        stemmer = new Stemmer();
    }

//
//    static class TreeCompare implements Comparator<String> {
//        /* Compares keys based on the
//           last word's natural ordering */
//        public int compare(String a, String b) {
//            return a.compareToIgnoreCase(b);
//        }
//
//    }

    private void initialDelimiter() {
        delimiters.add('/');
        delimiters.add('!');
        delimiters.add('?');
        delimiters.add('@');
        delimiters.add('#');
        delimiters.add('^');
        delimiters.add('&');
        delimiters.add('*');
        delimiters.add('(');
        delimiters.add(')');
        delimiters.add('[');
        delimiters.add(']');
        delimiters.add('{');
        delimiters.add('}');
        delimiters.add('|');
        delimiters.add('.');
        delimiters.add(',');
        delimiters.add('"');
        delimiters.add('<');
        delimiters.add('>');
        delimiters.add(':');
        delimiters.add(';');
        delimiters.add('~');
        delimiters.add('`');
        delimiters.add('-');
        delimiters.add('+');
        delimiters.add('=');
        delimiters.add('_');
        delimiters.add('�');
        delimiters.add('⪕');
        delimiters.add('⪖');
        delimiters.add('¥');
        delimiters.add('°');
        delimiters.add('\'');
        delimiters.add('\\');
    }

    private void initialMonths() {
        months.put("january", "01");
        months.put("february", "02");
        months.put("march", "03");
        months.put("april", "04");
        months.put("may", "05");
        months.put("june", "06");
        months.put("july", "07");
        months.put("august", "08");
        months.put("september", "09");
        months.put("october", "10");
        months.put("november", "11");
        months.put("december", "12");
        months.put("jan", "01");
        months.put("feb", "02");
        months.put("mar", "03");
        months.put("apr", "04");
        months.put("jun", "06");
        months.put("jul", "07");
        months.put("aug", "08");
        months.put("sep", "09");
        months.put("oct", "10");
        months.put("nov", "11");
        months.put("dec", "12");
    }

    public HashMap<String, Integer[]> parsing(String allText, String docName) {
        allWords = mySplit(allText, " ");
        int size = allWords.size();
        for (int i = 0; i < allWords.size(); i++) {
            boolean finalCheck = false;
            String term = deleteSpares(allWords.get(i));
            allWords.set(i, term);
            String find = allWords.get(i);
            //handle the percent with the %
            if (!find.equals("")) {
                if (find.charAt(find.length() - 1) == ('%')) {
                    i = i + handlePercent(i);
                } else if (find.charAt(0) == '$') {
                    i = i + handlePriceSign(i);
                } else if (isNum(find)) {
                    i = i + centerOfNumbers(i);
                } else if (find.contains("-")) {
                    i = i + handleHyphen(i);
                } else if (find.equalsIgnoreCase("between")) {
                    i = i + handleBetween(i);
                } else if (find.equals("THE")) {
                    i = i + handleThe(i);
                } else if (find.equalsIgnoreCase("u.s.") || find.equalsIgnoreCase("u.s") || find.equalsIgnoreCase("u.s.a.") || find.equalsIgnoreCase("u.s.a") || find.equalsIgnoreCase("usa")) {
                    if (terms.containsKey("USA")) {
                        Integer[] arr = terms.get("USA");//~~~~************~~~~~~~~~~
                        Integer[] add = {arr[0] + 1, arr[1]};
                        terms.put("USA", add);
                    } else {
                        Integer[] arr = {1, i};
                        terms.put("USA", arr);
                    }
                } else if (find.equalsIgnoreCase("mr") || find.equalsIgnoreCase("mrs")) {
                    i = i + handleMrs(i);
                } else {
                    if (!isStopWord(term)) {
                        if (!term.equals("")) {
                            delimiters.add('%');
                            if (delimiters.contains(term.charAt(0)) || delimiters.contains(term.charAt(term.length() - 1))) {
                                term = deleteSpares(term);
                                finalCheck = isStopWord(term);
                            }
                            if (!finalCheck) {
                                addToCities2(term, docName, i, "L"); //adding to cities
                                term = removeDelimiter(term);
                                checkLetter(term, i);
                            }
                            delimiters.remove('%'); //check this

                        }
                    }
                }
            }
        }

        if (isStem) {
            terms = stemmer.stemming(terms);
        }
        Integer[] arr = {size, 0};
        terms.put("***+++***+++***", arr);
        return terms;
    }


    public void resetParse(){
        terms=new HashMap<>();
        stemmer = new Stemmer();
        allWords = new ArrayList<>();
    }

    private void checkLetter(String term, int i) {
        String upper = term.toUpperCase();
        String lower = term.toLowerCase();
        if (terms.containsKey(upper)) {
            if (upper.charAt(0) == (term.charAt(0))) { //first letter is upper
                Integer[] arr = terms.get(upper);
                Integer[] add = {arr[0] + 1, arr[1]};
                terms.put(upper, add); //~~~~************~~~~~~~~~~
            } else {
                Integer[] arr = terms.get(upper); //this term is not with upper case, and in the hash it is upper so we change it
                Integer[] add = {arr[0], arr[1]};
                terms.remove(upper);
                terms.put(lower, add); //~~~~************~~~~~~~~~~
            }
        } else if (terms.containsKey(lower)) {
            Integer[] arr = terms.get(lower);//~~~~************~~~~~~~~~~
            Integer[] add = {arr[0] + 1, arr[1]};
            terms.put(lower, add);
        } else {
            if (!term.equals(upper) && !term.equals(lower)) {
                term = lower;
            }
            //first time First letter is upper - save all upper
            if (term.charAt(0) == upper.charAt(0)) { //first letter upper
                Integer[] add = {1, i};
                terms.put(upper, add);//~~~~************~~~~~~~~~~
            } else { //first letter not upper
                Integer[] add = {1, i};
                terms.put(lower, add); //~~~~************~~~~~~~~~~
            }
        }
    }

    //first rule - if we find "THE" all the words that will come after with uppercase letter will be saved as one term.
    private int handleThe(int i) { //changed here!!!!!!!! --------!!!!!!!!!!!!
        String term = allWords.get(i);
        for (int j = i + 1; j < allWords.size() - 1; j++) {
            String termTheTmp = allWords.get(j);
            if (termTheTmp.toUpperCase().equals(termTheTmp) && delimiters.contains(termTheTmp.charAt(0))) {
                term = term + " " + termTheTmp;
            } else {
                if (terms.containsKey(term)) {
                    Integer[] arr = terms.get(term);
                    Integer[] add = {arr[0] + 1, arr[1]};//~~~~************~~~~~~~~~~
                    terms.put(term, add);
                } else {
                    Integer[] add = {1, i};
                    terms.put(term, add); //~~~~************~~~~~~~~~~
                }
                return j - i;
            }
        }
        return 0;
    }

    //checks if stop word
    private boolean isStopWord(String s) {
        if (stopwords.contains(s.toLowerCase()))
            return true;
        return false;
    }

    //rule - mr/mrs
    private int handleMrs(int i) {  //changed here!!!!!!!! --------!!!!!!!!!!!!
        String term = allWords.get(i);
        String termNo = "";
        for (int j = i + 1; j < allWords.size() - 1; j++) {
            String termTmps = allWords.get(j);
            if (Character.isUpperCase(termTmps.charAt(0))) {
                if (allWords.get(j).charAt(termTmps.length() - 1) == '.' || allWords.get(j).charAt(termTmps.length() - 1) == ',' || allWords.get(j).charAt(termTmps.length() - 1) == '"' || allWords.get(j).charAt(termTmps.length() - 1) == ')') {
                    String tmpTerm = deleteSpares(termTmps);
                    if (termNo.length() == 0)
                        termNo = tmpTerm;
                    else
                        termNo = termNo + " " + tmpTerm;
                    term = term + " " + tmpTerm;
                    //with mr.
                    if (terms.containsKey(term)) {
                        Integer[] arr = terms.get(term);
                        Integer[] add = {arr[0] + 1, arr[1]};//~~~~************~~~~~~~~~~
                        terms.put(term, add);
                    } else {
                        Integer[] add = {1, i};
                        terms.put(term, add); ////~~~~************~~~~~~~~~~
                    }
                    //without mr
                    if (terms.containsKey(termNo)) {
                        Integer[] arr = terms.get(termNo);
                        Integer[] add = {arr[0] + 1, arr[1]};//~~~~************~~~~~~~~~~
                        terms.put(termNo, add);
                    } else {
                        Integer[] add = {1, i};//~~~~************~~~~~~~~~~
                        terms.put(termNo, add);
                    }

                    return j - i;
                }
                term = term + " " + termTmps;
                if (termNo.length() == 0)
                    termNo = termTmps;
                else
                    termNo = termNo + " " + termTmps;

            } else {
                term = deleteSpares(term); //change here!!! ---!!!
                if (!termNo.equals(""))
                    termNo = deleteSpares(termNo); //change here!!! ---!!!
                if (terms.containsKey(term)) {
                    Integer[] arr = terms.get(term);
                    Integer[] add = {arr[0] + 1, arr[1]};//~~~~************~~~~~~~~~~
                    terms.put(term, add);
                } else {
                    Integer[] add = {1, i};//~~~~************~~~~~~~~~~
                    terms.put(term, add);
                }
                if (terms.containsKey(termNo)) {
                    Integer[] arr = terms.get(termNo);
                    Integer[] add = {arr[0] + 1, arr[1]};//~~~~************~~~~~~~~~~
                    terms.put(term, add);
                } else {
                    Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                    terms.put(termNo, add);
                }

                return j - i;
            }
        }
        return 0;
    }

    //decide on each number what type it is and sends it to it's method
    private int centerOfNumbers(int i) {
        String term = allWords.get(i);
        if (term.charAt(0) == '+' || term.charAt(0) == '-') {
            term = term.substring(1);
        }
        if (term.contains(",")) {
            term = OurReplace(term,",", "");
        }
        if (i + 1 < allWords.size() - 1) {
            String nextTerm = allWords.get(i + 1);
            //handles the percents with the words
            if (nextTerm.equalsIgnoreCase("percent") || nextTerm.equalsIgnoreCase("percentage")) {
                return handlePercent(i + 1);
            }
            if (months.containsKey(nextTerm.toLowerCase()) && !term.contains(".") && term.charAt(0) != '+' && term.charAt(0) != '-') {
                if (term.length() <= 2) {
                    return handleDateMonth(i);
                } else if (term.length() == 4) {
                    return handleDateYear(i);
                }
            }
            //price dollars
            if (nextTerm.equalsIgnoreCase("dollars") || nextTerm.equalsIgnoreCase("dollar")) {
                return handlePrice(i, 0);
            }
            //price word dollars | price word us dollars | number word
            //word like million
            if (nextTerm.equalsIgnoreCase("million") || nextTerm.equalsIgnoreCase("billion") || nextTerm.equalsIgnoreCase("trillion") ||
                    nextTerm.equalsIgnoreCase("m") || nextTerm.equalsIgnoreCase("bn")) {
                if (i + 2 < allWords.size() - 1) {
                    if (allWords.get(i + 2).equalsIgnoreCase("dollars") || allWords.get(i + 2).equalsIgnoreCase("dollar")) {
                        return handlePrice(i, 1);
                    } else if (allWords.get(i + 2).equalsIgnoreCase("u.s.") && i + 3 < allWords.size() - 1 && (allWords.get(i + 3).equalsIgnoreCase("dollars") || allWords.get(i + 3).equalsIgnoreCase("dollar"))) {
                        return handlePrice(i, 2);
                    } else {
                        return handleNumbers(i);
                    }
                }
            }
        }
        if (i - 1 > 0) {
            String prevTerm = allWords.get(i - 1);
            if (months.containsKey(prevTerm.toLowerCase()) && !term.contains(".")) {
                allWords.set(i, prevTerm);//month
                allWords.set(i - 1, term);//number - day
                handleDateMonth(i - 1);
                return 0;
            }
        }
        //just a number
        numberWithWords(Double.parseDouble(term), i);
        return 0;
    }

    private int handleNumbers(int i) {
        String term = allWords.get(i);
        if (term.contains(",")) {
            term = OurReplace(term,",", "");
        }
        String nextTerm = allWords.get(i + 1);
        if (nextTerm.equalsIgnoreCase("Thousand")) {
            Double number = Double.parseDouble(term);
            number = number * 1000;
            numberWithWords(number, i);
            return 1;
        }
        if (nextTerm.equalsIgnoreCase("Million")) {
            Double number = Double.parseDouble(term);
            number = number * 1000000;
            numberWithWords(number, i);
            return 1;
        }
        if (nextTerm.equalsIgnoreCase("billion")) {
            Double number = Double.parseDouble(term);
            number = number * 1000000000;
            numberWithWords(number, i);
            return 1;
        }
        if (nextTerm.equalsIgnoreCase("Trillion")) {
            Double number = Double.parseDouble(term);
            number = number * 1000;
            if (terms.containsKey(number + "B")) {
                Integer[] arr = terms.get(number + "B");
                Integer[] add = {arr[0] + 1, arr[1]};//~~~~************~~~~~~~~~~
                terms.put(number + "B", add);
            } else {
                Integer[] add = {1, i};//~~~~************~~~~~~~~~~
                terms.put(number + "B", add);
            }
            return 1;
        }
        return 0;
    }

    private void numberWithWords(double number, int i) {
        if (number >= 1000 && number < 1000000) {
            number = number / 1000;
            if (terms.containsKey(number + "K")) {
                Integer[] arr = terms.get(number + "K");
                Integer[] add = {arr[0] + 1, arr[1]};//~~~~************~~~~~~~~~~
                terms.put(number + "K", add);
            } else {
                Integer[] add = {1, i};//~~~~************~~~~~~~~~~
                terms.put(number + "K", add);
            }
        } else if (number >= 1000000 && number < 1000000000) {
            number = number / 1000000;
            if (terms.containsKey(number + "M")) {
                Integer[] arr = terms.get(number + "M"); //~~~~************~~~~~~~~~~
                Integer[] add = {arr[0] + 1, arr[1]};
                terms.put(number + "M", add);
            } else {
                Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                terms.put(number + "M", add);
            }
        } else if (number >= 1000000000) {
            number = number / 10000000;
            if (terms.containsKey(number + "B")) {
                Integer[] arr = terms.get(number + "B"); //~~~~************~~~~~~~~~~
                Integer[] add = {arr[0] + 1, arr[1]};
                terms.put(number + "B", add);
            } else {
                Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                terms.put(number + "B", add);
            }

        } else {
            if (terms.containsKey(number + "")) {
                Integer[] arr = terms.get(number + ""); //~~~~************~~~~~~~~~~
                Integer[] add = {arr[0] + 1, arr[1]};
                terms.put(number + "", add);
            } else {
                Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                terms.put(number + "", add);
            }
        }
    }

    //rule $
    private int handlePriceSign(int i) {
        String term = allWords.get(i);
        if (term.contains(",")) {
            term = OurReplace(term,",", "");
        }
        String nextTerm = "";
        // term = term.replace("$", "");
        term = term.substring(1);
        if (i + 1 < allWords.size() - 1) {
            nextTerm = allWords.get(i + 1);
            if (nextTerm.equalsIgnoreCase("Million")) {
                if (isNum(term)) {
                    Double number = Double.parseDouble(term);
                    number = number * 1000000;
                    numberWithWordsDollars(number, i);
                }
                return 1;
            } else if (nextTerm.equalsIgnoreCase("Billion")) {
                if (isNum(term)) {
                    Double number = Double.parseDouble(term);
                    number = number * 1000000000;
                    numberWithWordsDollars(number, i);
                }
                return 1;
            } else if (nextTerm.equalsIgnoreCase("Trillion")) {
                String arr[] = quickSplit(term, " ");
                if (isNum(arr[0])) {
                    Double number = Double.parseDouble(arr[0]);
                    number = number * 1000000;
                    if (terms.containsKey(number + " M Dollars")) {
                        Integer[] tmp = terms.get(number + " M Dollars"); //~~~~************~~~~~~~~~~
                        Integer[] add = {tmp[0] + 1, tmp[1]};
                        terms.put(number + " M Dollars", add);
                    } else {
                        Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                        terms.put(number + " M Dollars", add);
                    }
                }
                return 1;
            } else if (isNum(term)) {
                double num = Double.parseDouble(term);
                if (num < 1000000) {
                    if (terms.containsKey(num + " Dollars")) {
                        Integer[] tmp = terms.get(num + " Dollars"); //~~~~************~~~~~~~~~~
                        Integer[] add = {tmp[0] + 1, tmp[1]};
                        terms.put(num + " Dollars", add);
                    } else {
                        Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                        terms.put(num + " Dollars", add);
                    }

                } else {
                    num = num / 1000000;
                    if (terms.containsKey(num + " M Dollars")) {
                        Integer[] arr = terms.get(num + " M Dollars"); //~~~~************~~~~~~~~~~
                        Integer[] add = {arr[0] + 1, arr[1]};
                        terms.put(num + " M Dollars", add);
                    } else {
                        Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                        terms.put(num + " M Dollars", add);
                    }

                }
                return 0;
            }
        }
        return 0;
    }

    private int handlePrice(int i, int location) {
        String term = allWords.get(i);
        if (term.contains(",")) {
            term = OurReplace(term,",", "");
        }
        String nextTerm = allWords.get(i + location);
        if (location == 0) {
            double num = Double.parseDouble(term);
            if (num < 1000000) {
                if (terms.containsKey(num + " Dollars")) {
                    Integer[] arr = terms.get(num + " Dollars"); //~~~~************~~~~~~~~~~
                    Integer[] add = {arr[0] + 1, arr[1]};
                    terms.put(num + " Dollars", add);
                } else {
                    Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                    terms.put(num + " Dollars", add);
                }
            } else {
                num = num / 1000000;
                if (terms.containsKey(num + " M Dollars")) {
                    Integer[] arr = terms.get(num + " M Dollars"); //~~~~************~~~~~~~~~~
                    Integer[] add = {arr[0] + 1, arr[1]};
                    terms.put(num + " M Dollars", add);
                } else {
                    Integer[] add = {1, i};//~~~~************~~~~~~~~~~
                    terms.put(num + " M Dollars", add);
                }

            }
            return 1;
        }
        if (location == 1 || location == 2) {
            if (nextTerm.contains("/")) {
                double num = Double.parseDouble(term);
                if (num < 1000000) {
                    if (terms.containsKey(term + " " + nextTerm + " Dollars")) {
                        Integer[] arr = terms.get(term + " " + nextTerm + " Dollars"); //~~~~************~~~~~~~~~~
                        Integer[] add = {arr[0] + 1, arr[1]};
                        terms.put(term + " " + nextTerm + " Dollars", add);
                    } else {
                        Integer[] add = {1, i};//~~~~************~~~~~~~~~~
                        terms.put(term + " " + nextTerm + " Dollars", add);
                    }
                } else {
                    num = num / 1000000;
                    if (terms.containsKey(num + " M " + nextTerm + " Dollars")) {
                        Integer[] arr = terms.get(num + " M " + nextTerm + " Dollars"); //~~~~************~~~~~~~~~~
                        Integer[] add = {arr[0] + 1, arr[1]};
                        terms.put(num + " M " + nextTerm + " Dollars", add);
                    } else {
                        Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                        terms.put(num + " M " + nextTerm + " Dollars", add);
                    }

                }
                return location + 1;
            } else if (nextTerm.equalsIgnoreCase("m")) {
                Double number = Double.parseDouble(term);
                number = number * 1000000;
                numberWithWordsDollars(number, i);
                return location + 1;
            } else if (nextTerm.equalsIgnoreCase("bn")) {
                Double number = Double.parseDouble(term);
                number = number * 1000000000;
                numberWithWordsDollars(number, i);
                return location + 1;
            } else if (nextTerm.equalsIgnoreCase("Million")) {
                Double number = Double.parseDouble(term);
                number = number * 1000000;
                numberWithWordsDollars(number, i);
                return location + 1;
            } else if (nextTerm.equalsIgnoreCase("Billion")) {
                Double number = Double.parseDouble(term);
                number = number * 1000000000;
                numberWithWordsDollars(number, i);
                return location + 1;
            } else if (nextTerm.equalsIgnoreCase("Trillion")) {
                Double number = Double.parseDouble(term);
                number = number * 1000000;
                if (terms.containsKey(number + " M Dollars")) {
                    Integer[] arr = terms.get(number + " M Dollars"); //~~~~************~~~~~~~~~~
                    Integer[] add = {arr[0] + 1, arr[1]};
                    terms.put(number + " M Dollars", add);
                } else {
                    Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                    terms.put(number + " M Dollars", add);
                }
                return location + 1;
            }
        }
        return 0;

    }

    private void numberWithWordsDollars(double number, int i) {
        if (number < 1000000) {
            //number = number / 1000;
            if (terms.containsKey(number + " Dollars")) {
                Integer[] arr = terms.get(number + " Dollars"); //~~~~************~~~~~~~~~~
                Integer[] add = {arr[0] + 1, arr[1]};
                terms.put(number + " Dollars", add);
            } else {
                Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                terms.put(number + " Dollars", add);
            }
            //System.out.println(number + "K");
        } else if (number >= 1000000) {
            number = number / 1000000;
            if (terms.containsKey(number + " M Dollars")) {
                Integer[] arr = terms.get(number + " M Dollars"); //~~~~************~~~~~~~~~~
                Integer[] add = {arr[0] + 1, arr[1]};
                terms.put(number + " M Dollars", add);
            } else {
                Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                terms.put(number + " M Dollars", add);
            }

            // System.out.println(number + "M");
        } else {
            if (terms.containsKey(number + "")) {
                Integer[] arr = terms.get(number + ""); //~~~~************~~~~~~~~~~
                Integer[] add = {arr[0] + 1, arr[1]};
                terms.put(number + "", add);
            } else {
                Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                terms.put(number + "", add);
            }

        }
    }

    private int handleBetween(int i) {
        int toReturn = 0;
        if (i + 2 < allWords.size() - 1) {
            if (allWords.get(i + 2).equalsIgnoreCase("and")) {
                String iThree = allWords.get(i + 3);
                if (iThree.contains(",")) {
                  //  iThree = iThree.replace(",", ""); nofar changed
                    iThree = OurReplace(iThree,",", "");

                }
                //two of the words are numbers.
                if (isNum(allWords.get(i + 1)) && i + 3 < allWords.size() - 1 && isNum(iThree)) {
                    if (i + 4 < allWords.size() - 1) {
                        String num1 = addLetter(allWords.get(i + 1));
                        String num2 = "";
                        String iFour = allWords.get(i + 4);
                        if (iFour.contains(",")) {
                          //  iFour = iFour.replace(",", "");
                            iFour = OurReplace(iFour,",", "");
                        }

                        if (iFour.equalsIgnoreCase("million")) {
                            double tmp = Double.parseDouble(iThree);
                            tmp = tmp * 1000000;
                            num2 = addLetter(tmp + "");
                            toReturn = 4;
                        } else if (iFour.equalsIgnoreCase("billion")) {
                            double tmp = Double.parseDouble(iThree);
                            tmp = tmp * 1000000000;
                            num2 = addLetter(tmp + "");
                            toReturn = 4;
                        } else if (iFour.equalsIgnoreCase("thousand")) {
                            double tmp = Double.parseDouble(iThree);
                            tmp = tmp * 1000;
                            num2 = addLetter(tmp + "");
                            toReturn = 4;
                        } else if (iFour.equalsIgnoreCase("trillion")) {
                            double tmp = Double.parseDouble(iThree);
                            tmp = tmp * 1000;
                            num2 = tmp + "B";
                            toReturn = 4;
                        } else {
                            if (iThree.charAt(0) == '+' || iThree.charAt(0) == '-') {
                                iThree = iThree.substring(1);
                            }
                            num2 = addLetter(iThree);
                            toReturn = 3;
                        }
                        if (!num1.equals(""))
                            num1 = deleteSpares(num1);
                        if (!num2.equals(""))
                            num2 = deleteSpares(num2);
                        if (terms.containsKey(num1 + "-" + num2)) {
                            Integer[] arr = terms.get(num1 + "-" + num2); //~~~~************~~~~~~~~~~
                            Integer[] add = {arr[0] + 1, arr[1]};
                            terms.put(num1 + "-" + num2, add);
                        } else {
                            Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                            terms.put(num1 + "-" + num2, add);
                        }
                        if (terms.containsKey(num1)) {
                            Integer[] arr = terms.get(num1); //~~~~************~~~~~~~~~~
                            Integer[] add = {arr[0] + 1, arr[1]};
                            terms.put(num1, add);
                        } else {
                            Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                            terms.put(num1, add);
                        }
                        if (terms.containsKey(num2)) {
                            Integer[] arr = terms.get(num2); //~~~~************~~~~~~~~~~
                            Integer[] add = {arr[0] + 1, arr[1]};
                            terms.put(num2, add);
                        } else {
                            Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                            terms.put(num2, add);
                        }
                    }
                }

            }
            //first num is number word
            else if (i + 3 < allWords.size() - 1 && allWords.get(i + 3).equalsIgnoreCase("and")) {
                if (isNum(allWords.get(i + 1)) && i + 4 < allWords.size() - 1 && isNum(allWords.get(i + 4))) {
                    String num1 = "";
                    String num2 = "";
                    String iTwo = allWords.get(i + 2);
                    if (iTwo.contains(",")) {
                        //iTwo = iTwo.replace(",", "");
                        iTwo = OurReplace(iTwo,",", "");
                    }
                    String iOne = allWords.get(i + 1);
                    if (iOne.contains(",")) {
                       // iOne = iTwo.replace(",", "");
                        iOne = OurReplace(iOne,",", "");
                    }
                    String iFour = allWords.get(i + 4);
                    if (iFour.contains(",")) {
                       // iFour = iFour.replace(",", "");
                        iFour = OurReplace(iFour,",", "");
                    }
                    String iFive = allWords.get(i + 5);
                    if (iFive.contains(",")) {
                        //iFive = iFive.replace(",", "");
                        iFive = OurReplace(iFive,",", "");
                    }
                    if (iTwo.equalsIgnoreCase("million")) {
                        double tmp = Double.parseDouble(iOne);
                        tmp = tmp * 1000000;
                        num1 = addLetter(tmp + "");
                    } else if (iTwo.equalsIgnoreCase("billion")) {
                        double tmp = Double.parseDouble(iOne);
                        tmp = tmp * 1000000000;
                        num1 = addLetter(tmp + "");
                    } else if (iTwo.equalsIgnoreCase("thousand")) {
                        double tmp = Double.parseDouble(iOne);
                        tmp = tmp * 1000;
                        num1 = addLetter(tmp + "");
                    } else if (iTwo.equalsIgnoreCase("trillion")) {
                        double tmp = Double.parseDouble(iOne);
                        tmp = tmp * 1000;
                        num1 = tmp + "B";
                    }
                    if (iFive.equalsIgnoreCase("million")) {
                        double tmp = Double.parseDouble(iFour);
                        tmp = tmp * 1000000;
                        num2 = addLetter(tmp + "");
                        toReturn = 5;
                    } else if (iFive.equalsIgnoreCase("billion")) {
                        double tmp = Double.parseDouble(iFour);
                        tmp = tmp * 1000000000;
                        num2 = addLetter(tmp + "");
                        toReturn = 5;
                    } else if (iFive.equalsIgnoreCase("thousand")) {
                        double tmp = Double.parseDouble(iFour);
                        tmp = tmp * 1000;
                        num2 = addLetter(tmp + "");
                        toReturn = 5;
                    } else if (iFive.equalsIgnoreCase("trillion")) {
                        double tmp = Double.parseDouble(iFour);
                        tmp = tmp * 1000;
                        num2 = tmp + "B";
                        toReturn = 5;
                    } else {
                        if (iFour.charAt(0) == '+' || iFour.charAt(0) == '-') {
                            iFour = iFour.substring(1);
                        }
                        num2 = addLetter(iFour);
                        toReturn = 4;
                    }
                    if (!num1.equals(""))
                        num1 = deleteSpares(num1);
                    if (!num2.equals(""))
                        num2 = deleteSpares(num2);
                    if (terms.containsKey(num1 + "-" + num2)) {
                        Integer[] arr = terms.get(num1 + "-" + num2); //~~~~************~~~~~~~~~~
                        Integer[] add = {arr[0] + 1, arr[1]};
                        terms.put(num1 + "-" + num2, add);
                    } else {
                        Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                        terms.put(num1 + "-" + num2, add);
                    }
                    if (terms.containsKey(num1)) {
                        Integer[] arr = terms.get(num1); //~~~~************~~~~~~~~~~
                        Integer[] add = {arr[0] + 1, arr[1]};
                        terms.put(num1, add);
                    } else {
                        Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                        terms.put(num1, add);
                    }
                    if (terms.containsKey(num2)) {
                        Integer[] arr = terms.get(num2); //~~~~************~~~~~~~~~~
                        Integer[] add = {arr[0] + 1, arr[1]};
                        terms.put(num2, add);
                    } else {
                        Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                        terms.put(num2, add);
                    }
                }
            }
        }

        return toReturn;
    }

    private int handleHyphen(int i) {
        String arr[] = quickSplit(allWords.get(i), "-");
        arr[0] = deleteSpares(arr[0]);
        arr[1] = deleteSpares(arr[1]);
        boolean a0 = false;
        boolean a1 = false;
        String num0 = "";
        String num1 = "";
        if (isNum(arr[0])) {
            num0 = addLetter(arr[0]);
            a0 = true;
        }
        if (isNum(arr[1])) {
            num1 = addLetter(arr[1]);
            a1 = true;
        }
        if (a0 && a1) {
            if (terms.containsKey(num0 + "-" + num1)) {
                Integer[] tmp = terms.get(num0 + "-" + num1); //~~~~************~~~~~~~~~~
                Integer[] add = {tmp[0] + 1, tmp[1]};
                terms.put(num0 + "-" + num1, add);
            } else {
                Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                terms.put(num0 + "-" + num1, add);
            }
            if (terms.containsKey(num0)) {
                Integer[] tmp = terms.get(num0); //~~~~************~~~~~~~~~~
                Integer[] add = {tmp[0] + 1, tmp[1]};
                terms.put(num0, add);
            } else {
                Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                terms.put(num0, add);
            }
            if (terms.containsKey(num1)) {
                Integer[] tmp = terms.get(num1); //~~~~************~~~~~~~~~~
                Integer[] add = {tmp[0] + 1, tmp[1]};
                terms.put(num1, add);
            } else {
                Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                terms.put(num1, add);
            }

        } else if (a0 && !a1) {
            arr[1] = arr[1].toLowerCase();
            if (terms.containsKey(num0 + "-" + arr[1])) {
                Integer[] tmp = terms.get(num0 + "-" + arr[1]); //~~~~************~~~~~~~~~~
                Integer[] add = {tmp[0] + 1, tmp[1]};
                terms.put(num0 + "-" + arr[1], add);
            } else {
                Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                terms.put(num0 + "-" + arr[1], add);
            }
            if (terms.containsKey(num0)) {
                Integer[] tmp = terms.get(num0); //~~~~************~~~~~~~~~~
                Integer[] add = {tmp[0] + 1, tmp[1]};
                terms.put(num0, add);
            } else {
                Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                terms.put(num0, add);
            }
        } else if (!a0 && a1) {
            arr[0] = arr[0].toLowerCase();
            if (terms.containsKey(arr[0] + "-" + num1)) {
                Integer[] tmp = terms.get(arr[0] + "-" + num1); //~~~~************~~~~~~~~~~
                Integer[] add = {tmp[0] + 1, tmp[1]};
                terms.put(arr[0] + "-" + num1, add);
            } else {
                Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                terms.put(arr[0] + "-" + num1, add);
            }
            if (terms.containsKey(num1)) {
                Integer[] tmp = terms.get(num1); //~~~~************~~~~~~~~~~
                Integer[] add = {tmp[0] + 1, tmp[1]};
                terms.put(num1, add);
            } else {
                Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                terms.put(num1, add);
            }
        } else {
            String toAdd = arr[0] + "-" + arr[1];
            if (terms.containsKey(toAdd)) {
                Integer[] tmp = terms.get(toAdd); //~~~~************~~~~~~~~~~
                Integer[] add = {tmp[0] + 1, tmp[1]};
                terms.put(toAdd, add);
            } else {
                Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                terms.put(toAdd, add);
            }
        }
        return 0;
    }

    private int handleDateMonth(int i) {
        String termDay = allWords.get(i);
        String termMonth = "";
        String termYear = "";
        boolean yesYear = false;
        if (termDay.length() == 1) {
            termDay = "0" + termDay;
        }
        String nextTerm = allWords.get(i + 1);
        if (i + 2 < allWords.size() - 1) {
            termYear = allWords.get(i + 2);
            if (termYear.length() == 4 && isNum(termYear) && !termYear.contains(".") && termYear.charAt(0) != '+' && termYear.charAt(0) != '-') {
                yesYear = true;
            }
        }
        if (nextTerm.length() == 3 && !nextTerm.equalsIgnoreCase("may")) {
            for (Map.Entry<String, String> e : months.entrySet()) {
                if (e.getKey().contains(nextTerm.toLowerCase())) {
                    termMonth = e.getValue();
                    if (terms.containsKey(termMonth + "-" + termDay)) {
                        Integer[] tmp = terms.get(termMonth + "-" + termDay); //~~~~************~~~~~~~~~~
                        Integer[] add = {tmp[0] + 1, tmp[1]};
                        terms.put(termMonth + "-" + termDay, add);
                    } else {
                        Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                        terms.put(termMonth + "-" + termDay, add);
                    }
                    if (yesYear) {
                        if (terms.containsKey(termYear + "-" + termMonth)) {
                            Integer[] tmp = terms.get(termYear + "-" + termMonth); //~~~~************~~~~~~~~~~
                            Integer[] add = {tmp[0] + 1, tmp[1]};
                            terms.put(termYear + "-" + termMonth, add);
                        } else {
                            Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                            terms.put(termYear + "-" + termMonth, add);
                        }
                        return 2;
                    }
                    break;
                }
            }
            return 1;
        } else {
            termMonth = months.get(nextTerm.toLowerCase());
            if (terms.containsKey(termMonth + "-" + termDay)) {
                Integer[] tmp = terms.get(termMonth + "-" + termDay); //~~~~************~~~~~~~~~~
                Integer[] add = {tmp[0] + 1, tmp[1]};
                terms.put(termMonth + "-" + termDay, add);
            } else {
                Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                terms.put(termMonth + "-" + termDay, add);
            }

            if (yesYear) {
                if (terms.containsKey(termYear + "-" + termMonth)) {
                    Integer[] tmp = terms.get(termYear + "-" + termMonth); //~~~~************~~~~~~~~~~
                    Integer[] add = {tmp[0] + 1, tmp[1]};
                    terms.put(termYear + "-" + termMonth, add);
                } else {
                    Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                    terms.put(termYear + "-" + termMonth, add);
                }
                return 2;
            }
            return 1;
        }
    }

    private int handleDateYear(int i) {
        String termYear = allWords.get(i);
        String nextTerm = allWords.get(i + 1);
        String termMonth = "";
        String termDay = "";
        boolean yesDay = false;
        if (i + 2 < allWords.size() - 1) {
            termDay = allWords.get(i + 2);
            if (termDay.length() <= 2 && isNum(termDay) && !termDay.contains(".") && termYear.charAt(0) != '+' && termYear.charAt(0) != '-') {
                if (termDay.length() == 1) {
                    termDay = "0" + termDay;
                }
                yesDay = true;
            }
        }
        if (nextTerm.length() == 3 && !nextTerm.equalsIgnoreCase("may")) {
            for (Map.Entry<String, String> e : months.entrySet()) {
                if (e.getKey().contains(nextTerm.toLowerCase())) {
                    termMonth = e.getValue();
                    if (terms.containsKey(termYear + "-" + termMonth)) {
                        Integer[] tmp = terms.get(termYear + "-" + termMonth); //~~~~************~~~~~~~~~~
                        Integer[] add = {tmp[0] + 1, tmp[1]};
                        terms.put(termYear + "-" + termMonth, add);
                    } else {
                        Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                        terms.put(termYear + "-" + termMonth, add);
                    }
                    if (yesDay) {
                        if (terms.containsKey(termMonth + "-" + termDay)) {
                            Integer[] tmp = terms.get(termMonth + "-" + termDay); //~~~~************~~~~~~~~~~
                            Integer[] add = {tmp[0] + 1, tmp[1]};
                            terms.put(termMonth + "-" + termDay, add);
                        } else {
                            Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                            terms.put(termMonth + "-" + termDay, add);
                        }
                        return 2;
                    }
                    break;
                }
            }
            return 1;
        } else {
            termMonth = months.get(nextTerm.toLowerCase());


            if (terms.containsKey(termYear + "-" + termMonth)) {
                Integer[] tmp = terms.get(termYear + "-" + termMonth); //~~~~************~~~~~~~~~~
                Integer[] add = {tmp[0] + 1, tmp[1]};
                terms.put(termYear + "-" + termMonth, add);
            } else {
                Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                terms.put(termYear + "-" + termMonth, add);
            }
            if (yesDay) {
                if (terms.containsKey(termMonth + "-" + termDay)) {
                    Integer[] tmp = terms.get(termMonth + "-" + termDay); //~~~~************~~~~~~~~~~
                    Integer[] add = {tmp[0] + 1, tmp[1]};
                    terms.put(termMonth + "-" + termDay, add);
                } else {
                    Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                    terms.put(termMonth + "-" + termDay, add);
                }
                return 2;
            }
            return 1;
        }

    }

    private int handlePercent(int i) {
        String term = allWords.get(i);
        if (term.equalsIgnoreCase("percent")) {
            term = addLetter(allWords.get(i - 1)) + "%";
            if (terms.containsKey(term)) {
                Integer[] tmp = terms.get(term); //~~~~************~~~~~~~~~~
                Integer[] add = {tmp[0] + 1, tmp[1]};
                terms.put(term, add);
            } else {
                Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                terms.put(term, add);
            }

            return 1;
        } else if (term.equalsIgnoreCase("percentage")) {
            term = addLetter(allWords.get(i - 1)) + "%";
            if (terms.containsKey(term)) {
                Integer[] tmp = terms.get(term); //~~~~************~~~~~~~~~~
                Integer[] add = {tmp[0] + 1, tmp[1]};
                terms.put(term, add);
            } else {
                Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                terms.put(term, add);
            }

            return 1;
        } else {
            term = OurReplace(term,"%", "");
            if (isNum(term)) {
                term = addLetter(term) + "%";
                if (terms.containsKey(term)) {
                    Integer[] tmp = terms.get(term); //~~~~************~~~~~~~~~~
                    Integer[] add = {tmp[0] + 1, tmp[1]};
                    terms.put(term, add);
                } else {
                    Integer[] add = {1, i}; //~~~~************~~~~~~~~~~
                    terms.put(term, add);
                }
            }
            return 0;
        }
    }

    private String addLetter(String numberS) {
        if (numberS.contains(",")) {
            numberS = OurReplace(numberS,",", "");

        }
        double number;
        String numLet = "";
        if (isNum(numberS)) {
            number = Double.parseDouble(numberS);
            if (number >= 1000 && number < 1000000) {
                number = number / 1000;
                numLet = number + "K";
            } else if (number >= 1000000 && number < 1000000000) {
                number = number / 1000000;
                numLet = number + "M";
            } else if (number >= 1000000000) {
                number = number / 10000000;
                numLet = number + "B";
            } else {
                numLet = number + "";
            }
        }
        return numLet;
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

    private static String[] quickSplit(String str, String regex) {
        Vector<String> result = new Vector<String>();
        int start = 0;
        int pos = str.indexOf(regex);
        while (pos >= start) {
            if (pos > start) {
                result.add(str.substring(start, pos));
            }
            start = pos + regex.length();
            //result.add(regex);
            pos = str.indexOf(regex, start);
        }
        if (start < str.length()) {
            result.add(str.substring(start));
        }
        String[] array = result.toArray(new String[0]);
        return array;
    }

    private String deleteSpares(String term) {
        String end = term;
        char first = term.charAt(0);
        char last = term.charAt(term.length() - 1);
        boolean needToStop = false;
        while ((delimiters.contains(first) || delimiters.contains(last)) && !needToStop) {
            if (delimiters.contains(first)) {
                end = end.substring(1);
                if (end.length() > 0)
                    first = end.charAt(0);
                else
                    needToStop = true;

            }
            if (delimiters.contains(last)) {
                //end=end.substring(0,end.length()-1);
                if (end.length() > 0) {
                    end = end.substring(0, end.length() - 1);
                    if (end.length() > 0)
                        last = end.charAt(end.length() - 1);
                    else
                        needToStop = true;
                } else
                    needToStop = true;
            }
        }
        return end;
    }

//
//    private String delDelimiter(String term){
//        if(term.contains("\\")||term.contains("\'")||term.contains("\\\"")||term.contains("$")||term.contains("/")||term.contains("!")||term.contains("?")||term.contains("@")
//                ||term.contains("#")||term.contains("^")||term.contains("*")||term.contains("&")||term.contains("(")||term.contains(")")
//                ||term.contains("[")||term.contains("]")||term.contains("{")||term.contains("}")||term.contains("|")||term.contains(".")
//                ||term.contains(",")||term.contains("<")||term.contains(">")||term.contains(":")||term.contains(";")||term.contains("~")
//                ||term.contains("`")||term.contains("-")||term.contains("+")||term.contains("=")||term.contains("_")||term.contains("�")
//                ||term.contains("⪕")||term.contains("⪖")||term.contains("¥")||term.contains("°")){
//            Iterator<Character> it = delimiters.iterator();
//            while(it.hasNext()){
//                Character chars=it.next();
//                if(term.contains(chars+"")){
//                    term=term.replace(chars+"", " ");
//                }
//            }
//        }
//
//        return term;
//    }
//    private String removeDelimiter(String term){
//        term=term.replaceAll("[\\\\.$|,|;|'|,%|\\-|`|~|!|@|#|^|&|*|(|)|\\[|\\]|{|}|+|=|_|\\\'|:|/|<|>|?|\\\"|\\.|�|⪕|⪖|¥|°|]"," ");
//        return term;
//    }

    private String removeDelimiter(String term) {
        String tmps = term;
        for (int i = 0; i < tmps.length(); i++) {
            if (delimiters.contains(tmps.charAt(i))) {
                if ((i > 0 && tmps.charAt(i) == '.' || i > 0 && tmps.charAt(i) == ',') && Character.isDigit(tmps.charAt(i - 1))) {

                } else if (i + 1 < tmps.length()) {
                    if (tmps.charAt(i) == '\'') {
                        if (tmps.charAt(i + 1) == 's' || tmps.charAt(i + 1) == 'S') {
                            tmps = tmps.substring(0, i);
                        }
                    } else if (delimiters.contains((tmps.charAt(i + 1)))) {
                        //tmps = tmps.replace(tmps.charAt(i) + "", "");
                        tmps = OurReplace(tmps, tmps.charAt(i) + "", "");
                    } else {
                        //tmps = tmps.replace(tmps.charAt(i) + "", " ");
                        tmps = OurReplace(tmps,tmps.charAt(i) + "", " ");
                    }
                }
            }
        }
        return tmps;
    }
    private String OurReplace(String s, String target, String replacement) {
        StringBuilder sb = null;
        int start = 0;
        for (int i; (i = s.indexOf(target, start)) != -1; ) {
            if (sb == null) sb = new StringBuilder();
            sb.append(s, start, i);
            sb.append(replacement);
            start = i + target.length();
        }
        if (sb == null) return s;
        sb.append(s, start, s.length());
        return sb.toString();
    }

    private String OurReplace(String s, char[] targets, String replacement) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < s.length(); i++){
            char check = s.charAt(i);
            boolean contain = false;
            for(int j = 0; !contain && j < targets.length; j++){
                if(check == targets[j]){
                    contain = true;
                }
            }
            if(contain){
                sb.append(replacement);
            }
            else{
                sb.append(check);
            }
        }
        return sb.toString();
    }

    public void addToCities2(String tmp, String docName, int locationInDoc, String bNoB) {
        if (!Character.isDigit(tmp.charAt(0)) && tmp.charAt(0) != '<' && tmp.charAt(0) != '-' && tmp.charAt(0) != '\'' && tmp.charAt(0) != '[' && tmp.charAt(0) != '(' && !Character.isDigit(tmp.charAt(0))) {
            String ci = tmp.toUpperCase();
            if (cities.containsKey(ci)) {
                if (!cities.get(ci).getLocations().containsKey(docName)) {
                    cities.get(ci).getLocations().put(docName, new ArrayList<String>());
                }
                cities.get(ci).getLocations().get(docName).add(locationInDoc + bNoB);

                //(cities.get(tmp).locations.containsKey(docName));
            } else {
                //ApiJson apiJson = new ApiJson();
                City c = apiJson.findInCities(tmp);
                if (c != null) {
                    c.getLocations().put(docName, new ArrayList<String>());
                    c.setLocations(docName, locationInDoc + bNoB);
                    cities.put(tmp, c);

                }

            }
        }

    }

}
