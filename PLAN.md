# PLAN.md — Block-Backed Paintings for Minecraft 26.2 Fabric

## 1. Objective

Implement a **Fabric mod for Minecraft Java 26.2** that converts vanilla `Painting` entities into block-backed decorations.

Primary goals:

- Remove paintings from the normal entity subsystem.
- Preserve vanilla appearance and placement behavior as closely as practical.
- Support vanilla and datapack-added painting variants.
- Preserve painting dimensions from **1×1 through 16×16**. Painting variants are data-driven and allow dimensions up to 16 blocks in either direction. 
- Convert existing paintings in worlds safely.
- Render paintings without relying on raw OpenGL calls.
- Avoid ticking wherever possible.
- Provide measurable server/client performance improvement in painting-heavy areas.

Target:

```text
Minecraft: 26.2
Loader: Fabric
Java: version required by MC 26.2
Mappings: whatever the project standardizes on
```

Use the current 26.2 Fabric toolchain rather than blindly pinning versions in this document. Fabric's 26.2 documentation exists specifically for this target, and Fabric API `0.156.0+26.2` was published in July 2026. 

The Fabric 26.2 migration guidance also matters because 26.2 introduced rendering-backend changes; avoid raw OpenGL and stay in Minecraft/Blaze3D/Fabric rendering APIs. 

---

# 2. Reference implementations

## Primary reference: FastItemFrames 26.2

Use:

```text
Fuzss/fast-item-frames
branch: 26.2.x
```

as the primary implementation reference.

FastItemFrames already proves the central architecture works on Minecraft 26.2:

```text
ItemFrame entity
      ↓
entity-load hook
      ↓
block + block entity
      ↓
copy entity state
      ↓
remove original entity
```

Its conversion handler does exactly this for loaded/spawned item frames. 

Important classes to study:

```text
ItemFrameBlock
ItemFrameBlockEntity
ItemFrameHandler
ItemFrameBlockRenderer
HangingEntityItemMixin
```

The block implementation already demonstrates:

```text
wall attachment
thin voxel shapes
support checking
waterlogging
projectile interaction
drops
block states
neighbor updates
block entity synchronization
```



FastItemFrames is MPL-2.0, so code adaptation is possible provided MPL requirements are respected. 

## Secondary behavioral reference: Fast Paintings

Fast Paintings demonstrates that the overall concept is viable: it replaces paintings with block equivalents, removes painting entity ticking, uses block rendering, reacts immediately to support changes, and keeps paintings pass-through. 

However:

```text
Fast Paintings license: ARR / custom
```

Therefore:

**Do not copy or port its source code.**

It may be used only as behavioral/architectural inspiration.

---

# 3. Core design

The logical model should be:

```text
Painting Item
     │
     ▼
PaintingPlacementService
     │
     ▼
PaintingBlock
     │
     ▼
PaintingBlockEntity
     │
     ├── painting variant registry key
     └── state not representable in BlockState
     │
     ▼
Painting renderer
```

The normal world should contain:

```text
0 Painting entities
```

for converted ordinary paintings.

Instead:

```text
1 logical PaintingBlockEntity per painting
```

regardless of painting dimensions.

Do **not** create one block entity per occupied painting tile.

---

# 4. Important architectural gate: multi-block footprint

Do not commit immediately to either an anchor-only or helper-block implementation.

Build and test the anchor-only prototype first.

A 4×4 painting conceptually becomes:

```text
Rendered footprint:

┌────┬────┬────┬────┐
│    │    │    │    │
├────┼────┼────┼────┤
│    │ A  │    │    │
├────┼────┼────┼────┤
│    │    │    │    │
├────┼────┼────┼────┤
│    │    │    │    │
└────┴────┴────┴────┘

A = actual block
```

The renderer may extend beyond that block.

This is preferable because it gives:

```text
4×4 painting:
1 block
1 block entity
```

instead of:

```text
16 blocks
1 block entity
```

