#version 450 core
layout (location = 0) in vec3 position;
layout (location = 1) in vec3 color;
layout(location = 2) in vec2 texture_coordinate;    // Vertex color
layout (location = 3) in vec3 weights;
layout (location = 4) in ivec4 bones;

out vec3 fragColor; // Output color to fragment shader
out vec3 fragpos;
out vec3 frag_Normal;
out vec2 tt_coord; 


layout(std430, binding = 0) buffer MySSBO {
    mat4 perspective; // Shader Storage Buffer Object (SSBO)
    mat4 view;
    vec3 eyepos;
    vec3 lightpos;

};
layout(std430, binding = 2) buffer boneSSBO {
    mat4 bone[]; // Shader Storage Buffer Object (SSBO)
};
uniform mat4 uMMatrix;
uniform mat4 uVMatrix;
uniform mat4 uPMatrix;
void main()
{
    
    vec4 final_pos=vec4(0,0,0,0);
    vec3 normal=vec3(0,0,0);
    for(int i=0;i<4;i++){
        if(weights[i]!=0){
        final_pos=final_pos+bone[bones[i]]*vec4(position, 1.0)*weights[i];
        normal+=mat3(bone[bones.x])*color;
        }
    }
    gl_Position = perspective* view * uMMatrix * final_pos;
    fragColor = color;
    fragpos=vec3(uMMatrix*vec4(position,1.0));
    tt_coord=texture_coordinate;
    frag_Normal=vec3(mat3(uMMatrix)* normal);
}
