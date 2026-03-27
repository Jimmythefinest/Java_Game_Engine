#version 450 core
precision highp float;

in vec3 fragpos;
in vec3 frag_Normal;
in vec2 terrainLocalCoord;

uniform sampler2D uTexture0;
uniform sampler2D uTexture1;
uniform sampler2D uTexture2;
uniform sampler2D uTexture3;
uniform sampler2D uSplatMap;
uniform vec3 eyepos1;
uniform vec3 properties;
uniform vec3 terrainBlendConfig;
uniform vec3 terrainControlOffset;

layout(std430, binding = 0) buffer MySSBO {
    mat4 perspective;
    mat4 view;
    vec3 eyepos;
    vec3 lightpos;
};

out vec4 finalColor;

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
    vec3 lightDir = normalize(lightpos - fragpos);
    vec3 viewDir = normalize(eyepos - fragpos);

    vec3 ambientColor = vec3(0.1, 0.1, 0.1) * properties[1];
    vec3 ambient = ambientColor * vec3(texture_color);

    float diff = max(dot(norm, lightDir), 0.1);
    vec3 diffuse = diff * vec3(texture_color);

    vec3 reflection = reflect(-lightDir, norm);
    float spec = pow(max(dot(viewDir, reflection), 0.0), properties[0]);
    vec3 lightColor = vec3(1.0, 1.0, 1.0);
    vec3 specular = spec * lightColor / (32.0 / properties[0]);

    vec3 result = ambient + diffuse + specular;
    finalColor = vec4(result, texture_color.a);
}
