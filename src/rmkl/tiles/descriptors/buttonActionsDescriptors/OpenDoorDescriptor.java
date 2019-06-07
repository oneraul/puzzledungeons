package rmkl.tiles.descriptors.buttonActionsDescriptors;

import rmkl.tiles.descriptors.ButtonDescriptor;

public class OpenDoorDescriptor extends DoorButtonDescriptor {

    public OpenDoorDescriptor(ButtonDescriptor buttonDescriptor) {
        super(buttonDescriptor);
        type = "OpenDoor";
    }

    @Override
    public ButtonActionDescriptor instantiate() {
        return new OpenDoorDescriptor(buttonDescriptor);
    }
}
