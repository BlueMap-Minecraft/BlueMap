<template>
  <div id="app" :class="{'theme-light': appState.theme === 'light', 'theme-dark': appState.theme === 'dark', 'theme-contrast': appState.theme === 'contrast'}">
    <FreeFlightMobileControls v-if="appState.controls.state === 'free'" />
    <ZoomButtons v-if="appState.controls.showZoomButtons && appState.controls.state !== 'free'" />
    <ControlBar />
    <MainMenu :menu="appState.menu" />
  </div>
</template>

<script>
import ControlBar from "./components/ControlBar/ControlBar.vue";
import MainMenu from "./components/Menu/MainMenu.vue";
import FreeFlightMobileControls from "./components/Controls/FreeFlightMobileControls.vue";
import ZoomButtons from "./components/Controls/ZoomButtons.vue";

export default {
  name: 'App',
  components: {
    FreeFlightMobileControls,
    MainMenu,
    ControlBar,
    ZoomButtons
  },
  data() {
    return {
      appState: this.$bluemap.appState,
    }
  }
}
</script>

<style lang="scss">
  @import "./scss/global.scss";

  #map-container {
    position: absolute;
    width: 100%;
    height: 100%;
  }

  #app {
    position: absolute;
    width: 100%;
    height: 100%;

    z-index: 100; // put over bluemap markers

    pointer-events: none;

    font-size: 1rem;
    @media (max-width: $mobile-break) {
      font-size: 1.5rem;
    }
  }
</style>
