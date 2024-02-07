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

import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.common.web.http.HttpRequest;
import de.bluecolored.bluemap.common.web.http.HttpRequestHandler;
import de.bluecolored.bluemap.common.web.http.HttpResponse;
import de.bluecolored.bluemap.common.web.http.HttpStatusCode;
import org.intellij.lang.annotations.Language;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DebugDump
public class RoutingRequestHandler implements HttpRequestHandler {

    public LinkedList<Route> routes;

    public RoutingRequestHandler() {
        this.routes = new LinkedList<>();
    }

    public void register(@Language("RegExp") String pattern, HttpRequestHandler handler) {
        register(Pattern.compile(pattern), handler);
    }

    public void register(@Language("RegExp") String pattern, String replacementRoute, HttpRequestHandler handler) {
        register(Pattern.compile(pattern), replacementRoute, handler);
    }

    public void register(Pattern pattern, HttpRequestHandler handler) {
        this.routes.addFirst(new Route(pattern, handler));
    }

    public void register(Pattern pattern, String replacementRoute, HttpRequestHandler handler) {
        this.routes.addFirst(new Route(pattern, replacementRoute, handler));
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        String path = request.getPath();

        // normalize path
        if (path.startsWith("/")) path = path.substring(1);
        if (path.isEmpty()) path = "/";

        for (Route route : routes) {
            Matcher matcher = route.getRoutePattern().matcher(path);
            if (matcher.matches()) {
                request.setPath(matcher.replaceFirst(route.getReplacementRoute()));
                return route.getHandler().handle(request);
            }
        }

        return new HttpResponse(HttpStatusCode.BAD_REQUEST);
    }

    @DebugDump
    private static class Route {

        private final Pattern routePattern;
        private final HttpRequestHandler handler;
        private final String replacementRoute;

        public Route(Pattern routePattern, HttpRequestHandler handler) {
            this.routePattern = routePattern;
            this.replacementRoute = "$0";
            this.handler = handler;
        }

        public Route(Pattern routePattern, String replacementRoute, HttpRequestHandler handler) {
            this.routePattern = routePattern;
            this.replacementRoute = replacementRoute;
            this.handler = handler;
        }

        public Pattern getRoutePattern() {
            return routePattern;
        }

        public HttpRequestHandler getHandler() {
            return handler;
        }

        public String getReplacementRoute() {
            return replacementRoute;
        }

    }

}
