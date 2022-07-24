package de.bluecolored.bluemap.core.util;

import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.resources.ResourcePath;

@DebugDump
public class Key {

    private static final String MINECRAFT_NAMESPACE = "minecraft";

    private final String namespace;
    private final String value;
    private final String formatted;

    public Key(String formatted) {
        String namespace = MINECRAFT_NAMESPACE;
        String path = formatted;
        int namespaceSeparator = formatted.indexOf(':');
        if (namespaceSeparator > 0) {
            namespace = formatted.substring(0, namespaceSeparator);
            path = formatted.substring(namespaceSeparator + 1);
        }

        this.namespace = namespace.intern();
        this.value = path.intern();
        this.formatted = (this.namespace + ":" + this.value).intern();
    }

    public Key(String namespace, String value) {
        this.namespace = namespace.intern();
        this.value = value.intern();
        this.formatted = (this.namespace + ":" + this.value).intern();
    }

    public String getNamespace() {
        return namespace;
    }

    public String getValue() {
        return value;
    }

    public String getFormatted() {
        return formatted;
    }

    @SuppressWarnings("StringEquality")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourcePath<?> that = (ResourcePath<?>) o;
        return getFormatted() == that.getFormatted();
    }

    @Override
    public int hashCode() {
        return getFormatted().hashCode();
    }

    @Override
    public String toString() {
        return formatted;
    }
}
