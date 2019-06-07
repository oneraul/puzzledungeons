package rmkl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

class MainMenuScreen extends ScreenAdapter {

    private Launcher GAME;
    private Stage stage;

    MainMenuScreen(Launcher GAME) {
        this.GAME = GAME;
    }

    @Override
    public void show() {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);
        VisTable layout = new VisTable();
        layout.setFillParent(true);
        stage.addActor(layout);

        VisTextButton game_button = new VisTextButton("game");
        VisTextButton editor_button = new VisTextButton("editor");

        game_button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                new Clientesito(GAME);
            }
        });
        editor_button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                GAME.setScreenAndDispose(new EditorScreen(GAME));
            }
        });

        layout.add(game_button).width(150).fillX().row();
        layout.add(editor_button).fillX().padTop(10).row();
    }

    @Override
    public void render(float dt) {
        Gdx.gl.glClearColor(0.05f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act();
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
