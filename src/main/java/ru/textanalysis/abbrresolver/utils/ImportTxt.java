package ru.textanalysis.abbrresolver.utils;


import java.io.*;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.textanalysis.abbrresolver.pojo.Item;
import ru.textanalysis.abbrresolver.run.utils.DBManager;
import ru.textanalysis.abbrresolver.run.utils.Utils;

public class ImportTxt {

    private static final String DELIMITER = ";";
    private static final Logger log = LoggerFactory.getLogger(ImportTxt.class.getName()); 
    public void doImport(InputStream is, String filePath) throws Exception {
        DBManager dbManager = DBManager.getInstance();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("utf-8")))) {
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
