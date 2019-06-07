package rmkl.pjCommands;

import rmkl.tiles.Teleporter;

public class TeleportCD extends PjCommand {

    public static final short type = 2;
    private static final float duration = 1;
    private float accumulator;

    @Override
    public short type() {
        return type;
    }

    @Override
    public boolean update(float dt) {
        accumulator += dt;
        return (accumulator >= duration);
    }
}