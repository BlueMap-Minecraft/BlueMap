<template>
  <div class="choice-box">
    <div v-if="title" class="title">{{ title }}</div>
    <div class="choices">
      <div class="choice" v-for="choice of choices" :key="choice.id"
           :class="{selected: selection === choice.id}"
           @click="$emit('choice', choice)"
      >
        {{ choice.name }}
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "ChoiceBox",
  props: {
    title: {
      type: String,
      required: false,
      default: ''
    },
    choices: Array,
    selection: String
  }
}
</script>

<style lang="scss">
  .choice-box {
    display: flex;

    font-size: 0.8em;
    text-align: center;
    //margin-bottom: 0.5em;

    border: solid 2px var(--theme-bg-hover);
    //border-radius: 1em;
    overflow: hidden;

    .title, .choice {
      padding: 0.3em 0.5em;
    }

    .title {
      background-color: var(--theme-bg-hover);
    }

    .choices {
      display: flex;
      flex-grow: 1;

      .choice {
        flex-grow: 1;

        cursor: pointer;
        user-select: none;

        background-color: var(--theme-bg);
        &:hover {
          background-color: var(--theme-bg-hover);
        }
        &.selected {
          background-color: var(--theme-bg-light);
        }
      }
    }
  }
</style>