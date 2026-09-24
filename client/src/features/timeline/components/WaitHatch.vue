<template>
  <!-- "waiting for window": the crew is at the address, the requested window hasn't opened yet.
       1px diagonal lines from a 4×4 SVG tile (identical in every tile, so perfectly even), applied as a
       mask over a solid fill so the colour still follows the theme. Give it a whole-pixel box. -->
  <span
    class="bg-line-strong [mask-image:url(data:image/svg+xml,%3Csvg%20xmlns=%27http://www.w3.org/2000/svg%27%20width=%274%27%20height=%274%27%3E%3Cpath%20d=%27M-1%201l2-2M0%204l4-4M3%205l2-2%27%20stroke=%27%23000%27%20stroke-width=%271%27/%3E%3C/svg%3E)] [mask-size:4px_4px]"
    aria-hidden="true"
  ></span>
</template>
