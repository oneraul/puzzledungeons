package rmkl.tiles;

import collision.AABB;
import collision.Hit;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.Server;
import rmkl.*;

public class Button extends Tile {

    public static final char code = 'B';
    static Model model;
    private AnimationController animationController;
    private final AABB collider;
    private PjConnection pj;
    private Server server;
    private final Array<ButtonAction> actions;
    private boolean pressed;
    private boolean updateFlag;

    private int thisX, thisY;

    Button(int x, int y) {
        if(model != null) {
            modelInstance = new ModelInstance(model);
            modelInstance.transform.setTranslation(x+0.5f, 0, y+0.5f);
            animationController = new AnimationController(modelInstance);
            animationController.allowSameAnimation = true;
        }
        collider = new AABB(x+0.5f, y+0.5f, 0.25f, 0.25f);
        actions = new Array<>();

        thisX = x;
        thisY = y;
    }

    @Override
    public void update(float dt) {
        if(updateFlag) {

            //client
            if(animationController != null) {
                animationController.update(dt);
                updateFlag = animationController.current.time < animationController.current.duration;

            //server
            } else {
                Hit hit = PjConnection.collider(pj).intersectAABB(collider);
                if (hit == null) {
                    unpress(server);
                    updateFlag = false;
                }
            }
        }
    }

    public void press(Server server) {
        pressed = true;
        updateFlag = true;

        for(ButtonAction mode : actions) {
            Network.Packet p = mode.press();
            if(p != null) server.sendToAllTCP(p);
        }

        Network.PressButton p = new Network.PressButton();
        p.buttonX = thisX;
        p.buttonY = thisY;
        server.sendToAllUDP(p);
    }

    public void press() {
        pressed = true;
        if(animationController != null) {
            animationController.setAnimation("press");
            updateFlag = true;
        }
    }

    public void unpress(Server server) {
        pressed = false;
        for(ButtonAction mode : actions) {
            Network.Packet p = mode.unpress();
            if(p != null) server.sendToAllTCP(p);
        }

        this.server = null;
        this.pj = null;

        Network.UnpressButton p = new Network.UnpressButton();
        p.buttonX = thisX;
        p.buttonY = thisY;
        server.sendToAllUDP(p);
    }

    public void unpress() {
        if(animationController != null) {
            animationController.setAnimation("unpress");
            updateFlag = true;
        }
    }

    @Override
    public void serverCollisionResponse(Server server, PjConnection c) {
        if(pressed) return;

        Hit hit = PjConnection.collider(c).intersectAABB(collider);
        if(hit != null) {
            this.server = server;
            this.pj = c;
            press(server);
        }
    }

    public void disconnected(PjConnection c) {
        if(c == this.pj) {
            unpress(server);
            updateFlag = false;
        }
    }

    @Override
    public void collisionResponse(Pj3D pj) {}

    @Override
    public void cameraCollision(Pj3D pj, Camera3D cam) {}

    @Override
    public char type() {
        return code;
    }

    public void addMode(ButtonAction action) {
        actions.add(action);
    }



    public interface ButtonAction {
        default Network.Packet press() {
            return null;
        }
        default Network.Packet unpress() {
            return null;
        }
    }

    public class SwitchDoor implements ButtonAction {
        final Door door;

        public SwitchDoor(Door door) {
            this.door = door;
        }

        @Override
        public Network.Packet press() {
            door.setLocked(false);
            Network.OpenDoor p = new Network.OpenDoor();
            p.doorX = door.x;
            p.doorY = door.y;
            return p;
        }

        @Override
        public Network.Packet unpress() {
            door.setLocked(true);
            Network.CloseDoor p = new Network.CloseDoor();
            p.doorX = door.x;
            p.doorY = door.y;
            return p;
        }
    }

    public class CloseDoor implements ButtonAction {
         final Door door;

        public CloseDoor(Door door) {
            this.door = door;
        }

        @Override
        public Network.Packet press() {
            door.setLocked(true);
            Network.CloseDoor p = new Network.CloseDoor();
            p.doorX = door.x;
            p.doorY = door.y;
            return p;
        }
    }

    public class OpenDoor implements ButtonAction {
        final Door door;

        public OpenDoor(Door door) {
            this.door = door;
        }

        @Override
        public Network.Packet press() {
            door.setLocked(false);
            Network.OpenDoor p = new Network.OpenDoor();
            p.doorX = door.x;
            p.doorY = door.y;
            return p;
        }
    }
}
