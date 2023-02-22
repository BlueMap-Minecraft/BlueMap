/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.common.web;

import de.bluecolored.bluemap.common.web.http.*;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class FileRequestHandler implements HttpRequestHandler {

    private final Path webRoot;
    private final File emptyTileFile;

    public FileRequestHandler(Path webRoot) {
        this.webRoot = webRoot.normalize();
        this.emptyTileFile = webRoot.resolve("assets").resolve("emptyTile.json").toFile();
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        if (!request.getMethod().equalsIgnoreCase("GET"))
            return new HttpResponse(HttpStatusCode.BAD_REQUEST);
        return generateResponse(request);
    }

    private HttpResponse generateResponse(HttpRequest request) {
        String path = request.getPath();

        // normalize path
        if (path.startsWith("/")) path = path.substring(1);
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);

        Path filePath;
        try {
            filePath = webRoot.resolve(path);
        } catch (InvalidPathException e){
            return new HttpResponse(HttpStatusCode.NOT_FOUND);
        }

        // check if file is in web-root
        if (!filePath.normalize().startsWith(webRoot)){
            return new HttpResponse(HttpStatusCode.FORBIDDEN);
        }

        File file = filePath.toFile();

        // redirect to have correct relative paths
        if (file.isDirectory() && !request.getPath().endsWith("/")) {
            HttpResponse response = new HttpResponse(HttpStatusCode.SEE_OTHER);
            response.addHeader("Location", "/" + path + "/" + (request.getGETParamString().isEmpty() ? "" : "?" + request.getGETParamString()));
            return response;
        }

        // default to index.html
        if (!file.exists() || file.isDirectory()){
            file = new File(filePath + "/index.html");
        }

        // send empty tile-file if tile not exists
        if (!file.exists() && file.toPath().startsWith(webRoot.resolve("maps"))){
            file = emptyTileFile;
        }

        if (!file.exists() || file.isDirectory()) {
            return new HttpResponse(HttpStatusCode.NOT_FOUND);
        }

        // don't send php files
        if (file.getName().endsWith(".php")) {
            return new HttpResponse(HttpStatusCode.FORBIDDEN);
        }

        // check if file is still in web-root and is not a directory
        if (!file.toPath().normalize().startsWith(webRoot) || file.isDirectory()){
            return new HttpResponse(HttpStatusCode.FORBIDDEN);
        }

        // check modified
        long lastModified = file.lastModified();
        HttpHeader modHeader = request.getHeader("If-Modified-Since");
        if (modHeader != null){
            try {
                long since = stringToTimestamp(modHeader.getValue());
                if (since + 1000 >= lastModified){
                    return new HttpResponse(HttpStatusCode.NOT_MODIFIED);
                }
            } catch (IllegalArgumentException ignored){}
        }

        //check ETag
        String eTag = Long.toHexString(file.length()) + Integer.toHexString(file.hashCode()) + Long.toHexString(lastModified);
        HttpHeader etagHeader = request.getHeader("If-None-Match");
        if (etagHeader != null){
            if(etagHeader.getValue().equals(eTag)) {
                return new HttpResponse(HttpStatusCode.NOT_MODIFIED);
            }
        }

        //create response
        HttpResponse response = new HttpResponse(HttpStatusCode.OK);
        response.addHeader("ETag", eTag);
        if (lastModified > 0) response.addHeader("Last-Modified", timestampToString(lastModified));
        response.addHeader("Cache-Control", "public");
        response.addHeader("Cache-Control", "max-age=" + TimeUnit.HOURS.toSeconds(1));

        //add content type header
        String filetype = file.getName();
        int pointIndex = filetype.lastIndexOf('.');
        if (pointIndex >= 0) filetype = filetype.substring(pointIndex + 1);
        String contentType = toContentType(filetype);
        response.addHeader("Content-Type", contentType);

        //send response
        try {
            response.setData(new FileInputStream(file));
            return response;
        } catch (FileNotFoundException e) {
            return new HttpResponse(HttpStatusCode.NOT_FOUND);
        }
    }

    private static String timestampToString(long time){
        return DateFormatUtils.format(time, "EEE, dd MMM yyy HH:mm:ss 'GMT'", TimeZone.getTimeZone("GMT"), Locale.ENGLISH);
    }

    private static long stringToTimestamp(String timeString) throws IllegalArgumentException {
        try {
            int day = Integer.parseInt(timeString.substring(5, 7));

            int month = Calendar.JANUARY;
            switch (timeString.substring(8, 11)){
                case "Feb" : month = Calendar.FEBRUARY;  break;
                case "Mar" : month = Calendar.MARCH;  break;
                case "Apr" : month = Calendar.APRIL;  break;
                case "May" : month = Calendar.MAY;  break;
                case "Jun" : month = Calendar.JUNE;  break;
                case "Jul" : month = Calendar.JULY;  break;
                case "Aug" : month = Calendar.AUGUST;  break;
                case "Sep" : month = Calendar.SEPTEMBER;  break;
                case "Oct" : month = Calendar.OCTOBER; break;
                case "Nov" : month = Calendar.NOVEMBER; break;
                case "Dec" : month = Calendar.DECEMBER; break;
            }
            int year = Integer.parseInt(timeString.substring(12, 16));
            int hour = Integer.parseInt(timeString.substring(17, 19));
            int min = Integer.parseInt(timeString.substring(20, 22));
            int sec = Integer.parseInt(timeString.substring(23, 25));
            GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
            cal.set(year, month, day, hour, min, sec);
            return cal.getTimeInMillis();
        } catch (NumberFormatException | IndexOutOfBoundsException e){
            throw new IllegalArgumentException(e);
        }
    }

    private static String toContentType(String fileEnding) {
        String contentType = "text/plain";
        switch (fileEnding) {
            case "json" :
                contentType = "application/json";
                break;
            case "png" :
                contentType = "image/png";
                break;
            case "jpg" :
            case "jpeg" :
            case "jpe" :
                contentType = "image/jpeg";
                break;
            case "svg" :
                contentType = "image/svg+xml";
                break;
            case "css" :
                contentType = "text/css";
                break;
            case "js" :
                contentType = "text/javascript";
                break;
            case "html" :
            case "htm" :
            case "shtml" :
                contentType = "text/html";
                break;
            case "xml" :
                contentType = "text/xml";
                break;
        }
        return contentType;
    }

}
