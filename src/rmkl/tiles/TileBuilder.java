package rmkl.tiles;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;

public class TileBuilder {

    private final ObjectMap<GridPoint2, GridPoint2> teleporters; // this is passed to every Teleporter instance in the server

    private Pool<Error> errorPool = new Pool<Error>() {
        @Override
        protected Error newObject() {
            return new Error();
        }
    };
    private Pool<Floor> floorPool = new Pool<Floor>() {
        @Override
        protected Floor newObject() {
            return new Floor();
        }
    };
    private Pool<Wall> wallPool = new Pool<Wall>() {
        @Override
        protected Wall newObject() {
            return new Wall();
        }
    };

    public TileBuilder() {
        teleporters = null;
    }

    public TileBuilder(final ObjectMap<GridPoint2, GridPoint2> teleporters) {
        this.teleporters = teleporters;
    }

    public Tile build(char type, int x, int y, char[][] map) {

        switch(type) {
            case Floor.code: return floorPool.obtain().init(x, y);
            case Wall.code: return wallPool.obtain().init(x, y);
            case Teleporter.code: return new Teleporter(x, y, teleporters);
            case Button.code: return new Button(x, y);
            case Fence.code: return new Fence(x, y, map);
            case Door.code: return Door.build(x, y, map);
            case TWall.code: return TWall.build(x, y, map);
        }

        return errorPool.obtain().init(x, y);
    }
}
