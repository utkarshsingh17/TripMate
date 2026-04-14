package com.utkarsh.tripmate.model.interactive;

public class PlannerChatRequest {

    private String message;
    private int userSuppliedTopK;
    private String modelName;
    private String temperature;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getUserSuppliedTopK() {
        return userSuppliedTopK;
    }

    public void setUserSuppliedTopK(int userSuppliedTopK) {
        this.userSuppliedTopK = userSuppliedTopK;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }
}
