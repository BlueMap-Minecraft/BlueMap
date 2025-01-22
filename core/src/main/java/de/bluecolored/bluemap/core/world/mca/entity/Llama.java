package de.bluecolored.bluemap.core.world.mca.entity;

import de.bluecolored.bluenbt.NBTName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@SuppressWarnings("FieldMayBeFinal")
public class Llama extends MCAEntity {

    @NBTName("ChestedHorse") boolean withChest;
    @NBTName("Variant") Variant variant;

    public enum Variant {
        CREAMY,
        WHITE,
        BROWN,
        GRAY
    }

}
