package rmkl.tiles;

import collision.AABB;
import collision.Hit;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import rmkl.Bitmask;
import rmkl.Camera3D;
import rmkl.Pj3D;

public class Fence extends CacheableTile implements AutoTilable {

    public static final char code = 'h';
    static Model model;
    private static final String[][] tileMap;
    private static ObjectMap<String, AABB> protoColliders;
    private Array<AABB> colliders;

    static {
        tileMap = new String[][] {
                new String[] {"floor", "pilar"},
                new String[] {"floor", "pilar", "north"},
                new String[] {"floor", "pilar", "west"},
                new String[] {"floor", "pilar", "north", "west"},
                new String[] {"floor", "pilar", "east"},
                new String[] {"floor", "pilar", "north", "east"},
                new String[] {"floor", "pilar", "east", "west"},
                new String[] {"floor", "pilar", "north", "east", "west"},
                new String[] {"floor", "pilar", "south"},
                new String[] {"floor", "pilar", "north", "south"},
                new String[] {"floor", "pilar", "south", "west"},
                new String[] {"floor", "pilar", "north", "south", "west"},
                new String[] {"floor", "pilar", "south", "east"},
                new String[] {"floor", "pilar", "north", "south", "east"},
                new String[] {"floor", "pilar", "south", "east", "west"},
                new String[] {"floor", "pilar", "north", "south", "east", "west"}
        };
    }

    /* stores a prototype collider for every node (except floor). Needs to be called AFTER model is set */
    static void load() {
        protoColliders = new ObjectMap<>();

        BoundingBox box = new BoundingBox();
        for(Node node : model.nodes) {
            if(node.id.equals("floor")) continue;

            node.calculateBoundingBox(box);
            AABB collider = new AABB(node.translation.x, node.translation.z, box.getWidth()*0.5f, box.getDepth()*0.5f);
            protoColliders.put(node.id, collider);
        }
    }

    Fence(int x, int y, char[][] map) {

        if(model != null) {

            String[] nodes = null;
            int bitmask = Bitmask.check4bit(x, y, map, Fence.code, Wall.code, TWall.code, Door.code);
            if (bitmask >= 0 && bitmask < 16) nodes = tileMap[bitmask];

            modelInstance = new ModelInstance(model, nodes);
            modelInstance.transform.translate(x+0.5f, 0, y+0.5f);

            /* caches the collider instances built upon the prototypes */
            colliders = new Array<>();
            for (String node : nodes) {
                if (nodes.length > 2 && node.equals("pilar"))
                    continue;  // the pilar node only gets a collider if it's alone
                AABB protoCollider = protoColliders.get(node, null);
                if (protoCollider == null) continue;

                AABB aabb = new AABB(protoCollider.pos, protoCollider.half);
                aabb.pos.add(x+0.5f, y+0.5f);
                colliders.add(aabb);
            }
        }
    }

    @Override
    public void collisionResponse(Pj3D pj) {
        for(AABB collider : colliders) {
            Hit hit = pj.collider.intersectAABB(collider);
            if(hit != null) pj.collider.pos.add(hit.delta);
        }
    }

    @Override
    public void cameraCollision(Pj3D pj, Camera3D cam) {
        for(AABB collider : colliders) {
            Hit hit = collider.intersectSegment(pj.pos, cam.pos);
            if(hit != null) cam.pos.set(hit.pos);
        }
    }

    @Override
    public char type() {
        return code;
    }
}
