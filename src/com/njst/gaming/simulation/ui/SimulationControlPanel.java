package com.njst.gaming.simulation.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class SimulationControlPanel extends JFrame {
    private final JLabel generationLabel;
    private final JLabel aliveLabel;
    private final JLabel speedLabel;
    private float currentSpeed = 1.0f;
    private Consumer<Float> onSpeedChange;

    public SimulationControlPanel(Consumer<Float> onSpeedChange) {
        this.onSpeedChange = onSpeedChange;
        
        setTitle("Simulation Control Panel");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new GridLayout(6, 1, 10, 10));

        // Stats
        generationLabel = new JLabel("Generation: 1");
        aliveLabel = new JLabel("Alive NPCs: 0");
        speedLabel = new JLabel("Simulation Speed: 1.0x");
        
        generationLabel.setHorizontalAlignment(SwingConstants.CENTER);
        aliveLabel.setHorizontalAlignment(SwingConstants.CENTER);
        speedLabel.setHorizontalAlignment(SwingConstants.CENTER);

        add(generationLabel);
        add(aliveLabel);
        add(speedLabel);

        // Slider for fine Control
        JSlider speedSlider = new JSlider(1, 100, 10); // 0.1x to 10x (scale 10)
        speedSlider.addChangeListener(e -> {
            float speed = speedSlider.getValue() / 10.0f;
            updateSpeed(speed);
        });
        add(new JLabel("Adjust Speed (0.1x - 10x):", SwingConstants.CENTER));
        add(speedSlider);

        // Fast Forward Button
        JButton ffButton = new JButton("Fast Forward (Toggle 10x)");
        ffButton.addActionListener(e -> {
            if (currentSpeed < 10.0f) updateSpeed(10.0f);
            else updateSpeed(1.0f);
            speedSlider.setValue((int)(currentSpeed * 10));
        });
        add(ffButton);

        setVisible(true);
    }

    private void updateSpeed(float speed) {
        this.currentSpeed = speed;
        speedLabel.setText(String.format("Simulation Speed: %.1fx", speed));
        if (onSpeedChange != null) {
            onSpeedChange.accept(speed);
        }
    }

    public void updateStats(int generation, int aliveCount) {
        SwingUtilities.invokeLater(() -> {
            generationLabel.setText("Generation: " + generation);
            aliveLabel.setText("Alive NPCs: " + aliveCount);
        });
    }
}
