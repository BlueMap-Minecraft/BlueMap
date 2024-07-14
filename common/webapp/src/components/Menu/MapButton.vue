<template>
  <div class="map-button" :class="{selected: map.id === selectedMapId}" @click="switchMap(map.id)" :title="map.id">
    <span class="sky" :style="{color: 'rgb(' + map.skyColor.r * 255 + ',' + map.skyColor.g * 255 + ',' + map.skyColor.b * 255 + ')'}">&bull;</span>
    <span class="name">{{map.name}}</span>
  </div>
</template>

<script>
export default {
  name: "MapButton",
  props: {
    map: Object,
  },
  data() {
    return {
      mapViewer: this.$bluemap.mapViewer.data,
      appState: this.$bluemap.appState,
    }
  },
  computed: {
    selectedMapId() {
      return this.mapViewer.map ? this.mapViewer.map.id : null;
    }
  },
  methods: {
    switchMap(mapId) {
      this.$bluemap.switchMap(mapId);
    }
  }
}
</script>

<style lang="scss">
.side-menu .map-button {
  position: relative;
  cursor: pointer;
  user-select: none;

  height: 2em;
  line-height: 2em;

  white-space: nowrap;
  overflow-x: hidden;
  text-overflow: ellipsis;

  &.selected {
    background-color: var(--theme-bg-light);
  }

  &:hover {
    background-color: var(--theme-bg-hover);
  }

  .sky {
    float: left;
    border-radius: 100%;

    width: 0.5em;
    height: 0.5em;

    margin: 0 0.25em 0 0.5em;
  }

  .id {
    font-style: italic;
    color: var(--theme-fg-light);

    margin: 0 0.5em;
  }
}
</style>