package aman.three;


import static java.lang.Math.atan2;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import net.mgsx.gltf.scene3d.scene.Scene;

public class PlayerController {

    MyGame mainGameClass;
    float speed = 5f;
    float rotationSpeed = 80f;
    private final Matrix4 playerTransform = new Matrix4();
    private final Vector3 moveTranslation = new Vector3();
    private final Vector3 currentPosition = new Vector3();

    private float camPitch = Settings.CAMERA_START_PITCH;
    public float distanceFromPlayer = 20f;
    private float angleAroundPlayer = 0f;
    private float angleBehindPlayer = 0f;

    boolean inTouchpad; // Erkka: if the touch is in the touchpad, false otherwise
    PerspectiveCamera camera;
    Scene playerScene;
    TouchPad touchpad;
    int deltaX; // Erkka: a helper variable to store how much the touch has been dragged sideways
    float touchpadX; // Erkka: two more helper functions to store the touchpad knob position
    float touchpadY;
    float touchpadAngle; // Erkka: another helper function to store the touchpad angle. 0 for

    float previousGetX;
    float previousGetY;
    
    

    public void createContoller(MyGame game) {
        this.mainGameClass = game;

        camera = mainGameClass.camera;
        playerScene = mainGameClass.playerScene;
        touchpad = mainGameClass.touchpad;
    }

    public void processInput(float deltaTime) {

        deltaX = Gdx.input.getDeltaX();
        
        
        
        if (Gdx.input.getX() > Gdx.graphics.getWidth() / 2) {

            rotateCamera(-deltaX);
        }
       

        if (Gdx.input.getX(1) > Gdx.graphics.getWidth() / 2) {

             rotateCamera(-Gdx.input.getDeltaX(1));

            
        }

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

            // here we handle the touchpad
            inTouchpad = true;
            mainGameClass.sprinting = false;

            touchpadX = touchpad.getTouchpad().getKnobPercentX();
            touchpadY = touchpad.getTouchpad().getKnobPercentY();

            if ((touchpadX != 0) || (touchpadY != 0)) {

                mainGameClass.isWalking = true;
                // Erkka: atan2() function returns the angle in radians, so we convert it to degrees
                // by *180/pi
                touchpadAngle = (float) (atan2(touchpadY, touchpadX) * 180.0d / Math.PI);

                // we now have the absolute toucpadAngle, ie. 0 is always to the east, 90 always to
                // the right
                // but the camera might be rotated, and we probably want knob to the right moving
                // the character to the right of the screen, not to the absolute east
                // luckily, we have the camera facing in angleAroundPlayer
                // but angleAroundPlayer has 0 to north, and seems to be clock-wise
                // so we need to do some computation to make angleAroundPlayer compatible
                // with the angle we calculated for the knob

                float convertedAngle = (360 - (angleAroundPlayer - 90));
                while (convertedAngle < 0) convertedAngle += 360;

                touchpadAngle -= convertedAngle;

                rotatePlayerInGivenDirection(null, touchpadAngle);
                moveTranslation.z += speed * deltaTime;
            }

        } else {
            inTouchpad = false;
            mainGameClass.isWalking = false;
        }

        if (!inTouchpad) {

            // Erkka: here we read the touch dragged horizontally, outside the touchpad

            if (deltaX != 0) {
                if (mainGameClass.sprinting) {
                    rotatePlayer(-deltaX);
                }

                // if in sprint mode, we rotate both the player and the camera
                // if not in sprint mode, we rotate only the camera

            }
        }

        if (mainGameClass.isSprintBtnJustClicked) {
            rotatePlayerInCamDirection(null);
            mainGameClass.isSprintBtnJustClicked = false;
        }

        if (mainGameClass.sprinting) {
            moveTranslation.z += 10f * deltaTime;
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

    public void updateCamera() {
        float horDistance = calculateHorizontalDistance(distanceFromPlayer);
        float vertDistance = calculateVerticalDistance(distanceFromPlayer + 20);

        calculatePitch();
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

    public void rotateCamera(float angle) {
        angleAroundPlayer += angle;
        if (angleAroundPlayer >= 360) angleAroundPlayer -= 360;
        if (angleAroundPlayer < 0) angleAroundPlayer += 360;
    }

    public void rotatePlayer(float angle) {
        playerTransform.rotate(Vector3.Y, angle);
        angleBehindPlayer += angle;
        if (angleBehindPlayer >= 360) angleBehindPlayer -= 360;
        if (angleBehindPlayer < 0) angleBehindPlayer += 360;
    }

    public void rotatePlayerInCamDirection(Float rapidRotationMax) {

        // playerTransform.rotate(Vector3.Y , camera.direction);
        // Erkka: this is what you had. I try to simplify the problem:
        // suppose camera.direction is 270 degrees
        // playerTransfrom.rotate would then turn the player 270 degrees
        // and the next time this is run, it would again turn the player 270 degrees
        // so, playerTransfrom.rotate is not "rotate to given angle" but "rotate by given angle"
        // and then it gets more complicated, since I think camera.direction has the angles for X,Y
        // and Z axises

        // we get the difference between camera angle and the angle behind the player
        float diff = angleAroundPlayer - angleBehindPlayer;
        if (diff >= 360) diff -= 360;
        if (diff < 0) diff += 360;

        // if rapidRotationMax is null, we will turn the player all the way so that the player will
        // immediately face the camera direction
        // but if we need an animation-style slow turn towards the camera, then we'd need to set the
        // rapidRotationMax
        if (rapidRotationMax != null) {
            if (diff > rapidRotationMax) diff = rapidRotationMax;
            if (diff < -rapidRotationMax) diff = -rapidRotationMax;
        }

        // and then we turn
        rotatePlayer(diff);
    }

    public void rotatePlayerInGivenDirection(Float rapidRotationMax, float towards) {

        // we get the difference between the desired angle and the angle behind the player
        float diff = towards - angleBehindPlayer;
        if (diff >= 360) diff -= 360;
        if (diff < 0) diff += 360;

        // if rapidRotationMax is null, we will turn the player all the way so that the player will
        // immediately face the camera direction
        // but if we need an animation-style slow turn towards the camera, then we'd need to set the
        // rapidRotationMax
        if (rapidRotationMax != null) {
            if (diff > rapidRotationMax) diff = rapidRotationMax;
            if (diff < -rapidRotationMax) diff = -rapidRotationMax;
        }

        // and then we turn
        rotatePlayer(diff);
    }

    private void calculatePitch() {
        if (Gdx.input.getX() > Gdx.graphics.getWidth() / 2) {
            float pitchChange = -Gdx.input.getDeltaY() * Settings.CAMERA_PITCH_FACTOR;
            camPitch -= pitchChange;
        }

        if (Gdx.input.getX(1) > Gdx.graphics.getWidth() / 2) {

            float pitchChange = -Gdx.input.getDeltaY(1) * Settings.CAMERA_PITCH_FACTOR;
            camPitch -= pitchChange;
        }

        if (camPitch < Settings.CAMERA_MIN_PITCH) camPitch = Settings.CAMERA_MIN_PITCH;
        else if (camPitch > Settings.CAMERA_MAX_PITCH) camPitch = Settings.CAMERA_MAX_PITCH;
    }

    private float calculateVerticalDistance(float distanceFromPlayer) {
        return (float) (distanceFromPlayer * Math.sin(Math.toRadians(camPitch)));
    }

    private float calculateHorizontalDistance(float distanceFromPlayer) {
        return (float) (distanceFromPlayer * Math.cos(Math.toRadians(camPitch)));
    }
}
