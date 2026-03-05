#version 430 core

layout(local_size_x = 64) in;

layout(std430, binding = 0) buffer InputBuffer {
    float inputs[];
};

layout(std430, binding = 1) buffer WeightBuffer {
    float weights[];
};

layout(std430, binding = 2) buffer BiasBuffer {
    float biases[];
};

layout(std430, binding = 3) buffer OutputBuffer {
    float outputs[];
};

layout(std430, binding = 4) buffer MetaBuffer {
    int num_instances;
    int num_layers; // e.g., 3 for [input, hidden, output]
    int layer_sizes[8]; // Max 8 layers supported for now
    int weight_offsets[8];
    int bias_offsets[8];
};

float sigmoid(float x) {
    return 1.0 / (1.0 + exp(-x));
}

void main() {
    uint instance_id = gl_GlobalInvocationID.x;
    if (instance_id >= num_instances) return;

    // Work buffers (max size per layer, should be enough for most simulated agents)
    float current_activations[256];
    float next_activations[256];

    // 1. Initialize input layer
    int input_size = layer_sizes[0];
    uint input_start = instance_id * input_size;
    for (int i = 0; i < input_size; i++) {
        current_activations[i] = inputs[input_start + i];
    }

    // 2. Iterate through connections between layers
    for (int l = 0; l < num_layers - 1; l++) {
        int in_count = layer_sizes[l];
        int out_count = layer_sizes[l + 1];
        
        int w_offset = weight_offsets[l] + int(instance_id * weight_offsets[num_layers - 1]);
        int b_offset = bias_offsets[l] + int(instance_id * bias_offsets[num_layers - 1]);

        for (int i = 0; i < out_count; i++) {
            float sum = biases[b_offset + i];
            for (int j = 0; j < in_count; j++) {
                sum += weights[w_offset + i * in_count + j] * current_activations[j];
            }
            
            // Hidden layers use sigmoid, output layer uses linear.
            next_activations[i] = (l == num_layers - 2) ? sum : sigmoid(sum);
        }

        // Copy next to current for next layer iteration
        for (int i = 0; i < out_count; i++) {
            current_activations[i] = next_activations[i];
        }
    }

    // 3. Write output
    int output_size = layer_sizes[num_layers - 1];
    uint output_start = instance_id * output_size;
    for (int i = 0; i < output_size; i++) {
        outputs[output_start + i] = current_activations[i];
    }
}
