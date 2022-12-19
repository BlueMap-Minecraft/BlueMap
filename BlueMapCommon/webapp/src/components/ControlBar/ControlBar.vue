<template>
  <div class="control-bar">
    <MenuButton :close="appState.menu.isOpen" :back="false" @action="appState.menu.reOpenPage()" :title="$t('menu.tooltip')" />
    <div class="space thin-hide"></div>
    <SvgButton v-if="appState.maps.length > 0" class="thin-hide" :title="$t('maps.tooltip')"
               @action="appState.menu.openPage('maps', $t('maps.title'))">
      <svg viewBox="0 0 30 30">
        <polygon points="26.708,22.841 19.049,25.186 11.311,20.718 3.292,22.841 7.725,5.96 13.475,4.814 19.314,7.409 25.018,6.037 "/>
      </svg>
    </SvgButton>
    <SvgButton v-if="markers.markerSets.length > 0 || markers.markers.length > 0" class="thin-hide" :title="$t('markers.tooltip')"
               @action="appState.menu.openPage('markers', $t('markers.title'), {markerSet: markers})">
      <svg viewBox="0 0 30 30">
        <path d="M15,3.563c-4.459,0-8.073,3.615-8.073,8.073c0,6.483,8.196,14.802,8.196,14.802s7.951-8.013,7.951-14.802
			C23.073,7.177,19.459,3.563,15,3.563z M15,15.734c-2.263,0-4.098-1.835-4.098-4.099c0-2.263,1.835-4.098,4.098-4.098
			c2.263,0,4.098,1.835,4.098,4.098C19.098,13.899,17.263,15.734,15,15.734z"/>
      </svg>
    </SvgButton>
    <SvgButton v-if="!playerMarkerSet.fake" class="thin-hide" :title="$t('players.tooltip')" @action="openPlayerList">
      <svg viewBox="0 0 30 30">
        <g>
          <path d="M8.95,14.477c0.409-0.77,1.298-1.307,2.164-1.309h0.026c-0.053-0.234-0.087-0.488-0.087-0.755
			c0-1.381,0.715-2.595,1.791-3.301c-0.01,0-0.021-0.006-0.03-0.006h-1.427c-0.39,0-0.514-0.251-0.276-0.563
			c0,0,0.497-0.645,0.497-1.452c0-1.48-1.2-2.681-2.679-2.681c-1.481,0-2.679,1.2-2.679,2.681c0,0.807,0.496,1.452,0.496,1.452
			c0.24,0.311,0.114,0.565-0.275,0.565L5.042,9.118C4.649,9.119,4.182,9.405,3.998,9.75l-2.601,4.927
			c-0.184,0.347-0.062,0.802,0.265,1.015l1.297,0.83c0.332,0.213,0.794,0.135,1.034-0.18l0.598-0.775
			c0.238-0.31,0.471-0.245,0.516,0.141l0.454,3.854c0.035,0.311,0.272,0.566,0.564,0.66c0.018-0.279,0.087-0.561,0.225-0.82
			L8.95,14.477z"/>
          <path d="M28.604,14.677l-2.597-4.94c-0.185-0.346-0.65-0.631-1.042-0.631h-1.428c-0.39,0-0.514-0.251-0.274-0.563
			c0,0,0.496-0.645,0.496-1.452c0-1.48-1.2-2.681-2.68-2.681c-1.481,0-2.679,1.2-2.679,2.681c0,0.807,0.496,1.452,0.496,1.452
			c0.239,0.311,0.114,0.565-0.275,0.565l-1.428,0.009c-0.005,0-0.009,0.002-0.015,0.002c1.067,0.708,1.774,1.917,1.774,3.292
			c0,0.263-0.031,0.513-0.084,0.744h0.02c0.868,0,1.758,0.537,2.166,1.305l2.598,4.944c0.137,0.262,0.205,0.539,0.222,0.818
			c0.296-0.092,0.538-0.35,0.574-0.664l0.451-3.842c0.044-0.389,0.28-0.452,0.519-0.143l0.588,0.768
			c0.239,0.313,0.702,0.391,1.033,0.182l1.297-0.833C28.667,15.479,28.787,15.026,28.604,14.677z"/>
        </g>
        <path d="M19.932,15.058c-0.184-0.346-0.651-0.63-1.043-0.63h-1.427c-0.39,0-0.515-0.252-0.275-0.564c0,0,0.496-0.645,0.496-1.451
		c0-1.479-1.199-2.68-2.679-2.68c-1.482,0-2.679,1.201-2.679,2.68c0,0.806,0.496,1.451,0.496,1.451
		c0.24,0.312,0.114,0.566-0.275,0.566l-1.427,0.009c-0.393,0.001-0.861,0.287-1.045,0.632l-2.602,4.925
		c-0.185,0.348-0.062,0.803,0.266,1.016l1.297,0.832c0.332,0.213,0.794,0.133,1.034-0.18l0.598-0.775
		c0.239-0.311,0.472-0.246,0.517,0.141l0.454,3.854c0.043,0.389,0.403,0.705,0.794,0.705h5.148c0.392,0,0.749-0.316,0.794-0.705
		l0.45-3.844c0.045-0.389,0.282-0.451,0.52-0.143l0.587,0.768c0.239,0.313,0.703,0.393,1.033,0.182l1.297-0.832
		c0.331-0.213,0.451-0.666,0.269-1.016L19.932,15.058z"/>
      </svg>
    </SvgButton>
    <div class="space thin-hide greedy"></div>
    <DayNightSwitch class="thin-hide" :title="$t('lighting.dayNightSwitch.tooltip')" />
    <div class="space thin-hide"></div>
    <ControlsSwitch class="thin-hide"></ControlsSwitch>
    <div class="space thin-hide"></div>
    <SvgButton class="thin-hide" :title="$t('resetCamera.tooltip')" @action="$bluemap.resetCamera()">
      <svg viewBox="0 0 30 30">
        <rect x="7.085" y="4.341" transform="matrix(0.9774 0.2116 -0.2116 0.9774 3.2046 -1.394)" width="2.063" height="19.875"/>
        <path d="M12.528,5.088c0,0,3.416-0.382,4.479-0.031c1.005,0.332,2.375,2.219,3.382,2.545c1.096,0.354,4.607-0.089,4.607-0.089
      l-2.738,8.488c0,0-3.285,0.641-4.344,0.381c-1.049-0.257-2.607-2.015-3.642-2.324c-0.881-0.264-3.678-0.052-3.678-0.052
      L12.528,5.088z"/>
      </svg>
    </SvgButton>
    <PositionInput class="pos-input" />
    <Compass :title="$t('compass.tooltip')" />
  </div>
