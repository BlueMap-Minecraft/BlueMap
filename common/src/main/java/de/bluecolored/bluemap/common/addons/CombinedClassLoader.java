package de.bluecolored.bluemap.common.addons;

import java.util.Arrays;
import java.util.Objects;

public class CombinedClassLoader extends ClassLoader {

    private final ClassLoader[] delegates;

    public CombinedClassLoader(ClassLoader parent, ClassLoader... delegates) {
        super(parent);
        if (delegates.length == 0) throw new IllegalArgumentException("No parent classloaders provided");
        if (Arrays.stream(delegates).anyMatch(Objects::isNull)) throw new IllegalArgumentException("Parent classloaders can not be null");
        this.delegates = delegates;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (ClassLoader parent : delegates) {
            try {
                return parent.loadClass(name);
            } catch (ClassNotFoundException ignore) {
                // ignore ClassNotFoundException thrown if class not found in parent
            }
        }

        throw new ClassNotFoundException(name);
    }
}
