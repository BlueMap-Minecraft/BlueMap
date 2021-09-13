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
package de.bluecolored.bluemap.core.util;

import de.bluecolored.bluemap.core.logger.Logger;

import java.io.*;
import java.nio.file.*;

public class AtomicFileHelper {

	public static OutputStream createFilepartOutputStream(final File file) throws IOException {
		return createFilepartOutputStream(file.toPath());
	}

	public static OutputStream createFilepartOutputStream(final Path file) throws IOException {
		final Path partFile = getPartFile(file);
		Files.createDirectories(partFile.getParent());

		OutputStream os = Files.newOutputStream(partFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
		return new WrappedOutputStream(os, () -> {
			if (!Files.exists(partFile)) return;

			Files.deleteIfExists(file);
			Files.createDirectories(file.getParent());

			try {
				Files.move(partFile, file, StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException ex) {
				Files.move(partFile, file);
			}
		});
	}

	private static Path getPartFile(Path file) {
		return file.normalize().getParent().resolve(file.getFileName() + ".filepart");
	}

	private static class WrappedOutputStream extends OutputStream {

		private final OutputStream out;
		private final ThrowingRunnable<IOException> onClose;

		private WrappedOutputStream(OutputStream out, ThrowingRunnable<IOException> onClose) {
			this.out = out;
			this.onClose = onClose;
		}

		@Override
		public void write(int b) throws IOException {
			out.write(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			out.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			out.write(b, off, len);
		}

		@Override
		public void flush() throws IOException {
			out.flush();
		}

		@Override
		public void close() throws IOException {
			out.close();
			onClose.run();
		}

	}

}
