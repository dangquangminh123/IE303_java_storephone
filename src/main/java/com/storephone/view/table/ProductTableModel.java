package com.storephone.view.table;

import com.storephone.model.Product;
import com.storephone.util.MoneyFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

public class ProductTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {
            "ID", "Tên điện thoại", "Hãng", "Mục đích", "Giá bán", "Tồn kho", "Trạng thái"
    };
    private final List<Product> rows = new ArrayList<>();

    public void setRows(List<Product> products) {
        rows.clear();
        if (products != null) {
            rows.addAll(products);
        }
        fireTableDataChanged();
    }

    public Product getAt(int rowIndex) {
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
        Product p = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> p.getId();
            case 1 -> p.getName();
            case 2 -> p.getFactory();
            case 3 -> p.getTarget();
            case 4 -> MoneyFormatter.formatVnd(p.getPrice());
            case 5 -> p.getQuantity();
            case 6 -> p.isActive() ? "Đang bán" : "Ngừng bán";
            default -> "";
        };
    }
}
