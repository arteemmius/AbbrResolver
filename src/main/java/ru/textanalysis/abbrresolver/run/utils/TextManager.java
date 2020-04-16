package ru.textanalysis.abbrresolver.run.utils;


import com.ibm.icu.text.BreakIterator;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.textanalysis.abbrresolver.pojo.Descriptor;
import ru.textanalysis.abbrresolver.pojo.DescriptorType;
import ru.textanalysis.abbrresolver.pojo.Sentence;
import ru.textanalysis.abbrresolver.run.AbbrResolver;

public class TextManager {

    //Unicodes https://www.fileformat.info/info/unicode/category/Po/list.htm
    private static final Set<Character> SPACE_CHAR = new HashSet<>(Arrays.asList('\u0020', '\n', '\r', '\t'));   // LF, CR, HT
    private static final Set<Character> PUNCTUATION_CHAR = new HashSet<>(Arrays.asList('\u002C', '\u003B', '\u003A', '\u0021'/*, '\u002E', '\u003F', '\u2026'*/));     // , ; :  (! . ? ...)
    private static final Set<Character> SENTENCE_END = new HashSet<>(Arrays.asList('\u002E', '\u0021', '\u003F', '\u2026'));   // . ! ? ...
    private static final Set<Character> RUSSIAN_LEX = new HashSet<>();
    private static final Set<Character> FOREIGN_LEX = new HashSet<>();

    private static final List<Character> OPEN_BRACKET = Arrays.asList('\u0028', '\u005B', '\u007B');
    private static final List<Character> CLOSE_BRACKET = Arrays.asList('\u0029', '\u005D', '\u007D');
    private static final List<Character> OPEN_QUOTE = Arrays.asList('\u201C', '\u00AB');
    private static final List<Character> CLOSE_QUOTE = Arrays.asList('\u201D', '\u00BB');

    private static final Logger log = LoggerFactory.getLogger(TextManager.class.getName());
    
    static {
        for (char i = 'А'; i <= 'я'; i++) {
            RUSSIAN_LEX.add(i);
        }
        for (char i = 'A'; i <= 'z'; i++) {
            FOREIGN_LEX.add(i);
        }
    }

    private final PatternFinder patternFinder;
    private final AbbrResolver abbrResolver;
    private final DBManager dictionary;
    
    //регулярные выражения для сокращений
    private final String regexpAbbr = "[A-ЯЁA-Z]{2,}";
    private final String regexpStej = "[А-Яа-я.]+-[а-я]+";
    private final String regexpUsech = "\\b[а-я]+\\.";

    //DbDictionary
    public TextManager(PatternFinder patternFinder, AbbrResolver abbrResolver) {
        this.patternFinder = patternFinder;
        this.abbrResolver = abbrResolver;
        this.dictionary = DBManager.getInstance();
    }

    
    public List<Sentence> splitText(String text, JPanel findAbbrPanel) throws Exception {
        List<Sentence> temp = splitText(text);
        boolean caseNextWord = true;
        abbrResolver.clearAbbrList();
        abbrResolver.clearAbbrListWithoutDesc();
        for(int i = 0; i < temp.size(); i++) {
            log.info("temp.size() = " + temp.size());
            String words[] = temp.get(i).getContent().replaceAll("\\s+", " ").split(" ");
            List<Descriptor> sentenceDesc = new ArrayList<>();
            int index = 0;
            log.info("wordssize() = " + words.length);

            if (i != temp.size() - 1) {
                if (temp.get(i + 1).getContent().length() > 0) {
                      int ind = 0;
                      while (" ".equals(temp.get(i + 1).getContent().substring(ind, ind + 1))) {
                         ind = ind + 1;
                      }
                      caseNextWord = temp.get(i + 1).getContent().substring(ind, ind + 1).matches("[А-Я]");
                }
                else
                    caseNextWord = false;
            }
            
            if (words.length > 0) {
                boolean checkCase;
                for (int j = 0; j < words.length; j++) {
                    if ("".equals(words[j]) || " ".equals(words[j])) continue;
                    if (j != words.length - 1)
                        checkCase = false;
                    else
                        checkCase = caseNextWord;
                    if (!checkWordType(words[j], sentenceDesc, j, checkCase)) {
                        Descriptor desc = new Descriptor(DescriptorType.RUSSIAN_LEX, j, words[j].length(), words[j]);
                        sentenceDesc.add(desc);
                    }
                    
                }
            }

            for (int j = 0; j < sentenceDesc.size(); j++)      {  
                 log.info("sentenceDescDesc_" + i + " = " + sentenceDesc.get(j).getDesc());
                 log.info("sentenceDescValue_" + i + " = " + sentenceDesc.get(j).getValue());
                 log.info("sentenceDescType_" + i + " = " + sentenceDesc.get(j).getType());                 
            }            
            temp.get(i).getDescriptors().addAll(sentenceDesc);
        }
          
        for(int i = 0; i < temp.size(); i++)      {  
             log.info("0temp.get(i).getContent()= " + temp.get(i).getContent());
             log.info("0temp.get(i).getDescriptors()= " + temp.get(i).getDescriptors());            
        }
        abbrResolver.getAbbrList().addAll(abbrResolver.getAbbrListWithoutDesc());
        return temp;
    }

