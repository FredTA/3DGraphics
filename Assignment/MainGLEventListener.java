import gmaths.*;

import java.nio.*;
import com.jogamp.common.nio.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.awt.*;
import com.jogamp.opengl.util.glsl.*;

public class MainGLEventListener implements GLEventListener {

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
    spotlight.dispose(gl);
    floor.dispose(gl);
    background.dispose(gl);
    crate.dispose(gl);
    crate2.dispose(gl);
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
    securitySpotlight.toggle();
  }

  // ----------------------THE SCENE------------------------------

  private Camera camera;
  private Mat4 perspective;
  private Model floor, crate, crate2;
  private AnimatedModel background;
  private Light mainLight;
  private Spotlight spotlight;

  private Snowman snowman;
  private SecuritySpotlight securitySpotlight;


  //LIGHTS--------
  private static final float MAIN_LIGHT_X = 6.1f;
  private static final float MAIN_LIGHT_Y = 24.0f;
  private static final float MAIN_LIGHT_Z = 18.0f;
  private static float SPOTLIGHT_INNER_CUTTOFF = 28f;
  private static float SPOTLIGHT_OUTER_CUTOFF = 30.5f;

  private void initialise(GL3 gl) {
    setupLights(gl);
    setupModels(gl);

    //These are the two main parts of the scene, so they are separated out into different classes
    snowman = new Snowman(gl, camera, mainLight, spotlight);
    securitySpotlight = new SecuritySpotlight(gl, camera, mainLight, spotlight);
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
  }

  private void render(GL3 gl) {
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

    floor.render(gl);
    background.render(gl);
    crate.render(gl);
    crate2.render(gl);

    snowman.draw(gl);
    securitySpotlight.draw(gl);

    mainLight.render(gl);
    spotlight.render(gl);
  }
}
