package de.bluecolored.bluemap.core.util.stream;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DelegateInputStream extends InputStream {

    protected final InputStream in;

    public DelegateInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public int read(byte @NotNull [] b) throws IOException {
        return in.read(b);
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        return in.readAllBytes();
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        return in.readNBytes(len);
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        return in.readNBytes(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    @Override
    public long transferTo(OutputStream out) throws IOException {
        return in.transferTo(out);
    }

}