    private boolean checkWordType(String s, List<Descriptor> sentenceDescm, int j, boolean caseNextWord) throws Exception {
        log.info("s = " + s + "; caseNextWord = " + caseNextWord);
        //нашли ФИО
        List<Pattern> pList0 = PatternFinder.PATTERNS.get(DescriptorType.FIO);
        for (Pattern pList01 : pList0) {
            Matcher emath = pList01.matcher(s);
            if (emath.matches()) {
                Descriptor desc0 = new Descriptor(DescriptorType.FIO, j, s.length(), s);
                sentenceDescm.add(desc0);
                return true;
            }
        }
        //нашли почту
        List<Pattern> pList1 = PatternFinder.PATTERNS.get(DescriptorType.EMAIL);
        for (Pattern pList11 : pList1) {
            Matcher emath = pList11.matcher(s);
            if (emath.matches()) {
                Descriptor desc0 = new Descriptor(DescriptorType.EMAIL, j, s.length(), s);
                sentenceDescm.add(desc0);
                return true;
            }
        }  
        //последовательность чисел
        List<Pattern> pList2 = PatternFinder.PATTERNS.get(DescriptorType.NUM_SEQ);
        for (Pattern pList21 : pList2) {
            Matcher emath = pList21.matcher(s);
            if (emath.matches()) {
                Descriptor desc0 = new Descriptor(DescriptorType.NUM_SEQ, j, s.length(), s);
                sentenceDescm.add(desc0);
                return true;
            }
        }
        //нашли аббревиатуру
        Pattern patternAbbr = Pattern.compile(regexpAbbr);
        Matcher matchAbbr = patternAbbr.matcher(s);
        if (matchAbbr.matches()) {
                Descriptor desc0 = new Descriptor(DescriptorType.RUSSIAN_LEX, j, s.length(), s);
                sentenceDescm.add(setDesc(desc0));
                return true;
        }
        //нашли стяжение
        Pattern patternSt = Pattern.compile(regexpStej);
        Matcher matchSt = patternSt.matcher(s);
        if (matchSt.matches()) {
                Descriptor desc0 = new Descriptor(DescriptorType.RUSSIAN_LEX, j, s.length(), s);
                sentenceDescm.add(setDesc(desc0));
                return true;
        }
        
        //[SAM:K414] тестирование показало сокращения часто пишутся после первого употребления полной формы в скобках
        Pattern patternP = Pattern.compile("(.*[\\(])(" + regexpAbbr + "|" + regexpStej + "|" + regexpUsech + ")([\\)].*)");
        Matcher matchP = patternP.matcher(s);
        if (matchP.matches()) {
            //первая скобка
            Descriptor desc0 = new Descriptor(DescriptorType.PUNCTUATION_CHAR, j, matchP.group(1).length(), matchP.group(1));
            //вторая скобка
            Descriptor desc2 = new Descriptor(DescriptorType.PUNCTUATION_CHAR, j + 2, matchP.group(3).length(), matchP.group(3));
            //содержимое скобок
            Descriptor desc1 = new Descriptor(DescriptorType.RUSSIAN_LEX, j + 1, matchP.group(2).length(), matchP.group(2));
            if (!caseNextWord) {
                desc1 = setTypeAndDesc(desc1, regexpUsech);
                if (desc1.getType() != DescriptorType.RUSSIAN_LEX) {
                    sentenceDescm.add(desc0);
                    sentenceDescm.add(desc1);
                    sentenceDescm.add(desc2);
                    return true;
                }
            }
            //[SAM:K416] нашли стяжение
            desc1 = setTypeAndDesc(desc1, regexpStej);
            if (desc1.getType() != DescriptorType.RUSSIAN_LEX) {
                sentenceDescm.add(desc0);
                sentenceDescm.add(desc1);
                sentenceDescm.add(desc2);
                return true;
            }            
            
            //[SAM:K412] нашли аббревиатуру
            desc1 = setTypeAndDesc(desc1, regexpAbbr);
            sentenceDescm.add(desc0);
            sentenceDescm.add(desc1);
            sentenceDescm.add(desc2);
            return true;
        }
        //поймали последнее слово в предложении или сокращение
        Pattern pattern0 = Pattern.compile("([А-Яа-я]+)(\\.{1,3}|!|\\?)");
        Matcher m0 = pattern0.matcher(s);
        if (m0.matches()) {
            if (!caseNextWord) {
                Descriptor desc = new Descriptor(DescriptorType.RUSSIAN_LEX, j, m0.group().length(), m0.group());
                desc = setTypeAndDesc(desc, regexpUsech);
                if (desc.getType() != DescriptorType.RUSSIAN_LEX) {
                    sentenceDescm.add(desc);
                    return true;
                }
            }
            //[SAM:K416] нашли стяжение
            Descriptor desc = new Descriptor(DescriptorType.RUSSIAN_LEX, j, m0.group().length(), m0.group());
            desc = setTypeAndDesc(desc, regexpStej + "\\.");
            if (desc.getType() != DescriptorType.RUSSIAN_LEX) {
                sentenceDescm.add(desc);
                return true;
            }
            
            Descriptor desc0 = new Descriptor(DescriptorType.RUSSIAN_LEX, j, m0.group(1).length(), m0.group(1));
            //[SAM:K412] пересчитываем тип и заполняем расшифровку, если это необходимо
            desc0 = setTypeAndDesc(desc0, regexpAbbr);
            Descriptor desc1 = new Descriptor(DescriptorType.SENTENCE_END, j + 1, m0.group(2).length(), m0.group(2));
            sentenceDescm.add(desc0);
            sentenceDescm.add(desc1);
            return true;
        }
        //поймали последнее слово в предложении или сокращение но перед словом стоят знаки пунктуации
        Pattern pattern1 = Pattern.compile("([-\"])([А-Яа-я]+)(\\.{1,3}|!|\\?)");   
        Matcher m1 = pattern1.matcher(s);
        if (m1.matches()) {
            if (!caseNextWord) {
                Descriptor desc = new Descriptor(DescriptorType.RUSSIAN_LEX, j, m1.group().length(), m1.group());
                desc = setTypeAndDesc(desc, regexpUsech);
                if (desc.getType() != DescriptorType.RUSSIAN_LEX) {
                    sentenceDescm.add(desc);
                    return true;
                }
            }            
            Descriptor desc0 = new Descriptor(DescriptorType.PUNCTUATION_CHAR, j, m1.group(1).length(), m1.group(1));
            Descriptor desc1 = new Descriptor(DescriptorType.RUSSIAN_LEX, j + 1, m1.group(2).length(), m1.group(2));            
            //[SAM:K412] пересчитываем тип и заполняем расшифровку, если это необходимо
            desc1 = setTypeAndDesc(desc1, regexpAbbr);  
            //[SAM:K416] нашли стяжение
            if (desc1.getType() == DescriptorType.RUSSIAN_LEX) 
                desc1 = setTypeAndDesc(desc1, regexpStej);
            
            Descriptor desc2 = new Descriptor(DescriptorType.SENTENCE_END, j + 2, m1.group(3).length(), m1.group(3));
            sentenceDescm.add(desc0);
            sentenceDescm.add(desc1);
            sentenceDescm.add(desc2);
            return true;
        }        
        //поймали не последнее слово в предложении со знаком препинания сзади
        Pattern pattern2 = Pattern.compile("([А-Яа-я]+)(;|,|:|\"|-)");  
        Matcher m2 = pattern2.matcher(s);
        if (m2.matches()) {
            Descriptor desc0 = new Descriptor(DescriptorType.RUSSIAN_LEX, j, m2.group(1).length(), m2.group(1));
            //[SAM:K412] пересчитываем тип и заполняем расшифровку, если это необходимо
            desc0 = setTypeAndDesc(desc0, regexpAbbr);   
            //[SAM:K416] нашли стяжение
            if (desc0.getType() == DescriptorType.RUSSIAN_LEX) 
                desc0 = setTypeAndDesc(desc0, regexpStej);
                        
            Descriptor desc1 = new Descriptor(DescriptorType.PUNCTUATION_CHAR, j + 1, m2.group(2).length(), m2.group(2));
            sentenceDescm.add(desc0);
            sentenceDescm.add(desc1);
            return true;
        }          
        //поймали не последнее слово в предложении со знаком препинания с обеих сторон
        Pattern pattern4 = Pattern.compile("([-\"])([А-Яа-я]+)(;|,|:|\"|-)");       
        Matcher m3 = pattern4.matcher(s);        
        if (m3.matches()) {
            Descriptor desc0 = new Descriptor(DescriptorType.PUNCTUATION_CHAR, j, m3.group(1).length(), m3.group(1));
            Descriptor desc1 = new Descriptor(DescriptorType.RUSSIAN_LEX, j + 1, m3.group(2).length(), m3.group(2));
            //[SAM:K412] пересчитываем тип и заполняем расшифровку, если это необходимо
            desc1 = setTypeAndDesc(desc1, regexpAbbr);
            //[SAM:K416] нашли стяжение
            if (desc1.getType() == DescriptorType.RUSSIAN_LEX) 
                desc1 = setTypeAndDesc(desc1, regexpStej);            
            Descriptor desc2 = new Descriptor(DescriptorType.PUNCTUATION_CHAR, j + 2, m3.group(3).length(), m3.group(3));
            sentenceDescm.add(desc0);
            sentenceDescm.add(desc1);
            sentenceDescm.add(desc2);
            return true;            
        }   
        //пришел знак препинания
        Pattern pattern5 = Pattern.compile("[;,\\.\\-\\?\\!\\:\"]");       
        Matcher m4 = pattern5.matcher(s);        
        if (m4.matches()) {
            Descriptor desc0 = new Descriptor(DescriptorType.PUNCTUATION_CHAR, j, m4.group().length(), m4.group());
            sentenceDescm.add(desc0);
            return true;            
        }
        return false;
    }
    
