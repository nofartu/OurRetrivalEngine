package sample;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static sample.Indexer.docsCoprus;
import static sample.ViewController.addingCitiesFromChoose;

public class DocShow {
    private final SimpleStringProperty queryNum;
    private final SimpleStringProperty docName;
    private String docNameS;
    private Button btn_Entities;
    public TableView tableQuery;

    public DocShow(String docName, String queryNum) {
        docNameS=docName;
        this.docName = new SimpleStringProperty(docName);
        this.queryNum = new SimpleStringProperty(queryNum);
        this.btn_Entities = new Button("Show entities");
        btn_Entities.setOnAction(event -> {
            showEntities();
        });
    }

    private void showEntities() {
        try {
            tableQuery = new TableView();
            Stage stage = new Stage();
            Scene scene = new Scene(new Group());
            stage.setTitle("Entities");
            stage.setWidth(260);
            stage.setHeight(300);
            final Label label = new Label("Entities in this document:");
            label.setFont(new Font("Calibri Light", 22));
            tableQuery.setEditable(false);

            TableColumn entity = new TableColumn("Entities");
            entity.setMinWidth(150);
            entity.setCellValueFactory(new PropertyValueFactory<EntityShow, String>("entityName"));


            //table.setItems(getData());
            Documents doc=docsCoprus.get(docNameS);
            HashMap<String, Integer> entities = doc.getEntities();
            if( entities.size()!=0) {
                tableQuery.setItems(getData(entities));
                tableQuery.getColumns().addAll(entity);
                tableQuery.setMinHeight(150);
                tableQuery.setMaxHeight(150);
                tableQuery.setMinWidth(150);
                tableQuery.setMaxWidth(200);

                final VBox vbox = new VBox();
                vbox.setSpacing(20);
                vbox.setPadding(new Insets(10, 0, 0, 10));
                vbox.getChildren().addAll(label, tableQuery);

                ((Group) scene.getRoot()).getChildren().addAll(vbox);
                stage.setScene(scene);
                stage.show();
            }else{
                showAlert("No entities", "Attention!","There is no entities in this document");
            }

        } catch (Exception e) {
            System.out.println("not opening");
        }
    }

    private ObservableList<EntityShow> getData(HashMap<String, Integer> entities) {
        TreeMap<String, Integer> sorted=new TreeMap<>(new ValueComparator(entities));
        sorted.putAll(entities);
        ObservableList<EntityShow> data = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
            data.add(new EntityShow(entry.getKey()));
        }
        return data;
    }
    private void showAlert(String title, String header, String alertMessage) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(alertMessage);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.show();
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

    class ValueComparator implements Comparator<String> {

        TreeMap<String, Integer> map = new TreeMap<>();

        public ValueComparator(HashMap<String, Integer> map) {
            this.map.putAll(map);
        }

        @Override
        public int compare(String s1, String s2) {
            if (map.get(s1) >= map.get(s2)) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
