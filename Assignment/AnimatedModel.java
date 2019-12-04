import gmaths.*;
import java.nio.*;
import com.jogamp.common.nio.*;
import com.jogamp.opengl.*;

public class AnimatedModel extends Model {

  //Animation variables
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

  public AnimatedModel(GL3 gl, Camera camera, Light light, Spotlight spotlight, Shader shader, Material material, Mat4 modelMatrix, Mesh mesh, int[] textureId1, int[] textureId2) {
    super(gl, camera, light, spotlight, shader, material, modelMatrix, mesh, textureId1, textureId2);
    lastTime = timeOfLastAnimationChange = System.currentTimeMillis()/1000.0;
  }

  @Override
  public void render(GL3 gl, Mat4 modelMatrix) {
    setupShaders(gl, modelMatrix);

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

    mesh.render(gl);
  }

}
