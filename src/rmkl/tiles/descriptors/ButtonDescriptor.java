package rmkl.tiles.descriptors;

import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.*;
import rmkl.tiles.Button;
import rmkl.tiles.descriptors.buttonActionsDescriptors.*;

public class ButtonDescriptor extends TileDescriptor {

    public final Array<ButtonActionDescriptor> modes;
    private ButtonActionDescriptor activeModeCallback;
    private final VisTable GUI, modesGUI;

    public ButtonDescriptor() {
        type = Button.code;

        modes = new Array<>();

        GUI = new VisTable();
        VisSelectBox<ButtonActionDescriptor> modeSelectBox = new VisSelectBox<>();
        modeSelectBox.setItems(
            new SwitchDoorDescriptor(this),
            new CloseDoorDescriptor(this),
            new OpenDoorDescriptor(this)
        );
        VisTextButton addModeButton = new VisTextButton("add");
        addModeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                modes.add(modeSelectBox.getSelected().instantiate());
                setGUIModes();
            }
        });

        GUI.add(modeSelectBox);
        GUI.add(addModeButton).row();
        GUI.addSeparator().colspan(2).row();
        GUI.add(modesGUI = new VisTable()).colspan(2).expand().fill();
    }

    public void setGUIModes() {
        modesGUI.clear();
        for(ButtonActionDescriptor mode : modes) {
            mode.selectedWindow(modesGUI);
            modesGUI.addSeparator().row();
        }
    }

    @Override
    public void selectedWindow(VisWindow selectedWindow) {
        selectedWindow.add(GUI);
    }

    @Override
    public boolean clickCallback(Vector3 secondaryCursor) {
        if(activeModeCallback != null) {
            boolean value = activeModeCallback.secondaryCursorCallback(secondaryCursor);
            activeModeCallback = null;
            return value;
        }

        return false;
    }

    public void setActiveMode(ButtonActionDescriptor buttonMode) {
        activeModeCallback = buttonMode;
    }

    @Override
    public void extraDrawCalls(ModelBatch modelBatch) {
        for(ButtonActionDescriptor mode : modes) mode.extraDrawCalls(modelBatch);
    }

    @Override
    public String serialize() {

        String str = super.serialize();

        str += "        \"modes\" : [\n";
        for(ButtonActionDescriptor mode : modes) {
            str += mode.serialize();
            str += "            },\n";
        }
        str += "        ],\n";

        return str;
    }
}
