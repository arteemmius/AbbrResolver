/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import ru.textanalysis.abbrresolver.model.abbr.FullTextInputData;
/**
 *
 * @author asamokhin
 */
public class TestRestClientAbbr_2 {
    private static final Logger log = LoggerFactory.getLogger(TestRestClientAbbr_2.class.getName()); 
    public static void main(String[] args) throws Exception {
        FullTextInputData input = new FullTextInputData();

        RestTemplate restTemplate = new RestTemplate();
        //BufferedReader reader0 = new BufferedReader(new InputStreamReader(new FileInputStream("d:/modeluper/DocForTest/Авиация и космонавтика/0088c87ceb034ff81a0cb71238168b8a.xml.txt"), "UTF-8"));
        byte[] array = Files.readAllBytes(Paths.get("d:/modeluper/DocForTest/Авиация и космонавтика/0088c87ceb034ff81a0cb71238168b8a.xml.txt"));
        String text = new String(array, "UTF-8");
        log.info(text); 
        
        input.setText(text);
        input.setCheckGetAbbr(true);
        input.setCheckPO(false);
        //input.setPO("Авиация и космонавтика");
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.valueOf("application/json;charset=UTF-8"));  
        HttpEntity<FullTextInputData> entity = new HttpEntity<>(input, requestHeaders);
        ResponseEntity<String> response = null;

         try {
            response = restTemplate.postForEntity("http://localhost:8090/AbbrResolver-1.0/fullText", entity, String.class);
         }
         catch (HttpStatusCodeException e) {
             log.info(e.getResponseBodyAsString());
             return;
         }
                try(FileWriter writer = new FileWriter("c:/utils/log.txt", false))
                {
                    writer.write(response.getBody());
                    writer.flush();
                }
                catch(IOException ex){

                    log.info(ex.getMessage());
                } 

        log.info("finish successful!");

    }        
   
}