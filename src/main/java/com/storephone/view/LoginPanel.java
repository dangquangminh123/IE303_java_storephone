package com.storephone.view;

import com.storephone.controller.AuthController;
import com.storephone.controller.NavigationController;
import com.storephone.controller.NavigationRoutes;
import com.storephone.dto.LoginRequest;
import com.storephone.util.UiTaskRunner;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginPanel extends JPanel {
    private final AuthController authController;
    private final JTextField emailField = new JTextField(26);
    private final JPasswordField passwordField = new JPasswordField(26);
    private final JLabel feedbackLabel = new JLabel(" ");
    private final JButton submitBtn = UiTheme.primaryButton("Đăng nhập");
    private final JButton toRegisterBtn = UiTheme.navButton("Chưa có tài khoản? Đăng ký ngay");

    public LoginPanel(AuthController authController, NavigationController nav) {
        super(new GridBagLayout());
        this.authController = authController;
        setBackground(UiTheme.CONTENT_BG);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(UiTheme.CARD_BG);
        card.setPreferredSize(new Dimension(460, 360));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(28, 32, 28, 32)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 8, 0);

        JLabel title = new JLabel("Đăng Nhập Tài Khoản");
        title.setFont(UiTheme.FONT_TITLE);
        title.setForeground(UiTheme.TEXT_PRIMARY);
        card.add(title, gbc);

        gbc.gridy++;
        JLabel sub = new JLabel("Đăng nhập để quản lý giỏ hàng, đặt mua điện thoại hoặc quản trị hệ thống.");
        sub.setFont(UiTheme.FONT_BODY);
        sub.setForeground(UiTheme.TEXT_MUTED);
        card.add(sub, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(14, 0, 4, 0);
        JLabel emailLbl = new JLabel("Email đăng nhập *");
        emailLbl.setFont(UiTheme.FONT_SECTION);
        card.add(emailLbl, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 10, 0);
        emailField.setFont(UiTheme.FONT_BODY);
        card.add(emailField, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(4, 0, 4, 0);
        JLabel pwdLbl = new JLabel("Mật khẩu *");
        pwdLbl.setFont(UiTheme.FONT_SECTION);
        card.add(pwdLbl, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 10, 0);
        passwordField.setFont(UiTheme.FONT_BODY);
        card.add(passwordField, gbc);

        gbc.gridy++;
        feedbackLabel.setFont(UiTheme.FONT_BODY);
        feedbackLabel.setForeground(UiTheme.DANGER);
        card.add(feedbackLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 0, 0);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        actions.add(submitBtn);
        actions.add(toRegisterBtn);
        card.add(actions, gbc);

        submitBtn.addActionListener(e -> handleLogin());
        passwordField.addActionListener(e -> handleLogin());
        toRegisterBtn.addActionListener(e -> nav.navigateTo(NavigationRoutes.REGISTER));

        add(card);
    }

    public void resetForm() {
        passwordField.setText("");
        feedbackLabel.setText(" ");
        submitBtn.setEnabled(true);
    }

    public void prefillEmail(String email) {
        resetForm();
        if (email != null) {
            emailField.setText(email);
            feedbackLabel.setForeground(UiTheme.SUCCESS);
            feedbackLabel.setText("Đăng ký thành công! Mời bạn nhập mật khẩu để đăng nhập.");
        }
    }

    private void handleLogin() {
        char[] rawPwd = passwordField.getPassword();
        LoginRequest req = new LoginRequest(emailField.getText(), rawPwd);
        Arrays.fill(rawPwd, '\0');
        passwordField.setText("");

        submitBtn.setEnabled(false);
        feedbackLabel.setForeground(UiTheme.TEXT_MUTED);
        feedbackLabel.setText("Đang xác thực tài khoản...");

        UiTaskRunner.runAsync(
                () -> authController.login(req),
                session -> {
                    submitBtn.setEnabled(true);
                    feedbackLabel.setText(" ");
                    emailField.setText("");
                    authController.navigateAfterLogin(session);
                },
                err -> {
                    submitBtn.setEnabled(true);
                    feedbackLabel.setForeground(UiTheme.DANGER);
                    feedbackLabel.setText(err.getMessage());
                }
        );
    }
}
