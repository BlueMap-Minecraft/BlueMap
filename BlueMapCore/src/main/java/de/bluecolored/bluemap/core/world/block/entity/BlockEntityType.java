package de.bluecolored.bluemap.core.world.block.entity;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

public interface BlockEntityType extends Keyed, BlockEntityLoader {

    BlockEntityType SIGN = new Impl(Key.minecraft("sign"), SignBlockEntity::new);
    BlockEntityType SKULL = new Impl(Key.minecraft("skull"), SkullBlockEntity::new);
    BlockEntityType BANNER = new Impl(Key.minecraft("banner"), BannerBlockEntity::new);

    Registry<BlockEntityType> REGISTRY = new Registry<>(
            SIGN,
            SKULL,
            BANNER
    );

    @RequiredArgsConstructor
    class Impl implements BlockEntityType {

        @Getter
        private final Key key;
        private final BlockEntityLoader loader;

        @Override
        public BlockEntity load(Map<String, Object> raw) {
            return loader.load(raw);
        }

    }

}
