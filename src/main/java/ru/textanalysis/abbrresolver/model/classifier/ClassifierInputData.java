/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.textanalysis.abbrresolver.model.classifier;

import java.util.ArrayList;

/**
 *
 * @author artee
 */
public class ClassifierInputData {
    private String text;
    private int n;
    private ArrayList<String> classifier = new ArrayList<>();
    private String model;    

    public ArrayList<String> getClassifier() {
        return classifier;
    }

    public void setClassifier(ArrayList<String> classifier) {
        this.classifier = classifier;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
    
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }
    
}
