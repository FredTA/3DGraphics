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
    SlideAndRock,
    SlideAndRoll,
    RockAndRoll,
    None
  }

  private AnimationSelections currentAnimation = AnimationSelections.None;
  private AnimationSelections pendingAnimation = AnimationSelections.None;

  private static final float ROTATION_STOP_BOUNDS = 0.34f;
  private static final float MAX_ROTATION_ALL_ANGLE = 35f;
  private static final float MAX_ROTATION_HEAD_ANGLE = 35f;

  private static final float SLIDE_STOP_BOUNDS = 0.1f;
  private static final float MAX_SLIDE_POSITION = 2f;


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
    programStartTime = getSeconds();
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
      stoppingAnimation = true; //This triggers the animations to stop
    } else {
      System.out.println("Non None selection");
      //If an animation was selected, but we are already animating
      if (currentAnimation != AnimationSelections.None) {
        System.out.println("But we are here...");
        stoppingAnimation = true; //This triggers the animations to stop
        pendingAnimation = newAnimationSelection;
      } else {
        //If an animation was selected, and we aren't already animating
        System.out.println("We should be here...");
        this.currentAnimation = newAnimationSelection;
        animationStartTime = getSeconds(); //Reset the start time so the animation doesn't start with a jump
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

  private TransformNode translateX, rotateAll, translateHead, rotateHead;
  private float xPositionStart = 0, xPosition = xPositionStart;
  private Vec3 headPosition, headPositionStart;
  private float rotateAllAngleStart = 0, rotateAllAngle = rotateAllAngleStart;
  private float rotateHeadAngleStart = 0, rotateHeadAngle = rotateHeadAngleStart;

  private static final float MAXIMUM_ANIMATION_SPEED = 1.15f;
  private static final float MINIMUM_ANIMATION_SPEED = 0.15f;
  private static final float ANIMATION_RAMP_UP_TIME = 3f; //The time it takes for the animation to start or stop
  private static final float ANIMATION_RAMP_DOWN_TIME = 2f;
  private float currentAnimationSpeed = 0;

  private static final float BODY_DIAMETER = 3.5f;
  private static final float BODY_TO_HEAD_RATIO = 1.6f;
  private static final float HEAD_DIAMETER = BODY_DIAMETER / BODY_TO_HEAD_RATIO;


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
    Mat4 m = Mat4Transform.scale(BODY_DIAMETER, BODY_DIAMETER, BODY_DIAMETER);
    m = Mat4.multiply(m, Mat4Transform.translate(0, 0.5f, 0));
    //Mat4 m = Mat4Transform.translate(0, BODY_DIAMETER / 2, 0);
    //m = Mat4.multiply(m, Mat4Transform.scale(BODY_DIAMETER, BODY_DIAMETER, BODY_DIAMETER));
    TransformNode makeBody = new TransformNode("Scale to body size and move up", m);
    ModelNode bodyNode = new ModelNode("Body", snowball);

    //---------------------Head-----------------

    headPosition = new Vec3(0, BODY_DIAMETER, 0);
    headPositionStart = headPosition;
    translateHead = new TransformNode("translate(0, BODY_DIAMETER, 0)", Mat4Transform.translate(headPosition.x, headPosition.y, headPosition.z));
    rotateHead = new TransformNode("rotateAroundZ("+rotateHeadAngle+")", Mat4Transform.rotateAroundZ(rotateHeadAngle));
    NameNode head = new NameNode("Head");
    m = Mat4Transform.scale(HEAD_DIAMETER, HEAD_DIAMETER, HEAD_DIAMETER);
    m = Mat4.multiply(m, Mat4Transform.translate(0, 0.4f, 0)); //TODO comment
    TransformNode makeHead = new TransformNode("scale(1.4f,1.4f,1.4f);translate(0,0.5,0)", m);
    ModelNode headNode = new ModelNode("Head", snowball);

    //-------------------Nose-------------------

    // TransformNode translateHead2 = new TransformNode("translate(0, BODY_DIAMETER, 0)",Mat4Transform.translate(0, BODY_DIAMETER, 0));
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
         body.addChild(translateHead);
           translateHead.addChild(rotateHead);
             rotateHead.addChild(head);
               head.addChild(makeHead);
                 makeHead.addChild(headNode);
//              head.addChild(translateHead2);
//                translateHead2.addChild(nose);
//                  nose.addChild(makeNoseBranch);
//                    makeNoseBranch.addChild(noseNode);
    snowmanRoot.update();  // IMPORTANT – must be done every time any part of the scene graph changes
    //snowman.print(0, false);
    //System.exit(0);
  }


  //private double animationStartTime;
  //private double lastElapsedTime;

  private void render(GL3 gl) {
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
    light.setPosition(getLightPosition());  // changing light position each frame
    light.render(gl);
    floor.render(gl);
    //updateBranches();
    snowmanRoot.draw(gl);

    if (currentAnimation != AnimationSelections.None) {
      handleAnimations();
    }
  }

  private double elapsedTime;
  private float animationSpeedAtTimeOfStop = -1;
  private boolean slowingDown = false;

  private float lastAnimationSpeed = 0;

  private float lastSinMagnitude = -1;

  private void handleAnimations() {

    elapsedTime = getSeconds() - animationStartTime;

      //If we're not stopping, and not yet at full speed
      if (!stoppingAnimation && currentAnimationSpeed < MAXIMUM_ANIMATION_SPEED) {
        float animationRampProgress =  (float)elapsedTime / ANIMATION_RAMP_UP_TIME;
        currentAnimationSpeed = MAXIMUM_ANIMATION_SPEED * animationRampProgress;
      }
      //Else if we are stopping, and still above the min speed
      else if (stoppingAnimation && currentAnimationSpeed > 0){

        float sinMagnitude = Math.abs((float)Math.sin(elapsedTime));

        if (lastSinMagnitude != -1) {
          if (Math.abs(sinMagnitude) > 0.99f && sinMagnitude < lastSinMagnitude) {
            slowingDown = true;
            animationSpeedAtTimeOfStop = currentAnimationSpeed;
          }
          if (slowingDown) {
            //If we use max, the speed could jump up if we weren't already at max speed
            currentAnimationSpeed = animationSpeedAtTimeOfStop * sinMagnitude;
            System.out.println("sin " + sinMagnitude + " Speed " + currentAnimationSpeed + " diff - " + (lastAnimationSpeed - currentAnimationSpeed));
          }
        }
        lastSinMagnitude = sinMagnitude;

      }

      lastAnimationSpeed = currentAnimationSpeed;

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
      case RockAndRoll :
        rock();
        roll();
        break;
      case SlideAndRoll :
        roll();
        slide();
        break;
      case SlideAndRock :
        rock();
        slide();
        break;
    }
    snowmanRoot.update(); // IMPORTANT – the scene graph has changed
  }

  private void rock() {

    //If we're stopping the animation
    if (stoppingAnimation && slowingDown) {
      //If the current rotation is within the stop bounds
      float rotateAllXAngle = MAX_ROTATION_ALL_ANGLE * (float)Math.cos(elapsedTime) * currentAnimationSpeed;
      if (rotateAllAngle > rotateAllAngleStart - ROTATION_STOP_BOUNDS && rotateAllAngle < rotateAllAngleStart + ROTATION_STOP_BOUNDS) {
        if (rotateAllXAngle > rotateAllAngleStart - ROTATION_STOP_BOUNDS && rotateAllXAngle < rotateAllAngleStart + ROTATION_STOP_BOUNDS) {
          //If both x and z are within bounds, we can stop rocking and reset

          rotateAll.setTransform(Mat4Transform.rotateAroundZ(rotateAllAngleStart));
          switch(currentAnimation) {
            case Rock :
              reset();
              break;
            case RockAndRoll :
              currentAnimation = AnimationSelections.Roll;
              break;
            case SlideAndRock :
              currentAnimation = AnimationSelections.Slide;
              break;
            case SlideRockAndRoll :
              currentAnimation = AnimationSelections.SlideAndRoll;
              break;
            }
        } else {
          //If Z is in bounds but X isn't keep moving x
          rotateAll.setTransform(Mat4Transform.rotateAroundX(rotateAllXAngle));
        }
      } else {
        if (rotateAllXAngle > rotateAllAngleStart - ROTATION_STOP_BOUNDS && rotateAllXAngle < rotateAllAngleStart + ROTATION_STOP_BOUNDS) {
          //If we're not in Z bounds, but are in X, keep moving Z
          float rotateAllAngle = MAX_ROTATION_ALL_ANGLE * (float)Math.sin(elapsedTime) * currentAnimationSpeed;
          rotateAll.setTransform(Mat4Transform.rotateAroundZ(rotateAllAngle));
        } else {
          //If we are stopping, but in the bounds of neither
          rockBothAxis();
        }
      }
    } else {
      //If we are not stopping
      rockBothAxis();
    }
  }

  private void rockBothAxis() {
    if (elapsedTime > 1.565f) {
      rotateAllAngle = MAX_ROTATION_ALL_ANGLE * (float)Math.sin(elapsedTime) * currentAnimationSpeed;
      float rotateAllXAngle = MAX_ROTATION_ALL_ANGLE * (float)Math.cos(elapsedTime) * currentAnimationSpeed;
      Mat4 m = Mat4Transform.rotateAroundZ(rotateAllAngle);
      m = Mat4.multiply(m, Mat4Transform.rotateAroundX(rotateAllXAngle));
      rotateAll.setTransform(m);
      //System.out.println("ROtate angle is " + rotateAllAngle + " - " + rotateAllXAngle);
    } else {
      float rotateAllAngle = MAX_ROTATION_ALL_ANGLE * (float)Math.sin(elapsedTime) * currentAnimationSpeed;
      rotateAll.setTransform(Mat4Transform.rotateAroundZ(rotateAllAngle));
      //System.out.println("ROtate angle is " + rotateAllAngle + " - " + rotateAllXAngle);
    }

  }

  private void roll() {
    //double elapsedTime = getSeconds()-startTime;

    //We do the translations in two goes, so that the rotation occurs from the centre of the body
    rotateHeadAngle = MAX_ROTATION_HEAD_ANGLE * (float)Math.sin(elapsedTime) * currentAnimationSpeed;
    Mat4 translateRadius = Mat4Transform.translate(0, BODY_DIAMETER / 2, 0);
    Mat4 m = Mat4.multiply(Mat4Transform.rotateAroundZ(rotateHeadAngle), translateRadius);
    m = Mat4.multiply(translateRadius, m);
    translateHead.setTransform(m);

    //If we're stopping the animation
    if (stoppingAnimation && slowingDown) {
      //If the current rotation is within the stop bounds
      if (rotateHeadAngle > rotateHeadAngleStart - ROTATION_STOP_BOUNDS && rotateHeadAngle < rotateHeadAngleStart + ROTATION_STOP_BOUNDS) {
        rotateHead.setTransform(Mat4Transform.rotateAroundZ(rotateHeadAngleStart));
        translateHead.setTransform(Mat4Transform.translate(headPositionStart));
        switch(currentAnimation) {
          case Roll :
            reset();
            break;
          case RockAndRoll :
            currentAnimation = AnimationSelections.Rock;
            break;
          case SlideAndRoll :
            currentAnimation = AnimationSelections.Slide;
            break;
          case SlideRockAndRoll :
            currentAnimation = AnimationSelections.SlideAndRock;
            break;
        }
      }
    }
  }

  private void slide() {
    //double elapsedTime = getSeconds()-startTime;

    xPosition = MAX_SLIDE_POSITION * (float)Math.sin(elapsedTime) * currentAnimationSpeed;
    translateX.setTransform(Mat4Transform.translate(xPosition,0,0));
    //translateX.update(); // IMPORTANT – the scene graph has changed

    //If we're stopping the animation
    if (stoppingAnimation && slowingDown) {
      //If the current rotation is within the stop bounds
      if (xPosition > xPositionStart - SLIDE_STOP_BOUNDS && xPosition < xPositionStart + SLIDE_STOP_BOUNDS) {
        switch(currentAnimation) {
          case Slide :
            reset();
            break;
          case SlideAndRoll :
            currentAnimation = AnimationSelections.Roll;
            break;
          case SlideAndRock :
            currentAnimation = AnimationSelections.Rock;
            break;
          case SlideRockAndRoll :
            currentAnimation = AnimationSelections.RockAndRoll;
            break;
        }
      }
    }
  }


  public void reset() {
    stoppingAnimation = false;
    animationStartTime = getSeconds();

    animationSpeedAtTimeOfStop = -1;
    currentAnimationSpeed = 0;
    slowingDown = false;

    lastSinMagnitude = -1;

    if (pendingAnimation != AnimationSelections.None) {
      currentAnimation = pendingAnimation;
      pendingAnimation = AnimationSelections.None;
    } else {
      currentAnimation = AnimationSelections.None;
    }
  }

  // The light's postion is continually being changed, so needs to be calculated for each frame.
  private Vec3 getLightPosition() {
    double elapsedTime = getSeconds() - programStartTime;
    float x = 5.0f*(float)(Math.sin(Math.toRadians(elapsedTime*50)));
    float y = 2.7f;
    float z = 5.0f*(float)(Math.cos(Math.toRadians(elapsedTime*50)));
    return new Vec3(x,y,z);
    //return new Vec3(5f,3.4f,5f);
  }

  // ***************************************************
  /* TIME
   */

  private double programStartTime;
  private double animationStartTime = -1;
  //TODO change to boolean we don't slow down
  private boolean stoppingAnimation = false; //-1 indicates that we are not stopping

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
