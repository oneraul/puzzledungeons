package rmkl.tiles.descriptors;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisWindow;
import rmkl.tiles.Door;

public class DoorDescriptor extends TileDescriptor {

    public boolean locked = true;

    public DoorDescriptor() {
        type = Door.code;
    }

    @Override
    public void selectedWindow(VisWindow selectedWindow) {
        VisCheckBox lockedCheckBox = new VisCheckBox("locked");
        lockedCheckBox.setChecked(locked);
        lockedCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                locked = lockedCheckBox.isChecked();
                ((Door)grid[x][z]).setLocked(locked);
            }
        });
        selectedWindow.add(lockedCheckBox).row();
    }

    @Override
    public String serialize() {
        return super.serialize() +
                "        \"locked\" : " + locked + "\n";
    }
}
