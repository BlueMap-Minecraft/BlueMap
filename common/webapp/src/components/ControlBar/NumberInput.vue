<template>
  <div class="number-input">
    <label>
      <span class="label">{{label}}:</span>
      <input type="number"
             v-bind:value="format(value)"
             v-on:input="$emit('input', $event)"
             v-on:keydown="$event.stopPropagation()"
      >
    </label>
  </div>
</template>

<script>
export default {
  name: "NumberInput",
  props: {
    label: String,
    value: Number
  },
  computed: {
    format() {
      return (value) => {
        return Math.floor(value);
      }
    }
  }
}
</script>

<style lang="scss">
  .number-input {
    pointer-events: auto;

    background-color: var(--theme-bg);
    color: var(--theme-fg);

    min-height: 2em;

    .label {
      display: inline-block;
      width: 1em;
      padding: 0 0.5em 0 0.5em;

      color: var(--theme-fg-light);
    }

    input {
      height: 100%;
      line-height: 100%;
      width: calc(100% - 2em);

      background-color: inherit;
      color: inherit;

      // remove number spinner firefox
      -moz-appearance: textfield;

      // remove number spinner webkit
      &::-webkit-inner-spin-button,
      &::-webkit-outer-spin-button {
        -webkit-appearance: none;
        margin: 0;
      }
    }
  }
</style>