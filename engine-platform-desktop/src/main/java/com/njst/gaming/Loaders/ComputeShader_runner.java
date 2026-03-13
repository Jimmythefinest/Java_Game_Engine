package com.njst.gaming.Loaders;

import java.util.Arrays;

import com.njst.gaming.ai.NeuralNetwork;
import com.njst.gaming.Scene;
import com.njst.gaming.Scene.SceneLoader;
import com.njst.gaming.data;
import com.njst.gaming.Natives.ComputeShader;
import com.njst.gaming.Utils.GeneralUtil;

public class ComputeShader_runner implements SceneLoader {

  @Override
  public void load(Scene s) {
    run_ComputeShader();

  }

  public static void run_ComputeShader() {
    // SSBO ssbo=new SSBO();
    // ssbo.setData(new float[]{4,3,3,1,9,2}, GL30.GL_STATIC_READ);
    // ssbo.bindToShader(3);
    // ComputeShader shader = new
    // ComputeShader(GeneralUtil.readFile(data.rootDirectory+"/GPU_NeuralNetwork.glsl"));
    // float[] input = new float[10];
    // System.err.println(shader.err);
    // int i = 0;
    // for(i=0;i<input.length;i++){
    // input[i]=0;
    // }

    // // // ComputeShader tester=new
    // ComputeShader(GeneralUtil.readFile(data.rootDirectory+"/GPU_NeuralNetwork.glsl"));
    // shader.bindBufferToShader(3,new float[]{3,1,3,3,3});
    // float[] biases={
    // 1,2,3,
    // 4,5,6
    // };
    // float[] weights={
    // //Layer 1
    // 1,1,1,
    // 2,2,2,
    // 3,3,3,
    // //Layer 2
    // 1,1,1,
    // 2,2,2,
    // 2,2,2,
    // };
    // shader.bindBufferToShader(8, biases);
    // shader.bindBufferToShader(7, weights);
    // // // tester.bindBufferToShader(6,new float[14]);
    // // // tester.dispatch(2, 1, 1);
    // // // float[] data=tester.getBufferData(6);
    // // // System.out.println("Buffer data"+Arrays.toString(data));

    // shader.bindBufferToShader(6, input);
    // shader.dispatch(input.length, 1, 1);
    // float[] results = shader.getBufferData(6);
    // System.out.println("Compute Results" + Arrays.toString(results));
    // // NeuralNetwork nn=new NeuralNetwork(new int[]{1,2,1}, i, false);
    // System.exit(1);

    // float[] last_output = new float[] { 0.1f, 0.2f, 0.3f, 0.3f };
    int[] layersg = { 3, 4, 5 };
    NeuralNetwork nn = new NeuralNetwork(layersg, 0.01f, true);

    String forwardShaderCode = GeneralUtil.readFile(data.rootDirectory + "/GPU_NeuralNetwork.glsl");

    ComputeShader cc = new ComputeShader(forwardShaderCode);
    int meta_data_length = 2;
    int[] layerSizes = nn.getLayerSizes();
    float[] input = new float[] { 0.1f, 0.2f, 0.3f };
    float[] data = new float[(GeneralUtil.sum(layerSizes))];
    System.arraycopy(input, 0, data, 0, input.length);
    int[] meta_data = new int[layerSizes.length + meta_data_length];
    meta_data[0] = layerSizes.length;
    meta_data[1] = 1;
    System.arraycopy(layerSizes, 0, meta_data, meta_data_length, layerSizes.length);
    cc.bindBufferToShader(6, meta_data);
    cc.bindBufferToShader(10, new float[] { 1, 2, 3 });
    cc.bindBufferToShader(7, GeneralUtil.flatten3D(nn.getWeights()));
    cc.bindBufferToShader(8, GeneralUtil.flatten2D(nn.getBiases()));
    cc.bindBufferToShader(9, data);
    // cc.buffersizes.remove(9);
    // cc.buffersizes.put(9, GeneralUtil.sum(layerSizes));

    for (int i = 1; i < layerSizes.length; i++) {
      meta_data[1] = i;
      cc.bindBufferToShader(6, meta_data);
      cc.dispatch(layerSizes[i], 1, 1);
      System.out.println("layer  " + i);
    }
    // cc.dispatch(4,1,1);00

    System.out.println(cc.err);
    float[] out = cc.getBufferData(9);
    float[] important = new float[5];
    System.arraycopy(out, out.length - 5, important, 0, 5);
    System.out.println(Arrays.toString(out));
    nn.feedForward(input);
    System.out.println(Arrays.toString(nn.feedForward(input)));
    // // Add to ComputeShader_runner
    // System.out.println("CPU weights: " + Arrays.deepToString(nn.getWeights()));
    // System.out.println("GPU weights: " +
    // Arrays.toString(GeneralUtil.flatten3D(nn.getWeights())));

    System.out.println((GeneralUtil.flatten3D(nn.getWeights())).length);

  }

}
