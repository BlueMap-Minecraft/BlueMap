package de.bluecolored.bluemap.common.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class HttpRequest {

    private String path = null;
    private Map<String, String> getParams = null;
    private String getParamString = null;

    public abstract String getMethod();

    public abstract String getAddress();

    public abstract String getVersion();

    public abstract Map<String, Set<String>> getHeader();

    public abstract Map<String, Set<String>> getLowercaseHeader();

    public abstract Set<String> getHeader(String key);

    public abstract Set<String> getLowercaseHeader(String key);

    public String getPath() {
        if (path == null) parseAddress();
        return path;
    }

    public Map<String, String> getGETParams() {
        if (getParams == null) parseAddress();
        return Collections.unmodifiableMap(getParams);
    }

    public String getGETParamString() {
        if (getParamString == null) parseAddress();
        return getParamString;
    }

    protected void parseAddress() {
        String address = this.getAddress();
        if (address.isEmpty()) address = "/";
        String[] addressParts = address.split("\\?", 2);
        String path = addressParts[0];
        this.getParamString = addressParts.length > 1 ? addressParts[1] : "";

        Map<String, String> getParams = new HashMap<>();
        for (String getParam : this.getParamString.split("&")){
            if (getParam.isEmpty()) continue;
            String[] kv = getParam.split("=", 2);
            String key = kv[0];
            String value = kv.length > 1 ? kv[1] : "";
            getParams.put(key, value);
        }

        this.path = path;
        this.getParams = getParams;
    }

    public abstract InputStream getData();

    static HttpRequest read(InputStream in) throws IOException {
        return OriginalHttpRequest.read(in);
    }

}
