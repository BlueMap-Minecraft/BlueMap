package de.bluecolored.bluemap.core.map.hires;

import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;

public interface RenderPassFactory {

    RenderPass create(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings);

}
