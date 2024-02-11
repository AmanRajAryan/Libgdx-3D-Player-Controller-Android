package aman.three;
import aman.three.MyGame;
import aman.three.TouchPad;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;

public class HUD {
    MyGame mainGameClass;
    public void initializeHUD(MyGame game , Stage stage) {
    	this.mainGameClass = game;
        mainGameClass.touchpad = new TouchPad(stage);
        
        
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
                        if (mainGameClass.sprinting) {
                            mainGameClass.sprinting = false;
                        } else {
                            mainGameClass.isSprintBtnJustClicked = true;
                            mainGameClass.sprinting = true;
                        }
                        event.stop();
                        
                        return true;
                    }
                });
        stage.addActor(sprintBtn);
        
        
        
    }
	 
    
    
    
}