and datapacks can define paintings as large as 16×16, making helper blocks potentially **256 block states for one painting**. 

However, anchor-only must pass the following tests:

```text
raycast outer painting area
break outer painting area
projectile hits outer area
placing block through outer area
painting overlap detection
breaking backing blocks behind outer area
explosions
chunk boundaries
```

If vanilla block mechanics cannot provide correct outer-area interaction without invasive raycast/world hooks, move to the helper-block design.

---

# 5. Fallback multi-block design

If required, implement:

```text
PaintingAnchorBlock
PaintingPartBlock
```

Example:

```text
[P][P][P][P]
[P][A][P][P]
[P][P][P][P]

A = anchor
P = lightweight part
```

Only `A` has a block entity.

`P` must have:

```text
no ticker
no block entity
no collision
no opacity
very thin or empty collision
painting interaction shape
replace/break forwarding
```

Part blocks should encode their relative anchor coordinates in block state.

Since painting dimensions are bounded at 16×16, offsets can be represented with properties roughly equivalent to:

```text
OFFSET_X: 0..15
OFFSET_Y: 0..15
FACING
```

Then:

```java
anchor = calculateAnchor(partPos, state);
```

No global map is required.

This creates many possible block states, but avoids hundreds of block entities and avoids persistent spatial-index bookkeeping.

---

# 6. Suggested package layout

```text
src/main/java/<package>/

FastPaintings.java

registry/
    ModBlocks.java
    ModBlockEntities.java

block/
    PaintingBlock.java
    PaintingPartBlock.java        // only if required

block/entity/
    PaintingBlockEntity.java

painting/
    PaintingConversionService.java
    PaintingPlacementService.java
    PaintingFootprint.java
    PaintingSupport.java
    PaintingDrops.java

client/
    FastPaintingsClient.java

client/render/
    PaintingBlockRenderer.java
    PaintingBlockRenderState.java

event/
    PaintingEntityHandler.java

mixin/
    HangingEntityItemMixin.java   // possibly
    PaintingMixin.java            // only if required
    client/
        PaintingRendererMixin.java // only if required

command/
    PaintingMigrationCommand.java

config/
    FastPaintingsConfig.java
```

Keep mixins to a minimum.

Prefer Fabric events wherever the required hook exists; Fabric explicitly recommends events as a compatibility-friendly substitute for common mixin hooks. 

---

# 7. Phase 0 — Vanilla 26.2 investigation

Before implementing anything, inspect the actual Minecraft 26.2 code for:

```text
Painting
PaintingVariant
PaintingRenderer
PaintingRenderState
HangingEntity
HangingEntityItem
Painting placement factory/helper
Painting survival/attachment code
Painting bounding box calculation
Painting drops/damage
```

Do not reproduce these behaviors from memory.

Document:

```text
how anchor position is calculated
how odd/even width is centered
how vertical offset is calculated
how each facing transforms coordinates
how support blocks are checked
how overlapping hanging entities are checked
how variants are selected during placement
how the bounding box is generated
how projectiles/explosions break paintings
what NBT is serialized
```

Create unit-test fixtures from those observations before implementing custom footprint math.

---

# 8. Phase 1 — PaintingFootprint

Implement a pure utility:

```java
PaintingFootprint
```

Responsibilities:

```text
anchor
facing
width
height
    ↓
covered painting cells
supporting wall cells
world-space bounds
render origin
```

Suggested conceptual API:

```java
PaintingFootprint.from(
    BlockPos anchor,
    Direction facing,
    int width,
    int height
);
```

Methods:

```java
List<BlockPos> occupiedCells();

List<BlockPos> supportCells();

Box/AABB bounds();

boolean contains(BlockPos pos);

boolean isSupported(Level level);
```

This is one of the most important pieces of the mod.

All of these must use the same implementation:

```text
placement
survival
breaking
projectile detection
migration
rendering
helper blocks
```

Do not duplicate footprint math across classes.

---

# 9. Phase 2 — PaintingBlock

