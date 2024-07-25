<template>
  <div id="app" :class="{'theme-light': appState.theme === 'light', 'theme-dark': appState.theme === 'dark', 'theme-contrast': appState.theme === 'contrast'}">
    <FreeFlightMobileControls v-if="mapViewer.mapState === 'loaded' && appState.controls.state === 'free'" />
    <ZoomButtons v-if="showMapMenu && appState.controls.showZoomButtons && appState.controls.state !== 'free'" />
    <ControlBar />
    <div v-if="mapViewer.mapState !== 'loaded'" class="map-state-message">{{ $t("map." + mapViewer.mapState) }}</div>
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
  computed: {
    showMapMenu() {
      return this.mapViewer.mapState === "loading" || this.mapViewer.mapState === "loaded";
    }
  },
  data() {
    return {
      appState: this.$bluemap.appState,
      mapViewer: this.$bluemap.mapViewer.data
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

    z-index: 10000; // put over bluemap markers

    pointer-events: none;

    font-size: 1rem;
    @media (max-width: $mobile-break) {
      font-size: 1.5rem;
    }

    .map-state-message {
      position: fixed;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      color: var(--theme-fg-light);
      line-height: 1em;
      text-align: center;
    }
  }
</style>
