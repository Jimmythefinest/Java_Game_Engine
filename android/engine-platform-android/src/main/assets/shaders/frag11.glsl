#version 450 core
precision highp float;

in vec3 fragColor; // Input color from vertex shader (used for texture coordinates)
in vec3 fragpos;   // Fragment position in world space
in vec3 frag_Normal; // Normal vector at the fragment
in vec2 tt_coord; 
in vec4 fragLightSpacePos;


uniform sampler2D uTexture; // Texture sampler
uniform vec3 eyepos1; // Position of the camera
uniform vec3 properties;
uniform sampler2D uShadowMap;
uniform int uShadowEnabled;

layout(std430, binding = 0) buffer MySSBO {
    mat4 perspective; // Shader Storage Buffer Object (SSBO)
    mat4 view;
    vec3 eyepos;
    vec3 lightpos;

};
out vec4 finalColor;

float calculateShadow(vec4 lightSpacePos, vec3 normal, vec3 lightDir) {
    if (uShadowEnabled == 0) {
        return 0.0;
    }
    vec3 projected = lightSpacePos.xyz / max(lightSpacePos.w, 0.0001);
    projected = projected * 0.5 + 0.5;
    if (projected.z > 1.0 || projected.x < 0.0 || projected.x > 1.0 || projected.y < 0.0 || projected.y > 1.0) {
        return 0.0;
    }
    float currentDepth = projected.z;
    float ndotl = clamp(dot(normal, lightDir), 0.0, 1.0);
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

void main()
{
    vec4 texture_color = texture(uTexture, tt_coord.xy); // Sample the texture using fragColor as UV coordinates

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
    float shadow = calculateShadow(fragLightSpacePos, norm, lightDir);
    vec3 result = (ambient + ((1.0 - shadow) * diffuse) + ((1.0 - shadow) * specular)); // Final color result
   // result=(result-(result*pow(10.0,fog_factor)*length(fragpos-eyepos)))+fog;
    finalColor = vec4( result,texture_color.a); // Set the final color output

    //finalColor = vec4(texture(uTexture,tt_coord.xy));

}
