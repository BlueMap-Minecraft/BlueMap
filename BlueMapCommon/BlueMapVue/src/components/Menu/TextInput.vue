<template>
  <input class="text-input" type="text" :value="value" @input="$emit('input', $event)" @keydown="$event.stopPropagation()">
</template>

<script>
export default {
  name: "TextInput",
  props: {
    value: String
  }
}
</script>

<style lang="scss">
.side-menu .text-input {
  background-color: var(--theme-bg-hover);
  width: calc(100% - 1em);
  padding: 0.5em;
}
</style>