package rmkl;

import collision.AABB;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import rmkl.pjCommands.PjCommand;

public class PjConnection extends Connection {

    private static final AABB tmpAABB = new AABB(0, 0, 0.2f, 0.2f);

    public Network.Snapshot snapshot;
    String sessionID;
    String name;
    PartyGroup party;
    String map;
    public final Array<PjCommand> pjCommands = new Array<>();

    PjConnection() {}

    public static AABB collider(PjConnection c) {
        tmpAABB.pos.set(c.snapshot.pos);
        return tmpAABB;
    }

    static Network.AddPlayer addPlayer(PjConnection c) {
        Network.AddPlayer p = new Network.AddPlayer();
        p.id = c.getID();
        p.pos = c.snapshot.pos;
        p.angle = c.snapshot.angle;
        p.name = c.name;
        return p;
    }

    static void initSnapshot(PjConnection c, Network.Snapshot spawnSnapshot) {
        c.snapshot = new Network.Snapshot();
        c.snapshot.id = c.getID();
        c.snapshot.pos = new Vector2(spawnSnapshot.pos);
        c.snapshot.angle = spawnSnapshot.angle;
        c.snapshot.animation = "idle";
    }

    public static Network.Teleported teleported(PjConnection c) {
        Network.Teleported p = new Network.Teleported();
        p.id = c.getID();
        p.pos = c.snapshot.pos;
        p.angle = c.snapshot.angle;
        p.animation = c.snapshot.animation;
        return p;
    }
}

class PjServer extends Server {

    @Override
    protected Connection newConnection() {
        return new PjConnection();
    }
}

