package de.bluecolored.bluemap.core.world.mca.entity;

import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.Entity;
import de.bluecolored.bluenbt.NBTName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@EqualsAndHashCode
@ToString
@SuppressWarnings("FieldMayBeFinal")
public class MCAEntity implements Entity {

    Key id;
    @NBTName("UUID") UUID uuid;
    @NBTName("CustomName") String customName;
    @NBTName("CustomNameVisible") boolean customNameVisible;
    @NBTName("Pos") Vector3d pos;
    @NBTName("Motion") Vector3d motion;
    @NBTName("Rotation") Vector2f rotation;

}
