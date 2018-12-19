package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Group;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.*;

import static sample.ApiJson.cities;
import static sample.Indexer.sort;


public class ViewController implements Observer {

    @FXML
    public javafx.scene.control.Button btn_startOperation;
    public javafx.scene.control.Button btn_resetAndDelete;
    public javafx.scene.control.Button btn_browseCorpus;
    public javafx.scene.control.Button btn_browseStop;
    public javafx.scene.control.Button btn_browseDir;
    public javafx.scene.control.Button btn_browseQuery;
    public javafx.scene.control.Button btn_showDictionary;
    public javafx.scene.control.Button btn_chooseCity;

    @FXML
    public javafx.scene.control.TextField txtfld_corpus;
    public javafx.scene.control.TextField txtfld_stopWords;
    public javafx.scene.control.TextField txtfld_dirPath;
    public javafx.scene.control.TextField txtfld_Query;
    public javafx.scene.control.TextArea txt_Info;
    public javafx.scene.control.CheckBox check_stemm;
    public javafx.scene.control.CheckBox cb_choseCity;
    public javafx.scene.control.ComboBox comboBox;

    private Stage primaryStage;
    private TableView table;
    private TableView tableCity;
    private String info;
    private ReadFile readFile;
    public Thread t;
    private static ArrayList<String> chosenCities = new ArrayList<>();

    public void setStage(Stage stage) {
        this.primaryStage = stage;
        //controller = new Controller(model);
        txtfld_corpus.setText("D:\\documents\\users\\nofartu\\Downloads\\corpus\\corpus");
        txtfld_stopWords.setText("C:\\Users\\nofartu\\IdeaProjects\\OurRetrivalEngine");
        txtfld_dirPath.setText("D:\\documents\\users\\nofartu\\Downloads\\post");

        comboBox.setDisable(true);
        txt_Info.setDisable(true);
        txt_Info.setVisible(false);
        btn_showDictionary.setDisable(true);
        btn_chooseCity.setDisable(true);

    }