    //[SAM:K412] для вычисления типа слова в частных случаях функции checkWordType
    private Descriptor setTypeAndDesc(Descriptor abbr, String regExp) throws Exception {
        boolean check = checkRegExp(abbr.getValue(), regExp);
        String textPO = abbrResolver.getTextPO();
        List<String> longForm = null;
        if (check) {
            if (textPO != null)
                longForm = dictionary.findAbbrLongFormsWithMainWord(abbr.getValue(), textPO, abbrResolver.getClassesMappingDict());
            if (longForm == null || longForm.isEmpty())
                longForm = dictionary.findAbbrLongForms(abbr.getValue());
            if (longForm != null && longForm.size() > 0) {
                abbr.setType(DescriptorType.SHORT_WORD);
                abbr.setDesc(longForm.get(0));                  
            }
            String toAbbrList;
            if (abbr.getDesc() != null) {
                toAbbrList = abbr.getValue() + " : " + abbr.getDesc();
                if(!abbrResolver.getAbbrList().contains(toAbbrList) &&
                        !abbr.getValue().equals(abbr.getDesc())) {
                    abbrResolver.getAbbrList().add(toAbbrList);
                }                 
            }
            else {
                toAbbrList = abbr.getValue() + " : упс, расшифровки нет в словаре, но мы это скоро исправим :)";
                if(!abbrResolver.getAbbrListWithoutDesc().contains(toAbbrList) &&
                        !abbr.getValue().equals(abbr.getDesc())) {
                    abbrResolver.getAbbrListWithoutDesc().add(toAbbrList);
                }          
            }                 
        }
        return abbr;
    }
    
