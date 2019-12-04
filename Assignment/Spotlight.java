import gmaths.*;
import java.nio.*;
import com.jogamp.common.nio.*;
import com.jogamp.opengl.*;

public class Spotlight extends Light {

  private Vec3 direction;
  private Vec3 rotation = new Vec3(1, 1, 1);
  private float cutoff;
  private float outerCutoff;

  public Spotlight(GL3 gl, Vec3 ambient, Vec3 diffuse, Vec3 specular, float cutoff, float outerCutoff) {
    super(gl, ambient, diffuse, specular);
    this.cutoff = cutoff;
    this.outerCutoff = outerCutoff;
    direction = new Vec3(0, -1, 0);
  }

  public void toggle() {
    if (intensity == 1) {
      intensity = 0;
    } else {
      intensity = 1;
    }
    changeMaterialColourIntensities();
  }

  public void setDirection(float x, float y, float z) {
    direction.x = x;
    direction.y = y;
    direction.z = z;
  }

  public void setRotation(float x, float y, float z) {
    rotation.x = x;
    rotation.y = y;
    rotation.z = z;
  }

  public Vec3 getDirection(){
    return direction;
  }

  public Vec3 getPosition() {
    return position;
  }

  public float getCutoff(){
    return cutoff;
  }

  public float getOuterCutoff(){
    return outerCutoff;
  }

  @Override
  public void render(GL3 gl) {
    Mat4 model = new Mat4(1);
    model = Mat4.multiply(Mat4Transform.scale(0.6f,0.1f,0.6f), model);
    model = Mat4.multiply(Mat4Transform.rotateAroundZ(rotation.z), model);
    model = Mat4.multiply(Mat4Transform.rotateAroundY(rotation.y), model);
    model = Mat4.multiply(Mat4Transform.translate(position), model);
    renderModel(gl, model);
  }

}
