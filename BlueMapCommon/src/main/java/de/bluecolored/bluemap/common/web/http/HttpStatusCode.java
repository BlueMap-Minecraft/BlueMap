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
package de.bluecolored.bluemap.common.web.http;

public enum HttpStatusCode {

    CONTINUE (100, "Continue"),
    PROCESSING (102, "Processing"),

    OK (200, "OK"),
    NO_CONTENT (204, "No Content"),

    MOVED_PERMANENTLY (301, "Moved Permanently"),
    FOUND (302, "Found"),
    SEE_OTHER (303, "See Other"),
    NOT_MODIFIED (304, "Not Modified"),

    BAD_REQUEST (400, "Bad Request"),
    UNAUTHORIZED (401, "Unauthorized"),
    FORBIDDEN (403, "Forbidden"),
    NOT_FOUND (404, "Not Found"),

    INTERNAL_SERVER_ERROR (500, "Internal Server Error"),
    NOT_IMPLEMENTED (501, "Not Implemented"),
    SERVICE_UNAVAILABLE (503, "Service Unavailable"),
    HTTP_VERSION_NOT_SUPPORTED (505, "HTTP Version not supported");

    private int code;
    private String message;

    private HttpStatusCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode(){
        return code;
    }

    public String getMessage(){
        return message;
    }

    @Override
    public String toString() {
        return getCode() + " " + getMessage();
    }

}
