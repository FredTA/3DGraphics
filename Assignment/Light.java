import gmaths.*;
import java.nio.*;
import com.jogamp.common.nio.*;
import com.jogamp.opengl.*;

public class Light {

  private Material material;
  private Vec3 position;
  private Vec3 direction;
  private Mat4 model;
  private Shader shader;
  private Camera camera;

  private Vec3 originalAmbient;
  private Vec3 originalDiffuse;
  private Vec3 originalSpecular;

  private float cutoff;
  private float outerCutoff;

  private float intensity = 1;
  private static final float INTENSITY_STEP = 0.25f;

  public Light(GL3 gl) {
    material = new Material();

    originalAmbient = new Vec3(0.5f, 0.5f, 0.5f);
    originalDiffuse = new Vec3(0.8f, 0.8f, 0.8f);
    originalSpecular = new Vec3(0.8f, 0.8f, 0.8f);

    material.setAmbient(originalAmbient);
    material.setDiffuse(originalDiffuse);
    material.setSpecular(originalSpecular);

    position = new Vec3(3f,2f,1f);
    model = new Mat4(1);
    shader = new Shader(gl, "vs_light.txt", "fs_light.txt");
    fillBuffers(gl);
  }

  public Light(GL3 gl, Vec3 ambient, Vec3 diffuse, Vec3 specular) {
    material = new Material();

    originalAmbient = ambient;
    originalDiffuse = diffuse;
    originalSpecular = specular;

    material.setAmbient(originalAmbient);
    material.setDiffuse(originalDiffuse);
    material.setSpecular(originalSpecular);

    position = new Vec3(3f,2f,1f);
    model = new Mat4(1);
    shader = new Shader(gl, "vs_light.txt", "fs_light.txt");
    fillBuffers(gl);
  }

    public Light(GL3 gl, Vec3 ambient, Vec3 diffuse, Vec3 specular, float cutoff, float outerCutoff) {
      this(gl, ambient, diffuse, specular);
      this.cutoff = cutoff;
      this.outerCutoff = outerCutoff;
      direction = new Vec3(0, -1, 0);
    }

  public void decreaseLightIntensity() {
    if (intensity > 0) {
      intensity -= INTENSITY_STEP;
      changeMaterialColourIntensities();
    }
  }

  public void increaseLightIntensity() {
    if (intensity < 1) {
      intensity += INTENSITY_STEP;
      changeMaterialColourIntensities();
    }
  }

  public void toggle() {
    if (intensity == 1) {
      intensity = 0;
    } else {
      intensity = 1;
    }
    changeMaterialColourIntensities();
  }

  public void changeMaterialColourIntensities() {
    Vec3 newAmbient = new Vec3(originalAmbient);
    Vec3 newDiffuse = new Vec3(originalDiffuse);
    Vec3 newSpecular = new Vec3(originalSpecular);

    newAmbient.multiply(intensity);
    newDiffuse.multiply(intensity);
    newSpecular.multiply(intensity);

    material.setAmbient(newAmbient);
    material.setDiffuse(newDiffuse);
    material.setSpecular(newSpecular);
  }

  public void setPosition(Vec3 v) {
    position.x = v.x;
    position.y = v.y;
    position.z = v.z;
  }

  public void setPosition(float x, float y, float z) {
    position.x = x;
    position.y = y;
    position.z = z;
  }

  public void setDirection(float x, float y, float z) {
    direction.x = x;
    direction.y = y;
    direction.z = z;
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

  public void setMaterial(Material m) {
    material = m;
  }

  public Material getMaterial() {
    return material;
  }

  public void setCamera(Camera camera) {
    this.camera = camera;
  }

  public void render(GL3 gl) {
    Mat4 model = new Mat4(1);
    model = Mat4.multiply(Mat4Transform.scale(0.3f,0.3f,0.3f), model);
    model = Mat4.multiply(Mat4Transform.translate(position), model);

    Mat4 mvpMatrix = Mat4.multiply(camera.getPerspectiveMatrix(), Mat4.multiply(camera.getViewMatrix(), model));

    shader.use(gl);
    shader.setFloatArray(gl, "mvpMatrix", mvpMatrix.toFloatArrayForGLSL());

    gl.glBindVertexArray(vertexArrayId[0]);
    gl.glDrawElements(GL.GL_TRIANGLES, indices.length, GL.GL_UNSIGNED_INT, 0);
    gl.glBindVertexArray(0);
  }

  public void dispose(GL3 gl) {
    gl.glDeleteBuffers(1, vertexBufferId, 0);
    gl.glDeleteVertexArrays(1, vertexArrayId, 0);
    gl.glDeleteBuffers(1, elementBufferId, 0);
  }

    // ***************************************************
  /* THE DATA
   */
  // anticlockwise/counterclockwise ordering

    private float[] vertices = new float[] {  // x,y,z
      -0.5f, -0.5f, -0.5f,  // 0
      -0.5f, -0.5f,  0.5f,  // 1
      -0.5f,  0.5f, -0.5f,  // 2
      -0.5f,  0.5f,  0.5f,  // 3
       0.5f, -0.5f, -0.5f,  // 4
       0.5f, -0.5f,  0.5f,  // 5
       0.5f,  0.5f, -0.5f,  // 6
       0.5f,  0.5f,  0.5f   // 7
     };

    private int[] indices =  new int[] {
      0,1,3, // x -ve
      3,2,0, // x -ve
      4,6,7, // x +ve
      7,5,4, // x +ve
      1,5,7, // z +ve
      7,3,1, // z +ve
      6,4,0, // z -ve
      0,2,6, // z -ve
      0,4,5, // y -ve
      5,1,0, // y -ve
      2,3,7, // y +ve
      7,6,2  // y +ve
    };

  private int vertexStride = 3;
  private int vertexXYZFloats = 3;

  // ***************************************************
  /* THE LIGHT BUFFERS
   */

  private int[] vertexBufferId = new int[1];
  private int[] vertexArrayId = new int[1];
  private int[] elementBufferId = new int[1];

  private void fillBuffers(GL3 gl) {
    gl.glGenVertexArrays(1, vertexArrayId, 0);
    gl.glBindVertexArray(vertexArrayId[0]);
    gl.glGenBuffers(1, vertexBufferId, 0);
    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexBufferId[0]);
    FloatBuffer fb = Buffers.newDirectFloatBuffer(vertices);

    gl.glBufferData(GL.GL_ARRAY_BUFFER, Float.BYTES * vertices.length, fb, GL.GL_STATIC_DRAW);

    int stride = vertexStride;
    int numXYZFloats = vertexXYZFloats;
    int offset = 0;
    gl.glVertexAttribPointer(0, numXYZFloats, GL.GL_FLOAT, false, stride*Float.BYTES, offset);
    gl.glEnableVertexAttribArray(0);

    gl.glGenBuffers(1, elementBufferId, 0);
    IntBuffer ib = Buffers.newDirectIntBuffer(indices);
    gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, elementBufferId[0]);
    gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, Integer.BYTES * indices.length, ib, GL.GL_STATIC_DRAW);
    gl.glBindVertexArray(0);
  }

}
