package com.storephone.view.table;

import com.storephone.model.User;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

public class UserTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"ID", "Email", "Họ và tên", "Số điện thoại", "Vai trò", "Trạng thái"};
    private final List<User> rows = new ArrayList<>();

    public void setRows(List<User> users) {
        rows.clear();
        if (users != null) {
            rows.addAll(users);
        }
        fireTableDataChanged();
    }

    public User getAt(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return null;
        }
        return rows.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        User u = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> u.getId();
            case 1 -> u.getEmail();
            case 2 -> u.getFullName();
            case 3 -> u.getPhone() == null ? "-" : u.getPhone();
            case 4 -> u.getRoleName();
            case 5 -> u.isActive() ? "Hoạt động" : "Đã khóa";
            default -> "";
        };
    }
}
