package com.storephone.view;

import com.storephone.dto.PageResult;
import com.storephone.model.User;
import com.storephone.service.UserService;
import com.storephone.util.ImageStorage;
import com.storephone.util.UiTaskRunner;
import com.storephone.view.table.UserTableModel;
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

public class AdminUserPanel extends JPanel {
    private static final int PAGE_SIZE = 10;

    private final UserService userService;
    private final ImageStorage imageStorage;
    private final JTextField searchField = new JTextField(22);
    private final UserTableModel tableModel = new UserTableModel();
    private final JTable table = new JTable(tableModel);
    private final JLabel pageInfoLabel = new JLabel("Trang 1 / 1");
    private final JButton prevBtn = UiTheme.navButton("« Trước");
    private final JButton nextBtn = UiTheme.navButton("Sau »");

    private int currentPage = 1;
    private int totalPages = 1;

    public AdminUserPanel(UserService userService, ImageStorage imageStorage) {
        super(new BorderLayout(12, 12));
        this.userService = userService;
        this.imageStorage = imageStorage;
        setBackground(UiTheme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        buildUi();
    }

    private void buildUi() {
        JPanel top = new JPanel(new BorderLayout(12, 8));
        top.setOpaque(false);
        JLabel title = new JLabel("Quản Lý Người Dùng (ADMIN)");
        title.setFont(UiTheme.FONT_TITLE);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        JButton searchBtn = UiTheme.navButton("Tìm kiếm");
        JButton createBtn = UiTheme.primaryButton("+ Thêm tài khoản");
        JButton viewBtn = UiTheme.navButton("Xem chi tiết");
        JButton editBtn = UiTheme.navButton("Sửa / Phân quyền");
        JButton toggleActiveBtn = UiTheme.navButton("Khóa / Mở khóa");
        JButton refreshBtn = UiTheme.navButton("Làm mới");

        toolbar.add(new JLabel("Từ khóa:"));
        toolbar.add(searchField);
        toolbar.add(searchBtn);
        toolbar.add(createBtn);
        toolbar.add(viewBtn);
        toolbar.add(editBtn);
        toolbar.add(toggleActiveBtn);
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

        createBtn.addActionListener(e -> openDialog(UserFormDialog.Mode.CREATE, null));
        viewBtn.addActionListener(e -> withSelected(u -> openDialog(UserFormDialog.Mode.VIEW, u)));
        editBtn.addActionListener(e -> withSelected(u -> openDialog(UserFormDialog.Mode.EDIT, u)));
        toggleActiveBtn.addActionListener(e -> withSelected(this::toggleStatus));
    }

    public void reloadPage() {
        String kw = searchField.getText();
        UiTaskRunner.runAsync(
                () -> userService.searchUsersForAdmin(kw, currentPage, PAGE_SIZE),
                this::applyPage,
                err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Lỗi tải danh sách", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void applyPage(PageResult<User> page) {
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

    private void withSelected(java.util.function.Consumer<User> action) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một người dùng trong bảng.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        User selected = tableModel.getAt(row);
        if (selected != null) {
            action.accept(selected);
        }
    }

    private void openDialog(UserFormDialog.Mode mode, User selected) {
        if (selected == null) {
            new UserFormDialog(SwingUtilities.getWindowAncestor(this), mode, null, userService, imageStorage, this::reloadPage).setVisible(true);
            return;
        }
        UiTaskRunner.runAsync(
                () -> userService.getUserDetailForAdmin(selected.getId()),
                fresh -> new UserFormDialog(SwingUtilities.getWindowAncestor(this), mode, fresh, userService, imageStorage, this::reloadPage).setVisible(true),
                err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void toggleStatus(User user) {
        boolean nextStatus = !user.isActive();
        String verb = nextStatus ? "mở khóa" : "khóa";
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn " + verb + " tài khoản '" + user.getEmail() + "'?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        UiTaskRunner.runAsync(
                () -> {
                    userService.setUserActiveStatus(user.getId(), nextStatus);
                    return null;
                },
                ok -> reloadPage(),
                err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Không thể thay đổi trạng thái", JOptionPane.WARNING_MESSAGE)
        );
    }
}


