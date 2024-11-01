package de.bluecolored.bluemap.core.map.hires.blockmodel;

import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;

public interface BlockRendererFactory {

    BlockRenderer create(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings);

}
