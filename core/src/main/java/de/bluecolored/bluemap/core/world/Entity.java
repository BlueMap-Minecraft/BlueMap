package de.bluecolored.bluemap.core.world;

import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.core.util.Key;

import java.util.UUID;

public interface Entity {

    Key getId();

    UUID getUuid();

    String getCustomName();

    boolean isCustomNameVisible();

    Vector3d getPos();

    Vector3d getMotion();

    Vector2f getRotation();

}
