<template>
  <div id="ff-mobile-controls" :class="{disabled: !enabled}">
    <div class="move-fields">
      <div class="button up-button" @touchstart="forward = 1; forwardPointer = $event.changedTouches[0].identifier; $event.preventDefault();">
        <svg viewBox="0 0 100 50">
          <path d="M6.75,48.375c-2.75,0-3.384-1.565-1.409-3.479L46.41,5.104c1.975-1.914,5.207-1.913,7.182,0l41.067,39.792
            c1.975,1.914,1.341,3.479-1.409,3.479H6.75z"/>
        </svg>
      </div>
      <div class="button down-button" @touchstart="forward = -1; forwardPointer = $event.changedTouches[0].identifier; $event.preventDefault();">
        <svg viewBox="0 0 100 50" class="down">
          <path d="M6.75,48.375c-2.75,0-3.384-1.565-1.409-3.479L46.41,5.104c1.975-1.914,5.207-1.913,7.182,0l41.067,39.792
            c1.975,1.914,1.341,3.479-1.409,3.479H6.75z"/>
        </svg>
      </div>
    </div>
    <div class="height-fields">
      <div class="button up-button" @touchstart="up = 1; upPointer = $event.changedTouches[0].identifier; $event.preventDefault();">
        <svg viewBox="0 0 100 50">
          <path d="M6.75,48.375c-2.75,0-3.384-1.565-1.409-3.479L46.41,5.104c1.975-1.914,5.207-1.913,7.182,0l41.067,39.792
            c1.975,1.914,1.341,3.479-1.409,3.479H6.75z"/>
        </svg>
      </div>
      <div class="button down-button" @touchstart="up = -1; upPointer = $event.changedTouches[0].identifier; $event.preventDefault();">
        <svg viewBox="0 0 100 50" class="down">
          <path d="M6.75,48.375c-2.75,0-3.384-1.565-1.409-3.479L46.41,5.104c1.975-1.914,5.207-1.913,7.182,0l41.067,39.792
            c1.975,1.914,1.341,3.479-1.409,3.479H6.75z"/>
        </svg>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "FreeFlightMobileControls",
  data() {
    return {
      enabled: false,
      forward: 0,
      forwardPointer: -1,
      up: 0,
      upPointer: -1,
    }
  },
  methods: {
    onTouchStop(evt) {
      console.log("Stop: ", evt);

      if (evt.changedTouches[0].identifier === this.forwardPointer) this.forward = 0;
      if (evt.changedTouches[0].identifier === this.upPointer) this.up = 0;
    },
    onFrame(evt) {
      let cm = this.$bluemap.mapViewer.controlsManager;
      cm.position.x += this.forward * Math.sin(cm.rotation) * evt.detail.delta * 0.02;
      cm.position.z += this.forward * -Math.cos(cm.rotation) * evt.detail.delta * 0.02;
      cm.position.y += this.up * evt.detail.delta * 0.01;
    },
    enable(evt) {
      this.enabled = true;
    },
  },
  mounted() {
    window.addEventListener("touchstart", this.enable);

    window.addEventListener("touchend", this.onTouchStop);
    window.addEventListener("touchcancel", this.onTouchStop);
    this.$bluemap.events.addEventListener("bluemapRenderFrame", this.onFrame);
  },
  beforeDestroy() {
    window.removeEventListener("touchstart", this.enable);

    window.removeEventListener("touchend", this.onTouchStop);
    window.removeEventListener("touchcancel", this.onTouchStop);
    this.$bluemap.events.removeEventListener("bluemapRenderFrame", this.onFrame);
  }
}
</script>

<style lang="scss">

#ff-mobile-controls {
  font-size: 15vw;

  &.disabled {
    display: none;
  }

  @media (orientation: portrait) {
    font-size: 15vh;
  }

  .button {
    width: 1em;
    margin: 0.1em;
    opacity: 0.5;

    pointer-events: auto;

    svg {
      fill: var(--theme-bg);

      &:active {
        fill: var(--theme-bg-light);
        opacity: 0.8;
      }

      &.down {
        transform: scaleY(-1);
      }
    }
  }

  .move-fields {
    position: fixed;

    bottom: 0.2em;
    left: 0.2em;

  }

  .height-fields {
    position: fixed;

    bottom: 0.2em;
    right: 0.2em;
  }

}

</style>