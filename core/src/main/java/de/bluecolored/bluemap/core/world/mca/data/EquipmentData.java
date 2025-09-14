package de.bluecolored.bluemap.core.world.mca.data;

import lombok.Data;

@Data
public class EquipmentData {
    ItemStackDeserializer.ItemStack head, chest, legs, feet, mainhand, offhand, body, saddle;
}
