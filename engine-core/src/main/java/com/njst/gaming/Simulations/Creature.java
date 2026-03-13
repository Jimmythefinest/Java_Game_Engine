package com.njst.gaming.Simulations;

import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.ai.NeuralNetwork;

public class Creature {
    public Matrix4 render_data;
    public NeuralNetwork brain;
    public Vector3 position;
    public Creature(){
        position=new Vector3(
            (float)Math.random()*10,
            3,
            (float)Math.random()*10
        );
        brain=new NeuralNetwork(new int[]{3,10,3}, 0, true);
        render_data=new Matrix4().identity().translate(position);
    }
    public void Update(){
        float[] output=brain.feedForward(position.toArray());
        position.add(new Vector3(
            output).mul(0.01f));
        render_data.identity().translate(position);
    }
}
