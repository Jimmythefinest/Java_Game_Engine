#version 450 core
in vec3 fragPosition;
in vec3 fragNormal;
in vec2 fragTexCoord;

uniform sampler2D uTexture;
uniform vec3 properties;

layout(std430, binding = 0) buffer CameraData {
    mat4 perspective;
    mat4 view;
    vec3 eyepos;
    vec3 lightpos;
};

out vec4 finalColor;

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

    vec3 litColor = baseColor.rgb * (ambient + diffuse) + specular;
    finalColor = vec4(litColor, baseColor.a);
}
