package com.eduzk.utils;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import com.formdev.flatlaf.extras.FlatSVGIcon;

public class UIUtils {

    public static void showInfoMessage(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showWarningMessage(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
    }

    public static void showErrorMessage(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static boolean showConfirmDialog(Component parent, String title, String message) {
        int result = JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
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
