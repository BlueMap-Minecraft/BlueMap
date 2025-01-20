package de.bluecolored.bluemap.core.world.mca.entity.chunk;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.world.Entity;
import de.bluecolored.bluenbt.NBTName;
import lombok.Getter;

@Getter
public class MCAEntityChunk {

    private static final Entity[] EMPTY_ENTITIES = new Entity[0];

    public static final MCAEntityChunk EMPTY_CHUNK = new MCAEntityChunk();
    public static final MCAEntityChunk ERRORED_CHUNK = new MCAEntityChunk();

    @NBTName("Entities")
    public Entity[] entities = EMPTY_ENTITIES;

    @NBTName("DataVersion")
    public int dataVersion = -1;

    @NBTName("Position")
    public Vector2i position = Vector2i.ZERO;

}
