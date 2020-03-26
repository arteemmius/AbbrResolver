/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.textanalysis.abbrresolver.model.abbr;

/**
 *
 * @author artee
 */
public class AbbrListInputData {
    private String text;
    private Boolean checkPO;
    private String PO;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean isCheckPO() {
        return checkPO;
    }

    public void setCheckPO(Boolean checkPO) {
        this.checkPO = checkPO;
    }

    public String getPO() {
        return PO;
    }

    public void setPO(String PO) {
        this.PO = PO;
    }
    
    
}
