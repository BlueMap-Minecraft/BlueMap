package de.bluecolored.bluemap.core.world.mca.entity;

import de.bluecolored.bluenbt.NBTName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@SuppressWarnings("FieldMayBeFinal")
public class Cat extends AgeEntity {

    @NBTName("Sitting") int sitting;
    @NBTName("variant") Variant variant;

    public enum Variant {
        BLACK,
        ALL_BLACK,
        BRITISH_SHORTHAIR,
        CALICO,
        JELLIE,
        PERSIAN,
        RAGDOLL,
        RED,
        SIAMESE,
        TABBY,
        WHITE
    }

}
