package ru.textanalysis.abbrresolver.realization;

import ru.textanalysis.abbrresolver.beans.Descriptor;
import ru.textanalysis.abbrresolver.beans.DescriptorType;
import ru.textanalysis.abbrresolver.beans.Sentence;

import java.util.*;
import javax.swing.JPanel;
import org.apache.log4j.Logger;

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

    private static final Logger log = Logger.getLogger(TextManager.class.getName());
    
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

    //DbDictionary
    public TextManager(PatternFinder patternFinder, AbbrResolver abbrResolver) {
        this.patternFinder = patternFinder;
        this.abbrResolver = abbrResolver;
        this.dictionary = DBManager.getInstance();
    }

    
    public List<Sentence> splitText(String text, JPanel findAbbrPanel) throws Exception {

        //поиск всех дескрипторов, чтобы потом правильно определить границы предложений
        List<Descriptor> allPatternDescriptors = patternFinder.getDescriptors(text);
        log.info("0allPatternDescriptors= " + allPatternDescriptors.toString());
        

        //сокращения из паттерна
        fillShortWords(text, Utils.filter(allPatternDescriptors, descriptor -> Objects.equals(descriptor.getType(), DescriptorType.SHORT_WORD)), findAbbrPanel);
        fillShortWords(text, Utils.filter(allPatternDescriptors, descriptor -> Objects.equals(descriptor.getType(), DescriptorType.CUT_WORD)), findAbbrPanel);
        
        log.info("1allPatternDescriptors= " + allPatternDescriptors.toString());
        
        List<Sentence> temp = splitText(text, Utils.filter(allPatternDescriptors, descriptor -> !Objects.equals(descriptor.getType(), DescriptorType.RUSSIAN_LEX)));
        //общепринятые сокращения без точки
        fillShortWords(text, getSupposedCommonShortWords(temp), findAbbrPanel);
        
        return temp;
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
            System.out.println("Not founded " + notFounded.size() + " words: " + Arrays.toString(notFounded.toArray()));
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
