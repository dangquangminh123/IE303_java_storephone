package com.storephone.view;

import com.storephone.dto.PageResult;
import com.storephone.model.Product;
import com.storephone.service.ProductService;
import com.storephone.util.ImageStorage;
import com.storephone.util.UiTaskRunner;
import com.storephone.view.table.ProductTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

public class AdminProductPanel extends JPanel {
    private static final int PAGE_SIZE = 10;

    private final ProductService productService;
    private final ImageStorage imageStorage;
    private final JTextField searchField = new JTextField(20);
    private final ProductTableModel tableModel = new ProductTableModel();
    private final JTable table = new JTable(tableModel);
    private final JLabel pageInfoLabel = new JLabel("Trang 1 / 1");
    private final JButton prevBtn = UiTheme.navButton("« Trước");
    private final JButton nextBtn = UiTheme.navButton("Sau »");

    private int currentPage = 1;
    private int totalPages = 1;

    public AdminProductPanel(ProductService productService, ImageStorage imageStorage) {
        super(new BorderLayout(12, 12));
        this.productService = productService;
        this.imageStorage = imageStorage;
        setBackground(UiTheme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        buildUi();
    }

    private void buildUi() {
        JPanel top = new JPanel(new BorderLayout(12, 8));
        top.setOpaque(false);
        JLabel title = new JLabel("Quản Lý Điện Thoại (ADMIN)");
        title.setFont(UiTheme.FONT_TITLE);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        JButton searchBtn = UiTheme.navButton("Tìm");
        JButton createBtn = UiTheme.primaryButton("+ Thêm điện thoại");
        JButton viewBtn = UiTheme.navButton("Xem chi tiết");
        JButton editBtn = UiTheme.navButton("Cập nhật");
        JButton toggleBtn = UiTheme.navButton("Ngừng bán / Mở bán");
        JButton deleteBtn = UiTheme.navButton("Xóa an toàn");
        JButton refreshBtn = UiTheme.navButton("Làm mới");

        toolbar.add(new JLabel("Tên máy:"));
        toolbar.add(searchField);
        toolbar.add(searchBtn);
        toolbar.add(createBtn);
        toolbar.add(viewBtn);
        toolbar.add(editBtn);
        toolbar.add(toggleBtn);
        toolbar.add(deleteBtn);
        toolbar.add(refreshBtn);

        top.add(title, BorderLayout.NORTH);
        top.add(toolbar, BorderLayout.SOUTH);

        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);

        JPanel pager = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pager.setOpaque(false);
        pager.add(prevBtn);
        pager.add(pageInfoLabel);
        pager.add(nextBtn);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(pager, BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> { currentPage = 1; reloadPage(); });
        searchField.addActionListener(e -> { currentPage = 1; reloadPage(); });
        refreshBtn.addActionListener(e -> reloadPage());
        prevBtn.addActionListener(e -> { if (currentPage > 1) { currentPage--; reloadPage(); } });
        nextBtn.addActionListener(e -> { if (currentPage < totalPages) { currentPage++; reloadPage(); } });

        createBtn.addActionListener(e -> openDialog(ProductFormDialog.Mode.CREATE, null));
        viewBtn.addActionListener(e -> withSelected(p -> openDialog(ProductFormDialog.Mode.VIEW, p)));
        editBtn.addActionListener(e -> withSelected(p -> openDialog(ProductFormDialog.Mode.EDIT, p)));
        toggleBtn.addActionListener(e -> withSelected(this::toggleActive));
        deleteBtn.addActionListener(e -> withSelected(this::deleteProduct));
    }

    public void reloadPage() {
        String kw = searchField.getText();
        UiTaskRunner.runAsync(
                () -> productService.searchProductsForAdmin(kw, currentPage, PAGE_SIZE),
                this::applyPage,
                err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Lỗi tải điện thoại", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void applyPage(PageResult<Product> page) {
        this.totalPages = page.getTotalPages();
        if (currentPage > totalPages) {
            currentPage = totalPages;
            reloadPage();
            return;
        }
        tableModel.setRows(page.getItems());
        pageInfoLabel.setText("Trang " + page.getPage() + " / " + totalPages + " (Tổng: " + page.getTotalElements() + ")");
        prevBtn.setEnabled(page.getPage() > 1);
        nextBtn.setEnabled(page.getPage() < totalPages);
    }

    private void withSelected(java.util.function.Consumer<Product> action) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một điện thoại trong bảng.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Product p = tableModel.getAt(row);
        if (p != null) {
            action.accept(p);
        }
    }

    private void openDialog(ProductFormDialog.Mode mode, Product selected) {
        if (selected == null) {
            new ProductFormDialog(SwingUtilities.getWindowAncestor(this), mode, null, productService, imageStorage, this::reloadPage).setVisible(true);
            return;
        }
        UiTaskRunner.runAsync(
                () -> productService.getProductForAdmin(selected.getId()),
                fresh -> new ProductFormDialog(SwingUtilities.getWindowAncestor(this), mode, fresh, productService, imageStorage, this::reloadPage).setVisible(true),
                err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void toggleActive(Product p) {
        boolean next = !p.isActive();
        UiTaskRunner.runAsync(
                () -> { productService.setProductActiveStatus(p.getId(), next); return null; },
                ok -> reloadPage(),
                err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Lỗi", JOptionPane.WARNING_MESSAGE)
        );
    }

    private void deleteProduct(Product p) {
        int c = JOptionPane.showConfirmDialog(this,
                "Xóa điện thoại '" + p.getName() + "'? (Nếu máy đã có trong giỏ/đơn hàng sẽ chuyển sang Ngừng bán)",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) {
            return;
        }
        UiTaskRunner.runAsync(
                () -> productService.deactivateOrDeleteProduct(p.getId()),
                hardDeleted -> {
                    reloadPage();
                    String msg = hardDeleted
                            ? "Đã xóa vĩnh viễn sản phẩm chưa phát sinh giao dịch."
                            : "Sản phẩm đã có dữ liệu giỏ/đơn nên đã được chuyển sang Ngừng bán (an toàn lịch sử).";
                    JOptionPane.showMessageDialog(this, msg, "Kết quả", JOptionPane.INFORMATION_MESSAGE);
                },
                err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE)
        );
    }
}


