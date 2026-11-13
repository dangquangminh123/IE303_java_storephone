package com.storephone.view.table;

import com.storephone.model.Order;
import com.storephone.util.MoneyFormatter;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

public class OrderTableModel extends AbstractTableModel {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] COLUMNS = {
            "Mã đơn", "Người nhận", "Số điện thoại", "Tổng tiền", "Trạng thái", "Ngày đặt"
    };
    private final List<Order> rows = new ArrayList<>();

    public void setRows(List<Order> orders) {
        rows.clear();
        if (orders != null) {
            rows.addAll(orders);
        }
        fireTableDataChanged();
    }

    public Order getAt(int rowIndex) {
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
        Order o = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> "#" + o.getId();
            case 1 -> o.getReceiverName();
            case 2 -> o.getReceiverPhone();
            case 3 -> MoneyFormatter.formatVnd(o.getTotalAmount());
            case 4 -> o.getStatus().getDisplayName();
            case 5 -> o.getCreatedAt() == null ? "-" : o.getCreatedAt().format(FMT);
            default -> "";
        };
    }
}
