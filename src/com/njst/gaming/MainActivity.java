package com.njst.gaming;

public class MainActivity  {/* 
    private GLSurfaceView glSurfaceView;
    Scene s;
    RootLogger l;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create SoundPool with appropriate attributes
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        final SoundPool soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();
        
        // Load the sound
        final int soundId = soundPool.load(this, R.raw.sound, 1); // sound is the name of the audio file without extension
        //Toast.makeText(this,GeneralUtil.readFile("/sdcard/api.txt"),200).show();
        RelativeLayout relativeLayout = new RelativeLayout(this);

        LinearLayout dpadLayout = new LinearLayout(this);
        dpadLayout.setOrientation(LinearLayout.VERTICAL);
        dpadLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Create the button layout for the D-Pad
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Create D-Pad buttons
        Button buttonLeft = new Button(this);
        buttonLeft.setText("Left");
        buttonLeft.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        buttonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle left button press
                s.renderer.camera.cameraPosition.x+=1;
                soundPool.play(soundId, 1, 1, 0, 0, 1);

            }
        });

        Button buttonUp = new Button(this);
        buttonUp.setText("Up");
        buttonUp.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        buttonUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle up button press
               // s.r.angle=0;
                s.renderer.camera.targetPosition.y+=1;
            }
        });

        final Button buttonRight = new Button(this);
        buttonRight.setText("Right");
        buttonRight.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        buttonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle right button press
            s.renderer.camera.cameraPosition.z+=1;
            }
        });

        // Add buttons to button layout
        buttonLayout.addView(buttonLeft);
        buttonLayout.addView(buttonUp);
        buttonLayout.addView(buttonRight);
        // Create Down button
        Button buttonDown = new Button(this);
        buttonDown.setText("Down");
        buttonDown.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        buttonDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle down button press
                // s.renderer.camera.cameraPosition.x=10;
                // s.renderer.camera.cameraPosition.y=10;
                // s.renderer.camera.cameraPosition.z=10;
                s.object_should_move=true;
                s.renderer.camera.targetPosition.x=0;
                s.renderer.camera.targetPosition.y=0;
                s.renderer.camera.targetPosition.z=0;
            }
        });

        // Add button layout and down button to D-Pad layout
        dpadLayout.addView(buttonLayout);
        dpadLayout.addView(li2());
        dpadLayout.addView(buttonDown);

        // Set the layout parameters for the D-Pad layout
        RelativeLayout.LayoutParams dpadLayoutParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dpadLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        dpadLayout.setLayoutParams(dpadLayoutParams);

        // Add the GLSurfaceView and D-Pad layout to the RelativeLayout
        


         glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(3); // Use OpenGL ES 3
        s=new Scene(this);
        s.renderer.fpsView=buttonRight;
        l=new RootLogger(this,"log.txt");
        relativeLayout.addView(glSurfaceView, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        relativeLayout.addView(dpadLayout);
        l.logToRootDirectory("hu");
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            int i=0;
            @Override
            public void run() {
                // Code to execute every second
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    
                        buttonRight.setText("FPS:"+(s.renderer.fps*1000/(System.currentTimeMillis()-s.renderer.getlast())));
                    }
                });
            }
        }, 1000, 1000);
        //final RayTracingRenderer ray=new RayTracingRenderer(this);
        
        s.loader=new DefaultLoader();
        
        
      //  c.addAnimation(animate);
        
    //     Thread t=new Thread(){
    //         @Override
    //         public void run(){
            
    //         try{
    //             while (true) {
                    
                
    //             sleep(10000);
    //      //       l.logToRootDirectory((ray.frames)+"");
    //           //  last=s.r.getFps();             } 
    //           }  
    
    //         }catch(Exception e){
    //            // Toast.makeText(MainActivity.this,"HIEee",200).show();
            
    //         }
    //         }
    //     };
    //     try{
    //        // t.start();
    //     Toast.makeText(this,ShaderProgram.loadShader("/storage/emulated/0/android_vert.dart"),200).show();

    //     }catch(Exception e){
    //         Toast.makeText(MainActivity.this,"HIEe",200).show();
    //        // Toast.makeText(MainActivity.this,"HIEe",200).show();
    //     }
        glSurfaceView.setRenderer(s.getRenderer()); // Set the renderer
      //  l.logToRootDirectory("hu");
      glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
        float lastX, lastY;
       
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(event.getAction()==0){
                lastX=event.getY();
                lastY=event.getX();
            }
           // Toast.makeText(MainActivity.this, "button Clicked"+event.getAction(), 200).show();
            float newx=event.getY();
            float newy=event.getX();
            
            //s.renderer.camera.cameraPosition.add(new Vector3((newy-lastY)/80,(newx-lastX)/80,0));
          //  s.renderer.camera.cameraPosition=original_position;//.add(new Vector3((float)Math.sin(newx-lastX),0,0-(float)Math.sin(newx-lastX)));
           s.renderer.camera.targetPosition=s.renderer.camera.targetPosition.sub(
                    s.renderer.camera.cameraPosition)
                    .rotateY((newy-lastY)/80)
                    .rotateX((newx-lastX)/-80)
                    .add(s.renderer.camera.cameraPosition);
                        
            //s.renderer.camera.upDirection.add(new Vector3(0,(0)/80,(newx-lastX)/80));
        
            lastX=newx;
            lastY=newy;
            return true;
        }
    });
         setContentView(relativeLayout);
       // Toast.makeText(MainActivity.this,"HIEe",200).show();
    }

    @Override
    protected void onPause() {
         super.onPause();
    //     float[] mm=s.objects.get(0).collisionBox;
    //     String se="";
    //     for(int i=0;i<mm.length/4;i++){
    //         String rows="{";
    //         for(int i1=0;i1<4;i1++){
    //             if(i1!=0){
    //                 rows=rows+",";
    //             }
    //             rows=rows+mm[(i*4)+i1];
    //         }
    //         System.out.println(rows+"}\n");
    //         se=se+rows;
    //     }
    //   //  Toast.makeText(MainActivity.this,"--"+se,2000).show();
    //     //glSurfaceView.onPause();
    }
    // public static String floatatostring(float[] mm,int n){
    //     String se="{";
    //     for(int i=0;i<mm.length/n;i++){
    //         String rows="   {";
    //         for(int i1=0;i1<n;i1++){
    //             if(i1!=0){
    //                 rows=rows+",";
    //             }
    //             rows=rows+mm[(i*n)+i1];
    //         }
    //         //System.out.println(rows+"}\n");
    //         se=se+rows+"}\n";
    //     }
    //     return se+"}";
    // }
    // public static String shortatostring(short[] mm,int n){
    //     String se="{";
    //     for(int i=0;i<mm.length/n;i++){
    //         String rows="   {";
    //         for(int i1=0;i1<n;i1++){
    //             if(i1!=0){
    //                 rows=rows+",";
    //             }
    //             rows=rows+mm[(i*n)+i1];
    //         }
    //         //System.out.println(rows+"}\n");
    //         se=se+rows+"}\n";
    //     }
    //     return se+"}";
    // }
    // @Override
    // protected void onResume() {
    //     super.onResume();
    //     glSurfaceView.onResume();
    // }
    // @Override 
    // protected void onDestroy(){
      
    // }
    public LinearLayout li2(){
      LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Create D-Pad buttons
        Button buttonLeft = new Button(this);
        buttonLeft.setText("Left");
        buttonLeft.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        buttonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle left button press
                s.renderer.camera.moveForward(0.1f);
            }
        });

        Button buttonUp = new Button(this);
        buttonUp.setText("Up");
        buttonUp.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        buttonUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle up button press
               // s.r.angle=0;
                s.renderer.camera.targetPosition.y-=1;
            }
        });

        Button buttonRight = new Button(this);
        buttonRight.setText("Right");
        buttonRight.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        buttonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle right button press
                s.renderer.camera.cameraPosition.z-=1;
            }
        });
        buttonRight.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                //Toast.makeText(MainActivity.this, "button Clicked"+event.getAction(), 200).show();
                switch (event.getAction()) {
                    case 0:
                        Toast.makeText(MainActivity.this, "button Clicked", 200).show();
                        s.renderer.camera_should_move=true;
                        break;
                    case 1:
                        Toast.makeText(MainActivity.this, "button Released", 200).show();
                        s.renderer.camera_should_move=false;
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

        // Add buttons to button layout
        buttonLayout.addView(buttonLeft);
        buttonLayout.addView(buttonUp);
        buttonLayout.addView(buttonRight);
        return buttonLayout;
    }*/
}
