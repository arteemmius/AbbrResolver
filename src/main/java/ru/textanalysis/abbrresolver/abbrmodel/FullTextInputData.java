/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package  ru.textanalysis.abbrresolver.abbrmodel;
import java.util.ArrayList;

/**
 *
 * @author asamokhin
 */
public class FullTextInputData {
    private String text;
    private Boolean checkPO;
    private String PO;
    private Boolean checkGetAbbr;    
    
    public void setText(String text) {
        this.text = text;
    }   
    
    public String getText() {
        return this.text;
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

    public Boolean isCheckGetAbbr() {
        return checkGetAbbr;
    }

    public void setCheckGetAbbr(Boolean checkGetAbbr) {
        this.checkGetAbbr = checkGetAbbr;
    }
        
}
