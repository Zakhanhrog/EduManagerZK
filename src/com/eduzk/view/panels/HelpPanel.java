package com.eduzk.view.panels;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class HelpPanel extends JPanel {
    private JEditorPane helpEditorPane;
    private JScrollPane scrollPane;
    private BufferedImage backgroundImage;

    public HelpPanel() {
        setLayout(new BorderLayout());
        loadImageBackground();
        initComponents();
        setupLayout();
        loadHelpPage();
    }
    private void loadImageBackground() {
        try {
            URL imgUrl = getClass().getResource("/images/logo_background_blurred.png");
            if (imgUrl != null) {
                backgroundImage = ImageIO.read(imgUrl);
                System.out.println("HelpPanel: Background image loaded successfully.");
            } else {
                System.err.println("HelpPanel Error: Background image resource not found at /images/logo_background_blurred.png");
                backgroundImage = null;
            }
        } catch (IOException e) {
            System.err.println("HelpPanel Error: Failed to read background image: " + e.getMessage());
            backgroundImage = null;
        }
    }

    private void initComponents() {
        helpEditorPane = new JEditorPane();
        helpEditorPane.setEditable(false);
        helpEditorPane.setContentType("text/html");
        helpEditorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        helpEditorPane.setFont(UIManager.getFont("Label.font"));
        helpEditorPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        helpEditorPane.setOpaque(false);
        helpEditorPane.addHyperlinkListener(new HyperlinkHandler());
        scrollPane = new JScrollPane(helpEditorPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
    }

    private void setupLayout() {
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadHelpPage() {
        String helpFilePath = "/help/help.html";
        URL helpURL = getClass().getResource(helpFilePath);

        if (helpURL != null) {
            try {
                System.out.println("HelpPanel: Loading help content from: " + helpURL);
                helpEditorPane.setPage(helpURL);
            } catch (IOException e) {
                System.err.println("HelpPanel Error: Could not load help page: " + helpURL + " - " + e.getMessage());
                showError("Error loading help content. File might be missing or corrupted.");
                e.printStackTrace();
            }
        } else {
            System.err.println("HelpPanel Error: Help file resource not found at path: " + helpFilePath);
            showError("Help file not found. Please contact support.");
        }
    }
    private void showError(String message) {
        helpEditorPane.setContentType("text/plain");
        helpEditorPane.setText("ERROR:\n\n" + message);
    }
    private class HyperlinkHandler implements HyperlinkListener {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                System.out.println("HelpPanel: Link clicked: " + e.getURL());
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        try {
                            desktop.browse(e.getURL().toURI());
                        } catch (IOException | URISyntaxException ex) {
                            System.err.println("HelpPanel Error: Could not open link in browser - " + ex.getMessage());
                            JOptionPane.showMessageDialog(HelpPanel.this,
                                    "Could not open the link:\n" + e.getURL(),
                                    "Link Error", JOptionPane.WARNING_MESSAGE);
                        }
                    } else {
                        System.err.println("HelpPanel Warning: Desktop browsing action not supported.");
                    }
                } else {
                    System.err.println("HelpPanel Warning: Desktop interaction not supported on this system.");
                }
            }
        }
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (backgroundImage != null) {
            Graphics2D g2d = (Graphics2D) g.create();

            int x = (this.getWidth() - backgroundImage.getWidth()) / 2;
            int y = (this.getHeight() - backgroundImage.getHeight()) / 2;
            g2d.drawImage(backgroundImage, x, y, this);

            g2d.dispose();
        }
    }
}