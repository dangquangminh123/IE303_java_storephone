package com.storephone.view;

import com.storephone.dto.ProfileUpdateRequest;
import com.storephone.model.User;
import com.storephone.service.UserService;
import com.storephone.util.ImageStorage;
import com.storephone.util.UiTaskRunner;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ProfilePanel extends JPanel {
    private final UserService userService;
    private final ImageStorage imageStorage;
    private final JLabel avatarPreview = new JLabel();
    private final JTextField emailField = new JTextField(24);
    private final JTextField roleField = new JTextField(24);
    private final JTextField fullNameField = new JTextField(24);
    private final JTextField phoneField = new JTextField(24);
    private final JTextField addressField = new JTextField(24);
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton chooseAvatarBtn = UiTheme.navButton("Chọn ảnh đại diện...");
    private final JButton saveBtn = UiTheme.primaryButton("Lưu hồ sơ");
    private Path selectedAvatarFile;

    public ProfilePanel(UserService userService, ImageStorage imageStorage) {
        super(new BorderLayout(16, 16));
        this.userService = userService;
        this.imageStorage = imageStorage;
        setBackground(UiTheme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUi();
    }

    private void buildUi() {
        JLabel heading = new JLabel("Hồ Sơ Cá Nhân");
        heading.setFont(UiTheme.FONT_TITLE);
        add(heading, BorderLayout.NORTH);

        JPanel card = new JPanel(new BorderLayout(24, 16));
        card.setBackground(UiTheme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(24, 24, 24, 24)
        ));

        JPanel avatarBox = new JPanel(new BorderLayout(8, 8));
        avatarBox.setOpaque(false);
        avatarPreview.setHorizontalAlignment(JLabel.CENTER);
        avatarPreview.setIcon(ImageStorage.createPlaceholderIcon(140, 140, "Avatar"));
        avatarBox.add(avatarPreview, BorderLayout.CENTER);
        avatarBox.add(chooseAvatarBtn, BorderLayout.SOUTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        emailField.setEditable(false);
        roleField.setEditable(false);
        addRow(form, gbc, "Email đăng nhập", emailField);
        addRow(form, gbc, "Vai trò", roleField);
        addRow(form, gbc, "Họ và tên *", fullNameField);
        addRow(form, gbc, "Số điện thoại", phoneField);
        addRow(form, gbc, "Địa chỉ nhận hàng", addressField);

        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 8, 0);
        form.add(statusLabel, gbc);

        gbc.gridy++;
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        actions.add(saveBtn);
        form.add(actions, gbc);

        card.add(avatarBox, BorderLayout.WEST);
        card.add(form, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        chooseAvatarBtn.addActionListener(e -> pickAvatar());
        saveBtn.addActionListener(e -> saveProfile());
    }

    private static void addRow(JPanel form, GridBagConstraints gbc, String label, JTextField field) {
        gbc.gridy++;
        gbc.insets = new Insets(6, 0, 2, 0);
        JLabel l = new JLabel(label);
        l.setFont(UiTheme.FONT_SECTION);
        form.add(l, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0);
        field.setFont(UiTheme.FONT_BODY);
        form.add(field, gbc);
    }

    public void reloadProfile() {
        selectedAvatarFile = null;
        UiTaskRunner.runAsync(userService::getCurrentUserProfile, this::populateUser, err -> {
            statusLabel.setForeground(UiTheme.DANGER);
            statusLabel.setText(err.getMessage());
        });
    }

    private void populateUser(User u) {
        emailField.setText(u.getEmail());
        roleField.setText(u.getRoleName());
        fullNameField.setText(u.getFullName() == null ? "" : u.getFullName());
        phoneField.setText(u.getPhone() == null ? "" : u.getPhone());
        addressField.setText(u.getAddress() == null ? "" : u.getAddress());
        avatarPreview.setIcon(imageStorage.loadScaledIcon(u.getAvatarPath(), 140, 140, "Avatar"));
        statusLabel.setText(" ");
    }

    private void pickAvatar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Ảnh (PNG, JPG, JPEG, WEBP)", "png", "jpg", "jpeg", "webp"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedAvatarFile = chooser.getSelectedFile().toPath();
            avatarPreview.setIcon(imageStorage.loadPreviewFromSource(selectedAvatarFile, 140, 140, "Avatar"));
        }
    }

    private void saveProfile() {
        ProfileUpdateRequest req = new ProfileUpdateRequest(
                fullNameField.getText(), phoneField.getText(), addressField.getText(), selectedAvatarFile);
        saveBtn.setEnabled(false);
        UiTaskRunner.runAsync(() -> userService.updateCurrentUserProfile(req), updated -> {
            saveBtn.setEnabled(true);
            selectedAvatarFile = null;
            populateUser(updated);
            statusLabel.setForeground(UiTheme.SUCCESS);
            statusLabel.setText("Đã cập nhật hồ sơ thành công!");
        }, err -> {
            saveBtn.setEnabled(true);
            statusLabel.setForeground(UiTheme.DANGER);
            statusLabel.setText(err.getMessage());
        });
    }
}

