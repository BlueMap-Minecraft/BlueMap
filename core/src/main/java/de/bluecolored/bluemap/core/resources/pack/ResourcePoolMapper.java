package de.bluecolored.bluemap.core.resources.pack;

import de.bluecolored.bluemap.core.util.Key;

import java.lang.reflect.Type;

public interface ResourcePoolMapper {
    Key remapResource(Type type, Key src);
}
