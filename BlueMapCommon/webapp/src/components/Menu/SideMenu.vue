<template>
  <Transition name="side-menu" @enter="buttonEnterAnimation(); $emit('enter', $event)">
    <div class="side-menu" v-if="open">
      <MenuButton :close="open && rendered" :back="back" @action="$emit('back', $event)" />
      <MenuButton class="full-close" v-if="open && back" :close="true" @action="$emit('close', $event)" />
      <div class="title">{{ title }}</div>
      <div class="content">
        <slot />
      </div>
    </div>
  </Transition>
</template>

<script>
import MenuButton from "../ControlBar/MenuButton.vue";

export default {
  name: "SideMenu",
  components: {MenuButton},
  props: {
    title: {
      type: String,
      default: "Menu"
    },
    open: {
      type: Boolean,
      default: true
    },
    back: Boolean
  },
  data() {
    return {
      rendered: false
    }
  },
  methods: {
    async buttonEnterAnimation() {
      this.rendered = false;
      await this.$nextTick();
      await this.$nextTick();
      this.rendered = true;
    }
  }
}
</script>

<style lang="scss">
@import "/src/scss/variables.scss";

.side-menu {
  position: fixed;
  top: 0;
  left: 0;

  overflow: hidden;

  pointer-events: auto;

  width: 100%;
  max-width: 20em;
  height: 100%;

  filter: drop-shadow(1px 1px 3px #0008);

  background-color: var(--theme-bg);
  color: var(--theme-fg);

  &-enter-active, &-leave-active {
    transition: opacity 0.3s;
  }

  &-enter, &-leave-to {
    opacity: 0;
    pointer-events: none;

    * {
      pointer-events: none !important;
    }
  }

  > .menu-button {
    position: absolute;
    top: 0;
    left: 0;

    margin: 0.5em;

    @media (max-width: $mobile-break) {
      margin: 0;
    }

    &.full-close {
      right: 0;
      left: unset;
    }
  }

  > .title {
    line-height: 2em;
    text-align: center;

    background-color: inherit;
    border-bottom: solid 1px var(--theme-bg-hover);

    padding: 0.5em;
    @media (max-width: $mobile-break) {
      padding: 0;
    }
  }

  > .content {
    position: relative;

    overflow-y: auto;
    overflow-x: hidden;

    padding: 0.5em;

    height: calc(100% - 4em - 1px);
    @media (max-width: $mobile-break) {
      height: calc(100% - 3em - 1px);
    }

    hr {
      border: none;
      border-bottom: solid 2px var(--theme-bg-hover);
      margin: 0.5em 0;
    }
  }
}
</style>