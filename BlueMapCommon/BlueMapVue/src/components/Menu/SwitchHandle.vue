<template>
  <div class="switch" :class="{on: on}"></div>
</template>

<script>
export default {
  name: "SwitchHandle",
  props: {
    on: Boolean
  }
}
</script>

<style lang="scss">
.side-menu .switch {
  height: 1em;
  width: 2em;

  border-radius: 1em;
  background-color: var(--theme-bg-light);

  transition: background-color 0.3s;

  &::after {
    content: "";
    display: block;
    width: 0.75em;
    height: 0.75em;
    border-radius: 100%;

    background-color: var(--theme-bg);

    position: relative;
    top: 0.125em;
    left: 0.125em;

    transition: left 0.3s;
  }

  &.on {
    background-color: var(--theme-switch-button-on);

    &::after {
      left: 1.125em;
    }
  }
}
</style>