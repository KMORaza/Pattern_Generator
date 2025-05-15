module pattern.generator.app.patterngenerator {
    requires javafx.controls;
    requires javafx.fxml;


    opens pattern.generator.app to javafx.fxml;
    exports pattern.generator.app;
}