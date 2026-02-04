package com.njst.gaming.objects;

import com.njst.gaming.Geometries.Geometry;
import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.GlUtils;
import com.njst.gaming.Natives.ShaderProgram;

import static com.njst.gaming.Natives.GlUtils.*;

import com.njst.gaming.Bone;

public class Bone_object extends GameObject {
    public Bone bone;
    public Bone_object(Geometry p,int r){
        super(p, r);
    }
    public void render(ShaderProgram shaderProgram,int textureHandle) {
       // shaderProgram.use();
       // bind_vertex_array(vaoIds[0]);
        shaderProgram.ActivateTexture(textureHandle, texture);
        int modelLocation = shaderProgram.getUniformLocation("uMMatrix");
        shaderProgram.setUniformMatrix4fv(modelLocation, modelMatrix);
        int PropetiesLocation = shaderProgram.getUniformLocation("properties");
        shaderProgram.setUniformVector3(PropetiesLocation, new Vector3(shininess,ambientlight_multiplier,0));
        if(texture!=0){
            shaderProgram.ActivateTexture(shaderProgram.getTextureLocation("uTexture"), texture);
        }
        bone.update();
        render_bone(bone,shaderProgram);
      //  GlUtils.DrawElements(GlUtils.GL_TRIANGLES, indice_length, GlUtils.GL_UNSIGNED_INT, 0);
       // bind_vertex_array(0);
        x=0;
        first=false;
        //animation.Animate(this);
    }
    int x=0;
    boolean first=true;
    int tabs=0;
    public void render_bone(Bone bone,ShaderProgram shaderProgram){
        x++;
        //  if(tabs>17)return;
        // if(first)System.out.println(getTabs()+bone.name+"  :"+bone.rotation.toString());
        if(!bone.name.contains("$")){
        Matrix4 modelMatrix=new Matrix4().identity();
        //bone.global_position.mul(x*0.03f);
        modelMatrix.translate(bone.get_globalposition().x, bone.get_globalposition().y, bone.get_globalposition().z); // Translate to the desired position
        modelMatrix.rotate((float) (bone.get_globalrotation().x), new Vector3(1, 0, 0)); // Rotate around the Y-axis
        modelMatrix.rotate((float) (bone.get_globalrotation().y), new Vector3(0, 1, 0)); // Rotate around the Y-axis
        modelMatrix.rotate((float) (bone.get_globalrotation().z), new Vector3(0, 0, 1)); // Rotate around the Y-axis
       
        modelMatrix.scale(bone.scale); // Scale the square (1.0 means no scaling)
        shaderProgram.setUniformMatrix4fv("uMMatrix", modelMatrix);
        
       // GlUtils.bind_array(GlUtils.GL_ELEMENT_ARRAY_BUFFER, vboIds[4]); // Bind the VBO for indices
        if(x!=0){
            GlUtils.bind_vertex_array(vaoIds[0]); // Bind the VAO
            GlUtils.DrawElements(GL_TRIANGLES, geometry.getIndices().length, GL_UNSIGNED_INT, 0);
            // Unbind the VAO
            GlUtils.bind_vertex_array(0); // Unbind the VAO
        }
    }
        tabs++;
         for (Bone i : bone.Children) {
            render_bone(i,shaderProgram);
        }
        tabs--;
    }
    
    public String getTabs(){
        String tab="    ",dd="";
        for(int i=0;i<tabs;i++){
            dd+=tab;
        }
        return dd;
    }
}
