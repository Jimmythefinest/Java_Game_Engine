#version 450 core
in vec3 fragPosition;
in vec3 fragNormal;
in vec2 fragTexCoord;
in vec4 fragLightSpacePos;

uniform sampler2D uTexture;
uniform vec3 properties;
uniform sampler2D uShadowMap;
uniform int uShadowEnabled;

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
    vec3 lightDirection = normalize(lightpos - fragPosition);
    vec3 viewDirection = normalize(eyepos - fragPosition);
    vec3 reflectionDirection = reflect(-lightDirection, normal);

    float diffuseStrength = max(dot(normal, lightDirection), 0.15);
    float specularStrength = pow(max(dot(viewDirection, reflectionDirection), 0.0), max(properties.x, 1.0));

    vec3 ambient = vec3(0.12) * max(properties.y, 0.2);
    vec3 diffuse = vec3(diffuseStrength);
    vec3 specular = vec3(0.22) * specularStrength;
    float shadow = calculateShadow(fragLightSpacePos, normal, lightDirection);

    vec3 litColor = baseColor.rgb * (ambient + ((1.0 - shadow) * diffuse)) + ((1.0 - shadow) * specular);
    finalColor = vec4(litColor, baseColor.a);
}