    //[SAM:K412] для вычисления типа слова в частных случаях функции checkWordType
    private Descriptor setDesc(Descriptor abbr) throws Exception {
        String textPO = abbrResolver.getTextPO();
        List<String> longForm = null;
        if (textPO != null)
            longForm = dictionary.findAbbrLongFormsWithMainWord(abbr.getValue(), textPO, abbrResolver.getClassesMappingDict());
        if (longForm == null || longForm.isEmpty())
            longForm = dictionary.findAbbrLongForms(abbr.getValue());
        if (longForm != null && longForm.size() > 0) {
            abbr.setType(DescriptorType.SHORT_WORD);
            abbr.setDesc(longForm.get(0));               
        }
        String toAbbrList;
        if (abbr.getDesc() != null) {
            toAbbrList = abbr.getValue() + " : " + abbr.getDesc();
            if(!abbrResolver.getAbbrList().contains(toAbbrList) &&
                    !abbr.getValue().equals(abbr.getDesc())) {
                abbrResolver.getAbbrList().add(toAbbrList);
            }                 
        }
        else {
            toAbbrList = abbr.getValue() + " : упс, расшифровки нет в словаре, но мы это скоро исправим :)";
            if(!abbrResolver.getAbbrListWithoutDesc().contains(toAbbrList) &&
                    !abbr.getValue().equals(abbr.getDesc())) {
                abbrResolver.getAbbrListWithoutDesc().add(toAbbrList);
            }          
        }
        
        return abbr;
    }    
        
