package com.lazytechwork.mcsm;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;

public class MainController {

    @FXML
    private Button jarChoose;
    @FXML
    private TextField jarPath;
    @FXML
    private Slider ramSlider;
    @FXML
    private Label ramVal;

    public void initialize() {
        ramSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            ramVal.setText(((int)newValue.floatValue() * 1024) + "MB");
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
}
