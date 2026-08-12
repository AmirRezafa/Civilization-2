package controller.services;

import controller.GameController;
import model.GameState;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class SaveLoadService {
    private static final String SAVE_DIR = "saves";
    private static final int AUTOSAVE_SLOT = 0;

    public boolean save(GameController gc, int slot) {
        GameState state = gc.captureState();

        File dir = new File(SAVE_DIR);
        if (!dir.exists()) dir.mkdirs();

        File target = new File(slotFile(slot));
        File tempFile = new File(slotFile(slot) + ".tmp");

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(tempFile))) {
            out.writeObject(state);
        } catch (IOException e) {
            e.printStackTrace();
            tempFile.delete();
            return false;
        }

        try {
            Files.move(tempFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            tempFile.delete();
            return false;
        }
    }

    public boolean load(GameController gc, int slot) {
        File file = new File(slotFile(slot));
        if (!file.exists()) return false;

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            GameState state = (GameState) in.readObject();
            if (state.saveVersion > GameState.CURRENT_SAVE_VERSION) {
                System.out.println("Save file is from a newer, unsupported version: " + state.saveVersion);
                return false;
            }
            gc.restoreState(state);
            return true;
        } catch (InvalidClassException e) {
            System.out.println("Save file is incompatible with the current game version.");
            return false;
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
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
