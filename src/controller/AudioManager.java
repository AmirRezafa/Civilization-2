package controller;

import javax.sound.sampled.*;
import java.io.File;

public class AudioManager {
    private static AudioManager instance;
    private Clip backgroundMusic;
    private FloatControl volumeControl;

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    public void playBGM(String filePath) {
        try {
            File audioFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);

            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioStream);

            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);

            if (backgroundMusic.isControlSupported(FloatControl.Type.MASTER_GAIN))
                volumeControl = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);

            backgroundMusic.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setVolume(int volume) {
        if(volumeControl != null) {
            if(volume == 0)
                volumeControl.setValue(volumeControl.getMinimum());
            else {
                float dB = (float) (Math.log10(volume / 100.0) * 20.0);
                volumeControl.setValue(dB);
            }
        }
    }
}