    //[SAM:K412] для проверки аббревиатур при наличии рядом с ними знаков препинания
    private boolean checkRegExp(String text, String regExp) {
        Pattern pattern = Pattern.compile(regExp);       
        Matcher m = pattern.matcher(text);     
        return m.matches(); 
    }
    
    private void fillShortWords(String text, List<Descriptor> supposedShortWords, JPanel findAbbrPanel) throws Exception {
        abbrResolver.fillAbbrDescriptions(text, dictionary, supposedShortWords, findAbbrPanel);
        Set<String> notFounded = new HashSet<>();
        for (Descriptor descriptor : supposedShortWords) {
            if (descriptor.getDesc() != null && !descriptor.getDesc().isEmpty()) {
                descriptor.setType(DescriptorType.SHORT_WORD);
            } else {
                descriptor.setType(DescriptorType.RUSSIAN_LEX);
                notFounded.add(descriptor.getValue());
            }
        }
        if (!notFounded.isEmpty()) {
            log.info("Not founded " + notFounded.size() + " words: " + Arrays.toString(notFounded.toArray()));
        }
    }

  
    private List<Descriptor> getSupposedCommonShortWords(List<Sentence> sentences) {
        List<Descriptor> resList = new ArrayList<>();
        for (Sentence sentence : sentences) {
            int needAdd = 0;
            for (Descriptor curDesc : sentence.getDescriptors()) {
                if (Objects.equals(curDesc.getType(), DescriptorType.NUM_SEQ)) {
                    needAdd = 2;
                } else if (needAdd > 0) {
                    needAdd--;
                    if (Objects.equals(curDesc.getType(), DescriptorType.RUSSIAN_LEX) || Objects.equals(curDesc.getType(), DescriptorType.FOREIGN_LEX)) {
                        resList.add(curDesc);
                    }
                }
            }
        }
        return resList;
    }
    
