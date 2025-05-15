package pattern.generator.app;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import java.io.File;

public class PatternController {

    @FXML private TableView<int[]> patternTable;
    @FXML private Canvas waveformCanvas;
    @FXML private Spinner<Integer> channelSpinner;
    @FXML private Spinner<Integer> stepSpinner;
    @FXML private Spinner<Double> sampleRateSpinner;
    @FXML private ComboBox<String> bitWidthCombo;
    @FXML private ComboBox<String> ioStandardCombo;
    @FXML private Button clearButton;
    @FXML private Button randomizeButton;
    @FXML private Button saveButton;
    @FXML private Button loadButton;
    @FXML private Label statusLabel;
    @FXML private Label voltageInfoLabel;
    @FXML private ComboBox<String> patternTypeCombo;
    @FXML private Spinner<Double> dutyCycleSpinner;
    @FXML private Spinner<Double> patternFrequencySpinner;
    @FXML private TextField expressionField;
    @FXML private Button generateButton;

    private PatternModel model;
    private static final double WAVEFORM_HEIGHT = 50.0;
    private static final double TIME_STEP_WIDTH = 50.0;
    private static final double MINIMUM_CANVAS_WIDTH = 800.0;
    private static final double MINIMUM_CANVAS_HEIGHT = 200.0;

