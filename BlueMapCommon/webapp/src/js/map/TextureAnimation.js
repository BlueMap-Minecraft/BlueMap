

export class TextureAnimation {

    /**
     * @param uniforms {{
     *      animationFrameHeight: { value: number },
     *      animationFrameIndex: { value: number },
     *      animationInterpolationFrameIndex: { value: number },
     *      animationInterpolation: { value: number }
     * }}
     * @param data {{
     *      interpolate: boolean,
     *      width: number,
     *      height: number,
     *      frametime: number,
     *      frames: {
     *         index: number,
     *         time: number
     *      }[] | undefined
     * }}
     */
    constructor(uniforms, data) {
        this.uniforms = uniforms;
        this.data = {
            interpolate: false,
            width: 1,
            height: 1,
            frametime: 1,
            ...data
        };
        this.frameImages = 1;
        this.frameDelta = 0;
        this.frameTime = this.data.frametime * 50;
        this.frames = 1;
        this.frameIndex = 0;
    }

    /**
     * @param width {number}
     * @param height {number}
     */
    init(width, height) {
        this.frameImages = height / width;
        this.uniforms.animationFrameHeight.value = 1 / this.frameImages;
        this.frames = this.frameImages;
        if (this.data.frames && this.data.frames.length > 0) {
            this.frames = this.data.frames.length;
        } else {
            this.data.frames = null;
        }
    }

    /**
     * @param delta {number}
     */
    step(delta) {
        this.frameDelta += delta;

        if (this.frameDelta > this.frameTime) {
            this.frameDelta -= this.frameTime;
            this.frameDelta %= this.frameTime;

            this.frameIndex++;
            this.frameIndex %= this.frames;

            if (this.data.frames) {
                let frame = this.data.frames[this.frameIndex]
                let nextFrame = this.data.frames[(this.frameIndex + 1) % this.frames];

                this.uniforms.animationFrameIndex.value = frame.index;
                this.uniforms.animationInterpolationFrameIndex.value = nextFrame.index;
                this.frameTime = frame.time * 50;
            } else {
                this.uniforms.animationFrameIndex.value = this.frameIndex;
                this.uniforms.animationInterpolationFrameIndex.value = (this.frameIndex + 1) % this.frames;
            }
        }

        if (this.data.interpolate) {
            this.uniforms.animationInterpolation.value = this.frameDelta / this.frameTime;
        }
    }

}