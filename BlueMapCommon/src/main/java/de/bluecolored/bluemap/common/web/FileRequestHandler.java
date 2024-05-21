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
import de.bluecolored.bluemap.core.logger.Logger;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@Getter @Setter
public class FileRequestHandler implements HttpRequestHandler {

    private @NonNull Path webRoot;

    public FileRequestHandler(Path webRoot) {
        this.webRoot = webRoot.normalize();
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        if (!request.getMethod().equalsIgnoreCase("GET"))
            return new HttpResponse(HttpStatusCode.BAD_REQUEST);

        try {
            return generateResponse(request);
        } catch (IOException e) {
            Logger.global.logError("Failed to serve file", e);
            return new HttpResponse(HttpStatusCode.INTERNAL_SERVER_ERROR);
        }
    }

    private HttpResponse generateResponse(HttpRequest request) throws IOException {
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

        // redirect to have correct relative paths
        if (Files.isDirectory(filePath) && !request.getPath().endsWith("/")) {
            HttpResponse response = new HttpResponse(HttpStatusCode.SEE_OTHER);
            response.addHeader("Location", "/" + path + "/" + (request.getGETParamString().isEmpty() ? "" : "?" + request.getGETParamString()));
            return response;
        }

        // default to index.html
        if (!Files.exists(filePath) || Files.isDirectory(filePath)){
            filePath = filePath.resolve("index.html");
        }

        if (!Files.exists(filePath) || Files.isDirectory(filePath)){
            return new HttpResponse(HttpStatusCode.NOT_FOUND);
        }

        // don't send php files
        if (filePath.getFileName().toString().endsWith(".php")) {
            return new HttpResponse(HttpStatusCode.FORBIDDEN);
        }

        // check if file is still in web-root and is not a directory
        if (!filePath.normalize().startsWith(webRoot) || Files.isDirectory(filePath)){
            return new HttpResponse(HttpStatusCode.FORBIDDEN);
        }

        // check modified
        long lastModified = Files.getLastModifiedTime(filePath).to(TimeUnit.MILLISECONDS);
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
        String eTag =
                Long.toHexString(Files.size(filePath)) +
                Integer.toHexString(filePath.hashCode()) +
                Long.toHexString(lastModified);
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
        response.addHeader("Cache-Control", "max-age=" + TimeUnit.DAYS.toSeconds(1));

        //add content type header
        String filetype = filePath.getFileName().toString();
        int pointIndex = filetype.lastIndexOf('.');
        if (pointIndex >= 0) filetype = filetype.substring(pointIndex + 1);
        String contentType = toContentType(filetype);
        response.addHeader("Content-Type", contentType);

        //send response
        try {
            response.setData(Files.newInputStream(filePath));
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
            int month = switch (timeString.substring(8, 11)) {
                case "Jan" -> Calendar.JANUARY;
                case "Feb" -> Calendar.FEBRUARY;
                case "Mar" -> Calendar.MARCH;
                case "Apr" -> Calendar.APRIL;
                case "May" -> Calendar.MAY;
                case "Jun" -> Calendar.JUNE;
                case "Jul" -> Calendar.JULY;
                case "Aug" -> Calendar.AUGUST;
                case "Sep" -> Calendar.SEPTEMBER;
                case "Oct" -> Calendar.OCTOBER;
                case "Nov" -> Calendar.NOVEMBER;
                case "Dec" -> Calendar.DECEMBER;
                default -> throw new IllegalArgumentException("Invalid timestamp format");
            };
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
        return switch (fileEnding) {
            case "json" -> "application/json";
            case "png" -> "image/png";
            case "jpg",
                 "jpeg",
                 "jpe" -> "image/jpeg";
            case "svg" -> "image/svg+xml";
            case "css" -> "text/css";
            case "js" -> "text/javascript";
            case "html",
                 "htm",
                 "shtml" -> "text/html";
            case "xml" -> "text/xml";
            default -> "text/plain";
        };
    }

}
