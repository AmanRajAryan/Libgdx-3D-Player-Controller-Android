package aman.three;

import static java.lang.Math.abs;
import static java.lang.Math.atan2;


import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

public class MyGame extends ApplicationAdapter
        implements AnimationController.AnimationListener, InputProcessor {
    private SceneManager sceneManager;
    private SceneAsset sceneAsset;
    private Scene playerScene;
    private PerspectiveCamera camera;
    private Cubemap diffuseCubemap;
    private Cubemap environmentCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private float time;
    private SceneSkybox skybox;
    private DirectionalLightEx light;

    // Player Movement
    float speed = 5f;
    float rotationSpeed = 80f;
    private final Matrix4 playerTransform = new Matrix4();
    private final Vector3 moveTranslation = new Vector3();
    private final Vector3 currentPosition = new Vector3();
    boolean sprinting = false;

    // Camera
    private float camPitch = Settings.CAMERA_START_PITCH;
    private float distanceFromPlayer = 20f;
    private float angleAroundPlayer = 0f;
    private float angleBehindPlayer = 0f;
    

    // touchpad
    Stage stage;
    TouchPad touchpad;
    Batch batch;
    
    
    
    
    boolean inTouchpad; //Erkka: if the touch is in the touchpad, false otherwise
    int deltaX; //Erkka: a helper variable to store how much the touch has been dragged sideways
    float touchpadX; //Erkka: two more helper functions to store the touchpad knob position
    float touchpadY;
    float touchpadAngle; //Erkka: another helper function to store the touchpad angle. 0 for east, 90 for the north, so it is counter-clockwise


    @Override
    public void create() {
        // Create Sprite and a Stage
        batch = new SpriteBatch();
        stage = new Stage(new ScreenViewport(), batch);

        // sprint button
        Texture sprintBtnTexture = new Texture(Gdx.files.internal("sprint.png"));
        ImageButton.ImageButtonStyle sprintBtnStyle = new ImageButton.ImageButtonStyle();
        sprintBtnStyle.up = new TextureRegionDrawable(sprintBtnTexture);
        ImageButton sprintBtn = new ImageButton(sprintBtnStyle);
        sprintBtn.setWidth(150f);
        sprintBtn.setHeight(150f);
        sprintBtn.setPosition(100f, 300f);

        sprintBtn.addListener(
                new InputListener() {
                    @Override
                    public void touchUp(
                            InputEvent event, float x, float y, int pointer, int button) {}

                    @Override
                    public boolean touchDown(
                            InputEvent event, float x, float y, int pointer, int button) {
                        if (sprinting) {
                            sprinting = false;
                        } else {
                            sprinting = true;
                          //  playerRotationToFaceInCameraDirectionWhenSprinted();
                            
                        
                        }

                        return true;
                    }
                });
        stage.addActor(sprintBtn);

        // create touchPad
        touchpad = new TouchPad(stage, sprintBtn);

        // create scene
        sceneAsset = new GLBLoader().load(Gdx.files.internal("models/shiba.glb"));
        playerScene = new Scene(sceneAsset.scene);
        sceneManager = new SceneManager();
        playerScene.modelInstance.transform.scale(3, 3, 3);
        playerScene.modelInstance.transform.rotate(Vector3.Y, 180f);
        sceneManager.addScene(playerScene);

        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 1f;
        camera.far = 200;
        sceneManager.setCamera(camera);
        camera.position.set(0, 5f, 4f);

        // Gdx.input.setCursorCatched(true);
        // Gdx.input.setInputProcessor(this);


        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(stage);
        Gdx.input.setInputProcessor(inputMultiplexer);

        // setup light
        light = new DirectionalLightEx();
        light.direction.set(1, -3, 1).nor();
        light.color.set(Color.WHITE);
        sceneManager.environment.add(light);

        // setup quick IBL (image based lighting)
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
        environmentCubemap = iblBuilder.buildEnvMap(1024);
        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);
        iblBuilder.dispose();

        // This texture is provided by the library, no need to have it in your assets.
        brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

        sceneManager.setAmbientLight(1f);
        sceneManager.environment.set(
                new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

        // setup skybox
        skybox = new SceneSkybox(environmentCubemap);
        sceneManager.setSkyBox(skybox);

        buildBoxes();
    }

    @Override
    public void resize(int width, int height) {
        sceneManager.updateViewport(width, height);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        time += deltaTime;

        processInput(deltaTime);
        updateCamera();


        
        // render
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        sceneManager.update(deltaTime);
        sceneManager.render();

        stage.act(deltaTime);
        stage.draw();
    }

    private void processInput(float deltaTime) {
        //here we handle all the user input
        //usually it helps to process all the input in one place,
        //so avoid things like "Gdx.input.getDeltaX()" outside this function!
        //that way when you need to adjust anything with the control logic
        //you know you'll always find all the input handling here

        // Update the player transform
        playerTransform.set(playerScene.modelInstance.transform);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            moveTranslation.z += speed * deltaTime;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            moveTranslation.z -= speed * deltaTime;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            playerTransform.rotate(Vector3.Y, rotationSpeed * deltaTime);
            angleBehindPlayer += rotationSpeed * deltaTime;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            playerTransform.rotate(Vector3.Y, -rotationSpeed * deltaTime);
            angleBehindPlayer -= rotationSpeed * deltaTime;
        }

        if (touchpad.getTouchpad().isTouched()) {

            //here we handle the touchpad
            inTouchpad = true;
            sprinting = false;

            touchpadX = touchpad.getTouchpad().getKnobPercentX();
            touchpadY = touchpad.getTouchpad().getKnobPercentY();

            if ((touchpadX != 0) || (touchpadY != 0)) {
                //Erkka: atan2() function returns the angle in radians, so we convert it to degrees by *180/pi
                touchpadAngle = (float) (atan2(touchpadY, touchpadX) * 180.0d / Math.PI);

                //we now have the absolute toucpadAngle, ie. 0 is always to the east, 90 always to the right
                //but the camera might be rotated, and we probably want knob to the right moving the character to the right of the screen, not to the absolute east
                //luckily, we have the camera facing in angleAroundPlayer
                //but angleAroundPlayer has 0 to north, and seems to be clock-wise
                //so we need to do some computation to make angleAroundPlayer compatible
                //with the angle we calculated for the knob

                float convertedAngle = (360-(angleAroundPlayer-90));
                while (convertedAngle < 0)
                    convertedAngle +=360;

                touchpadAngle-=convertedAngle;

                rotatePlayerInGivenDirection(null, touchpadAngle);
                moveTranslation.z += speed * deltaTime;
            }

        }
        else inTouchpad = false;

        if (!inTouchpad) {

           

            //Erkka: here we read the touch dragged horizontally, outside the touchpad
            deltaX = Gdx.input.getDeltaX();
            if (deltaX != 0) {
                if (sprinting) {
                    rotatePlayer(-deltaX);
                }

                //if in sprint mode, we rotate both the player and the camera
                //if not in sprint mode, we rotate only the camera
                rotateCamera(-deltaX);

            }
        }

        if (sprinting) {
            moveTranslation.z += 5f * deltaTime;
        }

        

        // Apply the move translation to the transform
        playerTransform.translate(moveTranslation);

        // Set the modified transform
        playerScene.modelInstance.transform.set(playerTransform);

        // Update vector position
        playerScene.modelInstance.transform.getTranslation(currentPosition);

        // Clear the move translation out
        moveTranslation.set(0, 0, 0);
    }

    private void buildBoxes() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        for (int x = 0; x < 100; x += 10) {
            for (int z = 0; z < 100; z += 10) {
                Material material = new Material();
                material.set(PBRColorAttribute.createBaseColorFactor(Color.RED));
                MeshPartBuilder builder =
                        modelBuilder.part(
                                x + ", " + z,
                                GL20.GL_TRIANGLES,
                                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
                                material);
                BoxShapeBuilder.build(builder, x, 0, z, 1f, 1f, 1f);
            }
        }

        ModelInstance model = new ModelInstance(modelBuilder.end());
        sceneManager.addScene(new Scene(model));
    }

    private void updateCamera() {
        float horDistance = calculateHorizontalDistance(distanceFromPlayer);
        float vertDistance = calculateVerticalDistance(distanceFromPlayer);

        calculatePitch();
     //    calculateAngleAroundPlayer();
        calculateCameraPosition(currentPosition, -horDistance, vertDistance);

        camera.up.set(Vector3.Y);
        camera.lookAt(currentPosition);
        camera.update();
    }

    private void calculateCameraPosition(
            Vector3 currentPosition, float horDistance, float vertDistance) {
        float offsetX = (float) (horDistance * Math.sin(Math.toRadians(angleAroundPlayer)));
        float offsetZ = (float) (horDistance * Math.cos(Math.toRadians(angleAroundPlayer)));

        camera.position.x = currentPosition.x - offsetX;
        camera.position.z = currentPosition.z - offsetZ;
        camera.position.y = currentPosition.y + vertDistance;
    }

