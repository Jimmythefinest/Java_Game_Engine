package com.njst.gaming.ai;

import java.util.ArrayList;

import com.njst.gaming.Bone;
import com.njst.gaming.Animations.*;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Utils.GeneralUtil;
import com.njst.gaming.objects.GameObject;;


public class Entity extends Animation{
    public ArrayList<Bone> bones;
    public ArrayList<GameObject> skin;
    private RLNeuralNetwork neuralNetwork;

    public Entity(ArrayList<Bone> numBones) {
        bones = new ArrayList<>();
        for (int i = 0; i < numBones.size(); i++) {
            bones.add(numBones.get(i));
        
        }
        neuralNetwork = new RLNeuralNetwork(numBones.size() * 3, numBones.size() * 3, 10);
    }

    public void animate() {
        double[] state = new double[bones.size() * 3];
        for (int i = 0; i < bones.size(); i++) {
            state[i * 3] = bones.get(i).get_globalposition().x;
            state[i * 3 + 1] = bones.get(i).get_globalposition().y;
            state[i * 3 + 2] = bones.get(i).get_globalposition().z;
        }

        double[] predictedMovements = neuralNetwork.predict(state);
        
        for (int i = 0; i < bones.size(); i++) {
            bones.get(i).translate(new Vector3((float)predictedMovements[i * 3], (float)predictedMovements[i * 3 + 1],(float) predictedMovements[i * 3 + 2]).mul(0));
            bones.get(i).translate(new Vector3(0,0,1001f).normalize().mul(0.1f));
         // System.out.println(new Vector3((float)predictedMovements[i * 3], (float)predictedMovements[i * 3 + 1],(float) predictedMovements[i * 3 + 2]).toString());  
          skin.get(i).position=bones.get(i).get_globalposition();
        }

    }
    public double[] getCurrentState() {
        double[] state = new double[bones.size() * 3];
        for (int i = 0; i < bones.size(); i++) {
            state[i * 3] = bones.get(i).get_globalposition().x;
            state[i * 3 + 1] = bones.get(i).get_globalposition().y;
            state[i * 3 + 2] = bones.get(i).get_globalposition().z;
        }
        return state;
    }
    public void applyAction(double[] action) {
        for (int i = 0; i < bones.size(); i++) {
            bones.get(i).translate(
                new Vector3((float)action[i * 3], (float)action[i * 3 + 1],(float) action[i * 3 + 2]));
        }
    }
    
    
    public void trainEntity(int epochs, double learningRate) {
        for (int e = 0; e < epochs; e++) {
            double[] state = getCurrentState();
            double[] action = neuralNetwork.predict(state);
    
            // Apply movement
            applyAction(action);
    
            // Get new state and compute reward
            double[] newState = getCurrentState();
            double reward = computeReward(state, newState);
           // System.out.println(reward);
    
            // Convert reward into supervised learning labels
            double[] target = new double[action.length];
            for (int i = 0; i < target.length; i++) {
                target[i] = action[i] + reward * 10; // Small adjustment towards reward
            }
            
            Vector3 direction=new Vector3(5,5,5).sub(new Vector3((float)action[0],(float)action[1],(float)action[2]));
            target=new double[]{(double)direction.x,(double)direction.y,(double)direction.z};
            // Train network
            double[][] states = { RLNeuralNetwork.normalize(state,GeneralUtil.getMinValue(state),GeneralUtil.getMaxValue(state)) };
            double[][] targets = {  RLNeuralNetwork.normalize(target,GeneralUtil.getMinValue(target),GeneralUtil.getMaxValue(target)) };
            for (int i = 0; i < 20; i++) { // Increased training iterations
                neuralNetwork.train(states, targets, 0.01);
            }
            neuralNetwork.train(states, targets, learningRate);
        }
    }
    private double computeReward(double[] state, double[] newState) {
        double reward = 0.0;
        
        // Example: Encourage movement towards (5,5,5)
        double targetX = 5, targetY = 5, targetZ = 5;
        
        double oldDistance = Math.sqrt(Math.pow(state[0] - targetX, 2) + 
                                       Math.pow(state[1] - targetY, 2) + 
                                       Math.pow(state[2] - targetZ, 2));
    
        double newDistance = Math.sqrt(Math.pow(newState[0] - targetX, 2) + 
                                       Math.pow(newState[1] - targetY, 2) + 
                                       Math.pow(newState[2] - targetZ, 2));
    
        // Reward movement closer to target
        reward += (oldDistance - newDistance) * 10;
    
        return reward+(0.1*Math.random());
    }
    //Entity Test
    public static void main(String[] args) {
        ArrayList<Bone> bones=new ArrayList<>();
        bones.add(new Bone());
        Entity t=new Entity(bones);
        t.trainEntity(10000, 0.1);
        
        double[][] states =new double[100][3];// { RLNeuralNetwork.normalize(state,GeneralUtil.getMinValue(state),GeneralUtil.getMaxValue(state)) };
        double[][] targets =new double[100][3];// {  RLNeuralNetwork.normalize(target,GeneralUtil.getMinValue(target),GeneralUtil.getMaxValue(target)) };
        for(int i1=0;i1<100;i1++){
            double[] state=new double[3],target=new double[3];
            for(int i=0;i<3;i++){
                state[i]=Math.random();
            }
            Vector3 direction=new Vector3(5,5,5).sub(new Vector3((float)state[0],(float)state[1],(float)state[2]));
            target=new double[]{(double)direction.x,(double)direction.y,(double)direction.z};
            states[i1]=RLNeuralNetwork.normalize(state,GeneralUtil.getMinValue(state),GeneralUtil.getMaxValue(state));
            targets[i1]=RLNeuralNetwork.normalize(target,GeneralUtil.getMinValue(target),GeneralUtil.getMaxValue(target));
            
      
    }
    }
    
}