Implement:

```java
PaintingBlock extends BaseEntityBlock
```

or the current 26.2 equivalent.

BlockState should contain only properties that benefit from being block state.

At minimum:

```text
FACING
```

Potentially:

```text
WATERLOGGED
```

depending on desired parity and placement behavior.

Do **not** put the painting variant in BlockState.

The registry is dynamic and must support datapacks.

Block behavior:

```text
no normal collision
non-opaque
does not suffocate
does not block pathing
can be broken normally
supports projectiles
has correct outline/selection shape
checks attachment validity
drops painting item
```

FastItemFrames' implementation is an excellent model for thin wall shapes, `canSurvive`, neighbor updates, waterlogging, projectiles, and breaking. 

---

# 10. Phase 3 — PaintingBlockEntity

Implement:

```java
PaintingBlockEntity
```

It should be **non-ticking** unless a hard vanilla requirement is discovered.

Stored state should initially be only:

```text
painting variant registry identifier / holder
```

For example conceptually:

```java
RegistryKey<PaintingVariant> variant;
```

Do not serialize:

```text
width
height
asset id
```

unless required for recovery.

Those are properties of the registry entry.

Derive them from the variant.

This ensures datapack-defined paintings work naturally.

The BE must implement current 26.2 equivalents of:

```text
save
load
update packet
update tag
client synchronization
```

FastItemFrames provides the current 26.2 block-entity networking pattern. 

---

# 11. Datapack painting compatibility

This requirement is mandatory.

Never implement:

```java
switch (variant) {
    case KEBAB:
    case AZTEC:
    ...
}
```

Never enumerate vanilla paintings.

Minecraft painting variants are registry/data driven. 

The implementation must work with:

```text
minecraft:kebab
minecraft:wanderer
minecraft:...
some_mod:giant_picture
server_pack:logo
```

including unusual sizes:

```text
1×16
16×1
16×16
```

The texture/render pipeline should resolve the variant's existing asset rather than introducing a duplicate painting-texture registry.

---

# 12. Phase 4 — Rendering prototype

Start with a custom:

```java
PaintingBlockRenderer
```

using the current 26.2 block-entity render-state pipeline.

Investigate whether the vanilla:

```text
PaintingRenderer
PaintingRenderState
```

can be reused in the same way FastItemFrames reuses the existing `ItemFrameRenderer`.

FastItemFrames creates vanilla `ItemFrameRenderState` data from its block entity and invokes vanilla rendering for the contained item. 

Attempt the analogous architecture:

```text
PaintingBlockEntity
       ↓
PaintingBlockRenderState
       ↓
PaintingRenderState
       ↓
vanilla painting rendering code
```

If that becomes uglier than reproducing the painting geometry, implement painting geometry directly using the supported 26.2 rendering interfaces.

Do not use raw GL calls.

---

# 13. Renderer correctness

Rendering must match vanilla for:

```text
front texture
canvas thickness
wood/back texture
edges
orientation
light
chunk lighting
all four horizontal facings
odd/even dimensions
custom dimensions
```

Test:

```text
1×1
2×1
1×2
2×2
4×3
4×4
16×16 custom variant
```

Take side-by-side screenshots of entity vs block-backed rendering.

Pixel-perfect parity is desirable before optimizing further.

---

# 14. Phase 5 — entity → block conversion

Replicate the architecture used by FastItemFrames.

Observe painting entities entering the server world.

Conceptually:

```java
if (entity instanceof Painting painting) {
    tryConvert(painting);
}
```

Use Fabric server entity-load events if sufficient.

Flow:

```text
Painting loads/spawns
      ↓
validate convertible state
      ↓
read:
    anchor
    facing
    variant
      ↓
validate block-backed placement
      ↓
create PaintingBlock
      ↓
populate PaintingBlockEntity
      ↓
remove Painting entity
```

FastItemFrames does this on entity load and deliberately avoids conversion when the target block location is unsuitable. 

