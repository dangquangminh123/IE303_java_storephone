package com.storephone.view;

import com.storephone.dto.ProductSaveRequest;
import com.storephone.model.Product;
import com.storephone.service.ProductService;
import com.storephone.util.ImageStorage;
import com.storephone.util.MoneyFormatter;
import com.storephone.util.UiTaskRunner;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.math.BigDecimal;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ProductFormDialog extends JDialog {
    public enum Mode { CREATE, EDIT, VIEW }

    public static final String[] DEFAULT_FACTORIES = {
            "Apple", "Samsung", "Xiaomi", "OPPO", "Vivo", "Realme", "ASUS", "Google"
    };
    public static final String[] DEFAULT_TARGETS = {
            "Mỏng nhẹ / Thời trang",
            "Chơi game / Cấu hình cao",
            "Chụp ảnh / Quay phim",
            "Pin trâu / Phổ thông",
            "Doanh nhân / Gập mở"
    };

    private final Mode mode;
    private final Product targetProduct;
    private final ProductService productService;
    private final ImageStorage imageStorage;
    private final Runnable onSaved;

    private final JLabel imagePreview = new JLabel();
    private final JTextField nameField = new JTextField(24);
    private final JTextField priceField = new JTextField(24);
    private final JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(10, 0, 100000, 1));
    private final JComboBox<String> factoryCombo = new JComboBox<>(DEFAULT_FACTORIES);
    private final JComboBox<String> targetCombo = new JComboBox<>(DEFAULT_TARGETS);
    private final JTextField shortDescField = new JTextField(24);
    private final JTextArea detailDescArea = new JTextArea(4, 24);
    private final JCheckBox activeCheck = new JCheckBox("Đang mở bán", true);
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton saveBtn = UiTheme.primaryButton("Lưu điện thoại");
    private Path selectedImage;

    public ProductFormDialog(
            Window owner,
            Mode mode,
            Product targetProduct,
            ProductService productService,
            ImageStorage imageStorage,
            Runnable onSaved
    ) {
        super(owner, switch (mode) {
            case CREATE -> "Thêm Điện Thoại Mới";
            case EDIT -> "Cập Nhật Điện Thoại";
            case VIEW -> "Chi Tiết Điện Thoại (Admin)";
        }, ModalityType.APPLICATION_MODAL);
        this.mode = mode;
        this.targetProduct = targetProduct;
        this.productService = productService;
        this.imageStorage = imageStorage;
        this.onSaved = onSaved;
        factoryCombo.setEditable(true);
        targetCombo.setEditable(true);
        buildUi();
        populate();
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(16, 12));
        root.setBackground(UiTheme.CARD_BG);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel imgBox = new JPanel(new BorderLayout(6, 6));
        imgBox.setOpaque(false);
        imagePreview.setHorizontalAlignment(JLabel.CENTER);
        imagePreview.setIcon(ImageStorage.createPlaceholderIcon(140, 140, "Ảnh Máy"));
        imgBox.add(imagePreview, BorderLayout.CENTER);
        if (mode != Mode.VIEW) {
            JButton chooseBtn = UiTheme.navButton("Chọn ảnh máy...");
            chooseBtn.addActionListener(e -> pickImage());
            imgBox.add(chooseBtn, BorderLayout.SOUTH);
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        addRow(form, gbc, "Tên điện thoại *", nameField);
        addRow(form, gbc, "Giá bán VND * (VD: 24990000)", priceField);

        gbc.gridy++;
        gbc.insets = new Insets(4, 0, 2, 0);
        form.add(new JLabel("Số lượng tồn kho *"), gbc);
        gbc.gridy++;
        form.add(quantitySpinner, gbc);

        gbc.gridy++;
        form.add(new JLabel("Hãng sản xuất *"), gbc);
        gbc.gridy++;
        form.add(factoryCombo, gbc);

        gbc.gridy++;
        form.add(new JLabel("Mục đích sử dụng *"), gbc);
        gbc.gridy++;
        form.add(targetCombo, gbc);

        addRow(form, gbc, "Mô tả ngắn *", shortDescField);

        gbc.gridy++;
        form.add(new JLabel("Mô tả chi tiết"), gbc);
        gbc.gridy++;
        detailDescArea.setLineWrap(true);
        detailDescArea.setWrapStyleWord(true);
        form.add(new JScrollPane(detailDescArea), gbc);

        gbc.gridy++;
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

        root.add(imgBox, BorderLayout.WEST);
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
        if (targetProduct != null) {
            nameField.setText(targetProduct.getName());
            priceField.setText(targetProduct.getPrice().toPlainString());
            quantitySpinner.setValue(targetProduct.getQuantity());
            factoryCombo.setSelectedItem(targetProduct.getFactory());
            targetCombo.setSelectedItem(targetProduct.getTarget());
            shortDescField.setText(targetProduct.getShortDesc() == null ? "" : targetProduct.getShortDesc());
            detailDescArea.setText(targetProduct.getDetailDesc() == null ? "" : targetProduct.getDetailDesc());
            activeCheck.setSelected(targetProduct.isActive());
            imagePreview.setIcon(imageStorage.loadScaledIcon(targetProduct.getImagePath(), 140, 140, "Ảnh Máy"));
        }
        if (mode == Mode.VIEW) {
            nameField.setEditable(false);
            priceField.setEditable(false);
            quantitySpinner.setEnabled(false);
            factoryCombo.setEnabled(false);
            targetCombo.setEnabled(false);
            shortDescField.setEditable(false);
            detailDescArea.setEditable(false);
            activeCheck.setEnabled(false);
        }
    }

    private void pickImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Ảnh (PNG, JPG, JPEG, WEBP)", "png", "jpg", "jpeg", "webp"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedImage = chooser.getSelectedFile().toPath();
            imagePreview.setIcon(imageStorage.loadPreviewFromSource(selectedImage, 140, 140, "Ảnh Máy"));
        }
    }

    private void handleSave() {
        final BigDecimal parsedPrice;
        try {
            parsedPrice = MoneyFormatter.parseVnd(priceField.getText());
        } catch (RuntimeException ex) {
            errorLabel.setText(ex.getMessage());
            return;
        }
        int qty = ((Number) quantitySpinner.getValue()).intValue();
        Object factoryVal = factoryCombo.getEditor().getItem();
        Object targetVal = targetCombo.getEditor().getItem();
        ProductSaveRequest req = new ProductSaveRequest(
                nameField.getText(),
                parsedPrice,
                shortDescField.getText(),
                detailDescArea.getText(),
                qty,
                factoryVal == null ? String.valueOf(factoryCombo.getSelectedItem()) : factoryVal.toString(),
                targetVal == null ? String.valueOf(targetCombo.getSelectedItem()) : targetVal.toString(),
                selectedImage,
                activeCheck.isSelected()
        );
        saveBtn.setEnabled(false);
        UiTaskRunner.runAsync(
                () -> mode == Mode.CREATE
                        ? productService.createProduct(req)
                        : productService.updateProduct(targetProduct.getId(), req),
                saved -> {
                    if (onSaved != null) {
                        onSaved.run();
                    }
                    dispose();
                },
                err -> {
                    saveBtn.setEnabled(true);
                    errorLabel.setText(err.getMessage());
                }
        );
    }
}


