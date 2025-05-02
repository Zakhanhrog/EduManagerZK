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

public class LoginView extends JFrame {

    private final AuthController authController;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel statusLabel; // Optional: For messages like "Logging in..."

    public LoginView(AuthController authController) {
        this.authController = authController;
        initComponents();
        setupLayout();
        setupActions();
        configureWindow();
    }

    private void initComponents() {
        usernameField = new JTextField(20);
        passwordField = new JPasswordField();
        loginButton = new JButton("Login");
        registerButton = new JButton("Register");
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UIManager.getColor("Label.errorForeground"));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }
    private void styleComponents() {
        usernameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter your username");
        passwordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter your password");
        loginButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, "primary");
        registerButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, "borderless");
        passwordField.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");

        // Font labelFont = UIManager.getFont("Label.font");
        // usernameLabel.setFont(labelFont.deriveFont(labelFont.getSize() + 1f));
        // passwordLabel.setFont(labelFont.deriveFont(labelFont.getSize() + 1f));
    }

    private void setupLayout() {
        setTitle("EduZakhanh - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        // Label
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.insets = new Insets(5, 5, 5, 10);
        mainPanel.add(new JLabel("Password:"), gbc);

        // Username Label and Field
        gbc.gridy = 0;
        mainPanel.add(new JLabel("Username:"), gbc);

        // Cấu hình chung cho TextField/PasswordField
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 0, 5, 5);
        mainPanel.add(passwordField, gbc);

        // Username Field
        gbc.gridy = 0;
        mainPanel.add(usernameField, gbc);

        // Password Field
        gbc.gridy = 1;
        mainPanel.add(passwordField, gbc);

        // --- Panel chứa nút bấm: Dùng GridBagLayout để kiểm soát tốt hơn ---
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints btnGbc = new GridBagConstraints();

        // Cấu hình chung cho nút trong buttonPanel
        btnGbc.insets = new Insets(15, 5, 5, 5); // Khoảng cách trên cùng (từ password), giữa 2 nút
        btnGbc.fill = GridBagConstraints.HORIZONTAL; // Làm 2 nút có chiều rộng bằng nhau (tùy chọn)
        btnGbc.weightx = 0.5; // Chia đều không gian

        // Register Button (đặt bên trái)
        btnGbc.gridx = 0;
        btnGbc.gridy = 0;
        buttonPanel.add(registerButton, btnGbc);

        // Login Button (đặt bên phải)
        btnGbc.gridx = 1;
        btnGbc.gridy = 0;
        buttonPanel.add(loginButton, btnGbc);

        // Thêm buttonPanel vào mainPanel
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2; // Span 2 cột
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Cho buttonPanel giãn theo chiều ngang
        gbc.insets = new Insets(15, 5, 5, 5); // Khoảng cách trên (từ password field)
        mainPanel.add(buttonPanel, gbc);


        // Status Label
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Cho label chiếm hết chiều ngang
        gbc.insets = new Insets(10, 5, 0, 5); // Khoảng cách trên (từ nút), dưới (0)
        mainPanel.add(statusLabel, gbc);

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