Follow the same safety principle:

> **Conversion failure must leave the original painting intact.**

Never delete first and attempt block placement second.

---

# 15. Existing-world migration

Default config:

```text
convertExistingPaintings = true
```

When chunks containing vanilla paintings load:

```text
painting entity
    ↓
conversion attempt
```

No global world scan should be required.

This makes migration incremental and cheap.

Log conversion failures at debug level.

Example:

```text
Could not convert painting at 100 64 -230:
anchor block occupied
```

Do not spam production logs for normal incompatibility cases.

---

# 16. Phase 6 — direct placement

Initial MVP may allow vanilla to create a `Painting` entity and immediately convert it.

Once conversion is stable, optimize placement so the entity never enters the world.

Intercept the normal painting item placement path.

FastItemFrames has a current 26.2 `HangingEntityItem` mixin around entity creation/placement that is useful for understanding this path. 

Implement:

```text
Painting item used on wall
      ↓
determine candidate variants using vanilla algorithm
      ↓
select variant
      ↓
validate footprint
      ↓
place PaintingBlock directly
```

Do not invent a new placement algorithm.

Extract/reuse vanilla logic wherever practical.

Critical requirement:

```text
same available variants
same random selection semantics
same fitting behavior
same wall support behavior
```

---

# 17. Painting overlap handling

Because existing paintings are no longer entities, vanilla hanging-entity overlap checks will not see them.

Our placement service must explicitly reject:

```text
new painting footprint
        intersects
existing block-backed painting footprint
```

If helper blocks are used, this becomes straightforward.

If anchor-only is used, implement:

```text
PaintingSpatialLookup
```

or equivalent footprint lookup.

Do **not** scan every loaded block entity in the world for every placement.

At worst, query only relevant chunks/positions.

---

# 18. Support updates

A painting must disappear/drop when its backing wall becomes invalid.

For a 4×4 painting, all required backing cells matter.

Anchor-only means ordinary neighbor updates at the anchor are insufficient.

Therefore either:

### Anchor-only

Provide a block-change/neighbor mechanism that checks painting footprints touching the changed support cell.

or:

### Part blocks

Every painting part receives relevant neighbor updates and forwards validation to its anchor.

Prefer event-driven validation.

Do not add a periodic tick such as:

```java
every 20 ticks:
    check every painting
```

That would reintroduce precisely the kind of recurring work this mod is intended to eliminate.

Fast Paintings specifically advertises immediate support updates as one advantage of making paintings blocks. 

---

# 19. Breaking behavior

Breaking anywhere on the visible painting should destroy the logical painting.

For helper architecture:

```text
break part
    ↓
resolve anchor
    ↓
remove all parts
    ↓
remove anchor
    ↓
drop painting
```

Ensure recursive neighbor updates do not cause:

```text
16 painting item drops
```

for a 4×4 painting.

Only the anchor owns drops.

Use an internal removal guard or removal flags.

---

# 20. Drops

Match vanilla behavior for:

```text
survival break
creative break
projectile break
explosion
support removed
doEntityDrops / relevant gamerules
```

Do not assume block loot-table semantics automatically match painting entity drops.

Implement explicit parity tests.

The resulting item should remain the normal:

```text
minecraft:painting
```

unless an existing component-based way of preserving the selected variant should be retained.

---

# 21. Projectile behavior

FastItemFrames handles projectile interaction specially because a normally non-colliding wall decoration still needs a projectile hit shape. 

Use the same idea.

Requirements:

```text
player walks through painting
arrow can hit painting
trident can hit painting
other valid projectiles can destroy painting
```

Avoid giving paintings normal player collision simply to support projectiles.

---

# 22. Explosions

Explicitly test:

```text
TNT
creeper
fireball
bed explosion
respawn anchor explosion
```

Verify:

```text
correct destruction
one item drop maximum
all helper cells removed
anchor removed
no ghost painting
```

---

# 23. Chunk boundaries

