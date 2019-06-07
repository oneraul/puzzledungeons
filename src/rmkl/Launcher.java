package rmkl;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.file.FileChooser;

class Launcher extends Game {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("3D Game");
        cfg.setBackBufferConfig(8, 8, 8, 8, 16, 0, 8);
        cfg.useVsync(false);
        cfg.setWindowedMode(800, 600);
        new Lwjgl3Application(new Launcher(), cfg);
    }

    final AssetManager assets = new AssetManager();

    @Override
    public void create() {
        VisUI.load();
        FileChooser.setFavoritesPrefsName("rmkl");

        this.setScreen(new MainLoadScreen(this));
    }

    void setScreenAndDispose(Screen newScreen) {
        getScreen().dispose();
        setScreen(newScreen);
    }

    @Override
    public void dispose() {
        getScreen().dispose();
        assets.dispose();
        VisUI.dispose();
    }
}
