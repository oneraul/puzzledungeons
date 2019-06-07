package rmkl;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ObjectMap;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;

class GlobalServerListener extends Listener {

    private final Serversito serversito;
    private int nextPort = 1;

    GlobalServerListener(Serversito serversito) {
        this.serversito = serversito;
    }

    @Override
    public void connected(Connection connection) {
        PjConnection c = (PjConnection)connection;

        c.name = "name"+ MathUtils.random(10000); // TODO fetch name from persistent database
        if(c.getID() == 1) c.name = "primero";
        else if(c.getID() == 2) c.name = "segundo";
        else if(c.getID() == 3) c.name = "tercero";
        c.sessionID = ""+MathUtils.random(9)+MathUtils.random(9)+MathUtils.random(9)+MathUtils.random(9); //FIXME should be unique ids
        Serversito.globalConnectionsBySessionID.put(c.sessionID, c);
        Serversito.globalConnectionsByName.put(c.name, c);

        // send the connection the list of available instance maps
        for(ObjectMap.Entry<String, char[][]> map : serversito.maps.entries()) {
            Network.MapName p = new Network.MapName();
            p.name = map.key;
            c.sendTCP(p);
        }

        // tell the connection to connect to the Open World instance
        Network.InstancePacket p = new Network.InstancePacket();
        p.port = Network.TCPport+1;
        p.sessionID = c.sessionID;
        connection.sendTCP(p);
    }

    @Override
    public void received(Connection connection, Object o) {
        PjConnection c = (PjConnection)connection;

        if(o instanceof Network.InstancePacket) {

            // TODO ports should be pooled and freed when no longer needed

            int port;

            // if the player belongs to a party and they are assigned to an instance, send their port
            if(c.party != null && c.party.instancePort > 1) port = c.party.instancePort;

            // else instantiate a new instance
            else {
                // get the selected instance map
                String map = c.party == null ? c.map : c.party.map;

                // if it is the open world, assign the global instance at port +1
                if(map.equals("OpenWorld")) port = 1;

                else {
                    port = getNextPort();
                    try {
                        serversito.instances.add(new Instance(Serversito.mapsPath, map, port));
                    } catch (IOException e) {
                        System.out.println("Error initializing instance");
                        e.printStackTrace();
                        return;
                    }

                    // if the player belongs to a party, assign them the port
                    if (c.party != null) c.party.instancePort = port;
                }
            }

            // send instance packet
            Network.InstancePacket p = new Network.InstancePacket();
            p.port = Network.TCPport + port;
            p.sessionID = c.sessionID;
            connection.sendTCP(p);

        } else if(o instanceof Network.PartyPacket) {

            if(o instanceof Network.PartyLeader) {
                if(c.party == null) {
                    c.sendTCP(new Network.PartyLeader());
                    c.party = new PartyGroup();
                    c.party.leader = c;
                    c.party.addMember(c);
                }

            } else if(o instanceof Network.PartyRequest) {
                Network.PartyRequest p = (Network.PartyRequest)o;

                if(Serversito.globalConnectionsByName.containsKey(p.member)) {
                    PjConnection newMember = Serversito.globalConnectionsByName.get(p.member);
                    p.leader = c.name;
                    newMember.sendTCP(p);
                }


            } else if(o instanceof Network.JoinParty) {
                Network.JoinParty p = (Network.JoinParty)o;

                if(c.party == null) {
                    if(Serversito.globalConnectionsByName.containsKey(p.leader)) {
                        PartyGroup party = Serversito.globalConnectionsByName.get(p.leader).party;
                        party.addMember(c);
                    }
                }

            } else if(o instanceof Network.LeaveParty) {
                if(c.party != null) {
                    c.party.leaveParty(c);
                }
            }

        } else if(o instanceof Network.MapSelected) {
            Network.MapSelected p = (Network.MapSelected)o;
            if(c.party == null) {
                c.map = p.name;
                c.sendTCP(p);
            } else if(c == c.party.leader) {
                c.party.setMap(p);
            }
        }
    }

    @Override
    public void disconnected(Connection connection) {
        PjConnection c = (PjConnection)connection;
        Serversito.globalConnectionsBySessionID.remove(c.sessionID);
        Serversito.globalConnectionsByName.remove(c.name);
        if(c.party != null) c.party.leaveParty(c);
    }

    private int getNextPort() {
        return ++nextPort;
    }
}
