package com.eduzk.view.panels;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class HelpPanel extends JPanel {

    private JEditorPane helpEditorPane;
    private JScrollPane scrollPane;

    public HelpPanel() {
        setLayout(new BorderLayout());
        initComponents();
        setupLayout();
        loadHelpPage();
    }

    private void initComponents() {
        helpEditorPane = new JEditorPane();
        helpEditorPane.setEditable(false);
        helpEditorPane.setContentType("text/html");
        helpEditorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        helpEditorPane.setFont(UIManager.getFont("Label.font"));
        helpEditorPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        helpEditorPane.addHyperlinkListener(new HyperlinkHandler());
        scrollPane = new JScrollPane(helpEditorPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
        helpEditorPane.setContentType("text/plain"); // Chuyển sang text thường để hiển thị lỗi
        helpEditorPane.setText("ERROR:\n\n" + message);
    }
    private class HyperlinkHandler implements HyperlinkListener {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                // Khi người dùng nhấp vào link
                System.out.println("HelpPanel: Link clicked: " + e.getURL());
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        try {
                            // Mở link bằng trình duyệt mặc định của hệ thống
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
}