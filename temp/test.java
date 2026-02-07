package com.njst.game1;
 
 class test extends RotatingCube{
    @Override
    public void init(){
        super.init();
        renderer.scene.loader=new Loader();
		System.out.println("initialised test");

    }
    public static void main(String[] args){
        new test().run();
    }
 }