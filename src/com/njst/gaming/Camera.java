package com.njst.gaming;

import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Vector3;

public class Camera {
    public Vector3 cameraPosition;
    public Vector3 targetPosition; // The point around which the camera rotates
    public Vector3 upDirection; // The up direction of the camera
    public float distanceFromTarget; // Distance from the target point
    float FOV=60;
    float aspect;
    float near=0.1f,far=100;

    public Camera(Vector3 cameraPosition, Vector3 targetPosition, Vector3 upDirection) {
        this.cameraPosition = cameraPosition;
        this.targetPosition = targetPosition;
        this.upDirection = upDirection;
        this.distanceFromTarget = cameraPosition.distance(targetPosition);
    }
    public void lookAt(Vector3 cameraPosition, Vector3 targetPosition, Vector3 upDirection) {
        this.cameraPosition = cameraPosition;
        this.targetPosition = targetPosition;
        this.upDirection = upDirection;
        this.distanceFromTarget = cameraPosition.distance(targetPosition);
    }
    public void setPerspective(float FOV,float aspect,float near,float far) {
        this.FOV = FOV;
        this.aspect = aspect;
        this.near = near;
        this.far = far;
        }

    public void rotate(float pitch, float yaw) {
        // Update the target position based on pitch and yaw
        Vector3 direction = new Vector3(targetPosition).sub(cameraPosition).normalize();
        if(targetPosition.z>cameraPosition.z){
          //  direction = new Vector3(cameraPosition).sub(targetPosition).normalize();
          pitch=-pitch;
        }
        // Rotate around the up vector (yaw)
        direction.rotateY((float) Math.toRadians(yaw));
        
        // Rotate around the right vector (pitch)
       // Vector3 right = new Vector3(direction).cross(upDirection).normalize();
        direction.rotateX((float) Math.toRadians(pitch));
        // Update the target position
        targetPosition.set(cameraPosition).add(direction);
    }
    public void moveForward(float distance) {
        Vector3 direction = new Vector3(targetPosition).sub(cameraPosition);
        // direction.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        // direction.y = (float) (Math.sin(Math.toRadians(pitch)));
        // direction.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        direction.normalize();
        direction.mul(distance);
        cameraPosition.add(direction);
        targetPosition.add(direction);
       // upDirection.add(direction);
    }
    
    public Matrix4 getViewMatrix() {
        // Create a view matrix based on the camera position, target position, and up direction
        return new Matrix4().lookAt(cameraPosition, targetPosition, upDirection);
    }
    public Matrix4 getProjectionMatrix(){
        return new Matrix4().perspective(FOV,aspect,near,far);
    }
}