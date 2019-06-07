package rmkl.tiles.descriptors.buttonActionsDescriptors;

import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import rmkl.tiles.Door;
import rmkl.tiles.descriptors.ButtonDescriptor;
import rmkl.tiles.descriptors.TileDescriptor;

public abstract class DoorButtonDescriptor extends ButtonActionDescriptor {

    protected ModelInstance modelInstance;
    private boolean modelInstanceIsSet;
    private VisLabel doorLabel = new VisLabel();
    public int doorX = -1, doorY = -1;

    DoorButtonDescriptor(ButtonDescriptor buttonDescriptor) {
        super(buttonDescriptor);
    }

    @Override
    public void selectedWindow(VisTable selectedWindow) {
        setModelInstance();

        selectedWindow.add(type).row();
        selectedWindow.add(doorLabel).row();
        if(doorX == -1 && doorY == -1)
            doorLabel.setText("door not set");
        else {
            doorLabel.setText("door: " + doorX + ", " + doorY);
            modelInstance.transform.setTranslation(doorX+0.5f, 0.01f, doorY+0.5f);
        }

        VisTextButton button = new VisTextButton("set door");
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                buttonDescriptor.useSecondaryCursor = true;
                setActiveMode();
            }
        });
        selectedWindow.add(button).row();
    }

    private void setModelInstance() {
        if(doorX != -1 && doorY != -1) {

            modelInstanceIsSet = true;
            if(modelInstance == null) modelInstance = new ModelInstance(model);
            modelInstance.transform.setTranslation(doorX+0.5f, 0.01f, doorY+0.5f);
        }
    }

    @Override
    public boolean secondaryCursorCallback(Vector3 secondaryCursor) {
        if(buttonDescriptor.useSecondaryCursor) {
            int x = (int)secondaryCursor.x;
            int z = (int)secondaryCursor.z;
            if(TileDescriptor.grid[x][z].type() == Door.code) {

                buttonDescriptor.useSecondaryCursor = false;

                doorX = x;
                doorY = z;
                doorLabel.setText("door: " + doorX + ", " + doorY);

                setModelInstance();
                return true;
            }
        }
        return false;
    }

    @Override
    public void extraDrawCalls(ModelBatch modelBatch) {
        if(modelInstanceIsSet) modelBatch.render(modelInstance);
    }

    @Override
    public String serialize() {
        return super.serialize() +
                "                \"doorX\" : " + doorX + ",\n" +
                "                \"doorY\" : " + doorY + "\n";
    }
}
