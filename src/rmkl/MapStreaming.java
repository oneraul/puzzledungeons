package rmkl;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;

class MapStreamingServer extends Server {
    @Override
    protected Connection newConnection() {
        return new MapStreamingConnection();
    }
}

class MapStreamingConnection extends Connection {
    char[][] map;
    String commands = "";
    int x;
    int numberOfMapPackets;
    int numberOfCommandsPackets;
    int currentPacket;
}

class MapStreamingListener extends Listener {

    private final Serversito serversito;
    private boolean busy;
    private final Array<MapStreamingConnection> queue = new Array<>();
    private MapStreamingConnection currentStreamer;

    MapStreamingListener(Serversito serversito) {
        this.serversito = serversito;
    }

    @Override
    public void received(Connection connection, Object o) {
        MapStreamingConnection c = (MapStreamingConnection)connection;

        if(o instanceof Network.MapHeader) {
            // setup
            Network.MapHeader p = (Network.MapHeader)o;
            c.map = new char[p.width][];
            c.numberOfMapPackets = p.numberOfMapPackets;
            c.numberOfCommandsPackets = p.numberOfCommandsPackets;

            //if ready tell them to start streaming
            if(!busy) requestStartStreaming(c);

            //if not ready add them to queue
            else {
                queue.add(c);
                //TODO send status in queue
            }

        } else if(o instanceof Network.MapStreamingPart) {
            Network.MapStreamingPart p = (Network.MapStreamingPart)o;

            for(char[] column : p.part) {
                c.map[c.x] = column;
                c.x++;
            }

            c.currentPacket++;
            if(c.currentPacket < c.numberOfMapPackets) {
                // send back an ack and continue streaming
                c.sendTCP(new Network.StreamMapPartRequest());

            } else if(c.currentPacket == c.numberOfMapPackets) {
                // map complete. request commands
                c.currentPacket = 0;
                c.sendTCP(new Network.StreamMapCommandPartRequest());

            } else {
                //error. send back an error message and close
                c.sendTCP(new Network.ErrorStreamingMap());
                c.close();
            }

        } else if(o instanceof Network.MapCommandsStreamingPart) {
            Network.MapCommandsStreamingPart p = (Network.MapCommandsStreamingPart)o;

            c.commands += p.part;

            c.currentPacket++;
            if(c.currentPacket < c.numberOfCommandsPackets) {
                // send back an ack and continue streaming
                c.sendTCP(new Network.StreamMapCommandPartRequest());

            } else if(c.currentPacket == c.numberOfCommandsPackets) {

                addMapToDataBase(c);

                //reset listener
                busy = false;
                //if there is someone queued start their stream
                if(queue.size > 0) requestStartStreaming(queue.pop());

            } else {
                //error. send back an error message and close
                c.sendTCP(new Network.ErrorStreamingMap());
                c.close();
            }
        }
    }

    private void requestStartStreaming(MapStreamingConnection c) {
        busy = true;
        currentStreamer = c;
        c.sendTCP(new Network.StreamMapPartRequest());
    }

    private void addMapToDataBase(MapStreamingConnection c) {
        // generate a name for the map TODO instead should gather some information (author, date, etc)
        String name = ""+ MathUtils.random(9)+MathUtils.random(9)+MathUtils.random(9);

        // export map
        FileHandle newMapFile = new FileHandle(Serversito.mapsPath + name + ".txt");
        EditorScreen.exportMap(newMapFile, c.map);

        // export commands
        FileHandle newMapCommandsFile = new FileHandle(Serversito.mapsPath + name + "_commands.txt");
        newMapCommandsFile.writeString(c.commands, false);

        // add map to db
        try(FileWriter file = new FileWriter(Serversito.mapsDBpath)) {

            JSONObject out = new JSONObject();
            out.put("name", name);

            serversito.mapsDBarray.add(out);
            file.write(serversito.mapsDB.toJSONString());

            serversito.maps.put(name, null);

        } catch(IOException e) {
            e.printStackTrace();
        }

        // send confirmation dialog to the sender
        Network.MapSent m = new Network.MapSent();
        m.name = name;
        c.sendTCP(m);

        // update players with the new map
        Network.MapName q = new Network.MapName();
        q.name = name;
        serversito.globalServer.sendToAllTCP(q);
    }

    @Override
    public void disconnected(Connection connection) {
        MapStreamingConnection c = (MapStreamingConnection)connection;
        queue.removeValue(c, false); // TODO test!
        if(c == currentStreamer) {
            if(queue.size > 0) requestStartStreaming(queue.pop());
            else busy = false;
        }
    }
}
