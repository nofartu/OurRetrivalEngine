package sample;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.CheckBox;

import java.util.ArrayList;

import static sample.ViewController.addingCitiesFromChoose;

public class CityShow {
    private final SimpleStringProperty cityName;
    private CheckBox cb_request;

    public CityShow(String cityName){

        this.cityName=new SimpleStringProperty(cityName);
        this.cb_request=new CheckBox();
        cb_request.setOnAction(event -> {
            addingCitiesFromChoose(cityName);
        });
    }

    private void chooseCity() {

    }

    public String getCityName() {
        return cityName.get();
    }

    public SimpleStringProperty cityNameProperty() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName.set(cityName);
    }

    public CheckBox getCb_request() {
        return cb_request;
    }

    public void setCb_request(CheckBox cb_request) {
        this.cb_request = cb_request;
    }



}
