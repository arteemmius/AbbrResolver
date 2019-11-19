package ru.textanalysis.abbrresolver.beans;

import java.util.HashMap;
import java.util.Map;

public class ShortType {

    public static final int ABBREVIATION = 0;
    public static final int TRUNCATION = 1;
    public static final int CONTRACTION = 2;
    public static final int COMMON = 3;

    private static final Map<Integer, String> VALUES = new HashMap<>();

    static {
        VALUES.put(ABBREVIATION, "Аббревиатура");
        VALUES.put(TRUNCATION, "Усечение");
        VALUES.put(CONTRACTION, "Cтягивание");
        VALUES.put(COMMON, "Общепринятое");
    }

    public static String getValue(int key) {
        return VALUES.get(key);
    }

}
