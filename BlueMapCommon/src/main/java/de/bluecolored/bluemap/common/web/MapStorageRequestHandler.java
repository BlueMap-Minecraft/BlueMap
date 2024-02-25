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

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.api.ContentTypeRegistry;
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.common.web.http.*;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.storage.CompressedInputStream;
import de.bluecolored.bluemap.core.storage.Compression;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.storage.TileInfo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DebugDump
public class MapStorageRequestHandler implements HttpRequestHandler {

    private static final Pattern TILE_PATTERN = Pattern.compile("tiles/([\\d/]+)/x(-?[\\d/]+)z(-?[\\d/]+).*");

    private final String mapId;
    private final Storage mapStorage;


    public MapStorageRequestHandler(BmMap map) {
        this.mapId = map.getId();
        this.mapStorage = map.getStorage();
    }

    public MapStorageRequestHandler(String mapId, Storage mapStorage) {
        this.mapId = mapId;
        this.mapStorage = mapStorage;
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        String path = request.getPath();

        //normalize path
        if (path.startsWith("/")) path = path.substring(1);
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);

        try {

            // provide map-tiles
            Matcher tileMatcher = TILE_PATTERN.matcher(path);
            if (tileMatcher.matches()) {
                int lod = Integer.parseInt(tileMatcher.group(1));
                int x = Integer.parseInt(tileMatcher.group(2).replace("/", ""));
                int z = Integer.parseInt(tileMatcher.group(3).replace("/", ""));
                Optional<TileInfo> optTileInfo = mapStorage.readMapTileInfo(mapId, lod, new Vector2i(x, z));

                if (optTileInfo.isPresent()) {
                    TileInfo tileInfo = optTileInfo.get();

                    // check e-tag
                    String eTag = calculateETag(path, tileInfo);
                    HttpHeader etagHeader = request.getHeader("If-None-Match");
                    if (etagHeader != null){
                        if(etagHeader.getValue().equals(eTag)) {
                            return new HttpResponse(HttpStatusCode.NOT_MODIFIED);
                        }
                    }

                    // check modified-since
                    long lastModified = tileInfo.getLastModified();
                    HttpHeader modHeader = request.getHeader("If-Modified-Since");
                    if (modHeader != null){
                        try {
                            long since = stringToTimestamp(modHeader.getValue());
                            if (since + 1000 >= lastModified){
                                return new HttpResponse(HttpStatusCode.NOT_MODIFIED);
                            }
                        } catch (IllegalArgumentException ignored){}
                    }

                    CompressedInputStream compressedIn = tileInfo.readMapTile();
                    HttpResponse response = new HttpResponse(HttpStatusCode.OK);
                    response.addHeader("ETag", eTag);
                    if (lastModified > 0)
                        response.addHeader("Last-Modified", timestampToString(lastModified));

                    response.addHeader("Cache-Control", "public");
                    response.addHeader("Cache-Control", "max-age=" + TimeUnit.DAYS.toSeconds(1));

                    if (lod == 0) response.addHeader("Content-Type", "application/octet-stream");
                    else response.addHeader("Content-Type", "image/png");

                    writeToResponse(compressedIn, response, request);
                    return response;
                }
            }

            // provide meta-data
            Optional<InputStream> optIn = mapStorage.readMeta(mapId, path);
            if (optIn.isPresent()) {
                CompressedInputStream compressedIn = new CompressedInputStream(optIn.get(), Compression.NONE);
                HttpResponse response = new HttpResponse(HttpStatusCode.OK);
                response.addHeader("Cache-Control", "public");
                response.addHeader("Cache-Control", "max-age=" + TimeUnit.DAYS.toSeconds(1));
                response.addHeader("Content-Type", ContentTypeRegistry.fromFileName(path));
                writeToResponse(compressedIn, response, request);
                return response;
            }

        } catch (NumberFormatException | NoSuchElementException ignore){
        } catch (IOException ex) {
            Logger.global.logError("Failed to read map-tile for web-request.", ex);
            return new HttpResponse(HttpStatusCode.INTERNAL_SERVER_ERROR);
        }

        return new HttpResponse(HttpStatusCode.NO_CONTENT);
    }

    private String calculateETag(String path, TileInfo tileInfo) {
        return Long.toHexString(tileInfo.getSize()) + Integer.toHexString(path.hashCode()) + Long.toHexString(tileInfo.getLastModified());
    }

    private void writeToResponse(CompressedInputStream data, HttpResponse response, HttpRequest request) throws IOException {
        Compression compression = data.getCompression();
        if (
                compression != Compression.NONE &&
                request.hasHeaderValue("Accept-Encoding", compression.getTypeId())
        ) {
            response.addHeader("Content-Encoding", compression.getTypeId());
            response.setData(data);
        } else if (
                compression != Compression.GZIP &&
                !response.hasHeaderValue("Content-Type", "image/png") &&
                request.hasHeaderValue("Accept-Encoding", Compression.GZIP.getTypeId())
        ) {
            response.addHeader("Content-Encoding", Compression.GZIP.getTypeId());
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            try (OutputStream os = Compression.GZIP.compress(byteOut)) {
                IOUtils.copyLarge(data.decompress(), os);
            }
            byte[] compressedData = byteOut.toByteArray();
            response.setData(new ByteArrayInputStream(compressedData));
        } else {
            response.setData(new BufferedInputStream(data.decompress()));
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

}
