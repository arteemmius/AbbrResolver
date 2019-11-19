package ru.textanalysis.abbrresolver.beans;

public class Item {

    private String word;
    private String definition;
    private String description;
    private Integer type;

    public Item() {
    }

    public Item(String word, String definition, String description, Integer type) {
        this.word = word;
        this.definition = definition;
        this.description = description;
        this.type = type;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
    
}
