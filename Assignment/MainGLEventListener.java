import gmaths.*;

import java.nio.*;
import com.jogamp.common.nio.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.awt.*;
import com.jogamp.opengl.util.glsl.*;

public class MainGLEventListener implements GLEventListener {

  private static final boolean DISPLAY_SHADERS = false;

  public enum AnimationSelections{
    Rock,
    Roll,
    Slide,
    SlideRockAndRoll,
    None
  }

  private AnimationSelections currentAnimation = AnimationSelections.None;
  private AnimationSelections pendingAnimation = AnimationSelections.None;

  private static final float ANIMATION_STOP_BOUNDS = 0.5f;
  private static final float MAX_ROTATION_ALL_ANGLE = 35f;

  private static final float MAX_ROTATION_HEAD_ANGLE = 35f;

  private TransformNode initialBodyRotation;
  private TransformNode initialHeadRotation;
  private TransformNode initialBodyPosition;

  public MainGLEventListener(Camera camera) {
    this.camera = camera;
    this.camera.setPosition(new Vec3(4f,12f,18f));
  }

  // ***************************************************
  /*
   * METHODS DEFINED BY GLEventListener
   */

  /* Initialisation */
  public void init(GLAutoDrawable drawable) {
    GL3 gl = drawable.getGL().getGL3();
    System.err.println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities());
    gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    gl.glClearDepth(1.0f);
    gl.glEnable(GL.GL_DEPTH_TEST);
    gl.glDepthFunc(GL.GL_LESS);
    gl.glFrontFace(GL.GL_CCW);    // default is 'CCW'
    gl.glEnable(GL.GL_CULL_FACE); // default is 'not enabled'
    gl.glCullFace(GL.GL_BACK);   // default is 'back', assuming CCW
    initialise(gl);
    startTime = getSeconds();
  }

  /* Called to indicate the drawing surface has been moved and/or resized  */
  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    GL3 gl = drawable.getGL().getGL3();
    gl.glViewport(x, y, width, height);
    float aspect = (float)width/(float)height;
    camera.setPerspectiveMatrix(Mat4Transform.perspective(45, aspect));
  }

  /* Draw */
  public void display(GLAutoDrawable drawable) {
    GL3 gl = drawable.getGL().getGL3();
    render(gl);
  }

  /* Clean up memory, if necessary */
  public void dispose(GLAutoDrawable drawable) {
    GL3 gl = drawable.getGL().getGL3();
    light.dispose(gl);
    floor.dispose(gl);
    snowball.dispose(gl);
  }


  // ***************************************************
  /* INTERACTION
   *
   *
   */

  public void selectAnimation(AnimationSelections newAnimationSelection) {
    if (newAnimationSelection == AnimationSelections.None) {
      if (stopTime == -1) {
        //Set the stop time if we aren't already stopping
        stopTime = getSeconds(); //This triggers the animations to stop
      }
    } else {
      System.out.println("Non None selection");
      //If an animation was selected, but we are already animating
      if (currentAnimation != AnimationSelections.None) {
        stopTime = getSeconds(); //This triggers the animations to stop
        pendingAnimation = newAnimationSelection;
      } else {
        //If an animation was selected, and we aren't already animating
        System.out.println("We should be here...");
        this.currentAnimation = newAnimationSelection;
        startTime = getSeconds(); //Reset the start time so the animation doesn't start with a jump
      }
    }
  }

  // ***************************************************
  /* THE SCENE
   * Now define all the methods to handle the scene.
   * This will be added to in later examples.
   */

  private Camera camera;
  private Mat4 perspective;
  private Model floor, snowball, nose;
  private Light light;
  private SGNode snowmanRoot;

  private TransformNode translateX, rotateAll, rotateHead;
  private float xPosition = 0;
  private float rotateAllAngleStart = 0, rotateAllAngle = rotateAllAngleStart;
  private float rotateHeadAngleStart = 0, rotateHeadAngle = rotateHeadAngleStart;

  private float maximumRotationSpeed = 1.15f;
  private float rotationRampTime = 0.01f; //The time it takes for the animation to start or stop
  private float currentRotationSpeed = 0;

  private float bodyDiameter = 3.5f;
  private float bodyToHeadRatio = 1.6f;
  private float headDiameter = bodyDiameter / bodyToHeadRatio;


  private void initialise(GL3 gl) {
    createRandomNumbers();
    int[] textureId0 = TextureLibrary.loadTexture(gl, "textures/chequerboard.jpg");
    int[] textureId1 = TextureLibrary.loadTexture(gl, "textures/jade.jpg");
    int[] textureId2 = TextureLibrary.loadTexture(gl, "textures/jade_specular.jpg");

    light = new Light(gl);
    light.setCamera(camera);

    //-----------Floor--------------------

    Mesh mesh = new Mesh(gl, TwoTriangles.vertices.clone(), TwoTriangles.indices.clone());
    Shader shader = new Shader(gl, "vs_tt.txt", "fs_tt.txt");
    Material material = new Material(new Vec3(0.0f, 0.5f, 0.81f), new Vec3(0.0f, 0.5f, 0.81f), new Vec3(0.3f, 0.3f, 0.3f), 32.0f);
    Mat4 modelMatrix = Mat4Transform.scale(16,1f,16);
    floor = new Model(gl, camera, light, shader, material, modelMatrix, mesh, textureId0);


    //------------Body & Head--------------

    mesh = new Mesh(gl, Sphere.vertices.clone(), Sphere.indices.clone());
    shader = new Shader(gl, "vs_cube.txt", "fs_cube.txt");
    material = new Material(new Vec3(1.0f, 0.5f, 0.31f), new Vec3(1.0f, 0.5f, 0.31f), new Vec3(0.5f, 0.5f, 0.5f), 32.0f);
    //modelMatrix = Mat4.multiply(Mat4Transform.scale(4,4,4), Mat4Transform.translate(0,0.5f,0)); Not using this, so lets just set this to the identity matrix instead
    modelMatrix = new Mat4(1);
    snowball = new Model(gl, camera, light, shader, material, modelMatrix, mesh, textureId1, textureId2);

    //------------Nose---------------

    //mesh = new Mesh(gl, Sphere.vertices.clone(), Sphere.indices.clone());
    shader = new Shader(gl, "vs_cube.txt", "fs_cube.txt");
    material = new Material(new Vec3(1.0f, 0.5f, 0.31f), new Vec3(1.0f, 0.5f, 0.31f), new Vec3(0.5f, 0.5f, 0.5f), 32.0f);
    //modelMatrix = Mat4.multiply(Mat4Transform.scale(4,4,4), Mat4Transform.translate(0,0.5f,0)); Not using this, so lets just set this to the identity matrix instead
    modelMatrix = new Mat4(1);
    nose = new Model(gl, camera, light, shader, material, modelMatrix, mesh, textureId1, textureId2); //TODO change texture

   //------------------------------Making the snoman---------------------------

    snowmanRoot = new NameNode("snowman structure");

    //------------------Body----------------------

    translateX = new TransformNode("translate("+xPosition+",0,0)", Mat4Transform.translate(xPosition,0,0));
    rotateAll = new TransformNode("rotateAroundZ("+rotateAllAngle+")", Mat4Transform.rotateAroundZ(rotateAllAngle));
    NameNode body = new NameNode("Body");
    Mat4 m = Mat4Transform.scale(bodyDiameter, bodyDiameter, bodyDiameter);
    m = Mat4.multiply(m, Mat4Transform.translate(0, 0.5f, 0));
    //Mat4 m = Mat4Transform.translate(0, bodyDiameter / 2, 0);
    //m = Mat4.multiply(m, Mat4Transform.scale(bodyDiameter, bodyDiameter, bodyDiameter));
    TransformNode makeBody = new TransformNode("Scale to body size and move up", m);
    ModelNode bodyNode = new ModelNode("Body", snowball);

    //---------------------Head-----------------

    TransformNode translateToTop = new TransformNode("translate(0, bodyDiameter, 0)",Mat4Transform.translate(0, bodyDiameter, 0));
    rotateHead = new TransformNode("rotateAroundZ("+rotateHeadAngle+")",Mat4Transform.rotateAroundZ(rotateHeadAngle));
    NameNode head = new NameNode("Head");
    m = Mat4Transform.scale(headDiameter, headDiameter, headDiameter);
    m = Mat4.multiply(m, Mat4Transform.translate(0, 0.4f, 0)); //TODO comment
    TransformNode makeHead = new TransformNode("scale(1.4f,1.4f,1.4f);translate(0,0.5,0)", m);
    ModelNode headNode = new ModelNode("Head", snowball);

    //-------------------Nose-------------------

    // TransformNode translateToTop2 = new TransformNode("translate(0, bodyDiameter, 0)",Mat4Transform.translate(0, bodyDiameter, 0));
    // NameNode nose = new NameNode("Nose");
    // m = Mat4Transform.scale(1.6f,2.8f,1.6f);
    // m = Mat4.multiply(m, Mat4Transform.translate(0,0.5f,0));
    // TransformNode makeNoseBranch = new TransformNode("scale(0,6f,1.4f,0.6f);translate(0,0.5,0)", m);
    // ModelNode noseNode = new ModelNode("Nose", snowball);

    snowmanRoot.addChild(translateX);
      translateX.addChild(rotateAll);
        rotateAll.addChild(body);
          body.addChild(makeBody);
            makeBody.addChild(bodyNode);
         body.addChild(translateToTop);
           translateToTop.addChild(rotateHead);
             rotateHead.addChild(head);
               head.addChild(makeHead);
                 makeHead.addChild(headNode);
//              head.addChild(translateToTop2);
//                translateToTop2.addChild(nose);
//                  nose.addChild(makeNoseBranch);
//                    makeNoseBranch.addChild(noseNode);
    snowmanRoot.update();  // IMPORTANT – must be done every time any part of the scene graph changes
    //snowman.print(0, false);
    //System.exit(0);
  }

  private void render(GL3 gl) {
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
    light.setPosition(getLightPosition());  // changing light position each frame
    light.render(gl);
    floor.render(gl);
    //updateBranches();
    snowmanRoot.draw(gl);

    handleAnimations();
  }

  private void handleAnimations() {
    //If there is a pending animation, and the current one has stopped...
    if (pendingAnimation != AnimationSelections.None && stopTime == -1) {
      currentAnimation = pendingAnimation;
      pendingAnimation = AnimationSelections.None;
    }

    if (currentAnimation != AnimationSelections.None) {
      switch(currentAnimation) {
        case Rock :
          rock();
          break;
        case Roll :
          roll();
          break;
        case Slide :
          slide();
          break;
        case SlideRockAndRoll :
          rock();
          roll();
          slide();
          break;
      }
      snowmanRoot.update(); // IMPORTANT – the scene graph has changed
    }
  }

   private void updateX() {
     translateX.setTransform(Mat4Transform.translate(xPosition,0,0));
     translateX.update(); // IMPORTANT – the scene graph has changed
   }

  private void rock() {
    double elapsedTime = getSeconds()-startTime;

    rotateAllAngle = MAX_ROTATION_ALL_ANGLE*(float)Math.sin(elapsedTime);
    rotateAll.setTransform(Mat4Transform.rotateAroundZ(rotateAllAngle));
    //System.out.println("ROtate angle is " + rotateAllAngle);

    //If we're stopping the animation
    if (stopTime != -1) {
      System.out.println("Looking to stop");
      //If the current rotation is within the stop bounds
      if (rotateAllAngle > rotateAllAngleStart - ANIMATION_STOP_BOUNDS && rotateAllAngle < rotateAllAngleStart + ANIMATION_STOP_BOUNDS) {
        reset();
      }
    }
  }

  private void roll() {
    double elapsedTime = getSeconds()-startTime;

    rotateHeadAngle = MAX_ROTATION_HEAD_ANGLE*(float)Math.sin(elapsedTime);
    rotateHead.setTransform(Mat4Transform.rotateAroundZ(rotateHeadAngle));

    //If we're stopping the animation
    if (stopTime != -1) {
      System.out.println("Looking to stop");
      //If the current rotation is within the stop bounds
      if (rotateHeadAngle > rotateHeadAngleStart - ANIMATION_STOP_BOUNDS && rotateHeadAngle < rotateHeadAngleStart + ANIMATION_STOP_BOUNDS) {
        reset();
      }
    }
  }

  private void slide() {

  }

  public void reset() {
    //TODO bottom two lines shouldn't be needed
    rotateAll.setTransform(Mat4Transform.rotateAroundZ(rotateAllAngleStart));
    rotateHead.setTransform(Mat4Transform.rotateAroundZ(rotateHeadAngleStart));
    stopTime = -1;
    currentRotationSpeed = 0;
    //Reset slide

    System.out.println("Resetting...");

    currentAnimation = AnimationSelections.None;

    snowmanRoot.update();
  }

  // The light's postion is continually being changed, so needs to be calculated for each frame.
  private Vec3 getLightPosition() {
    double elapsedTime = getSeconds()-startTime;
    float x = 5.0f*(float)(Math.sin(Math.toRadians(elapsedTime*50)));
    float y = 2.7f;
    float z = 5.0f*(float)(Math.cos(Math.toRadians(elapsedTime*50)));
    return new Vec3(x,y,z);
    //return new Vec3(5f,3.4f,5f);
  }

  // ***************************************************
  /* TIME
   */

  private double startTime;
  private double stopTime = -1; //-1 indicates that we are not stopping

  private double getSeconds() {
    return System.currentTimeMillis()/1000.0;
  }

  // ***************************************************
  /* An array of random numbers
   */

  private int NUM_RANDOMS = 1000;
  private float[] randoms;

  private void createRandomNumbers() {
    randoms = new float[NUM_RANDOMS];
    for (int i=0; i<NUM_RANDOMS; ++i) {
      randoms[i] = (float)Math.random();
    }
  }

}
