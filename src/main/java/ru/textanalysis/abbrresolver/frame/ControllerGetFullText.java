package ru.textanalysis.abbrresolver.frame;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.textanalysis.abbrresolver.abbrmodel.FullTextInputData;
import ru.textanalysis.abbrresolver.abbrmodel.FullTextOutputData;
import ru.textanalysis.abbrresolver.beans.Sentence;
import ru.textanalysis.abbrresolver.realization.AbbrResolver;
import ru.textanalysis.abbrresolver.realization.utils.PatternFinder;
import ru.textanalysis.abbrresolver.realization.utils.TextManager;
import ru.textanalysis.tawt.jmorfsdk.JMorfSdk;
import ru.textanalysis.tawt.jmorfsdk.loader.*;

@Component       
@RestController
public class ControllerGetFullText {  
    private static final Logger log = Logger.getLogger(ControllerGetFullText.class.getName());
    public final static JMorfSdk jMorfSdk =  JMorfSdkFactory.loadFullLibrary(); 
    @Value("${classifier.run}")
    private boolean runTextAnalizer;
    @Value("${classifier.url}")
    private String urlTextAnalizer;
    
    @RequestMapping(value = "/fullText", method = {RequestMethod.POST}, consumes = {"application/json"})
    public FullTextOutputData getFullText(@RequestBody FullTextInputData input) throws Exception{
        PatternFinder patternFinder = new PatternFinder();  
        FullTextOutputData output = new FullTextOutputData();
        AbbrResolver abbrResolver = new AbbrResolver(input.getText(), input.getPO(), input.isCheckPO(), runTextAnalizer, urlTextAnalizer);
        if (input.isCheckGetAbbr())
            output.setAbbrList(abbrResolver.getAbbrList());
        TextManager textManager = new TextManager(patternFinder, abbrResolver);              
            try {
            //TODO: проверить как разбиваем на предложения, мб подключить либу?
            //убрать JPanel()
            List<Sentence> sentences = textManager.splitText(input.getText(), new javax.swing.JPanel()); 
            log.info("List of sentence = " + sentences);
            log.info("Count sentence = " + sentences.size());
            List<String> result = new ArrayList<>(sentences.size());
            //на выходе должны получить 4 списка
            //списка предложений(уже есть), список списка слов(для каждого предложения)
            //список списков индексов сокращений(для каждого предложения)
            //список сокращений по предложениям
            
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
            output.setText(resultCommaSeparated);
            output.setTextPO(abbrResolver.getTextPO());            
            return output;
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }        
    }
}