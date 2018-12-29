package sample;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.*;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static sample.ReadFile.mySplit;

public class ApiJson {

    private JsonElement ele;
    public static HashMap<String, City> cityHashMap;
    public static HashMap<String, City> cities;

    public ApiJson() {
        cityHashMap = new HashMap<>();
        cities = new HashMap<>();
        try {
            OkHttpClient client = new OkHttpClient();
            HttpUrl.Builder urlBuilder = HttpUrl.parse("https://restcountries.eu/rest/v2/all?fields=capital;name;population;currencies").newBuilder();
            String url = urlBuilder.build().toString();
            Request request = new Request.Builder().url(url).build();
            Call call = client.newCall(request);
            Response response = call.execute();
            JsonParser parser = new JsonParser();
            ele = parser.parse(response.body().string());
        } catch (Exception e) {

        }
        init();

    }

    public void init() {
        String number;
        double num;
        JsonArray arr = ele.getAsJsonArray();
        for (int i = 0; i < arr.size(); i++) {
            JsonObject o = (JsonObject) (arr.get(i));
            String str = o.get("capital").getAsString();
            String str1 = str.toUpperCase();
            //get country name
            String country = o.get("name").getAsString();
            //get currency code
            JsonArray currencies = o.get("currencies").getAsJsonArray();
            JsonObject o1;//= (JsonObject) (currencies.get(0));
            String currencyCode = "";
            try {
                o1 = (JsonObject) (currencies.get(0));
                currencyCode = o1.get("code").getAsString();
            } catch (Exception e) {
                o1 = (JsonObject) (currencies.get(1));
                currencyCode = o1.get("code").getAsString();
            }

            //get population in the correct format
            number = o.get("population").getAsString();
            //CHANGE NUMBER INTO THE CORRECT FORMAT K/M/B
            if (!number.equals("")) {
                num = Double.parseDouble(number);
                if (num >= 1000 && num < 1000000) {
                    num = num / 1000;
                    number = roundUp(num) + "K";
                } else if (num >= 1000000 && num < 1000000000) {
                    num = num / 1000000;
                    number = roundUp(num) + "M";
                } else if (num >= 1000000000) {
                    num = num / 1000000000;
                    number = roundUp(num) + "B";
                } else {
                    //in case the number is with more then 2 numbers after the dot
                    number = roundUp(num) + "";
                }
            }
            City city = new City(str1, country, number, currencyCode);
            cityHashMap.put(str1, city);
        }
    }

    public City findInCities(String name) {
        if (cityHashMap.containsKey(name))
            return cityHashMap.get(name);
        return null;
    }

    private double roundUp(double num) {
        num = Math.round(num * 100);
        num = num / 100;
        return num;
    }

