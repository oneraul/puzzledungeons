package rmkl;

import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import rmkl.tiles.Button;
import rmkl.tiles.Tile;
import rmkl.tiles.TileBuilder;

import java.io.IOException;

class Instance {

    private final Server server;
    private final World world;
    private final ObjectMap<PjConnection, PjConnection> globalConnections = new ObjectMap<>();  // instance connection -> global connection
    private final Array<PjConnection> players = new Array<>();

    Instance(String path, String fileName, int port) throws IOException {
        TileBuilder tileBuilder = new TileBuilder(new ObjectMap<>());
        world = new World(path, fileName, tileBuilder);
        server = new PjServer();
        Network.register(server);
        server.addListener(new ConnectionListener());
        server.addListener(new GameplayListener());

        server.bind(Network.TCPport + port, Network.UDPport + port);
        server.start();
    }

    void update(float dt) {
        for (PjConnection c : players) {

            for (int i = c.pjCommands.size - 1; i >= 0; i--) {
                if (c.pjCommands.get(i).update(dt)) c.pjCommands.removeIndex(i);
            }

            server.sendToAllExceptUDP(c.getID(), c.snapshot);
        }

        world.updateTiles(dt);
    }

    private class ConnectionListener extends Listener {

        private final Network.MapHeader header;
        private final int columnsPerPacket;

        private final Array<Connection> queue = new Array<>();
        private Connection currentConnection;
        private int x;

        ConnectionListener() {

            // search map sizes
            int width = world.map.length;
            int height = world.map[0].length;
            columnsPerPacket = Math.min(Network.PACKET_LIMIT / height, width);//FIXME assumes that columns are <= Network.PACKET_LIMIT
            int numberOfMapPackets = width / columnsPerPacket + (width % columnsPerPacket > 0 ? 1 : 0);

            // cache header
            header = new Network.MapHeader();
            header.width = width;
            header.numberOfMapPackets = numberOfMapPackets;
        }

        @Override
        public void received(Connection connection, Object o) {
            PjConnection c = (PjConnection) connection;

            if (o instanceof Network.InstancePacket) {
                Network.InstancePacket p = (Network.InstancePacket) o;

                // get the connection id in the globalServer
                PjConnection globalConnection = Serversito.globalConnectionsBySessionID.get(p.sessionID);

                // validate the connection
                if (globalConnection == null) {
                    connection.close();
                    return;
                }

                // store the connection with its global id & set the identity
                PjConnection.initSnapshot(c, world.spawnSnapshot);
                players.add(c);
                globalConnections.put(c, globalConnection);
                c.sessionID = p.sessionID;
                c.name = globalConnection.name;

                // start init
                if (currentConnection == null) startStreaming(connection);
                else queue.add(connection);

            } else if (o instanceof Network.StreamMapPartRequest) {
                Network.MapStreamingPart p = new Network.MapStreamingPart();

                p.part = new char[columnsPerPacket][];
                for (int i = 0; i < columnsPerPacket; i++) {
                    p.part[i] = world.map[x];
                    x++;
                }
                c.sendTCP(p);

            } else if (o instanceof Network.StreamPjRequest) {

                // add player and tell everybody
                Network.AddPlayer p = PjConnection.addPlayer(c);
                server.sendToAllTCP(p);

                // tell the new boy about everybody
                for (PjConnection con : players) {
                    if (con.getID() == c.getID()) continue;
                    Network.AddPlayer q = PjConnection.addPlayer(con);
                    c.sendTCP(q);
                }

                reset();
            }
        }

        @Override
        public void disconnected(Connection connection) {
            PjConnection c = (PjConnection)connection;

            // remove the connection from this instance
            players.removeValue(c, true); //TODO test
            globalConnections.remove(c);

            // notify other players
            Network.RemovePlayer p = new Network.RemovePlayer();
            p.id = connection.getID();
            server.sendToAllTCP(p);

            // handle queue
            queue.removeValue(connection, false);
            if (connection == currentConnection) reset();
        }

        private void startStreaming(Connection connection) {
            currentConnection = connection;
            connection.sendTCP(header);

            // sends environment
            ColorAttribute ca = (ColorAttribute)world.environment.get(ColorAttribute.AmbientLight);
            DirectionalLight dl = ((DirectionalLightsAttribute)world.environment.get(DirectionalLightsAttribute.Type)).lights.peek();
            Network.EnvironmentPacket p = new Network.EnvironmentPacket();
            p.ambient = ca.color.a;
            p.dirX = dl.direction.x;
            p.dirY = dl.direction.y;
            p.dirZ = dl.direction.z;
            connection.sendTCP(p);
        }

        private void reset() {
            currentConnection = null;
            x = 0;
            if (queue.size > 0) startStreaming(queue.pop());
        }
    }

    private class GameplayListener extends Listener {

        @Override
        public void received(Connection connection, Object o) {
            PjConnection c = (PjConnection) connection;

            if (o instanceof Network.Snapshot) {
                Network.Snapshot p = (Network.Snapshot) o;

                c.snapshot.pos.set(p.pos);
                c.snapshot.angle = p.angle;
                c.snapshot.animation = p.animation;

                // check for special tiles
                Tile tile = Commons.tileFromCoords(c.snapshot.pos, world.grid);
                if (tile != null) {
                    tile.serverCollisionResponse(server, c);
                }

            } else if (o instanceof Network.Jump) {
                Network.Jump p = (Network.Jump) o;
                p.id = c.getID();
                server.sendToAllExceptUDP(p.id, p);

            }
        }

        @Override
        public void disconnected(Connection connection) {
            PjConnection c = (PjConnection) connection;

            // check if the pj was pressing a button and unpresses it
            Tile tile = Commons.tileFromCoords(c.snapshot.pos, world.grid);
            if (tile != null && tile.type() == Button.code) ((Button) tile).disconnected(c);
        }
    }

}
