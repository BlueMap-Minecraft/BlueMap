package de.bluecolored.bluemap.core.world.mca.entity;

import de.bluecolored.bluenbt.NBTName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@SuppressWarnings("FieldMayBeFinal")
public class Fox extends AgeEntity {

    @NBTName("Sitting") boolean sitting;
    @NBTName("Sleeping") boolean sleeping;
    @NBTName("Type") Type type;

    public enum Type {
        RED,
        SNOW
    }

}
