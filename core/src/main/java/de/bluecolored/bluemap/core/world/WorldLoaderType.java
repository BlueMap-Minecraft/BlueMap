package de.bluecolored.bluemap.core.world;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import de.bluecolored.bluemap.core.world.mca.MCAWorld;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

public interface WorldLoaderType extends Keyed, WorldLoader {

    WorldLoaderType ANVIL = new Impl(Key.bluemap("anvil"), MCAWorld::load);

    Registry<WorldLoaderType> REGISTRY = new Registry<>(
            ANVIL
    );

    @RequiredArgsConstructor
    class Impl implements WorldLoaderType {

        @Getter private final Key key;
        @Delegate private final WorldLoader loader;

    }

}
