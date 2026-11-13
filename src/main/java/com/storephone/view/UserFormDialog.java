package com.storephone.view;

import com.storephone.dto.UserAdminUpdateRequest;
import com.storephone.dto.UserCreateRequest;
import com.storephone.model.Role;
import com.storephone.model.User;
import com.storephone.service.UserService;
import com.storephone.util.ImageStorage;
import com.storephone.util.UiTaskRunner;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.nio.file.Path;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

public class UserFormDialog extends JDialog {
    public enum Mode { CREATE, EDIT, VIEW }

    private final UserService userService;
    private final ImageStorage imageStorage;
    private final Mode mode;
    private final User targetUser;
    private final Runnable onSaved;

    private final JLabel avatarPreview = new JLabel();
    private final JTextField emailField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JTextField fullNameField = new JTextField(22);
    private final JTextField phoneField = new JTextField(22);
    private final JTextField addressField = new JTextField(22);
    private final JComboBox<String> roleCombo = new JComboBox<>(new String[]{Role.USER, Role.ADMIN});
    private final JCheckBox activeCheck = new JCheckBox("Đang hoạt động", true);
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton saveBtn = UiTheme.primaryButton("Lưu");
    private Path selectedAvatar;

    public UserFormDialog(
            Window owner,
            Mode mode,
            User targetUser,
            UserService userService,
            ImageStorage imageStorage,
            Runnable onSaved
    ) {
        super(owner, switch (mode) {
            case CREATE -> "Thêm Tài Khoản Mới";
            case EDIT -> "Cập Nhật Tài Khoản";
            case VIEW -> "Chi Tiết Tài Khoản";
        }, ModalityType.APPLICATION_MODAL);
        this.mode = mode;
        this.targetUser = targetUser;
        this.userService = userService;
        this.imageStorage = imageStorage;
        this.onSaved = onSaved;
        buildUi();
        populate();
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(16, 12));
        root.setBackground(UiTheme.CARD_BG);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel avatarBox = new JPanel(new BorderLayout(6, 6));
        avatarBox.setOpaque(false);
        avatarPreview.setHorizontalAlignment(JLabel.CENTER);
        avatarPreview.setIcon(ImageStorage.createPlaceholderIcon(110, 110, "Avatar"));
        avatarBox.add(avatarPreview, BorderLayout.CENTER);
        if (mode != Mode.VIEW) {
            JButton pickBtn = UiTheme.navButton("Chọn ảnh...");
            pickBtn.addActionListener(e -> pickAvatar());
            avatarBox.add(pickBtn, BorderLayout.SOUTH);
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        addRow(form, gbc, "Email *", emailField);
        if (mode == Mode.CREATE) {
            addRow(form, gbc, "Mật khẩu * (>= 8 ký tự)", passwordField);
        }
        addRow(form, gbc, "Họ và tên *", fullNameField);
        addRow(form, gbc, "Số điện thoại", phoneField);
        addRow(form, gbc, "Địa chỉ", addressField);

        gbc.gridy++;
        gbc.insets = new Insets(6, 0, 2, 0);
        form.add(new JLabel("Vai trò *"), gbc);
        gbc.gridy++;
        form.add(roleCombo, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(6, 0, 4, 0);
        activeCheck.setOpaque(false);
        form.add(activeCheck, gbc);

        gbc.gridy++;
        errorLabel.setForeground(UiTheme.DANGER);
        form.add(errorLabel, gbc);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        JButton closeBtn = UiTheme.navButton(mode == Mode.VIEW ? "Đóng" : "Hủy");
        closeBtn.addActionListener(e -> dispose());
        if (mode != Mode.VIEW) {
            saveBtn.addActionListener(e -> handleSave());
            footer.add(saveBtn);
        }
        footer.add(closeBtn);

        root.add(avatarBox, BorderLayout.WEST);
        root.add(form, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private static void addRow(JPanel p, GridBagConstraints gbc, String lbl, JTextField field) {
        gbc.gridy++;
        gbc.insets = new Insets(4, 0, 2, 0);
        p.add(new JLabel(lbl), gbc);
        gbc.gridy++;
        p.add(field, gbc);
    }

    private void populate() {
        if (targetUser != null) {
            emailField.setText(targetUser.getEmail());
            emailField.setEditable(false);
            fullNameField.setText(targetUser.getFullName() == null ? "" : targetUser.getFullName());
            phoneField.setText(targetUser.getPhone() == null ? "" : targetUser.getPhone());
            addressField.setText(targetUser.getAddress() == null ? "" : targetUser.getAddress());
            roleCombo.setSelectedItem(targetUser.getRoleName());
            activeCheck.setSelected(targetUser.isActive());
            avatarPreview.setIcon(imageStorage.loadScaledIcon(targetUser.getAvatarPath(), 110, 110, "Avatar"));
        }
        if (mode == Mode.VIEW) {
            emailField.setEditable(false);
            fullNameField.setEditable(false);
            phoneField.setEditable(false);
            addressField.setEditable(false);
            roleCombo.setEnabled(false);
            activeCheck.setEnabled(false);
        }
    }

    private void pickAvatar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Ảnh (PNG, JPG, JPEG, WEBP)", "png", "jpg", "jpeg", "webp"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedAvatar = chooser.getSelectedFile().toPath();
            avatarPreview.setIcon(imageStorage.loadPreviewFromSource(selectedAvatar, 110, 110, "Avatar"));
        }
    }

    private void handleSave() {
        saveBtn.setEnabled(false);
        String role = String.valueOf(roleCombo.getSelectedItem());
        if (mode == Mode.CREATE) {
            char[] pwd = passwordField.getPassword();
            UserCreateRequest req = new UserCreateRequest(
                    emailField.getText(), pwd, fullNameField.getText(),
                    phoneField.getText(), addressField.getText(), role,
                    selectedAvatar, activeCheck.isSelected()
            );
            Arrays.fill(pwd, '\0');
            passwordField.setText("");
            UiTaskRunner.runAsync(() -> userService.createUserByAdmin(req), saved -> {
                if (onSaved != null) { onSaved.run(); }
                dispose();
            }, err -> {
                saveBtn.setEnabled(true);
                errorLabel.setText(err.getMessage());
            });
        } else {
            UserAdminUpdateRequest req = new UserAdminUpdateRequest(
                    targetUser.getId(), fullNameField.getText(), phoneField.getText(),
                    addressField.getText(), role, selectedAvatar, activeCheck.isSelected()
            );
            UiTaskRunner.runAsync(() -> userService.updateUserByAdmin(req), saved -> {
                if (onSaved != null) { onSaved.run(); }
                dispose();
            }, err -> {
                saveBtn.setEnabled(true);
                errorLabel.setText(err.getMessage());
            });
        }
    }
}


