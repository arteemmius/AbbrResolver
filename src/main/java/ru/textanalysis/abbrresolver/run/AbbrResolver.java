package ru.textanalysis.abbrresolver.run;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.util.*;
import javax.swing.JPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import ru.textanalysis.abbrresolver.model.classifier.ClassifierInputData;
import ru.textanalysis.abbrresolver.model.classifier.ClassifierOutputData;
import ru.textanalysis.abbrresolver.pojo.Descriptor;
import ru.textanalysis.abbrresolver.pojo.DescriptorType;
import ru.textanalysis.abbrresolver.pojo.Item;
import ru.textanalysis.abbrresolver.pojo.Sentence;
import ru.textanalysis.abbrresolver.run.utils.DBManager;
import ru.textanalysis.abbrresolver.run.utils.Utils;
import ru.textanalysis.tawt.jmorfsdk.*;
import ru.textanalysis.tawt.ms.grammeme.MorfologyParameters;
import ru.textanalysis.tawt.ms.internal.IOmoForm;
import ru.textanalysis.tawt.ms.storage.OmoFormList;

public class AbbrResolver {

    private static final List<Byte> VERB_TYPES = Arrays.asList(MorfologyParameters.TypeOfSpeech.VERB, MorfologyParameters.TypeOfSpeech.INFINITIVE);
    private static final Set<String> GENITIVE_PREPOSITIONS = new HashSet<>(Arrays.asList("без", "у", "до", "от", "с", "около", "из", "возле", "после", "для", "вокруг"));
    private static final Set<String> DATIVE_PREPOSITIONS = new HashSet<>(Arrays.asList("к", "по"));
    private static final Set<String> ACCUSATIVE_PREPOSITIONS = new HashSet<>(Arrays.asList("в", "за", "на", "про", "через"));
    private static final Set<String> ABLTIVE_PREPOSITIONS = new HashSet<>(Arrays.asList("за", "над", "под", "перед", "с"));
    private static final Set<String> PREPOSITIONA_PREPOSITIONS = new HashSet<>(Arrays.asList("в", "на", "о", "об", "обо", "при"));
    
    private static final Logger log = LoggerFactory.getLogger(AbbrResolver.class.getName());    
    private static String textPO = null;
    private static String text;
    private static boolean runTextAnalizer;
    private static String urlTextAnalizer;
    private static HashMap<String, String> classesMappingDict;
    private static Boolean checkPO;
    private static ArrayList<String> abbrList = new ArrayList<>();
    
    public AbbrResolver(String text, String PO, Boolean checkPO, boolean runTextAnalizer, String urlTextAnalizer, String classesMappingPath) {
        this.text = text;
        this.textPO = PO;
        this.runTextAnalizer = runTextAnalizer;
        this.checkPO = checkPO;        
        this.urlTextAnalizer = urlTextAnalizer;
        this.classesMappingDict = fillMappingDict(classesMappingPath);
    }    
    @SuppressWarnings("empty-statement")
    public static HashMap<String, String> fillMappingDict(String path) {
        HashMap<String, String> classesMappingVoc = new HashMap<>();
        try {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF8"));
                while ((line = br.readLine()) != null) {
                        classesMappingVoc.put(line.split("=")[0], line.split("=")[1]);
                }
                br.close();
        }
        catch (Exception e) {
                log.info("не могу прочитать файл!!!");
                e.printStackTrace();
        }          
        return classesMappingVoc;
    }
    public void fillAbbrDescriptions(String ptest, DBManager dictionary, List<Descriptor> descriptors, JPanel findAbbrPanel) throws Exception {
        log.info("fillAbbrDescriptions: start fillAbbrDescriptions()");
//        log.info("text = " + text);
        if (textPO == null && runTextAnalizer && checkPO)
            textPO = runClassifier(text);
        log.info("fillAbbrDescriptions: textPO = " + textPO);
        log.info("fillAbbrDescriptions: runTextAnalizer = " + runTextAnalizer);
        for (Descriptor curDescriptor : descriptors) {
            List<String> longForms = null;
            if (textPO != null)
                longForms = dictionary.findAbbrLongFormsWithMainWord(curDescriptor.getValue(), textPO, classesMappingDict);
            if (longForms == null || longForms.isEmpty())
                longForms = dictionary.findAbbrLongForms(curDescriptor.getValue());
            
            List<String> properties = dictionary.findAbbrInfo(curDescriptor.getValue());

            if (longForms.isEmpty() && curDescriptor.getValue().contains("-")) {
                for (String s : getPossibleInfinitives(curDescriptor.getValue())) {
                    longForms = dictionary.findAbbrLongForms(s);
                    if (!longForms.isEmpty()) {
                        break;
                    }
                }
            }
            
            
            if (!longForms.isEmpty()) {
/*                
                if(!abbrList.contains(curDescriptor.getValue() + " : " + longForms.get(0)))
                    abbrList.add(curDescriptor.getValue() + " : " + longForms.get(0));
*/                
                curDescriptor.setDesc(longForms.get(0));
            }else{
                curDescriptor.setDesc(curDescriptor.getValue());
            }
            Item item = new Item();
            if (!properties.isEmpty()) {
                item.setWord(properties.get(0));
                item.setDefinition(properties.get(1));
                item.setDescription("\t"); 
            }
            log.info("fillAbbrDescriptions: " + item.getWord() + "\t" + item.getDefinition());
            //AbbrPanel abbrPanel = new AbbrPanel();
            //abbrPanel.addAbbrPanel(item.getWord(), item.getDefinition(), item.getDescription());
            //findAbbrPanel.add(abbrPanel);
            //findAbbrPanel.repaint();
            //findAbbrPanel.revalidate();
        }
        log.info("fillAbbrDescriptions: success complete");           
    }

