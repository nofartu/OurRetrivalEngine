package sample;

import javafx.beans.property.SimpleStringProperty;

public class EntityShow {
    private final SimpleStringProperty entityName;

    public EntityShow(String entityName){
        this.entityName = new SimpleStringProperty(entityName);
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
}
