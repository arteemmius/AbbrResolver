/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.university.abbrresolver.frame;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.io.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import java.nio.file.Files;
import java.nio.file.Paths;
/**
 *
 * @author asamokhin
 */
public class TestRestClient_2 {
    public static void main(String[] args) throws Exception {
InputData input = new InputData();
ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();

RestTemplate restTemplate = new RestTemplate();
BufferedReader reader0 = new BufferedReader(new InputStreamReader(new FileInputStream("d:\\modeluper\\DocForTest\\Астрономия\\0231737bae6d321179db98b04c3c9a53.xml.txt"), "UTF-8"));
String text = "";
String line;
while ((line = reader0.readLine()) != null) {
    text = text + line;
    text = text + "\n";
} 
input.setText(text);
HttpEntity<InputData> entity = new HttpEntity<InputData>(input);
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