//    private void calculateAngleAroundPlayer() {
//        float angleChange = Gdx.input.getDeltaX() * Settings.CAMERA_ANGLE_AROUND_PLAYER_FACTOR;
//
//            angleAroundPlayer -= angleChange;
//        
//        
//        if(sprinting)
//        playerScene.modelInstance.transform.rotate(Vector3.Y, -angleChange);
//    }

    public void rotateCamera(float angle)
    {
        angleAroundPlayer += angle;
        if (angleAroundPlayer >= 360) angleAroundPlayer-=360;
        if (angleAroundPlayer < 0) angleAroundPlayer +=360;
    }

    public void rotatePlayer(float angle)
    {
        playerTransform.rotate(Vector3.Y, angle);
        angleBehindPlayer += angle;
        if (angleBehindPlayer >= 360) angleBehindPlayer-=360;
        if (angleBehindPlayer < 0) angleBehindPlayer +=360;
    }
    
    
    
    public void rotatePlayerInCamDirection(Float rapidRotationMax) {

        //playerTransform.rotate(Vector3.Y , camera.direction);
        //Erkka: this is what you had. I try to simplify the problem:
        //suppose camera.direction is 270 degrees
        //playerTransfrom.rotate would then turn the player 270 degrees
        //and the next time this is run, it would again turn the player 270 degrees
        //so, playerTransfrom.rotate is not "rotate to given angle" but "rotate by given angle"
        //and then it gets more complicated, since I think camera.direction has the angles for X,Y and Z axises


        //we get the difference between camera angle and the angle behind the player
        float diff = angleAroundPlayer-angleBehindPlayer;
        if (diff >= 360) diff-=360;
        if (diff < 0) diff+=360;

        //if rapidRotationMax is null, we will turn the player all the way so that the player will immediately face the camera direction
        //but if we need an animation-style slow turn towards the camera, then we'd need to set the rapidRotationMax
        if (rapidRotationMax != null) {
            if (diff > rapidRotationMax) diff = rapidRotationMax;
            if (diff < -rapidRotationMax) diff = -rapidRotationMax;
        }

        //and then we turn
        rotatePlayer(diff);
    }

    public void rotatePlayerInGivenDirection(Float rapidRotationMax, float towards) {

        //we get the difference between the desired angle and the angle behind the player
        float diff = towards-angleBehindPlayer;
        if (diff >= 360) diff-=360;
        if (diff < 0) diff+=360;

        //if rapidRotationMax is null, we will turn the player all the way so that the player will immediately face the camera direction
        //but if we need an animation-style slow turn towards the camera, then we'd need to set the rapidRotationMax
        if (rapidRotationMax != null) {
            if (diff > rapidRotationMax) diff = rapidRotationMax;
            if (diff < -rapidRotationMax) diff = -rapidRotationMax;
        }

        //and then we turn
        rotatePlayer(diff);
    }

    private void calculatePitch() {
        float pitchChange = -Gdx.input.getDeltaY() * Settings.CAMERA_PITCH_FACTOR;
        camPitch -= pitchChange;

        if (camPitch < Settings.CAMERA_MIN_PITCH) camPitch = Settings.CAMERA_MIN_PITCH;
        else if (camPitch > Settings.CAMERA_MAX_PITCH) camPitch = Settings.CAMERA_MAX_PITCH;
    }

    private float calculateVerticalDistance(float distanceFromPlayer) {
        return (float) (distanceFromPlayer * Math.sin(Math.toRadians(camPitch)));
    }

    private float calculateHorizontalDistance(float distanceFromPlayer) {
        return (float) (distanceFromPlayer * Math.cos(Math.toRadians(camPitch)));
    }

    @Override
    public void dispose() {
        sceneManager.dispose();
        sceneAsset.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
        skybox.dispose();
    }

    @Override
    public void onEnd(AnimationController.AnimationDesc animation) {}

    @Override
    public void onLoop(AnimationController.AnimationDesc animation) {}

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        float zoomLevel = amountY * Settings.CAMERA_ZOOM_LEVEL_FACTOR;
        distanceFromPlayer += zoomLevel;
        if (distanceFromPlayer < Settings.CAMERA_MIN_DISTANCE_FROM_PLAYER)
            distanceFromPlayer = Settings.CAMERA_MIN_DISTANCE_FROM_PLAYER;
        return false;
    }

    @Override
    public boolean touchCancelled(int arg0, int arg1, int arg2, int arg3) {
        return false;
    }
}
