import gmaths.*;
import java.nio.*;
import com.jogamp.common.nio.*;
import com.jogamp.opengl.*;

public class Model {

  private Mesh mesh;
  private int[] textureId1;
  private int[] textureId2;
  private Material material;
  private Shader shader;
  private Mat4 modelMatrix;
  private Camera camera;
  private Light light, spotlight;


  //Animation variables
  private boolean secondTextureIsAnimated = false;
  private float lastAnimationSpeed = 0.01f;
  private float targetAnimationSpeed;
  private float lastToTargetAnimationDifference;
  private double timeOfLastAnimationChange = 0;
  private float animationSpeedX;
  private static final float MAX_ANIMATION_SPEED_CHANGE = 0.02f;
  private static final float BASE_ANIMATION_SPEED_X = 0.022f;
  private static final double ANIMATION_SPEED_CHANGE_INTERVAL = 3.5f;
  private static final float MAX_ANIMATION_SPEED_X = BASE_ANIMATION_SPEED_X + MAX_ANIMATION_SPEED_CHANGE;
  private static final float MAX_ANIMATION_SPEED_Y = 0.025f;
  private static final float MIN_ANIMATION_SPEED_Y = 0.015f;
  private float offsetX;
  private float offsetY;
  private double lastTime;

  public Model(GL3 gl, Camera camera, Light light, Light spotlight, Shader shader, Material material, Mat4 modelMatrix, Mesh mesh, int[] textureId1, int[] textureId2, boolean animate) {
    this.mesh = mesh;
    this.material = material;
    this.modelMatrix = modelMatrix;
    this.shader = shader;
    this.camera = camera;
    this.light = light;
    this.spotlight = spotlight;
    this.textureId1 = textureId1;
    this.textureId2 = textureId2;
    secondTextureIsAnimated = animate;
    lastTime = timeOfLastAnimationChange = System.currentTimeMillis()/1000.0;
  }

  public Model(GL3 gl, Camera camera, Light light, Light spotlight, Shader shader, Material material, Mat4 modelMatrix, Mesh mesh, int[] textureId1) {
    this(gl, camera, light, spotlight, shader, material, modelMatrix, mesh, textureId1, null, false);
  }

  public Model(GL3 gl, Camera camera, Light light, Light spotlight, Shader shader, Material material, Mat4 modelMatrix, Mesh mesh) {
    this(gl, camera, light, spotlight, shader, material, modelMatrix, mesh, null, null, false);
  }

  public void setModelMatrix(Mat4 m) {
    modelMatrix = m;
  }

  public void setCamera(Camera camera) {
    this.camera = camera;
  }

  public void setLight(Light light) {
    this.light = light;
  }

  public void render(GL3 gl, Mat4 modelMatrix) {
    Mat4 mvpMatrix = Mat4.multiply(camera.getPerspectiveMatrix(), Mat4.multiply(camera.getViewMatrix(), modelMatrix));
    shader.use(gl);
    shader.setFloatArray(gl, "model", modelMatrix.toFloatArrayForGLSL());
    shader.setFloatArray(gl, "mvpMatrix", mvpMatrix.toFloatArrayForGLSL());

    shader.setVec3(gl, "viewPos", camera.getPosition());

    shader.setVec3(gl, "light.position", light.getPosition());
    shader.setVec3(gl, "light.ambient", light.getMaterial().getAmbient());
    shader.setVec3(gl, "light.diffuse", light.getMaterial().getDiffuse());
    shader.setVec3(gl, "light.specular", light.getMaterial().getSpecular());

    shader.setVec3(gl, "spotlight.position", spotlight.getPosition());
    shader.setVec3(gl, "spotlight.ambient", spotlight.getMaterial().getAmbient());
    shader.setVec3(gl, "spotlight.diffuse", spotlight.getMaterial().getDiffuse());
    shader.setVec3(gl, "spotlight.specular", spotlight.getMaterial().getSpecular());

    shader.setVec3(gl, "material.ambient", material.getAmbient());
    shader.setVec3(gl, "material.diffuse", material.getDiffuse());
    shader.setVec3(gl, "material.specular", material.getSpecular());
    shader.setFloat(gl, "material.shininess", material.getShininess());

    if (textureId1!=null) {
      shader.setInt(gl, "first_texture", 0);  // be careful to match these with GL_TEXTURE0 and GL_TEXTURE1
      gl.glActiveTexture(GL.GL_TEXTURE0);
      gl.glBindTexture(GL.GL_TEXTURE_2D, textureId1[0]);
    }
    if (textureId2!=null) {
      shader.setInt(gl, "second_texture", 1);
      gl.glActiveTexture(GL.GL_TEXTURE1);
      gl.glBindTexture(GL.GL_TEXTURE_2D, textureId2[0]);


      if (secondTextureIsAnimated) {
        double currentTime = System.currentTimeMillis()/1000.0;

        if(currentTime > timeOfLastAnimationChange + ANIMATION_SPEED_CHANGE_INTERVAL) {
          lastAnimationSpeed = animationSpeedX;
          //set the speed change target to +- the max speed change
          targetAnimationSpeed = BASE_ANIMATION_SPEED_X + ((float)Math.random() * MAX_ANIMATION_SPEED_CHANGE * 2) - MAX_ANIMATION_SPEED_CHANGE;
          timeOfLastAnimationChange = currentTime;
          lastToTargetAnimationDifference = targetAnimationSpeed - animationSpeedX;
        }

        float progressToSpeedTarget = (float)((currentTime - timeOfLastAnimationChange) / ANIMATION_SPEED_CHANGE_INTERVAL);

        animationSpeedX = lastAnimationSpeed + (lastToTargetAnimationDifference * progressToSpeedTarget);

        //Make the animation speed y inversly proportional to x
        float animationSpeedY = MAX_ANIMATION_SPEED_Y * (1 - (animationSpeedX / MAX_ANIMATION_SPEED_X));

        if (animationSpeedY < MIN_ANIMATION_SPEED_Y) {
          animationSpeedY = MIN_ANIMATION_SPEED_Y;
        }

        double deltaTime = currentTime - lastTime;
        offsetX += (animationSpeedX * (float)deltaTime);
        offsetY += (animationSpeedY * (float)deltaTime);

        shader.setFloat(gl, "offset", offsetX, offsetY);
        lastTime = currentTime;
      }
    }
    //System.out.println("MMM yes we are model code");
    mesh.render(gl);
  }

  public void render(GL3 gl) {
    render(gl, modelMatrix);
  }

  public void dispose(GL3 gl) {
    mesh.dispose(gl);
    if (textureId1!=null) gl.glDeleteBuffers(1, textureId1, 0);
    if (textureId2!=null) gl.glDeleteBuffers(1, textureId2, 0);
  }

}
