package rmkl.pjCommands;

import rmkl.Pj3D;

public class Gravity extends PjCommand {

    public static final short type = 1;
    private final Pj3D pj;
    private float v;

    @Override
    public short type() {
        return type;
    }

    public Gravity(Pj3D pj) {
        this.pj = pj;
        this.reset();
    }

    public void reset() {
        final float initial_v = 5f;

        v = initial_v;
        //pj.animationController.action("jump", 1, 1, null, 0.1f);
    }

    @Override
    public boolean update(float dt) {
        final float gravity = 3f * 9.8f;

        v -= gravity * dt;
        pj.y += v * dt;

        if(pj.y <= 0) {
            pj.y = 0;
            //pj.animationController.setAnimation("idle");
            return true;
        }

        return false;
    }
}
