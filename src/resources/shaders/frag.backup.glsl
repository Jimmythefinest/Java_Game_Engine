#version 450 core
precision highp float;

in vec3 fragColor; // Input color from vertex shader (used for texture coordinates)
in vec3 fragpos;   // Fragment position in world space
in vec3 frag_Normal; // Normal vector at the fragment

uniform vec3 eyepos; // Position of the camera
uniform vec3 properties; 
uniform sampler2D uTexture; // Texture sampler
out vec4 finalColor; // Output color of the fragment

layout(std430, binding = 0) buffer MySSBO {
    float data[]; // Shader Storage Buffer Object (SSBO)
};

void main() {
    vec3 lightpos = vec3(0, 11, 0); // Position of the light source
    vec4 texture_color = texture(uTexture, fragColor.xy); // Sample the texture using fragColor as UV coordinates

    // Normalize the normal vector
    vec3 norm = normalize(frag_Normal);
    
    // Calculate the light direction and view direction
    vec3 lightDir = normalize(lightpos - fragpos);
    vec3 viewDir = normalize(eyepos - fragpos);
    
    // Calculate ambient light
    vec3 ambientColor = vec3(0.1, 0.1, 0.1)* properties[1]; // Low ambient light
    vec3 ambient = ambientColor * vec3(texture_color); // Ambient contribution

    // Calculate diffuse light
    float diff = max(dot(norm, lightDir), 0.0); // Diffuse factor
    vec3 diffuse = diff * vec3(texture_color) * vec3(1.0); // Diffuse contribution

    // Calculate reflection vector for specular highlights
    vec3 reflection = reflect(-lightDir, norm);
    float spec = pow(max(dot(viewDir, reflection), 0.0), properties[0]); // Specular factor with shininess

    // Calculate specular light
    vec3 lightColor = vec3(1.0, 1.0, 1.0); // White light
    vec3 specular = spec * lightColor/(32/properties[0]); // Specular contribution

    // Combine all components
    vec3 result = ambient + diffuse + specular; // Final color result
    finalColor = vec4(result, texture_color.a); // Set the final color output
}