    //[SAM:K323] жутко работает, лучше отказаться
    private List<Sentence> splitText(String text, List<Descriptor> descriptors) {

        List<Sentence> sentences = new ArrayList<>();        
        //sentence attributes
        int sentenceIndex = 1;
        Sentence sentence = new Sentence();
        int sentenceStartPos = 0;
        int sentenceEndPos = text.length() - 1;
        boolean foundEnd = false;

        //word attributes
        char ch;
        int wordStartPos = 0;
        boolean hasRussianLex = false;
        boolean hasDigit = false;
        boolean hasForeignLex = false;

        //descriptor
        int desciptorIndex = descriptors.isEmpty() ? -1 : 0;
        Descriptor curDescriptor = descriptors.isEmpty() ? null : descriptors.get(desciptorIndex);

        //vars
        Stack<Character> pairsStack = new Stack<>();
        int searchPos = -1;

        for (int i = 0; i < text.length(); i++) {
            ch = text.charAt(i);
            if (foundEnd) {
                if (SPACE_CHAR.contains(ch)) {
                    continue;
                } else if (Character.isUpperCase(ch) && pairsStack.isEmpty() && !hasIntersection(i, descriptors)) {
                    if (wordStartPos != i) {
                        sentence.addDescriptor(new Descriptor(getDescriptorType(hasRussianLex, hasDigit, hasForeignLex), wordStartPos, sentenceEndPos - wordStartPos, text.substring(wordStartPos, sentenceEndPos)));
                        log.info("0text.substring(wordStartPos, sentenceEndPos)= " + text.substring(wordStartPos, sentenceEndPos));
                        hasRussianLex = false;
                        hasDigit = false;
                        hasForeignLex = false;
                    }

                    wordStartPos = i;

                    if (RUSSIAN_LEX.contains(ch)) {
                        hasRussianLex = true;
                    } else if (Character.isDigit(ch)) {
                        hasDigit = true;
                    } else if (FOREIGN_LEX.contains(ch)) {
                        hasForeignLex = true;
                    }

                    sentence.setIndexInText(sentenceIndex++);
                    sentence.setStartPos(sentenceStartPos);
                    sentence.setLength(sentenceEndPos - sentenceStartPos);
                    sentence.setContent(text.substring(sentenceStartPos, sentenceEndPos));

                    sentences.add(sentence);

                    sentence = new Sentence();
                    foundEnd = false;
                    sentenceStartPos = i;
                    sentenceEndPos = text.length();
                } else {
                    foundEnd = false;
                }
            } else if (SPACE_CHAR.contains(ch)) {
                if (wordStartPos != i) {
                    sentence.addDescriptor(new Descriptor(getDescriptorType(hasRussianLex, hasDigit, hasForeignLex), wordStartPos, i - wordStartPos, text.substring(wordStartPos, i)));
                    log.info("1text.substring(wordStartPos, sentenceEndPos)= " + text.substring(wordStartPos, i));
                    hasRussianLex = false;
                    hasDigit = false;
                    hasForeignLex = false;
                }
                wordStartPos = i + 1;
            } 
            
            else if (curDescriptor != null && i == curDescriptor.getStartPos()) {    //натолкнулись на ранее найденный дескриптор
                sentence.addDescriptor(curDescriptor);
                log.info("curDescriptor= " + curDescriptor);

                hasRussianLex = false;
                hasDigit = false;
                hasForeignLex = false;

                i += curDescriptor.getLength() - 1;
                wordStartPos = i + 1;
                desciptorIndex++;
                curDescriptor = desciptorIndex >= descriptors.size() ? null : descriptors.get(desciptorIndex);
            }
            
              else if (RUSSIAN_LEX.contains(ch)) {
                hasRussianLex = true;
            } else if (Character.isDigit(ch)) {
                hasDigit = true;
            } else if (FOREIGN_LEX.contains(ch)) {
                hasForeignLex = true;
            } else if (OPEN_BRACKET.contains(ch)) {
                if (wordStartPos != i) {
                    sentence.addDescriptor(new Descriptor(getDescriptorType(hasRussianLex, hasDigit, hasForeignLex), wordStartPos, i - wordStartPos, text.substring(wordStartPos, i)));
                    log.info("2text.substring(wordStartPos, sentenceEndPos)= " + text.substring(wordStartPos, i));
                    hasRussianLex = false;
                    hasDigit = false;
                    hasForeignLex = false;
                }
                sentence.addDescriptor(new Descriptor(DescriptorType.OPEN_BRACKET, i, 1, Character.toString(ch)));
                wordStartPos = i + 1;
                pairsStack.add(ch);
            } else if (OPEN_QUOTE.contains(ch)) {
                if (wordStartPos != i) {
                    sentence.addDescriptor(new Descriptor(getDescriptorType(hasRussianLex, hasDigit, hasForeignLex), wordStartPos, i - wordStartPos, text.substring(wordStartPos, i)));
                    log.info("3text.substring(wordStartPos, sentenceEndPos)= " + text.substring(wordStartPos, i));                    
                    hasRussianLex = false;
                    hasDigit = false;
                    hasForeignLex = false;
                }
                sentence.addDescriptor(new Descriptor(DescriptorType.OPEN_QUOTE, i, 1, Character.toString(ch)));
                wordStartPos = i + 1;
                pairsStack.add(ch);
            } else if ((searchPos = CLOSE_BRACKET.indexOf(ch)) >= 0) {
                if (wordStartPos != i) {
                    sentence.addDescriptor(new Descriptor(getDescriptorType(hasRussianLex, hasDigit, hasForeignLex), wordStartPos, i - wordStartPos, text.substring(wordStartPos, i)));
                    log.info("4text.substring(wordStartPos, sentenceEndPos)= " + text.substring(wordStartPos, i));                    
                    hasRussianLex = false;
                    hasDigit = false;
                    hasForeignLex = false;
                }
                sentence.addDescriptor(new Descriptor(DescriptorType.CLOSE_BRACKET, i, 1, Character.toString(ch)));
                wordStartPos = i + 1;
                //rem from stack
                if (!pairsStack.isEmpty() && OPEN_BRACKET.indexOf(pairsStack.peek()) == searchPos) {
                    pairsStack.pop();
                    foundEnd = false;
                }
            } else if ((searchPos = CLOSE_QUOTE.indexOf(ch)) >= 0) {
                if (wordStartPos != i) {
                    sentence.addDescriptor(new Descriptor(getDescriptorType(hasRussianLex, hasDigit, hasForeignLex), wordStartPos, i - wordStartPos, text.substring(wordStartPos, i)));
                    log.info("5text.substring(wordStartPos, sentenceEndPos)= " + text.substring(wordStartPos, i));                    
                    hasRussianLex = false;
                    hasDigit = false;
                    hasForeignLex = false;
                }
                sentence.addDescriptor(new Descriptor(DescriptorType.CLOSE_QUOTE, i, 1, Character.toString(ch)));
                wordStartPos = i + 1;
                //rem from stack
                if (!pairsStack.isEmpty() && OPEN_QUOTE.indexOf(pairsStack.peek()) == searchPos) {
                    pairsStack.pop();
                    foundEnd = false;
                }
            } else if (PUNCTUATION_CHAR.contains(ch)) {
                if (wordStartPos != i) {
                    sentence.addDescriptor(new Descriptor(getDescriptorType(hasRussianLex, hasDigit, hasForeignLex), wordStartPos, i - wordStartPos, text.substring(wordStartPos, i)));
                    log.info("6text.substring(wordStartPos, sentenceEndPos)= " + text.substring(wordStartPos, i));                    
                    hasRussianLex = false;
                    hasDigit = false;
                    hasForeignLex = false;
                }
                sentence.addDescriptor(new Descriptor(DescriptorType.PUNCTUATION_CHAR, i, 1, Character.toString(ch)));
                wordStartPos = i + 1;
            } else if (SENTENCE_END.contains(ch)) {
                log.info("find_end_pos= " + ch);                   
                sentenceEndPos = i;
                foundEnd = true;
            }
        }

        if (wordStartPos < text.length() - 1) {
            sentence.addDescriptor(new Descriptor(getDescriptorType(hasRussianLex, hasDigit, hasForeignLex), wordStartPos, (text.length() - 1) - wordStartPos, text.substring(wordStartPos, (text.length() - 1))));
            log.info("1text.substring(wordStartPos, sentenceEndPos)= " + text.substring(wordStartPos, (text.length() - 1)));            
        }

        sentence.setIndexInText(sentenceIndex++);
        sentence.setStartPos(sentenceStartPos);
        sentence.setLength(sentenceEndPos - sentenceStartPos);
        sentence.setContent(text.substring(sentenceStartPos, sentenceEndPos));
        sentences.add(sentence);

        return sentences;
    }
    
