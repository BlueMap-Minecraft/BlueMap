package de.bluecolored.bluemap.core.storage;

import java.io.IOException;
import java.io.InputStream;

public interface MetaInfo {

    InputStream readMeta() throws IOException;

    long getSize();

}
