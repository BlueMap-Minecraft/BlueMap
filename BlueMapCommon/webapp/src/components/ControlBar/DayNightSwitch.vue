<template>
  <SvgButton class="day-night-switch" :active="!isDay" @action="action">
    <svg viewBox="0 0 30 30">
      <path d="M17.011,19.722c-3.778-1.613-5.533-5.982-3.921-9.76c0.576-1.348,1.505-2.432,2.631-3.204
        c-3.418-0.243-6.765,1.664-8.186,4.992c-1.792,4.197,0.159,9.053,4.356,10.844c3.504,1.496,7.462,0.377,9.717-2.476
        C20.123,20.465,18.521,20.365,17.011,19.722z"/>
      <circle cx="5.123" cy="7.64" r="1.196"/>
      <circle cx="23.178" cy="5.249" r="1.195"/>
      <circle cx="20.412" cy="13.805" r="1.195"/>
      <circle cx="25.878" cy="23.654" r="1.195"/>
    </svg>
  </SvgButton>
</template>

<script>
import {animate, EasingFunctions} from "../../js/util/Utils";
import SvgButton from "./SvgButton.vue";

let animation;

export default {
    name: "DayNightSwitch",
  components: {SvgButton},
  data() {
      return {
        mapViewer: this.$bluemap.mapViewer.data
      }
    },
    computed: {
      isDay() {
        return this.mapViewer.uniforms.sunlightStrength.value > 0.6;
      }
    },
    methods: {
      action(evt) {

        evt.preventDefault();

        if (animation) animation.cancel();

        let startValue = this.mapViewer.uniforms.sunlightStrength.value;
        let targetValue = this.isDay ? 0.25 : 1;
        animation = animate(t => {
          let u = EasingFunctions.easeOutQuad(t);
          this.mapViewer.uniforms.sunlightStrength.value = startValue * (1-u) + targetValue * u;
        }, 300);
      }
    }
  }
</script>

<style lang="scss">
  .day-night-switch {
    svg {
      fill: var(--theme-moon-day);
      circle {
        fill: var(--theme-stars-day);
      }
    }

    &:active {
      svg {
        fill: var(--theme-moon-night);
        circle {
          fill: var(--theme-stars-night);
        }
      }
    }
  }
</style>