<template>
<div>
  <div class="marker-sets">
    <MarkerSet v-for="markerSet of filteredMarkerSets" :key="markerSet.id" :marker-set="markerSet" @more="openMore(markerSet)" />
  </div>
  <hr v-if="filteredMarkerSets.length > 0 & thisMarkerSet.markers.length > 0">
  <div class="markers" v-if="thisMarkerSet.markers.length > 0">
    <TextInput :value="filter.search" @input="filter.search = $event.target.value" :placeholder="$t('markers.searchPlaceholder')" />
    <MarkerItem v-for="marker of filteredMarkers" :key="marker.id" :marker="marker" />
  </div>
</div>
</template>

<script>
import MarkerItem from "./MarkerItem.vue";
import TextInput from "./TextInput.vue";
import MarkerSet from "./MarkerSet.vue";
import {MainMenu} from "../../js/MainMenu";
export default {
  name: "MarkerSetMenu",
  components: {MarkerSet, TextInput, MarkerItem},
  props: {
    menu: MainMenu
  },
  data() {
    return {
      filter: {
        search: "",
      }
    }
  },
  computed: {
    thisMarkerSet() {
      return this.menu.currentPage().markerSet;
    },
    filteredMarkers() {
      return [...this.thisMarkerSet.markers].sort((a, b) => {
        if (a.id < b.id) return -1;
        if (a.id > b.id) return 1;
        return 0;
      }).filter(marker => {
        if (!this.filter.search) return true;
        if (marker.id.includesCI(this.filter.search)) return true;
        if (marker.label && marker.label.includesCI(this.filter.search)) return true;
        return marker.type === "player" && (marker.name.includesCI(this.filter.search) || marker.playerUuid.includesCI(this.filter.search));
      });
    },
    filteredMarkerSets() {
      return this.thisMarkerSet.markerSets.filter(markerSet => {
        return (markerSet.id !== "bm-popup-set");
      });
    }
  },
  methods: {
    openMore(markerSet) {
      this.menu.openPage(
          this.menu.currentPage().id,
          this.menu.currentPage().title + " > " + markerSet.label,
          {markerSet: markerSet}
      )
    }
  }
}
</script>

<style>

</style>