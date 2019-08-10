package com.lazytechwork.mcsm;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.apache.log4j.Logger;

import java.io.*;

public class MainController {

    @FXML
    private Button jarChoose;
    @FXML
    private TextField jarPath;
    @FXML
    private Slider ramSlider;
    @FXML
    private Label ramVal;
    @FXML
    private TextArea console;
    @FXML
    private TextField cmdField;
    @FXML
    private Button sendButton;
    @FXML
    private Button stopButton;
    @FXML
    private Button startButton;

    private static Process serverProcess;
    private static Thread consoleReader;
    private static Logger LOG = Logger.getLogger(MainController.class);
    private static BufferedWriter consoleWriter;
    private static boolean waitingForClose = false;

    public void initialize() {
        ramSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            ramVal.setText(((int) newValue.floatValue() * 1024) + "MB");
        });
        setControl(false);
        Platform.runLater(() -> {
            ramSlider.getScene().getWindow().setOnCloseRequest(e -> {
                LOG.info("Starting closing procedure");
                cmdField.setDisable(true);
                sendButton.setDisable(true);
                startButton.setDisable(true);
                ramSlider.setDisable(true);
                jarPath.setDisable(true);
                jarChoose.setDisable(true);
                stopButton.setDisable(true);
                LOG.info("Checking is server started");
                if (serverProcess != null) {
                    LOG.info("Server online, stopping...");
                    stopServer();
                    Platform.setImplicitExit(true);
                }
            });
        });
    }

    public void jarChoose() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR files (*.jar)", "*.jar"));
        File file = fileChooser.showOpenDialog(jarChoose.getScene().getWindow());
        if (file != null) {
            jarPath.setText(file.getPath());
        }
    }

    public void startServer() {
        LOG.info("Got signal to start server. Checking is server path correct..");
        if (jarPath.getText().isEmpty() && !new File(jarPath.getText()).exists()) return;
        LOG.info("Checking is server already started");
        if (serverProcess != null && serverProcess.isAlive()) return;
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-Xmx" + Math.round(ramSlider.getValue() * 1024) + "M", "-jar", jarPath.getText(), "nogui");
            pb.directory(new File(jarPath.getText()).getParentFile());
            LOG.info("Set working directory: " + pb.directory().getAbsolutePath());
            LOG.info("Checking is EULA signed");
            File eula = new File(new File(jarPath.getText()).getParentFile().getAbsolutePath() + "/eula.txt");
            if (!eula.exists()) {
                LOG.info("EULA doesn't exists, creating..");
                eula.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(eula));
                writer.write("eula=true");
                writer.close();
            } else {
                LOG.info("EULA file found");
                BufferedReader reader = new BufferedReader(new FileReader(eula));
                String line;
                boolean signed = false;
                while ((line = reader.readLine()) != null) {
                    if (line.toLowerCase().equals("eula=true".toLowerCase())) signed = true;
                }
                if (!signed) {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(eula, false));
                    writer.write("eula=true");
                    writer.close();
                }
                reader.close();
            }
            LOG.info("Starting MC Server with cmd: " + String.join(" ", pb.command()));
            serverProcess = pb.start();
        } catch (IOException e) {
            LOG.error("There is PIZDEC!", e);
            return;
        }
        consoleReader = new Thread(() -> {
            console.setText("");
            consoleWriter = new BufferedWriter(new OutputStreamWriter(serverProcess.getOutputStream()));
            try (BufferedReader serverConsole = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()));) {
                String line;
                while ((line = serverConsole.readLine()) != null) {
                    String finalLine = line;
                    Platform.runLater(() -> console.appendText(finalLine + "\n"));
                }
            } catch (IOException e) {
                LOG.error("Error while reading console", e);
            }
        });
        consoleReader.setDaemon(true);
        consoleReader.start();
        setControl(true);
    }

    public void sendCommandToServer(String cmd) {
        if (serverProcess == null || consoleWriter == null || cmd.isEmpty()) return;
        try {
            consoleWriter.write(cmd);
            consoleWriter.newLine();
            consoleWriter.flush();
        } catch (IOException e) {
            LOG.error("Error while writing to console", e);
        }
    }

    public void stopServer() {
        sendCommandToServer("stop");
        LOG.info("Detaching process and terminating console reader");
        serverProcess = null;
        try {
            consoleWriter.close();
        } catch (IOException e) {
            LOG.error("Error while closing console writer", e);
        }
        consoleReader.interrupt();
        setControl(false);
    }

    public void setControl(boolean state) {
        cmdField.setDisable(!state);
        sendButton.setDisable(!state);
        startButton.setDisable(state);
        ramSlider.setDisable(state);
        jarPath.setDisable(state);
        jarChoose.setDisable(state);
        stopButton.setDisable(!state);
    }

    public void sendCmd() {
        if (serverProcess == null) return;
        if (cmdField.getText().isEmpty()) return;
        sendCommandToServer(cmdField.getText());
        LOG.info("Command sent: " + cmdField.getText());
        cmdField.setText("");
    }

    public void clearConsole() {
        console.setText("");
    }
}
