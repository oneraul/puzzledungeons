package rmkl.tiles;

import collision.AABB;
import collision.Hit;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.ObjectMap;
import com.esotericsoftware.kryonet.Server;
import rmkl.Camera3D;
import rmkl.Pj3D;
import rmkl.PjConnection;
import rmkl.pjCommands.TeleportCD;

public class Teleporter extends Tile {

    public static final char code = 'T';
    private static final GridPoint2 tmpGridPoint2 = new GridPoint2();
    static Model model;
    private final ObjectMap<GridPoint2, GridPoint2> teleporters;
    private final AABB collider;
    public int angle;

    private int thisX, thisY;

    Teleporter(int x, int y, final ObjectMap<GridPoint2, GridPoint2> teleporters) {
        this.teleporters = teleporters;

        if(model != null) {
            modelInstance = new ModelInstance(model);
            modelInstance.transform.setTranslation(x+0.5f, 0, y+0.5f);
        }
        collider = new AABB(x+0.5f, y+0.5f, 0.25f, 0.25f);
        thisX = x;
        thisY = y;
    }

    @Override
    public void collisionResponse(Pj3D pj) {}

    @Override
    public void serverCollisionResponse(Server server, PjConnection c) {
        Hit hit = PjConnection.collider(c).intersectAABB(collider);
        if(hit != null) {
            if(!Pj3D.hasCommand(TeleportCD.type, c.pjCommands)) {
                c.pjCommands.add(new TeleportCD());

                tmpGridPoint2.set(thisX, thisY);
                if (!teleporters.containsKey(tmpGridPoint2)) return; // null teleporter

                GridPoint2 other = teleporters.get(tmpGridPoint2);
                c.snapshot.pos.set(other.x, other.y).add(0.5f, 0.5f);
                c.snapshot.angle = this.angle;
                server.sendToAllTCP(PjConnection.teleported(c));
            }
        }
    }

    @Override
    public void cameraCollision(Pj3D pj, Camera3D cam) {}

    @Override
    public char type() {
        return code;
    }

    public void addToTeleportersList(GridPoint2 me, GridPoint2 target) {
        if(teleporters != null) teleporters.put(me, target);
    }
}