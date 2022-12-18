<template>
  <div class="svg-button" :class="{active: active}" @click="$emit('action', $event)">
    <slot />
  </div>
</template>

<script>
  export default {
    name: "SvgButton",
    props: {
      active: Boolean,
    }
  }
</script>

<style lang="scss">
.svg-button {
  position: relative;
  pointer-events: auto;
  overflow: hidden;
  cursor: pointer;

  min-width: 2em;
  min-height: 2em;

  background-color: var(--theme-bg);
  color: var(--theme-fg);

  &:hover {
    background-color: var(--theme-bg-hover);
  }

  &.active {
    background-color: var(--theme-bg-light);
  }

  &:active {
    background-color: var(--theme-fg-light);
    color: var(--theme-bg);
  }

  svg {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);

    height: 1.8em;

    fill: var(--theme-fg-light);
  }

  &:active {
    svg {
      fill: var(--theme-bg-light);
    }
  }
}
</style>