package com.university.abbrresolver.realization;

import com.university.abbrresolver.beans.Descriptor;
import com.university.abbrresolver.beans.DescriptorType;
import com.university.abbrresolver.beans.Item;
import com.university.abbrresolver.beans.Sentence;
import com.university.abbrresolver.frame.AbbrPanel;
import com.university.abbrresolver.frame.MainWindow;
import com.university.abbrresolver.realization.TextManager;
import grammeme.MorfologyParameters;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.*;
import java.util.logging.Level;
import javax.swing.JPanel;
import jmorfsdk.JMorfSdk;
import morphologicalstructures.OmoForm;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import storagestructures.OmoFormList;
import org.springframework.stereotype.Component;

public class AbbrResolver {

    private static final List<Byte> VERB_TYPES = Arrays.asList(MorfologyParameters.TypeOfSpeech.VERB, MorfologyParameters.TypeOfSpeech.INFINITIVE);
    private static final Set<String> GENITIVE_PREPOSITIONS = new HashSet<>(Arrays.asList("без", "у", "до", "от", "с", "около", "из", "возле", "после", "для", "вокруг"));
    private static final Set<String> DATIVE_PREPOSITIONS = new HashSet<>(Arrays.asList("к", "по"));
    private static final Set<String> ACCUSATIVE_PREPOSITIONS = new HashSet<>(Arrays.asList("в", "за", "на", "про", "через"));
    private static final Set<String> ABLTIVE_PREPOSITIONS = new HashSet<>(Arrays.asList("за", "над", "под", "перед", "с"));
    private static final Set<String> PREPOSITIONA_PREPOSITIONS = new HashSet<>(Arrays.asList("в", "на", "о", "об", "обо", "при"));
    
    private static final Logger log = Logger.getLogger(AbbrResolver.class.getName());    
    private static String textPO = null;
    private String text;
    private boolean runTextAnalizer;
    private static String urlTextAnalizer;
    
    public AbbrResolver(String text, boolean runTextAnalizer, String urlTextAnalizer) {
        this.text = text;
        this.runTextAnalizer = runTextAnalizer;
        AbbrResolver.urlTextAnalizer = urlTextAnalizer;
    }    
     
