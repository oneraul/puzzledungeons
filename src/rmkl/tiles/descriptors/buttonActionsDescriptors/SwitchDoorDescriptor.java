package rmkl.tiles.descriptors.buttonActionsDescriptors;

import rmkl.tiles.descriptors.ButtonDescriptor;

public class SwitchDoorDescriptor extends DoorButtonDescriptor {

    public SwitchDoorDescriptor(ButtonDescriptor buttonDescriptor) {
        super(buttonDescriptor);
        type = "SwitchDoor";
    }

    @Override
    public ButtonActionDescriptor instantiate() {
        return new SwitchDoorDescriptor(buttonDescriptor);
    }
}
