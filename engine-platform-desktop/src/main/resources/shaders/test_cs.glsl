#version 450 core
layout(local_size_x=1) in;
layout(std430,binding=5) buffer InputBuffer{
    float data[];
};
void main(){
    uint id=gl_GlobalInvocationID.x;
    data[id] *=id;
}