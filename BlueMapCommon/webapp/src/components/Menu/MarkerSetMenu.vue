<template>
<div>
  <div class="marker-sets">
    <MarkerSet v-for="markerSet of filteredMarkerSets" :key="markerSet.id" :marker-set="markerSet" @more="openMore(markerSet)" />
  </div>
  <hr v-if="filteredMarkerSets.length > 0 & thisMarkerSet.markers.length > 0">
  <div class="markers" v-if="thisMarkerSet.markers.length > 0">
    <TextInput :value="filter.search" @input="filter.search = $event.target.value" :placeholder="$t('markers.searchPlaceholder')" />
    <ChoiceBox
        :title="$t('markers.sort.title')"
        :choices="[
          {id: 'default', name: $t('markers.sort.by.default')},
          {id: 'label', name: $t('markers.sort.by.label')},
          {id: 'distance', name: $t('markers.sort.by.distance')}
        ]"
        :selection="filter.order"
        @choice="filter.order = $event.id"
    />
    <MarkerItem v-for="marker of filteredMarkers" :key="marker.id" :marker="marker" />
  </div>
</div>
</template>

<script>
import MarkerItem from "./MarkerItem.vue";
import TextInput from "./TextInput.vue";
import MarkerSet from "./MarkerSet.vue";
import Group from "./Group.vue";
import SimpleButton from "./SimpleButton.vue";
import {MainMenu} from "../../js/MainMenu";
import ChoiceBox from "./ChoiceBox.vue";

export default {
  name: "MarkerSetMenu",
  components: {ChoiceBox, SimpleButton, MarkerSet, TextInput, MarkerItem, Group},
  props: {
    menu: MainMenu
  },
  data() {
    return {
      controls: this.$bluemap.mapViewer.controlsManager.data,
      filter: {
        search: "",
        order: "default"
      }
    }
  },
  computed: {
    thisMarkerSet() {
      return this.menu.currentPage().markerSet;
    },
    filteredMarkers() {
      return this.thisMarkerSet.markers.filter(marker => {
        if (!marker.listed) return false;
        if (!this.filter.search) return true;
        if (marker.id.includesCI(this.filter.search)) return true;
        if (marker.label && marker.label.includesCI(this.filter.search)) return true;
        return marker.type === "player" && (marker.name.includesCI(this.filter.search) || marker.playerUuid.includesCI(this.filter.search));
      }).sort((a, b) => {
        if (this.filter.order === "label") {
          let la = (a.type === "player" ? a.name : a.label).toLowerCase();
          let lb = (b.type === "player" ? b.name : b.label).toLowerCase();
          if (la < lb) return -1;
          if (la > lb) return 1;
          return 0;
        }
        if (this.filter.order === "distance") {
          return a.position.distanceToSquared(this.controls.position) - b.position.distanceToSquared(this.controls.position);
        }
        return (a.sorting || 0) - (b.sorting || 0);
      });
    },
    filteredMarkerSets() {
      return this.thisMarkerSet.markerSets.filter(markerSet => {
        return markerSet.listed;
      }).sort((a, b) => {
        return (a.sorting || 0) - (b.sorting || 0);
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