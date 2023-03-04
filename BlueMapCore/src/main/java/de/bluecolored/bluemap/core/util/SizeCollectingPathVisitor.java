package de.bluecolored.bluemap.core.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

public class SizeCollectingPathVisitor extends SimpleFileVisitor<Path> {

    private final AtomicLong size = new AtomicLong(0);

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        size.addAndGet(attrs.size());
        return FileVisitResult.CONTINUE;
    }

    public long getSize() {
        return size.get();
    }

}
