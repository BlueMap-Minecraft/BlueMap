<template>
  <div id="zoom-buttons">
    <SvgButton @action="zoom(-3)">
      <svg viewBox="0 0 30 30">
        <path d="M22.471,12.95H17.05V7.527c0-1.297-0.917-2.348-2.05-2.348c-1.132,0-2.05,1.051-2.05,2.348v5.423H7.527
	c-1.297,0-2.348,0.917-2.348,2.05c0,1.132,1.051,2.05,2.348,2.05h5.423v5.421c0,1.299,0.918,2.351,2.05,2.351
	c1.133,0,2.05-1.052,2.05-2.351V17.05h5.421c1.299,0,2.351-0.918,2.351-2.05C24.821,13.867,23.77,12.95,22.471,12.95z"/>
      </svg>
    </SvgButton>
    <SvgButton @action="zoom(3)">
      <svg viewBox="0 0 30 30">
        <g>
          <path d="M24.821,15c0,1.132-1.052,2.05-2.351,2.05H7.527c-1.297,0-2.348-0.918-2.348-2.05l0,0c0-1.133,1.051-2.05,2.348-2.05
		h14.944C23.77,12.95,24.821,13.867,24.821,15L24.821,15z"/>
        </g>
      </svg>
    </SvgButton>
  </div>
</template>

<script>
import SvgButton from "../ControlBar/SvgButton.vue";

export default {
  name: "ZoomButtons",
  components: {
    SvgButton
  },
  methods: {
    zoom(delta) {
      let mouseZoom = this.$bluemap.mapViewer.controlsManager.controls?.mouseZoom;
      if (mouseZoom) {
        mouseZoom.deltaZoom += delta;
      }
    }
  }
}
</script>

<style lang="scss">
  #zoom-buttons {
    position: fixed;
    bottom: 0;
    right: 0;

    display: flex;
    flex-direction: column;

    filter: drop-shadow(1px 1px 3px rgba(0, 0, 0, 0.53));
    width: 2em;

    margin: 0.5em;
  }
</style>