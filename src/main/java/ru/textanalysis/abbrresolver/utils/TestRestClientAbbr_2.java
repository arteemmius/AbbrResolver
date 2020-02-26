/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.textanalysis.abbrresolver.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import ru.textanalysis.abbrresolver.abbrmodel.FullTextInputData;
/**
 *
 * @author asamokhin
 */
public class TestRestClientAbbr_2 {
    public static void main(String[] args) throws Exception {
        FullTextInputData input = new FullTextInputData();

        RestTemplate restTemplate = new RestTemplate();
        //BufferedReader reader0 = new BufferedReader(new InputStreamReader(new FileInputStream("d:/modeluper/DocForTest/Авиация и космонавтика/0088c87ceb034ff81a0cb71238168b8a.xml.txt"), "UTF-8"));
        byte[] array = Files.readAllBytes(Paths.get("d:/modeluper/DocForTest/Авиация и космонавтика/0088c87ceb034ff81a0cb71238168b8a.xml.txt"));
        String text = new String(array, "UTF-8");
        System.out.println(text); 
        
        input.setText(text);
        input.setCheckGetAbbr(true);
        input.setCheckPO(true);
        //input.setPO("Авиация и космонавтика");
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.valueOf("application/json;charset=UTF-8"));  
        HttpEntity<FullTextInputData> entity = new HttpEntity<>(input, requestHeaders);
        ResponseEntity<String> response = null;

         try {
            response = restTemplate.postForEntity("http://localhost:8090/AbbrResolver-1.0/fullText", entity, String.class);
         }
         catch (HttpStatusCodeException e) {
             System.out.println(e.getResponseBodyAsString());
             return;
         }
                try(FileWriter writer = new FileWriter("c:/utils/log.txt", false))
                {
                    writer.write(response.getBody());
                    writer.flush();
                }
                catch(IOException ex){

                    System.out.println(ex.getMessage());
                } 

        System.out.println("finish successful!");

    }        
   
}