package rmkl.tiles.descriptors;

import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.NumberSelector;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisWindow;
import rmkl.tiles.Teleporter;

public class TeleporterDescriptor extends TileDescriptor {

    private static VisLabel destinationLabel = new VisLabel();
    static ModelInstance destinationModelInstance, angleModelInstance;
    private boolean modelInstanceIsSet;
    public int targetX = -1, targetY = -1;
    public int angle;

    public TeleporterDescriptor() {
        type = Teleporter.code;
    }

    @Override
    public void selectedWindow(VisWindow selectedWindow) {
        selectedWindow.add(destinationLabel).row();
        if(targetX == -1 && targetY == -1)
            destinationLabel.setText("destination not set");
        else {
            destinationLabel.setText("destination: " + targetX + ", " + targetY);
            destinationModelInstance.transform.setTranslation(targetX+0.5f, 0.01f, targetY+0.5f);
            angleModelInstance.transform.setToRotation(Vector3.Y, angle);
            angleModelInstance.transform.setTranslation(targetX+0.5f, 0.01f, targetY+0.5f);
        }

        VisTextButton button = new VisTextButton("set destination");
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

    @Override
    public boolean clickCallback(Vector3 secondaryCursor) {
        if(useSecondaryCursor) {
            useSecondaryCursor = false;
            targetX = (int)secondaryCursor.x;
            targetY = (int)secondaryCursor.z;
            destinationLabel.setText("destination: " + targetX + ", " + targetY);

            setModelInstance();

            return true;
        }

        else return false;
    }

    public void setModelInstance() {
        if(targetX != -1 && targetY != -1) {
            modelInstanceIsSet = true;
            destinationModelInstance.transform.setTranslation(targetX+0.5f, 0.01f, targetY+0.5f);
            angleModelInstance.transform.setToRotation(Vector3.Y, -angle);
            angleModelInstance.transform.setTranslation(targetX+0.5f, 0.01f, targetY+0.5f);
        }
    }

    @Override
    public void extraDrawCalls(ModelBatch modelBatch) {
        if(modelInstanceIsSet) {
            modelBatch.render(destinationModelInstance);
            modelBatch.render(angleModelInstance);
        }
    }

    @Override
    public String serialize() {
        return super.serialize() +
                "        \"targetX\" : " + targetX + ",\n" +
                "        \"targetY\" : " + targetY + ",\n" +
                "        \"angle\" : " + angle + "\n";
    }
}
