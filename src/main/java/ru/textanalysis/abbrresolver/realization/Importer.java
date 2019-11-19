package ru.textanalysis.abbrresolver.realization;

import ru.textanalysis.abbrresolver.beans.Item;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

public abstract class Importer {

    private static final String DELIMITER = ";";

    public abstract void doImport(InputStream is, String filePath) throws Exception;

    protected Item readItem(BufferedReader reader) throws IOException {
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
