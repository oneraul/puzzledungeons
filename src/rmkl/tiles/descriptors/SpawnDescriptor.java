package rmkl.tiles.descriptors;

import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.NumberSelector;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisWindow;
import rmkl.tiles.Tile;

public class SpawnDescriptor extends TileDescriptor {

    static ModelInstance positionModelInstance, angleModelInstance;
    public int angle;

    public SpawnDescriptor() {
        type = Tile.spawnCode;
    }

    @Override
    public void selectedWindow(VisWindow selectedWindow) {
        VisTextButton button = new VisTextButton("set");
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                useSecondaryCursor = true;
            }
        });
        selectedWindow.add(button).row();

        NumberSelector angleSelector = new NumberSelector("Angle", angle, 0, 359, 1);
        angleSelector.addChangeListener(v -> {
            angle = (int)angleSelector.getValue();
            setModelInstance();
        });
        selectedWindow.add(angleSelector);
    }

    public void setModelInstance() {
        positionModelInstance.transform.setTranslation(x+0.5f, 0.01f, z+0.5f);
        angleModelInstance.transform.setToRotation(Vector3.Y, -angle);
        angleModelInstance.transform.setTranslation(x+0.5f, 0.01f, z+0.5f);
    }

    @Override
    public boolean clickCallback(Vector3 secondaryCursor) {
        if(useSecondaryCursor) {
            useSecondaryCursor = false;

            x = (int)secondaryCursor.x;
            z = (int)secondaryCursor.z;
            setModelInstance();
            return true;
        }

        else return false;
    }

    @Override
    public void extraDrawCalls(ModelBatch modelBatch) {
        modelBatch.render(positionModelInstance);
        modelBatch.render(angleModelInstance);
    }

    @Override
    public String serialize() {
        return super.serialize() +
                "        \"angle\" : " + angle + "\n";
    }
}
