<template>
  <span class="wait-hatch" aria-hidden="true">
    <!-- "waiting for window": the crew is at the address, the requested window hasn't opened yet -->
  </span>
</template>

<style scoped>
/* 1px diagonal lines from a 4×4 SVG tile (identical in every tile, so perfectly even), applied as a
   mask over a solid fill so the colour still follows the theme. Give it a whole-pixel box. */
.wait-hatch {
  --hatch-tile: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='4' height='4'%3E%3Cpath d='M-1 1l2-2M0 4l4-4M3 5l2-2' stroke='%23000' stroke-width='1'/%3E%3C/svg%3E");
  background-color: var(--line-strong);
  -webkit-mask-image: var(--hatch-tile);
  mask-image: var(--hatch-tile);
  -webkit-mask-size: 4px 4px;
  mask-size: 4px 4px;
}
</style>
