#version 450 core
precision highp float;

in vec3 fragpos;
in vec3 frag_Normal;
in vec2 terrainLocalCoord;
in vec4 fragLightSpacePos;

uniform sampler2D uTexture0;
uniform sampler2D uTexture1;
uniform sampler2D uTexture2;
uniform sampler2D uTexture3;
uniform sampler2D uSplatMap;
uniform vec3 eyepos1;
uniform vec3 properties;
uniform vec3 terrainBlendConfig;
uniform vec3 terrainControlOffset;
uniform sampler2D uShadowMap;
uniform int uShadowEnabled;
const int MAX_LIGHTS = 8;
uniform int uLightCount;
uniform vec3 uLightPositions[MAX_LIGHTS];
uniform vec3 uLightColors[MAX_LIGHTS];
uniform vec3 uLightProperties[MAX_LIGHTS];

layout(std430, binding = 0) buffer MySSBO {
    mat4 perspective;
    mat4 view;
    vec3 eyepos;
    vec3 lightpos;
};

out vec4 finalColor;

float calculateShadow(vec4 lightSpacePos, vec3 normal, vec3 lightDir) {
    if (uShadowEnabled == 0) {
        return 0.0;
    }
    vec3 projected = lightSpacePos.xyz / max(lightSpacePos.w, 0.0001);
    projected = projected * 0.5 + 0.5;
    if (projected.z > 1.0 || projected.x < 0.0 || projected.x > 1.0 || projected.y < 0.0 || projected.y > 1.0) {
        return 0.0;
    }
    float currentDepth = projected.z;
    float ndotl = clamp(dot(normal, lightDir), 0.0, 1.0);
    float bias = max(0.004 * (1.0 - ndotl), 0.001);
    vec2 texelSize = 1.0 / vec2(textureSize(uShadowMap, 0));
    float occlusion = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float closestDepth = texture(uShadowMap, projected.xy + (vec2(x, y) * texelSize)).r;
            occlusion += currentDepth - bias > closestDepth ? 1.0 : 0.0;
        }
    }
    return (occlusion / 9.0) * 0.45;
}

void main()
{
    vec2 detailUv = fract(fragpos.xz / terrainBlendConfig.x);
    vec2 localControlUv = clamp((terrainLocalCoord + vec2(0.5)) / (terrainBlendConfig.y + 1.0), vec2(0.0), vec2(1.0));
    vec2 controlUv = terrainControlOffset.xy + (localControlUv * terrainBlendConfig.z);
    vec4 splat = texture(uSplatMap, controlUv);
    float splatSum = splat.r + splat.g + splat.b + splat.a;
    vec4 weights = splatSum > 0.0001 ? (splat / splatSum) : vec4(0.0, 1.0, 0.0, 0.0);

    vec4 layer0 = texture(uTexture0, detailUv);
    vec4 layer1 = texture(uTexture1, detailUv);
    vec4 layer2 = texture(uTexture2, detailUv);
    vec4 layer3 = texture(uTexture3, detailUv);
    vec4 texture_color =
        (layer0 * weights.r) +
        (layer1 * weights.g) +
        (layer2 * weights.b) +
        (layer3 * weights.a);

    vec3 norm = normalize(frag_Normal);
    vec3 viewDir = normalize(eyepos - fragpos);

    vec3 ambientColor = vec3(0.1, 0.1, 0.1) * properties[1];
    vec3 ambient = ambientColor * vec3(texture_color);

    vec3 lit = vec3(0.0);
    int activeLightCount = clamp(uLightCount, 1, MAX_LIGHTS);
    for (int i = 0; i < activeLightCount; i++) {
        vec3 toLight = uLightPositions[i] - fragpos;
        float distanceToLight = length(toLight);
        vec3 lightDir = distanceToLight > 0.0001 ? toLight / distanceToLight : vec3(0.0, 1.0, 0.0);
        float range = max(uLightProperties[i].x, 0.001);
        float attenuation = clamp(1.0 - (distanceToLight / range), 0.0, 1.0);
        attenuation *= attenuation;

        float diff = max(dot(norm, lightDir), 0.0);
        vec3 diffuse = diff * vec3(texture_color) * uLightColors[i] * attenuation;

        vec3 reflection = reflect(-lightDir, norm);
        float spec = pow(max(dot(viewDir, reflection), 0.0), max(properties[0], 1.0));
        vec3 specular = spec * uLightColors[i] * attenuation / max(32.0 / max(properties[0], 1.0), 1.0);
        float shadow = uLightProperties[i].y > 0.5 ? calculateShadow(fragLightSpacePos, norm, lightDir) : 0.0;

        lit += ((1.0 - shadow) * diffuse) + ((1.0 - shadow) * specular);
    }

    vec3 result = ambient + lit;
    finalColor = vec4(result, texture_color.a);
}
