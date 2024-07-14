<template>
<div class="simple-button" :class="{active: active}" @click="$emit('action')">
  <div class="label"><slot /></div>
  <div class="submenu-icon" v-if="submenu">
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
export default {
  name: "SimpleButton",
  props: {
    submenu: Boolean,
    active: {
      type: Boolean,
      default: false,
    }
  }
}
</script>

<style lang="scss">

.side-menu .simple-button {
  cursor: pointer;
  user-select: none;
  display: flex;
  line-height: 2em;

  padding: 0 0.5em;

  > .label {
    flex-grow: 1;

    white-space: nowrap;
    overflow-x: hidden;
    text-overflow: ellipsis;
  }

  &:hover {
    background-color: var(--theme-bg-hover);
  }

  &.active {
    background-color: var(--theme-bg-light);
  }

  > .submenu-icon {
    width: 2em;
    height: 2em;

    flex-shrink: 0;

    margin-right: -0.5em;

    > svg {
      fill: var(--theme-fg-light);

      path:nth-child(1) {
        transform-origin: 15px 9px;
        transform: translate(0, 10px) rotate(-30deg);
      }
      path:nth-child(2) {
        transform-origin: 15px 21px;
        transform: translate(0, -10px) rotate(30deg);
      }

      transform: scale(0.75);
    }
  }

  &:active {
    background-color: var(--theme-fg-light);
    color: var(--theme-bg);


    > .submenu-icon > svg {
      fill: var(--theme-bg-light);
    }
  }
}

</style>