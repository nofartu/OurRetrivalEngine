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

/**
 * Class that support the show of the results of docs and queries.
 */
public class DocShow {
    private final SimpleStringProperty queryNum;
    private final SimpleStringProperty docName;
    private String docNameS;
    private Button btn_Entities;
    public TableView tableQuery;

    public DocShow(String docName, String queryNum) {
        docNameS = docName;
        this.docName = new SimpleStringProperty(docName);
        this.queryNum = new SimpleStringProperty(queryNum);
        this.btn_Entities = new Button("Show entities");
        btn_Entities.setOnAction(event -> {
            btn_Entities.setDisable(true);
            showEntities();
            btn_Entities.setDisable(false);
        });
    }

    /**
     * show the entities of each doc
     */
    private void showEntities() {
        try {
            tableQuery = new TableView();
            Stage stage = new Stage();
            Scene scene = new Scene(new Group());
            stage.setTitle("Entities");
            stage.setWidth(350);
            stage.setHeight(300);
            final Label label = new Label("Entities in this document:");
            label.setFont(new Font("Calibri Light", 20));
            tableQuery.setEditable(false);

            TableColumn entity = new TableColumn("Entities");
            entity.setMinWidth(150);
            entity.setCellValueFactory(new PropertyValueFactory<EntityShow, String>("entityName"));

            TableColumn rank = new TableColumn("Rank");
            rank.setMinWidth(150);
            rank.setCellValueFactory(new PropertyValueFactory<EntityShow, String>("entityRate"));

            //table.setItems(getData());
            Documents doc = docsCoprus.get(docNameS);
            HashMap<String, Double> entities = doc.getEntities();
            if (entities.size() != 0) {
                tableQuery.setItems(getData(entities));
                tableQuery.getColumns().addAll(entity, rank);
                tableQuery.setMinHeight(150);
                tableQuery.setMaxHeight(150);
                tableQuery.setMinWidth(320);
                tableQuery.setMaxWidth(320);

                final VBox vbox = new VBox();
                vbox.setSpacing(10);
                vbox.setPadding(new Insets(10, 0, 0, 10));
                vbox.getChildren().addAll(label, tableQuery);

                ((Group) scene.getRoot()).getChildren().addAll(vbox);
                stage.setScene(scene);
                stage.show();
            } else {
                showAlert("No entities", "Attention!", "There is no entities in this document");
            }
        } catch (Exception e) {
            System.out.println("not opening");
        }
    }

    private ObservableList<EntityShow> getData(HashMap<String, Double> entities) {
        TreeMap<String, Double> sorted = new TreeMap<>(new ValueComparator(entities));
        sorted.putAll(entities);
        ObservableList<EntityShow> data = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> entry : sorted.entrySet()) {
            data.add(new EntityShow(entry.getKey(), entry.getValue().toString()));
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

        TreeMap<String, Double> map = new TreeMap<>();

        public ValueComparator(HashMap<String, Double> map) {
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