    public String addToCities(String tmp, String ans1, String docName, int locationInDoc, String cityName, String bNoB) {
        if (tmp.charAt(0) != '<' && tmp.charAt(0) != '-' && tmp.charAt(0) != '\'' && tmp.charAt(0) != '[' && tmp.charAt(0) != '(' && !Character.isDigit(tmp.charAt(0))) {
            tmp = tmp.toUpperCase();
            ans1 = ans1.toUpperCase();
            if (cities.containsKey(tmp)) {
                if (!cities.get(tmp).getLocations().containsKey(docName)) {
                    cities.get(tmp).getLocations().put(docName, new ArrayList<String>());
                }
                cities.get(tmp).getLocations().get(docName).add(locationInDoc + bNoB);
                return tmp;
                //(cities.get(tmp).locations.containsKey(docName));
            } else if (!ans1.equals("") && cities.containsKey(ans1)) {
                if (!cities.get(ans1).getLocations().containsKey(docName)) {
                    cities.get(ans1).getLocations().put(docName, new ArrayList<String>());
                }
                cities.get(ans1).getLocations().get(docName).add(locationInDoc + bNoB);
                return ans1;
                //(cities.get(tmp).locations.containsKey(docName));
            } else {
                //ApiJson apiJson = new ApiJson();
                City c = findInCities(tmp);
                if (c != null) {
                    c.getLocations().put(docName, new ArrayList<String>());
                    c.setLocations(docName, locationInDoc + bNoB);
                    cities.put(tmp, c);
                    return tmp;
                } else {
                    if (!ans1.equals("")) {
                        City c2 = findInCities(ans1);
                        if (c2 != null) {
                            c2.getLocations().put(docName, new ArrayList<String>());
                            c2.setLocations(docName, locationInDoc + bNoB);
                            cities.put(ans1, c2);
                            return ans1;

                        } else {
                            City city = new City(tmp, "", "", "");
                            city.getLocations().put(docName, new ArrayList<String>());
                            city.getLocations().get(docName).add(locationInDoc + bNoB);
                            cities.put(tmp, city);
                            return tmp;
                        }
                    }
                }
            }
        }

        return "";
    }

//    public void writeCityToDisk(boolean stemming, String pathPost) {
//        try {
//            int count = 0;
//            String namedir = "";
//            if (stemming) {
//                namedir = "Stem";
//            } else {
//                namedir = "WithoutStem";
//            }
//            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathPost + "\\" + namedir + "\\Cities.txt",true), StandardCharsets.UTF_8));
//            StringBuilder toWrite = new StringBuilder("");
//            for (Map.Entry<String, City> entry : cities.entrySet()) {
//                count++;
//                City value = entry.getValue();
//                String print = value.getCityname() + ";" + value.getCountry() + ";" + value.getCurrency() + ";" + value.getPopulation() + ".";
//                toWrite.append(print + " ");
//                String printadd = "";
//                for (Map.Entry<String, ArrayList<String>> entryLocation : value.getLocations().entrySet()) {
//                    String keyLoc = entryLocation.getKey();
//                    ArrayList<String> ValueList = entryLocation.getValue();
//                    printadd += keyLoc + "; ";
//                    for (String val : ValueList) {
//                        printadd += val + ",";
//                    }
//                }
//                toWrite.append(printadd + "\n");
//                if (count % 100 == 0) {
//                    bw.write(toWrite.toString());
//                    bw.flush();
//                    toWrite = new StringBuilder("");
//                }
//            }
//            bw.write(toWrite.toString());
//            bw.flush();
//            bw.close();
//            System.out.println(cities.size());
//            cities=new HashMap<>();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public void writeCityToDisk(boolean stemming, String pathPost) {
        try {
            int count = 0;
            String namedir = "";
            if (stemming) {
                namedir = "Stem";
            } else {
                namedir = "WithoutStem";
            }
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathPost + "\\" + namedir + "\\Cities.txt", false), StandardCharsets.UTF_8));
            StringBuilder toWrite = new StringBuilder("");
            //HashSet<String> keys = getKeysByValue();
            //Iterator<String> it = keys.iterator();
            for(Map.Entry<String, City> entry:cities.entrySet()) {
                //for (Map.Entry<String, City> entry : cities.entrySet()) {
                //String key = it.next();
                City value = cities.get(entry.getKey());
                count++;
                //City value = entry.getValue();
                String print = value.getCityname() + ";" + value.getCountry() + ";" + value.getCurrency() + ";" + value.getPopulation() + "*";
                toWrite.append(print + " ");
                StringBuilder printadd=new StringBuilder("");
                //String printadd = "";
                for (Map.Entry<String, ArrayList<String>> entryLocation : value.getLocations().entrySet()) {
                    String keyLoc = entryLocation.getKey();
                    ArrayList<String> ValueList = entryLocation.getValue();
                    printadd.append(keyLoc + ";");
                    //printadd += keyLoc + ";";
                    for (String val : ValueList) {
                        printadd.append(val + ",");
                        //printadd += val + ",";
                    }
                    printadd.append(" ");
                    //printadd += " ";
                }
                toWrite.append(printadd + "\n");
                printadd=new StringBuilder("");
                if (count % 10 == 0) {
                    bw.write(toWrite.toString());
                    bw.flush();
                    toWrite = new StringBuilder("");
                }
                //   }
            }
            bw.write(toWrite.toString());
            bw.flush();
            bw.close();
            System.out.println(cities.size());
//            cities = new HashMap<>();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //This function upload the cities to disk
    public void loadCities(boolean stemming, String pathPost) {
        try {
            BufferedReader br = null;
            if (stemming)
                br = new BufferedReader(new InputStreamReader(new FileInputStream(pathPost + "\\Stem\\Cities.txt"), StandardCharsets.UTF_8));
            else
                br = new BufferedReader(new InputStreamReader(new FileInputStream(pathPost + "\\WithoutStem\\Cities.txt"), StandardCharsets.UTF_8));
            String line;
            int count = 0;

            while ((line = br.readLine()) != null) {
                count++;
                ArrayList<String> arrayListLine = mySplit(line, "*");
                ArrayList<String> arrayListStart = mySplit(arrayListLine.get(0), ";");
                String cityname = arrayListStart.get(0);
                String country = "";
                String currency = "";
                String population = "";
                if (arrayListStart.size() > 1) {
                    country = arrayListStart.get(1);
                    currency = arrayListStart.get(2);
                    population = arrayListStart.get(3);
                }
                City cityAdd = new City(cityname, country, population, currency);
                ArrayList<String> alldocs = mySplit(arrayListLine.get(1), " ");
                for (int i = 0; i < alldocs.size(); i++) {
                    ArrayList<String> docAppear = mySplit(alldocs.get(i), ";");
                    String docName = docAppear.get(0);
                    cityAdd.addLocation(docName);
                    ArrayList<String> locations = mySplit(docAppear.get(1), ",");
                    for (int j = 0; j < locations.size(); j++) {
                        cityAdd.setLocations(docName, locations.get(j));
                    }
                }
                cities.put(cityname, cityAdd);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashSet<String> getKeysByValue() {
        HashSet<String> keys = new HashSet<>();
        for (Map.Entry<String, City> entry : cities.entrySet()) {
            keys.add(entry.getKey());
        }
        return keys;
    }

    public static HashMap<String, City> getCities(){
        return cities;
    }

}

