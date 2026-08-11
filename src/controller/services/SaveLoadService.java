package controller.services;

import controller.GameController;
import model.GameState;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SaveLoadService {
    private static final String SAVE_DIR = "saves";
    private static final int AUTOSAVE_SLOT = 0;

    public boolean save(GameController gc, int slot) {
        GameState state = gc.captureState();

        File dir = new File(SAVE_DIR);
        if (!dir.exists()) dir.mkdirs();

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(slotFile(slot)))) {
            out.writeObject(state);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean load(GameController gc, int slot) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(slotFile(slot)))) {
            GameState state = (GameState) in.readObject();
            gc.restoreState(state);
            return true;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void autosave(GameController gc) {
        save(gc, AUTOSAVE_SLOT);
    }

    private String slotFile(int slot) {
        return SAVE_DIR + "/slot" + slot + ".sav";
    }
}
