package ru.textanalysis.abbrresolver.utils;


import java.io.*;
import java.nio.charset.Charset;
import ru.textanalysis.abbrresolver.beans.Item;
import ru.textanalysis.abbrresolver.realization.utils.DBManager;

public class ImportDb extends Importer{

    @Override
    public void doImport(InputStream is, String filePath) throws Exception {
        DBManager dbManager = DBManager.getInstance();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("windows-1251")))) {
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
}
