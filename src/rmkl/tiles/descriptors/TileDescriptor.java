package rmkl.tiles.descriptors;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.kotcrab.vis.ui.widget.VisWindow;
import rmkl.tiles.Tile;
import rmkl.tiles.descriptors.buttonActionsDescriptors.ButtonActionDescriptor;

public class TileDescriptor {

    public static Tile[][] grid;
    public static void loadModelInstances(Model model) {
        TeleporterDescriptor.destinationModelInstance = new ModelInstance(model, "destination");
        TeleporterDescriptor.angleModelInstance = new ModelInstance(model, "angle");
        SpawnDescriptor.positionModelInstance = new ModelInstance(model, "destination");
        SpawnDescriptor.angleModelInstance = new ModelInstance(model, "angle");
        ButtonActionDescriptor.model = model;
    }

    protected char type;
    public int x, z;
    public boolean useSecondaryCursor;

    public void selectedWindow(VisWindow selectedWindow) {}

    public boolean clickCallback(Vector3 secondaryCursor) {
        return false;
    }

    public void extraDrawCalls(ModelBatch modelBatch) {}

    public String serialize() {
        return "        \"type\" : " + type + ",\n" +
                "        \"x\" : " + x + ",\n" +
                "        \"y\" : " + z + ",\n";
    }
}
