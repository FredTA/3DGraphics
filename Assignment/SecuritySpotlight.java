import gmaths.*;
import java.nio.*;
import com.jogamp.common.nio.*;
import com.jogamp.opengl.*;

public class SecuritySpotlight {

  private Spotlight spotlight;

  private SGNode spotlightRoot;
  private Model metal;

  private boolean spotlightActive = true;
  private TransformNode rotateSpotlight;
  private float rotateSpotlightAngle = 0;

  private float spotlightLampBaseX = -9.5f;
  private float spotlightLampBaseY = 13f;
  private float spotlightLampBaseZ = 0;
  private static float SPOTLIGHT_ROTATION_Z = 40f;
  private static final float SPOTLIGHT_ROTATION_SPEED = 90f;

  public SecuritySpotlight(GL3 gl, Camera camera, Light mainLight, Spotlight spotlight){
   this.spotlight = spotlight;
   setupModels(gl, camera, mainLight);
   setupSpotlightSceneGraph();

   programStartTime = getSeconds();
   lastTime = getSeconds();
  }

  private void setupModels(GL3 gl, Camera camera, Light mainLight) {
    int[] spotlightTexture = TextureLibrary.loadTexture(gl, "textures/metal.jpg");

    Mesh mesh = new Mesh(gl, Cube.vertices.clone(), Cube.indices.clone());
    Shader shader = new Shader(gl, "vs_main.txt", "fs_main.txt");
    Material material = new Material(new Vec3(0.8f, 0.8f, 0.9f), new Vec3(0.8f, 0.8f, 0.9f), new Vec3(0.9f, 0.9f, 0.9f), 32.0f);
    Mat4 modelMatrix = new Mat4(1);
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

  public void toggle(){
    spotlight.toggle();
    spotlightActive = !spotlightActive;
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
    float x = spotlightLampBaseX - 3.75f*(float)(Math.sin(Math.toRadians(rotateSpotlightAngle - 90)));
    float y = spotlightLampBaseY;
    float z = spotlightLampBaseZ - 3.75f*(float)(Math.cos(Math.toRadians(rotateSpotlightAngle - 90)));

    spotlight.setPosition(x, y, z);
    spotlight.setRotation(0, rotateSpotlightAngle, SPOTLIGHT_ROTATION_Z);
  }

   //TIME------
  private double lastTime;
  private double programStartTime;

  private double getSeconds() {
    return System.currentTimeMillis()/1000.0;
  }


  public void draw(GL3 gl){
    if (spotlightActive) {
      rotateSpotlight();
    }
    lastTime = getSeconds();

    spotlightRoot.draw(gl);
  }

  /* Clean up memory, if necessary */
  public void dispose(GLAutoDrawable drawable) {
    GL3 gl = drawable.getGL().getGL3();
    metal.dispose(gl);
  }

}