    public void fillAbbrDescriptions(String text, DBManager dictionary, List<Descriptor> descriptors, JPanel findAbbrPanel) throws Exception {
        log.info("Start fillAbbrDescriptions()");
        if (textPO == null && runTextAnalizer)
            textPO = runClassifier(text);
        System.out.println("textPO = " + textPO);
        System.out.println("runTextAnalizer = " + runTextAnalizer);
        for (Descriptor curDescriptor : descriptors) {
            List<String> longForms = null;
            if (textPO != null)
                longForms = dictionary.findAbbrLongFormsWithMainWord(curDescriptor.getValue(), textPO);
            if (longForms == null)
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
                curDescriptor.setDesc(longForms.get(0));           //пока берется первое попавшееся значение аббревиатуры
            }else{
                curDescriptor.setDesc(curDescriptor.getValue());
            }
            Item item = new Item();
            if (!properties.isEmpty()) {
                item.setWord(properties.get(0));
                item.setDefinition(properties.get(1));
                item.setDescription("\t"); 
            }
            System.out.println(item.getWord() + "\t" + item.getDefinition());
            AbbrPanel abbrPanel = new AbbrPanel();
            abbrPanel.addAbbrPanel(item.getWord(), item.getDefinition(), item.getDescription());
            //findAbbrPanel.add(abbrPanel);
            //findAbbrPanel.repaint();
            //findAbbrPanel.revalidate();
        }
        log.info("fillAbbrDescriptions() success complete");           
    }

    public String resolveAcronyms(JMorfSdk jMorfSdk, Sentence sentence) throws Exception {

        List<Descriptor> descriptors = sentence.getDescriptors(); //разделение текста на слова и знаки препинания
        Descriptor curDescriptor;
        log.info("descriptors= " + descriptors);        
        String copy = sentence.getContent(); //копируем все слова в переменную copy
        String[] acronymWords;
        log.info("copy= " + copy);  
        
        for (int i = 0; i < descriptors.size(); i++) {
                curDescriptor = descriptors.get(i);
                log.info("i-ый элемент= " + curDescriptor);      
                log.info("i-ый элемент_start_pos= " + curDescriptor.getStartPos());                    
                if (Objects.equals(curDescriptor.getType(), DescriptorType.SHORT_WORD) || Objects.equals(curDescriptor.getType(), DescriptorType.CUT_WORD)) {        
               // if (Objects.equals(curDescriptor.getType(), DescriptorType.SHORT_WORD)) {        
                log.info("curDescriptor.getType()= " + curDescriptor.getType());                  
                log.info("Нашел сокращение= " + descriptors.get(i));                
                if(Objects.equals(curDescriptor.getType(), DescriptorType.SHORT_WORD)) {
                    acronymWords = curDescriptor.getDesc().split(" "); //делим сокращение, состоящее из нескольких слов на части  
                }
                else {
                    acronymWords = curDescriptor.getDesc().split("");
                }
                log.info("curDescriptor.getDesc().split(\" \")= " + curDescriptor.getDesc().split(" ").toString());                                 
                log.info("1acronymWords= " + String.join(",", acronymWords));               
                boolean[] capitalizeWords = new boolean[acronymWords.length];

                //save acronym case
                for (int j = 0; j < acronymWords.length; j++) {
                    String word = acronymWords[j];
                    log.info("word= " + word);                     
                    if (Character.isUpperCase(word.charAt(0))) {
                        acronymWords[j] = Utils.uncapitalize(word);
                        capitalizeWords[j] = true;
                    }
                }
                log.info("2acronymWords= " + String.join(",", acronymWords)); 
                int mainWordAcronymIndex = getMainWordAcronymIndex(acronymWords, jMorfSdk); //getMainWordAcronymIndex - индекс главного слова внутри сокращения
                log.info("mainWordAcronymIndex= " + mainWordAcronymIndex);
                Integer collacationMainWordIndex = getMainWordIndex(descriptors, i, acronymWords[mainWordAcronymIndex], jMorfSdk);
                log.info("collacationMainWordIndex= " + collacationMainWordIndex);                
                if (collacationMainWordIndex != null) {
                    String collMainWord = descriptors.get(collacationMainWordIndex).getValue();
                    log.info("collMainWord= " + collMainWord);                    
                    Integer prepositionIndex = getPrepositionIndex(descriptors, i, collacationMainWordIndex, jMorfSdk);
                    log.info("prepositionIndex= " + prepositionIndex);                     
                    String prepositionWord = prepositionIndex != null ? descriptors.get(prepositionIndex).getValue() : "";
                    log.info("prepositionWord= " + prepositionWord);                     
                    acronymWords[mainWordAcronymIndex] = getTrueAcronymForm(acronymWords[mainWordAcronymIndex], collMainWord, prepositionWord, jMorfSdk);
                    log.info("acronymWords[mainWordAcronymIndex]= " + acronymWords[mainWordAcronymIndex]);  
                } else{
                    //acronymWords[mainWordAcronymIndex];
                }
                
                
                
                if (acronymWords.length > 1) {
                    adaptAcronymWords(acronymWords, mainWordAcronymIndex, jMorfSdk);
                }

                //restore acronym case
                for (int j = 0; j < acronymWords.length; j++) {
                    if (capitalizeWords[j]) {
                        acronymWords[j] = Utils.capitalize(acronymWords[j]);
                    }
                }

                copy = copy.replace(curDescriptor.getValue(), Utils.concat(" ", Arrays.asList(acronymWords)));      //replaceAll заменить
            } else{
                
            }
        }
        return copy;
    }

    /**
     * Определяет главное слово в полной форме сокращения, возвращает индекс главного слова внутри сокращения
     */
    private int getMainWordAcronymIndex(String[] acronymWords, JMorfSdk jMorfSdk) throws Exception {
        log.info("Start getMainWordAcronymIndex()");       
        log.info("acronymWords.length= " + acronymWords.length);           
        if (acronymWords.length > 1) {
            OmoFormList omoForms;        
            OmoForm omoForm;        
            for (int i = 0; i < acronymWords.length; i++) {                   
                if (!(jMorfSdk.getAllCharacteristicsOfForm(acronymWords[i])).isEmpty()) {
                    omoForms = jMorfSdk.getAllCharacteristicsOfForm(acronymWords[i]);
                    omoForm = omoForms.get(0);
                    log.info("omoForms= " + omoForms);  
                    log.info("omoForm= " + omoForm);  
                    log.info("omoForm.getTypeOfSpeech()= " + omoForm.getTypeOfSpeech());  
                    log.info("MorfologyParameters.TypeOfSpeech.NOUN= " + MorfologyParameters.TypeOfSpeech.NOUN);  
                    log.info("omoForm.getTheMorfCharacteristics(MorfologyParameters.Case.class= " + omoForm.getTheMorfCharacteristics(MorfologyParameters.Case.class));  
                    log.info("MorfologyParameters.Case.NOMINATIVE= " + MorfologyParameters.Case.NOMINATIVE);                    
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
        log.info("Start getMainWordIndex()"); 
        OmoFormList omoForms = jMorfSdk.getAllCharacteristicsOfForm(acronym);
        log.info("omoForms= " + omoForms);
        if (omoForms.isEmpty()) {
            return null;
        }
        byte acronymTypeOfSpeech = omoForms.get(0).getTypeOfSpeech();
        log.info("acronymTypeOfSpeech= " + acronymTypeOfSpeech);
        int wordIndex = acronymIndex;

        if (Arrays.asList(MorfologyParameters.TypeOfSpeech.ADJECTIVEFULL, MorfologyParameters.TypeOfSpeech.ADJECTIVESHORT, MorfologyParameters.TypeOfSpeech.NOUNPRONOUN,
                MorfologyParameters.TypeOfSpeech.PARTICIPLE, MorfologyParameters.TypeOfSpeech.PARTICIPLEFULL, MorfologyParameters.TypeOfSpeech.NUMERAL).contains(acronymTypeOfSpeech)) {
            //определяющее слово впереди
            while (++wordIndex < descriptors.size()) {
                Descriptor curDescriptor = descriptors.get(wordIndex);
                log.info("1curDescriptor= " + curDescriptor);  
                if (Objects.equals(curDescriptor.getType(), DescriptorType.RUSSIAN_LEX) && !Character.isUpperCase(curDescriptor.getValue().charAt(0))) {
                    OmoForm wordOmoForm = null;
                    if (!jMorfSdk.getAllCharacteristicsOfForm(curDescriptor.getValue()).isEmpty())
                        wordOmoForm = jMorfSdk.getAllCharacteristicsOfForm(curDescriptor.getValue()).get(0);
                    else
                        return null;
                    if (wordOmoForm.getTypeOfSpeech() == MorfologyParameters.TypeOfSpeech.NOUN) {
                        log.info("1wordIndex= " + wordIndex);                        
                        return wordIndex;
                    }
                }
            }
        } else if (acronymTypeOfSpeech == MorfologyParameters.TypeOfSpeech.NOUN) {
            //главное слово позади
            while (--wordIndex >= 0) {
                Descriptor curDescriptor = descriptors.get(wordIndex);
                log.info("2curDescriptor= " + curDescriptor);                  
                if (Objects.equals(curDescriptor.getType(), DescriptorType.RUSSIAN_LEX) && !Character.isUpperCase(curDescriptor.getValue().charAt(0))) {
                    OmoForm wordOmoForm;
                    if (!jMorfSdk.getAllCharacteristicsOfForm(curDescriptor.getValue()).isEmpty())
                        wordOmoForm = jMorfSdk.getAllCharacteristicsOfForm(curDescriptor.getValue()).get(0);
                    else
                        return null;
                    if (wordOmoForm.getTypeOfSpeech() == MorfologyParameters.TypeOfSpeech.NOUN
                            || VERB_TYPES.contains(wordOmoForm.getTypeOfSpeech())) {
                        log.info("2wordIndex= " + wordIndex);                         
                        return wordIndex;
                    }
                } else if (Objects.equals(curDescriptor.getType(), DescriptorType.NUM_SEQ) && acronymIndex - wordIndex < 3) {
                    OmoFormList omoFormList = jMorfSdk.getAllCharacteristicsOfForm(curDescriptor.getValue());
                    if (!omoFormList.isEmpty() && omoFormList.get(0).getTypeOfSpeech() == MorfologyParameters.TypeOfSpeech.NUMERAL) {
                        log.info("3wordIndex= " + wordIndex);                         
                        return wordIndex;
                    }
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
            if (Objects.equals(curDescriptor.getType(), DescriptorType.RUSSIAN_LEX) && !Character.isUpperCase(curDescriptor.getValue().charAt(0))) {
                OmoForm wordOmoForm = jMorfSdk.getAllCharacteristicsOfForm(curDescriptor.getValue()).get(0);
                if (wordOmoForm.getTypeOfSpeech() == MorfologyParameters.TypeOfSpeech.PRETEXT) { 
                    return wordIndex;
                }
            }
        }
        return null;
    }

    /**
     * Согласует сокращение с главным словом в предложении
     */
    private String getTrueAcronymForm(String acronymMainWord, String collacationMainWord, String preposition, JMorfSdk jMorfSdk) throws Exception {
        OmoForm acronymOmoForm = jMorfSdk.getAllCharacteristicsOfForm(acronymMainWord).get(0);
        byte acronymTypeOfSpeech = acronymOmoForm.getTypeOfSpeech();
        long acronymNumber = acronymOmoForm.getTheMorfCharacteristics(MorfologyParameters.Numbers.IDENTIFIER);

        OmoForm mainWordOmoForm = jMorfSdk.getAllCharacteristicsOfForm(collacationMainWord.toLowerCase()).get(0);
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
                e.printStackTrace();
                return acronymMainWord;
            }            
            removeIf(matchList, MorfologyParameters.Numbers.class, mainWordNumbers, jMorfSdk);
            //removeIf(jMorfSdk, matchList, MorfologyParameters.Gender.class, mainWordGender);       //мужской - средний род (противоположном направлении, противоположном ключе)
            return matchList.get(0);
        } else if (acronymTypeOfSpeech == MorfologyParameters.TypeOfSpeech.NOUN && VERB_TYPES.contains(mainWordTypeOfSpeech)) {
            //model 2:  глаг. (главн.) + сокр. (сущ.)
            if (preposition != null && !preposition.isEmpty()) {
                long prepositionCase = getCaseByPreposition(preposition);
                List<String> matchList = jMorfSdk.getDerivativeForm(acronymMainWord, prepositionCase);
                removeIf(matchList, MorfologyParameters.Numbers.class, mainWordNumbers, jMorfSdk);
                return matchList.get(0);
            } else {
                long transitivity = mainWordOmoForm.getTheMorfCharacteristics(MorfologyParameters.Transitivity.class);  //переходный
                if (transitivity == MorfologyParameters.Transitivity.TRAN) {
                    List<String> matchList = jMorfSdk.getDerivativeForm(acronymMainWord, MorfologyParameters.Case.ACCUSATIVE);  //винительный
                    removeIf(matchList, MorfologyParameters.Numbers.class, mainWordNumbers, jMorfSdk);
                    return matchList.get(0);
                } else if (transitivity == MorfologyParameters.Transitivity.INTR) {
                    //глагол непереходный. Что делать?
                }
            }
        } else if (acronymTypeOfSpeech == MorfologyParameters.TypeOfSpeech.NOUN && mainWordTypeOfSpeech == MorfologyParameters.TypeOfSpeech.NOUN) {
            //model 3:  сущ. (главн.) + сокр. (сущ.)
            if (preposition != null && !preposition.isEmpty()) {
                long prepositionCase = getCaseByPreposition(preposition);
                List<String> matchList = jMorfSdk.getDerivativeForm(acronymMainWord, prepositionCase);
                removeIf(matchList, MorfologyParameters.Numbers.class, MorfologyParameters.Numbers.SINGULAR, jMorfSdk);
                return matchList.get(0);
            } else {
                List<String> matchList = null;
                try {
                    matchList = jMorfSdk.getDerivativeForm(acronymMainWord, MorfologyParameters.Case.GENITIVE);
                }
                catch(Exception e) {
                    e.printStackTrace();
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
                    List<String> matchList = jMorfSdk.getDerivativeForm(acronymMainWord, MorfologyParameters.Case.GENITIVE);
                    removeIf(matchList, MorfologyParameters.Numbers.class, MorfologyParameters.Numbers.SINGULAR, jMorfSdk);
                    return matchList.get(0);

                }else {
                    List<String> matchList = null;
                    try {
                        matchList = jMorfSdk.getDerivativeForm(acronymMainWord, MorfologyParameters.Case.GENITIVE);
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                        return acronymMainWord;
                    }                    
                    removeIf(matchList, MorfologyParameters.Numbers.class, MorfologyParameters.Numbers.PLURAL, jMorfSdk);
                    
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
        OmoForm acronymOmoForm = jMorfSdk.getAllCharacteristicsOfForm(acronymWords[mainWordAcronymIndex]).get(0);
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
                OmoForm wordOmoForm = jMorfSdk.getAllCharacteristicsOfForm(curWord).get(0);
                if (wordOmoForm.getTypeOfSpeech() == MorfologyParameters.TypeOfSpeech.NOUN) {
                    lastNounNumbers = wordOmoForm.getTheMorfCharacteristics(MorfologyParameters.Numbers.IDENTIFIER);
                    lastNounGender = wordOmoForm.getTheMorfCharacteristics(MorfologyParameters.Gender.IDENTIFIER);
                }
                if ((i != mainWordAcronymIndex) && (wordOmoForm.getTypeOfSpeech() != MorfologyParameters.TypeOfSpeech.PRETEXT) && (wordOmoForm.getTheMorfCharacteristics(MorfologyParameters.Case.IDENTIFIER) == MorfologyParameters.Case.NOMINATIVE)) {
                    String initialForm = jMorfSdk.getAllCharacteristicsOfForm(curWord).get(0).getInitialFormString();
                    List<String> matchList = jMorfSdk.getDerivativeForm(initialForm, acronymCase);
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
                    e.printStackTrace();
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

    public static String runClassifier(String text) throws Exception {
        System.out.println("text = " + text);
        HashMap<String,  Double>  newmap = new HashMap<>();
        InputData input = new InputData();
        RestTemplate restTemplate = new RestTemplate();
        byte[] array;
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        array = text.getBytes();
        outputBuffer.write(array);        
        ResponseEntity<String> response;
        ArrayList<String> classifier = new ArrayList<>();
        classifier.add("MYLTI_CLASSIFIER");
        input.setClassifier(classifier);
        input.setModel("DOC2VEC");
        HttpEntity<InputData> entity = new HttpEntity<InputData>(input);
        String textStream = new String(outputBuffer.toByteArray(), "UTF-8");
        input.setText(textStream);
        try {
            response = restTemplate.postForEntity(urlTextAnalizer, entity, String.class);
            System.out.println("response.getBody() = " + response.getBody());
            return getTopClassifierResult(getClassifierResult(response.getBody())).toLowerCase();
        }
        catch (HttpStatusCodeException e) {
            System.out.println(e.getResponseBodyAsString());
            return null;
        }  
    }
    
    public static HashMap<String, Double> getClassifierResult(String json) throws Exception {
        JSONParser parser = new JSONParser();
        JSONObject json0 = (JSONObject) parser.parse(json);
        String json1 = json0.get("MYLTI_CLASSIFIER").toString();
        JSONObject json2 = (JSONObject) parser.parse(json1);
        return json2;    
    }     
    
    public static String getTopClassifierResult(HashMap<String,  Double>  map) {
        Object[] list = map.entrySet().stream()
            .sorted(HashMap.Entry.<String,  Double>comparingByValue().reversed()).toArray();
            String str = list[0].toString();
            return str.substring(0, list[0].toString().lastIndexOf("="));
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
}