This is mandatory because large paintings can cross chunk boundaries.

Test paintings where:

```text
anchor is in chunk A
visual footprint reaches chunk B
```

and four-chunk corner cases.

Required behavior:

```text
loading chunk A alone must not corrupt painting
unloading B must not delete painting
support updates in B must find anchor in A
rendering must not vanish because anchor is outside normal render section assumptions
```

This may become the main argument against an anchor-only baked model.

Treat chunk-boundary behavior as an architectural gate, not a late bug.

---

# 24. Renderer performance phase

Once the functional implementation works, profile:

```text
Painting entity renderer
vs
PaintingBlockEntity renderer
```

If the BE renderer is still a meaningful client cost, implement the second optimization stage:

```text
dynamic/baked block model rendering
```

Goal:

```text
painting geometry included in chunk/block render data
```

rather than rendered every frame as a block entity.

This is closer to Fast Paintings' stated optimization approach. 

However, do **not** delay the entire project on this.

There are two independent wins:

```text
Phase A
Entity → non-ticking BE

Phase B
BER → baked/chunk model
```

Benchmark them independently.

---

# 25. Synthetic Painting object

If vanilla rendering or some compatibility API absolutely requires a `Painting` object, follow FastItemFrames' pattern.

FastItemFrames creates an internal `ItemFrame` instance for map-related vanilla APIs without adding it to the world. 

Therefore it is acceptable to have:

```java
private Painting syntheticPainting;
```

provided that it is:

```text
never added to Level
never entity-tracked
never independently ticked
never serialized as an entity
```

But only do this when necessary.

---

# 26. Compatibility policy

Automatically convert ordinary paintings.

Potentially skip paintings with unusual entity state such as:

```text
custom entity tags
custom command tags
invulnerable
glowing
custom name
unexpected NBT
modded subclass
```

until each field has defined behavior.

Safe default:

```text
convert vanilla Painting instances
convert registry-defined variants
skip unknown subclasses
```

Do not blindly transform modded `Painting` subclasses.

---

# 27. Commands and administration

Add:

```text
/fastpaintings stats
```

Example:

```text
Block paintings: 842
Painting entities: 3
Loaded block paintings: 121
```

Add:

```text
/fastpaintings convert
```

to convert currently loaded compatible painting entities.

Also add:

```text
/fastpaintings restore
```

which converts block-backed paintings back into vanilla `Painting` entities.

This is important for uninstallability.

Flow:

```text
PaintingBlock
     ↓
construct vanilla Painting
     ↓
restore variant/facing/position
     ↓
add entity
     ↓
remove block
```

A server administrator should be able to run restore before removing the mod.

---

# 28. Configuration

Keep configuration minimal.

Suggested options:

```properties
convertExistingPaintings=true
convertOnPlacement=true
convertCommandCreatedPaintings=true

skipSpecialEntityData=true

renderMode=AUTO
```

Potential render modes later:

```text
AUTO
BLOCK_ENTITY
BAKED
```

Avoid exposing dozens of implementation details as config.

---

# 29. Automated tests

Use Fabric's game-test/test infrastructure where practical.

Create automated tests for:

### Placement

```text
1×1
2×1
1×2
2×2
4×3
4×4
16×16
```

each facing:

```text
NORTH
SOUTH
EAST
WEST
```

### Conversion

```text
spawn vanilla painting
tick
assert entity absent
assert block exists
assert BE variant matches
```

### Persistence

```text
place
save world
reload
verify variant
verify facing
verify footprint
```

### Support

```text
remove each backing block
verify painting breaks exactly once
```

### Drops

```text
survival
creative
projectile
support break
explosion
```

### Custom registry variant

Add test datapack:

```text
fastpaintings_test:giant
width=16
height=16
```

and verify placement/conversion/render state.

### Chunk boundaries

Test:

```text
X=15/16 boundary
Z=15/16 boundary
both simultaneously
```

---

# 30. Manual compatibility test matrix

