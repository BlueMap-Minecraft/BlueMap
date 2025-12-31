import {Loader, ImageLoader, Texture} from "three";
import {RevalidatingFileLoader} from "./RevalidatingFileLoader";

/**
 * @import {TextureLoader} from "three"
 */

/**
 * An alternative to {@link TextureLoader} for loading textures with support for
 * forcing revalidation like {@link RevalidatingFileLoader}.
 *
 * Images are internally loaded via {@link ImageLoader} or
 * {@link RevalidatingFileLoader} if an uncached request is made.
 *
 * ```js
 * const loader = new RevalidatingTextureLoader();
 * const texture = await loader.loadAsync( 'textures/land_ocean_ice_cloud_2048.jpg' );
 *
 * const material = new THREE.MeshBasicMaterial( { map:texture } );
 * ```
 */
export class RevalidatingTextureLoader extends Loader {
    /** @type {Set<string> | undefined} */
    #revalidatedUrls;
    #revalidatingFileLoader = new RevalidatingFileLoader(this.manager);
    #imageLoader = new ImageLoader(this.manager);

    /**
     * @param {Set<string> | undefined} revalidatedUrls - If set to a Set, this
     *   loader will revalidate URLs by setting the Request cache option to
     *   "no-cache" for URLs not in the Set, adding them to the Set once loaded.
     */
    setRevalidatedUrls(revalidatedUrls) {
        this.#revalidatedUrls = revalidatedUrls;
        this.#revalidatingFileLoader.setRevalidatedUrls(revalidatedUrls);
        return this;
    }

    /**
	 * Starts loading from the given URL and pass the fully loaded texture
	 * to the `onLoad()` callback. The method also returns a new texture object which can
	 * directly be used for material creation. If you do it this way, the texture
	 * may pop up in your scene once the respective loading process is finished.
	 *
	 * @param {string} url - The path/URL of the file to be loaded. This can also be a data URI.
	 * @param {function(Texture<HTMLImageElement | ImageBitmap>)} onLoad - Executed when the loading process has been finished.
	 * @param {onProgressCallback} onProgress - Unsupported in this loader.
	 * @param {onErrorCallback} onError - Executed when errors occur.
	 * @return {Texture<HTMLImageElement | ImageBitmap>} The texture.
	 */
    load(url, onLoad, onProgress, onError) {
        // copy reference at start of method in case it is changed while loading
        const revalidatedUrls = this.#revalidatedUrls;

        const texture = new Texture();

        if (revalidatedUrls && !revalidatedUrls.has(url)) {
            const loader = this.#revalidatingFileLoader;
            loader.setResponseType('blob');
            loader.setWithCredentials(this.withCredentials);
            loader.setCrossOrigin(this.crossOrigin);
            loader.setPath(this.path);

            loader.loadAsync(url, onProgress)
                .then(async blob => {
                    revalidatedUrls.add(url);

                    const imageBitmap = await createImageBitmap(blob, {colorSpaceConversion: 'none'});
                    texture.image = imageBitmap;
                    texture.needsUpdate = true;
                    return texture;
                })
                .then(onLoad, onError)
        } else {
            const loader = this.#imageLoader;
            loader.setCrossOrigin(this.crossOrigin);
            loader.setPath(this.path);

            loader.loadAsync(url, onProgress)
                .then(image => {
                    texture.image = image;
                    texture.needsUpdate = true;
                    return texture;
                })
                .then(onLoad, onError);
        }

        return texture;
    }
}
