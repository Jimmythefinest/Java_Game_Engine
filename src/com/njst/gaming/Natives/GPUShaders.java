package com.njst.gaming.Natives;

/**
 * Utility class to store GLSL shader strings.
 */
public class GPUShaders {
    
    public static final String NN_COMPUTE_SHADER = 
        "#version 430 core\n" +
        "\n" +
        "layout(local_size_x = 64) in;\n" +
        "\n" +
        "layout(std430, binding = 0) buffer InputBuffer {\n" +
        "    float inputs[];\n" +
        "};\n" +
        "\n" +
        "layout(std430, binding = 1) buffer WeightBuffer {\n" +
        "    float weights[];\n" +
        "};\n" +
        "\n" +
        "layout(std430, binding = 2) buffer BiasBuffer {\n" +
        "    float biases[];\n" +
        "};\n" +
        "\n" +
        "layout(std430, binding = 3) buffer OutputBuffer {\n" +
        "    float outputs[];\n" +
        "};\n" +
        "\n" +
        "layout(std430, binding = 4) buffer MetaBuffer {\n" +
        "    int num_instances;\n" +
        "    int num_layers; // e.g., 3 for [input, hidden, output]\n" +
        "    int layer_sizes[8]; // Max 8 layers supported for now\n" +
        "    int weight_offsets[8];\n" +
        "    int bias_offsets[8];\n" +
        "};\n" +
        "\n" +
        "float sigmoid(float x) {\n" +
        "    return 1.0 / (1.0 + exp(-x));\n" +
        "}\n" +
        "\n" +
        "void main() {\n" +
        "    uint instance_id = gl_GlobalInvocationID.x;\n" +
        "    if (instance_id >= num_instances) return;\n" +
        "\n" +
        "    // Work buffers (max size per layer, should be enough for most simulated agents)\n" +
        "    float current_activations[256];\n" +
        "    float next_activations[256];\n" +
        "\n" +
        "    // 1. Initialize input layer\n" +
        "    int input_size = layer_sizes[0];\n" +
        "    uint input_start = instance_id * input_size;\n" +
        "    for (int i = 0; i < input_size; i++) {\n" +
        "        current_activations[i] = inputs[input_start + i];\n" +
        "    }\n" +
        "\n" +
        "    // 2. Iterate through connections between layers\n" +
        "    for (int l = 0; l < num_layers - 1; l++) {\n" +
        "        int in_count = layer_sizes[l];\n" +
        "        int out_count = layer_sizes[l + 1];\n" +
        "        \n" +
        "        int w_offset = weight_offsets[l] + int(instance_id * weight_offsets[num_layers - 1]);\n" +
        "        int b_offset = bias_offsets[l] + int(instance_id * bias_offsets[num_layers - 1]);\n" +
        "\n" +
        "        for (int i = 0; i < out_count; i++) {\n" +
        "            float sum = biases[b_offset + i];\n" +
        "            for (int j = 0; j < in_count; j++) {\n" +
        "                sum += weights[w_offset + i * in_count + j] * current_activations[j];\n" +
        "            }\n" +
        "            \n" +
        "            // Hidden layers use sigmoid, output layer (if l is the last connection) uses linear if required?\n" +
        "            // For now, let's stick to sigmoid for all. (Logic can be extended for flags)\n" +
        "            next_activations[i] = (l == num_layers - 2) ? sum : sigmoid(sum);\n" +
        "        }\n" +
        "\n" +
        "        // Copy next to current for next layer iteration\n" +
        "        for (int i = 0; i < out_count; i++) {\n" +
        "            current_activations[i] = next_activations[i];\n" +
        "        }\n" +
        "    }\n" +
        "\n" +
        "    // 3. Write output\n" +
        "    int output_size = layer_sizes[num_layers - 1];\n" +
        "    uint output_start = instance_id * output_size;\n" +
        "    for (int i = 0; i < output_size; i++) {\n" +
        "        outputs[output_start + i] = current_activations[i];\n" +
        "    }\n" +
        "}\n";
}
