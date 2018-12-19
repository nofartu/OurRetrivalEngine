package sample;

import java.util.ArrayList;
import java.util.HashMap;

public class City {
    private String cityname;
    private String country;
    private String population;
    private String currency;
    private HashMap<String, ArrayList<String>> locations; // <name of doc, array of locations in the doc>;

    public City(String cityname, String country, String population, String currency) {
        this.cityname = cityname;
        this.country = country;
        this.population = population;
        this.currency = currency;
        locations = new HashMap<>();
    }

    public String getCityname() {
        return cityname;
    }

    public void setCityname(String cityname) {
        this.cityname = cityname;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPopulation() {
        return population;
    }

    public void setPopulation(String population) {
        this.population = population;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public HashMap<String, ArrayList<String>> getLocations() {
        return locations;
    }

    public void setLocations(String doc, String locationInDoc) {
        locations.get(doc).add(locationInDoc);
    }

    public void addLocation(String doc){
        locations.put(doc,new ArrayList<>());
    }

    @Override
    public int hashCode() {
        return City.class.hashCode();
    }
}
