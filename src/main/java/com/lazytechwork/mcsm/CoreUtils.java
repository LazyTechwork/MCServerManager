package com.lazytechwork.mcsm;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import java.io.IOException;

public class CoreUtils {

    public static Scene loadFXML(String filename) throws IOException {
        FXMLLoader loader = new FXMLLoader(Launcher.class.getResource("/fxml/" + filename + ".fxml"));
        return new Scene(loader.load());
    }

}
