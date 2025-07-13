package de.bluecolored.bluemap.core.util;

import java.util.concurrent.ConcurrentHashMap;

public class StringUtil {

    private static final ConcurrentHashMap<String, String> STRING_INTERN_POOL = new ConcurrentHashMap<>();

    /**
     * Using our own function instead of {@link String#intern()} since the ConcurrentHashMap is much faster.
     */
    public static String intern(String string) {
        String interned = STRING_INTERN_POOL.putIfAbsent(string, string);
        return interned != null ? interned : string;
    }

}
