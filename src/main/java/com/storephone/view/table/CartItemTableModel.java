package com.storephone.view.table;

import com.storephone.model.CartItem;
import com.storephone.util.MoneyFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

public class CartItemTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"Mã SP", "Tên điện thoại", "Đơn giá", "Số lượng", "Thành tiền"};
    private final List<CartItem> rows = new ArrayList<>();

    public void setRows(List<CartItem> items) {
        rows.clear();
        if (items != null) {
            rows.addAll(items);
        }
        fireTableDataChanged();
    }

    public CartItem getAt(int rowIndex) {
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
        CartItem item = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> item.getProductId();
            case 1 -> item.getProductName();
            case 2 -> MoneyFormatter.formatVnd(item.getUnitPrice());
            case 3 -> item.getQuantity();
            case 4 -> MoneyFormatter.formatVnd(item.getLineTotal());
            default -> "";
        };
    }
}
