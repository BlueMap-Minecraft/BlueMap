<template>
  <div class="lighting-switch">
    <SvgButton :active="mode === 'live'" :title="$t('lighting.dayNightSwitch.live')" @action="setMode('live')">
      <svg viewBox="0 0 30 30">
        <path fill-rule="evenodd" d="M15,4 a11,11 0 1,0 0.001,0 Z M15,7.5 a7.5,7.5 0 1,1 -0.001,0 Z"/>
        <rect x="13.8" y="9" width="2.4" height="6" rx="1.2" transform="rotate(-60 15 15)"/>
        <rect x="13.8" y="9" width="2.4" height="6" rx="1.2"/>
        <circle cx="15" cy="15" r="1.5"/>
      </svg>
    </SvgButton>
    <SvgButton :active="mode === 'day'" :title="$t('lighting.dayNightSwitch.day')" @action="setMode('day')">
      <svg viewBox="0 0 30 30">
        <circle cx="15" cy="15" r="4.5"/>
        <rect x="13.5" y="1.5" width="3" height="4.5" rx="1.5"/>
        <rect x="13.5" y="1.5" width="3" height="4.5" rx="1.5" transform="rotate(45 15 15)"/>
        <rect x="13.5" y="1.5" width="3" height="4.5" rx="1.5" transform="rotate(90 15 15)"/>
        <rect x="13.5" y="1.5" width="3" height="4.5" rx="1.5" transform="rotate(135 15 15)"/>
        <rect x="13.5" y="1.5" width="3" height="4.5" rx="1.5" transform="rotate(180 15 15)"/>
        <rect x="13.5" y="1.5" width="3" height="4.5" rx="1.5" transform="rotate(225 15 15)"/>
        <rect x="13.5" y="1.5" width="3" height="4.5" rx="1.5" transform="rotate(270 15 15)"/>
        <rect x="13.5" y="1.5" width="3" height="4.5" rx="1.5" transform="rotate(315 15 15)"/>
      </svg>
    </SvgButton>
    <SvgButton :active="mode === 'night'" :title="$t('lighting.dayNightSwitch.night')" @action="setMode('night')">
      <svg viewBox="0 0 30 30">
        <path d="M17.011,19.722c-3.778-1.613-5.533-5.982-3.921-9.76c0.576-1.348,1.505-2.432,2.631-3.204
          c-3.418-0.243-6.765,1.664-8.186,4.992c-1.792,4.197,0.159,9.053,4.356,10.844c3.504,1.496,7.462,0.377,9.717-2.476
          C20.123,20.465,18.521,20.365,17.011,19.722z"/>
        <circle cx="5.123" cy="7.64" r="1.196"/>
        <circle cx="23.178" cy="5.249" r="1.195"/>
        <circle cx="20.412" cy="13.805" r="1.195"/>
        <circle cx="25.878" cy="23.654" r="1.195"/>
      </svg>
    </SvgButton>
  </div>
</template>

<script>
import {animate, EasingFunctions} from "../../js/util/Utils";
import SvgButton from "./SvgButton.vue";

const SUNLIGHT_DAY = 1;
const SUNLIGHT_NIGHT = 0.25;
const LIVE_POLL_INTERVAL = 5000;
const VALID_MODES = ['live', 'day', 'night'];

function timeToSunlight(time) {
    const t = ((time % 24000) + 24000) % 24000;
    const angle = (t - 6000) * 2 * Math.PI / 24000;
    return 0.25 + 0.75 * Math.max(0, (Math.cos(angle) + 1) / 2);
}

function initialMode() {
    const param = new URLSearchParams(window.location.search).get('lighting');
    return VALID_MODES.includes(param) ? param : 'live';
}

let animation;

export default {
    name: "DayNightSwitch",
    components: {SvgButton},
    inheritAttrs: false,
    data() {
        return {
            mapViewer: this.$bluemap.mapViewer.data,
            mode: initialMode(),
            _liveTimer: null,
        }
    },
    computed: {
        currentMap() {
            return this.mapViewer.map;
        }
    },
    watch: {
        currentMap(newMap, oldMap) {
            if (this.mode === 'live' && newMap !== oldMap) {
                this.stopLivePoll();
                this.fetchAndApplyTime();
                this._liveTimer = setInterval(() => this.fetchAndApplyTime(), LIVE_POLL_INTERVAL);
            }
        }
    },
    mounted() {
        // apply initial mode without requiring a click
        if (this.mode === 'live') {
            this.fetchAndApplyTime();
            this._liveTimer = setInterval(() => this.fetchAndApplyTime(), LIVE_POLL_INTERVAL);
        } else if (this.mode === 'day') {
            this.animateTo(SUNLIGHT_DAY, 300);
        } else if (this.mode === 'night') {
            this.animateTo(SUNLIGHT_NIGHT, 300);
        }
    },
    beforeUnmount() {
        this.stopLivePoll();
    },
    methods: {
        setMode(newMode) {
            if (this.mode === newMode) return;
            this.mode = newMode;
            this.stopLivePoll();

            if (newMode === 'day') {
                this.animateTo(SUNLIGHT_DAY, 300);
            } else if (newMode === 'night') {
                this.animateTo(SUNLIGHT_NIGHT, 300);
            } else if (newMode === 'live') {
                this.fetchAndApplyTime();
                this._liveTimer = setInterval(() => this.fetchAndApplyTime(), LIVE_POLL_INTERVAL);
            }
        },
        animateTo(target, duration) {
            const current = this.mapViewer.uniforms.sunlightStrength.value;
            if (Math.abs(current - target) < 0.005) return;
            if (animation) animation.cancel();
            const start = current;
            animation = animate(t => {
                const u = EasingFunctions.easeOutQuad(t);
                this.mapViewer.uniforms.sunlightStrength.value = start * (1 - u) + target * u;
                this.$bluemap.mapViewer.redraw();
            }, duration);
        },
        async fetchAndApplyTime() {
            const map = this.currentMap;
            if (!map) return;
            try {
                const response = await fetch(map.liveDataRoot + "/live/world.json", {cache: "no-store"});
                if (!response.ok) return;
                const data = await response.json();
                if (typeof data.timeOfDay === 'number') {
                    this.animateTo(timeToSunlight(data.timeOfDay), 2000);
                }
            } catch {
                // endpoint unavailable — stay at current lighting
            }
        },
        stopLivePoll() {
            if (this._liveTimer) {
                clearInterval(this._liveTimer);
                this._liveTimer = null;
            }
        }
    }
}
</script>

<style lang="scss">
.lighting-switch {
    display: flex;

    .svg-button {
        min-width: 1.5em;

        svg {
            fill: var(--theme-fg-light);
        }

        &.active svg {
            fill: var(--theme-fg);
        }
    }
}
</style>
