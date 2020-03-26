/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.textanalysis.abbrresolver.model.abbr;

import java.util.ArrayList;

/**
 *
 * @author artee
 */
public class AbbrListOutputData {
    private ArrayList<String> abbrList;

    public ArrayList<String> getAbbrList() {
        return abbrList;
    }

    public void setAbbrList(ArrayList<String> abbrList) {
        this.abbrList = new ArrayList(abbrList);
    }
    
}
