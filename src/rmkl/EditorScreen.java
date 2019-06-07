package rmkl;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ObjectMap;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.kotcrab.vis.ui.widget.*;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.SingleFileChooserListener;
import rmkl.shaders.MyShaderProvider;
import rmkl.tiles.*;
import rmkl.tiles.descriptors.*;

import java.io.IOException;

class EditorScreen extends ScreenAdapter {

    private AssetManager assets;
    private ShapeRenderer shaper;
    private ModelBatch modelBatch;
    private PerspectiveCamera cam;
    private FileHandle mapFile, commandsFile;
    private Stage stage;
    private VisWindow selectedWindow;
    private VisSlider environmentAmbientSlider;
    private VisSlider environmentDirLightX;
    private VisSlider environmentDirLightY;
    private VisSlider environmentDirLightZ;

    private World world;
    private final ObjectMap<GridPoint2, TileDescriptor> digits;
    private final SpawnDescriptor spawnDescriptor;
    private final Vector3 cursor, secondaryCursor;
    private TileDescriptor selectedDescriptor;
    private char tool;

    private boolean editing = true;

    private final TileBuilder tileBuilder = new TileBuilder();
    private final GridPoint2 tmpGridPoint2 = new GridPoint2();
    private final Vector3 tmpVector3 = new Vector3();

    {
        spawnDescriptor = new SpawnDescriptor();
        cursor = new Vector3();
        secondaryCursor = new Vector3();
        digits = new ObjectMap<>();
    }

    EditorScreen(Launcher GAME) {
        this.assets = GAME.assets;
    }

