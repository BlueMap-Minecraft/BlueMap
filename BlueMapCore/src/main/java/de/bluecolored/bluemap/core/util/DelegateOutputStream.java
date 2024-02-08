package de.bluecolored.bluemap.core.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;

public class DelegateOutputStream extends OutputStream {

    protected final OutputStream out;

    public DelegateOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte @NotNull [] b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte @NotNull [] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

}
