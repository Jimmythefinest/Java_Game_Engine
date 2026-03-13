package com.njst.gaming.Physics;

import com.njst.gaming.Scene;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.objects.GameObject;
public class PhysicsEngine {
    Scene scene;
    public PhysicsEngine(Scene scene) {
        this.scene = scene;
    }
    public boolean check_collionsbound_overlap(Vector3 min1,Vector3 max1,Vector3 min2,Vector3 max2) {
        if(min1.x>max2.x){
            return false;
        }
        if(min2.x>max1.x){
            return false;
        }
        if(min1.y>max2.y){
            return false;
        }
        if(min2.y>max1.y){
            return false;
        }
        if(min1.z>max2.z){
            return false;
        }
        if(min2.z>max1.z){
            return false;
        } 
        return true;
    }
    public void simulate(float deltatime){
      //  System.err.println("Simulating");
        for (GameObject gameObject : scene.objects) {
            Vector3 displacement=new Vector3(gameObject.velocity);
            displacement.mul(deltatime);
            gameObject.translate(displacement);
            if(scene.object_should_move&& gameObject.name!="Plane"){
                Vector3 direction=new Vector3();
                direction.set(scene.getRenderer().camera.targetPosition);
                direction.sub(scene.getRenderer().camera.cameraPosition);
                if(ray_intesects_Box(gameObject.min, gameObject.max, scene.getRenderer().camera.cameraPosition, direction)){
                    gameObject.translate(new Vector3(0,0.01f,0));
                }
            }
        }
        for (GameObject gameObject : scene.objects) {
            Vector3 min1=gameObject.min;
            Vector3 max1=gameObject.max;
            for(GameObject gameObject2:scene.objects){
                if(gameObject!=gameObject2){
                    Vector3 min2=gameObject2.min;
                    Vector3 max2=gameObject2.max;
                    if(check_collionsbound_overlap(min1,max1,min2,max2)&&gameObject.name!="Plane"&&gameObject2.name!="Plane"){
                      //  System.out.println("Collision detected:"+gameObject.name+" and "+gameObject2.name);
                        gameObject.translate(new Vector3().mul(deltatime).add(new Vector3(0,0,0)));
                        float x_momentum=(gameObject.velocity[0]*gameObject.mass)+(gameObject2.velocity[0]*gameObject2.mass);
                        float new_x_velociy=x_momentum/(gameObject.mass+gameObject2.mass);
                        gameObject.velocity[0]=new_x_velociy;
                        gameObject2.velocity[0]=new_x_velociy;
                    }
                }
            }
            
        }
    }
    public static boolean ray_intesects_Box(Vector3 min,Vector3 max,Vector3 ray_origin,Vector3 ray_direction){
            Vector3[] vertices={
                new Vector3(min.x,min.y,min.z),
                new Vector3(max.x,min.y,min.z),
                new Vector3(max.x,max.y,min.z),
                new Vector3(min.x,max.y,min.z),

                new Vector3(min.x,min.y,max.z),
                new Vector3(max.x,min.y,max.z),
                new Vector3(max.x,max.y,max.z),
                new Vector3(min.x,max.x,max.z),
            };
          //  System.out.println("Verticesend");
            if(ray_intesects_square(new Vector3[]{vertices[0],vertices[1],vertices[2],vertices[3]}, ray_origin, ray_direction, 2)){
                return true;
            }
            if(ray_intesects_square(new Vector3[]{vertices[4],vertices[5],vertices[6],vertices[7]}, ray_origin, ray_direction, 2)){
                return true;
            }
            if(ray_intesects_square(new Vector3[]{vertices[0],vertices[1],vertices[5],vertices[4]}, ray_origin, ray_direction, 2)){
                return true;
            }
            if(ray_intesects_square(new Vector3[]{vertices[2],vertices[3],vertices[6],vertices[7]}, ray_origin, ray_direction, 1)){
                return true;
            }
            if(ray_intesects_square(new Vector3[]{vertices[0],vertices[3],vertices[4],vertices[7]}, ray_origin, ray_direction, 0)){
                return true;
            }
            if(ray_intesects_square(new Vector3[]{vertices[1],vertices[2],vertices[5],vertices[6]}, ray_origin, ray_direction, 0)){
                return true;
            }
            
            return false;
            
        }
        public void tests() {
     //       System.err.println("max1"+scene.objects.get(2).max.x);  
       //     System.err.println("min2"+scene.objects.get(3).min.x);   
    
          //  scene.objects.get(2).rotatex(1);;  
        }
        public static boolean ray_intesects_plane(Vector3 plane_normal, Vector3 plane_point, Vector3 ray_origin, Vector3 ray_direction, Vector3 intersection_point) {
            float denominator = plane_normal.dot(ray_direction);
            if (denominator == 0) {
                return false;
            }
            //print_vector(plane_normal);
            float t = plane_normal.dot(plane_point.sub(ray_origin, new Vector3())) / denominator;
           // System.err.println(t);
            if (t < 0) {
                //return false;
            }
            intersection_point.set(ray_origin).add( ray_direction.mul(t));
            return true;
        }
       public static boolean ray_intesects_square(float[] square_vertices,Vector3 ray_origin,Vector3 raydirection,int square_direction){
            Vector3 translation=new Vector3(-square_vertices[0],-square_vertices[1],-square_vertices[2]);
            Vector3[] vertices=new Vector3[4];
            for(int i=0;i<4;i++){
                vertices[i]=new Vector3(square_vertices[i*3],square_vertices[(i*3)+1],square_vertices[(i*3)+2]).add(translation);
                //print_vector(vertices[i]);
            }
            Vector3 rayposition_when_plane_reached=new Vector3();
            float mul=ray_origin.x/raydirection.x;
            if(square_direction==1){
                mul=ray_origin.y/raydirection.y;
                
            }
            if(square_direction==2){
                mul=ray_origin.z/raydirection.z;
            }
            //ray_origin.sub(raydirection.mul(mul),rayposition_when_plane_reached);
            if(rayposition_when_plane_reached.x!=0){
               }
           // System.err.println("mul="+mul+";"+rayposition_when_plane_reached.x+"-"+rayposition_when_plane_reached.y+" "+rayposition_when_plane_reached.z);
            if(mul>0){
                return false;
            }
            if(square_direction==0){
                if(rayposition_when_plane_reached.y>vertices[2].y){
                    return false;
                }
                if(rayposition_when_plane_reached.z>vertices[2].z){
                    return false;
                }
                if(!(rayposition_when_plane_reached.y>0)){
                   // return false;
                }
                if(!(rayposition_when_plane_reached.z>0)){
                   // return false;
                }
            }
            if(square_direction==1){
                if(rayposition_when_plane_reached.x>vertices[2].x){
                    return false;
                }
                if(rayposition_when_plane_reached.z>vertices[2].z){
                    return false;
                }
                if(!(rayposition_when_plane_reached.x>0)){
                    return false;
                }
                if(!(rayposition_when_plane_reached.z>0)){
                    return false;
                }
            }
            if(square_direction==2){
                if(rayposition_when_plane_reached.y>vertices[2].y){
                    return false;
                }
                if(rayposition_when_plane_reached.x>vertices[2].x){
                    return false;
                }
                if(!(rayposition_when_plane_reached.y>0)){
                    return false;
                }
                if(!(rayposition_when_plane_reached.x>0)){
                    return false;
                }
            }
            return true;
       }
       public static boolean ray_intesects_square(Vector3[] vertices,Vector3 ray_origin,Vector3 raydirection,int square_direction){
            Vector3 normal = new Vector3();
            // for (Vector3 Vector3 : vertices) {
            //     Vector3.sub(vertices[0]);
            // }
            // ray_origin.sub(vertices[0]);
            Vector3 v0 = vertices[0];
            Vector3 v1 = vertices[1];
            Vector3 v2 = vertices[2];
            normal.set(v1.sub(v0, new Vector3()).cross( v2.sub(v0, new Vector3())));
            normal.normalize();
            System.err.println("square");
            for (Vector3 iterable_element : vertices) {
                print_vector(iterable_element);
            }
            System.err.println("Normal");
            print_vector(normal);
            Vector3 intersection_point = new Vector3();
            if (!ray_intesects_plane(normal, v0, ray_origin, raydirection, intersection_point)) {
                // System.err.println("Doesn't intesect Plane");
                // print_vector(v0);
                // print_vector(normal);
                // print_vector(ray_origin);
                // print_vector(raydirection.normalize());
                return false;
            }
            print_vector(intersection_point);
            Vector3 v = intersection_point.sub(v0, new Vector3());
            Vector3 edge1 = v1.sub(v0, new Vector3());
            Vector3 edge2 = v2.sub(v0, new Vector3());
            float dot00 = edge1.dot(edge1);
            float dot01 = edge1.dot(edge2);
            float dot02 = edge1.dot(v);
            float dot11 = edge2.dot(edge2);
            float dot12 = edge2.dot(v);
            float invDenom = 1 / (dot00 * dot11 - dot01 * dot01);
            float u = (dot11 * dot02 - dot01 * dot12) * invDenom;
            float vParam = (dot00 * dot12 - dot01 * dot02) * invDenom;
            return (u >= 0) && (vParam >= 0) && (u + vParam < 1);
        }
       public static void maisn(String[] args) {
         Vector3[] square_vertices={
            new Vector3(0,0,0),
            new Vector3(1,1,1.8f  ),
            new Vector3(1,1,0),
            new Vector3(1,0,0)
         };
         Vector3 ray_origin=new Vector3(5.0f,5.0f,5f);
         Vector3 ray_direction=new Vector3(-1f,-1f,-0.3f);
         Vector3 min=square_vertices[0];
         Vector3 max=square_vertices[1];
         System.err.println(ray_intesects_Box(max,min,ray_origin,ray_direction));
         //print_vector(max);
        }       
    public static void print_vector(Vector3 v){
        System.err.println("x="+v.x+"; y="+v.y+"; z=+"+v.z);
    }
    public static float[] get_rectangl(float[] position,float height ,float width,int axis){
            return null;

    }
    Vector3[] vertices=new Vector3[8];
    public static Vector3[] get_full_box(Vector3 min,Vector3 max){
        float[] boxVertices = new float[24];
        boxVertices[0] = min.x; boxVertices[1] = min.y; boxVertices[2] = min.z;
        boxVertices[3] = max.x; boxVertices[4] = min.y; boxVertices[5] = min.z;
        boxVertices[6] = max.x; boxVertices[7] = max.y; boxVertices[8] = min.z;
        boxVertices[9] = min.x; boxVertices[10] = max.y; boxVertices[11] = min.z;
        boxVertices[12] = min.x; boxVertices[13] = min.y; boxVertices[14] = max.z;
        boxVertices[15] = max.x; boxVertices[16] = min.y; boxVertices[17] = max.z;
        boxVertices[18] = max.x; boxVertices[19] = max.y; boxVertices[20] = max.z;
        boxVertices[21] = min.x; boxVertices[22] = max.y; boxVertices[23] = max.z;
        Vector3[] vertices=new Vector3[8];
        for(int i=0;i<8;i++){
            vertices[i]=new Vector3(boxVertices[i*3],boxVertices[(i*3)+1],boxVertices[(i*3)+2]);
        }
        return vertices;
}
    
}