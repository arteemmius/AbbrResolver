/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.textanalysis.abbrresolver.model.classifier;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author asamokhin
 */

public class ClassifierOutputData {
    private String classifier;
    private ArrayList<PredictionData>  newmap;
    
    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public ArrayList<PredictionData> getNewmap() {
        return newmap;
    }

    public void setNewmap(ArrayList<PredictionData> newmap) {
        this.newmap = newmap;
    }  

}
