<template>
  <div class="marker-set" :title="markerSet.id">
    <div class="info" @click="toggle">
      <div class="marker-set-switch">
        <div class="label">{{ label }}</div>
        <SwitchHandle :on="markerSet.visible" v-if="markerSet.toggleable"/>
      </div>
      <div class="stats">
        <div>
          {{ markerSet.markers.length }}
          {{ $t('markers.marker', markerSet.markers.length) }}
        </div>
        <div v-if="filteredMarkerSets.length > 0">
          {{ filteredMarkerSets.length }}
          {{ $t('markers.markerSet', filteredMarkerSets.length) }}
        </div>
      </div>
    </div>
    <div class="open-menu-button" @click="$emit('more', $event)">
      <svg viewBox="0 0 30 30">
        <path d="M25.004,9.294c0,0.806-0.75,1.46-1.676,1.46H6.671c-0.925,0-1.674-0.654-1.674-1.46l0,0
	c0-0.807,0.749-1.461,1.674-1.461h16.657C24.254,7.833,25.004,8.487,25.004,9.294L25.004,9.294z"/>
        <path d="M25.004,20.706c0,0.807-0.75,1.461-1.676,1.461H6.671c-0.925,0-1.674-0.654-1.674-1.461l0,0
	c0-0.807,0.749-1.461,1.674-1.461h16.657C24.254,19.245,25.004,19.899,25.004,20.706L25.004,20.706z"/>
      </svg>
    </div>
  </div>
</template>

<script>
import SwitchHandle from "./SwitchHandle.vue";

export default {
  name: "MarkerSet",
  components: {SwitchHandle},
  props: {
    markerSet: Object,
  },
  computed: {
    filteredMarkerSets() {
      return this.markerSet.markerSets.filter(markerSet => {
        return (markerSet.id !== "bm-popup-set");
      });
    },
    label() {
      if (this.markerSet.id === "bm-players") return this.$t("players.title");
      return this.markerSet.label;
    }
  },
  methods: {
    toggle() {
      if (this.markerSet.toggleable) {
        // eslint-disable-next-line vue/no-mutating-props
        this.markerSet.visible = !this.markerSet.visible
      }
    }
  }
}
</script>

<style lang="scss">
.side-menu .marker-set {
  display: flex;
  user-select: none;

  line-height: 2em;

  margin: 0.5em 0;

  &:first-child {
    margin-top: 0;
  }

  &:last-child {
    margin-bottom: 0;
  }

  > .info {
    flex-grow: 1;
    cursor: pointer;

    &:hover {
      background-color: var(--theme-bg-hover);
    }

    > .marker-set-switch {
      position: relative;

      .label {
        margin: 0 3em 0 0.5em;
      }

      > .switch {
        position: absolute;
        top: 0.5em;
        right: 0.5em;
      }
    }

    > .stats {
      display: flex;

      margin: 0 0.5em;

      font-size: 0.8em;
      line-height: 2em;
      color: var(--theme-fg-light);

      > div {
        &:not(:first-child) {
          margin-left: 0.5em;
          padding-left: 0.5em;
          border-left: solid 1px var(--theme-bg-light);
        }
      }
    }
  }

  > .open-menu-button {
    width: 2em;
    cursor: pointer;

    &:hover {
      background-color: var(--theme-bg-hover);
    }

    > svg {
      position: relative;
      fill: var(--theme-fg-light);

      top: 50%;
      transform: translate(0, -50%) scale(0.75);

      path:nth-child(1) {
        transform-origin: 15px 9px;
        transform: translate(0, 10px) rotate(-30deg);
      }

      path:nth-child(2) {
        transform-origin: 15px 21px;
        transform: translate(0, -10px) rotate(30deg);
      }
    }

    &:active {
      background-color: var(--theme-fg-light);
      color: var(--theme-bg);

      > svg {
        fill: var(--theme-bg-light);
      }
    }
  }
}
</style>