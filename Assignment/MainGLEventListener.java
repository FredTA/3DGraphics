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

  private TransformNode initialBodyRotation;
  private TransformNode initialHeadRotation;
  private TransformNode initialBodyPosition;

  public MainGLEventListener(Camera camera) {
    this.camera = camera;
    this.camera.setPosition(new Vec3(-7f,16f,26f));
    camera.updateYawPitch((float)Math.toRadians(-3), (float)Math.toRadians(9));
  }

  //-----------------METHODS DEFINED BY GLEventListener----------------------

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
    lastTime = getSeconds();
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
    mainLight.dispose(gl);
    floor.dispose(gl);
    snowball.dispose(gl);
  }

  //---------------------------INTERACTION----------------------------------

  public void selectAnimation(AnimationSelections newAnimationSelection) {
    //Check if the selected animation isn't already selected
    if (newAnimationSelection != currentAnimation) {
      if (newAnimationSelection == AnimationSelections.None) {
        stoppingAnimation = true; //This triggers the animations to begin stopping
      } else {
        //If an animation was selected, but we are already animating
        if (currentAnimation != AnimationSelections.None) {
          stoppingAnimation = true; //This triggers the animations to stop
          pendingAnimation = newAnimationSelection;
        } else {
          //If an animation was selected, and we aren't already animating
          this.currentAnimation = newAnimationSelection;
          animationStartTime = getSeconds(); //Reset the start time so the animation doesn't start with a jump
        }
      }
    }
  }

  public void decreaseLightIntensity() {
    mainLight.decreaseLightIntensity();
  }

  public void increaseLightIntensity() {
    mainLight.increaseLightIntensity();
  }

  public void toggleSpotlight(){
    spotlightActive = !spotlightActive;
    spotlight.toggle();
  }


  // ----------------------THE SCENE------------------------------

  private Camera camera;
  private Mat4 perspective;
  private Model floor, snowball, smoothStone, roughStone, topHatMain, topHatRibbon, crate, crate2, metal;
  private AnimatedModel background;
  private Light mainLight;
  private Spotlight spotlight;
  private SGNode snowmanRoot, spotlightRoot;

  private TransformNode translateX, rotateAll, translateHead, rollHead, rotateSpotlight;
  private float xPositionStart = 0, xPosition = xPositionStart;
  private Vec3 headPosition, headPositionStart;
  private float rotateAllAngleStart = 0, rotateAllAngle = rotateAllAngleStart;
  private float rotateSpotlightAngleStart = 0, rotateSpotlightAngle = rotateSpotlightAngleStart;
  private float rollHeadAngleStart = 0, rollHeadAngle = rollHeadAngleStart;

  //SNOWMAN BODY-----
  private static final float BODY_DIAMETER = 3.5f;
  private static final float HEAD_HEIGHT_OFFSET = -0.2f; //I think the head looks a little better slightly clipped into the body, "mushed together" like a real snowman
  private static final float BODY_TO_HEAD_RATIO = 1.6f;
  private static final float BODY_TO_BUTTON_RATIO = 6.5f;
  private static final float BUTTON_SIZE = BODY_DIAMETER / BODY_TO_BUTTON_RATIO;
  private static final float ODD_BUTTONS_ANGLE =25f;
  //SNOWMAN HEAD------
  private static final float HEAD_DIAMETER = BODY_DIAMETER / BODY_TO_HEAD_RATIO;
  private static final float HEAD_TO_NOSE_RATIO = 6.7f;
  private static final float NOSE_LENGTH_RATIO = 0.45f;
  private static final float NOSE_SIZE = HEAD_DIAMETER / HEAD_TO_NOSE_RATIO;
  private static final float NOSE_LENGTH = NOSE_SIZE / NOSE_LENGTH_RATIO;
  private static final float MOUTH_ANGLE = 20f;
  private static final float HEAD_TO_EYE_RATIO = 5.3f;
  private static final float EYE_SIZE = HEAD_DIAMETER / HEAD_TO_EYE_RATIO;
  private static final float EYE_ANGLE_X = -15f;
  private static final float EYE_ANGLE_Y = 20f;
  private static final float EYE_OFFSET = 0.5f;
  //SNOMAN HAT------
  private static final float TOP_HAT_MAIN_OFFSET = -0.3f; //So that it sits a bit lower on the head
  private static final float TOP_HAT_MAIN_HEIGHT = 1.45f;
  private static final float TOP_HAT_MAIN_WIDTH = 1.3f;
  private static final float TOP_HAT_RIM_HEIGHT = 0.15f;
  private static final float TOP_HAT_RIM_WIDTH = 2.2f;
  private static final float TOP_HAT_BAND_HEIGHT = 0.3f;
  private static final float TOP_HAT_BAND_WIDTH = 1.34f;
  //LIGHTS--------
  private static final float MAIN_LIGHT_X = 6.1f;
  private static final float MAIN_LIGHT_Y = 18.4f;
  private static final float MAIN_LIGHT_Z = 15.0f;
  private float spotlightLampBaseX = -9.5f;
  private float spotlightLampBaseY = 13f;
  private float spotlightLampBaseZ = 0;
  private static float SPOTLIGHT_ROTATION_Z = 40f;
  private static float SPOTLIGHT_INNER_CUTTOFF = 28f;
  private static float SPOTLIGHT_OUTER_CUTOFF = 30.5f;

  private void initialise(GL3 gl) {
    setupLights(gl);
    setupModels(gl);
    setupSpotlightSceneGraph();
    setupSnowmanSceneGraph();
    //
    // spotlightRoot.print(0, false);
    // snowmanRoot.print(0, false);
    // System.exit(0);
  }

  private void setupLights(GL3 gl) {
    //Setup the main world light - make it a little yellowy
    Vec3 mainLightAmbient = new Vec3(0.5f, 0.5f, 0.47f);
    Vec3 mainLightDiffuse = new Vec3(0.8f, 0.8f, 0.77f);
    Vec3 mainLightSpecular = new Vec3(0.8f, 0.8f, 0.77f);

    mainLight = new Light(gl, mainLightAmbient, mainLightDiffuse, mainLightSpecular);
    mainLight.setPosition(new Vec3(MAIN_LIGHT_X, MAIN_LIGHT_Y, MAIN_LIGHT_Z));
    mainLight.setCamera(camera);

    //Setup the main spotlight - make it a lot yellowy
    Vec3 spotlightAmbient = new Vec3(0, 0, 0); //None for a spotlight!
    Vec3 spotlightDiffuse = new Vec3(1, 1, 0.25f);
    Vec3 spotlightSpecular = new Vec3(1, 1, 0.25f);

    spotlight = new Spotlight(gl, spotlightAmbient, spotlightDiffuse, spotlightSpecular,
                         (float)Math.cos(Math.toRadians(SPOTLIGHT_INNER_CUTTOFF)),
                         (float)Math.cos(Math.toRadians(SPOTLIGHT_OUTER_CUTOFF)));
    spotlight.setCamera(camera);
  }

  private void setupModels(GL3 gl) {
    int[] groundTexture = TextureLibrary.loadTexture(gl, "textures/ice.jpg");
    int[] backgroundTexture = TextureLibrary.loadTexture(gl, "textures/woods.jpg");
    int[] snowfallTexture = TextureLibrary.loadTexture(gl, "textures/snowfall.jpg");

    int[] crateTexture = TextureLibrary.loadTexture(gl, "textures/container2.jpg");
    int[] crateSpeculularTexture = TextureLibrary.loadTexture(gl, "textures/container2_specular.jpg");

    int[] spotlightTexture = TextureLibrary.loadTexture(gl, "textures/metal.jpg");

    int[] snowTexture = TextureLibrary.loadTexture(gl, "textures/snow.jpg");
    int[] stoneRoughTexture = TextureLibrary.loadTexture(gl, "textures/stone.jpg");
    int[] stoneSmoothTexture = TextureLibrary.loadTexture(gl, "textures/stoneSmooth.jpg");
    int[] topHatMainTexture = TextureLibrary.loadTexture(gl, "textures/hatMain.jpg");
    int[] topHatBandTexture = TextureLibrary.loadTexture(gl, "textures/ribbon.jpg");

    //-----------Floor--------------------

    Mesh mesh = new Mesh(gl, TwoTriangles.vertices.clone(), TwoTriangles.indices.clone());
    Shader shader = new Shader(gl, "vs_main.txt", "fs_main.txt");
    Material material = new Material(new Vec3(0.68f, 0.73f, 0.8f), new Vec3(0.58f, 0.63f, 0.7f), new Vec3(0.9f, 0.9f, 0.9f), 32.0f);
    Mat4 modelMatrix = Mat4Transform.scale(32,1f,24);
    floor = new Model(gl, camera, mainLight, spotlight, shader, material, modelMatrix, mesh, groundTexture);

    //-----------Background--------------------

    mesh = new Mesh(gl, TwoTriangles.vertices.clone(), TwoTriangles.indices.clone());
    shader = new Shader(gl, "vs_animated.txt", "fs_animated.txt");
    material = new Material(new Vec3(0.8f, 0.8f, 0.8f), new Vec3(0.8f, 0.8f, 0.8f), new Vec3(0.0f, 0.0f, 0.0f), 32.0f);
    modelMatrix = Mat4Transform.translate(0, 8, -12f);
    modelMatrix = Mat4.multiply(modelMatrix, Mat4Transform.rotateAroundX(90));
    modelMatrix = Mat4.multiply(modelMatrix, Mat4Transform.scale(32,1f,16));
    background = new AnimatedModel(gl, camera, mainLight, spotlight, shader, material, modelMatrix, mesh, backgroundTexture, snowfallTexture);

    //-----------Crates--------------------

    mesh = new Mesh(gl, Cube.vertices.clone(), Cube.indices.clone());
    shader = new Shader(gl, "vs_main.txt", "fs_crate.txt");
    material = new Material(new Vec3(0.9f, 0.9f, 0.9f), new Vec3(0.7f, 0.7f, 0.7f), new Vec3(1, 1, 1), 32.0f);
    modelMatrix = Mat4Transform.translate(10.5f, 2.9f, 0f);
    modelMatrix = Mat4.multiply(modelMatrix, Mat4Transform.rotateAroundY(25));
    modelMatrix = Mat4.multiply(modelMatrix, Mat4Transform.rotateAroundZ(55));
    modelMatrix = Mat4.multiply(modelMatrix, Mat4Transform.scale(4.2f, 4.2f, 4.2f));
    crate = new Model(gl, camera, mainLight, spotlight, shader, material, modelMatrix, mesh, crateTexture, crateSpeculularTexture);

    modelMatrix = Mat4Transform.translate(7.4f, (1.7f / 2), 0f);
    modelMatrix = Mat4.multiply(modelMatrix, Mat4Transform.rotateAroundY(25));
    modelMatrix = Mat4.multiply(modelMatrix, Mat4Transform.translate(0f, 0f, 1.3f));
    modelMatrix = Mat4.multiply(modelMatrix, Mat4Transform.scale(1.7f, 1.7f, 1.7f));
    crate2 = new Model(gl, camera, mainLight, spotlight, shader, material, modelMatrix, mesh, crateTexture, crateSpeculularTexture);

    //-----------Spotlight pole--------------------

    mesh = new Mesh(gl, Cube.vertices.clone(), Cube.indices.clone());
    shader = new Shader(gl, "vs_main.txt", "fs_main.txt");
    material = new Material(new Vec3(0.8f, 0.8f, 0.9f), new Vec3(0.8f, 0.8f, 0.9f), new Vec3(0.9f, 0.9f, 0.9f), 32.0f);
    modelMatrix = new Mat4(1);
    metal = new Model(gl, camera, mainLight, spotlight, shader, material, modelMatrix, mesh, spotlightTexture);

    //------------Body & Head--------------

    mesh = new Mesh(gl, Sphere.vertices.clone(), Sphere.indices.clone());
    shader = new Shader(gl, "vs_main.txt", "fs_main.txt");
    material = new Material(new Vec3(0.7f, 0.7f, 0.7f), new Vec3(0.7f, 0.7f, 0.7f), new Vec3(0.2f, 0.2f, 0.2f), 32.0f);
    modelMatrix = new Mat4(1);
    snowball = new Model(gl, camera, mainLight, spotlight, shader, material, modelMatrix, mesh, snowTexture);

    //------------Nose & Mouth---------------

    //Smoth, polished stone, so we should have a greater specular
    material = new Material(new Vec3(0.7f, 0.7f, 0.7f), new Vec3(0.7f, 0.7f, 0.7f), new Vec3(0.7f, 0.7f, 0.7f), 32.0f);
    modelMatrix = new Mat4(1);
    smoothStone = new Model(gl, camera, mainLight, spotlight, shader, material, modelMatrix, mesh, stoneSmoothTexture);

    //------------Eyes and buttons---------------

    //Rough stone, so less specular
    material = new Material(new Vec3(0.85f, 0.85f, 0.85f), new Vec3(0.7f, 0.7f, 0.7f), new Vec3(0, 0, 0), 32.0f);
    modelMatrix = new Mat4(1);
    roughStone = new Model(gl, camera, mainLight, spotlight, shader, material, modelMatrix, mesh, stoneRoughTexture);

    //------------Top hat cylinders

    mesh = new Mesh(gl, Cube.vertices.clone(), Cube.indices.clone());
    shader = new Shader(gl, "vs_main.txt", "fs_main.txt");
    //Top hat material should have little specular
    material = new Material(new Vec3(1, 1, 1), new Vec3(1, 1, 1), new Vec3(0.1f, 0.1f, 0.1f), 32.0f);
    modelMatrix = new Mat4(1);
    topHatMain = new Model(gl, camera, mainLight, spotlight, shader, material, modelMatrix, mesh, topHatMainTexture);

    //Top hat ribon should have lots of specular
    material = new Material(new Vec3(0.5f, 0.5f, 0.5f), new Vec3(0.7f, 0.7f, 0.7f), new Vec3(0.9f, 0.9f, 0.9f), 32.0f);
    modelMatrix = new Mat4(1);
    topHatRibbon = new Model(gl, camera, mainLight, spotlight, shader, material, modelMatrix, mesh, topHatBandTexture);
  }

  private void setupSpotlightSceneGraph() {
    spotlightRoot = new NameNode("Spotlight Root");
    NameNode spotlightPole = new NameNode("Spotlight pole");
    Mat4 m = Mat4Transform.translate(-9.5f, 6f, 0f);
    TransformNode makeSpotlightPole = new TransformNode("Move pole and scale", m);
    TransformNode scaleSpotlightPole = new TransformNode("Scale spotlight pole", Mat4Transform.scale(0.6f, 12f, 0.6f));
    ModelNode spotlightPoleNode = new ModelNode("Spotlight Pole", metal);

    NameNode spotlightPole2 = new NameNode("Spotlight pole 2");
    rotateSpotlight = new TransformNode("Rotate spotlight", Mat4Transform.rotateAroundY(rotateSpotlightAngle));
    m = Mat4Transform.translate(2.2f, 6f, 0f);
    m = Mat4.multiply(m, Mat4Transform.rotateAroundZ(SPOTLIGHT_ROTATION_Z));
    TransformNode makeSpotlightPole2 = new TransformNode("Move pole 2 and rotate", m);
    TransformNode scaleSpotlightPole2 = new TransformNode("Scale spotlight pole 2", Mat4Transform.scale(5f, 0.4f, 0.4f));
    ModelNode spotlightPole2Node = new ModelNode("Spotlight Pole2 ", metal);

    spotlight.setPosition(new Vec3(spotlightLampBaseX, spotlightLampBaseY, spotlightLampBaseZ));

    spotlightRoot.addChild(spotlightPole);
     spotlightPole.addChild(makeSpotlightPole);
       makeSpotlightPole.addChild(scaleSpotlightPole);
         scaleSpotlightPole.addChild(spotlightPoleNode);
       makeSpotlightPole.addChild(spotlightPole2);
        spotlightPole2.addChild(rotateSpotlight);
          rotateSpotlight.addChild(makeSpotlightPole2);
            makeSpotlightPole2.addChild(scaleSpotlightPole2);
              scaleSpotlightPole2.addChild(spotlightPole2Node);
    spotlightRoot.update();
  }

  private void setupSnowmanSceneGraph() {
     snowmanRoot = new NameNode("snowman structure");

     //------------------Body----------------------

     translateX = new TransformNode("Translate body X", Mat4Transform.translate(xPosition,0,0));
     rotateAll = new TransformNode("Rotate body Z", Mat4Transform.rotateAroundZ(rotateAllAngle));
     Mat4 m = Mat4Transform.translate(0, BODY_DIAMETER / 2, 0);
     TransformNode positionBody = new TransformNode("Move body up to the floor", m);
     NameNode body = new NameNode("Body");
     m = Mat4Transform.scale(BODY_DIAMETER, BODY_DIAMETER, BODY_DIAMETER);
     TransformNode scaleBody = new TransformNode("Scale to body size", m);
     ModelNode bodyNode = new ModelNode("Body", snowball);

     //-------------------Buttons-------------------

     NameNode button2 = new NameNode("button2");
     m = Mat4Transform.scale(BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);
     m = Mat4.multiply(Mat4Transform.rotateAroundY(180), m); //Hide texture seam
     m = Mat4.multiply(Mat4Transform.translate(0, 0, BODY_DIAMETER / 2), m);
     TransformNode makeButton2 = new TransformNode("Scale, flip, move to body surface", m);
     ModelNode button2Node = new ModelNode("Button2", roughStone);

     NameNode button1 = new NameNode("button1");
     Mat4 mOddButton = Mat4.multiply(Mat4Transform.rotateAroundX(ODD_BUTTONS_ANGLE), m);
     TransformNode makeButton1 = new TransformNode("Scale, flip, move to body surface and rotate to button position", mOddButton);
     ModelNode button1Node = new ModelNode("Button1", roughStone);

     NameNode button3 = new NameNode("button3");
     mOddButton = Mat4.multiply(Mat4Transform.rotateAroundX(-ODD_BUTTONS_ANGLE), m);
     TransformNode makeButton3 = new TransformNode("Scale, flip, move to body surface and rotate to button position", mOddButton);
     ModelNode button3Node = new ModelNode("Button3", roughStone);

     //---------------------Head-----------------

     //Called "Roll" as it is the parent of headPosition
     rollHead = new TransformNode("Rotate head before translation", Mat4Transform.rotateAroundZ(rollHeadAngle));
     m = Mat4Transform.translate(0, (BODY_DIAMETER / 2) + (HEAD_DIAMETER / 2) + HEAD_HEIGHT_OFFSET, 0);
     TransformNode headPosition = new TransformNode("Move head to body surface", m);
     NameNode head = new NameNode("Head");
     m = Mat4Transform.scale(HEAD_DIAMETER, HEAD_DIAMETER, HEAD_DIAMETER);
     TransformNode scaleHead = new TransformNode("Scale to head size", m);
     ModelNode headNode = new ModelNode("Head", snowball);

     //-------------------Nose-------------------

     NameNode nose = new NameNode("Nose");
     m = Mat4Transform.scale(NOSE_SIZE, NOSE_SIZE, NOSE_LENGTH);
     m = Mat4.multiply(Mat4Transform.rotateAroundY(180), m); //Hide texture seam
     m = Mat4.multiply(Mat4Transform.translate(0, 0, HEAD_DIAMETER / 2), m);
     TransformNode makeNose = new TransformNode("Scale, flip, move to head surface, scale", m);
     ModelNode noseNode = new ModelNode("Nose", smoothStone);

     //-------------------Mouth-------------------
     NameNode mouth = new NameNode("Mouth");
     m = Mat4Transform.scale(NOSE_LENGTH, NOSE_SIZE, NOSE_SIZE);
     m = Mat4.multiply(Mat4Transform.rotateAroundY(180), m); //Hide texture seam
     m = Mat4.multiply(Mat4Transform.translate(0, 0, HEAD_DIAMETER / 2), m);
     m = Mat4.multiply(Mat4Transform.rotateAroundX(MOUTH_ANGLE), m);
     TransformNode makeMouth = new TransformNode("Scale, flip, move to head surface, scale, rotate to mouth position", m);
     ModelNode mouthNode = new ModelNode("Mouth", smoothStone);

     //-------------------Eyes-------------------

     NameNode leftEye = new NameNode("leftEye");
     m = Mat4Transform.scale(EYE_SIZE, EYE_SIZE, EYE_SIZE);
     m = Mat4.multiply(Mat4Transform.rotateAroundY(180), m); //Hide texture seam
     m = Mat4.multiply(Mat4Transform.translate(0, 0, HEAD_DIAMETER / 2), m);
     m = Mat4.multiply(Mat4Transform.rotateAroundX(EYE_ANGLE_X), m);
     Mat4 mLeft = Mat4.multiply(Mat4Transform.rotateAroundY(-EYE_ANGLE_Y), m);
     TransformNode makeLeftEye = new TransformNode("Scale, flip, move to head surface, rotate to left eye position", mLeft);
     ModelNode leftEyeNode = new ModelNode("LeftEye", roughStone);

     NameNode rightEye = new NameNode("rightEye");
     m = Mat4.multiply(Mat4Transform.rotateAroundY(EYE_ANGLE_Y), m);
     TransformNode makeRightEye = new TransformNode("Scale, flip, move to head surface, rotate to right eye position", m);
     ModelNode rightEyeNode = new ModelNode("RightEye", roughStone);

     //-------------------Top hat-------------------

     m = Mat4Transform.translate(0, (HEAD_DIAMETER / 2) + (TOP_HAT_MAIN_HEIGHT / 2) + TOP_HAT_MAIN_OFFSET, 0);
     TransformNode positionTopHatBody = new TransformNode("Move top hat to top of head", m);
     NameNode topHatBody = new NameNode("Top hat body");
     m = Mat4Transform.scale(TOP_HAT_MAIN_WIDTH, TOP_HAT_MAIN_HEIGHT, TOP_HAT_MAIN_WIDTH);
     TransformNode scaleTopHatBody = new TransformNode("Scale to top hat size", m);
     ModelNode topHatBodyNode = new ModelNode("topHatBody", topHatMain);

     NameNode topHatRim = new NameNode("topHatRim");
     m = Mat4Transform.scale(TOP_HAT_RIM_WIDTH, TOP_HAT_RIM_HEIGHT, TOP_HAT_RIM_WIDTH);
     m = Mat4.multiply(Mat4Transform.translate(0, -TOP_HAT_MAIN_HEIGHT / 2, 0), m);
     TransformNode makeTopHatRim = new TransformNode("Scale and move to bottom of top hat", m);
     ModelNode topHatRimNode = new ModelNode("topHatRim", topHatMain);

     NameNode topHatBand = new NameNode("topHatBand");
     m = Mat4Transform.scale(TOP_HAT_BAND_WIDTH, TOP_HAT_BAND_HEIGHT, TOP_HAT_BAND_WIDTH);
     m = Mat4.multiply(Mat4Transform.translate(0, (-TOP_HAT_MAIN_HEIGHT / 2) + (TOP_HAT_BAND_HEIGHT / 2), 0), m);
     TransformNode makeTopHatBand = new TransformNode("Scale and move to bottom of top hat", m);
     ModelNode topHatBandNode = new ModelNode("topHatBand", topHatRibbon);

     //-------------------------SCENE GRAPH------------------------------------

     snowmanRoot.addChild(translateX);
       translateX.addChild(rotateAll);
         rotateAll.addChild(positionBody);
           positionBody.addChild(body);
             body.addChild(scaleBody);
               scaleBody.addChild(bodyNode);
             body.addChild(button1);
               button1.addChild(makeButton1);
                 makeButton1.addChild(button1Node);
             body.addChild(button2);
               button2.addChild(makeButton2);
                 makeButton2.addChild(button2Node);
             body.addChild(button3);
               button3.addChild(makeButton3);
                 makeButton3.addChild(button3Node);

             body.addChild(rollHead);
               rollHead.addChild(headPosition);
                 headPosition.addChild(head);
                   head.addChild(scaleHead);
                     scaleHead.addChild(headNode);
                     head.addChild(nose);
                       nose.addChild(makeNose);
                         makeNose.addChild(noseNode);
                     head.addChild(mouth);
                       mouth.addChild(makeMouth);
                         makeMouth.addChild(mouthNode);
                     head.addChild(leftEye);
                       leftEye.addChild(makeLeftEye);
                         makeLeftEye.addChild(leftEyeNode);
                     head.addChild(rightEye);
                       rightEye.addChild(makeRightEye);
                         makeRightEye.addChild(rightEyeNode);
                     head.addChild(positionTopHatBody);
                       positionTopHatBody.addChild(topHatBody);
                         topHatBody.addChild(scaleTopHatBody);
                           scaleTopHatBody.addChild(topHatBodyNode);
                         topHatBody.addChild(topHatRim);
                           topHatRim.addChild(makeTopHatRim);
                             makeTopHatRim.addChild(topHatRimNode);
                         topHatBody.addChild(topHatBand);
                           topHatBand.addChild(makeTopHatBand);
                             makeTopHatBand.addChild(topHatBandNode);
     snowmanRoot.update();
  }

  private void render(GL3 gl) {
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

    mainLight.render(gl);
    spotlight.render(gl);
    floor.render(gl);
    background.render(gl);
    crate.render(gl);
    crate2.render(gl);

    snowmanRoot.draw(gl);
    spotlightRoot.draw(gl);

    if (currentAnimation != AnimationSelections.None) {
      handleSnowmanAnimations();
    }
    if (spotlightActive) {
      rotateSpotlight();
    }

    lastTime = getSeconds();
  }

  //--------------------------ANIMATIONS----------------------------------------

  //ANIMATION SELECTION---------
  private AnimationSelections currentAnimation = AnimationSelections.None;
  private AnimationSelections pendingAnimation = AnimationSelections.None;
  //ANIMATION LIMITS--------
  private static final float MAX_ROTATION_ALL_ANGLE = 20f;
  private static final float MAX_ROTATION_HEAD_ANGLE = 30f;
  private static final float MAX_SLIDE_POSITION = 1.75f;
  //ANIMATION SPEED------------
  private static final float MAXIMUM_ANIMATION_SPEED = 1.15f;
  private float currentAnimationSpeed = 0;
  private float lastAnimationSpeed = 0;
  //SLOWING / STOPPING / STARTING THE ANIMATION-----------
  private static final float ANIMATION_RAMP_UP_TIME = 6f; //The time it takes for the animation to reach full speed
  private static final float SLIDE_STOP_BOUNDS = 0.1f;
  private static final float ROTATION_STOP_BOUNDS = 0.34f;
  private boolean stoppingAnimation = false;
  private float animationSpeedAtTimeOfStop = -1;
  private boolean slowingDown = false;
  private float lastSinMagnitude = -1;
  //SPOTLIGHT ANIMATION------------
  private boolean spotlightActive = true;
  private static final float SPOTLIGHT_ROTATION_SPEED = 90f;

  private void handleSnowmanAnimations() {
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
        //If we have just started decreasing, towards 0
        if (Math.abs(sinMagnitude) > 0.99f && sinMagnitude < lastSinMagnitude) {
          slowingDown = true;
          animationSpeedAtTimeOfStop = currentAnimationSpeed;
        }
        if (slowingDown) {
          //If we use max, the speed could jump up if we weren't already at max speed
          currentAnimationSpeed = animationSpeedAtTimeOfStop * sinMagnitude;
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

  private void rotateSpotlight() {
    double deltaTime = getSeconds() - lastTime;
    rotateSpotlightAngle += SPOTLIGHT_ROTATION_SPEED * deltaTime;
    rotateSpotlight.setTransform(Mat4Transform.rotateAroundY(rotateSpotlightAngle));
    spotlightRoot.update();

    float xDir = (float)Math.sin(Math.toRadians(rotateSpotlightAngle + 90));
    float zDir = (float)Math.cos(Math.toRadians(rotateSpotlightAngle + 90));

    float yDir = -1 * Math.abs((float)Math.cos(Math.toRadians(SPOTLIGHT_ROTATION_Z)));
    float horizontalComponent = Math.abs((float)Math.sin(Math.toRadians(SPOTLIGHT_ROTATION_Z)));

    xDir = xDir * horizontalComponent;
    zDir = zDir * horizontalComponent;

    spotlight.setDirection(xDir, -1, zDir);
    rotateSpotlightLamp();
  }

  private void rotateSpotlightLamp() {
    double elapsedTime = getSeconds() - programStartTime;

    float x = spotlightLampBaseX - 3.75f*(float)(Math.sin(Math.toRadians(rotateSpotlightAngle - 90)));
    float y = spotlightLampBaseY;
    float z = spotlightLampBaseZ - 3.75f*(float)(Math.cos(Math.toRadians(rotateSpotlightAngle - 90)));

    spotlight.setPosition(x, y, z);
    spotlight.setRotation(0, rotateSpotlightAngle, SPOTLIGHT_ROTATION_Z);
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
              resetAnimations();
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
    //If we've reached the first maximum
    if (elapsedTime > 1.565f) {
      rotateAllAngle = MAX_ROTATION_ALL_ANGLE * (float)Math.sin(elapsedTime) * currentAnimationSpeed;
      float rotateAllXAngle = MAX_ROTATION_ALL_ANGLE * (float)Math.cos(elapsedTime) * currentAnimationSpeed;
      Mat4 m = Mat4Transform.rotateAroundZ(rotateAllAngle);
      m = Mat4.multiply(m, Mat4Transform.rotateAroundX(rotateAllXAngle));
      rotateAll.setTransform(m);
    } else {
      float rotateAllAngle = MAX_ROTATION_ALL_ANGLE * (float)Math.sin(elapsedTime) * currentAnimationSpeed;
      rotateAll.setTransform(Mat4Transform.rotateAroundZ(rotateAllAngle));
    }

  }

  private void roll() {
    rollHeadAngle = MAX_ROTATION_HEAD_ANGLE * (float)Math.sin(elapsedTime) * currentAnimationSpeed;
    Mat4 m = Mat4Transform.rotateAroundZ(rollHeadAngle);
    rollHead.setTransform(m);

    //If we're stopping the animation
    if (stoppingAnimation && slowingDown) {
      //If the current rotation is within the stop bounds
      if (rollHeadAngle > rollHeadAngleStart - ROTATION_STOP_BOUNDS && rollHeadAngle < rollHeadAngleStart + ROTATION_STOP_BOUNDS) {
        rollHead.setTransform(Mat4Transform.rotateAroundZ(rollHeadAngleStart));
        switch(currentAnimation) {
          case Roll :
            resetAnimations();
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
    //Multiply by -1 so that when we slide, rock and roll..
    //We rock and roll in the same direction as the slide, looks a bit more believable
    xPosition = MAX_SLIDE_POSITION * (float)Math.sin(elapsedTime) * currentAnimationSpeed * -1;
    translateX.setTransform(Mat4Transform.translate(xPosition,0,0));

    //If we're stopping the animation
    if (stoppingAnimation && slowingDown) {
      //If the current rotation is within the stop bounds
      if (xPosition > xPositionStart - SLIDE_STOP_BOUNDS && xPosition < xPositionStart + SLIDE_STOP_BOUNDS) {
        switch(currentAnimation) {
          case Slide :
            resetAnimations();
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

  public void resetAnimations() {
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

  //------------------------------------TIME-----------------------------------

  private double elapsedTime;
  private double programStartTime;
  private double lastTime;
  private double animationStartTime = -1;

  private double getSeconds() {
    return System.currentTimeMillis()/1000.0;
  }

}
