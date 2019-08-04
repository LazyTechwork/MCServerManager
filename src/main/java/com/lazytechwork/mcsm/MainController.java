package com.lazytechwork.mcsm;

import javafx.event.ActionEvent;
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

    private static Process serverProcess;
    private static Thread consoleReader;
    private static Logger LOG = Logger.getLogger(MainController.class);

    public void initialize() {
        ramSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            ramVal.setText(((int) newValue.floatValue() * 1024) + "MB");
        });
    }

    public void jarChoose(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR files (*.jar)", "*.jar"));
        File file = fileChooser.showOpenDialog(jarChoose.getScene().getWindow());
        if (file != null) {
            jarPath.setText(file.getPath());
        }
    }

    public void startServer(ActionEvent actionEvent) {
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
            try (BufferedReader serverConsole = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()));) {
                String line;
                while ((line = serverConsole.readLine()) != null) {
                    console.appendText(line + "\n");
                }
            } catch (IOException e) {
                LOG.error("Error while reading console", e);
            }
        });
        consoleReader.setDaemon(true);
        consoleReader.start();
    }

    public void stopServer(ActionEvent actionEvent) {
        if (serverProcess == null) return;
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(serverProcess.getOutputStream()))) {
            writer.write("stop");
            writer.flush();
        } catch (IOException e) {
            LOG.error("Error while writing to console", e);
        }
        LOG.info("Detaching process and terminating console reader");
        serverProcess = null;
        consoleReader.interrupt();
    }
}
