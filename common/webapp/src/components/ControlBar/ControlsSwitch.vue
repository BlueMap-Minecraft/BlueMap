<template>
  <div class="controls-switch" v-if="showViewControls">
    <SvgButton v-if="mapViewer.map.perspectiveView" :active="isPerspectiveView" @action="setPerspectiveView" :title="$t('controls.perspective.tooltip')">
      <svg viewBox="0 0 30 30">
        <path d="M19.475,10.574c-0.166-0.021-0.337-0.036-0.51-0.045c-0.174-0.009-0.35-0.013-0.525-0.011
          c-0.176,0.002-0.353,0.01-0.526,0.024c-0.175,0.015-0.347,0.036-0.515,0.063l-13.39,2.189
          c-0.372,0.061-0.7,0.146-0.975,0.247c-0.276,0.102-0.5,0.221-0.66,0.349c-0.161,0.129-0.259,0.268-0.282,0.408
          c-0.024,0.141,0.028,0.285,0.165,0.421l5.431,5.511c0.086,0.087,0.191,0.167,0.314,0.241s0.263,0.142,0.417,0.202
          c0.155,0.062,0.323,0.115,0.502,0.162c0.18,0.046,0.371,0.085,0.569,0.116s0.405,0.054,0.616,0.068
          c0.211,0.015,0.427,0.021,0.645,0.017c0.217-0.003,0.436-0.016,0.652-0.037c0.217-0.022,0.431-0.054,0.641-0.095L27.12,17.43
          c0.371-0.073,0.679-0.175,0.917-0.296c0.236-0.12,0.404-0.259,0.497-0.407c0.093-0.147,0.111-0.305,0.052-0.461
          c-0.059-0.156-0.195-0.313-0.415-0.46l-7.089-4.742c-0.089-0.06-0.192-0.115-0.308-0.166
          c-0.116-0.051-0.243-0.097-0.381-0.138c-0.137-0.041-0.283-0.078-0.438-0.108C19.803,10.621,19.641,10.595,19.475,10.574"/>
      </svg>
    </SvgButton>
    <SvgButton v-if="mapViewer.map.flatView" :active="isFlatView" @action="setFlatView" :title="$t('controls.flatView.tooltip')">
      <svg viewBox="0 0 30 30">
        <path d="M22.371,4.158c1.65,0,3,1.35,3,3v15.684c0,1.65-1.35,3-3,3H7.629c-1.65,0-3-1.35-3-3V7.158c0-1.65,1.35-3,3-3H22.371z"/>
      </svg>
    </SvgButton>
    <SvgButton v-if="mapViewer.map.freeFlightView" :active="isFreeFlight" @action="setFreeFlight" :title="$t('controls.freeFlight.tooltip')">
      <svg viewBox="0 0 30 30">
        <path d="M21.927,11.253c-0.256-0.487-0.915-0.885-1.465-0.885h-2.004c-0.55,0-0.726-0.356-0.39-0.792c0,0,0.698-0.905,0.698-2.041
          c0-2.08-1.687-3.767-3.767-3.767s-3.767,1.687-3.767,3.767c0,1.136,0.698,2.041,0.698,2.041c0.336,0.436,0.161,0.794-0.389,0.797
          l-2.005,0.01c-0.55,0.002-1.21,0.403-1.467,0.889l-3.656,6.924c-0.257,0.487-0.088,1.128,0.375,1.425l1.824,1.171
          c0.462,0.297,1.116,0.184,1.451-0.253l0.839-1.092c0.335-0.437,0.662-0.346,0.726,0.2l0.637,5.415
          c0.064,0.546,0.567,0.993,1.117,0.993h7.234c0.55,0,1.053-0.447,1.117-0.993l0.635-5.401c0.064-0.546,0.392-0.637,0.727-0.2
          l0.828,1.078c0.335,0.437,0.988,0.55,1.451,0.253l1.823-1.171c0.463-0.297,0.633-0.938,0.377-1.425L21.927,11.253z"/>
      </svg>
    </SvgButton>
  </div>
</template>

<script>
  import SvgButton from "./SvgButton.vue";

  export default {
    name: "ControlsSwitch",
    components: {SvgButton},
    data() {
      return {
        controls: this.$bluemap.appState.controls,
        mapViewer: this.$bluemap.mapViewer.data
      }
    },
    computed: {
      isPerspectiveView() {
        return this.controls.state === "perspective";
      },
      isFlatView() {
        return this.controls.state === "flat";
      },
      isFreeFlight() {
        return this.controls.state === "free";
      },
      showViewControls() {
        if (!this.mapViewer.map) return 0;
        return this.mapViewer.map.views.length > 1;
      }
    },
    methods: {
      setPerspectiveView() {
        this.$bluemap.setPerspectiveView(500, this.isFreeFlight ? 100 : 0);
      },
      setFlatView() {
        this.$bluemap.setFlatView(500, this.isFreeFlight ? 100 : 0);
      },
      setFreeFlight() {
        this.$bluemap.setFreeFlight(500);
      }
    }
  }
</script>

<style lang="scss">
  .controls-switch {
    display: flex;
  }
</style>