Test alongside common performance/render mods likely to be present in an SMP stack:

```text
Lithium
FerriteCore
Sodium
ImmediatelyFast
ModernFix if available for 26.2
server profiling tools
```

Also test:

```text
WorldEdit
structure blocks
/fill
/clone
pistons
water
lava
doors
trapdoors
fences
glass
slabs
stairs
```

Particularly watch for commands copying only half of a helper-block painting.

---

# 31. Performance benchmark

Create a reproducible benchmark world.

Test:

```text
0 paintings
100 paintings
500 paintings
1,000 paintings
5,000 paintings
```

Measure vanilla vs modded.

Server measurements:

```text
MSPT
entity tick time
entity count
chunk tick time
memory
network entity packets
```

Client:

```text
FPS
frame time
render thread time
entity rendering time
block entity rendering time
chunk rebuild cost
```

The benchmark should include both:

```text
1×1 dense gallery
4×4 gallery
```

because helper-block architecture changes the cost profile.

---

# 32. Success criteria

The first release is complete when:

```text
[ ] Vanilla paintings placed normally become block-backed.
[ ] Existing paintings convert safely during chunk load.
[ ] No Painting entity remains for ordinary converted paintings.
[ ] Datapack painting variants work.
[ ] 1×1 through 16×16 sizes work.
[ ] Rendering closely matches vanilla.
[ ] All four horizontal facings work.
[ ] Players can walk through paintings.
[ ] Projectiles can break paintings.
[ ] Removing backing blocks breaks paintings.
[ ] Drops occur exactly once.
[ ] Save/reload works.
[ ] Chunk-boundary paintings work.
[ ] Overlapping paintings cannot be created.
[ ] Conversion failure never destroys the source entity.
[ ] Restore command can turn blocks back into vanilla entities.
[ ] No painting block entity ticks continuously.
[ ] Performance benchmark demonstrates a measurable benefit.
```

---

# 33. Explicit non-goals for v1

Do not scope-creep into:

```text
item frames
glow item frames
custom painting GUI
painting resizing
painting rotation
custom URLs/images
animated paintings
arbitrary custom models
server-only compatibility
```

The first release should do one thing:

> **Replace vanilla-style Painting entities with efficient block-backed paintings while keeping vanilla behavior.**

---

# 34. Recommended implementation order

```text
1. Bootstrap Fabric 26.2 project
          ↓
2. Study vanilla Painting 26.2
          ↓
3. Implement PaintingFootprint
          ↓
4. Add exhaustive footprint tests
          ↓
5. PaintingBlock + PaintingBlockEntity
          ↓
6. 1×1 rendering
          ↓
7. Arbitrary-size rendering
          ↓
8. Manual placement command
          ↓
9. Entity → block converter
          ↓
10. Existing-world migration
          ↓
11. Vanilla painting item placement
          ↓
12. Support validation
          ↓
13. Projectile/explosion behavior
          ↓
14. Multi-block interaction prototype
          ↓
15. Decide anchor-only vs part blocks
          ↓
16. Chunk-boundary handling
          ↓
17. Restore-to-entity command
          ↓
18. Automated parity tests
          ↓
19. Performance profiling
          ↓
20. Optional baked-model renderer
```

## Architectural rule for the agent

Do **not** implement the entire mod around `PaintingPartBlock` before testing whether it is necessary.

Do **not** implement a complicated global spatial index before testing whether normal block mechanics are sufficient.

The preferred progression is:

```text
ONE ANCHOR BLOCK
       ↓
test actual Minecraft behavior
       ↓
works?
 ┌─────┴─────┐
YES           NO
 │             │
keep it       helper parts
```

And regardless of which wins:

```text
one painting
=
one logical block entity
=
zero world Painting entities
```

That gives us the cleanest shot at a genuinely useful **Fast Paintings for 26.2**, while using FastItemFrames' current 26.2 implementation as the safe modern reference instead of trying to blindly port the old ARR Fast Paintings code.