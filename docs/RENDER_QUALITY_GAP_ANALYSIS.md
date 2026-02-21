# Render Quality Gap Analysis

This note answers: **"What is missing to render cinematic landscapes like the references?"**

## 1) What the engine currently does

From the current `Renderer` and default shaders:
- One forward pass over scene objects.
- Basic texture + ambient/diffuse/specular lighting.
- Single light vector in SSBO.
- No post-processing chain (HDR, bloom, tone mapping, color grading).
- No active shadow map pass in the default draw loop.

That is enough for functional real-time 3D, but not enough for high-end landscape shots.

## 2) Biggest quality blockers right now

## A. Material/lighting model is too simple
Current fragment shading is Blinn/Phong-style and misses physically-based behavior.

**Needed:**
- PBR workflow (metal/roughness).
- Image-based lighting (irradiance + prefiltered environment map + BRDF LUT).
- Energy-conserving diffuse/specular response.

## B. Normal handling appears incorrect
In the current vertex shader, `frag_Normal` is built from vertex color instead of a true normal attribute.

**Impact:**
- Lighting can look stylized/wrong even with good textures.

**Needed:**
- Vertex normals/tangents in geometry.
- Proper normal transform via inverse-transpose matrix.
- Optional normal map support in fragment shading.

## C. No modern shadow pipeline
Cinematic mountain scenes depend heavily on believable shadows.

**Needed:**
- Directional-light shadow mapping.
- Cascaded Shadow Maps (CSM) for large outdoor scenes.
- PCF/PCSS filtering to soften aliasing.

## D. Missing atmosphere and volumetrics
The reference images rely on aerial perspective and light shafts.

**Needed:**
- Height fog + distance fog.
- Atmospheric scattering (approximate is fine to start).
- Optional volumetric lighting for sun shafts.

## E. Missing reflection/water pipeline
Lake reflections in the second reference require more than a static textured plane.

**Needed:**
- Reflection/refraction pass for water.
- Fresnel + roughness-based reflection.
- Optional SSR (screen-space reflections) as an intermediate step.

## F. No HDR post process stack
A lot of the "cinematic" feel is post processing, not just geometry.

**Needed:**
- Render to HDR framebuffer.
- Tone mapping (ACES/Reinhard).
- Bloom + exposure control.
- Color grading (LUT), vignette, and subtle chromatic aberration (optional).

## G. Environment + asset fidelity
Even a strong renderer looks flat with weak content.

**Needed:**
- High-resolution terrain data/height maps.
- Good albedo/normal/roughness maps.
- Sky model (procedural sky or HDRI).
- Better composition and lighting direction in scenes.

## 3) Suggested build order (highest value first)

1. **Fix normals + add gamma-correct pipeline** (quick win).
2. **HDR framebuffer + tone mapping + bloom**.
3. **Directional shadows with CSM**.
4. **PBR material model + IBL**.
5. **Atmospheric fog/scattering**.
6. **Water reflections/refractions**.
7. **Polish pass** (color grading, TAA, SSAO/SSGI).

## 4) Practical "done" targets

You are close to reference quality when you can check these boxes:
- Terrain is lit with PBR and still looks plausible under multiple sun angles.
- Distant mountains fade naturally from atmospheric perspective.
- Sun at horizon produces controlled bloom and exposure adaptation.
- Water shows stable reflections with Fresnel response.
- Shadows remain stable and detailed from foreground to far mountains.

## 5) Minimal next sprint recommendation

If you only do one sprint, prioritize:
- Correct normals/tangents and normal maps.
- Add HDR render target + ACES tone mapping + bloom.
- Implement directional shadow mapping (1-2 cascades first).

That alone will produce a major visual jump toward the reference images.
