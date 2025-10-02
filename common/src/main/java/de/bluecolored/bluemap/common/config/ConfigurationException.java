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
package de.bluecolored.bluemap.common.config;

import de.bluecolored.bluemap.core.logger.Logger;

public class ConfigurationException extends Exception {

    private static final String FORMATTING_BAR = "################################";

    private final String explanation;

    public ConfigurationException(String explanation) {
        super();
        this.explanation = explanation;
    }

    public ConfigurationException(String message, String explanation) {
        super(message);
        this.explanation = explanation;
    }

    public ConfigurationException(String explanation, Throwable cause) {
        super(cause);
        this.explanation = explanation;
    }

    public ConfigurationException(String message, String explanation, Throwable cause) {
        super(message, cause);
        this.explanation = explanation;
    }

    public Throwable getRootCause() {
        Throwable cause;
        do { cause = getCause(); }
        while (cause instanceof ConfigurationException);
        return cause;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getFullExplanation() {
        Throwable cause = getCause();
        while (cause != null && !(cause instanceof ConfigurationException)) {
            cause = cause.getCause();
        }

        if (cause != null) {
            return getExplanation() + "\n\n" + ((ConfigurationException) cause).getFullExplanation();
        } else {
            return getExplanation();
        }
    }

    public String getFormattedExplanation() {
        String indentedExplanation = " " + getFullExplanation().replace("\n", "\n ");
        return "\n" + FORMATTING_BAR +
               "\n There is a problem with your BlueMap setup!\n\n" +
               indentedExplanation +
               "\n" + FORMATTING_BAR;
    }

    public void printLog(Logger logger) {
        logger.logWarning(getFormattedExplanation());
        Throwable cause = getRootCause();
        if (cause != null)
            logger.logError("Detailed error:", this);
    }

}
