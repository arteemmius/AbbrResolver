package ru.textanalysis.abbrresolver.realization;

import ru.textanalysis.abbrresolver.beans.Descriptor;
import ru.textanalysis.abbrresolver.beans.DescriptorType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class PatternFinder {

    private static final Map<Integer, List<Pattern>> PATTERNS = new HashMap<>();
    private static final Set<Integer> DESCRIPTORS_WITH_DOT_IN_END = new HashSet<>(Arrays.asList(DescriptorType.FIO, DescriptorType.SHORT_WORD, DescriptorType.CUT_WORD));
    //private static final Set<Integer> DESCRIPTORS_WITH_DOT_IN_END = new HashSet<>(Arrays.asList(DescriptorType.FIO, DescriptorType.SHORT_WORD));
    
    private static final Logger log = Logger.getLogger(PatternFinder.class.getName());    

    static {
        PATTERNS.put(DescriptorType.FIO, Arrays.asList(
                Pattern.compile("[А-Я][А-Яа-я]+ [А-Я]\\.[A-Я]\\."),
                Pattern.compile("[А-Я]\\.[А-Я]\\. ?[А-Я][А-Яа-я]+"))
        );
        PATTERNS.put(DescriptorType.SHORT_WORD, Arrays.asList(
                //Pattern.compile("\\b[а-я]+\\.([а-я]+\\.)?"),      //Сокр. усечение
                Pattern.compile("[A-ЯЁA-Z]{2,}"),                     //абревиатура
                Pattern.compile("[А-Яа-я.]+-[а-я]+"),                //Сокр.стяжение
                Pattern.compile("[а-я.]+\\/[а-я]+"))                //общепринятое            
        ); 
        PATTERNS.put(DescriptorType.CUT_WORD, Arrays.asList(
                //Pattern.compile("\\b[а-я]+\\.([а-я]+\\.)?\\s[а-я]*"))      //Сокр. усечение
                Pattern.compile("\\b[а-я]+\\.([А-Яа-я]\\s{0,})?"))      //Сокр. усечение
        );      
        PATTERNS.put(DescriptorType.EMAIL, Collections.singletonList(Pattern.compile("[a-zA-Z1-9\\-\\._]+@[a-z1-9]+(.[a-z1-9]+){1,}")));
        PATTERNS.put(DescriptorType.NUM_SEQ, Collections.singletonList(Pattern.compile("\\d+[.,]\\d+")));
    }

    public List<Descriptor> getDescriptors(String text) {
        log.info("Start getDescriptors()");         
        log.info("text= " + text);   
        DescriptorType type = new DescriptorType();
        List<Descriptor> result = new ArrayList<>();
        Descriptor curDescriptor;
        for (Map.Entry<Integer, List<Pattern>> patternEntry : PATTERNS.entrySet()) {
            int descriptorType = patternEntry.getKey();
            log.info("descriptorType= " + descriptorType);             
            boolean mayStayInEnd = DESCRIPTORS_WITH_DOT_IN_END.contains(descriptorType);
            for (Pattern pattern : patternEntry.getValue()) {
                Matcher m = pattern.matcher(text);
                while (m.find()) {
                    
                    // если работаем с усечением
                    if(descriptorType == type.CUT_WORD) {
                    log.info("CutString= " + text.substring(m.start(), m.start() + text.substring(m.start(), m.end()).indexOf('.') + 1)); 
                    curDescriptor = new Descriptor(descriptorType, m.start(), (m.start() + text.substring(m.start(), m.end()).indexOf('.') + 1 - m.start()), text.substring(m.start(), m.start() + text.substring(m.start(), m.end()).indexOf('.') + 1));
                    } 
                    else {
                    // если с любым другим видом сокращения
                    curDescriptor = new Descriptor(descriptorType, m.start(), (m.end() - m.start()), text.substring(m.start(), m.end())); 
                    }
                    log.info("text.substring(m.start(), m.end()= " + text.substring(m.start(), m.end()));   
                    log.info("type= " + descriptorType);                      
                    curDescriptor.setMayStayInEnd(mayStayInEnd);
                    result.add(curDescriptor);
                }
            }
        }

        result.sort(Comparator.comparing(Descriptor::getStartPos));

        //поиск пересечения дескрипторов - занял Иванов В.И. При оформлении
        Descriptor prevDescriptor = null;
        Iterator<Descriptor> iter = result.iterator();
        while (iter.hasNext()) {
            curDescriptor = iter.next();
            if (prevDescriptor != null && curDescriptor.getStartPos() < (prevDescriptor.getStartPos() + prevDescriptor.getLength())) {
                iter.remove();
            } else {
                prevDescriptor = curDescriptor;
            }
        }
        log.info("result_short= " + result.toString());
        log.info("getDescriptors() success commplete");    
        return result;
    }

}
