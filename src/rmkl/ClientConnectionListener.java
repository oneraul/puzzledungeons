package rmkl;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

class ClientConnectionListener extends Listener {

    private final Clientesito clientesito;
    private final Client client;
    private final LoadGameScreen loadGameScreen;

    private char[][] map;
    private Network.EnvironmentPacket environmentPacket;
    private int numberOfMapPackets;
    private int x;
    private int currentPacket;

    ClientConnectionListener(Clientesito clientesito, Client client, LoadGameScreen loadGameScreen) {
        this.clientesito = clientesito;
        this.client = client;
        this.loadGameScreen = loadGameScreen;
    }

    @Override
    public void received(Connection connection, Object o) {

        if(o instanceof Network.MapHeader) {
            Network.MapHeader p = (Network.MapHeader) o;
            map = new char[p.width][];
            numberOfMapPackets = p.numberOfMapPackets;

            connection.sendTCP(new Network.StreamMapPartRequest());

        } else if(o instanceof Network.EnvironmentPacket) {
            environmentPacket = (Network.EnvironmentPacket)o;

        } else if(o instanceof Network.MapStreamingPart) {
            Network.MapStreamingPart p = (Network.MapStreamingPart)o;

            for(char[] column : p.part) {
                map[x] = column;
                x++;
            }

            currentPacket++;
            if(currentPacket < numberOfMapPackets) {
                // send back an ack and continue streaming
                connection.sendTCP(new Network.StreamMapPartRequest());

            } else if(currentPacket == numberOfMapPackets) {
                // map complete. request pj init
                connection.sendTCP(new Network.StreamPjRequest());

            } else {
                //TODO error handling (received more packets than expected)
            }

            loadGameScreen.percent = setLoadingBar(currentPacket);

        } else if(o instanceof Network.AddPlayer) {
            Network.AddPlayer p = (Network.AddPlayer)o;

            Pj3D pj = new Pj3D(clientesito.assets.get("assets/y_bot.g3dj", Model.class), p);
            pj.name = p.name;
            pj.pos.set(p.pos);
            pj.angle = p.angle;

            clientesito.players.put(p.id, pj);
            clientesito.me = pj;

            loadGameScreen.percent = setLoadingBar(currentPacket+1);

            finished();
        }
    }

    private int setLoadingBar(int packet) {
        // I arbitrarily add two extra packets to the total: one for AddPlayer and another for finished()
        return packet/(numberOfMapPackets+2);
    }

    private void finished() {
        client.removeListener(this);
        client.addListener(new ClientGameListener(clientesito));

        clientesito.world = new World(map);
        clientesito.world.initChunksAround(clientesito.me.pos.x, clientesito.me.pos.y);
        clientesito.setInput();

        clientesito.world.environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.2f, environmentPacket.ambient));
        clientesito.world.environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, environmentPacket.dirX, environmentPacket.dirY, environmentPacket.dirZ));

        // change back to the clientesito screen
        loadGameScreen.percent = 1;
    }

    @Override
    public void disconnected(Connection connection) {
        client.removeListener(this);
    }
}