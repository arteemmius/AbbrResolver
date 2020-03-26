/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.textanalysis.abbrresolver.frame;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.textanalysis.abbrresolver.model.abbr.AbbrListInputData;
import ru.textanalysis.abbrresolver.model.abbr.AbbrListOutputData;
import ru.textanalysis.abbrresolver.run.AbbrResolver;

/**
 *
 * @author artee
 */

@Component       
@RestController
public class ControllerGetAbbrList {
    @Value("${classifier.run}")
    private boolean runTextAnalizer;
    @Value("${classifier.url}")
    private String urlTextAnalizer;
    
    @RequestMapping(value = "/AbbrList", method = {RequestMethod.POST}, consumes = {"application/json"})
    public AbbrListOutputData getAbbrList(@RequestBody AbbrListInputData input) throws Exception{
        AbbrListOutputData output = new AbbrListOutputData();
        AbbrResolver abbrResolver = new AbbrResolver(input.getText(), input.getPO(), input.isCheckPO(), runTextAnalizer, urlTextAnalizer);
        output.setAbbrList(abbrResolver.getAbbrList());
        abbrResolver.clearAbbrList();
        return output;
    }    
}
