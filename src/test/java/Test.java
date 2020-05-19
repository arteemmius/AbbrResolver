/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ru.textanalysis.tawt.ms.external.sp.BearingPhraseExt;
import ru.textanalysis.tawt.sp.api.SyntaxParser;

/**
 *
 * @author artee
 */
public class Test {
    public static void main(String[] args) throws Exception {
        //поймали последнее слово в предложении
        Pattern pattern0 = Pattern.compile("([А-Яа-я]+)(\\.{1,3}|!|\\?)");
        //поймали последнее слово в предложении но перед словом стоят знаки пунктуации
        Pattern pattern1 = Pattern.compile("([-\"])([А-Яа-я]+)(\\.{1,3}|!|\\?)");     
        //поймали не последнее слово в предложении со знаком препинания сзади
        Pattern pattern2 = Pattern.compile("([А-Яа-я]+)(,|:|\"|-)");   
        //поймали не последнее слово в предложении со знаком препинания с обеих сторон
        Pattern pattern3 = Pattern.compile("([-\"])([А-Яа-я]+)(,|:|\"|-)");   
        Pattern pattern = Pattern.compile("[,\\.\\-\\?\\!\\:\"]");
        Matcher m = pattern.matcher(":");
        //System.out.println(m.matches());        
        //System.out.println(m.group());
        String sent = "Солнце село за село.";
      System.out.println(sent + ":");
      SyntaxParser sp = new SyntaxParser();
      List<BearingPhraseExt> exts = sp.getTreeSentenceWithoutAmbiguity(sent);
      exts.forEach(bearingPhraseExt -> {
          System.out.println(bearingPhraseExt.getMainOmoForms());
      });
    }    
}
