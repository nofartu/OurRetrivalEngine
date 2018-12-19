package sample;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.CheckBox;

public class CityShow {
    private final SimpleStringProperty cityName;
    private CheckBox cb_request;

    public CityShow(String cityName){
        this.cityName=new SimpleStringProperty(cityName);
        this.cb_request=new CheckBox();
        cb_request.setOnAction(event -> {
            chooseCity();
        });
    }

    private void chooseCity() {
    }

}
