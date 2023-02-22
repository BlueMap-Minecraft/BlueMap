package de.bluecolored.bluemap.common.web.http;

import java.util.*;

public class HttpHeader {

    private final String key;
    private final String value;
    private List<String> values;
    private Set<String> valuesLC;

    public HttpHeader(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public List<String> getValues() {
        if (values == null) {
            values = new ArrayList<>();
            for (String v : value.split(",")) {
                values.add(v.trim());
            }
        }

        return values;
    }

    public boolean contains(String value) {
        if (valuesLC == null) {
            valuesLC = new HashSet<>();
            for (String v : getValues()) {
                valuesLC.add(v.toLowerCase(Locale.ROOT));
            }
        }

        return valuesLC.contains(value);
    }

}
