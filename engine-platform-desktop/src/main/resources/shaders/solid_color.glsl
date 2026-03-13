#version 450 core
precision highp float;

in vec3 fragColor; // Input color from vertex shader (used for texture coordinates)
in vec3 fragpos;   // Fragment position in world space
in vec3 frag_Normal; // Normal vector at the fragment
in vec2 tt_coord; 


uniform sampler2D uTexture; // Texture sampler
uniform vec3 eyepos1; // Position of the camera
uniform vec3 properties;

layout(std430, binding = 0) buffer MySSBO {
    mat4 perspective; // Shader Storage Buffer Object (SSBO)
    mat4 view;
    vec3 eyepos;
    vec3 lightpos;

};
out vec4 finalColor;
void main()
{
    vec4 texture_color =vec4(0,1,0,1); // Sample the texture using fragColor as UV coordinates

    // Normalize the normal vector
    vec3 norm = normalize(frag_Normal);
    
    // Calculate the light direction and view direction
    vec3 lightDir = normalize(lightpos - fragpos);
    vec3 viewDir = normalize(eyepos - fragpos);
    
    // Calculate ambient light
    vec3 ambientColor = vec3(0.1, 0.1, 0.1) * properties[1]; // Low ambient light
    vec3 ambient = ambientColor * vec3(texture_color); // Ambient contribution

    // Calculate diffuse light
    float diff = max(dot(norm, lightDir), 0.1); // Diffuse factor
    vec3 diffuse = diff * vec3(texture_color) * vec3(1.0); // Diffuse contribution

    // Calculate reflection vector for specular highlights
    vec3 reflection = reflect(-lightDir, norm);
    float spec = pow(max(dot(viewDir, reflection), 0.0), properties[0]); // Specular factor with shininess

    // Calculate specular light
    float fog_factor=-2.0;
    vec3 lightColor = vec3(1.0, 1.0, 1.0); // White light
    vec3 specular = spec * lightColor / (32 / properties[0]); // Specular contribution
    vec3 fog=vec3(1,1,1)*pow(10.0,fog_factor)*length(fragpos-eyepos);
    vec3 result = (ambient + diffuse + specular); // Final color result
   // result=(result-(result*pow(10.0,fog_factor)*length(fragpos-eyepos)))+fog;
    finalColor = vec4( result,texture_color.a); // Set the final color output

    //finalColor = vec4(texture(uTexture,tt_coord.xy));

}