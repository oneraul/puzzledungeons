package rmkl;

import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

public class Network {

    static final int TCPport = 1993, UDPport = 1993;
    static final String ip = "localhost";
    static final int PACKET_LIMIT = 400;

    static void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(int.class);
        kryo.register(float.class);
        kryo.register(char.class);
        kryo.register(char[].class);
        kryo.register(char[][].class);
        kryo.register(String.class);
        kryo.register(Vector2.class);
        kryo.register(InstancePacket.class);
        kryo.register(PartyLeader.class);
        kryo.register(PartyRequest.class);
        kryo.register(JoinParty.class);
        kryo.register(LeaveParty.class);
        kryo.register(Snapshot.class);
        kryo.register(Teleported.class);
        kryo.register(Jump.class);
        kryo.register(AddPlayer.class);
        kryo.register(RemovePlayer.class);
        kryo.register(PressButton.class);
        kryo.register(UnpressButton.class);
        kryo.register(OpenDoor.class);
        kryo.register(CloseDoor.class);
        kryo.register(MapName.class);
        kryo.register(MapSelected.class);
        kryo.register(MapSent.class);
        kryo.register(MapHeader.class);
        kryo.register(EnvironmentPacket.class);
        kryo.register(StreamMapPartRequest.class);
        kryo.register(ErrorStreamingMap.class);
        kryo.register(MapStreamingPart.class);
        kryo.register(StreamMapCommandPartRequest.class);
        kryo.register(MapCommandsStreamingPart.class);
        kryo.register(StreamPjRequest.class);
    }

    public static abstract class Packet {}

    public static class InstancePacket extends Packet {
        public int port;
        public String sessionID;
    }

    public static abstract class PartyPacket extends Packet {}
    public static class PartyLeader extends PartyPacket {}
    public static class PartyRequest extends PartyPacket {
        public String member, leader;
    }
    public static class JoinParty extends PartyPacket {
        public String member, leader;
    }
    public static class LeaveParty extends PartyPacket {
        public String member;
    }

    public static class Snapshot extends Packet {
        public int id;
        public Vector2 pos;
        public float angle;
        public String animation;
    }
    public static class Teleported extends Snapshot {}

    public static class Jump extends Packet {
        public int id;
    }

    public static class AddPlayer extends Packet {
        public int id;
        public String name;
        public Vector2 pos;
        public float angle;
    }
    public static class RemovePlayer extends Packet {
        public int id;
    }

    public static abstract class ButtonPacket extends Packet {
        public int buttonX, buttonY;
    }
    public static class PressButton extends ButtonPacket {}
    public static class UnpressButton extends ButtonPacket {}

    public static abstract class DoorPacket extends Packet {
        public int doorX, doorY;
    }
    public static class OpenDoor extends DoorPacket {}
    public static class CloseDoor extends DoorPacket {}

    public static class MapName extends Packet {
        // this one is for informing the players about the available maps
        public String name;
    }
    public static class MapSelected extends Packet {
        public String name;
    }
    public static class MapSent extends Packet {
        public String name;
    }

    public static class MapHeader extends Packet {
        public int width;
        public int numberOfMapPackets;
        public int numberOfCommandsPackets;
    }
    public static class EnvironmentPacket extends Packet {
        public float ambient;
        public float dirX, dirY, dirZ;
    }
    public static class StreamMapPartRequest extends Packet {}
    public static class ErrorStreamingMap extends Packet {}
    public static class MapStreamingPart extends Packet {
        public char[][] part;
    }
    public static class StreamMapCommandPartRequest extends Packet {}
    public static class MapCommandsStreamingPart extends Packet {
        public String part;
    }
    public static class StreamPjRequest extends Packet {}
}
