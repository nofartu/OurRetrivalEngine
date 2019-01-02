package sample;

import javafx.beans.property.SimpleStringProperty;

/**
 * Class that support the show of Entities in tableView
 */
public class EntityShow {
    private final SimpleStringProperty entityName;
    private final SimpleStringProperty entityRate;


    public EntityShow(String entityName, String entityRate) {
        this.entityName = new SimpleStringProperty(entityName);
        this.entityRate = new SimpleStringProperty(entityRate);
    }

    public String getEntityName() {
        return entityName.get();
    }

    public SimpleStringProperty entityNameProperty() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName.set(entityName);
    }

    public String getEntityRate() {
        return entityRate.get();
    }

    public SimpleStringProperty entityRateProperty() {
        return entityRate;
    }

    public void setEntityRate(String entityRate) {
        this.entityRate.set(entityRate);
    }
}
