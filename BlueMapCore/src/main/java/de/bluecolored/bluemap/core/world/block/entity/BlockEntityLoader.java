package de.bluecolored.bluemap.core.world.block.entity;

import java.util.Map;

public interface BlockEntityLoader {

    BlockEntity load(Map<String, Object> raw);

}
