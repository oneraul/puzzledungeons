package rmkl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Model;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import rmkl.pjCommands.Interpolate;
import rmkl.tiles.Button;
import rmkl.tiles.Door;

class ClientGameListener extends Listener {

    private Clientesito clientesito;

    ClientGameListener(Clientesito clientesito) {
        this.clientesito = clientesito;
    }

    @Override
    public void received(Connection connection, Object o) {
        if(o instanceof Network.Snapshot) {
            Network.Snapshot p = (Network.Snapshot)o;
            if(clientesito.players.containsKey(p.id)) {
                Pj3D player = clientesito.players.get(p.id);

                if(o instanceof Network.Teleported) {
                    player.pos.set(p.pos);
                    player.angle = p.angle;
                    if(player != clientesito.me) player.interpolate.newSnapshot = p;
                    else clientesito.getController3D().syncAngle();
                }

                //interpolation
                if(player != clientesito.me) {
                    player.interpolate.oldSnapshot = player.interpolate.newSnapshot;
                    player.interpolate.newSnapshot = p;
                    player.interpolate.timer = 0;
                }

                player.animationController.animate(p.animation, -1, null, 0.1f);
            }

        } else if(o instanceof Network.Jump) {
            clientesito.players.get(((Network.Jump)o).id).jump();

        } else if(o instanceof Network.AddPlayer) {
            Network.AddPlayer p = (Network.AddPlayer)o;

            Pj3D pj = new Pj3D(clientesito.assets.get("assets/y_bot.g3dj", Model.class), p);
            pj.name = p.name;
            pj.pos.set(p.pos);
            pj.angle = p.angle;

            Interpolate interpolate = new Interpolate(pj);
            pj.interpolate = interpolate;
            pj.commands.add(interpolate);

            Gdx.app.postRunnable(() -> clientesito.players.put(p.id, pj));


        } else if(o instanceof Network.RemovePlayer) {
            Gdx.app.postRunnable(() -> clientesito.players.remove(((Network.RemovePlayer) o).id));

        } else if(o instanceof Network.ButtonPacket) {
            Network.ButtonPacket p = (Network.ButtonPacket)o;
            Button button = (Button)clientesito.world.grid[p.buttonX][p.buttonY];

            if(p instanceof Network.PressButton) button.press();
            else if(p instanceof Network.UnpressButton) button.unpress();

        } else if(o instanceof Network.DoorPacket) {
            Network.DoorPacket p = (Network.DoorPacket)o;
            Door door = (Door)clientesito.world.grid[p.doorX][p.doorY];

            if(p instanceof Network.OpenDoor) door.setLocked(false);
            else if(p instanceof Network.CloseDoor) door.setLocked(true);
        }
    }
}