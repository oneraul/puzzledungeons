package rmkl;

import com.badlogic.gdx.utils.*;
import com.esotericsoftware.kryonet.Server;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;

public class Serversito {
    public static void main (String[] args) {
        try {
            new Serversito();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    static final String mapsPath = "C:/Users/oneraul/Desktop/juego/";
    static final String mapsDBpath = mapsPath + "maps_db.txt";

    final static ObjectMap<String, PjConnection> globalConnectionsBySessionID = new ObjectMap<>();  // sessionID -> global connection
    final static ObjectMap<String, PjConnection> globalConnectionsByName = new ObjectMap<>();              // name -> global connection
    final Array<Instance> instances;

    final Server globalServer;
    final ObjectMap<String, char[][]> maps;
    final JSONObject mapsDB;
    final JSONArray mapsDBarray;

    private Serversito() throws IOException {
        globalConnectionsBySessionID.clear();
        globalConnectionsByName.clear();

        final Server editorServer = new MapStreamingServer();
        globalServer = new PjServer();
        Network.register(editorServer);
        Network.register(globalServer);

        maps = new ObjectMap<>();
        mapsDB = loadMapsDB(mapsDBpath);
        mapsDBarray = (JSONArray)mapsDB.get("maps");
        for(Object m : mapsDBarray) {
            JSONObject map = (JSONObject)m;
            String name = (String)map.get("name");
            maps.put(name, null);
        }

        final Instance openWorldInstance = new Instance(mapsPath, "OpenWorld", 1);
        instances = new Array<>();
        instances.add(openWorldInstance);

        editorServer.addListener(new MapStreamingListener(this));
        editorServer.bind(Network.TCPport-1, Network.UDPport-1);
        editorServer.start();


        globalServer.addListener(new GlobalServerListener(this));
        globalServer.bind(Network.TCPport, Network.UDPport);
        globalServer.start();


        System.out.println("Server running...");

        // Game loop
        final float TICK_RATE = 0.15f;
        while(true) {

            gameLoop(TICK_RATE);

            try {
                Thread.sleep((long)(TICK_RATE * 1000));
            } catch(InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private static JSONObject loadMapsDB(String path) {
        try {
            JSONParser parser = new JSONParser();
            return (JSONObject)parser.parse(new FileReader(path));

        } catch(ParseException | IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return null;
    }

    private void gameLoop(float dt) {
        for(Instance instance : instances) {
            instance.update(dt);
        }
    }
}
