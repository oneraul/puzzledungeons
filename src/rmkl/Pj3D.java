package rmkl;

import collision.AABB;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import rmkl.pjCommands.Gravity;
import rmkl.pjCommands.Interpolate;
import rmkl.pjCommands.PjCommand;

public class Pj3D {
    public final Vector2 pos;    public float y;    //TODO merge into Vector3
    final Vector2 dir;
    public float angle;
    public AABB collider;
    public ModelInstance modelInstance;
    AnimationController animationController;
    final Array<PjCommand> commands;
    private final Gravity tmpGravity;
    Interpolate interpolate;
    String name;

    Pj3D(Model model, Network.AddPlayer spawnPacket) {
        pos = new Vector2();
        dir = new Vector2();

        modelInstance = new ModelInstance(model);
        animationController = new AnimationController(modelInstance);
        animationController.setAnimation("idle");

        collider = new AABB(0, 0, 0.2f, 0.2f);

        commands = new Array<>();
        tmpGravity = new Gravity(this);

        pos.set(spawnPacket.pos);
        angle = spawnPacket.angle;
        update(0);
    }

    void update(float dt) {
        for(int i = commands.size-1; i >= 0; i--) {
            if(commands.get(i).update(dt)) commands.removeIndex(i);
        }

        modelInstance.transform.setToRotation(Vector3.Y, 90-angle);
        modelInstance.transform.setTranslation(pos.x, y, pos.y);
        animationController.update(dt);
    }

    void jump() {
        if(y > 0) return;
        if(hasCommand(Gravity.type, commands)) return;
        tmpGravity.reset();
        commands.add(tmpGravity);
    }

    public static boolean hasCommand(short type, Array<PjCommand> commands) {
        for(PjCommand c : commands) {
            if(c.type() == type) return true;
        }
        return false;
    }

    private static final Network.Snapshot tmpSnapshot = new Network.Snapshot();
    static Network.Snapshot snapshot(Pj3D pj) {
        tmpSnapshot.pos = pj.pos;
        tmpSnapshot.angle = pj.angle;
        tmpSnapshot.animation = pj.animationController.current.animation.id;
        return tmpSnapshot;
    }
}