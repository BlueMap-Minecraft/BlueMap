package de.bluecolored.bluemap.core.resources.resourcepack.blockstate;

import de.bluecolored.bluemap.api.debug.DebugDump;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@SuppressWarnings("FieldMayBeFinal")
@DebugDump
public class BlockState {

    private Variants variants = null;
    private Multipart multipart = null;

    private BlockState(){}

    @Nullable
    public Variants getVariants() {
        return variants;
    }

    @Nullable
    public Multipart getMultipart() {
        return multipart;
    }

    public void forEach(de.bluecolored.bluemap.core.world.BlockState blockState, int x, int y, int z, Consumer<Variant> consumer) {
        if (variants != null) variants.forEach(blockState, x, y, z, consumer);
        if (multipart != null) multipart.forEach(blockState, x, y, z, consumer);
    }

}