    @FXML
    public void initialize() {
        model = new PatternModel(16, 4, 8, 1.0, "TTL", "Manual", 50.0, 1000.0, "");

        bitWidthCombo.setItems(FXCollections.observableArrayList("8-bit", "16-bit", "32-bit"));
        bitWidthCombo.setValue("16-bit");
        channelSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 16, 4));
        stepSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 32, 8));
        sampleRateSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 100.0, 1.0, 0.1));
        ioStandardCombo.setItems(FXCollections.observableArrayList("TTL", "LVTTL", "LVCMOS", "LVDS"));
        ioStandardCombo.setValue("TTL");
        patternTypeCombo.setItems(FXCollections.observableArrayList("Manual", "PWM", "PRBS", "Clock", "Expression"));
        patternTypeCombo.setValue("Manual");
        dutyCycleSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 100.0, 50.0, 1.0));
        patternFrequencySpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(1.0, 1_000_000.0, 1000.0, 100.0));
        expressionField.setText("");
        expressionField.setTooltip(new Tooltip("Supported: sin(2*pi*t*freq), cos(2*pi*t*freq), t (ramp), constant (e.g., 0.7)"));
        statusLabel.setText("Ready");
        updateVoltageInfo();
        updatePatternControlsVisibility();

        setupTable();
        updateWaveform();

        // Event handlers
        bitWidthCombo.setOnAction(e -> updateChannelLimit());
        channelSpinner.valueProperty().addListener((obs, oldVal, newVal) -> resizePattern());
        stepSpinner.valueProperty().addListener((obs, oldVal, newVal) -> resizePattern());
        sampleRateSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            model.setSampleRate(newVal);
            updateWaveform();
            statusLabel.setText("Sample rate set to " + newVal + " MHz (" + String.format("%.2f", 1000/newVal) + " ns/step)");
        });
        ioStandardCombo.setOnAction(e -> {
            model.setIoStandard(ioStandardCombo.getValue());
            updateVoltageInfo();
            statusLabel.setText("I/O Standard set to " + ioStandardCombo.getValue());
        });
        patternTypeCombo.setOnAction(e -> {
            model.setPatternType(patternTypeCombo.getValue());
            updatePatternControlsVisibility();
            setupTable(); // Update table editability
            statusLabel.setText("Pattern type set to " + patternTypeCombo.getValue());
        });
        dutyCycleSpinner.valueProperty().addListener((obs, oldVal, newVal) -> model.setDutyCycle(newVal));
        patternFrequencySpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            model.setPatternFrequency(newVal);
            updateGenerateButtonState();
        });
        expressionField.textProperty().addListener((obs, oldVal, newVal) -> {
            model.setExpression(newVal);
            updateGenerateButtonState();
        });
        clearButton.setOnAction(e -> clearPattern());
        randomizeButton.setOnAction(e -> randomizePattern());
        saveButton.setOnAction(e -> savePattern());
        loadButton.setOnAction(e -> loadPattern());
        generateButton.setOnAction(e -> generatePattern());
    }

    private void updateChannelLimit() {
        String bitWidth = bitWidthCombo.getValue();
        int maxChannels = switch (bitWidth) {
            case "8-bit" -> 8;
            case "16-bit" -> 16;
            case "32-bit" -> 32;
            default -> 16;
        };
        model.setMaxChannels(maxChannels);
        channelSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, maxChannels, Math.min(model.getChannels(), maxChannels)));
        resizePattern();
        statusLabel.setText("Bit width set to " + bitWidth + " (max " + maxChannels + " channels)");
    }

    private void updateVoltageInfo() {
        String standard = ioStandardCombo.getValue();
        String info = switch (standard) {
            case "TTL" -> "TTL: 0V (low), 5V (high)";
            case "LVTTL" -> "LVTTL: 0V (low), 3.3V (high)";
            case "LVCMOS" -> "LVCMOS: 0V (low), 1.8V–3.3V (high)";
            case "LVDS" -> "LVDS: Differential, ~1.2V common mode";
            default -> "Unknown standard";
        };
        voltageInfoLabel.setText(info);
    }

    private void updatePatternControlsVisibility() {
        String patternType = patternTypeCombo.getValue();
        boolean isPWMorClock = patternType.equals("PWM") || patternType.equals("Clock");
        boolean isExpression = patternType.equals("Expression");
        dutyCycleSpinner.setVisible(isPWMorClock);
        dutyCycleSpinner.setManaged(isPWMorClock);
        patternFrequencySpinner.setVisible(isPWMorClock);
        patternFrequencySpinner.setManaged(isPWMorClock);
        expressionField.setVisible(isExpression);
        expressionField.setManaged(isExpression);
        generateButton.setVisible(!patternType.equals("Manual"));
        generateButton.setManaged(!patternType.equals("Manual"));
        updateGenerateButtonState();
    }

    private void updateGenerateButtonState() {
        String patternType = patternTypeCombo.getValue();
        if (patternType.equals("Manual")) {
            generateButton.setDisable(true);
        } else if (patternType.equals("PWM") || patternType.equals("Clock")) {
            double freq = patternFrequencySpinner.getValue();
            double sampleRateHz = sampleRateSpinner.getValue() * 1_000_000;
            generateButton.setDisable(freq > sampleRateHz / 2); // Nyquist limit
        } else if (patternType.equals("Expression")) {
            generateButton.setDisable(expressionField.getText().trim().isEmpty());
        } else {
            generateButton.setDisable(false); // PRBS
        }
    }

    private void setupTable() {
        patternTable.getColumns().clear();
        for (int step = 0; step < model.getSteps(); step++) {
            final int stepIndex = step;
            TableColumn<int[], String> column = new TableColumn<>("Step " + step);
            column.setCellValueFactory(cellData -> {
                int[] row = cellData.getValue();
                return new SimpleStringProperty(String.valueOf(row[stepIndex]));
            });
            column.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                    }
                }
            });
            column.setOnEditCommit(event -> {
                int[] row = event.getRowValue();
                try {
                    row[stepIndex] = Integer.parseInt(event.getNewValue());
                    model.setPattern(event.getTablePosition().getRow(), stepIndex, row[stepIndex]);
                    updateWaveform();
                    statusLabel.setText("Pattern updated at Channel " + event.getTablePosition().getRow() + ", Step " + stepIndex);
                } catch (NumberFormatException e) {
                    statusLabel.setText("Invalid input: Enter 0 or 1");
                }
            });
            column.setEditable(model.getPatternType().equals("Manual"));
            patternTable.getColumns().add(column);
        }
        patternTable.setEditable(model.getPatternType().equals("Manual"));
        updateTableData();
    }

    private void updateTableData() {
        patternTable.getItems().clear();
        for (int i = 0; i < model.getChannels(); i++) {
            patternTable.getItems().add(model.getPattern()[i]);
        }
    }

    private void updateWaveform() {
        double canvasWidth = Math.max(MINIMUM_CANVAS_WIDTH, model.getSteps() * TIME_STEP_WIDTH);
        double canvasHeight = Math.max(MINIMUM_CANVAS_HEIGHT, model.getChannels() * WAVEFORM_HEIGHT);
        waveformCanvas.setWidth(canvasWidth);
        waveformCanvas.setHeight(canvasHeight);

        GraphicsContext gc = waveformCanvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvasWidth, canvasHeight);

        double period = 1000.0 / model.getSampleRate(); // ns
        gc.setFont(new javafx.scene.text.Font("System", 12));
        gc.setFill(Color.BLACK);
        gc.fillText("Time per step: " + String.format("%.2f", period) + " ns", 10, 20);

        for (int channel = 0; channel < model.getChannels(); channel++) {
            double yOffset = channel * WAVEFORM_HEIGHT;
            gc.setStroke(Color.BLUE);
            gc.setLineWidth(2.0);
            int[] pattern = model.getPattern()[channel];
            for (int step = 0; step < model.getSteps(); step++) {
                double x = step * TIME_STEP_WIDTH;
                double y = yOffset + (pattern[step] == 1 ? 10 : WAVEFORM_HEIGHT - 10);
                if (step == 0) {
                    gc.beginPath();
                    gc.moveTo(x, y);
                } else {
                    gc.lineTo(x, y);
                }
                gc.stroke();
                gc.beginPath();
                gc.moveTo(x, y);
                gc.lineTo(x + TIME_STEP_WIDTH, y);
                gc.stroke();
            }
            gc.setFill(Color.BLACK);
            gc.fillText("Ch " + channel, 0, yOffset + WAVEFORM_HEIGHT / 2);
        }
    }

    private void resizePattern() {
        model.resize(channelSpinner.getValue(), stepSpinner.getValue());
        setupTable();
        updateWaveform();
        statusLabel.setText("Pattern resized to " + model.getChannels() + " channels, " + model.getSteps() + " steps");
    }

    private void clearPattern() {
        model.clear();
        updateTableData();
        updateWaveform();
        statusLabel.setText("Pattern cleared");
    }

    private void randomizePattern() {
        if (model.getPatternType().equals("Manual")) {
            model.randomize();
            updateTableData();
            updateWaveform();
            statusLabel.setText("Pattern randomized");
        } else {
            statusLabel.setText("Randomize only available in Manual mode");
        }
    }

    private void savePattern() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Pattern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pattern Files", "*.pat"));
        File file = fileChooser.showSaveDialog(patternTable.getScene().getWindow());
        if (file != null) {
            PatternFileHandler.savePattern(model, file);
            statusLabel.setText("Pattern saved to " + file.getName());
        }
    }

    private void loadPattern() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Pattern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pattern Files", "*.pat"));
        File file = fileChooser.showOpenDialog(patternTable.getScene().getWindow());
        if (file != null) {
            model = PatternFileHandler.loadPattern(file);
            bitWidthCombo.setValue(model.getMaxChannels() + "-bit");
            channelSpinner.getValueFactory().setValue(model.getChannels());
            stepSpinner.getValueFactory().setValue(model.getSteps());
            sampleRateSpinner.getValueFactory().setValue(model.getSampleRate());
            ioStandardCombo.setValue(model.getIoStandard());
            patternTypeCombo.setValue(model.getPatternType());
            dutyCycleSpinner.getValueFactory().setValue(model.getDutyCycle());
            patternFrequencySpinner.getValueFactory().setValue(model.getPatternFrequency());
            expressionField.setText(model.getExpression());
            updateChannelLimit();
            updateVoltageInfo();
            updatePatternControlsVisibility();
            setupTable();
            updateWaveform();
            statusLabel.setText("Pattern loaded from " + file.getName());
        }
    }

    private void generatePattern() {
        try {
            String status = model.generatePattern();
            updateTableData();
            updateWaveform();
            statusLabel.setText(status);
        } catch (Exception e) {
            statusLabel.setText("Error generating pattern: " + e.getMessage());
        }
    }
}