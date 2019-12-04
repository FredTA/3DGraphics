import gmaths.*;

import java.nio.*;
import com.jogamp.common.nio.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.awt.*;
import com.jogamp.opengl.util.glsl.*;

public class MainGLEventListener implements GLEventListener {

  private static final boolean DISPLAY_SHADERS = false;

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
    //snowball.dispose(gl);
  }

  //---------------------------INTERACTION----------------------------------

  public void selectSnowmanAnimation(AnimationSelections newAnimationSelection) {
    snowman.selectAnimation(newAnimationSelection);
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
  private SGNode spotlightRoot;

  private TransformNode rotateSpotlight;


  private float rotateSpotlightAngleStart = 0, rotateSpotlightAngle = rotateSpotlightAngleStart;

  private Snowman snowman;


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

    snowman = new Snowman(gl, camera, mainLight, spotlight);

    setupSpotlightSceneGraph();
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

  private void render(GL3 gl) {
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

    mainLight.render(gl);
    spotlight.render(gl);
    floor.render(gl);
    background.render(gl);
    crate.render(gl);
    crate2.render(gl);

    snowman.draw(gl);
    spotlightRoot.draw(gl);

    if (spotlightActive) {
      rotateSpotlight();
    }

    lastTime = getSeconds();
  }

  //--------------------------ANIMATIONS----------------------------------------

  //SPOTLIGHT ANIMATION------------
  private boolean spotlightActive = true;
  private static final float SPOTLIGHT_ROTATION_SPEED = 90f;

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
  //------------------------------------TIME-----------------------------------

  private double elapsedTime;
  private double programStartTime;
  private double lastTime;
  private double animationStartTime = -1;

  private double getSeconds() {
    return System.currentTimeMillis()/1000.0;
  }

}
