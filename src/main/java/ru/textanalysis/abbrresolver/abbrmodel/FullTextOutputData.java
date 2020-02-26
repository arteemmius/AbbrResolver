/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.textanalysis.abbrresolver.abbrmodel;

import java.util.ArrayList;

/**
 *
 * @author artee
 */
public class FullTextOutputData {
    private String text;
    private ArrayList<String> abbrList;
    private String textPO;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ArrayList<String> getAbbrList() {
        return abbrList;
    }

    public void setAbbrList(ArrayList<String> abbrList) {
        this.abbrList = abbrList;
    }

    public String getTextPO() {
        return textPO;
    }

    public void setTextPO(String textPO) {
        this.textPO = textPO;
    }
    
}
