package de.bluecolored.bluemap.core.util.stream;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;

public class CountingOutputStream extends DelegateOutputStream {

    private long count;

    public CountingOutputStream(OutputStream out) {
        this(out, 0);
    }

    public CountingOutputStream(OutputStream out, int initialCount) {
        super(out);
        this.count = initialCount;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        count ++;
    }

    @Override
    public void write(byte @NotNull [] b) throws IOException {
        out.write(b);
        count += b.length;
    }

    @Override
    public void write(byte @NotNull [] b, int off, int len) throws IOException {
        out.write(b, off, len);
        count += len;
    }

    public long getCount() {
        return count;
    }

    @Override
    public void close() throws IOException {
        count = 0;
        super.close();
    }

}