    @Override
    public void show() {
        shaper = new ShapeRenderer();
        modelBatch = new ModelBatch(new MyShaderProvider());

        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(0, 10, 0);
        cam.lookAt(0, 0, 0);
        cam.near = 1f;
        cam.far = 300f;
        cam.update();

        Tile.load(assets);
        TileDescriptor.loadModelInstances(assets.get("assets/tiles/selection.g3dj", Model.class));


        // GUI
        stage = new Stage();
        final VisTable layout = new VisTable();
        stage.addActor(layout);
        layout.setFillParent(true);

        final MenuBar menuBar = new MenuBar();
        layout.add(menuBar.getTable()).expandX().fillX().top().colspan(2).row();

        final VisWindow newmapWindow = new VisWindow("New");
        final NumberSelector widthSelector = new NumberSelector("width:", 20, 10, 400, 1);
        final NumberSelector heightSelector = new NumberSelector("height:", 20, 10, 400, 1);
        final VisTextButton newmapButton = new VisTextButton("create");
        final VisTextButton cancelNewmapButton = new VisTextButton("cancel");
        newmapWindow.add(widthSelector).row();
        newmapWindow.add(heightSelector).row();
        newmapWindow.add(newmapButton);
        newmapWindow.add(cancelNewmapButton);
        newmapWindow.setSize(newmapWindow.getPrefWidth(), newmapWindow.getPrefHeight());

        newmapButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                restart((int)widthSelector.getValue(), (int)heightSelector.getValue());
                newmapWindow.remove();
            }
        });
        cancelNewmapButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                newmapWindow.remove();
            }
        });


        final FileChooser fileChooserOpen = new FileChooser(FileChooser.Mode.OPEN);
        final FileChooser fileChooserSave = new FileChooser(FileChooser.Mode.SAVE);
        fileChooserOpen.setListener(new SingleFileChooserListener() {
            @Override
            protected void selected(FileHandle fileHandle) {
                setFileHandles(fileHandle);

                try{
                    load();
                } catch(IOException e) {
                    Dialogs.showErrorDialog(stage, "Error cargando los archivos del mapa", e);
                    e.printStackTrace();
                }
            }
        });
        fileChooserSave.setListener(new SingleFileChooserListener() {
            @Override
            protected void selected(FileHandle fileHandle) {
                if(fileHandle.extension().equals("txt")) {
                    setFileHandles(fileHandle);
                    exportMap(mapFile, world.map);
                    exportCommands(commandsFile);

                } else Dialogs.showErrorDialog(stage, "file not valid");
            }
        });

        final Menu fileMenu = new Menu("File");
        fileMenu.addItem(new MenuItem("New", new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                Dialogs.showConfirmDialog(stage, "Sure?", "All work not saved will be lost", new String[] {"Yes", "No"}, new Boolean[] {true, false},
                    agreed -> { if(agreed) { stage.addActor(newmapWindow); } });
            }
        }));
        fileMenu.addItem(new MenuItem("Load", new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                stage.addActor(fileChooserOpen.fadeIn());
            }
        }));
        fileMenu.addItem(new MenuItem("Save", new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                if(mapFile == null) {
                    stage.addActor(fileChooserSave.fadeIn());

                } else {
                    exportMap(mapFile, world.map);
                    exportCommands(commandsFile);
                }
            }
        }));
        fileMenu.addItem(new MenuItem("Publish", new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                try {
                    publishMap();
                } catch(IOException e) {
                    Dialogs.showErrorDialog(stage, "Error connecting to the server", e);
                }
            }
        }));
        menuBar.addMenu(fileMenu);

        final VisWindow environmentWindow = new VisWindow("Environment");
        environmentWindow.addCloseButton();
        environmentWindow.setPosition(Gdx.graphics.getWidth()/2-environmentWindow.getWidth()/2, Gdx.graphics.getHeight()/2-environmentWindow.getHeight()/2);
        environmentAmbientSlider = new VisSlider(0, 1, 0.005f, false);
        environmentDirLightX = new VisSlider(-1, 1, 0.005f, false);
        environmentDirLightY = new VisSlider(-1, 1, 0.005f, false);
        environmentDirLightZ = new VisSlider(-1, 1, 0.005f, false);
        environmentAmbientSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                ColorAttribute ambient = (ColorAttribute)world.environment.get(ColorAttribute.AmbientLight);
                ambient.color.a = environmentAmbientSlider.getValue();
            }
        });
        environmentDirLightX.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                DirectionalLight dl = ((DirectionalLightsAttribute)world.environment.get(DirectionalLightsAttribute.Type)).lights.peek();
                dl.direction.x = environmentDirLightX.getValue();
            }
        });
        environmentDirLightY.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                DirectionalLight dl = ((DirectionalLightsAttribute)world.environment.get(DirectionalLightsAttribute.Type)).lights.peek();
                dl.direction.y = environmentDirLightY.getValue();
            }
        });
        environmentDirLightZ.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                DirectionalLight dl = ((DirectionalLightsAttribute)world.environment.get(DirectionalLightsAttribute.Type)).lights.peek();
                dl.direction.z = environmentDirLightZ.getValue();
            }
        });
        environmentWindow.add("Ambient");
        environmentWindow.add(environmentAmbientSlider).row();
        environmentWindow.add("DirX");
        environmentWindow.add(environmentDirLightX).row();
        environmentWindow.add("DirY");
        environmentWindow.add(environmentDirLightY).row();
        environmentWindow.add("DirZ");
        environmentWindow.add(environmentDirLightZ);


        final Menu menu = new Menu("Menu");
        menu.addItem(new MenuItem("Spawn point", new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                selectedDescriptor = spawnDescriptor;
                selectedWindow.clear();
                selectedWindow.add(spawnDescriptor.x + ", " + spawnDescriptor.z).row();
                selectedDescriptor.selectedWindow(selectedWindow);
                selectedWindow.setVisible(true);
            }
        }));
        menu.addItem(new MenuItem("Environment", new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                stage.addActor(environmentWindow);
            }
        }));
        menuBar.addMenu(menu);

        final VisWindow toolsWindow = new VisWindow("Tiles");
        toolsWindow.setMovable(false);
        selectedWindow = new VisWindow("Selection");
        selectedWindow.setMovable(false);
        selectedWindow.setVisible(false);
        layout.add(toolsWindow).expand().left().bottom().pad(10);
        layout.add(selectedWindow).expand().right().bottom().pad(10);

        final VisTextButton floorButton = new VisTextButton("Floor");
        final VisTextButton wallButton = new VisTextButton("Wall");
        final VisTextButton twallButton = new VisTextButton("TWall");
        final VisTextButton fenceButton = new VisTextButton("Fence");
        final VisTextButton doorButton = new VisTextButton("Door");
        final VisTextButton buttonButton = new VisTextButton("Button");
        final VisTextButton teleporterButton = new VisTextButton("Teleporter");


        floorButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                tool = Floor.code;
            }
        });
        wallButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                tool = Wall.code;
            }
        });
        twallButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                tool = TWall.code;
            }
        });
        fenceButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                tool = Fence.code;
            }
        });
        doorButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                tool = Door.code;
            }
        });
        buttonButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                tool = Button.code;
            }
        });
        teleporterButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                tool = Teleporter.code;
            }
        });

        toolsWindow.add(floorButton).fillX().padBottom(3).row();
        toolsWindow.add(wallButton).fillX().padBottom(3).row();
        toolsWindow.add(twallButton).fillX().padBottom(3).row();
        toolsWindow.add(fenceButton).fillX().padBottom(3).row();
        toolsWindow.add(doorButton).fillX().padBottom(3).row();
        toolsWindow.add(buttonButton).fillX().padBottom(3).row();
        toolsWindow.add(teleporterButton).fillX().padBottom(3).row();


        // INPUT
        InputMultiplexer multiplexer = new InputMultiplexer();
        Gdx.input.setInputProcessor(multiplexer);
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if(!editing) return true;

                // only acts if something is selected
                if(selectedDescriptor == null) return false;

                // if the callback doesn't return true deselects the descriptor
                if(!selectedDescriptor.clickCallback(secondaryCursor)) {
                    selectedDescriptor = null;
                    selectedWindow.setVisible(false);
                }

                // doesn't let the input fall to the next processor
                return true;
            }
        });
        multiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if(!editing) return true;

                // select
                if(button == 1) {
                    int x = (int)cursor.x;
                    int z = (int)cursor.z;
                    if(world.grid[x][z] != null) {

                        tmpGridPoint2.set(x, z);
                        TileDescriptor descriptor = digits.get(tmpGridPoint2);
                        if(descriptor == null) return true;

                        selectedDescriptor = descriptor;

                        selectedWindow.clear();
                        selectedWindow.add(descriptor.x + ", " + descriptor.z).row();

                        selectedDescriptor.selectedWindow(selectedWindow);
                        selectedWindow.setVisible(true);
                    }

                } else {
                    paint();
                }

                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if(!editing) return true;

                paint();
                return true;
            }

            private void paint() {
                int x = (int)cursor.x;
                int z = (int)cursor.z;
                if(world.grid[x][z] == null || world.grid[x][z].type() != tool) {

                    // erase previous digits
                    tmpGridPoint2.set(x, z);
                    TileDescriptor descriptor = digits.get(tmpGridPoint2);
                    if(descriptor != null) digits.remove(tmpGridPoint2);

                    // paint tile
                    world.map[x][z] = tool;
                    world.grid[x][z] = tileBuilder.build(tool, x, z, world.map);
                    refreshAround(x, z);

                    // set digits
                    TileDescriptor tileDescriptor = newDigit(x, z);
                    if(tileDescriptor != null)
                        digits.put(new GridPoint2(x, z), tileDescriptor);
                }
            }

            @Override
            public boolean keyDown(int keycode) {
                if(!editing) return true;

                switch(keycode) {
                    case Input.Keys.SPACE: tool = Floor.code; break;
                    case Input.Keys.X: tool = Wall.code; break;
                    case Input.Keys.M: tool = TWall.code; break;
                    case Input.Keys.H: tool = Fence.code; break;
                    case Input.Keys.P: tool = Door.code; break;
                    case Input.Keys.B: tool = Button.code; break;
                    case Input.Keys.T: tool = Teleporter.code; break;
                }
                return true;
            }
        });

        restart(20, 20);
    }

    @Override
    public void render(float dt) {

        continousInput(dt);
        stage.act();
        world.updateTiles(dt);

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        shaper.setProjectionMatrix(cam.combined);
        shaper.begin(ShapeRenderer.ShapeType.Line);
        shaper.setColor(Color.DARK_GRAY);
        for(int x = 0; x <= world.grid.length; x++) shaper.line(x, 0, 0, x, 0, world.grid[0].length);
        for(int z = 0; z <= world.grid[0].length; z++) shaper.line(0, 0, z, world.grid.length, 0, z);
        shaper.setColor(Color.RED);     shaper.line(Vector3.Zero, Vector3.X);
        shaper.setColor(Color.GREEN);   shaper.line(Vector3.Zero, Vector3.Y);
        shaper.setColor(Color.BLUE);    shaper.line(Vector3.Zero, Vector3.Z);
        shaper.end();

        modelBatch.begin(cam);
        for(int x = 0; x < world.grid.length; x++) {
            for(int z = 0; z < world.grid[x].length; z++) {
                Tile tile = world.grid[x][z];
                if(tile != null && Commons.isVisible(cam, tile.getModelInstance())) {
                    tile.render(modelBatch, world.environment);
                }
            }
        }
        if(selectedDescriptor != null) {
            selectedDescriptor.extraDrawCalls(modelBatch);
        }
        modelBatch.end();

        shaper.begin(ShapeRenderer.ShapeType.Line);
        shaper.setColor(Color.WHITE);
        shaper.box(cursor.x, cursor.y, cursor.z+1, 1, 2, 1);
        if(selectedDescriptor != null && selectedDescriptor.useSecondaryCursor) {
            shaper.setColor(Color.CYAN);
            shaper.box(secondaryCursor.x, secondaryCursor.y, secondaryCursor.z + 1, 1, 2, 1);
        }
        shaper.end();

        stage.draw();
    }

    private void continousInput(float dt) {
        if(!editing) return;

        // camera movement
        final float v = 10f * dt;
        if(Gdx.input.isKeyPressed(Input.Keys.W)) cam.position.z -= v;
        if(Gdx.input.isKeyPressed(Input.Keys.A)) cam.position.x -= v;
        if(Gdx.input.isKeyPressed(Input.Keys.S)) cam.position.z += v;
        if(Gdx.input.isKeyPressed(Input.Keys.D)) cam.position.x += v;
        cam.update();

        // cursor
        int screenX = Gdx.input.getX(), screenY = Gdx.input.getY();
        Ray ray = cam.getPickRay(screenX, screenY);
        final float distance = -ray.origin.y / ray.direction.y;
        tmpVector3.set(ray.direction).scl(distance).add(ray.origin);
        tmpVector3.x = MathUtils.clamp((int)tmpVector3.x, 0, world.grid.length - 1);
        tmpVector3.z = MathUtils.clamp((int)tmpVector3.z, 0, world.grid[(int) cursor.x].length - 1);

        if(selectedDescriptor == null) {
            cursor.set(tmpVector3);
        } else if(selectedDescriptor.useSecondaryCursor) {
            secondaryCursor.set(tmpVector3);
        }
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        stage.dispose();
        shaper.dispose();
    }

    private void refreshAround(int x, int z) {
        refreshTile(x-1, z-1);
        refreshTile(  x, z-1);
        refreshTile(x+1, z-1);
        refreshTile(x-1,   z);
        refreshTile(  x,   z);
        refreshTile(x+1,   z);
        refreshTile(x-1, z+1);
        refreshTile(  x, z+1);
        refreshTile(x+1, z+1);
    }

    private void refreshTile(int x, int z) {
        // early exit: out of bounds or tile == null
        if(x < 0 || x >= world.grid.length || z < 0 || z >= world.grid[0].length
        || world.grid[x][z] == null) return;

        // only refresh tiles with bitmasking
        Tile tile = world.grid[x][z];
        if(tile instanceof AutoTilable)
            world.grid[x][z] = tileBuilder.build(tile.type(), x, z, world.map);
    }

    private void setFileHandles(FileHandle fileHandle) {

        String commandsPath = fileHandle.parent().path() + "/" + fileHandle.nameWithoutExtension() + "_commands.txt";

        mapFile = fileHandle;

        if(!Gdx.files.absolute(commandsPath).exists()) Gdx.files.absolute(commandsPath).writeString(" ", false);
        commandsFile = Gdx.files.absolute(commandsPath);
    }

    private void load() throws IOException {
        String path = mapFile.parent().path() + "/";
        String name = mapFile.nameWithoutExtension();

        world = new World(path, name);
        TileDescriptor.grid = world.grid;
        spawnDescriptor.x = (int)world.spawnSnapshot.pos.x;
        spawnDescriptor.z = (int)world.spawnSnapshot.pos.y;

        digits.clear();
        EditorMapLoader.loadCommands(commandsFile.path(), digits, world.environment, spawnDescriptor);

        setEnvironmentWindow(world.environment);
    }

    private void setEnvironmentWindow(Environment environment) {
        ColorAttribute ca = (ColorAttribute)environment.get(ColorAttribute.AmbientLight);
        DirectionalLight dl = ((DirectionalLightsAttribute)environment.get(DirectionalLightsAttribute.Type)).lights.peek();

        environmentAmbientSlider.setValue(ca.color.a);
        environmentDirLightX.setValue(dl.direction.x);
        environmentDirLightY.setValue(dl.direction.y);
        environmentDirLightZ.setValue(dl.direction.z);
    }

    private void restart(int mapWidth, int mapHeight) {
        world = new World(mapWidth, mapHeight);
        TileDescriptor.grid = world.grid;
        digits.clear();
        spawnDescriptor.x = spawnDescriptor.z = 0;
        spawnDescriptor.setModelInstance();
        tool = Wall.code;
        selectedDescriptor = null;
        mapFile = commandsFile = null;

        for(int x = 0; x < world.map.length; x++) {
            for(int y = 0; y < world.map[x].length; y++) {
                world.map[x][y] = Floor.code;
                world.grid[x][y] = tileBuilder.build(Floor.code, x, y, world.map);
            }
        }

        //default environment
        world.environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.2f, 0.2f));
        world.environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, 0.25f, -0.6f, -0.6f));
        setEnvironmentWindow(world.environment);
    }

    static void exportMap(FileHandle file, char[][] map) {
        String out = "";

        for(int y = 0; y < map[0].length; y++) {
            for(int x = 0; x < map.length; x++) {
                out += map[x][y];
            }

            if(map[0].length-y > 1) out += '\n'; //the check is to avoid an empty line at the end
        }

        file.writeString(out, false);
    }

    private void exportCommands(FileHandle file) {
        String out = serializeCommands();
        file.writeString(out, false);
    }

    private String serializeCommands() {
        String out = "{\n" +

                "    \"environment\" : {\n" +
                serializeEnvironment(world.environment) +
                "    },\n" +

                "    \"0\" : {\n" +
                spawnDescriptor.serialize() +
                "    },\n";

        int id = 1;
        for(TileDescriptor descriptor : digits.values().toArray()) {
            String str = "    \"" + id + "\" : {\n";
            str += descriptor.serialize();
            str += "    },\n";

            out += str;
            id++;
        }

        out += "}";

        return out;
    }

    private String serializeEnvironment(Environment environment) {
        ColorAttribute ca = (ColorAttribute)environment.get(ColorAttribute.AmbientLight);
        DirectionalLight dl = ((DirectionalLightsAttribute)world.environment.get(DirectionalLightsAttribute.Type)).lights.peek();

        return  "        \"ambient\" : " + ca.color.a + ",\n" +
                "        \"dirX\" : " + dl.direction.x + ",\n" +
                "        \"dirY\" : " + dl.direction.y + ",\n" +
                "        \"dirZ\" : " + dl.direction.z + "\n";

    }

    private TileDescriptor newDigit(int x, int z) {

        TileDescriptor descriptor = null;

        switch(world.map[x][z]) {
            case Teleporter.code: descriptor = new TeleporterDescriptor(); break;
            case Door.code: descriptor = new DoorDescriptor(); break;
            case Button.code: descriptor = new ButtonDescriptor(); break;
        }

        if(descriptor == null) return null; //early exit, the tile doesn't need a descriptor

        descriptor.x = x;
        descriptor.z = z;

        return descriptor;
    }

    private void publishMap() throws IOException {

        // network setup
        Client client = new Client();
        Network.register(client);
        client.start();
        client.addListener(new Listener() {

            private final char[][] map = world.map;
            private String commands = serializeCommands();
            private int columnsPerPacket;
            private int x;
            private final VisWindow window = new VisWindow("Publishing");
            private VisProgressBar progressBar;
            private boolean streaming;

            @Override
            public void connected(Connection connection) {
                editing = false;

                // search map sizes
                int width = map.length;
                int height = map[0].length;
                columnsPerPacket = Math.min(Network.PACKET_LIMIT/height, width);
                int numberOfMapPackets = width/columnsPerPacket + (width%columnsPerPacket > 0 ? 1 : 0);
                int numberOfCommandPackets = commands.length()/Network.PACKET_LIMIT + (commands.length()%Network.PACKET_LIMIT > 0 ? 1 : 0);

                // send header
                Network.MapHeader header = new Network.MapHeader();
                header.width = width;
                header.numberOfMapPackets = numberOfMapPackets;
                header.numberOfCommandsPackets = numberOfCommandPackets;
                client.sendTCP(header);

                // ui
                stage.addActor(window);
                window.add("waiting for server...");
                progressBar = new VisProgressBar(0, numberOfMapPackets+numberOfCommandPackets, 1, false);
            }

            @Override
            public void received(Connection connection, Object o) {
                if(o instanceof Network.StreamMapPartRequest) {

                    // set ui
                    if (!streaming) {
                        streaming = true;
                        window.clearChildren();
                        window.add(progressBar).row();
                        window.setSize(window.getPrefWidth(), window.getPrefHeight());
                    }

                    sendMapPart();

                } else if(o instanceof Network.StreamMapCommandPartRequest) {
                    sendCommandsPart();

                } else if(o instanceof Network.MapSent) {
                    setWindowFinished("The map has been published with the code " + ((Network.MapSent)o).name);
                    editing = true;

                } else if(o instanceof Network.ErrorStreamingMap) {
                    setWindowFinished("ERROR");
                }
            }

            private void setWindowFinished(String message) {
                VisTextButton closeButton = new VisTextButton("ok");
                closeButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent changeEvent, Actor actor) {
                        window.remove();
                    }
                });

                window.clearChildren();
                window.add(message).row();
                window.add(closeButton);
                window.setSize(window.getPrefWidth(), window.getPrefHeight());
            }

            private void sendMapPart() {
                Network.MapStreamingPart p = new Network.MapStreamingPart();

                p.part = new char[columnsPerPacket][];
                for(int i = 0; i < columnsPerPacket; i++) {
                    p.part[i] = map[x];
                    x++;
                }
                client.sendTCP(p);

                progressBar.setValue(progressBar.getValue()+1);
            }

            private void sendCommandsPart() {
                Network.MapCommandsStreamingPart p = new Network.MapCommandsStreamingPart();

                if(commands.length() >= Network.PACKET_LIMIT) {
                    String commandsPart = commands.substring(0, Network.PACKET_LIMIT);
                    commands = commands.substring(Network.PACKET_LIMIT);
                    p.part = commandsPart;

                } else {
                    p.part = commands;
                }

                client.sendTCP(p);
            }

            @Override
            public void disconnected(Connection connection) {
                editing = true;
            }
        });
        client.connect(5000, Network.ip, Network.TCPport-1, Network.UDPport-1);
    }
}
