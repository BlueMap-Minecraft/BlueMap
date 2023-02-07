<template>
  <div class="marker-item" :class="{'marker-hidden': !marker.visible}">
    <div class="marker-button" :title="marker.id" @click="click(false)">
      <div class="icon" v-if="marker.type === 'player'">
        <img :src="'maps/' + mapId +  '/assets/playerheads/' + marker.playerUuid + '.png'" alt="playerhead" @error="steve">
      </div>
      <div class="info">
        <div class="label">{{markerLabel}}</div>
        <div class="stats">
          <div v-if="appState.debug">
            {{marker.type}}-marker
          </div>
          <div>
            ({{ position(marker.position.x) }} | {{ position(marker.position.y) }} | {{ position(marker.position.z) }})
          </div>
        </div>
      </div>
    </div>
    <div class="follow-player-button" :class="{active: controls.controls.followingPlayer && controls.controls.followingPlayer.id === marker.id}"
         v-if="marker.type === 'player'" @click="click(true)" :title="$t('markers.followPlayerTitle')">
      <svg viewBox="0 0 30 30">
        <circle fill="none" stroke-width="3" stroke-miterlimit="10" cx="15" cy="15" r="10.375"/>
        <line fill="none" stroke-width="3" stroke-miterlimit="10" x1="3.25" y1="15" x2="1.063" y2="15"/>
        <line fill="none" stroke-width="3" stroke-miterlimit="10" x1="15" y1="26.75" x2="15" y2="28.938"/>
        <line fill="none" stroke-width="3" stroke-miterlimit="10" x1="26.75" y1="15" x2="28.938" y2="15"/>
        <line fill="none" stroke-width="3" stroke-miterlimit="10" x1="15" y1="3.25" x2="15" y2="1.063"/>
        <circle stroke="none" cx="15" cy="15" r="6.042"/>
      </svg>
    </div>
  </div>
</template>

<script>
export default {
  name: "MarkerItem",
  props: {
    marker: Object,
  },
  data() {
    return {
      appState: this.$bluemap.appState,
      controls: this.$bluemap.mapViewer.controlsManager.data,
      mapId: this.$bluemap.mapViewer.data.map.id
    }
  },
  computed: {
    markerLabel() {
      switch (this.marker.type) {
        case "player" : return this.marker.name;
        default : break;
      }

      if (this.marker.label){
        let strippedLabel = /^(?:<[^>]*>\s*)*([^<>]*\S[^<>]*)(?:<|$)/gi.exec(this.marker.label);
        if (strippedLabel && strippedLabel.length > 1) {
          return strippedLabel[1];
        }
      }

      return this.marker.id;
    },
    position() {
      return (v) => {
        return Math.floor(v);
      }
    }
  },
  methods: {
    async click(follow) {
      let cm = this.$bluemap.mapViewer.controlsManager;
      
      if (cm.controls && cm.controls.stopFollowingPlayerMarker) {
        cm.controls.stopFollowingPlayerMarker();
      }

      if (this.marker.type === "player") {

        if (this.marker.foreign) {
          let matchingMap = await this.$bluemap.findPlayerMap(this.marker.playerUuid);
          if (!matchingMap) return;

          //this.$bluemap.appState.menu.closeAll();
          await this.$bluemap.switchMap(matchingMap.data.id);
        }

        if (follow && cm.controls && cm.controls.followPlayerMarker && this.marker.visible) {
          cm.controls.followPlayerMarker(this.marker);
        }

      } else if (!this.marker.visible) {
        return;
      }

      cm.position.copy(this.marker.position);
    },
    steve(event) {
      event.target.src = "assets/steve.png";
    }
  }
}
</script>

<style lang="scss">
@import "/src/scss/variables.scss";

.side-menu .marker-item {
  display: flex;
  white-space: nowrap;
  user-select: none;

  line-height: 1em;

  margin: 0.5em 0;

  &:first-child {
    margin-top: 0;
  }

  &:last-child {
    margin-bottom: 0;
  }

  &.marker-hidden {
    opacity: 0.5;
    filter: grayscale(1);
  }

  .marker-button {
    display: flex;
    flex-grow: 1;
    cursor: pointer;

    &:hover {
      background-color: var(--theme-bg-hover);
    }

    > .info {
      flex-grow: 1;
      text-overflow: ellipsis;

      padding: 0.5em;

      .label {
        text-overflow: ellipsis;
      }

      .stats {
        display: flex;
        font-size: 0.8em;
        color: var(--theme-fg-light);

        > div {
          &:not(:first-child) {
            margin-left: 0.5em;
            padding-left: 0.5em;
            border-left: solid 1px var(--theme-bg-hover);
          }
        }
      }
    }

    > .icon {
      height: 2.5em;
      margin: 0.5em;
      flex-shrink: 0;

      img {
        image-rendering: pixelated;
        height: 100%;
      }
    }
  }

  > .follow-player-button {
    width: 2em;
    cursor: pointer;
    background-color: var(--theme-bg);

    &:hover, &.active {
      background-color: var(--theme-bg-light);
    }

    > svg {
      position: relative;
      fill: var(--theme-fg-light);
      stroke: var(--theme-fg-light);

      top: 50%;
      transform: translate(0, -50%) scale(0.75);
    }

    &:active {
      background-color: var(--theme-fg-light);
      color: var(--theme-bg);

      > svg {
        fill: var(--theme-bg-light);
        stroke: var(--theme-bg-light);
      }
    }
  }
}
</style>