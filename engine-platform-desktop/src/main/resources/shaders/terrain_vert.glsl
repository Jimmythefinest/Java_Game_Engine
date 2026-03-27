#version 450 core
layout (location = 0) in vec3 position;
layout (location = 1) in vec3 color;
layout (location = 2) in vec2 texture_coordinate;

out vec3 fragpos;
out vec3 frag_Normal;
out vec2 terrainLocalCoord;

layout(std430, binding = 0) buffer MySSBO {
    mat4 perspective;
    mat4 view;
    vec3 eyepos;
    vec3 lightpos;
};

uniform mat4 uMMatrix;

void main()
{
    gl_Position = perspective * view * uMMatrix * vec4(position, 1.0);
    fragpos = vec3(uMMatrix * vec4(position, 1.0));
    frag_Normal = vec3(mat3(uMMatrix) * color);
    terrainLocalCoord = position.xz;
}
