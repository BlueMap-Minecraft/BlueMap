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
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.storage.*;
import de.bluecolored.bluemap.common.webserver.HttpRequest;
import de.bluecolored.bluemap.common.webserver.HttpRequestHandler;
import de.bluecolored.bluemap.common.webserver.HttpResponse;
import de.bluecolored.bluemap.common.webserver.HttpStatusCode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapStorageRequestHandler implements HttpRequestHandler {

    private static final Pattern TILE_PATTERN = Pattern.compile("data/([^/]+)/([^/]+)/x(-?[\\d/]+)z(-?[\\d/]+).*");
    private static final Pattern META_PATTERN = Pattern.compile("data/([^/]+)/(.*)");

    private final Function<? super String, Storage> mapStorageProvider;
    private final HttpRequestHandler notFoundHandler;

    public MapStorageRequestHandler(Function<? super String, Storage> mapStorageProvider, HttpRequestHandler notFoundHandler) {
        this.mapStorageProvider = mapStorageProvider;
        this.notFoundHandler = notFoundHandler;
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
                String mapId = tileMatcher.group(1);
                String tileTypeId = tileMatcher.group(2);
                Storage storage = mapStorageProvider.apply(mapId);
                if (storage != null) {
                    TileType tileType = TileType.forTypeId(tileTypeId);
                    int x = Integer.parseInt(tileMatcher.group(3).replace("/", ""));
                    int z = Integer.parseInt(tileMatcher.group(4).replace("/", ""));
                    Optional<TileData> optTileData = storage.readMapTileData(mapId, tileType, new Vector2i(x, z));

                    if (optTileData.isPresent()) {
                        TileData tileData = optTileData.get();

                        // check e-tag
                        String eTag = calculateETag(path, tileData);
                        Set<String> etagStringSet = request.getHeader("If-None-Match");
                        if (!etagStringSet.isEmpty()){
                            if(etagStringSet.iterator().next().equals(eTag)) {
                                return new HttpResponse(HttpStatusCode.NOT_MODIFIED);
                            }
                        }

                        // check modified-since
                        long lastModified = tileData.getLastModified();
                        Set<String> modStringSet = request.getHeader("If-Modified-Since");
                        if (!modStringSet.isEmpty()){
                            try {
                                long since = stringToTimestamp(modStringSet.iterator().next());
                                if (since + 1000 >= lastModified){
                                    return new HttpResponse(HttpStatusCode.NOT_MODIFIED);
                                }
                            } catch (IllegalArgumentException ignored){}
                        }

                        CompressedInputStream compressedIn = tileData.readMapTile();
                        HttpResponse response = new HttpResponse(HttpStatusCode.OK);
                        response.addHeader("ETag", eTag);
                        if (lastModified > 0)
                            response.addHeader("Last-Modified", timestampToString(lastModified));
                        response.addHeader("Content-Type", "application/json");
                        writeToResponse(compressedIn, response, request);
                        return response;
                    }
                }
            }

            // provide meta-data
            Matcher metaMatcher = META_PATTERN.matcher(path);
            if (metaMatcher.matches()) {
                String mapId = metaMatcher.group(1);
                String metaFilePath = metaMatcher.group(2);

                Storage storage = mapStorageProvider.apply(mapId);
                if (storage != null) {

                    MetaType metaType = null;
                    for (MetaType mt : MetaType.values()) {
                        if (mt.getFilePath().equals(metaFilePath)) {
                            metaType = mt;
                            break;
                        }
                    }

                    if (metaType != null) {
                        Optional<CompressedInputStream> optIn = storage.readMeta(mapId, metaType);
                        if (optIn.isPresent()) {
                            CompressedInputStream compressedIn = optIn.get();
                            HttpResponse response = new HttpResponse(HttpStatusCode.OK);
                            response.addHeader("Content-Type", metaType.getContentType());
                            writeToResponse(compressedIn, response, request);
                            return response;
                        }
                    }
                }
            }

        } catch (NumberFormatException | NoSuchElementException ignore){
        } catch (IOException ex) {
            Logger.global.logError("Failed to read map-tile for web-request.", ex);
            return new HttpResponse(HttpStatusCode.INTERNAL_SERVER_ERROR);
        }

        return this.notFoundHandler.handle(request);
    }

    private String calculateETag(String path, TileData tileData) {
        return Long.toHexString(tileData.getSize()) + Integer.toHexString(path.hashCode()) + Long.toHexString(tileData.getLastModified());
    }

    private void writeToResponse(CompressedInputStream data, HttpResponse response, HttpRequest request) throws IOException {
        Compression compression = data.getCompression();
        if (
                compression != Compression.NONE &&
                request.getLowercaseHeader("Accept-Encoding").contains(compression.getTypeId())
        ) {
            response.addHeader("Content-Encoding", compression.getTypeId());
            response.setData(data);
        } else if (
                compression != Compression.GZIP &&
                request.getLowercaseHeader("Accept-Encoding").contains(Compression.GZIP.getTypeId())
        ) {
            response.addHeader("Content-Encoding", Compression.GZIP.getTypeId());
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            try (OutputStream os = Compression.GZIP.compress(byteOut)) {
                IOUtils.copyLarge(data.decompress(), os);
            }
            byte[] compressedData = byteOut.toByteArray();
            response.setData(new ByteArrayInputStream(compressedData));
        } else {
            response.setData(data.decompress());
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
