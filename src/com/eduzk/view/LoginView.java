package com.eduzk.view;

import com.eduzk.controller.AuthController;

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
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Login");
        registerButton = new JButton("Register");
        statusLabel = new JLabel(" "); // Initialize with space for layout stability
        statusLabel.setForeground(Color.RED);
    }

    private void setupLayout() {
        setTitle("EduZakhanh - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Username Label and Field
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        mainPanel.add(usernameField, gbc);

        // Password Label and Field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE; // Reset fill
        gbc.weightx = 0.0;             // Reset weight
        mainPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        mainPanel.add(passwordField, gbc);

        // Button Panel (Chứa cả Login và Register)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        // Login Button
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2; // Span across both columns
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(buttonPanel, gbc);


        // Status Label
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
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
        pack(); // Adjust window size to fit components
        setLocationRelativeTo(null); // Center on screen (uses UIUtils internally in newer Swing)
        // For older Java or more precise centering: UIUtils.centerWindow(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }


    private void performLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        statusLabel.setText(" "); // Clear previous status

        // Disable button during login attempt
        loginButton.setEnabled(false);
        statusLabel.setText("Logging in...");

        // Use SwingWorker for background task to avoid freezing UI
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                authController.attemptLogin(username, password);
                return null;
            }

            @Override
            protected void done() {
                // Re-enable button and clear status label on completion (success or failure)
                loginButton.setEnabled(true);
                statusLabel.setText(" "); // Clear "Logging in..."
                // Error messages are shown by the controller via UIUtils
            }
        };
        worker.execute();

    }

    // Method for controller to potentially show messages directly (alternative to UIUtils)
    public void showLoginError(String message) {
        statusLabel.setText(message);
        // Optional: Clear password field on error
        passwordField.setText("");
        usernameField.requestFocusInWindow(); // Focus username again
    }

}