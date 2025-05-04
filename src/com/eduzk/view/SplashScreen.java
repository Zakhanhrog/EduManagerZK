package com.eduzk.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class SplashScreen extends JWindow {

    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JLabel logoLabel;
    private JLabel titleLabel;

    private static final int WIDTH = 480;
    private static final int HEIGHT = 320;

    public SplashScreen(Frame owner) {
        super(owner);
        initComponents();
        setupLayout();
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        logoLabel = new JLabel();
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            URL logoUrl = getClass().getResource("/icons/logo.svg");
            if (logoUrl != null) {
                FlatSVGIcon svgIcon = new FlatSVGIcon(logoUrl).derive(0.6f);
                logoLabel.setIcon(svgIcon);
            } else {
                logoLabel.setText("[Logo]");
            }
        } catch (Exception e) {
            logoLabel.setText("[Logo Error]");
        }

        titleLabel = new JLabel("EduZakhanh Management");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(UIManager.getColor("Label.foreground"));

        statusLabel = new JLabel("Initializing...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(UIManager.getColor("Label.foreground"));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(false);
    }

    private void setupLayout() {
        JPanel contentPanel = new JPanel(new BorderLayout(0, 15));
        contentPanel.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1));
        contentPanel.setBackground(UIManager.getColor("Panel.background"));

        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(20, 10, 5, 10);
        topPanel.add(logoLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(5, 10, 20, 10);
        topPanel.add(titleLabel, gbc);

        contentPanel.add(topPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 5));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 15, 20));
        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);

        contentPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(contentPanel);
    }

    public void setStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(status == null ? "" : status);
            }
        });
    }

    public void setProgress(int value) {
        SwingUtilities.invokeLater(() -> {
            if (progressBar != null) {
                progressBar.setIndeterminate(false);
                progressBar.setValue(value);
            }
        });
    }
}