    public String resolveAcronyms(JMorfSdk jMorfSdk, Sentence sentence) throws Exception {

        List<Descriptor> descriptors = sentence.getDescriptors(); //разделение текста на слова и знаки препинания
        Descriptor curDescriptor;
        log.info("resolveAcronyms: descriptors= " + descriptors);        
        String copy = sentence.getContent(); //копируем все слова в переменную copy
        log.info("resolveAcronyms: copy= " + copy);  
        String[] acronymWords;
        StringBuilder text = new StringBuilder("");
        for (int i = 0; i < descriptors.size(); i++) {
                curDescriptor = descriptors.get(i);
                Descriptor curDescriptorNext = null;
                if (i != descriptors.size() - 1)
                    curDescriptorNext = descriptors.get(i + 1);
                log.info("resolveAcronyms: curDescriptor.getDesc() = " + curDescriptor.getDesc()); 
                log.info("resolveAcronyms: curDescriptor.getValue() = " + curDescriptor.getValue());   
                log.info("resolveAcronyms: curDescriptor.getType() = " + curDescriptor.getType());
                log.info("resolveAcronyms: curDescriptor.getStartPos() = " + curDescriptor.getStartPos());
                log.info("resolveAcronyms: i-ый элемент= " + curDescriptor);      
                log.info("resolveAcronyms: i-ый элемент_start_pos= " + curDescriptor.getStartPos());                    
                if (Objects.equals(curDescriptor.getType(), DescriptorType.SHORT_WORD) || Objects.equals(curDescriptor.getType(), DescriptorType.CUT_WORD)) {        
               // if (Objects.equals(curDescriptor.getType(), DescriptorType.SHORT_WORD)) {        
                log.info("resolveAcronyms: curDescriptor.getType()= " + curDescriptor.getType());                  
                log.info("resolveAcronyms: find abbr= " + descriptors.get(i));                
                if(Objects.equals(curDescriptor.getType(), DescriptorType.SHORT_WORD)) {
                    acronymWords = curDescriptor.getDesc().replaceAll("\\s+", " ").split(" "); //делим сокращение, состоящее из нескольких слов на части  
                }
                else {
                    acronymWords = curDescriptor.getDesc().split("");
                }
                log.info("resolveAcronyms: curDescriptor.getDesc().split(\" \")= " + curDescriptor.getDesc().split(" ").toString());                                 
                log.info("resolveAcronyms: 1acronymWords= " + String.join(",", acronymWords));               
                boolean[] capitalizeWords = new boolean[acronymWords.length];

                //save acronym case
                for (int j = 0; j < acronymWords.length; j++) {
                    String word = acronymWords[j];
                    //[SAM:K414] в БД сокращения лежат с лишними пробелами, надо учитывать
                    if ("".equals(word) || " ".equals(word)) continue;
                    
                    log.info("resolveAcronyms: word= " + word);                     
                    if (Character.isUpperCase(word.charAt(0))) {
                        acronymWords[j] = Utils.uncapitalize(word);
                        capitalizeWords[j] = true;
                    }
                }
                log.info("resolveAcronyms: 2acronymWords= " + String.join(",", acronymWords)); 
                int mainWordAcronymIndex = getMainWordAcronymIndex(acronymWords, jMorfSdk); //getMainWordAcronymIndex - индекс главного слова внутри сокращения
                log.info("resolveAcronyms: mainWordAcronymIndex= " + mainWordAcronymIndex);
                Integer collacationMainWordIndex = getMainWordIndex(descriptors, i, acronymWords[mainWordAcronymIndex], jMorfSdk);
                log.info("resolveAcronyms: collacationMainWordIndex= " + collacationMainWordIndex);                
                if (collacationMainWordIndex != null) {
                    String collMainWord = descriptors.get(collacationMainWordIndex).getValue();
                    log.info("resolveAcronyms: collMainWord= " + collMainWord);                    
                    Integer prepositionIndex = getPrepositionIndex(descriptors, i, collacationMainWordIndex, jMorfSdk);
                    log.info("resolveAcronyms: prepositionIndex= " + prepositionIndex);                     
                    String prepositionWord = prepositionIndex != null ? descriptors.get(prepositionIndex).getValue() : "";
                    log.info("resolveAcronyms: prepositionWord= " + prepositionWord);                     
                    acronymWords[mainWordAcronymIndex] = getTrueAcronymForm(acronymWords[mainWordAcronymIndex], collMainWord, prepositionWord, jMorfSdk);
                    log.info("resolveAcronyms: acronymWords[mainWordAcronymIndex]= " + acronymWords[mainWordAcronymIndex]);  
                } else{
                    //acronymWords[mainWordAcronymIndex];
                }
                for (int j = 0; j < acronymWords.length; j++) {
                        log.info("resolveAcronyms: acronymWords[" + j + "]" + acronymWords[j]);
                }                
                log.info("resolveAcronyms: before mainWordAcronymIndex" + mainWordAcronymIndex);                
                
                if (acronymWords.length > 1) {
                    try {
                        adaptAcronymWords(acronymWords, mainWordAcronymIndex, jMorfSdk);
                    }
                    catch(Exception e) {
                        log.info("resolveAcronyms: resolveAcronyms: can't acronymWords.length " + acronymWords.length);
                        //e.printStacklog.info();
                    }
                }
                log.info("resolveAcronyms: after mainWordAcronymIndex" + mainWordAcronymIndex);
                //restore acronym case
                for (int j = 0; j < acronymWords.length; j++) {
                    if (capitalizeWords[j]) {
                        acronymWords[j] = Utils.capitalize(acronymWords[j]);
                        log.info("resolveAcronyms: acronymWords[" + j + "]" + acronymWords[j]);
                    }
                }
                log.info("resolveAcronyms: Utils.concat(\" \", Arrays.asList(acronymWords)) = " + Utils.concat(" ", Arrays.asList(acronymWords)));
                log.info("resolveAcronyms: curDescriptor.getValue()" + curDescriptor.getValue());
//                copy = copy.replace(curDescriptor.getValue(), Utils.concat(" ", Arrays.asList(acronymWords)));      //replaceAll заменить
                
                text.append(Utils.concat(" ", Arrays.asList(acronymWords)));
                text.append(" ");
            } else{
                text.append(curDescriptor.getValue());
                if (curDescriptorNext != null && curDescriptorNext.getType() != DescriptorType.PUNCTUATION_CHAR &&
                        curDescriptorNext.getType() != DescriptorType.SENTENCE_END || curDescriptor.getType() == DescriptorType.SENTENCE_END)
                    text.append(" ");
            }
        }
        return text.toString().replaceAll("  ", " ");
    }

