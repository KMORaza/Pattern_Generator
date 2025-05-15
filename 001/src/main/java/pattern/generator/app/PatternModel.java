package pattern.generator.app;

import java.util.Random;

public class PatternModel {
    private int channels;
    private int steps;
    private int maxChannels; // 8, 16, or 32
    private double sampleRate; // in MHz
    private String ioStandard;
    private String patternType; // Manual, PWM, PRBS, Clock, Expression
    private double dutyCycle; // 0-100%
    private double patternFrequency; // Hz
    private String expression;
    private int[][] pattern;
    private static final Random random = new Random(42);

    public PatternModel(int maxChannels, int channels, int steps, double sampleRate, String ioStandard,
                        String patternType, double dutyCycle, double patternFrequency, String expression) {
        this.maxChannels = maxChannels;
        this.channels = Math.min(channels, maxChannels);
        this.steps = steps;
        this.sampleRate = sampleRate;
        this.ioStandard = ioStandard;
        this.patternType = patternType;
        this.dutyCycle = dutyCycle;
        this.patternFrequency = patternFrequency;
        this.expression = expression;
        this.pattern = new int[this.channels][steps];
    }

    // Getters and setters
    public int getChannels() { return channels; }
    public int getSteps() { return steps; }
    public int getMaxChannels() { return maxChannels; }
    public double getSampleRate() { return sampleRate; }
    public String getIoStandard() { return ioStandard; }
    public String getPatternType() { return patternType; }
    public double getDutyCycle() { return dutyCycle; }
    public double getPatternFrequency() { return patternFrequency; }
    public String getExpression() { return expression; }

    public void setMaxChannels(int maxChannels) {
        this.maxChannels = maxChannels;
        if (channels > maxChannels) {
            resize(maxChannels, steps);
        }
    }

    public void setSampleRate(double sampleRate) {
        if (sampleRate <= 0) throw new IllegalArgumentException("Sample rate must be positive");
        this.sampleRate = sampleRate;
    }

    public void setIoStandard(String ioStandard) { this.ioStandard = ioStandard; }
    public void setPatternType(String patternType) { this.patternType = patternType; }
    public void setDutyCycle(double dutyCycle) {
        if (dutyCycle < 0 || dutyCycle > 100) throw new IllegalArgumentException("Duty cycle must be 0-100%");
        this.dutyCycle = dutyCycle;
    }

    public void setPatternFrequency(double patternFrequency) {
        if (patternFrequency <= 0) throw new IllegalArgumentException("Frequency must be positive");
        this.patternFrequency = patternFrequency;
    }

    public void setExpression(String expression) { this.expression = expression; }

    public int[][] getPattern() { return pattern; }

    public void setPattern(int channel, int step, int value) {
        if (channel >= 0 && channel < channels && step >= 0 && step < steps) {
            pattern[channel][step] = value > 0 ? 1 : 0;
        }
    }

    public void resize(int newChannels, int newSteps) {
        if (newChannels < 1 || newSteps < 1) throw new IllegalArgumentException("Channels and steps must be positive");
        newChannels = Math.min(newChannels, maxChannels);
        int[][] newPattern = new int[newChannels][newSteps];
        int minChannels = Math.min(channels, newChannels);
        int minSteps = Math.min(steps, newSteps);
        for (int i = 0; i < minChannels; i++) {
            for (int j = 0; j < minSteps; j++) {
                newPattern[i][j] = pattern[i][j];
            }
        }
        this.channels = newChannels;
        this.steps = newSteps;
        this.pattern = newPattern;
    }

    public void clear() {
        pattern = new int[channels][steps];
    }

    public void randomize() {
        for (int i = 0; i < channels; i++) {
            for (int j = 0; j < steps; j++) {
                pattern[i][j] = random.nextBoolean() ? 1 : 0;
            }
        }
    }

