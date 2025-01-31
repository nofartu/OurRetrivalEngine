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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static sample.ApiJson.cities;
import static sample.Indexer.sort;
import static sample.ReadFile.getLanguages;
import static sample.ReadFile.loadLanguages;
import static sample.ReadFile.mySplit;


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
    public javafx.scene.control.Button btn_loadDictionary;
    public javafx.scene.control.Button btn_runQuery;

    @FXML
    public javafx.scene.control.TextField txtfld_corpus;
    public javafx.scene.control.TextField txtfld_stopWords;
    public javafx.scene.control.TextField txtfld_dirPath;
    public javafx.scene.control.TextField txtfld_QueryHand;
    public javafx.scene.control.TextField txtfld_QueryBrowse;
    public javafx.scene.control.TextArea txt_Cities;
    public javafx.scene.control.TextArea txt_Info;
    public javafx.scene.control.CheckBox check_stemm;
    public javafx.scene.control.CheckBox cb_choseCity;
    public javafx.scene.control.CheckBox cb_semantic;
    public javafx.scene.control.CheckBox cb_handQuery;
    public javafx.scene.control.CheckBox cb_fileQuery;
    public javafx.scene.control.ComboBox comboBox;

    private Stage primaryStage;
    private TableView table;
    private TableView tableCity;
    private TableView tableQuery;
    private String info;
    private ReadFile readFile;
    private ApiJson api;
    public Thread t;
    public static ArrayList<String> chosenCities = new ArrayList<>();
    private Queue<Pair<String, TreeMap<String, Double>>> docsToShow;


    public void setStage(Stage stage) {
        this.primaryStage = stage;
        comboBox.setDisable(true);
        txt_Info.setDisable(true);
        txt_Info.setVisible(false);
        txt_Cities.setDisable(true);
        txt_Cities.setVisible(false);
        btn_showDictionary.setDisable(true);
        btn_chooseCity.setDisable(true);
        btn_browseQuery.setDisable(true);
        btn_runQuery.setDisable(true);
        txtfld_QueryHand.setDisable(true);
        txtfld_QueryBrowse.setDisable(true);
        cb_handQuery.setDisable(true);
        cb_fileQuery.setDisable(true);
        cb_semantic.setDisable(true);
        cb_choseCity.setDisable(true);
    }


    /**
     * Create the indexer
     */
    public void startOperation() {
        String corpus = txtfld_corpus.getText();
        String stopWords = txtfld_stopWords.getText();
        String dirPath = txtfld_dirPath.getText();
        boolean stemm = check_stemm.isSelected();
        showAlert("Started!", "The process started", "You can't press ok until the program finished");

        if (txtfld_corpus.getText().isEmpty() || txtfld_stopWords.getText().isEmpty() || txtfld_dirPath.getText().isEmpty()) {
            showAlert("Warning", "path can not be null", "please select a path using the browse button and press Start creating Dictionary again");
        } else {
            long start = System.currentTimeMillis();
            try {
                readFile = new ReadFile(corpus, stopWords, dirPath, stemm);
                t = new Thread(() -> {
                    info = readFile.reading();
                });
                controlAll(true);
                try {
                    t.start();
                    t.join();
                    controlAll(false);
                } catch (InterruptedException e1) {
                    System.out.println("Problem thread");
                }
            } catch (Exception e) {
                System.out.println("problem with read file");
            }
            long end = System.currentTimeMillis();
            comboBox.setDisable(false);
            setData();
            long time = (end - start) / 1000;
            String finalInfo = info + "Finish time: " + time + "s \n";
            showAlert("Finished!", "The process ended successfully", "Information appears on the right side below");
            showResults(finalInfo);
        }
        btn_showDictionary.setDisable(false);
        cb_handQuery.setDisable(false);
        cb_fileQuery.setDisable(false);
        cb_semantic.setDisable(false);
        cb_choseCity.setDisable(false);
    }

    private void controlAll(boolean what) {
        btn_browseCorpus.setDisable(what);
        btn_browseDir.setDisable(what);
        btn_browseStop.setDisable(what);
        btn_resetAndDelete.setDisable(what);
        check_stemm.setDisable(what);
        btn_loadDictionary.setDisable(what);

    }

    private void showResults(String finalInfo) {
        txt_Info.setDisable(false);
        txt_Info.setVisible(true);
        txt_Info.setText("Information:\n" + finalInfo);
    }

    /**
     * Resets the file that we save
     */
    public void reset() {
        String path = txtfld_dirPath.getText();
        try {
            String paths = path;
            if (check_stemm.isSelected()) {
                paths += "\\Stem";
            } else {
                paths += "\\WithoutStem";
            }
            File directory = new File(paths);
            File currentFile = new File(paths + "\\Cities.txt");
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
            File currentFile6 = new File(paths + "\\Languages.txt");
            currentFile6.delete();
            if (directory.exists())
                directory.delete();
            System.gc();
            if (!cb_fileQuery.isDisabled() || !cb_handQuery.isDisabled()) {
                cb_handQuery.setDisable(true);
                cb_fileQuery.setDisable(true);
                cb_handQuery.setSelected(false);
                cb_fileQuery.setSelected(false);
                txtfld_QueryBrowse.setDisable(true);
                txtfld_QueryHand.setDisable(true);
                btn_runQuery.setDisable(true);
                cb_choseCity.setDisable(true);
                cb_choseCity.setSelected(false);
                cb_semantic.setDisable(true);
                cb_semantic.setSelected(false);
                btn_browseQuery.setDisable(true);
                btn_chooseCity.setDisable(true);
                comboBox.setDisable(true);
                comboBox.setValue("");

            }
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


    /**
     * shows the dictionary
     */
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
            //System.out.println();
        }
    }

    private ObservableList<TermShow> getData() {
        ObservableList<TermShow> data = FXCollections.observableArrayList();
        TreeMap<String, Integer[]> dictionary = sort(); //check not calling before the stating
        for (Map.Entry<String, Integer[]> entry : dictionary.entrySet()) {
            String term = entry.getKey();
            int value = entry.getValue()[1];
            data.add(new TermShow(term, value));
        }
        return data;
    }

    //show the cities for the user to choose city if he want
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
            SetStageCloseEvent(stage);
            stage.show();

        } catch (Exception e) {
            System.out.println("not opening");
        }
    }

    //writes the chosen city
    private String setTxt() {
        String s = "Chosen cities: \n";
        for (String city : chosenCities)
            s = s + city + "\n";
        s += "\n";
        return s;
    }

    private ObservableList<CityShow> getDataCities() {
        ObservableList<CityShow> data = FXCollections.observableArrayList();
        TreeMap<String, City> sets = new TreeMap<>();
        sets.putAll(cities);
        for (Map.Entry<String, City> entry : sets.entrySet()) {
            data.add(new CityShow(entry.getKey()));
        }
        return data;
    }

    //shows the results of the query
    private void showDocsQueries() {
        try {
            tableQuery = new TableView();
            Stage stage = new Stage();
            Scene scene = new Scene(new Group());
            stage.setTitle("Relevant documents");
            stage.setWidth(500);
            stage.setHeight(950);
            final Label label = new Label("Relevant documents:");
            label.setFont(new Font("Calibri Light", 22));
            final Button btn_Save = new Button("Save");
            btn_Save.setOnAction(event -> {
                btn_Save.setDisable(true);
                writeQueryToDisk();
                btn_Save.setDisable(false);
            });
            btn_Save.setMinWidth(350);
            tableQuery.setEditable(false);

            TableColumn query = new TableColumn("Query number");
            query.setMinWidth(100);
            query.setCellValueFactory(new PropertyValueFactory<DocShow, String>("queryNum"));

            TableColumn Docs = new TableColumn("Document number");
            Docs.setMinWidth(130);
            Docs.setCellValueFactory(new PropertyValueFactory<DocShow, String>("docName"));

            TableColumn button = new TableColumn("Entities");
            button.setMinWidth(100);
            button.setCellValueFactory(new PropertyValueFactory<DocShow, Button>("btn_Entities"));


            tableQuery.setItems(getDataQueries());
            tableQuery.getColumns().addAll(query, Docs, button);
            tableQuery.setMinHeight(800);
            tableQuery.setMaxHeight(900);
            tableQuery.setMinWidth(350);
            tableQuery.setMaxWidth(450);

            final VBox vbox = new VBox();
            vbox.setSpacing(20);
            vbox.setPadding(new Insets(10, 0, 0, 10));
            vbox.getChildren().addAll(label, btn_Save, tableQuery);

            ((Group) scene.getRoot()).getChildren().addAll(vbox);
            stage.setScene(scene);
            SetStageCloseEventAfterQuery(stage);
            stage.show();


        } catch (Exception e) {
            System.out.println("not opening");
        }
    }

    private ObservableList<DocShow> getDataQueries() {
        ObservableList<DocShow> data = FXCollections.observableArrayList();
        Queue<Pair<String, TreeMap<String, Double>>> set = new LinkedList<>();
        set.addAll(docsToShow);
        while (set.size() > 0) {
            Pair<String, TreeMap<String, Double>> it = set.poll();
            for (Map.Entry<String, Double> entry : it.getValue().entrySet()) {
                data.add(new DocShow(entry.getKey(), it.getKey()));
            }
        }
        return data;
    }

    /**
     * Inform the user that his cities are saved.
     * updates the text filed of the chosen city.
     *
     * @param primaryStage
     */
    private void SetStageCloseEvent(Stage primaryStage) {
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent windowEvent) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Saved");
                alert.setHeaderText("Your cities are saved!");
                alert.setContentText("Are you sure you done?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {
                    //model.stopServers();
                    // ... user chose OK
                    // Close program
                } else {
                    // ... user chose CANCEL or closed the dialog
                    windowEvent.consume();
                }
                txt_Cities.setText(setTxt());
                txt_Cities.setDisable(false);
                txt_Cities.setVisible(true);
            }
        });
    }

    /**
     * wait for the users confirmation of exiting the show of the results.
     * Reset the chosen city after the this iteration.
     *
     * @param primaryStage
     */
    private void SetStageCloseEventAfterQuery(Stage primaryStage) {
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent windowEvent) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Exit");
                alert.setHeaderText("Exiting");
                alert.setContentText("Are you sure you done?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {
                    //model.stopServers();
                    // ... user chose OK
                    // Close program
                } else {
                    // ... user chose CANCEL or closed the dialog
                    windowEvent.consume();
                }
                txt_Cities.setText("");
                chosenCities = new ArrayList<>();
            }
        });
    }

    /**
     * Loads the dictionary (all the indexes that we created and need for this query) to the memory
     */
    public void loadDictToMemory() {
        String path = txtfld_dirPath.getText();
        boolean isStem = check_stemm.isSelected();
        if (!isStem) {
            if (!new File(path + "\\WithoutStem\\dictionaryNoStem.txt").exists())
                showAlert("Bad request", "There is no file to load", "Check it again");
            else {
                uploading(path);
            }
        } else if (isStem) {
            if (!new File(path + "\\Stem\\dictionaryStem.txt").exists())
                showAlert("Bad request", "There is no file to load", "Check it again");
            else {
                uploading(path);
            }
        }
    }

    /**
     * help function for the load dictionaries
     *
     * @param path
     */
    private void uploading(String path) {
        boolean b = check_stemm.isSelected();
        Indexer indexer = new Indexer(b, path);
        indexer.addToDict();
        indexer.loadDocuments();
        api = new ApiJson();
        api.loadCities(b, path);
        loadLanguages(b, path);
        btn_showDictionary.setDisable(false);
        cb_handQuery.setDisable(false);
        cb_fileQuery.setDisable(false);
        cb_semantic.setDisable(false);
        cb_choseCity.setDisable(false);
        comboBox.setDisable(false);
        comboBox.setValue("");
        setData();
        showAlert("Alert", "Dictionary uploaded", "Press ok to continue");
    }

    /**
     * add the chosen city to the chosen cities (array list)
     *
     * @param cityname
     */
    public static void addingCitiesFromChoose(String cityname) {
        chosenCities.add(cityname);
    }

    /**
     * for case that the user chose a city and regret it. remove the city from the list
     *
     * @param cityname
     */
    public static void removingCitiesFromChoose(String cityname) {
        if (chosenCities.contains(cityname))
            chosenCities.remove(cityname);
    }

    @Override
    public void update(Observable o, Object arg) {

    }

    /**
     * Handle the situation when user loaded a dictionary (let's say without stem) and then check the stem check box (let's say now it is selected).
     * He will not be able to run the query without loading the dictionary again.
     */
    public void cb_StemOnOrOff() {
        if (!cb_handQuery.isDisabled() || !cb_fileQuery.isDisabled()) {
            showAlert("Attention", "You changed the Stem/No stem", "You must upload the dictionary again to run the query.");
            cb_handQuery.setDisable(true);
            cb_fileQuery.setDisable(true);
            cb_handQuery.setSelected(false);
            cb_fileQuery.setSelected(false);
            txtfld_QueryBrowse.setDisable(true);
            txtfld_QueryHand.setDisable(true);
            btn_runQuery.setDisable(true);
            cb_choseCity.setDisable(true);
            cb_choseCity.setSelected(false);
            cb_semantic.setDisable(true);
            cb_semantic.setSelected(false);
            btn_browseQuery.setDisable(true);
            btn_chooseCity.setDisable(true);
            txt_Cities.setText("");
            chosenCities = new ArrayList<>();
            txt_Cities.setDisable(true);
            txt_Cities.setVisible(false);
            comboBox.setValue("");
        }
    }

    /**
     * The following two function are to not let the user to run both hand query and browse query. he can choose one of them
     * and if he regerts he can uncheck the selected check box and select the other one.
     */
    public void cb_HandOnOrOff() {
        if (!cb_fileQuery.isSelected()) {
            if (cb_handQuery.isSelected()) {
                txtfld_QueryHand.setDisable(false);
                btn_runQuery.setDisable(false);
            } else if (!cb_handQuery.isSelected()) {
                txtfld_QueryHand.setDisable(true);
                btn_runQuery.setDisable(true);
            }
        } else {
            if (cb_handQuery.isSelected()) {
                cb_handQuery.setSelected(false);
                showAlert("Wrong move", "You can only chose one of the option!", "If you wnat to change your selection, uncheck the other checkbox and then check this checkbox");
            }
        }
    }

    public void cb_BrowseOnOrOff() {
        if (!cb_handQuery.isSelected()) {
            if (cb_fileQuery.isSelected()) {
                txtfld_QueryBrowse.setDisable(false);
                btn_browseQuery.setDisable(false);
                btn_runQuery.setDisable(false);
            } else if (!cb_fileQuery.isSelected()) {
                txtfld_QueryBrowse.setDisable(true);
                btn_browseQuery.setDisable(true);
                btn_runQuery.setDisable(true);
            }
        } else {
            if (cb_fileQuery.isSelected()) {
                cb_fileQuery.setSelected(false);
                showAlert("Wrong move", "You can only chose one of the option!", "If you wnat to change your selection, uncheck the other checkbox and then check this checkbox");
            }
        }
    }

    /**
     * The following 4 function are the browse function for the corpus, stop words, saving directory and query browsing.
     */
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
        FileChooser fileChooser = new FileChooser();
        File path = fileChooser.showOpenDialog(null);
        if (path != null) {
            txtfld_QueryBrowse.setText(path.getAbsolutePath());
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

    //sets the Languages
    private void setData() {
        HashSet<String> languages = getLanguages();
        comboBox.getItems().addAll(languages);
    }

    //checks if city is chosen. if soo it will un disable the button to chose the city
    public void checkCities() {
        if (cb_choseCity.isSelected())
            btn_chooseCity.setDisable(false);
        else
            btn_chooseCity.setDisable(true);
    }

    /**
     * parse the file of query
     *
     * @param path
     * @return
     */
    private LinkedHashMap<String, String[]> getTheQuery(String path) {
        LinkedHashMap<String, String[]> queries = new LinkedHashMap<>();
        File file = new File(path);
        if (file.isFile()) {
            try {
                Document doc = Jsoup.parse(file, "UTF-8");
                Elements docs = doc.select("top");
                for (Element e : docs) {
                    String query = e.getElementsByTag("title").text();
                    String desc = e.getElementsByTag("desc").text();
                    String numQuery = e.getElementsByTag("num").text();
                    if (!numQuery.equals("")) {
                        ArrayList<String> split = mySplit(numQuery, " ");
                        numQuery = split.get(1);
                    }
                    String[] fromTags = {numQuery, desc}; //0 - num query, 1 - desc
                    queries.put(query, fromTags);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return queries;
    }

    /**
     * operates the moment when the use clicks the "RUN" button.
     */
    public void runQuery() {
        btn_runQuery.setDisable(true);
        showAlert("Runing Query/Queries started!", "Don't touch anything", "");
        docsToShow = new LinkedList<>();
        String stopWords = System.getProperty("user.dir");
        //String stopWords = txtfld_stopWords.getText();
        String dirPath = txtfld_dirPath.getText();
        boolean stem = check_stemm.isSelected();
        boolean semantic = cb_semantic.isSelected();
        if (cb_handQuery.isSelected()) { //for hand query (One query)
            if (!txtfld_QueryHand.getText().equals("")) {
                Searcher searcher = new Searcher(stopWords, dirPath, stem, api, semantic, "", cb_choseCity.isSelected());
                String query = txtfld_QueryHand.getText();
                searcher.doQuery(query);
                collectedDocs(searcher.getDocs(), "100");
                if (searcher.getDocs().size() == 0) {
                    showAlert("Attention!", "There is no documents for your query", "Please try another query.");
                    txt_Cities.setText("");
                    chosenCities = new ArrayList<>();
                }
                else
                    showDocsQueries(); //show the results
            }
        } else if (cb_fileQuery.isSelected()) { //for list of queries
            if (!txtfld_QueryBrowse.getText().equals("")) {
                LinkedHashMap<String, String[]> queries = getTheQuery(txtfld_QueryBrowse.getText());
                for (Map.Entry<String, String[]> entry : queries.entrySet()) { //go through the queries and operates all the steps per query.
                    String query = entry.getKey();
                    System.out.println("the query is:" + query);
                    Searcher searcher = new Searcher(stopWords, dirPath, stem, api, semantic, entry.getValue()[1], cb_choseCity.isSelected());
                    searcher.doQuery(query);
                    if (searcher.getDocs().size() == 0) {
                        showAlert("Attention!", "There is no documents for your query", "Please try another query.");
                        txt_Cities.setText("");
                        chosenCities = new ArrayList<>();
                    }
                    else
                        collectedDocs(searcher.getDocs(), entry.getValue()[0]); //collect all the answers of the queries and prepare it to showing results!
                }
            }
            if (docsToShow.size() != 0)
                showDocsQueries(); //show the results
            else {
                showAlert("Attention!", "There is problem with your query document", "Please try another query.");
                txt_Cities.setText("");
                chosenCities = new ArrayList<>();
            }
        }
        btn_runQuery.setDisable(false);
    }

    /**
     * collects all the doc for showing!
     *
     * @param docs
     * @param numQuery
     */
    private void collectedDocs(TreeMap<String, Double> docs, String numQuery) {
        docsToShow.add(new Pair<>(numQuery, docs));
    }

    /**
     * writes the queries to disk in the treceval format.
     */
    private void writeQueryToDisk() {
        try {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File path = directoryChooser.showDialog(null);
            String fullPath = "";
            if (path != null) {
                fullPath = path.getAbsolutePath();
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fullPath + "\\results.txt", false), StandardCharsets.UTF_8));
                int count = 0;
                StringBuilder writeIt = new StringBuilder("");
                Queue<Pair<String, TreeMap<String, Double>>> set = new LinkedList<>();
                set.addAll(docsToShow);
                while (set.size() > 0) {
                    Pair<String, TreeMap<String, Double>> pair = set.poll();
                    String num = pair.getKey();
                    for (Map.Entry<String, Double> entry : pair.getValue().entrySet()) {
                        count++;
                        String docname = entry.getKey();
                        writeIt.append(num + " 0 " + docname + " 0 0 mt\n");
                    }
                    if (count % 50 == 0) {
                        bw.write(writeIt.toString());
                        bw.flush();
                        writeIt = new StringBuilder("");
                    }
                }
                bw.write(writeIt.toString());
                bw.flush();
                bw.close();
                showAlert("Done", "Saving finished!", "The file of queries is ready!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