    public void startOperation() {
        String corpus = txtfld_corpus.getText();
        String stopWords = txtfld_stopWords.getText();
        String dirPath = txtfld_dirPath.getText();
        boolean stemm = check_stemm.isSelected();
        showAlert("Started!", "The process started", "You can't press ok until the program finished");

        if (txtfld_corpus.getText().isEmpty() || txtfld_stopWords.getText().isEmpty() || txtfld_dirPath.getText().isEmpty()) {
            showAlert("Warning", "path can not be null", "please select a path using the browse button and press Start creating Dictionary again");
        } else {
            //controller.startOperation(txtfld_corpus.getText(),txtfld_stopWords.getText(),txtfld_dirPath.getText(),stemm);
            long start = System.currentTimeMillis();
            try {

                readFile = new ReadFile(corpus, stopWords, dirPath, stemm);
                t = new Thread(() -> {
                    info = readFile.reading();
                });
                try {
                    t.start();
                    t.join();
                } catch (InterruptedException e1) {
                    System.out.println("Problem thread");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            long end = System.currentTimeMillis();
            comboBox.setDisable(false);
            setData();
            long time = (end - start) / 1000;
            String finalInfo = info + "Finish time: " + time + "s \n";
            showAlert("Finished!", "The process ended successfully", "Information appears on the right side below");
            showResults(finalInfo);
            //do the func
        }
        btn_showDictionary.setDisable(false);
    }

    private void showResults(String finalInfo) {
        txt_Info.setDisable(false);
        txt_Info.setVisible(true);
        txt_Info.setText("Information:\n" + finalInfo);
    }

    public void reset() {
        String path = txtfld_dirPath.getText();
        try {
            String paths = path;
            if (check_stemm.isSelected()) {
                paths += "\\Stem";
            } else {
                paths += "\\WithoutStem";
            }
            File currentFile = new File(paths + "\\cities.txt");
            currentFile.delete();
            File currentFile1 = new File(paths + "\\dictionaryStem.txt");
            currentFile1.delete();
            File currentFile2 = new File(paths + "\\dictionaryNoStem.txt");
            currentFile2.delete();
            File currentFile3 = new File(paths + "\\postFileWithStem.txt");
            currentFile3.delete();
            File currentFile4 = new File(paths + "\\postFileWithoutStem.txt");
            currentFile4.delete();
            File currentFile5 = new File(paths + "\\Documents.txt");
            currentFile5.delete();
            System.gc();

        } catch (Exception e) {
            e.printStackTrace();
        }

        txtfld_corpus.setText("");
        txtfld_stopWords.setText("");
        txtfld_dirPath.setText("");
        txt_Info.setText(" ");
        btn_showDictionary.setDisable(true);
        showAlert("Alert", "Reset completed", "Press ok to continue");
    }


    public void showDictionary() {
        try {
            table = new TableView();
            Stage stage = new Stage();
            Scene scene = new Scene(new Group());
            stage.setTitle("Dictionary");
            stage.setWidth(440);
            stage.setHeight(940);
            final Label label = new Label("Dictionary:");
            label.setFont(new Font("Calibri Light", 22));
            table.setEditable(false);

            TableColumn termName = new TableColumn("Term");
            termName.setMinWidth(200);
            termName.setCellValueFactory(new PropertyValueFactory<TermShow, String>("termName"));

            TableColumn value = new TableColumn("Number of Occurrence");
            value.setMinWidth(200);
            value.setCellValueFactory(new PropertyValueFactory<TermShow, String>("value"));

            //table.setItems(getData());
            table.setItems(getData());
            table.getColumns().addAll(termName, value);
            table.setMinHeight(800);

            final VBox vbox = new VBox();
            vbox.setSpacing(20);
            vbox.setPadding(new Insets(10, 0, 0, 10));
            vbox.getChildren().addAll(label, table);

            ((Group) scene.getRoot()).getChildren().addAll(vbox);
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            //System.out.println("not opening");
        }
    }


    public ObservableList<TermShow> getData() {
        ObservableList<TermShow> data = FXCollections.observableArrayList();
        TreeMap<String, Integer[]> dictionary = sort(); //check not calling before the stating
        for (Map.Entry<String, Integer[]> entry : dictionary.entrySet()) {
            String term = entry.getKey();
            int value = entry.getValue()[1];
            data.add(new TermShow(term, value));
        }
        return data;
    }


    public void showCities() {
        try {

            tableCity = new TableView();
            Stage stage = new Stage();
            Scene scene = new Scene(new Group());
            stage.setTitle("Cities");
            stage.setWidth(450);
            stage.setHeight(900);
            final Label label = new Label("Choose Cities:");
            label.setFont(new Font("Calibri Light", 22));
            tableCity.setEditable(false);

            TableColumn city = new TableColumn("City");
            city.setMinWidth(200);
            city.setCellValueFactory(new PropertyValueFactory<CityShow, String>("cityName"));


            TableColumn checkbox = new TableColumn("Choose city");
            checkbox.setMinWidth(100);
            checkbox.setCellValueFactory(new PropertyValueFactory<CityShow, CheckBox>("cb_request"));


            //table.setItems(getData());
            tableCity.setItems(getDataCities());
            tableCity.getColumns().addAll(city, checkbox);
            tableCity.setMinHeight(800);
            tableCity.setMaxHeight(900);
            tableCity.setMinWidth(350);
            tableCity.setMaxWidth(450);

            final VBox vbox = new VBox();
            vbox.setSpacing(20);
            vbox.setPadding(new Insets(10, 0, 0, 10));
            vbox.getChildren().addAll(label, tableCity);

            ((Group) scene.getRoot()).getChildren().addAll(vbox);
            stage.setScene(scene);
            stage.show();


        } catch (Exception e) {
            System.out.println("not opening");
        }
    }

    public ObservableList<CityShow> getDataCities() {
        ObservableList<CityShow> data = FXCollections.observableArrayList();
        Map<String, City> set = cities;
        for (Map.Entry<String, City> entry : set.entrySet()) {
            data.add(new CityShow(entry.getKey()));
        }
        return data;
    }

//    private void SetStageCloseEvent(Stage primaryStage) {
//        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
//            public void handle(WindowEvent windowEvent) {
//                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
//                alert.setTitle("Exit");
//                alert.setHeaderText("Exit confirmation");
//                alert.setContentText("Are you sure you want to quit?");
//                Optional<ButtonType> result = alert.showAndWait();
//                if (result.get() == ButtonType.OK) {
//                    //model.stopServers();
//                    // ... user chose OK
//                    // Close program
//                } else {
//                    // ... user chose CANCEL or closed the dialog
//                    windowEvent.consume();
//                }
//                for(int i=0;i<chosenCities.size();i++){
//                    System.out.println(chosenCities.get(i));
//                }
//            }
//        });
//    }

    public void loadDictToMemory() {
        String path = txtfld_dirPath.getText();
        boolean isStem = check_stemm.isSelected();
        if (!isStem) {
            if (!new File(path + "\\WithoutStem\\dictionaryNoStem.txt").exists())
                showAlert("Bad request", "There is no file to load", "Check it again");
            else {
                boolean b = check_stemm.isSelected();
                Indexer indexer = new Indexer(b, path);
                indexer.addToDict();
                indexer.loadDocuments();
                ApiJson api = new ApiJson();
                api.loadCities(b, path);
                btn_showDictionary.setDisable(false);
                showAlert("Alert", "Dictionary uploaded", "Press ok to continue");
            }
        } else if (isStem) {
            if (!new File(path + "\\Stem\\dictionaryStem.txt").exists())
                showAlert("Bad request", "There is no file to load", "Check it again");
            else {
                boolean b = check_stemm.isSelected();
                Indexer indexer = new Indexer(b, path);
                indexer.addToDict();
                indexer.loadDocuments();
                ApiJson api = new ApiJson();
                api.loadCities(b, path);
                btn_showDictionary.setDisable(false);
                showAlert("Alert", "Dictionary uploaded", "Press ok to continue");
            }
        }

    }

    public static void addingCitiesFromChoose(String cityname) {
        chosenCities.add(cityname);
    }

    @Override
    public void update(Observable o, Object arg) {

    }

    public void BrowseCorpus(ActionEvent actionEvent) {
        btn_browseCorpus.setDisable(true);
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File path = directoryChooser.showDialog(null);
        if (path != null) {
            txtfld_corpus.setText(path.getAbsolutePath());
        }
        btn_browseCorpus.setDisable(false);
    }

    public void BrowseStop(ActionEvent actionEvent) {
        btn_browseStop.setDisable(true);
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File path = directoryChooser.showDialog(null);
        if (path != null) {
            txtfld_stopWords.setText(path.getAbsolutePath());
        }
        btn_browseStop.setDisable(false);
    }

    public void BrowseDirPath(ActionEvent actionEvent) {
        btn_browseDir.setDisable(true);
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File path = directoryChooser.showDialog(null);
        if (path != null) {
            txtfld_dirPath.setText(path.getAbsolutePath());
        }
        btn_browseDir.setDisable(false);
    }

    public void BrowseQuery(ActionEvent actionEvent) {
        btn_browseQuery.setDisable(true);
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File path = directoryChooser.showDialog(null);
        if (path != null) {
            txtfld_Query.setText(path.getAbsolutePath());
        }
        btn_browseQuery.setDisable(false);
    }

    private void showAlert(String title, String header, String alertMessage) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(alertMessage);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.show();
    }

    public void setData() {

        HashSet<String> languages = readFile.getLanguages();

        comboBox.getItems().addAll(languages
        );

    }

    public void checkCities() {
        if (cb_choseCity.isSelected())
            btn_chooseCity.setDisable(false);
        else
            btn_chooseCity.setDisable(true);
    }

    public String getTheQuery(String path) {
        File file = new File(path);
        if (file.isFile()) {
            try {
                Document doc = Jsoup.parse(file, "UTF-8");
                Elements docs = doc.select("top");
                for (Element e : docs) {
                    String query = e.getElementsByTag("title").text();
                    return query;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public void doIt() {
        Searcher searcher = new Searcher(null, "C:\\Users\\nofartu\\IdeaProjects\\OurRetrivalEngine", "D:\\documents\\users\\nofartu\\Downloads\\post", false, new ApiJson());
        searcher.parseTheQuery("human smuggling");
        searcher.createCountWordsQuery("human smuggling");
        searcher.createDocsContainsQuery();
        searcher.sendToRanker();
    }

}
