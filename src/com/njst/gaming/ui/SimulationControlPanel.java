package com.njst.gaming.ui;

import com.njst.gaming.simulation.entities.NPC;
import javax.swing.*;
import java.awt.*;

public class SimulationControlPanel extends JFrame {

    public interface ControlActions {
        void saveMovement();

        void saveState();

        void saveTarget();

        void loadMovementToAll();

        void loadStateToAll();

        void loadTargetToAll();

        void toggleFastTraining();

        boolean isFastTraining();

        void toggleForcedTargetMode();

        boolean isForcedTargetMode();
    }

    private final ControlActions actions;
    private final JPanel sliderContainer;
    private final CardLayout cardLayout;

    public SimulationControlPanel(ControlActions actions) {
        super("Simulation Controls");
        this.actions = actions;

        setLayout(new BorderLayout());
        setSize(400, 450);
        setAlwaysOnTop(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Top Selection Panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Select Brain:"));

        String[] brains = { "Movement", "State", "Target" };
        JComboBox<String> brainSelector = new JComboBox<>(brains);
        topPanel.add(brainSelector);

        JCheckBox targetBrainToggle = new JCheckBox("Use Target Brain", NPC.useTargetSetterBrain);
        targetBrainToggle.addActionListener(e -> {
            NPC.useTargetSetterBrain = targetBrainToggle.isSelected();
        });
        topPanel.add(targetBrainToggle);

        add(topPanel, BorderLayout.NORTH);

        // Slider Container with CardLayout
        cardLayout = new CardLayout();
        sliderContainer = new JPanel(cardLayout);

        sliderContainer.add(createBrainSettingsPanel("Movement",
                () -> NPC.movementMutationRate, v -> NPC.movementMutationRate = v,
                () -> NPC.movementMutationStrength, v -> NPC.movementMutationStrength = v), "Movement");

        sliderContainer.add(createBrainSettingsPanel("State",
                () -> NPC.stateMutationRate, v -> NPC.stateMutationRate = v,
                () -> NPC.stateMutationStrength, v -> NPC.stateMutationStrength = v), "State");

        sliderContainer.add(createBrainSettingsPanel("Target",
                () -> NPC.targetMutationRate, v -> NPC.targetMutationRate = v,
                () -> NPC.targetMutationStrength, v -> NPC.targetMutationStrength = v), "Target");

        add(sliderContainer, BorderLayout.CENTER);

        // Bottom Action Panel
        JPanel bottomPanel = new JPanel(new GridLayout(4, 2, 5, 5));

        JButton btnSaveMove = new JButton("Save Best Movement");
        btnSaveMove.addActionListener(e -> this.actions.saveMovement());

        JButton btnLoadMove = new JButton("Load Movement to All");
        btnLoadMove.addActionListener(e -> this.actions.loadMovementToAll());

        JButton btnSaveState = new JButton("Save Best State");
        btnSaveState.addActionListener(e -> this.actions.saveState());

        JButton btnLoadState = new JButton("Load State to All");
        btnLoadState.addActionListener(e -> this.actions.loadStateToAll());

        JButton btnSaveTarget = new JButton("Save Best Target");
        btnSaveTarget.addActionListener(e -> this.actions.saveTarget());

        JButton btnLoadTarget = new JButton("Load Target to All");
        btnLoadTarget.addActionListener(e -> this.actions.loadTargetToAll());

        JButton btnFastTrain = new JButton("Process Fast Training");
        btnFastTrain.setBackground(this.actions.isFastTraining() ? Color.GREEN : Color.LIGHT_GRAY);
        btnFastTrain.addActionListener(e -> {
            this.actions.toggleFastTraining();
            btnFastTrain.setText(this.actions.isFastTraining() ? "Stop Fast Training" : "Start Fast Training");
            btnFastTrain.setBackground(this.actions.isFastTraining() ? Color.GREEN : Color.LIGHT_GRAY);
        });

        JButton btnForcedMode = new JButton(
                this.actions.isForcedTargetMode() ? "Forced Target: ON" : "Forced Target: OFF");
        btnForcedMode.setBackground(this.actions.isForcedTargetMode() ? Color.GREEN : Color.LIGHT_GRAY);
        btnForcedMode.addActionListener(e -> {
            this.actions.toggleForcedTargetMode();
            btnForcedMode.setText(this.actions.isForcedTargetMode() ? "Forced Target: ON" : "Forced Target: OFF");
            btnForcedMode.setBackground(this.actions.isForcedTargetMode() ? Color.GREEN : Color.LIGHT_GRAY);
        });

        bottomPanel.add(btnSaveMove);
        bottomPanel.add(btnLoadMove);
        bottomPanel.add(btnSaveState);
        bottomPanel.add(btnLoadState);
        bottomPanel.add(btnSaveTarget);
        bottomPanel.add(btnLoadTarget);
        bottomPanel.add(btnForcedMode);
        bottomPanel.add(btnFastTrain);

        add(bottomPanel, BorderLayout.SOUTH);

        brainSelector.addActionListener(e -> {
            cardLayout.show(sliderContainer, (String) brainSelector.getSelectedItem());
        });

        setVisible(true);
    }

    private JPanel createBrainSettingsPanel(String label,
            java.util.function.Supplier<Float> rateGet, java.util.function.Consumer<Float> rateSet,
            java.util.function.Supplier<Float> strGet, java.util.function.Consumer<Float> strSet) {

        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Rate Slider
        JPanel pRate = new JPanel(new BorderLayout());
        JLabel lRate = new JLabel(label + " Mut Rate: " + String.format("%.2f", rateGet.get()));
        JSlider sRate = new JSlider(0, 100, (int) (rateGet.get() * 100));
        sRate.addChangeListener(e -> {
            rateSet.accept(sRate.getValue() / 100.0f);
            lRate.setText(label + " Mut Rate: " + String.format("%.2f", rateGet.get()));
        });
        pRate.add(lRate, BorderLayout.NORTH);
        pRate.add(sRate, BorderLayout.CENTER);

        // Strength Slider
        JPanel pStr = new JPanel(new BorderLayout());
        JLabel lStr = new JLabel(label + " Mut Strength: " + String.format("%.2f", strGet.get()));
        JSlider sStr = new JSlider(0, 100, (int) (strGet.get() * 100));
        sStr.addChangeListener(e -> {
            strSet.accept(sStr.getValue() / 100.0f);
            lStr.setText(label + " Mut Strength: " + String.format("%.2f", strGet.get()));
        });
        pStr.add(lStr, BorderLayout.NORTH);
        pStr.add(sStr, BorderLayout.CENTER);

        panel.add(pRate);
        panel.add(pStr);
        return panel;
    }
}
