package com.njst.gaming;

import com.njst.gaming.Scene.SceneLoader;

import java.util.ArrayList;
import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Geometries.TerrainGeometry;
import com.njst.gaming.Math.Tetrahedron;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.objects.GameObject;

public class TetraLoader implements SceneLoader {

    @Override
    public void load(Scene scene) {
        int texture1 = ShaderProgram.loadTexture("/jimmy/images (2).jpeg");
        System.out.println("tetra");
        int skybox = ShaderProgram.loadTexture("/jimmy/desertstorm.jpg");
        GameObject skyboxo = new GameObject(new SphereGeometry(1, 20, 20), skybox);
        scene.renderer.skybox = (skyboxo);
        skyboxo.ambientlight_multiplier = 5;
        skyboxo.shininess = 1;
        skyboxo.scale = new float[] { 500, 500, 500 };
        skyboxo.updateModelMatrix();
        scene.addGameObject(skyboxo);

        Tetrahedron t = new Tetrahedron();
        t.v1 = new Vector3(0, 0, 0);
        t.v2 = new Vector3(0, 1, 1);
        t.v3 = new Vector3(1, 1, 0);
        t.v4 = new Vector3(0, 1, 0);
        Tetrahedron t1 = new Tetrahedron();
        GameObject tetra = new GameObject(t, texture1);
        scene.temp=t;
        scene.addGameObject(tetra);
        GameObject terrain = new GameObject(new TerrainGeometry(128, 128), texture1);
        terrain.translate(new Vector3(-64, 0, -64));
        scene.addGameObject(terrain);
        Tetrahedron[] tera = new Tetrahedron[terrain.geometry.getIndices().length / 3];
        float[] vertices = terrain.geometry.getVertices();
        int[] indices = terrain.geometry.getIndices();
        ShaderProgram shader= new ShaderProgram(
            ShaderProgram.loadShader("/jimmy/vert11.glsl"), ShaderProgram.loadShader("/jimmy/solid_color.glsl"));
    
        
        for (int i = 0; i < 1; i++) {
            tera[i] = new Tetrahedron();
            int index_location = i * 3;
            tera[i].v4 = retrieve_Vertice(indices[index_location], vertices);
            index_location++;
            tera[i].v2 =retrieve_Vertice(indices[index_location], vertices);
            index_location++;
            tera[i].v3 =retrieve_Vertice(indices[index_location], vertices);
            tera[i].v1 = tera[i].v4.clone().add(new Vector3(0, -10f, 0));
            
            GameObject obj = new GameObject(tera[i], texture1);
            scene.addGameObject(obj);
            // System.out.println(tera[i].v1);
            // System.out.println(tera[i].v2);
            // System.out.println(tera[i].v3);
            obj.translate(new Vector3(-64,0,-64));
            obj.shaderprogram=shader;
            if(obj.shaderprogram==null){
                System.out.println("still null");
            }
            
        }
        // System.exit(1);

        GameObject reference_Cube = new GameObject(new CubeGeometry(), texture1);
        reference_Cube.translate(new Vector3(1, 0, 0));
        scene.addGameObject(reference_Cube);
        ArrayList<Tetrahedron> list = new ArrayList<>();
        list.add(t1);
        for (int i = 0; i < 1; i++) {
            Vector3 vec = list.get(list.size() - 1).Normal(list.get(list.size() - 1).v2, list.get(list.size() - 1).v4,
                    list.get(list.size() - 1).v3);
            vec.add(list.get(list.size() - 1).v1.clone().add(list.get(list.size() - 1).v2)
                    .add(list.get(list.size() - 1).v3).mul(1.0f / 3.0f));
            Tetrahedron temp = new Tetrahedron();
            temp.v1 = list.get(list.size() - 1).v4.clone();
            temp.v2 = list.get(list.size() - 1).v3.clone();
            temp.v3 = list.get(list.size() - 1).v2.clone();
            temp.v4 = new Vector3(0, i, 0);
            GameObject temp2 = new GameObject(temp, texture1);
            scene.addGameObject(temp2);

        }
        scene.animations.add(new Animation() {
            int i = 0;

            @Override
            public void animate() {
                i++;
                tetra.rotate(1, 1, 1);
                t.modelMatrix(tetra.modelMatrix);
                if (i % 60 == 0) {
                    // System.err.println(scene.renderer.camera.cameraPosition.clone());
                    // System.err.println(scene.renderer.camera.targetPosition.clone());
                }
                if (t.ray_intersects(scene.renderer.camera.cameraPosition.clone(),
                        scene.renderer.camera.targetPosition.clone().sub(scene.renderer.camera.cameraPosition))) {
                    tetra.translate(new Vector3()
                            .sub(scene.renderer.camera.targetPosition.clone().sub(scene.renderer.camera.cameraPosition))
                            .normalize().mul(0.01f));
                    // System.out.println("pulling");
                }

            }

        });
        System.out.println("tetra");

    }
    public Vector3 retrieve_Vertice(int index,float[] data){
        return new Vector3(
            data[index*3],
            data[(index*3)+1],
            data[(index*3)+2]
            );
    }

}
