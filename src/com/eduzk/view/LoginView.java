package com.eduzk.view;

import com.eduzk.controller.AuthController;
import com.formdev.flatlaf.FlatClientProperties;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import com.eduzk.view.dialogs.RegisterDialog;
import java.net.URL;
import com.formdev.flatlaf.extras.FlatSVGIcon;

public class LoginView extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel statusLabel;
    private JLabel logoLabel;
    private AuthController authController;

    public LoginView(AuthController authController) {
        if (authController == null) {
            throw new IllegalArgumentException("AuthController cannot be null for LoginView");
        }
        this.authController = authController;
        initComponents();
        setupLayout();
        setupActions();
        configureWindow();
        styleComponents();
    }

    private void initComponents() {
        usernameField = new JTextField(20);
        passwordField = new JPasswordField();
        loginButton = new JButton("Login");
        registerButton = new JButton("Register");
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UIManager.getColor("Label.errorForeground"));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoLabel = new JLabel(); // Tạo JLabel trống trước
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }
    private void styleComponents() {
        usernameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter your username");
        passwordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter your password");
        loginButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, "primary");
        registerButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, "borderless");
        passwordField.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");

    }


    private void setupLayout() {
        setTitle("EduZakhanh - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30)); // Giảm nhẹ padding trên/dưới tổng thể
        GridBagConstraints gbc = new GridBagConstraints();

        // --- 1. THÊM SPACER Ở TRÊN CÙNG ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.weighty = 0.7;
        gbc.fill = GridBagConstraints.VERTICAL;
        mainPanel.add(Box.createVerticalGlue(), gbc);

        try {
            URL logoUrl = getClass().getResource("/icons/logo.svg");
            if (logoUrl != null) {
//                FlatSVGIcon svgIcon = new FlatSVGIcon(logoUrl).derive(180, -1);
                FlatSVGIcon svgIcon = new FlatSVGIcon(logoUrl);
                logoLabel.setIcon(svgIcon);
            } else {
                System.err.println("LoginView Error: Logo SVG resource not found!");
                logoLabel.setText("[Logo Not Found]");
            }
        } catch (Exception e) {
            System.err.println("LoginView Error: Could not load logo SVG - " + e.getMessage());
            logoLabel.setText("[Error Loading Logo]");
            e.printStackTrace();
        }

        // Thiết lập ràng buộc cho logoLabel (đã dịch xuống)
        gbc.gridx = 0;
        gbc.gridy = 1;       // Logo ở hàng 1
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;     // *** Logo không có weighty ***
        gbc.insets = new Insets(0, 5, 20, 5); // Khoảng cách dưới logo (20), trên (0 vì đã có spacer)
        mainPanel.add(logoLabel, gbc);

        // --- 3. Thêm Username Label và Field (ở gridy = 2) ---
        gbc.gridwidth = 1; // Reset
        gbc.fill = GridBagConstraints.NONE;

        gbc.gridx = 0;
        gbc.gridy = 2;       // Hàng 2
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.insets = new Insets(5, 5, 5, 10);
        mainPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;       // Hàng 2
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 0, 5, 5);
        mainPanel.add(usernameField, gbc);

        // --- 4. Thêm Password Label và Field (ở gridy = 3) ---
        gbc.weightx = 0.0; // Reset

        gbc.gridx = 0;
        gbc.gridy = 3;       // Hàng 3
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 5, 5, 10);
        mainPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;       // Hàng 3
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 0, 5, 5);
        mainPanel.add(passwordField, gbc);

        // --- 5. Thêm Button Panel (ở gridy = 4) ---
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints btnGbc = new GridBagConstraints();
        btnGbc.fill = GridBagConstraints.HORIZONTAL;
        btnGbc.weightx = 0.5;
        btnGbc.insets = new Insets(0, 5, 0, 5);
        btnGbc.gridx = 0;
        btnGbc.gridy = 0;
        buttonPanel.add(registerButton, btnGbc);
        btnGbc.gridx = 1;
        btnGbc.gridy = 0;
        buttonPanel.add(loginButton, btnGbc);

        gbc.gridx = 0;
        gbc.gridy = 4;       // Hàng 4
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.insets = new Insets(20, 5, 5, 5);
        mainPanel.add(buttonPanel, gbc);

        // --- 6. Thêm Status Label (ở gridy = 5) ---
        gbc.gridx = 0;
        gbc.gridy = 5;       // Hàng 5
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 5, 5); // Giảm khoảng cách dưới status label
        mainPanel.add(statusLabel, gbc);

        // --- 7. THÊM SPACER Ở DƯỚI CÙNG (ở gridy = 6) ---
        gbc.gridx = 0;
        gbc.gridy = 6;      // Hàng cuối cùng
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.weighty = 0.3;  // *** Trọng số nhỏ hơn ở dưới ***
        gbc.fill = GridBagConstraints.VERTICAL;
        mainPanel.add(Box.createVerticalGlue(), gbc);

        // Thêm panel chính vào JFrame
        add(mainPanel);
    }

    private void setupActions() {
        // Action for login button click
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        });

        // Action for pressing Enter in password field
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        });

        // Action for pressing Enter in username field (move focus to password)
        usernameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    passwordField.requestFocusInWindow();
                }
            }
        });
        registerButton.addActionListener(e -> {
            RegisterDialog registerDialog = new RegisterDialog(LoginView.this, authController);
            registerDialog.setVisible(true);
        });
    }

    private void configureWindow() {
        pack();
        setMinimumSize(new Dimension(400, 140));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                usernameField.requestFocusInWindow();
            }
        });
    }


    private void performLogin() {
        String username = usernameField.getText().trim(); // Trim whitespace
        String password = new String(passwordField.getPassword());
        statusLabel.setText(" ");

        if (username.isEmpty() || password.isEmpty()) {
            showLoginError("Username and password cannot be empty.");
            if (username.isEmpty()) {
                usernameField.requestFocusInWindow();
            } else {
                passwordField.requestFocusInWindow();
            }
            return;
        }

        setLoginInProgress(true);

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String errorMessage = null;

            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    return authController.attemptLogin(username, password);
                } catch (Exception e) {
                    errorMessage = "Login failed: " + e.getMessage();
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void done() {
                setLoginInProgress(false);
                try {
                    boolean loggedIn = get();
                    if (!loggedIn) {
                        if (errorMessage == null) {
                             showLoginError("Invalid username or password.");
                        } else {
                            showLoginError(errorMessage); // Hiển thị lỗi exception
                        }
                    }
                } catch (Exception e) {
                    showLoginError("An unexpected error occurred during login.");
                    e.printStackTrace();
                }
            }
        };
        worker.execute();

    }
    private void setLoginInProgress(boolean inProgress) {
        usernameField.setEnabled(!inProgress);
        passwordField.setEnabled(!inProgress);
        loginButton.setEnabled(!inProgress);
        registerButton.setEnabled(!inProgress);
        if (inProgress) {
            statusLabel.setText("Logging in...");
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            statusLabel.setText(" ");
            setCursor(Cursor.getDefaultCursor());
        }
    }

    public void showLoginError(String message) {
        statusLabel.setText(message);
        passwordField.setText("");
        if (usernameField.getText().isEmpty()) {
            usernameField.requestFocusInWindow();
        } else {
            passwordField.requestFocusInWindow();
        }
    }

}