    /**
     * Определяет главное слово в полной форме сокращения, возвращает индекс главного слова внутри сокращения
     */
    private int getMainWordAcronymIndex(String[] acronymWords, JMorfSdk jMorfSdk) throws Exception {
        log.info("getMainWordAcronymIndex: start getMainWordAcronymIndex()");       
        log.info("getMainWordAcronymIndex: acronymWords.length= " + acronymWords.length);           
        if (acronymWords.length > 1) {
            OmoFormList omoForms;        
            IOmoForm omoForm;        
            for (int i = 0; i < acronymWords.length; i++) {                   
                if (!(jMorfSdk.getAllCharacteristicsOfForm(acronymWords[i])).isEmpty()) {
                    omoForms = jMorfSdk.getAllCharacteristicsOfForm(acronymWords[i]);
                    omoForm = omoForms.get(0);
                    log.info("getMainWordAcronymIndex: omoForms= " + omoForms);  
                    log.info("getMainWordAcronymIndex: omoForm= " + omoForm);  
                    log.info("getMainWordAcronymIndex: omoForm.getTypeOfSpeech()= " + omoForm.getTypeOfSpeech());  
                    log.info("getMainWordAcronymIndex: MorfologyParameters.TypeOfSpeech.NOUN= " + MorfologyParameters.TypeOfSpeech.NOUN);  
                    log.info("getMainWordAcronymIndex: omoForm.getTheMorfCharacteristics(MorfologyParameters.Case.class= " + omoForm.getTheMorfCharacteristics(MorfologyParameters.Case.class));  
                    log.info("getMainWordAcronymIndex: MorfologyParameters.Case.NOMINATIVE= " + MorfologyParameters.Case.NOMINATIVE);                    
                    if (omoForm.getTypeOfSpeech() == MorfologyParameters.TypeOfSpeech.NOUN
                            && omoForm.getTheMorfCharacteristics(MorfologyParameters.Case.class) == MorfologyParameters.Case.NOMINATIVE) {  //именительный
                        return i;
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Ищет главное слово в предложении для сокращения
     */
    private Integer getMainWordIndex(List<Descriptor> descriptors, int acronymIndex, String acronym, JMorfSdk jMorfSdk) throws Exception {
        log.info("getMainWordIndex: start getMainWordIndex()"); 
        OmoFormList omoForms = jMorfSdk.getAllCharacteristicsOfForm(acronym);
        log.info("getMainWordIndex: omoForms= " + omoForms);
        if (omoForms.isEmpty()) {
            return null;
        }
        byte acronymTypeOfSpeech = omoForms.get(0).getTypeOfSpeech();
        log.info("getMainWordIndex: acronymTypeOfSpeech= " + acronymTypeOfSpeech);
        int wordIndex = acronymIndex;

        if (Arrays.asList(MorfologyParameters.TypeOfSpeech.ADJECTIVEFULL, MorfologyParameters.TypeOfSpeech.ADJECTIVESHORT, MorfologyParameters.TypeOfSpeech.NOUNPRONOUN,
                MorfologyParameters.TypeOfSpeech.PARTICIPLE, MorfologyParameters.TypeOfSpeech.PARTICIPLEFULL, MorfologyParameters.TypeOfSpeech.NUMERAL).contains(acronymTypeOfSpeech)) {
            //определяющее слово впереди
            while (++wordIndex < descriptors.size()) {
                Descriptor curDescriptor = descriptors.get(wordIndex);
                log.info("getMainWordIndex: 1curDescriptor= " + curDescriptor); 
                try {
                    if (Objects.equals(curDescriptor.getType(), DescriptorType.RUSSIAN_LEX) && !Character.isUpperCase(curDescriptor.getValue().charAt(0))) {
                        IOmoForm wordOmoForm = null;
                        if (!jMorfSdk.getAllCharacteristicsOfForm(curDescriptor.getValue()).isEmpty())
                            wordOmoForm = jMorfSdk.getAllCharacteristicsOfForm(curDescriptor.getValue()).get(0);
                        else
                            return null;
                        if (wordOmoForm.getTypeOfSpeech() == MorfologyParameters.TypeOfSpeech.NOUN) {
                            log.info("getMainWordIndex: 1wordIndex= " + wordIndex);                        
                            return wordIndex;
                        }
                    }
                }
                catch (Exception e) {
                    log.error("getMainWordIndex: error = " + e.getMessage());   
                    return null;
                }
            }
        } else if (acronymTypeOfSpeech == MorfologyParameters.TypeOfSpeech.NOUN) {
            //главное слово позади
            while (--wordIndex >= 0) {
                Descriptor curDescriptor = descriptors.get(wordIndex);
                log.info("getMainWordIndex: 2curDescriptor= " + curDescriptor);  
                try {
                    if (Objects.equals(curDescriptor.getType(), DescriptorType.RUSSIAN_LEX) && !Character.isUpperCase(curDescriptor.getValue().charAt(0))) {
                        IOmoForm wordOmoForm;
                        if (!jMorfSdk.getAllCharacteristicsOfForm(curDescriptor.getValue()).isEmpty())
                            wordOmoForm = jMorfSdk.getAllCharacteristicsOfForm(curDescriptor.getValue()).get(0);
                        else
                            return null;
                        if (wordOmoForm.getTypeOfSpeech() == MorfologyParameters.TypeOfSpeech.NOUN
                                || VERB_TYPES.contains(wordOmoForm.getTypeOfSpeech())) {
                            log.info("getMainWordIndex: 2wordIndex= " + wordIndex);                         
                            return wordIndex;
                        }
                    }
                    else if (Objects.equals(curDescriptor.getType(), DescriptorType.NUM_SEQ) && acronymIndex - wordIndex < 3) {
                        OmoFormList omoFormList = jMorfSdk.getAllCharacteristicsOfForm(curDescriptor.getValue());
                        if (!omoFormList.isEmpty() && omoFormList.get(0).getTypeOfSpeech() == MorfologyParameters.TypeOfSpeech.NUMERAL) {
                            log.info("getMainWordIndex: 3wordIndex= " + wordIndex);                         
                            return wordIndex;
                        }
                    }                    
                }
                catch (Exception e) {
                    log.error("getMainWordIndex: error = " + e.getMessage());   
                    return null;                    
                }
            }
        }
        return null;
    }

    private Integer getPrepositionIndex(List<Descriptor> descriptors, int acronymIndex, int mainWordIndex, JMorfSdk jMorfSdk) throws Exception {
        int delta = acronymIndex - mainWordIndex;
        int startIndex = delta > 0 ? mainWordIndex : acronymIndex;
        int endIndex = startIndex + Math.abs(delta);
        for (int wordIndex = startIndex + 1; wordIndex < endIndex; wordIndex++) {
            Descriptor curDescriptor = descriptors.get(wordIndex);
            try {
                if (Objects.equals(curDescriptor.getType(), DescriptorType.RUSSIAN_LEX) && !Character.isUpperCase(curDescriptor.getValue().charAt(0))) {
                    IOmoForm wordOmoForm = jMorfSdk.getAllCharacteristicsOfForm(curDescriptor.getValue()).get(0);
                    if (wordOmoForm.getTypeOfSpeech() == MorfologyParameters.TypeOfSpeech.PRETEXT) { 
                        return wordIndex;
                    }
                }
            }
            catch (Exception e) {
                log.error("getMainWordIndex: error = " + e.getMessage());   
                return null;                    
            }            
        }
        return null;
    }

    /**
     * Согласует сокращение с главным словом в предложении
     */
    private String getTrueAcronymForm(String acronymMainWord, String collacationMainWord, String preposition, JMorfSdk jMorfSdk) throws Exception {
        IOmoForm acronymOmoForm = jMorfSdk.getAllCharacteristicsOfForm(acronymMainWord).get(0);
        byte acronymTypeOfSpeech = acronymOmoForm.getTypeOfSpeech();
        long acronymNumber = acronymOmoForm.getTheMorfCharacteristics(MorfologyParameters.Numbers.IDENTIFIER);

        IOmoForm mainWordOmoForm = jMorfSdk.getAllCharacteristicsOfForm(collacationMainWord.toLowerCase()).get(0);
        byte mainWordTypeOfSpeech = mainWordOmoForm.getTypeOfSpeech();
        long mainWordCase = mainWordOmoForm.getTheMorfCharacteristics(MorfologyParameters.Case.IDENTIFIER);
        long mainWordNumbers = mainWordOmoForm.getTheMorfCharacteristics(MorfologyParameters.Numbers.IDENTIFIER);
        long mainWordGender = mainWordOmoForm.getTheMorfCharacteristics(MorfologyParameters.Gender.IDENTIFIER);

        if (Arrays.asList(MorfologyParameters.TypeOfSpeech.ADJECTIVEFULL, MorfologyParameters.TypeOfSpeech.ADJECTIVESHORT, MorfologyParameters.TypeOfSpeech.NOUNPRONOUN,
                MorfologyParameters.TypeOfSpeech.PARTICIPLE, MorfologyParameters.TypeOfSpeech.PARTICIPLEFULL, MorfologyParameters.TypeOfSpeech.NUMERAL).contains(acronymTypeOfSpeech)) {
            //model 1: сокр. (прил., местоим., причастие, числит.) + сущ. (глав.)
            //getDerivativeForm - формируем словоформу по заданным параметрам
            List<String> matchList;
            try {
                matchList = jMorfSdk.getDerivativeForm(acronymMainWord, mainWordCase);
            }
            catch(Exception e) {
                //e.printStacklog.info();
                log.error("getTrueAcronymForm: error on " + acronymMainWord);       
                return acronymMainWord;
            }            
            removeIf(matchList, MorfologyParameters.Numbers.class, mainWordNumbers, jMorfSdk);
            //removeIf(jMorfSdk, matchList, MorfologyParameters.Gender.class, mainWordGender);       //мужской - средний род (противоположном направлении, противоположном ключе)
            return matchList.get(0);
        } else if (acronymTypeOfSpeech == MorfologyParameters.TypeOfSpeech.NOUN && VERB_TYPES.contains(mainWordTypeOfSpeech)) {
            //model 2:  глаг. (главн.) + сокр. (сущ.)
            if (preposition != null && !preposition.isEmpty()) {
                long prepositionCase = getCaseByPreposition(preposition);
                
            try {                
                List<String> matchList = jMorfSdk.getDerivativeForm(acronymMainWord, prepositionCase);
                removeIf(matchList, MorfologyParameters.Numbers.class, mainWordNumbers, jMorfSdk);
                return matchList.get(0);
            }
            catch(Exception e) {
                //e.printStacklog.info();
                log.error("getTrueAcronymForm: error on " + acronymMainWord);        
                return acronymMainWord;
            } 
            
            } else {
                long transitivity = mainWordOmoForm.getTheMorfCharacteristics(MorfologyParameters.Transitivity.class);  //переходный
                if (transitivity == MorfologyParameters.Transitivity.TRAN) {
                try {                     
                    List<String> matchList = jMorfSdk.getDerivativeForm(acronymMainWord, MorfologyParameters.Case.ACCUSATIVE);  //винительный
                    removeIf(matchList, MorfologyParameters.Numbers.class, mainWordNumbers, jMorfSdk);
                    return matchList.get(0);
                }
                catch(Exception e) {
                    //e.printStacklog.info();
                    log.error("getTrueAcronymForm: error on " + acronymMainWord);        
                    return acronymMainWord;
                }                 
                } else if (transitivity == MorfologyParameters.Transitivity.INTR) {
                    //глагол непереходный. Что делать?
                }
            }
        } else if (acronymTypeOfSpeech == MorfologyParameters.TypeOfSpeech.NOUN && mainWordTypeOfSpeech == MorfologyParameters.TypeOfSpeech.NOUN) {
            //model 3:  сущ. (главн.) + сокр. (сущ.)
            if (preposition != null && !preposition.isEmpty()) {
                long prepositionCase = getCaseByPreposition(preposition);
                try {                 
                    List<String> matchList = jMorfSdk.getDerivativeForm(acronymMainWord, prepositionCase);
                    removeIf(matchList, MorfologyParameters.Numbers.class, MorfologyParameters.Numbers.SINGULAR, jMorfSdk);
                    return matchList.get(0);
                }
                catch(Exception e) {
                    //e.printStacklog.info();
                    log.error("getTrueAcronymForm: error on " + acronymMainWord);        
                    return acronymMainWord;
                }                  
            } else {
                List<String> matchList = null;
                try {
                    matchList = jMorfSdk.getDerivativeForm(acronymMainWord, MorfologyParameters.Case.GENITIVE);
                }
                catch(Exception e) {
                    //e.printStacklog.info();
                    log.error("getTrueAcronymForm: error on " + acronymMainWord);   
                    return acronymMainWord;
                }
                removeIf(matchList, MorfologyParameters.Numbers.class, MorfologyParameters.Numbers.SINGULAR, jMorfSdk);
                //removeIf(matchList, MorfologyParameters.Gender.class, mainWordGender, jMorfSdk);    //мужской - средний род (противоположном направлении, противоположном ключе)
                if (!matchList.isEmpty())
                    return matchList.get(0);
                else
                    return acronymMainWord;
            } 
        } else if (acronymTypeOfSpeech == MorfologyParameters.TypeOfSpeech.NOUN && mainWordTypeOfSpeech == MorfologyParameters.TypeOfSpeech.NUMERAL) {
            Integer count = Utils.parseInt(collacationMainWord);
            if (count != null) {
                int tailTen = count % 10;
                int tailHundred = count % 100;
                if (tailTen == 1 && tailHundred != 11) {
                    return acronymMainWord;
                } 
                if ((tailTen == 2 && tailHundred != 12) || (tailTen == 3 && tailHundred != 13) || (tailTen == 4 && tailHundred != 14)) {
                    try {                    
                        List<String> matchList = jMorfSdk.getDerivativeForm(acronymMainWord, MorfologyParameters.Case.GENITIVE);
                        removeIf(matchList, MorfologyParameters.Numbers.class, MorfologyParameters.Numbers.SINGULAR, jMorfSdk);
                        return matchList.get(0);
                    }
                    catch(Exception e) {
                        //e.printStacklog.info();
                        log.error("getTrueAcronymForm: error on " + acronymMainWord);    
                        return acronymMainWord;
                    }                       
                }else {
                    List<String> matchList = null;
                    try {
                        log.info("acronymMainWord = " + acronymMainWord);
                        matchList = jMorfSdk.getDerivativeForm(acronymMainWord, MorfologyParameters.Case.GENITIVE);
                        removeIf(matchList, MorfologyParameters.Numbers.class, MorfologyParameters.Numbers.PLURAL, jMorfSdk);
                    }
                    catch(Exception e) {
                        //e.printStacklog.info();
                        log.error("getTrueAcronymForm: error on " + acronymMainWord);                        
                        return acronymMainWord;
                    }                    
                    
                    if (!matchList.isEmpty())
                        return matchList.get(0);
                    else
                        return acronymMainWord;
                }
            }
        }else{
            return acronymMainWord;
        }
        return acronymMainWord;
    }

    private void adaptAcronymWords(String[] acronymWords, int mainWordAcronymIndex, JMorfSdk jMorfSdk) throws Exception {
        String curWord;
        IOmoForm acronymOmoForm = jMorfSdk.getAllCharacteristicsOfForm(acronymWords[mainWordAcronymIndex]).get(0);
        long acronymCase = acronymOmoForm.getTheMorfCharacteristics(MorfologyParameters.Case.IDENTIFIER); //определяем падеж главного слова
        log.info("adaptAcronymWords_acronymWords= " + String.join(",", acronymWords)); 
        if (acronymCase != MorfologyParameters.Case.NOMINATIVE) {
            long lastNounNumbers = acronymOmoForm.getTheMorfCharacteristics(MorfologyParameters.Numbers.IDENTIFIER);
            long lastNounGender = acronymOmoForm.getTheMorfCharacteristics(MorfologyParameters.Gender.IDENTIFIER);
            for (int i = acronymWords.length - 1; i >= 0; i--) {
                curWord = acronymWords[i];  
                if(jMorfSdk.getAllCharacteristicsOfForm(curWord).isEmpty()){
                    continue;
                }
                log.info("curWord= " + curWord);                   
                log.info("jMorfSdk.getAllCharacteristicsOfForm(curWord)= " + jMorfSdk.getAllCharacteristicsOfForm(curWord));                  
                IOmoForm wordOmoForm = jMorfSdk.getAllCharacteristicsOfForm(curWord).get(0);
                if (wordOmoForm.getTypeOfSpeech() == MorfologyParameters.TypeOfSpeech.NOUN) {
                    lastNounNumbers = wordOmoForm.getTheMorfCharacteristics(MorfologyParameters.Numbers.IDENTIFIER);
                    lastNounGender = wordOmoForm.getTheMorfCharacteristics(MorfologyParameters.Gender.IDENTIFIER);
                }
                if ((i != mainWordAcronymIndex) && (wordOmoForm.getTypeOfSpeech() != MorfologyParameters.TypeOfSpeech.PRETEXT) && (wordOmoForm.getTheMorfCharacteristics(MorfologyParameters.Case.IDENTIFIER) == MorfologyParameters.Case.NOMINATIVE)) {
                    String initialForm = jMorfSdk.getAllCharacteristicsOfForm(curWord).get(0).getInitialFormString();
                    List<String> matchList  = null;
                    try {
                        matchList = jMorfSdk.getDerivativeForm(initialForm, acronymCase);
                    }
                    catch(Exception e) {
                        log.error("adaptAcronymWords: catch exception!" + e.getMessage());
                        matchList.add(initialForm);
                    }  
                    removeIf(matchList, MorfologyParameters.Numbers.class, lastNounNumbers, jMorfSdk);
                    removeIf(matchList, MorfologyParameters.Gender.class, lastNounGender, jMorfSdk);       //мужской - средний род (противоположном направлении, противоположном ключе)
                    if (matchList.size() > 0) {
                        acronymWords[i] = matchList.get(0);
                    }
                }
            }
        }
    }

    private void removeIf(List<String> matchList, Class morfologyParameterClass, long param, JMorfSdk jMorfSdk) {
        if (param != 0L) {
            matchList.removeIf(s -> {
                try {
                    OmoFormList omoForms = jMorfSdk.getAllCharacteristicsOfForm(s);
                    return omoForms.isEmpty() || omoForms.get(0).getTheMorfCharacteristics(morfologyParameterClass) != param;
                } catch (Exception e) {
                    log.error("removeIf: catch exception!" + e.getMessage());
                }
                return false;
            });
        }
    }

    private long getCaseByPreposition(String preposition) throws UnsupportedEncodingException, UnsupportedEncodingException {
        if (GENITIVE_PREPOSITIONS.contains(preposition)) {
            return MorfologyParameters.Case.GENITIVE;  
        } else if (DATIVE_PREPOSITIONS.contains(preposition)) {
            return MorfologyParameters.Case.DATIVE;
        } else if (ACCUSATIVE_PREPOSITIONS.contains(preposition)) {
            return MorfologyParameters.Case.ACCUSATIVE; 
        } else if (ABLTIVE_PREPOSITIONS.contains(preposition)) {
            return MorfologyParameters.Case.ABLTIVE;
        } else if (PREPOSITIONA_PREPOSITIONS.contains(preposition)) {
            return MorfologyParameters.Case.PREPOSITIONA;   
        } else {
            return MorfologyParameters.Case.NOMINATIVE;   //default
        }
    }

    public String runClassifier(String text) throws Exception {
        log.info("text = " + text);
        ClassifierInputData input = new ClassifierInputData();      
        ArrayList<String> classifier = new ArrayList<>();
        classifier.add("MYLTI_CLASSIFIER");
        input.setClassifier(classifier);
        input.setModel("DOC2VEC");
        byte[] array = text.getBytes();
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        outputBuffer.write(array);
        String textStream = new String(outputBuffer.toByteArray(), "UTF-8");
        input.setText(text);
        input.setN(1); //запрашиваем топ - 1
        try {
            ClassifierOutputData[] res = sendREST_POST(input);
            log.info("sendREST_POST: result = " + res[0].getNewmap().get(0).getTopic());
            return  res[0].getNewmap().get(0).getTopic();
        }
        catch (Exception e) {
                    log.error("runClassifier: catch exception!" + e.getMessage());
            return null;
        }  
    }
/*
    private static ClassifierOutputData[] sendREST_POST(ClassifierInputData obj, String uri) throws Exception {
        log.info("sendREST_POST: start");
        ObjectMapper mapper = new ObjectMapper();
        String str = mapper.writeValueAsString(obj);	
   
        StringEntity strEntity = new StringEntity(str, "UTF-8");
        strEntity.setContentType("application/json");
        HttpPost post = new HttpPost(uri);
        post.setEntity(strEntity);
        //
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = httpClient.execute(post);
        try {
            HttpEntity entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() != 200) {
                log.info("sendREST_POST : error!");
                return null;
            }
            InputStream is = entity.getContent();
            ClassifierOutputData[] result = mapper.readValue(is, ClassifierOutputData[].class);
            log.info("sendREST_POST: result = " + result[0].getNewmap().get(0).getTopic());
            return result;
        }

        finally {
            response.close();
            httpClient.close();
        }
    }     
*/    
    
    private ClassifierOutputData[] sendREST_POST(ClassifierInputData input) throws Exception {
        RestTemplate restTemplate = new RestTemplate();     
        ResponseEntity<ClassifierOutputData[]> response;
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.valueOf("application/json;charset=UTF-8"));   
        
        HttpEntity<ClassifierInputData> entity = new HttpEntity<>(input, requestHeaders);
        try {
            response = restTemplate.postForEntity(urlTextAnalizer, entity, ClassifierOutputData[].class);
            log.info("response.getBody() = " + response.getBody());
            return response.getBody();
        }
        catch (HttpStatusCodeException e) {
            log.info(e.getResponseBodyAsString());
            return null;
        }         
    }    
    
    private List<String> getPossibleInfinitives(String str) {
        char lastChar = str.charAt(str.length() - 1);
        char prevLastChar = str.charAt(str.length() - 2);
        if (lastChar == 'а') {
            return Arrays.asList(
                    str.substring(0, str.length() - 1) + 'о',
                    str.substring(0, str.length() - 1)
            );
        } else if (lastChar == 'ы') {
            return Collections.singletonList(str.substring(0, str.length() - 1) + 'а');
        } else if (lastChar == 'и') {
            return Arrays.asList(
                    str.substring(0, str.length() - 1) + 'я',
                    str.substring(0, str.length() - 1) + 'ь'
            );
        } else if (lastChar == 'е') {
            return Arrays.asList(
                    str.substring(0, str.length() - 1) + 'а',
                    str.substring(0, str.length() - 1) + 'я',
                    str.substring(0, str.length() - 1) + 'о',
                    str.substring(0, str.length() - 1) + 'е',
                    str.substring(0, str.length() - 1) + 'ь'
            );
        } else if (lastChar == 'я') {
            return Collections.singletonList(str.substring(0, str.length() - 1) + 'ь');
        } else if (lastChar == 'ю') {
            return Arrays.asList(
                    str.substring(0, str.length() - 1) + 'е',
                    str.substring(0, str.length() - 1) + 'я',
                    str.substring(0, str.length() - 1) + 'ь'
            );
        } else if (lastChar == 'у') {
            return Arrays.asList(
                    str.substring(0, str.length() - 1) + 'а',
                    str.substring(0, str.length() - 1) + 'о',
                    str.substring(0, str.length() - 1)
            );
        } else if (lastChar == 'й') {
            if (prevLastChar == 'о') {
                return Collections.singletonList(str.substring(0, str.length() - 2) + 'а');
            } else if (prevLastChar == 'е') {
                return Collections.singletonList(str.substring(0, str.length() - 2) + 'я');
            }
        } else if (lastChar == 'м') {
            if (prevLastChar == 'о') {
                return Arrays.asList(
                        str.substring(0, str.length() - 2) + 'о',
                        str.substring(0, str.length() - 2)
                );
            } else if (prevLastChar == 'e') {
                return Collections.singletonList(str.substring(0, str.length() - 2) + 'e');
            } else if (prevLastChar == 'ё') {
                return Collections.singletonList(str.substring(0, str.length() - 1) + 'ь');
            }
        }
        return Collections.emptyList();
    }

    public String getTextPO() {
        return textPO;
    }

    public ArrayList<String> getAbbrList() {
        return abbrList;
    }
    
    public void clearAbbrList() {
        abbrList.clear();
    }
    
    public static HashMap<String, String> getClassesMappingDict() {
        return classesMappingDict;
    }
}
