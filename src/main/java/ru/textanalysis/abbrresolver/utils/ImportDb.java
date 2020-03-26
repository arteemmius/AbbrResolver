package ru.textanalysis.abbrresolver.utils;


import java.io.*;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.textanalysis.abbrresolver.pojo.Item;
import ru.textanalysis.abbrresolver.run.utils.DBManager;

public class ImportDb extends Importer{
    private static final Logger log = LoggerFactory.getLogger(ImportDb.class.getName()); 
    @Override
    public void doImport(InputStream is, String filePath) throws Exception {
        DBManager dbManager = DBManager.getInstance();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("windows-1251")))) {
            log.info("Начат импорт файла '" + filePath + "'.");
            Item item;
            while ((item = readItem(reader)) != null) {
                dbManager.addItem(item);
            }
            log.info("Импорт завершен.");
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found!", e);
        }
    }
}
