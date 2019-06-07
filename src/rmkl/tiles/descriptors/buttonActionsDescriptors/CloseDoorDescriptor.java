package rmkl.tiles.descriptors.buttonActionsDescriptors;

import rmkl.tiles.descriptors.ButtonDescriptor;

public class CloseDoorDescriptor extends DoorButtonDescriptor {

    public CloseDoorDescriptor(ButtonDescriptor buttonDescriptor) {
        super(buttonDescriptor);
        type = "CloseDoor";
    }

    @Override
    public ButtonActionDescriptor instantiate() {
        return new CloseDoorDescriptor(buttonDescriptor);
    }
}
