package com.university.abbrresolver.frame;

import com.university.abbrresolver.beans.Sentence;
import static com.university.abbrresolver.frame.MainWindow.findAbbrPanel;
import static com.university.abbrresolver.frame.MainWindow.jMorfSdk;
import com.university.abbrresolver.realization.AbbrResolver;
import com.university.abbrresolver.realization.PatternFinder;
import com.university.abbrresolver.realization.TextManager;
import java.util.List;
import java.util.ArrayList;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component       
@RestController
public class GetFullText {  
    private static final Logger log = Logger.getLogger(GetFullText.class.getName());
    @Value("${classifier.run}")
    private boolean runTextAnalizer;
    
    @RequestMapping(value = "/fullText", method = {RequestMethod.POST}, consumes = {"application/json"})
    public String getFullText(@RequestBody InputData input) throws Exception{
        PatternFinder patternFinder = new PatternFinder();  
        AbbrResolver abbrResolver = new AbbrResolver(input.getText(), runTextAnalizer);
        TextManager textManager = new TextManager(patternFinder, abbrResolver);              
            try {
            List<Sentence> sentences = textManager.splitText(input.getText(), findAbbrPanel); 
            log.info("List of sentence = " + sentences);
            log.info("Count sentence = " + sentences.size());
            List<String> result = new ArrayList<>(sentences.size());          
            for (Sentence sentence : sentences) {
                try {
                    result.add(abbrResolver.resolveAcronyms(jMorfSdk, sentence));
                }
                catch(Exception e) {
                    e.printStackTrace();
                    result.add(sentence.toString());
                }

            }
            String resultCommaSeparated = String.join(".", result); //выход
            log.info("Result = " + resultCommaSeparated);  
            System.out.println("Result = " + resultCommaSeparated);
            return resultCommaSeparated;
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }        
    }          
}