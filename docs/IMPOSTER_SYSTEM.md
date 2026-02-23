# Imposter System

This document defines the required behavior for the engine's imposter system. An imposter is a simplified representation of a complex 3D object used for distant rendering.

## Goals
- Provide stable visual results at distance with minimal GPU cost.
- Support static and animated sources (where animation is baked into the imposter when needed).
- Integrate with existing scene, renderer, and asset systems.
- Be deterministic and reproducible across machines.

## Terminology
- **Source Mesh**: The original, high-detail mesh (and materials) used to bake imposters.
- **Imposter**: The rendered representation used at runtime in place of the source mesh.
- **Atlas**: A texture containing multiple pre-rendered views of the source.
- **View Cell**: A discrete camera direction used during bake.

## Supported Imposter Types
1. **Billboard Atlas**
   - Single quad using a texture atlas of views.
   - Best for static props and foliage.
2. **Sliced Imposter (Optional Extension)**
   - Multiple layered quads to improve parallax.
   - Use for large objects with depth variation.

## Bake Pipeline
### Inputs
- Source mesh + materials.
- Bake settings:
  - `view_count_azimuth` (e.g. 16)
  - `view_count_elevation` (e.g. 8)
  - `atlas_resolution` (e.g. 2048x2048)
  - `padding_px` (e.g. 4)
  - `use_alpha_cutout` (true/false)
  - `normal_map` generation (true/false)
  - `depth_map` generation (true/false)

### Outputs
- `*_imposter_albedo.png` (RGBA)
- `*_imposter_normal.png` (RGBA, optional)
- `*_imposter_depth.png` (R16/R32, optional)
- `*_imposter.meta` (JSON or engine-native, includes view grid, bounds, scale, pivot)

### Bake Steps
1. **Normalize**: Compute source bounds and pivot; store in metadata.
2. **Capture Views**: Render the source mesh from each view cell into the atlas.
3. **Padding**: Dilate each view to avoid seams during sampling.
4. **Optional Maps**: Render normals/depth into their respective atlases.
5. **Write Meta**: Store grid layout, bounds, and scale.

### Bake Requirements
- Baking must be headless-capable (no UI required).
- Results must be deterministic given the same inputs.
- Texture output uses premultiplied alpha to avoid haloing.

## Runtime System
### LOD Selection
- If `distance_to_camera > imposter_threshold`, swap source mesh for imposter.
- Threshold should support hysteresis to prevent flicker:
  - `enter_distance`
  - `exit_distance`

### Orientation
- Imposter quad rotates to face the camera.
- Rotation uses camera position relative to object pivot.
- For spherical imposters, use both azimuth/elevation.
- For cylindrical imposters, lock rotation to Y-axis only.

### View Selection and Blending
- Determine best view cell(s) based on camera direction.
- Support nearest view cell or blend between 2-4 neighbors.
- Blending must be stable to avoid visible popping.

### Shading
- Albedo sampled from atlas.
- If normal map exists, compute lighting in view space or use baked lighting.
- Depth map (if present) enables soft depth-based fading and approximate parallax.

### Shadows
- Imposters should cast a simplified shadow:
  - Use a projected quad shadow or depth-map-based shadow (if available).
- If this is not feasible, fall back to no-cast and document it per asset.

### Culling
- Frustum culling applies to imposter bounds.
- Optional per-imposter distance fade-out for very small screen sizes.

## Asset Integration
- `ImposterComponent` references a source mesh and a baked imposter asset.
- Asset loader must validate atlas size and metadata.
- Missing imposter data should fall back to source mesh.

## Render Integration
- Imposters render in the same pass as opaque geometry unless alpha-cutout or blend is required.
- Alpha-cutout uses discard threshold from metadata or material default.
- Ensure correct depth write and sorting for alpha blended imposters.

## Debugging & Diagnostics
- Debug overlay to visualize:
  - Active imposters
  - LOD switch distance
  - View cell selection
- Option to force imposter rendering for a selected object.

## Performance Targets
- 1 draw call per imposter (billboard type).
- GPU memory per imposter atlas should be bounded by settings.
- Bake time must scale linearly with view count.

## Failure Modes
- Missing textures: render fallback magenta quad and log error.
- Incorrect metadata: disable imposter for that asset.
- Atlas resolution mismatch: log error and fallback.

## Open Questions (Needs Final Decisions)
- Default view grid (e.g. 16x8 vs 12x6).
- Normal/depth map formats and precision.
- Shadow strategy for imposters.
- Whether imposters participate in SSAO/SSR.
