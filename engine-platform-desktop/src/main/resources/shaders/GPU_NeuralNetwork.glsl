#version 450 core
layout(local_size_x = 1) in;

layout(std430, binding = 6) buffer Info_buffer {
    int layer_number;
    int active_layer;
    int layerSizes[4];
};

layout(std430, binding = 7) buffer weights_buffer {
    float weights[];
};

layout(std430, binding = 8) buffer biases_buffer {
    float biases[];
};

layout(std430, binding = 9) buffer output_bufer {
    float data[];
};
layout(std430, binding = 10) buffer input_buffer {
    float inputs[];
};
float sigmoid(float x) {
    return 1.0 / (1.0 + exp(-x));
}

void main() {
    int idx = int(gl_GlobalInvocationID.x);
    int weights_start=0;
    int biases_start=0;
    int data_start=0;
    for(int i=1;i<active_layer;i++){
        biases_start+=layerSizes[i];
        data_start+=layerSizes[i-1];
        weights_start+=layerSizes[i]*layerSizes[i-1];
    }
    for(int i=0;i<idx;i++){
        weights_start+=layerSizes[active_layer-1];
        biases_start+=1;
    }
    float out_put=biases[biases_start];
    for(int i=0;i<layerSizes[active_layer-1];i++){
        out_put+=weights[weights_start+i]*data[data_start+i];
    }
    
    data[idx+layerSizes[active_layer-1]+data_start] =sigmoid(out_put);// float(layer_number);
}
