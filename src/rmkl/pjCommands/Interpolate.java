package rmkl.pjCommands;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import rmkl.Network;
import rmkl.Pj3D;

public class Interpolate extends PjCommand {

    public static final short type = 0;
    private final Pj3D pj;
    public Network.Snapshot oldSnapshot;
    public Network.Snapshot newSnapshot;
    public float timer;

    public Interpolate(Pj3D pj) {
        this.pj = pj;

        Network.Snapshot snapshot = new Network.Snapshot();
        snapshot.pos = new Vector2(pj.pos);
        snapshot.angle = pj.angle;
        newSnapshot = snapshot;
        oldSnapshot = snapshot;
    }

    @Override
    public short type() {
        return type;
    }

    @Override
    public boolean update(float dt) {
        timer = MathUtils.clamp(timer + dt, 0, 1);
        float factor = timer / 0.15f;    // TODO make it relative to ping
        pj.pos.x = oldSnapshot.pos.x * (1-factor) + newSnapshot.pos.x * factor;
        pj.pos.y = oldSnapshot.pos.y * (1-factor) + newSnapshot.pos.y * factor;
        pj.angle = oldSnapshot.angle * (1-factor) + newSnapshot.angle * factor;

        return false;
    }
}
