package de.bluecolored.bluemap.core.resources.pack.resourcepack.atlas;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public interface SourceType extends Keyed {

    Registry<SourceType> REGISTRY = new Registry<>(
        new Impl(Key.minecraft("single"), SingleSource.class),
        new Impl(Key.minecraft("directory"), DirectorySource.class),
        new Impl(Key.minecraft("filter"), Source.class),
        new Impl(Key.minecraft("unstitch"), UnstitchSource.class),
        new Impl(Key.minecraft("paletted_permutations"), PalettedPermutationsSource.class)
    );

    Class<? extends Source> getType();

    @RequiredArgsConstructor
    @Getter
    class Impl implements SourceType {

        private final Key key;
        private final Class<? extends Source> type;

    }

}