</template>

<script>
  import PositionInput from "./PositionInput.vue";
  import Compass from "./Compass.vue";
  import DayNightSwitch from "./DayNightSwitch.vue";
  import ControlsSwitch from "./ControlsSwitch.vue";
  import MenuButton from "./MenuButton.vue";
  import SvgButton from "./SvgButton.vue";

  export default {
    name: "ControlBar",
    components: {
      SvgButton,
      MenuButton,
      ControlsSwitch,
      DayNightSwitch,
      PositionInput,
      Compass
    },
    data() {
      return {
        appState: this.$bluemap.appState,
        markers: this.$bluemap.mapViewer.markers.data,
      }
    },
    computed: {
      playerMarkerSet() {
        for (let set of this.markers.markerSets) {
          if (set.id === "bm-players") return set;
        }

        return {
          id: "bm-players",
          label: "Players",
          markerSets: [],
          markers: [],
          fake: true,
        }
      }
    },
    methods: {
      openPlayerList() {
        let playerList = this.playerMarkerSet;
        this.appState.menu.openPage('markers', this.$t("players.title"), {markerSet: playerList});
      }
    }
  }
</script>

<style lang="scss">
@import "/src/scss/variables.scss";

  .control-bar {
    position: fixed;
    top: 0;
    left: 0;

    display: flex;

    filter: drop-shadow(1px 1px 3px rgba(0, 0, 0, 0.53));
    height: 2em;

    margin: 0.5em;
    width: calc(100% - 1em);

    .pos-input {
      max-width: 20em;
      width: 100%;
    }

    > :not(:first-child) {
      border-left: solid 1px var(--theme-bg-light);
    }

    .space {
      width: 0.5em;
      flex-shrink: 0;

      &.greedy {
        flex-grow: 1;
      }
    }

    .space, .space + * {
      border-left: none;
    }

    @media (max-width: $mobile-break) {
      margin: 0;
      width: 100%;

      background-color: var(--theme-bg-light);

      .pos-input {
        max-width: unset;
      }

      .thin-hide {
        display: none;
      }

      .space {
        width: 1px;
      }
    }

  }
</style>