#version 450 core
in vec3 fragPosition;
in vec3 fragNormal;
in vec2 fragTexCoord;
in vec4 fragLightSpacePos;

uniform sampler2D uTexture;
uniform vec3 properties;
uniform sampler2D uShadowMap;
uniform int uShadowEnabled;
const int MAX_LIGHTS = 8;
uniform int uLightCount;
uniform vec3 uLightPositions[MAX_LIGHTS];
uniform vec3 uLightColors[MAX_LIGHTS];
uniform vec3 uLightProperties[MAX_LIGHTS];

layout(std430, binding = 0) buffer CameraData {
    mat4 perspective;
    mat4 view;
    vec3 eyepos;
    vec3 lightpos;
};

out vec4 finalColor;

float calculateShadow(vec4 lightSpacePos, vec3 normal, vec3 lightDirection) {
    if (uShadowEnabled == 0) {
        return 0.0;
    }
    vec3 projected = lightSpacePos.xyz / max(lightSpacePos.w, 0.0001);
    projected = projected * 0.5 + 0.5;
    if (projected.z > 1.0 || projected.x < 0.0 || projected.x > 1.0 || projected.y < 0.0 || projected.y > 1.0) {
        return 0.0;
    }
    float currentDepth = projected.z;
    float ndotl = clamp(dot(normal, lightDirection), 0.0, 1.0);
    float bias = max(0.0035 * (1.0 - ndotl), 0.00075);
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

void main() {
    vec4 baseColor = texture(uTexture, fragTexCoord);
    vec3 normal = normalize(fragNormal);
    vec3 viewDirection = normalize(eyepos - fragPosition);
    vec3 ambient = vec3(0.12) * max(properties.y, 0.2) * baseColor.rgb;
    vec3 lit = vec3(0.0);

    int activeLightCount = clamp(uLightCount, 1, MAX_LIGHTS);
    for (int i = 0; i < activeLightCount; i++) {
        vec3 toLight = uLightPositions[i] - fragPosition;
        float distanceToLight = length(toLight);
        vec3 lightDirection = distanceToLight > 0.0001 ? toLight / distanceToLight : vec3(0.0, 1.0, 0.0);
        float range = max(uLightProperties[i].x, 0.001);
        float attenuation = clamp(1.0 - (distanceToLight / range), 0.0, 1.0);
        attenuation *= attenuation;

        float diffuseStrength = max(dot(normal, lightDirection), 0.0);
        vec3 diffuse = baseColor.rgb * diffuseStrength * uLightColors[i] * attenuation;

        vec3 reflectionDirection = reflect(-lightDirection, normal);
        float specularStrength = pow(max(dot(viewDirection, reflectionDirection), 0.0), max(properties.x, 1.0));
        vec3 specular = uLightColors[i] * specularStrength * attenuation * 0.22;

        float shadow = uLightProperties[i].y > 0.5 ? calculateShadow(fragLightSpacePos, normal, lightDirection) : 0.0;
        lit += ((1.0 - shadow) * diffuse) + ((1.0 - shadow) * specular);
    }

    finalColor = vec4(ambient + lit, baseColor.a);
}
