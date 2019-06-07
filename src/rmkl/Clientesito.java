package rmkl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntMap;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import rmkl.shaders.MyShaderProvider;
import rmkl.tiles.Tile;

import java.io.IOException;

class Clientesito extends ScreenAdapter {

    private Launcher GAME;
    private final ModelBatch modelBatch;
    private final SpriteBatch spriteBatch;
    private final BitmapFont font;
    private final GameGUI GUI;
    final AssetManager assets;

    private ModelInstance skybox;
    private Camera3D cam3D;
    private CameraController3D controller3D;

    final IntMap<Pj3D> players = new IntMap<>();    // modifications to the map should be made through Gdx.app.postRunnable so the happen in the main thread and don't interfere with rendering
    World world;
    Pj3D me;

    private Client client;
    static Client CLIENT, GLOBAL_CLIENT;

    Clientesito(Launcher game) {
        this.GAME = game;
        this.assets = game.assets;

        Tile.load(assets);
        font = new BitmapFont();
        GUI = new GameGUI(GAME);
        modelBatch = new ModelBatch(new MyShaderProvider());
        spriteBatch = new SpriteBatch();
        skybox = new ModelInstance(assets.get("assets/spacesphere.obj", Model.class));
        cam3D = new Camera3D();

        /* NETWORK ----- */

        final Client globalClient = new Client();
        Network.register(globalClient);
        globalClient.start();
        globalClient.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object o) {
                if(o instanceof Network.InstancePacket) {
                    Network.InstancePacket p = (Network.InstancePacket)o;
                    Gdx.app.postRunnable(() -> connectToInstance(p));

                } else if(o instanceof Network.PartyPacket) {

                    if(o instanceof Network.PartyLeader) {
                        GUI.setPartyLeader();

                    } else if(o instanceof Network.PartyRequest) {
                        Network.PartyRequest p = (Network.PartyRequest)o;
                        GUI.partyRequest(p.leader);

                    } else if(o instanceof Network.JoinParty) {
                        Network.JoinParty p = (Network.JoinParty)o;
                        GUI.addPartyMember(p.member);

                    } else if(o instanceof Network.LeaveParty) {
                        Network.LeaveParty p = (Network.LeaveParty)o;
                        if(p.member.equals(me.name)) GUI.leaveParty();
                        else GUI.removePartyMember(p.member);
                    }

                } else if(o instanceof Network.MapName) {
                    Network.MapName p = (Network.MapName)o;
                    GUI.addMap(p.name);

                } else if(o instanceof Network.MapSelected) {
                    Network.MapSelected p = (Network.MapSelected)o;
                    GUI.setMap(p.name);
                }
            }
        });

        try {
            globalClient.connect(5000, Network.ip, Network.TCPport, Network.UDPport);
            Clientesito.GLOBAL_CLIENT = globalClient;
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void clearInstance() {
        players.clear();
        me = null;
        if(client != null) {
            client.close();
            client = null;
            CLIENT = null;
        }
    }

    private void connectToInstance(Network.InstancePacket p) {
        LoadGameScreen loadGameScreen = new LoadGameScreen(GAME, this);
        GAME.setScreen(loadGameScreen);

        clearInstance();

        client = new Client();
        Network.register(client);
        client.start();
        client.addListener(new ClientConnectionListener(this, client, loadGameScreen));

        try {
            client.connect(5000, Network.ip, p.port, p.port);
            Clientesito.CLIENT = client;
            client.sendTCP(p);

        } catch(IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    void setInput() {
        controller3D = new CameraController3D(cam3D, me);
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(GUI.stage);
        multiplexer.addProcessor(GUI.getInputProcessor());
        multiplexer.addProcessor(controller3D);
        Gdx.input.setInputProcessor(multiplexer);
    }

    @Override
    public void render(float dt) {
        logic(dt);
        display();
    }

    private void logic(float dt) {

        // update world
        world.updateTiles(dt);

        // process my input
        controller3D.update(world.grid, dt);
        Commons.checkCollisions(me, world.grid);
        client.sendUDP(Pj3D.snapshot(me));

        // manage world chunks
        world.swapChunks(me.pos.x, me.pos.y);

        // update players
        for(Pj3D pj : players.values()) {
            pj.update(dt);
        }

        // update skybox
        skybox.transform.setTranslation(cam3D.pos.x, 0, cam3D.pos.y);

        // update GUI
        GUI.update();
    }

    private void display() {
        // clear screen
        Gdx.gl.glClearColor(0.05f, 0.1f, 0.15f, 1);
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // init modelBatch
        modelBatch.begin(cam3D.perspectiveCamera);
        modelBatch.render(skybox);

        // draw world
        world.draw(modelBatch, cam3D.perspectiveCamera);

        // draw players
        for(Pj3D pj : players.values()) {
            modelBatch.render(pj.modelInstance, world.environment);
        }

        modelBatch.end();

        // draw texts
        spriteBatch.begin();
        final Vector3 text = new Vector3(), textTmp = new Vector3(); //FIXME gc
        for(Pj3D pj : players.values()) {
            if(pj != me) {
                text.set(cam3D.perspectiveCamera.project(textTmp.set(pj.pos.x, pj.y+1, pj.pos.y)));
                font.draw(spriteBatch, pj.name, text.x-pj.name.length()*4f, text.y);
            }
        }
        spriteBatch.end();

        // draw GUI
        GUI.stage.draw();
    }

    @Override
    public void dispose() {
        font.dispose();
        spriteBatch.dispose();
        modelBatch.dispose();
        GUI.dispose();

        if(client != null) client.close();
        if(CLIENT != null) CLIENT.close();
        if(GLOBAL_CLIENT != null) GLOBAL_CLIENT.close();
    }

    @Override
    public void resize(int width, int height) {
        GUI.stage.getViewport().update(width, height, true);
    }

    CameraController3D getController3D() {
        return controller3D;
    }
}