    //[SAM:K324] инициализируем объекты Sentence, поля: списки слов и предложения 
    private List<Sentence> splitText(String text) { 
        List<Sentence> sentences = new ArrayList<>();
        PatternFinder patternFinderSentence = new PatternFinder();
        Locale rus = new Locale("ru", "RU");
        BreakIterator iterator = BreakIterator.getSentenceInstance(rus);
        iterator.setText(text);
        int start = iterator.first();
        for (int end = iterator.next();
            end != BreakIterator.DONE;
            start = end, end = iterator.next()) {
                log.info(text.substring(start,end));
                Sentence sentence = new Sentence();
                sentence.setContent(text.substring(start,end));
                sentence.setStartPos(start);
                sentences.add(sentence);
        }        
        return sentences;
    }

  private List<String> getAllWords(final String preparedString) {
    final List<String> words = new ArrayList<>();
    Locale rus = new Locale("ru", "RU");    
    final BreakIterator breakIterator = BreakIterator.getWordInstance(rus);
    breakIterator.setText(preparedString);
    int start = breakIterator.first();

    for (int end = breakIterator.next(); end != BreakIterator.DONE; start = end, end = breakIterator.next()) {
        words.add(preparedString.substring(start, end));
    }
    return words;
 }


    private int getDescriptorType(boolean hasRussianLex, boolean hasDigit, boolean hasForeignLex) {
        if (hasRussianLex && !hasDigit && !hasForeignLex) {
            return DescriptorType.RUSSIAN_LEX;
        } else if (!hasRussianLex && hasDigit && !hasForeignLex) {
            return DescriptorType.NUM_SEQ;
        } else if (!hasRussianLex && !hasDigit && hasForeignLex) {
            return DescriptorType.FOREIGN_LEX;
        } else {
            return DescriptorType.OTHER_LEX;
        }
    }

    private boolean hasIntersection(int pos, List<Descriptor> descriptors) {
        if (descriptors.isEmpty() || pos < descriptors.get(0).getStartPos()) {
            return false;
        } else {
            for (Descriptor descriptor : descriptors) {
                if (descriptor.getStartPos() > pos) {
                    return false;
                } else if (pos > descriptor.getStartPos() && pos <= (descriptor.getStartPos() + descriptor.getLength())) {    //точка - участник дескриптора
                    return true;
                }
            }
        }
        return false;
    }

}