    public String generatePattern() {
        clear();
        if (patternType.equals("Manual")) return "Manual mode: edit table directly.";

        double samplePeriod = 1.0 / (sampleRate * 1_000_000);
        double totalDuration = steps * samplePeriod;
        System.out.println("Generating " + patternType + ": sampleRate=" + sampleRate + " MHz, samplePeriod=" + samplePeriod + " s, totalDuration=" + totalDuration + " s");

        switch (patternType) {
            case "PWM":
            case "Clock":
                if (patternFrequency > sampleRate * 1_000_000 / 2) {
                    throw new IllegalStateException("Pattern frequency exceeds Nyquist limit (" + (sampleRate * 500_000) + " Hz)");
                }
                double period = 1.0 / patternFrequency;
                int stepsPerCycle = (int) Math.round(period / samplePeriod);
                if (stepsPerCycle > steps) {
                    stepsPerCycle = Math.max(2, steps / 2);
                    period = stepsPerCycle * samplePeriod;
                    patternFrequency = 1.0 / period;
                    System.out.println("Adjusted frequency to " + patternFrequency + " Hz to fit within " + steps + " steps");
                }
                int highSteps = (int) Math.round(stepsPerCycle * dutyCycle / 100.0);
                highSteps = Math.max(0, Math.min(highSteps, stepsPerCycle - 1)); // Ensure at least one transition
                System.out.println("PWM/Clock: period=" + period + " s, stepsPerCycle=" + stepsPerCycle + ", highSteps=" + highSteps + ", dutyCycle=" + dutyCycle + "%");
                for (int channel = 0; channel < channels; channel++) {
                    for (int step = 0; step < steps; step++) {
                        int cycleStep = step % stepsPerCycle;
                        pattern[channel][step] = (cycleStep < highSteps) ? 1 : 0;
                    }
                    if (channel == 0) {
                        System.out.print("PWM/Clock pattern (ch 0): ");
                        for (int i = 0; i < steps; i++) {
                            System.out.print(pattern[0][i] + " ");
                        }
                        System.out.println();
                    }
                }
                if (stepsPerCycle > steps) {
                    return "Warning: Pattern frequency too low (" + patternFrequency + " Hz). Increase steps or frequency to see transitions.";
                }
                return "Generated " + patternType + " pattern.";
            case "PRBS":
                int[] prbs = generatePRBS7();
                System.out.println("PRBS: sequence length=" + prbs.length);
                for (int channel = 0; channel < channels; channel++) {
                    for (int step = 0; step < steps; step++) {
                        pattern[channel][step] = prbs[step % prbs.length];
                    }
                    if (channel == 0) {
                        System.out.print("PRBS pattern (ch 0): ");
                        for (int i = 0; i < steps; i++) {
                            System.out.print(pattern[0][i] + " ");
                        }
                        System.out.println();
                    }
                }
                return "Generated PRBS pattern.";
            case "Expression":
                if (expression == null || expression.trim().isEmpty()) {
                    throw new IllegalArgumentException("Expression cannot be empty");
                }
                double exprFreq = 1.0;
                if (expression.contains("*t*")) {
                    String[] parts = expression.replace(" ", "").split("\\*t\\*");
                    if (parts.length > 1 && !parts[1].isEmpty()) {
                        try {
                            exprFreq = Double.parseDouble(parts[1].replace(")", "").trim());
                        } catch (NumberFormatException e) {
                        }
                    }
                }
                double exprPeriod = 1.0 / exprFreq;
                int exprStepsPerCycle = (int) Math.round(exprPeriod / samplePeriod);
                if (exprStepsPerCycle > steps) {
                    exprStepsPerCycle = Math.max(2, steps / 2);
                    exprPeriod = exprStepsPerCycle * samplePeriod;
                    exprFreq = 1.0 / exprPeriod;
                    System.out.println("Adjusted expression frequency to " + exprFreq + " Hz to fit within " + steps + " steps");
                }
                for (int channel = 0; channel < channels; channel++) {
                    for (int step = 0; step < steps; step++) {
                        double t = step * samplePeriod;
                        double value = evaluateExpression(expression, t, exprFreq);
                        pattern[channel][step] = (value >= 0.5) ? 1 : 0;
                    }
                    if (channel == 0) {
                        System.out.print("Expression pattern (ch 0): ");
                        for (int i = 0; i < steps; i++) {
                            System.out.print(pattern[0][i] + " ");
                        }
                        System.out.println();
                    }
                }
                if (exprStepsPerCycle > steps) {
                    return "Warning: Expression frequency too low (" + exprFreq + " Hz). Increase steps or frequency to see transitions.";
                }
                return "Generated Expression pattern.";
            default:
                throw new IllegalStateException("Unknown pattern type: " + patternType);
        }
    }

    private int[] generatePRBS7() {
        int[] sequence = new int[127];
        int register = 0x7F;
        for (int i = 0; i < 127; i++) {
            sequence[i] = register & 1;
            int bit6 = (register >> 6) & 1;
            int bit5 = (register >> 5) & 1;
            int newBit = bit6 ^ bit5;
            register = (register >> 1) | (newBit << 6);
        }
        System.out.print("PRBS sequence: ");
        for (int i = 0; i < Math.min(10, sequence.length); i++) {
            System.out.print(sequence[i] + " ");
        }
        System.out.println();
        return sequence;
    }

    private double evaluateExpression(String expr, double t, double freq) {
        try {
            expr = expr.toLowerCase().replace(" ", "").replace("pi", String.valueOf(Math.PI)).trim();
            if (expr.startsWith("sin")) {
                String arg = expr.substring(4, expr.length() - 1);
                double phase = 2 * Math.PI * freq * t;
                double value = Math.sin(phase);
                return 0.5 * (1 + value);
            } else if (expr.startsWith("cos")) {
                String arg = expr.substring(4, expr.length() - 1);
                double phase = 2 * Math.PI * freq * t;
                double value = Math.cos(phase);
                return 0.5 * (1 + value);
            } else if (expr.equals("t")) {
                double totalDuration = steps * (1.0 / (sampleRate * 1_000_000));
                return (t % totalDuration) / totalDuration;
            } else {
                double value = Double.parseDouble(expr);
                return value;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid expression: " + expr + ". Use sin(2*pi*t*freq), cos(2*pi*t*freq), t, or a constant.");
        }
    }
}