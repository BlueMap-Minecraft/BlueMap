package de.bluecolored.bluemap.core.world;

import de.bluecolored.bluemap.core.util.Key;

public interface BlockEntity {

    Key getId();

    int getX();
    int getY();
    int getZ();

    boolean isKeepPacked();

}
