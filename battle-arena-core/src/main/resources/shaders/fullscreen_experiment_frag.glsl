#version 330 core

in vec2 vUv;

uniform vec3 uResolution;
uniform vec3 uTimeMouse;

out vec4 finalColor;

// =====================================================
// HASH / RANDOM
// =====================================================
float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

float hash2(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

// =====================================================
// SOFT PARTICLE
// =====================================================
float particle(vec2 uv, vec2 pos, float size) {
    float d = length(uv - pos);
    return smoothstep(size, size * 0.15, d);
}

// =====================================================
// FLAME COLOR
// =====================================================
vec3 flamePalette(float t) {
    vec3 deepRed   = vec3(0.25, 0.01, 0.0);
    vec3 red       = vec3(0.85, 0.08, 0.0);
    vec3 orange    = vec3(1.0, 0.45, 0.0);
    vec3 yellow    = vec3(1.0, 0.82, 0.18);
    vec3 whiteHot  = vec3(1.0);

    vec3 col = mix(deepRed, red, smoothstep(0.0, 0.2, t));
    col = mix(col, orange, smoothstep(0.2, 0.5, t));
    col = mix(col, yellow, smoothstep(0.5, 0.8, t));
    col = mix(col, whiteHot, smoothstep(0.8, 1.0, t));

    return col;
}

// =====================================================
// 2D ROTATION
// =====================================================
mat2 rot(float a) {
    float s = sin(a);
    float c = cos(a);
    return mat2(c, -s, s, c);
}

// =====================================================
// MAIN
// =====================================================
void main() {
    float time = uTimeMouse.x;

    vec2 uv = (vUv - 0.5) * 2.0;
    uv.x *= uResolution.x / max(uResolution.y, 1.0);

    vec3 color = vec3(0.0);

    // =================================================
    // CAMERA TILT FOR DEPTH FEEL
    // =================================================
    uv *= rot(sin(time * 0.25) * 0.04);

    // =================================================
    // VOLUMETRIC LAYERS
    // Multiple depth slices
    // =================================================
    const int DEPTH_LAYERS = 4;
    const int PARTICLE_COUNT = 36;

    for (int layer = 0; layer < DEPTH_LAYERS; layer++) {
        float z = float(layer) / float(DEPTH_LAYERS - 1);

        // Front layers larger, back layers smaller
        float depthScale = mix(1.35, 0.55, z);

        // Slight vertical offset
        float depthLift = z * 0.18;

        // Darker in back
        float brightnessScale = mix(1.25, 0.35, z);

        // Simulated perspective narrowing
        vec2 layerUV = uv;
        layerUV.x *= mix(1.0, 0.65, z);

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float fi = float(i);
            float id = fi * 17.31 + float(layer) * 91.7;

            // Looping lifetime
            float speed = 0.45 + hash(id) * 0.9;
            float life = fract(time * speed + hash(id * 2.7));

            // Spiral / turbulent upward drift
            float angle =
                hash(id * 5.1) * 6.2831 +
                time * (0.4 + hash(id));

            // Base spread expands upward
            float spread = mix(0.03, 0.42, life);

            // Fake cylindrical volume
            float radial =
                (hash(id * 3.3) - 0.5) * 2.0;

            float x =
                radial * spread +
                sin(angle) * 0.08 * life;

            float y =
                -1.05 +
                life * 2.25 +
                depthLift;

            // Torch widening
            x *= (0.25 + life * 1.55);

            vec2 pos = vec2(x, y);

            // Perspective scaling
            float size =
                mix(0.22, 0.035, life) *
                depthScale *
                (0.75 + hash(id * 8.1));

            // Particle body
            float p = particle(layerUV, pos, size);

            // Depth fade
            p *= brightnessScale;

            // Fade near top
            p *= (1.0 - life);

            // Inner shading for sphere-like blobs
            float centerGlow =
                smoothstep(size * 0.9, 0.0, length(layerUV - pos));

            // Heat
            float heat =
                (1.0 - life) * 0.9 +
                centerGlow * 0.6;

            vec3 flameColor = flamePalette(heat);

            // Slight blue core for hotter center
            flameColor += vec3(0.15, 0.22, 0.4) * pow(centerGlow, 3.0);

            color += flameColor * p * (1.1 + centerGlow);
        }
    }

    // =================================================
    // CENTRAL VOLUME CORE
    // =================================================
    float core =
        smoothstep(
            0.7,
            0.0,
            length(vec2(uv.x * 1.65, uv.y + 0.72))
        );

    color += vec3(1.0, 0.95, 0.82) * core * 2.4;

    // =================================================
    // INNER SHADED SPHERE BASE
    // =================================================
    vec2 sphereUV = vec2(uv.x, uv.y + 0.82);

    float sphere = smoothstep(0.55, 0.0, length(sphereUV));

    // Fake normal lighting
    vec3 normal = normalize(vec3(sphereUV, sqrt(max(0.0, 1.0 - dot(sphereUV, sphereUV)))));
    vec3 lightDir = normalize(vec3(-0.4, 0.7, 0.6));

    float diffuse = max(dot(normal, lightDir), 0.0);
    float fresnel = pow(1.0 - max(normal.z, 0.0), 3.0);

    vec3 sphereColor =
        vec3(1.0, 0.35, 0.08) * diffuse +
        vec3(1.0, 0.75, 0.35) * fresnel;

    color += sphereColor * sphere * 0.9;

    // =================================================
    // ATMOSPHERIC OUTER GLOW
    // =================================================
    float glow =
        exp(-length(vec2(uv.x * 0.85, uv.y + 0.28)) * 2.1);

    color += vec3(1.0, 0.28, 0.04) * glow * 0.42;

    // =================================================
    // HEAT HAZE FEEL
    // =================================================
    float haze =
        sin(uv.y * 18.0 + time * 4.5) *
        smoothstep(-0.9, 0.8, uv.y) *
        0.015;

    color += vec3(1.0, 0.2, 0.05) * max(haze, 0.0);

    // =================================================
    // GAMMA
    // =================================================
    color = pow(color, vec3(0.82));

    float alpha = clamp(max(color.r, max(color.g, color.b)), 0.0, 1.0);

    finalColor = vec4(color, alpha);
}