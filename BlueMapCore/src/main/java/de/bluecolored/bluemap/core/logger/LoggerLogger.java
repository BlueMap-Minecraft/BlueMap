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
package de.bluecolored.bluemap.core.logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class LoggerLogger extends AbstractLogger {
	private static LoggerLogger instance = null;
	
	private Logger logger;
	private SimpleFormatter formatter;

	private LoggerLogger() {
		this.logger = Logger.getLogger("bluemap");
		this.logger.setUseParentHandlers(false);
		ConsoleHandler cHandler = new ConsoleHandler();
		formatter = new SimpleFormatter() {
			@Override
			public synchronized String format(LogRecord record) {
				String stackTrace = record.getThrown() == null ? "" : ExceptionUtils.getStackTrace(record.getThrown());
				return String.format("[%1$s] %2$s%3$s%n", record.getLevel(), record.getMessage(), stackTrace);
			}
		};
		cHandler.setFormatter(formatter);
		this.logger.addHandler(cHandler);
		
	}
	
	public static LoggerLogger getInstance() {
		if (instance == null) {
			instance = new LoggerLogger();
		}
		return instance;
	}
	
	public void addFileHandler(String filename, boolean append) {
		try {
			FileHandler fHandler = new FileHandler(filename, append);
			fHandler.setFormatter(formatter);
			this.logger.addHandler(fHandler);
		} catch (NoSuchFileException e) {
			// Directory may not exist. Create it and try again.
			File parent = Paths.get(e.getFile()).getParent().toFile();
			if (!parent.exists()) {
				parent.mkdirs();
				addFileHandler(filename, append);
			} else {
				de.bluecolored.bluemap.core.logger.Logger.global.logError("Error while opening log file!", e);
			}
		} catch (IOException e) {
			de.bluecolored.bluemap.core.logger.Logger.global.logError("Error while opening log file!", e);
		}
	}

	@Override
	public void logError(String message, Throwable throwable) {
		logger.log(Level.SEVERE, message, throwable);
	}

	@Override
	public void logWarning(String message) {
		logger.log(Level.WARNING, message);
	}

	@Override
	public void logInfo(String message) {
		logger.log(Level.INFO, message);
	}

	@Override
	public void logDebug(String message) {
		logger.log(Level.FINE, message);
	}
	
}
