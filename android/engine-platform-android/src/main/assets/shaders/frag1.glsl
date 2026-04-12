#version 450 core
precision highp float;

out vec4 finalColor; // Output color of the fragment

void main() {
    finalColor = vec4(1,1,0,1);//texture(uTexture,vec2(fragColor.xy)); // Set the fragment color with full opacity
}