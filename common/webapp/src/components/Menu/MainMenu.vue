<template>
  <SideMenu :open="menu.isOpen"
            :title="menu.currentPage().title"
            :back="menu.pageStack.length > 1"
            @back="menu.closePage()"
            @close="menu.closeAll()">

    <div v-if="menu.currentPage().id === 'root'">
      <SimpleButton @action="menu.openPage('maps', () => $t('maps.title'))" :submenu="true">{{ $t("maps.button") }}</SimpleButton>
      <SimpleButton @action="menu.openPage('markers', () => $t('markers.title'), {markerSet: markers})" :submenu="true">{{ $t("markers.button") }}</SimpleButton>
      <SimpleButton @action="menu.openPage('settings', () => $t('settings.title'))" :submenu="true">{{ $t("settings.button") }}</SimpleButton>
      <SimpleButton @action="menu.openPage('info', () => $t('info.title'))" :submenu="true">{{ $t("info.button") }}</SimpleButton>
      <hr>
      <SimpleButton @action="goFullscreen">{{ $t("goFullscreen.button") }}</SimpleButton>
      <SimpleButton @action="$bluemap.resetCamera()">{{ $t("resetCamera.button") }}</SimpleButton>
      <SimpleButton @action="$bluemap.takeScreenshot()">{{ $t("screenshot.button") }}</SimpleButton>
      <SimpleButton @action="$bluemap.updateMap()" :title="$t('updateMap.tooltip')">{{ $t("updateMap.button") }}</SimpleButton>
    </div>

    <div v-if="menu.currentPage().id === 'maps'">
      <MapButton v-for="map of appState.maps" :key="map.id" :map="map" />
    </div>

    <MarkerSetMenu v-if="menu.currentPage().id === 'markers'" :menu="menu" />

    <SettingsMenu v-if="menu.currentPage().id === 'settings'" />

    <div class="info-content" v-if="menu.currentPage().id === 'info'" v-html="$t('info.content', {
      version: $bluemap.settings.version
    })"></div>

  </SideMenu>
</template>

<script>
import SideMenu from "./SideMenu.vue";
import SimpleButton from "./SimpleButton.vue";
import SettingsMenu from "./SettingsMenu.vue";
import {MainMenu} from "../../js/MainMenu";
import MarkerSetMenu from "./MarkerSetMenu.vue";
import MapButton from "./MapButton.vue";

export default {
  name: "MainMenu",
  components: {MapButton, MarkerSetMenu, SettingsMenu, SimpleButton, SideMenu},
  props: {
    menu: MainMenu
  },
  data() {
    return {
      appState: this.$bluemap.appState,
      markers: this.$bluemap.mapViewer.markers.data,
    }
  },
  methods: {
    goFullscreen() {
      document.body.requestFullscreen();
    }
  }
}
</script>

<style lang="scss">
.info-content {
  font-size: 0.8em;

  table {
    border-collapse: collapse;
    width: 100%;

    tr {
      th, td {
        padding: 0.2em 0.5em;
        border: solid 1px var(--theme-bg-light);
      }

      th {
        font-weight: inherit;
        text-align: inherit;
      }
    }
  }

  .info-footer {
    text-align: center;
  }
}
</style>