package de.bluecolored.bluemap.core.resources.pack.resourcepack;

import de.bluecolored.bluemap.core.resources.pack.PackExtension;
import de.bluecolored.bluemap.core.util.Key;

import java.util.Set;

public interface ResourcePackExtension extends PackExtension {

    default Set<Key> collectUsedTextureKeys() {
        return Set.of();
    }

}
