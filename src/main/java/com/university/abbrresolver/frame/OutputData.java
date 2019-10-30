/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mai.textanalyzer.web.vaadin.pages.classification;

import java.util.HashMap;

/**
 *
 * @author asamokhin
 */
public class OutputData {
    private HashMap<String,  HashMap<String, Double>>  newmap = new HashMap<>();
     
    
    public void setData(String modelname, HashMap<String, Double> map) {
        this.newmap.put(modelname, map);
    }
    
    public HashMap<String,  HashMap<String, Double>> getData() {
        return this.newmap;
    }    

}
