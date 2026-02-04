package com.njst.gaming.SceneLoaders;

import com.njst.gaming.ai.NeuralNetwork;
import com.njst.gaming.Bone;
import com.njst.gaming.Scene;
import com.njst.gaming.Scene.SceneLoader;
import com.njst.gaming.Animations.*;
import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Loaders.FBXAnimationLoader;
import com.njst.gaming.Loaders.FBXBoneLoader;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.Utils.SkeletonSSBOManager;
import com.njst.gaming.objects.Bone_object;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.objects.Weighted_GameObject;

import java.util.*;

import org.lwjgl.glfw.GLFW;

public class CharacterAnimationTest implements SceneLoader {

    private Scene scene;
    private Skeleton skeleton;
    private SkeletonSSBOManager ssboManager = new SkeletonSSBOManager();

    private AnimationGroup run_animation = new AnimationGroup();
    private ArrayList<Animation> anims = new ArrayList<>();
     int skybox;
    @Override
    public void load(Scene scene) {
        this.scene = scene;

        // Load and setup skybox
         skybox = ShaderProgram.loadTexture("/jimmy/desertstorm.jpg");
        GameObject skyboxObj = new GameObject(new SphereGeometry(1, 20, 20), skybox);
        skyboxObj.ambientlight_multiplier = 5;
        skyboxObj.shininess = 1;
        skyboxObj.scale = new float[]{500, 500, 500};
        skyboxObj.updateModelMatrix();
        scene.renderer.skybox = skyboxObj;
        scene.addGameObject(skyboxObj);

        // Load animation and skeleton data
        Map<String, KeyframeAnimation> fbxanims = FBXAnimationLoader.extractAnimation("/jimmy/dance.fbx", 0, 100);
        Bone root_bone = FBXBoneLoader.loadBones("/jimmy/Defeated.fbx", fbxanims, 100);
        skeleton = new Skeleton(root_bone);
        ssboManager.addSkeleton(skeleton);  // Register skeleton for SSBO

        // Load model and textures
        int[] fbx_textures = FBXBoneLoader.load_Textures("/jimmy/P.fbx");
        for(int i=0;i<2;i++){
        Weighted_GameObject character = new Weighted_GameObject(
            FBXBoneLoader.loadModel("/jimmy/Defeated.fbx", skeleton.getBoneList(), i, 1.0f),
            fbx_textures[0]
        );
        scene.addGameObject(character);
        }
        // Setup bone SSBO and animation update logic
        setupAnimations(fbxanims);
       
        Animation updateBones = new Animation() {
            @Override
            public void animate() {
                ssboManager.updateAll();
                ssboManager.bindToShader(2); // Binding point for GPU bone access
            }
        };
        anims.add(updateBones);

        // Add all animations to scene
        for (Animation anim : anims) {
            scene.animations.add(anim);
        }
        scene.animations.add(run_animation);
         Bone_object bone_visualiser=new Bone_object(new CubeGeometry(), skybox);
        bone_visualiser.bone=skeleton.getRootBone();
        scene.addGameObject(bone_visualiser);

        // Keyboard actions
        scene.actions.put(GLFW.GLFW_KEY_R, () -> {
            System.out.println("Stopping animation");
            run_animation.stop(false);
        });

        scene.actions.put(GLFW.GLFW_KEY_T, () -> {
            System.out.println("Starting animation");
            run_animation.start();
        });
    }
    NeuralNetwork brain;

