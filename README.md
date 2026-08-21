# Fast Paintings (Minecraft 26.2 Fabric)

Block-backed paintings for Minecraft Java 26.2 on Fabric.

## Architecture

- Converts vanilla `Painting` entities into non-ticking block-backed decorations.
- Uses **1 logical `PaintingBlockEntity` per painting** placed at the anchor block.
- Uses lightweight, non-ticking **`PaintingPartBlock`** parts for remaining tiles ($2\times 1$ up to $16\times 16$).
  - Part blocks store `OFFSET_X: 0..15`, `OFFSET_Y: 0..15`, `FACING` directly in `BlockState`.
  - Anchor coordinate calculation is $O(1)$ arithmetic with zero spatial lookup maps or background tickers.
  - Native Minecraft block raycasts, projectile collisions, and neighbor support updates work seamlessly across all occupied tiles.
- Client rendering uses `PaintingBlockRenderer` with off-screen registration (`shouldRenderOffScreen() = true`) to ensure paintings spanning across chunk sections are never frustum-culled.
- Datapack-compatible: painting dimensions ($1\times 1$ to $16\times 16$) and variant assets are resolved dynamically from `Registries.PAINTING_VARIANT`.

## Configuration (`config/fastpaintings.json`)

- `preserveVariantOnDrop` (default: `true`): When broken/mined, drops a painting item with its variant preserved (using `minecraft:painting_variant` data component). Placing this item will restore the exact same painting instead of selecting randomly.
- `convertExistingPaintings` (default: `true`): Automatically converts vanilla painting entities in loaded chunks.
- `convertOnPlacement` (default: `true`): Directly places block-backed decorations when using a painting item on a wall.
- `skipSpecialEntityData` (default: `true`): Preserves entity paintings with custom names, invulnerability, or glowing tags.

## Commands

- `/fastpaintings stats`: Displays count of loaded vanilla painting entities.
- `/fastpaintings convert`: Manually triggers conversion of loaded vanilla painting entities to block-backed decorations.
- `/fastpaintings restore`: Restores block-backed paintings in loaded chunks back to vanilla `Painting` entities (for safe uninstallation).

## License

MPL-2.0
