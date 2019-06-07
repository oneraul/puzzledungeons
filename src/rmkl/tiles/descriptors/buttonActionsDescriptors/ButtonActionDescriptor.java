package rmkl.tiles.descriptors.buttonActionsDescriptors;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector3;
import com.kotcrab.vis.ui.widget.VisTable;
import rmkl.tiles.descriptors.ButtonDescriptor;

public abstract class ButtonActionDescriptor {

    protected ButtonDescriptor buttonDescriptor;
    public static Model model;
    String type;

    public ButtonActionDescriptor(ButtonDescriptor buttonDescriptor) {
        this.buttonDescriptor = buttonDescriptor;
    }

    @Override
    public String toString() {
        return type;
    }

    void setActiveMode() {
        buttonDescriptor.setActiveMode(this);
    }

    public abstract ButtonActionDescriptor instantiate();
    public abstract void selectedWindow(VisTable selectedWindow);
    public boolean secondaryCursorCallback(Vector3 secondaryCursor) {
        return false;
    }
    public void extraDrawCalls(ModelBatch modelBatch) {}

    public String serialize() {
        return "            {\n                \"mode\" : " + type + ",\n";
    }
}
