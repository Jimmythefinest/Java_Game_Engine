#version 450 core
precision highp float;

in vec3 fragColor; // Input color from vertex shader (used for texture coordinates)
in vec3 fragpos;   // Fragment position in world space
in vec3 frag_Normal; // Normal vector at the fragment

uniform vec3 eyepos; // Position of the camera
uniform vec3 properties; 
uniform sampler2D uTexture; // Texture sampler
out vec4 finalColor; // Output color of the fragmentt data[]; // Shader Storage Buffer Object (SSBO)


void main() {
    vec4 texture_color = texture(uTexture, fragColor.xy); // Sample the texture using fragColor as UV coordinates
    finalColor = texture_color; // Set the final color output
}