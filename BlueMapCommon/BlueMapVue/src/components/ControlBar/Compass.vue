<template>
  <SvgButton class="compass" @action="action">
    <svg viewBox="0 0 30 30" :style="style">
      <path class="north" d="M14.792,1.04c0.114-0.354,0.299-0.354,0.412,0l4.089,12.729c0.114,0.353-0.097,0.642-0.468,0.642
        l-7.651,0.001c-0.371,0-0.581-0.288-0.468-0.642L14.792,1.04z"/>
      <path class="south" d="M10.707,16.23c-0.114-0.353,0.097-0.642,0.468-0.642l7.651-0.001c0.371,0,0.581,0.289,0.468,0.642
        l-4.086,12.73c-0.113,0.353-0.299,0.353-0.412,0L10.707,16.23z"/>
    </svg>
  </SvgButton>
</template>

<script>
  import {animate, EasingFunctions} from"@/js/util/Utils";
  import SvgButton from "@/components/ControlBar/SvgButton";

  let animation;

  export default {
    name: "Compass",
    components: {SvgButton},
    data() {
      return {
        controls: this.$bluemap.mapViewer.controlsManager.data
      }
    },
    computed: {
      style() {
        return {transform: "translate(-50%, -50%) rotate(" + (-this.controls.rotation) + "rad)"}
      }
    },
    methods: {
      action(evt) {
        evt.preventDefault();

        if (animation) animation.cancel();

        let startRotation = this.controls.rotation;
        animation = animate(t => {
          this.controls.rotation = startRotation * (1-EasingFunctions.easeOutQuad(t));
        }, 300);
      }
    }
  }
</script>

<style lang="scss">
  .compass {
    svg {
      height: 1.8em;

      .north {
        fill: var(--theme-fg);
      }

      .south {
        fill: var(--theme-fg-light);
      }
    }

    &:active {
      svg {
        .north {
          fill: var(--theme-bg);
        }

        .south {
          fill: var(--theme-bg-light);
        }
      }
    }
  }
</style>