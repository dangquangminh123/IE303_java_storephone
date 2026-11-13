package com.storephone.view;

import com.storephone.controller.AuthController;
import com.storephone.controller.NavigationController;
import com.storephone.controller.NavigationRoutes;
import com.storephone.dto.RegisterRequest;
import com.storephone.util.UiTaskRunner;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class RegisterPanel extends JPanel {
    private final AuthController authController;
    private final NavigationController nav;
    private final Consumer<String> onRegisteredCallback;

    private final JTextField fullNameField = new JTextField(26);
    private final JTextField emailField = new JTextField(26);
    private final JTextField phoneField = new JTextField(26);
    private final JTextField addressField = new JTextField(26);
    private final JPasswordField passwordField = new JPasswordField(26);
    private final JPasswordField confirmPasswordField = new JPasswordField(26);
    private final JLabel feedbackLabel = new JLabel(" ");
    private final JButton submitBtn = UiTheme.primaryButton("Tạo tài khoản USER");
    private final JButton toLoginBtn = UiTheme.navButton("Đã có tài khoản? Đăng nhập");

    public RegisterPanel(
            AuthController authController,
            NavigationController nav,
            Consumer<String> onRegisteredCallback
    ) {
        super(new GridBagLayout());
        this.authController = authController;
        this.nav = nav;
        this.onRegisteredCallback = onRegisteredCallback;
        setBackground(UiTheme.CONTENT_BG);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(UiTheme.CARD_BG);
        card.setPreferredSize(new Dimension(520, 540));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(24, 32, 24, 32)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 6, 0);

        JLabel title = new JLabel("Đăng Ký Tài Khoản Khách Hàng");
        title.setFont(UiTheme.FONT_TITLE);
        title.setForeground(UiTheme.TEXT_PRIMARY);
        card.add(title, gbc);

        addField(card, gbc, "Họ và tên *", fullNameField);
        addField(card, gbc, "Email đăng nhập *", emailField);
        addField(card, gbc, "Số điện thoại (tùy chọn, VD: 0912345678)", phoneField);
        addField(card, gbc, "Địa chỉ nhận hàng (tùy chọn)", addressField);
        addField(card, gbc, "Mật khẩu * (tối thiểu 8 ký tự)", passwordField);
        addField(card, gbc, "Xác nhận mật khẩu *", confirmPasswordField);

        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 8, 0);
        feedbackLabel.setFont(UiTheme.FONT_BODY);
        feedbackLabel.setForeground(UiTheme.DANGER);
        card.add(feedbackLabel, gbc);

        gbc.gridy++;
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        actions.add(submitBtn);
        actions.add(toLoginBtn);
        card.add(actions, gbc);

        submitBtn.addActionListener(e -> handleRegister());
        toLoginBtn.addActionListener(e -> nav.navigateTo(NavigationRoutes.LOGIN));

        add(card);
    }

    private static void addField(JPanel card, GridBagConstraints gbc, String labelText, JTextField field) {
        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 3, 0);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UiTheme.FONT_SECTION);
        card.add(lbl, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0);
        field.setFont(UiTheme.FONT_BODY);
        card.add(field, gbc);
    }

    public void resetForm() {
        passwordField.setText("");
        confirmPasswordField.setText("");
        feedbackLabel.setText(" ");
        submitBtn.setEnabled(true);
    }

    private void handleRegister() {
        char[] pwd = passwordField.getPassword();
        char[] confirm = confirmPasswordField.getPassword();
        RegisterRequest req = new RegisterRequest(
                fullNameField.getText(),
                emailField.getText(),
                phoneField.getText(),
                addressField.getText(),
                pwd,
                confirm
        );
        Arrays.fill(pwd, '\0');
        Arrays.fill(confirm, '\0');
        passwordField.setText("");
        confirmPasswordField.setText("");

        submitBtn.setEnabled(false);
        feedbackLabel.setForeground(UiTheme.TEXT_MUTED);
        feedbackLabel.setText("Đang tạo tài khoản...");

        UiTaskRunner.runAsync(
                () -> authController.register(req),
                savedUser -> {
                    submitBtn.setEnabled(true);
                    fullNameField.setText("");
                    emailField.setText("");
                    phoneField.setText("");
                    addressField.setText("");
                    feedbackLabel.setText(" ");
                    nav.navigateTo(NavigationRoutes.LOGIN);
                    if (onRegisteredCallback != null) {
                        onRegisteredCallback.accept(savedUser.getEmail());
                    }
                },
                err -> {
                    submitBtn.setEnabled(true);
                    feedbackLabel.setForeground(UiTheme.DANGER);
                    feedbackLabel.setText(err.getMessage());
                }
        );
    }
}
