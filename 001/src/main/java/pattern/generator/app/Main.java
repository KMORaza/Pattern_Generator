package pattern.generator.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/pattern/generator/app/MainUI.fxml"));
        Parent root = loader.load();

        String css = """
            .root {
                -fx-background-color: #c0c0c0;
                -fx-font-family: "Consolas";
                -fx-font-size: 14px;
            }
            .toolbar {
                -fx-padding: 5;
                -fx-border-color: #ffffff #808080 #808080 #ffffff;
                -fx-border-width: 2;
                -fx-background-color: #c0c0c0;
            }
            .button-3d {
                -fx-background-color: #c0c0c0;
                -fx-border-color: #ffffff #808080 #808080 #ffffff;
                -fx-border-width: 2;
                -fx-padding: 3 10 3 10;
                -fx-effect: dropshadow(gaussian, #000000, 2, 0, 1, 1);
            }
            .button-3d:hover {
                -fx-background-color: #d0d0d0;
            }
            .button-3d:pressed {
                -fx-border-color: #808080 #ffffff #ffffff #808080;
                -fx-background-color: #b0b0b0;
            }
            .titled-pane {
                -fx-border-color: #808080 #ffffff #ffffff #808080;
                -fx-background-color: #c0c0c0;
            }
            .titled-pane > .title {
                -fx-background-color: #c0c0c0;
                -fx-border-color: #ffffff #808080 #808080 #ffffff;
                -fx-padding: 2;
            }
            .titled-pane > .content {
                -fx-border-color: #808080 #ffffff #ffffff #808080;
                -fx-background-color: #c0c0c0;
                -fx-padding: 5;
            }
            .label {
                -fx-text-fill: #000000;
            }
            .spinner {
                -fx-border-color: #808080 #ffffff #ffffff #808080;
                -fx-background-color: #ffffff;
            }
            .combo-box {
                -fx-border-color: #808080 #ffffff #ffffff #808080;
                -fx-background-color: #ffffff;
            }
            .table-view {
                -fx-border-color: #808080 #ffffff #ffffff #808080;
                -fx-background-color: #ffffff;
            }
            .canvas {
                -fx-border-color: #808080 #ffffff #ffffff #808080;
                -fx-background-color: #ffffff;
            }
            .scroll-pane {
                -fx-border-color: #808080 #ffffff #ffffff #808080;
                -fx-background-color: #c0c0c0;
            }
            .scroll-pane > .viewport {
                -fx-background-color: #ffffff;
            }
            .status-bar {
                -fx-border-color: #ffffff #808080 #808080 #ffffff;
                -fx-background-color: #c0c0c0;
                -fx-padding: 2;
            }
            .status-label {
                -fx-text-fill: #000000;
            }
            .text-field {
                -fx-border-color: #808080 #ffffff #ffffff #808080;
                -fx-background-color: #ffffff;
                -fx-padding: 2;
            }
            .generate-button {
                -fx-background-color: #c0c0c0;
                -fx-border-color: #ffffff #808080 #808080 #ffffff;
                -fx-border-width: 2;
                -fx-padding: 3 10 3 10;
                -fx-effect: dropshadow(gaussian, #000000, 2, 0, 1, 1);
            }
            .generate-button:hover {
                -fx-background-color: #d0d0d0;
            }
            .generate-button:pressed {
                -fx-border-color: #808080 #ffffff #ffffff #808080;
                -fx-background-color: #b0b0b0;
            }
            .tooltip {
                -fx-background-color: #ffffe0;
                -fx-text-fill: #000000;
                -fx-border-color: #808080;
                -fx-border-width: 1;
                -fx-padding: 5;
            }
        """;

        Scene scene = new Scene(root, 1000, 600);
        scene.getStylesheets().add("data:text/css," + css);
        primaryStage.setTitle("Digital Pattern Generator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}