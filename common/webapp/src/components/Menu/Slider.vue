<template>
<div class="slider">
  <div class="label"><slot />: <span class="value">{{formatter(value)}}</span></div>
  <label>
    <input type="range" :min="min" :max="max" :step="step" :value="value" @input="$emit('update', parseFloat($event.target.value))" @change="$emit('lazy', parseFloat($event.target.value))">
  </label>
</div>
</template>

<script>

function countDecimals(value) {
  if(Math.floor(value) === value) return 0;
  return value.toString().split(".")[1].length || 0;
}

export default {
  name: "Slider",
  props: {
    value: Number,
    min: Number,
    max: Number,
    step: Number,
    formatter: {
      type: Function,
      default: function(value) {
        return parseFloat(value).toFixed(countDecimals(this.step));
      }
    }
  }
}
</script>

<style lang="scss">
.side-menu .slider {
  line-height: 2em;
  padding: 0 0.5em;

  &:hover {
    background-color: var(--theme-bg-hover);
  }

  > .label {
    > .value {
      float: right;
    }
  }

  > label {
    > input {
      appearance: none;
      -moz-appearance: none;
      -webkit-appearance: none;
      outline: none;

      width: 100%;
      height: 1em;

      border-radius: 1em;
      //border: solid 0.125em var(--theme-fg-light);

      overflow: hidden;
      background-color: var(--theme-bg-light);

      &::-webkit-slider-thumb {
        appearance: none;
        -moz-appearance: none;
        -webkit-appearance: none;
        outline: none;

        width: 1em;
        height: 1em;

        border-radius: 1em;
        border: solid 0.125em var(--theme-bg-light);

        background-color: var(--theme-bg);

        //box-shadow: calc(-100vw - 0.375em) 0 0 100vw var(--theme-switch-button-on);
      }

      &::-moz-range-thumb {
        width: 0.75em;
        height: 0.75em;

        border-radius: 0.75em;
        border: solid 0.125em var(--theme-bg-light);

        background-color: var(--theme-bg);
      }
    }
  }
}
</style>