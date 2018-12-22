package sample;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;

import static sample.ViewController.addingCitiesFromChoose;

public class DocShow {
    private final SimpleStringProperty queryNum;
    private final SimpleStringProperty docName;
    private Button btn_Entities;

    public DocShow(String cityName, String queryNum) {
        this.docName = new SimpleStringProperty(cityName);
        this.queryNum = new SimpleStringProperty(queryNum);
        this.btn_Entities = new Button("Show entities");
        btn_Entities.setOnAction(event -> {

        });
    }

    public String getQueryNum() {
        return queryNum.get();
    }

    public SimpleStringProperty queryNumProperty() {
        return queryNum;
    }

    public void setQueryNum(String queryNum) {
        this.queryNum.set(queryNum);
    }

    public String getDocName() {
        return docName.get();
    }

    public SimpleStringProperty docNameProperty() {
        return docName;
    }

    public void setDocName(String docName) {
        this.docName.set(docName);
    }

    public Button getBtn_Entities() {
        return btn_Entities;
    }

    public void setBtn_Entities(Button btn_Entities) {
        this.btn_Entities = btn_Entities;
    }
}
