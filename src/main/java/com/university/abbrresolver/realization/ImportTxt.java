package com.university.abbrresolver.realization;

import com.university.abbrresolver.beans.Item;

import java.io.*;
import java.nio.charset.Charset;

public class ImportTxt {

    private static final String DELIMITER = ";";

    public void doImport(InputStream is, String filePath) throws Exception {
        DBManager dbManager = DBManager.getInstance();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("utf-8")))) {
            System.out.println("Начат импорт файла '" + filePath + "'.");
            Item item;
            while ((item = readItem(reader)) != null) {
                dbManager.addItem(item);
            }
            System.out.println("Импорт завершен.");
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found!", e);
        }
    }

    private Item readItem(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line != null && !line.isEmpty()) {
            String[] pieces = line.split(DELIMITER);
            if (pieces.length > 0) {
                Item Item = new Item();
                Item.setWord(pieces[0]);
                Item.setDefinition(pieces.length > 1 ? pieces[1] : null);
                Item.setDescription(pieces.length > 2 ? pieces[2] : null);
                Item.setType(pieces.length > 3 ? Utils.parseInt(pieces[3]) : Integer.valueOf(99));
                return Item;
            }
        }
        return null;
    }
}
