package com.university.abbrresolver.frame;

import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class AbbrPanel extends JPanel{
    
    private JLabel wordLbl;
    private JLabel definitionLbl;
    private JLabel descriptionLbl;

    public AbbrPanel(){
        super(new FlowLayout(FlowLayout.LEADING));
    }
    public void addAbbrPanel(String word, String definition, String description){
        
        this.wordLbl = new JLabel(word);
        this.definitionLbl = new JLabel(definition);
        this.descriptionLbl = new JLabel(description);
        
        this.add(wordLbl);
        this.add(definitionLbl);
        this.add(descriptionLbl);
        
        this.setBackground(new Color(240,240,240));
        
        
    }
    
}
