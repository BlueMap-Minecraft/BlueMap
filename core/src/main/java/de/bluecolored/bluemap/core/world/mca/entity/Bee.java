package de.bluecolored.bluemap.core.world.mca.entity;

import de.bluecolored.bluenbt.NBTName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@SuppressWarnings("FieldMayBeFinal")
public class Bee extends AgeEntity {

    @NBTName("HasStung") boolean hasStung;
    @NBTName("HasNectar") int hasNectar;
}
