<template>
  <SvgButton class="menu-button" :class="{close: close, back: back}" @action="$emit('action', $event)">
    <svg viewBox="0 0 30 30">
      <g>
        <path d="M25.004,9.294c0,0.806-0.75,1.46-1.676,1.46H6.671c-0.925,0-1.674-0.654-1.674-1.46l0,0
    c0-0.807,0.749-1.461,1.674-1.461h16.657C24.254,7.833,25.004,8.487,25.004,9.294L25.004,9.294z"/>
        <path d="M25.004,15c0,0.807-0.75,1.461-1.676,1.461H6.671c-0.925,0-1.674-0.654-1.674-1.461l0,0
    c0-0.807,0.749-1.461,1.674-1.461h16.657C24.254,13.539,25.004,14.193,25.004,15L25.004,15z"/>
        <path d="M25.004,20.706c0,0.807-0.75,1.461-1.676,1.461H6.671c-0.925,0-1.674-0.654-1.674-1.461l0,0
    c0-0.807,0.749-1.461,1.674-1.461h16.657C24.254,19.245,25.004,19.899,25.004,20.706L25.004,20.706z"/>
      </g>
    </svg>
  </SvgButton>
</template>

<script>
import SvgButton from "@/components/ControlBar/SvgButton";
export default {
  name: "MenuButton",
  components: {SvgButton},
  props: {
    close: Boolean,
    back: Boolean,
  }
}
</script>

<style lang="scss">
  .menu-button {

    svg {
      g {
        transform-origin: center;
        transition: transform 0.3s;
      }
      path {
        transition: transform 0.3s, fill 0.3s;
        transform: translate(0 0) rotate(0);

        &:nth-child(1) {
          transform-origin: 15px 9px;
        }

        &:nth-child(2) {
          transform-origin: 15px 15px;
        }

        &:nth-child(3) {
          transform-origin: 15px 21px;
        }
      }
    }

    &.close {
      svg {
        path:nth-child(1) {
          transform: translate(0, 5.75px) rotate(45deg);
        }
        path:nth-child(2) {
          transform: translate(-100%, 0) rotate(0);
        }
        path:nth-child(3) {
          transform: translate(0, -5.75px) rotate(-45deg);
        }
      }

      &.back {
        svg {
          g {
            transform: scale(0.75);
          }
          path:nth-child(1) {
            transform: translate(0, 10px) rotate(30deg);
          }
          path:nth-child(2) {
            transform: translate(-150%, 0) rotate(0);
          }
          path:nth-child(3) {
            transform: translate(0, -10px) rotate(-30deg);
          }
        }
      }
    }
  }
</style>