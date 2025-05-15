package pattern.generator.app;

import java.io.*;

public class PatternFileHandler {

    public static void savePattern(PatternModel model, File file) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeInt(model.getMaxChannels());
            oos.writeInt(model.getChannels());
            oos.writeInt(model.getSteps());
            oos.writeDouble(model.getSampleRate());
            oos.writeObject(model.getIoStandard());
            oos.writeObject(model.getPatternType());
            oos.writeDouble(model.getDutyCycle());
            oos.writeDouble(model.getPatternFrequency());
            oos.writeObject(model.getExpression());
            int[][] pattern = model.getPattern();
            for (int i = 0; i < model.getChannels(); i++) {
                for (int j = 0; j < model.getSteps(); j++) {
                    oos.writeInt(pattern[i][j]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PatternModel loadPattern(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            int maxChannels = ois.readInt();
            int channels = ois.readInt();
            int steps = ois.readInt();
            double sampleRate = ois.readDouble();
            String ioStandard = (String) ois.readObject();
            String patternType = (String) ois.readObject();
            double dutyCycle = ois.readDouble();
            double patternFrequency = ois.readDouble();
            String expression = (String) ois.readObject();
            PatternModel model = new PatternModel(maxChannels, channels, steps, sampleRate, ioStandard,
                    patternType, dutyCycle, patternFrequency, expression);
            int[][] pattern = model.getPattern();
            for (int i = 0; i < channels; i++) {
                for (int j = 0; j < steps; j++) {
                    pattern[i][j] = ois.readInt();
                }
            }
            return model;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new PatternModel(16, 4, 8, 1.0, "TTL", "Manual", 50.0, 1000.0, "");
        }
    }
}