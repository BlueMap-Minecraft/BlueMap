package de.bluecolored.bluemap.core.map.hires.blockmodel;

import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;

public interface BlockRenderer {

    /**
     * Renders the given blocks (block-state-)variant into the given blockModel, and sets the given blockColor to the
     * color that represents the rendered block.
     * <p>
     *  <b>Implementation Note:</b><br>
     *  This method is guaranteed to be called only on <b>one thread per BlockRenderer instance</b>, so you can use this
     *  for optimizations.<br>
     *  Keep in mind this method will be called once for every block that is being rendered, so be very careful
     *  about performance and instance-creations.
     * </p>
     * @param block The block information that should be rendered.
     * @param variant The block-state variant that should be rendered.
     * @param blockModel The model(-view) where the block should be rendered to.
     * @param blockColor The color that should be set to the color that represents the rendered block.
     */
    void render(BlockNeighborhood<?> block, Variant variant, TileModelView blockModel, Color blockColor);

}
