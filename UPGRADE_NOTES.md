# Upgrade notes

## Visual/world pass 1

- Larger playable boundary: 104 -> 148 world units.
- Terrain and camera draw distances extended.
- Added a suburban perimeter, forest belt, rubble scatter, wrecked vehicles, road dividers, and roadside props.
- Added tactical gear layers to the human mesh and additional anatomy/clothing forms to the zombie mesh.
- Updated KSP to 2.3.6.

All new environment meshes use existing `MaterialKind` and `MeshShape` values, so no new texture/material switch branches were required. New debris props are non-collidable to avoid trapping the player.

## Known limitations

- Character models remain procedural meshes, not imported photorealistic rigged models.
- No external Mixamo animations or GLB assets are included in the source archive.
- The map is expanded but is still a single continuous procedural level, not a seamless streaming open world.
