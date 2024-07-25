<template>
  <div class="marker-set" :title="markerSet.id">
    <div class="info" @click="toggle">
      <div class="marker-set-switch">
        <div class="label">{{ label }}</div>
        <SwitchHandle :on="markerSet.visible" v-if="markerSet.toggleable"/>
      </div>
      <div class="stats">
        <div v-if="filteredMarkerCount > 0">
          {{ filteredMarkerCount }}
          {{ $t('markers.marker', filteredMarkerCount) }}
        </div>
        <div v-if="filteredMarkerSetCount > 0">
          {{ filteredMarkerSetCount }}
          {{ $t('markers.markerSet', filteredMarkerSetCount) }}
        </div>
      </div>
    </div>
    <div class="open-menu-button"
         :class="{active: active}"
         @click="more($event)">
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
    filteredMarkerSetCount() {
      let count = 0;
      for (let markerSet of this.markerSet.markerSets) {
        if (markerSet.listed) count++;
      }
      return count;
    },
    filteredMarkerCount() {
      let count = 0;
      for (let marker of this.markerSet.markers) {
        if (marker.listed) count++;
      }
      return count;
    },
    label() {
      if (this.markerSet.id === "bm-players") return this.$t("players.title");
      return this.markerSet.label;
    },
    active() {
      for (let marker of this.markerSet.markers) {
        if (marker.listed) return true;
      }
      for (let markerSet of this.markerSet.markerSets) {
        if (markerSet.listed) return true;
      }
      return false;
    }
  },
  methods: {
    toggle() {
      if (this.markerSet.toggleable) {
        // eslint-disable-next-line vue/no-mutating-props
        this.markerSet.visible = !this.markerSet.visible
        this.markerSet.saveState();
      }
    },
    more(event) {
      if (this.active) {
        this.$emit('more', event);
      }
    }
  }
}
</script>

<style lang="scss">
.side-menu .marker-set {
  display: flex;
  user-select: none;

  line-height: 1em;

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

    padding: 0.5em;

    &:hover {
      background-color: var(--theme-bg-hover);
    }

    > .marker-set-switch {
      position: relative;

      .label {
        margin: 0 2.5em 0 0;
      }

      > .switch {
        position: absolute;
        top: 0;
        right: 0;
      }
    }

    > .stats {
      display: flex;
      font-size: 0.8em;
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

    &.active {
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

    &:not(.active) {
      svg {
        display: none;
      }
    }
  }
}
</style>