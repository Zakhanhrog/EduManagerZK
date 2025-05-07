package com.eduzk.utils;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import com.formdev.flatlaf.extras.FlatSVGIcon;

public class UIUtils {

    // Center a JFrame or JDialog on the screen
    public static void centerWindow(Window window) {
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((dimension.getWidth() - window.getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - window.getHeight()) / 2);
        window.setLocation(x, y);
    }

    // Create an ImageIcon from a resource path
    public static ImageIcon createImageIcon(String path, String description) {
        if (path == null || path.isEmpty()) {
            System.err.println("Error: Image path is null or empty.");
            return null;
        }
        // Prepend "/" if path is relative to the classpath root
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        URL imgURL = UIUtils.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null; // Or return a default placeholder icon
        }
    }

    // Set preferred, minimum, and maximum size for a component
    public static void setFixedSize(Component component, Dimension size) {
        component.setPreferredSize(size);
        component.setMinimumSize(size);
        component.setMaximumSize(size);
    }

    // Show a standard information message dialog
    public static void showInfoMessage(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    // Show a standard warning message dialog
    public static void showWarningMessage(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
    }

    // Show a standard error message dialog
    public static void showErrorMessage(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    // Show a confirmation dialog (Yes/No)
    public static boolean showConfirmDialog(Component parent, String title, String message) {
        int result = JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }

    // Add padding/border around a component
    public static void addPadding(JComponent component, int top, int left, int bottom, int right) {
        component.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
    }

    // Find the top-level Window (JFrame or JDialog) containing a component
    public static Window getWindowForComponent(Component component) {
        if (component == null) {
            return JOptionPane.getRootFrame(); // Fallback
        }
        if (component instanceof Window) {
            return (Window) component;
        }
        return SwingUtilities.getWindowAncestor(component);
    }
    public static Icon loadSVGIcon(String path, int size) {
        if (path == null || path.isEmpty()) return null;
        try {
            URL iconUrl = UIUtils.class.getResource(path);
            if (iconUrl != null) {
                return new FlatSVGIcon(iconUrl).derive(size, size);
            } else {
                System.err.println("UIUtils Warning: SVG icon resource not found at: " + path);
                return null;
            }
        } catch (Exception e) {
            System.err.println("UIUtils Error: loading/parsing SVG icon from path: " + path + " - " + e.getMessage());
            return null;
        }
    }
}