    private void setupAnimations(Map<String, KeyframeAnimation> fbxanims) {
        Bone root=new Bone();
        Bone child=new Bone();
        child.translate(new Vector3(0,1,0));
        Bone child2=new Bone();
        child2.translate(new Vector3(0,1,0));
        root.Children.add(child);
        // child.Children.add(child2);
        root.translate(new Vector3(3,0,0));
        Bone_object Skeleton_Viewer=new Bone_object(new CubeGeometry(),skybox);
        Skeleton_Viewer.bone=root;
        scene.addGameObject(Skeleton_Viewer);
        Skeleton skeleton=new Skeleton(root);
        final int[] datat=new int[4];
        int bone_number=skeleton.getBoneList().size();
        brain=new NeuralNetwork(new int[]{bone_number*2*3,bone_number*20*3,bone_number*3}, 0.01f, true);
        NeuralNetwork height_predicter=new NeuralNetwork(new int[]{bone_number*2*3,bone_number*20*3,1}, 0.01f, true);
        final int[] scores=new int[2];
        Animation thinker=new Animation(){
            float last_height;
            Bone head=child;
            @Override
            public void animate(){
                scores[1]++;

                float[] input=new float[bone_number*2*3];
                int i=0;
                
                for(Bone bone:skeleton.getBoneList()){
                    if(head==null){
                        // if(bone.name.equals("mixamorig:Head"))head=bone;
                        // System.err.println(bone.name);
                    }
                    Vector3 rotation=bone.rotation.clone();
                    if( rotation.y>360){
                        rotation.y=rotation.y%360;
                    }
                    if(rotation.z>360){
                        rotation.z=rotation.z%360;
                    }
                    if(rotation.x>360){
                        rotation.x=rotation.x%360;
                    }
                    System.arraycopy(rotation.mul(1/360f).toArray(),0,input,i*3,3);
                    i++;
                }
                float[] output=brain.feedForward(input);
                float[] height_array=new float[bone_number*2*3];
                System.arraycopy(input,0,height_array,0,bone_number*3);
                System.arraycopy(output,0,height_array,bone_number*3,bone_number*3);
                float prediction=height_predicter.feedForward(height_array)[0];
                 
                i=0;
                for(Bone bone:skeleton.getBoneList()){
                    //System.arraycopy(bone.rotation.toArray(),0,input,i*3,3);
                    // if(Math.random()<0.1)output[i*3+(int)(2*Math.random())]+=(float)(Math.random());
                    Vector3 change=new Vector3();
                    change.x+=output[i*3];
                    change.y+=output[i*3+1];
                    // change.z+=output[i*3+2];

                    change.mul(0.1f);
                    bone.rotation.add(change);
                    i++;
                }
                skeleton.getRootBone().update();

                // head.get_globalposition
                if(head.get_globalposition().y<last_height){
                    if(prediction>0.5){
                        scores[0]++;
                        height_predicter.train(height_array, new float[]{prediction});
                    }
                    
                    brain.train(input, output);
                    datat[0]++;
                }else{
                    for(float o:output){
                        o=0-o;

                    }
                     if(prediction<0.5){
                        scores[0]++;
                        height_predicter.train(height_array, new float[]{prediction});
                    }
                    brain.train(input, output);

                }
                last_height=head.get_globalposition().y;

            }

        };
        scene.actions.put(GLFW.GLFW_KEY_0, new Runnable() {
            public void run(){
                // float[] input=new float[bone_number*2*3];
                // System.out.println(Arrays.toString(brain.feedForward(input)));
                // int i=0;
                // for(Bone bone:skeleton.getBoneList()){
                    
                //     System.arraycopy(bone.rotation.toArray(),0,input,i*3,3);
                //     i++;
                // }
                // System.out.println(Arrays.toString((input)));
                // System.out.println(Arrays.toString(brain.feedForward(input)));
                System.out.println("Score:"+(scores[0]/scores[1]));
                // scores[0]=0;
                // scores[1]=0;

            }
        });
        // anims.add(thinker);
        Thread thread=new Thread(){
            public void run(){
                try{
                long last_chect=System.currentTimeMillis();
                int runs=0;
                int frames=0;
                while(true){
                    thinker.animate();
                    runs++;
                    if(System.currentTimeMillis()-last_chect>1000){
                        System.out.println("runs per second"+runs);
                        System.out.println("trains per second"+datat[0]);
                        System.out.println("% right:"+(datat[0]/(double)runs));
                        runs=0;
                        datat[0]=0;
                        last_chect=System.currentTimeMillis();
                    }
                    if(frames>1000){
                    sleep(100);

                    }
                    frames++;


                }
            }catch(Exception e){
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        fbxanims.forEach((na, anim) -> {
            if (anim.bone != null) {
                anim.onfinish = () -> anim.time = 0; // Loop animation
                //run_animation.add_animation(anim);
                // scene.KEY_ANIMATIONS.add(anim);
                // Optionally, anim.start(); if you want them to begin automatically
            }
        });
    }
}
