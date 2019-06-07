package rmkl;

import com.badlogic.gdx.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.kotcrab.vis.ui.widget.*;

class GameGUI implements Disposable {
    private InputAdapter inputAdapter;
    Stage stage;

    private VisLabel fpsLabel;

    private final VisWindow party_window;
    private final VisSelectBox<String> mapsSelectbox;
    private final VisTextButton createPartyButton;
    private final VisTextButton leavePartyButton;
    private final VisTextButton inviteButton;
    private final VisTextField inviteTextfield;
    private final VisList<String> partyNames;

    GameGUI(Launcher game) {
        stage = new Stage();
        VisTable layout = new VisTable();
        layout.setFillParent(true);
        stage.addActor(layout);

        layout.add(fpsLabel = new VisLabel()).expand().top().left().row();

        createPartyButton = new VisTextButton("create party");
        inviteTextfield = new VisTextField();
        inviteButton = new VisTextButton("invite");
        leavePartyButton = new VisTextButton("leave party");
        partyNames = new VisList<>();
        mapsSelectbox = new VisSelectBox<>();

        party_window = new VisWindow("Party");
        stage.addActor(party_window);
        party_window.setVisible(false);
        party_window.add(createPartyButton).row();
        party_window.setSize(party_window.getPrefWidth(), party_window.getPrefHeight());

        VisTextButton instanceButton = new VisTextButton("instance");
        VisTextButton partyWindowButton = new VisTextButton("social");
        VisCheckBox fullscreen_checkbox = new VisCheckBox("fullscreen");
        VisTextButton exitButton = new VisTextButton("exit");

        VisWindow menu_window = new VisWindow("Menu");
        stage.addActor(menu_window);
        menu_window.setMovable(false);
        menu_window.setVisible(false);
        menu_window.setPosition(Gdx.graphics.getWidth()/2 - menu_window.getWidth()/2,
                Gdx.graphics.getHeight()/2 - menu_window.getHeight()/2);
        menu_window.add(instanceButton).fillX().row();
        menu_window.add(mapsSelectbox).fillX().row();
        menu_window.add(partyWindowButton).fillX().row();
        menu_window.add(fullscreen_checkbox).fillX().row();
        menu_window.add(exitButton).fillX().fillX().row();

        instanceButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                Clientesito.GLOBAL_CLIENT.sendTCP(new Network.InstancePacket());
                menu_window.setVisible(false);
            }
        });
        partyWindowButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                party_window.setVisible(true);
                menu_window.setVisible(false);
            }
        });
        createPartyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                Clientesito.GLOBAL_CLIENT.sendTCP(new Network.PartyLeader());
                createPartyButton.setDisabled(true);
            }
        });
        inviteButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                Network.PartyRequest p = new Network.PartyRequest();
                p.member = inviteTextfield.getText();
                Clientesito.GLOBAL_CLIENT.sendTCP(p);
                inviteTextfield.setText("");
            }
        });
        leavePartyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                Clientesito.GLOBAL_CLIENT.sendTCP(new Network.LeaveParty());
                leavePartyButton.setDisabled(true);
            }
        });
        mapsSelectbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                Network.MapSelected p = new Network.MapSelected();
                p.name = mapsSelectbox.getSelected();
                Clientesito.GLOBAL_CLIENT.sendTCP(p);
            }
        });
        fullscreen_checkbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                final boolean fullscreen = fullscreen_checkbox.isChecked();

                if(fullscreen) {
                    Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
                    Gdx.graphics.setFullscreenMode(displayMode);

                } else {
                    Gdx.graphics.setWindowedMode(800, 600);
                }
            }
        });
        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                Gdx.app.exit();
            }
        });

        inputAdapter = new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                switch(keycode) {
                    case Input.Keys.ESCAPE:
                        menu_window.setVisible(!menu_window.isVisible());
                        return true;
                    case Input.Keys.O:
                        party_window.setVisible(!party_window.isVisible());
                        return true;
                }
                return false;
            }
        };
    }

    InputProcessor getInputProcessor() {
        return inputAdapter;
    }

    void update() {
        fpsLabel.setText(Gdx.graphics.getFramesPerSecond() + "fps");
        stage.act();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    void setPartyLeader() {
        createPartyButton.setDisabled(false);
        party_window.clearChildren();
        party_window.add(inviteTextfield);
        party_window.add(inviteButton).row();
        joinParty();
        mapsSelectbox.setDisabled(false);
    }

    private void joinParty() {
        party_window.add(partyNames).colspan(2).row();
        party_window.add(leavePartyButton).colspan(2).row();
        party_window.add(mapsSelectbox).colspan(2).row();
        mapsSelectbox.setDisabled(true);
        party_window.setSize(party_window.getPrefWidth(), party_window.getPrefHeight());
    }

    void leaveParty() {
        party_window.clearChildren();
        partyNames.clearItems();
        party_window.add(createPartyButton);
        party_window.setSize(party_window.getPrefWidth(), party_window.getPrefHeight());
        leavePartyButton.setDisabled(false);
    }

    void addPartyMember(String member) {
        Array<String> party = partyNames.getItems();
        party.add(member);

        // not sure why, but setItems() doesn't work with a libGDX Array
        String[] strs = new String[party.size];
        for(int i = 0; i < strs.length; i++) {
            strs[i] = party.get(i);
        }

        partyNames.setItems(strs);
        party_window.setSize(party_window.getPrefWidth(), party_window.getPrefHeight());
    }

    void removePartyMember(String member) {
        Array<String> party = partyNames.getItems();
        party.removeValue(member, false);

        // not sure why, but setItems() doesn't work with a libGDX Array
        String[] strs = new String[party.size];
        for(int i = 0; i < strs.length; i++) {
            strs[i] = party.get(i);
        }

        partyNames.setItems(strs);
        party_window.setSize(party_window.getPrefWidth(), party_window.getPrefHeight());
    }

    void partyRequest(String partyLeader) {
        Dialogs.showConfirmDialog(stage, "Let's party!", "Party request from " + partyLeader + " received. Do you want to accept?", new String[]{"yes", "no"}, new Boolean[]{true, false},
        accept -> {
            if(accept) {
                Network.JoinParty p = new Network.JoinParty();
                p.leader = partyLeader;
                Clientesito.GLOBAL_CLIENT.sendTCP(p);

                createPartyButton.remove();
                joinParty();
            }
        });
    }

    void addMap(String mapName) {
        Array<String> maps = mapsSelectbox.getItems();
        maps.add(mapName);

        // not sure why, but setItems() doesn't work with a libGDX Array
        String[] strs = new String[maps.size];
        for(int i = 0; i < strs.length; i++) {
            strs[i] = maps.get(i);
        }

        mapsSelectbox.setItems(strs);
    }

    void setMap(String mapName) {
        //FIXME mapsSelectbox.setSelected(mapName);
    }
}
