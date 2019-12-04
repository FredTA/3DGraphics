import gmaths.*;
import java.nio.*;
import com.jogamp.common.nio.*;
import com.jogamp.opengl.*;

public class Snowman {

  private SGNode snowmanRoot;

  private Model snowball, smoothStone, roughStone, topHatMain, topHatRibbon;

  private TransformNode initialBodyRotation;
  private TransformNode initialHeadRotation;
  private TransformNode initialBodyPosition;

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

  private TransformNode translateX, rotateAll, translateHead, rollHead;
  private float xPositionStart = 0, xPosition = xPositionStart;
  private Vec3 headPosition, headPositionStart;
  private float rotateAllAngleStart = 0, rotateAllAngle = rotateAllAngleStart;
  private float rollHeadAngleStart = 0, rollHeadAngle = rollHeadAngleStart;

  public Snowman(GL3 gl, Camera camera, Light mainLight, Spotlight spotlight){
   setupModels(gl, camera, mainLight, spotlight);
   setupSnowmanSceneGraph();
  }

  private void setupModels(GL3 gl, Camera camera, Light mainLight, Spotlight spotlight) {
    int[] snowTexture = TextureLibrary.loadTexture(gl, "textures/snow.jpg");
    int[] stoneRoughTexture = TextureLibrary.loadTexture(gl, "textures/stone.jpg");
    int[] stoneSmoothTexture = TextureLibrary.loadTexture(gl, "textures/stoneSmooth.jpg");
    int[] topHatMainTexture = TextureLibrary.loadTexture(gl, "textures/hatMain.jpg");
    int[] topHatBandTexture = TextureLibrary.loadTexture(gl, "textures/ribbon.jpg");

    //------------Body & Head--------------

    Mesh mesh = new Mesh(gl, Sphere.vertices.clone(), Sphere.indices.clone());
    Shader shader = new Shader(gl, "vs_main.txt", "fs_main.txt");
    Material material = new Material(new Vec3(0.7f, 0.7f, 0.7f), new Vec3(0.7f, 0.7f, 0.7f), new Vec3(0.2f, 0.2f, 0.2f), 32.0f);
    Mat4 modelMatrix = new Mat4(1);
    snowball = new Model(gl, camera, mainLight, spotlight, shader, material, modelMatrix, mesh, snowTexture);

    //------------Nose & Mouth---------------

    //Smoth, polished stone, so we should have a greater specular
    material = new Material(new Vec3(0.7f, 0.7f, 0.7f), new Vec3(0.7f, 0.7f, 0.7f), new Vec3(0.7f, 0.7f, 0.7f), 32.0f);
    smoothStone = new Model(gl, camera, mainLight, spotlight, shader, material, modelMatrix, mesh, stoneSmoothTexture);

    //------------Eyes and buttons---------------

    //Rough stone, so less specular
    material = new Material(new Vec3(0.85f, 0.85f, 0.85f), new Vec3(0.7f, 0.7f, 0.7f), new Vec3(0, 0, 0), 32.0f);
    roughStone = new Model(gl, camera, mainLight, spotlight, shader, material, modelMatrix, mesh, stoneRoughTexture);

    //------------Top hat cylinders

    mesh = new Mesh(gl, Cube.vertices.clone(), Cube.indices.clone());
    shader = new Shader(gl, "vs_main.txt", "fs_main.txt");
    //Top hat material should have little specular
    material = new Material(new Vec3(1, 1, 1), new Vec3(1, 1, 1), new Vec3(0.1f, 0.1f, 0.1f), 32.0f);
    topHatMain = new Model(gl, camera, mainLight, spotlight, shader, material, modelMatrix, mesh, topHatMainTexture);

    //Top hat ribon should have lots of specular
    material = new Material(new Vec3(0.5f, 0.5f, 0.5f), new Vec3(0.7f, 0.7f, 0.7f), new Vec3(0.9f, 0.9f, 0.9f), 32.0f);
    topHatRibbon = new Model(gl, camera, mainLight, spotlight, shader, material, modelMatrix, mesh, topHatBandTexture);
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

  public void draw(GL3 gl){
    if (currentAnimation != AnimationSelections.None) {
      animate();
    }

    snowmanRoot.draw(gl);
  }

  //---------------------------ANIMATIONS------------------------------------

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

  private void animate() {
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

  //TODO do we need these?
  private double elapsedTime;
  private double animationStartTime = -1;

  private double getSeconds() {
    return System.currentTimeMillis()/1000.0;
  }

  /* Clean up memory, if necessary */
  public void dispose(GLAutoDrawable drawable) {
    GL3 gl = drawable.getGL().getGL3();
    snowball.dispose(gl);
    smoothStone.dispose(gl);
    roughStone.dispose(gl);
    topHatMain.dispose(gl);
    topHatRibbon.dispose(gl);
  }

}
