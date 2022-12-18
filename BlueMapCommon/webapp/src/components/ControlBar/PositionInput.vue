<template>
  <div class="position-input">
    <NumberInput label="x" :value="controls.position.x" v-on:input="controls.position.x = parseFloat($event.target.value);" />
    <NumberInput label="y" :value="controls.position.y" v-on:input="controls.position.y = parseFloat($event.target.value);" v-if="appState.controls.state === 'free'" />
    <NumberInput label="z" :value="controls.position.z" v-on:input="controls.position.z = parseFloat($event.target.value);" />
  </div>
</template>

<script>
  import NumberInput from "@/components/ControlBar/NumberInput";

  export default {
    name: "PositionInput",
    components: {
      NumberInput
    },
    data() {
      return {
        controls: this.$bluemap.mapViewer.controlsManager.data,
        appState: this.$bluemap.appState
      }
    }
  }
</script>

<style lang="scss">
  .position-input {
    display: flex;
    user-select: none;

    > * {
      width: 100%;

      &:not(:first-child) {
        border-left: solid 1px var(--theme-bg-light);
      }
    }
  }
</style>