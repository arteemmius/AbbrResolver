package ru.textanalysis.abbrresolver.frame;

import ru.textanalysis.abbrresolver.beans.Sentence;
import ru.textanalysis.abbrresolver.realization.AbbrResolver;
import ru.textanalysis.abbrresolver.realization.InputData;
import ru.textanalysis.abbrresolver.realization.PatternFinder;
import ru.textanalysis.abbrresolver.realization.TextManager;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.textanalysis.tawt.jmorfsdk.JMorfSdk;
import ru.textanalysis.tawt.jmorfsdk.loader.*;

@Component       
@RestController
public class GetFullText {  
    private static final Logger log = Logger.getLogger(GetFullText.class.getName());
    public final static JMorfSdk jMorfSdk =  JMorfSdkFactory.loadFullLibrary(); 
    @Value("${classifier.run}")
    private boolean runTextAnalizer;
    @Value("${classifier.url}")
    private String urlTextAnalizer;
    
    @RequestMapping(value = "/fullText", method = {RequestMethod.POST}, consumes = {"application/json"})
    public String getFullText(@RequestBody InputData input) throws Exception{
        PatternFinder patternFinder = new PatternFinder();  
        AbbrResolver abbrResolver = new AbbrResolver(input.getText(), runTextAnalizer, urlTextAnalizer);
        TextManager textManager = new TextManager(patternFinder, abbrResolver);              
            try {
            List<Sentence> sentences = textManager.splitText(input.getText(), new javax.swing.JPanel()); 
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