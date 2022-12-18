<template>
  <div class="group">
    <span class="title">{{ title }}</span>
    <div class="content">
      <slot/>
    </div>
  </div>
</template>

<script>
export default {
  name: "Group",
  props: {
    title: String
  }
}
</script>

<style lang="scss">
.side-menu .group {
  position: relative;
  margin: 2em 0 1em;
  padding-top: 1em;
  border: solid 2px var(--theme-bg-hover);


  > .title {
    position: absolute;
    top: calc(-0.5em - 1px);
    right: 0.5em;
    padding: 0 0.5em;
    background-color: var(--theme-bg);
  }

  &:first-child {
    margin-top: 1em;
  }

  > .content {
    max-height: 15em;
    overflow-y: auto;
  }
}
</style>