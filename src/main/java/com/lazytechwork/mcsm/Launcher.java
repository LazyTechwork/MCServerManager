package com.lazytechwork.mcsm;

import javafx.application.Application;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

import java.io.IOException;

public class Launcher extends Application {

    private static final Logger LOG = Logger.getLogger(Launcher.class);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        LOG.info("Starting app...");
        primaryStage.setTitle("Minecraft Server Manager");
        primaryStage.setResizable(true);
        try {
            primaryStage.setScene(CoreUtils.loadFXML("Main"));
        } catch (IOException e) {
            LOG.error("Error while loading FXML", e);
        }
        primaryStage.show();
    }
}
