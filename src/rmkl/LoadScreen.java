package rmkl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.VisProgressBar;
import com.kotcrab.vis.ui.widget.VisTable;

abstract class BaseLoadScreen extends ScreenAdapter {

    private final Stage stage;
    protected final VisProgressBar progressBar;

    BaseLoadScreen() {
        stage = new Stage();
        VisTable layout = new VisTable();
        layout.setFillParent(true);
        stage.addActor(layout);
        progressBar = new VisProgressBar(0, 1, 0.1f, false);
        layout.add(progressBar).row();
    }

    @Override
    public void show() {
        init();
    }

    @Override
    public void render(float dt) {
        Gdx.gl.glClearColor(0.05f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if(setProgressbarValue()) complete();

        stage.act();
        stage.draw();
    }

    abstract void init();
    abstract boolean setProgressbarValue();
    abstract void complete();

    @Override
    public void dispose() {
        stage.dispose();
    }
}

class MainLoadScreen extends BaseLoadScreen {

    private Launcher GAME;
    private AssetManager assets;

    MainLoadScreen(Launcher GAME) {
        this.GAME = GAME;
        assets = GAME.assets;
    }

    @Override
    void init() {
        TextureLoader.TextureParameter textureParameters = new TextureLoader.TextureParameter();
        textureParameters.genMipMaps = true;
        textureParameters.magFilter = Texture.TextureFilter.Linear;
        textureParameters.minFilter = Texture.TextureFilter.MipMapLinearLinear;
        textureParameters.wrapU = Texture.TextureWrap.Repeat;
        textureParameters.wrapV = Texture.TextureWrap.Repeat;

        assets.load("assets/tiles/wall.png", Texture.class, textureParameters);
        assets.load("assets/tiles/floor.jpg", Texture.class, textureParameters);
        assets.load("assets/tiles/wood.jpg", Texture.class, textureParameters);

        assets.load("assets/y_bot.g3dj", Model.class);
        assets.load("assets/spacesphere.obj", Model.class);
        assets.load("assets/tiles/floor.g3dj", Model.class);
        assets.load("assets/tiles/fence.g3dj", Model.class);
        assets.load("assets/tiles/wall.g3dj", Model.class);
        assets.load("assets/tiles/teleporter.g3dj", Model.class);
        assets.load("assets/tiles/door.g3dj", Model.class);
        assets.load("assets/tiles/error.g3dj", Model.class);
        assets.load("assets/tiles/button.g3dj", Model.class);
        assets.load("assets/tiles/twall.g3dj", Model.class);
        assets.load("assets/tiles/selection.g3dj", Model.class);
    }

    @Override
    boolean setProgressbarValue() {
        progressBar.setValue(assets.getProgress());
        return assets.update();
    }

    @Override
    void complete() {
        setAnisotropicFiltering();
        GAME.setScreenAndDispose(new MainMenuScreen(GAME));
    }

    private void setAnisotropicFiltering() {
        Array<Texture> textures = new Array<>();
        assets.getAll(Texture.class, textures);
        for(Texture texture : textures) {
            Anisotropy.setTextureAnisotropy(texture, 8);
        }
    }
}

class LoadGameScreen extends BaseLoadScreen {

    private final Launcher GAME;
    private final Clientesito gameScreen;
    float percent;

    LoadGameScreen(Launcher GAME, Clientesito gameScreen) {
        this.GAME = GAME;
        this.gameScreen = gameScreen;
    }

    @Override
    void init() {}

    @Override
    boolean setProgressbarValue() {
        progressBar.setValue(percent);
        return percent >= 1;
    }

    @Override
    void complete() {
        GAME.setScreenAndDispose(gameScreen);
    }
}
