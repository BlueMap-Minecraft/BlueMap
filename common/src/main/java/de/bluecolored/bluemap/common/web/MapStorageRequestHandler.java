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

import de.bluecolored.bluemap.api.ContentTypeRegistry;
import de.bluecolored.bluemap.common.web.http.*;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.storage.GridStorage;
import de.bluecolored.bluemap.core.storage.MapStorage;
import de.bluecolored.bluemap.core.storage.compression.CompressedInputStream;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.util.stream.StreamUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.*;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Getter @Setter
public class MapStorageRequestHandler implements HttpRequestHandler {

    private static final Pattern TILE_PATTERN = Pattern.compile("tiles/([\\d/]+)/x(-?[\\d/]+)z(-?[\\d/]+).*");

    private @NonNull MapStorage mapStorage;

    @Override
    public HttpResponse handle(HttpRequest request) {
        String path = request.getPath();

        //normalize path
        if (path.startsWith("/")) path = path.substring(1);
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);

        //provide compressed if requested
        boolean requestGzipped = path.endsWith(".gz");
        if (requestGzipped) path = path.substring(0, path.length() - 3);

        try {

            // provide map-tiles
            Matcher tileMatcher = TILE_PATTERN.matcher(path);
            if (tileMatcher.matches()) {
                int lod = Integer.parseInt(tileMatcher.group(1));
                int x = Integer.parseInt(tileMatcher.group(2).replace("/", ""));
                int z = Integer.parseInt(tileMatcher.group(3).replace("/", ""));

                GridStorage gridStorage = lod == 0 ? mapStorage.hiresTiles() : mapStorage.lowresTiles(lod);
                CompressedInputStream in = gridStorage.read(x, z);
                if (in == null) return new HttpResponse(HttpStatusCode.NO_CONTENT);

                HttpResponse response = new HttpResponse(HttpStatusCode.OK);
                if (lod == 0) response.setHeader("Content-Type", "application/octet-stream");
                else response.setHeader("Content-Type", "image/png");

                return StreamUtil.closeOnError(in, data -> writeToResponse(data, response, request, requestGzipped));
            }

            // provide meta-data
            CompressedInputStream in = switch (path) {
                case "settings.json" -> mapStorage.settings().read();
                case "textures.json" -> mapStorage.textures().read();
                case "live/markers.json" -> mapStorage.markers().read();
                case "live/players.json" -> mapStorage.players().read();
                default -> path.startsWith("assets/") ? mapStorage.asset(path.substring(7)).read() : null;
            };
            if (in != null){
                HttpResponse response = new HttpResponse(HttpStatusCode.OK);
                response.setHeader("Content-Type", ContentTypeRegistry.fromFileName(path));
                return StreamUtil.closeOnError(in, data -> writeToResponse(data, response, request, requestGzipped));
            }

        } catch (NumberFormatException | NoSuchElementException ignore){
        } catch (IOException ex) {
            Logger.global.logError("Failed to read map-tile for web-request.", ex);
            return new HttpResponse(HttpStatusCode.INTERNAL_SERVER_ERROR);
        }

        return new HttpResponse(HttpStatusCode.NOT_FOUND);
    }

    private static HttpResponse writeToResponse(CompressedInputStream data, HttpResponse response, HttpRequest request, boolean requestGzipped) throws IOException {
        Compression compression = data.getCompression();
        if (!requestGzipped) {
            if (
                    compression != Compression.NONE && acceptsEncoding(request, compression.getId())
            ) {
                response.setHeader("Content-Encoding", compression.getId());
                response.setBody(data);
            } else if (
                    compression != Compression.GZIP &&
                    !response.hasHeaderValue("Content-Type", "image/png") &&
                    acceptsEncoding(request, Compression.GZIP.getId())
            ) {
                response.setHeader("Content-Encoding", Compression.GZIP.getId());
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                try (InputStream in = data.decompress(); OutputStream os = Compression.GZIP.compress(byteOut)) {
                    in.transferTo(os);
                }
                byte[] compressedData = byteOut.toByteArray();
                response.setBody(new ByteArrayInputStream(compressedData));
            } else {
                response.setBody(data.decompress());
            }
        } else {
            if (compression == Compression.GZIP) {
                response.setBody(data);
            } else {
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                try (InputStream in = data.decompress(); OutputStream os = Compression.GZIP.compress(byteOut)) {
                    in.transferTo(os);
                }
                byte[] compressedData = byteOut.toByteArray();
                response.setBody(new ByteArrayInputStream(compressedData));
            }
        }
        return response;
    }

    private static boolean acceptsEncoding(HttpHeaderCarrier headerCarrier, String encoding) {
        HttpHeader header = headerCarrier.getHeader("Accept-Encoding");
        if (header == null) return false;

        boolean wildcardAccepted = false;
        for (String value : header.getValues()) {
            String[] parts = value.split(";");
            String coding = parts[0].trim();

            if (coding.equalsIgnoreCase(encoding)) return parseQuality(parts, 1) > 0f;
            if (coding.equals("*")) wildcardAccepted = parseQuality(parts, 1) > 0f;
        }

        return wildcardAccepted;
    }

    private static float parseQuality(String[] params, int offset) {
        for (int i = offset; i < params.length; i++) {
            String param = params[i].trim();
            if (param.length() < 2 || !param.regionMatches(true, 0, "q=", 0, 2)) continue;
            try {
                return Float.parseFloat(param.substring(2).trim());
            } catch (NumberFormatException ex) {
                return 0f;
            }
        }
        return 1f;
    }

}
