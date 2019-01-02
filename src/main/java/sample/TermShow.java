package sample;

import javafx.beans.property.SimpleStringProperty;


/**
 * Serves the TableView
 */
public class TermShow {
    private final SimpleStringProperty termName;
    private final SimpleStringProperty value;

    public TermShow(String termName, int value) {
        this.termName = new SimpleStringProperty(termName);
        this.value = new SimpleStringProperty(value + "");
    }

    public String getTermName() {
        return termName.get();
    }

    public SimpleStringProperty termNameProperty() {
        return termName;
    }

    public String getValue() {
        return value.get();
    }

    public SimpleStringProperty valueProperty() {
        return value;
    }
}
