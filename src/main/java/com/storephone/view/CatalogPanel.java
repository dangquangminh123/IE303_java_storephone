package com.storephone.view;

import com.storephone.controller.NavigationController;
import com.storephone.controller.NavigationRoutes;
import com.storephone.dto.PageResult;
import com.storephone.dto.ProductFilterCriteria;
import com.storephone.model.Product;
import com.storephone.security.AppSession;
import com.storephone.service.CartService;
import com.storephone.service.ProductService;
import com.storephone.util.ImageStorage;
import com.storephone.util.MoneyFormatter;
import com.storephone.util.UiTaskRunner;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class CatalogPanel extends JPanel {
    private static final int PAGE_SIZE = 9;

    private final ProductService productService;
    private final CartService cartService;
    private final ImageStorage imageStorage;
    private final AppSession session;
    private final NavigationController nav;

    private final JTextField keywordField = new JTextField(16);
    private final Map<String, JCheckBox> factoryChecks = new LinkedHashMap<>();
    private final Map<String, JCheckBox> targetChecks = new LinkedHashMap<>();
    private final Map<JCheckBox, ProductFilterCriteria.PriceRange> priceChecks = new LinkedHashMap<>();
    private final JComboBox<String> sortCombo = new JComboBox<>(new String[]{
            "Mới nhất", "Giá tăng dần", "Giá giảm dần", "Tên A → Z"
    });

    private final JPanel gridContainer = new JPanel(new BorderLayout());
    private final JLabel pageLabel = new JLabel("Trang 1 / 1");
    private final JButton firstBtn = UiTheme.navButton("« Đầu");
    private final JButton prevBtn = UiTheme.navButton("‹ Trước");
    private final JButton nextBtn = UiTheme.navButton("Sau ›");
    private final JButton lastBtn = UiTheme.navButton("Cuối »");

    private int currentPage = 1;
    private int totalPages = 1;

    public CatalogPanel(
            ProductService productService,
            CartService cartService,
            ImageStorage imageStorage,
            AppSession session,
            NavigationController nav
    ) {
        super(new BorderLayout(14, 14));
        this.productService = productService;
        this.cartService = cartService;
        this.imageStorage = imageStorage;
        this.session = session;
        this.nav = nav;
        setBackground(UiTheme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        buildUi();
    }

    private void buildUi() {
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBackground(UiTheme.CARD_BG);
        filterPanel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        filterPanel.add(groupLabel("Tìm theo tên máy"));
        keywordField.setMaximumSize(new Dimension(220, 30));
        keywordField.setAlignmentX(LEFT_ALIGNMENT);
        filterPanel.add(keywordField);
        filterPanel.add(Box.createVerticalStrut(10));

        filterPanel.add(groupLabel("Hãng sản xuất"));
        for (String f : ProductFormDialog.DEFAULT_FACTORIES) {
            JCheckBox cb = checkItem(f);
            factoryChecks.put(f, cb);
            filterPanel.add(cb);
        }
        filterPanel.add(Box.createVerticalStrut(10));

        filterPanel.add(groupLabel("Nhu cầu sử dụng"));
        for (String t : ProductFormDialog.DEFAULT_TARGETS) {
            JCheckBox cb = checkItem(t);
            targetChecks.put(t, cb);
            filterPanel.add(cb);
        }
        filterPanel.add(Box.createVerticalStrut(10));

        filterPanel.add(groupLabel("Mức giá"));
        addPriceCheck(filterPanel, "Dưới 10 triệu", null, new BigDecimal("10000000"));
        addPriceCheck(filterPanel, "Từ 10 - 15 triệu", new BigDecimal("10000000"), new BigDecimal("15000000"));
        addPriceCheck(filterPanel, "Từ 15 - 25 triệu", new BigDecimal("15000000"), new BigDecimal("25000000"));
        addPriceCheck(filterPanel, "Trên 25 triệu", new BigDecimal("25000000"), null);
        filterPanel.add(Box.createVerticalStrut(10));

        filterPanel.add(groupLabel("Sắp xếp theo"));
        sortCombo.setMaximumSize(new Dimension(220, 30));
        sortCombo.setAlignmentX(LEFT_ALIGNMENT);
        filterPanel.add(sortCombo);
        filterPanel.add(Box.createVerticalStrut(12));

        JButton applyBtn = UiTheme.primaryButton("Lọc sản phẩm");
        applyBtn.setAlignmentX(LEFT_ALIGNMENT);
        JButton clearBtn = UiTheme.navButton("Xóa bộ lọc");
        clearBtn.setAlignmentX(LEFT_ALIGNMENT);
        filterPanel.add(applyBtn);
        filterPanel.add(Box.createVerticalStrut(6));
        filterPanel.add(clearBtn);

        JScrollPane filterScroll = new JScrollPane(filterPanel);
        filterScroll.setPreferredSize(new Dimension(245, 0));
        filterScroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));

        gridContainer.setOpaque(false);

        JPanel pager = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        pager.setOpaque(false);
        pager.add(firstBtn);
        pager.add(prevBtn);
        pager.add(pageLabel);
        pager.add(nextBtn);
        pager.add(lastBtn);

        add(filterScroll, BorderLayout.WEST);
        add(gridContainer, BorderLayout.CENTER);
        add(pager, BorderLayout.SOUTH);

        applyBtn.addActionListener(e -> { currentPage = 1; reloadCatalog(); });
        keywordField.addActionListener(e -> { currentPage = 1; reloadCatalog(); });
        sortCombo.addActionListener(e -> { currentPage = 1; reloadCatalog(); });
        clearBtn.addActionListener(e -> resetFilters());

        firstBtn.addActionListener(e -> { currentPage = 1; reloadCatalog(); });
        prevBtn.addActionListener(e -> { if (currentPage > 1) { currentPage--; reloadCatalog(); } });
        nextBtn.addActionListener(e -> { if (currentPage < totalPages) { currentPage++; reloadCatalog(); } });
        lastBtn.addActionListener(e -> { currentPage = totalPages; reloadCatalog(); });
    }

    private void addPriceCheck(JPanel container, String label, BigDecimal min, BigDecimal max) {
        JCheckBox cb = checkItem(label);
        priceChecks.put(cb, new ProductFilterCriteria.PriceRange(min, max));
        container.add(cb);
    }

    private static JLabel groupLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UiTheme.FONT_SECTION);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JCheckBox checkItem(String text) {
        JCheckBox cb = new JCheckBox(text);
        cb.setOpaque(false);
        cb.setFont(UiTheme.FONT_BODY);
        cb.setAlignmentX(LEFT_ALIGNMENT);
        return cb;
    }

    public void reloadCatalog() {
        ProductFilterCriteria criteria = collectCriteria();
        UiTaskRunner.runAsync(
                () -> productService.searchPublicCatalog(criteria),
                this::renderPage,
                err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Lỗi tải danh mục", JOptionPane.ERROR_MESSAGE)
        );
    }

    private ProductFilterCriteria collectCriteria() {
        ProductFilterCriteria c = new ProductFilterCriteria();
        c.setKeyword(keywordField.getText());
        List<String> factories = new ArrayList<>();
        factoryChecks.forEach((k, cb) -> { if (cb.isSelected()) { factories.add(k); } });
        c.setFactories(factories);

        List<String> targets = new ArrayList<>();
        targetChecks.forEach((k, cb) -> { if (cb.isSelected()) { targets.add(k); } });
        c.setTargets(targets);

        List<ProductFilterCriteria.PriceRange> ranges = new ArrayList<>();
        priceChecks.forEach((cb, r) -> { if (cb.isSelected()) { ranges.add(r); } });
        c.setPriceRanges(ranges);

        c.setSortBy(switch (sortCombo.getSelectedIndex()) {
            case 1 -> ProductFilterCriteria.SortBy.PRICE_ASC;
            case 2 -> ProductFilterCriteria.SortBy.PRICE_DESC;
            case 3 -> ProductFilterCriteria.SortBy.NAME_ASC;
            default -> ProductFilterCriteria.SortBy.NEWEST;
        });
        c.setPage(currentPage);
        c.setPageSize(PAGE_SIZE);
        return c;
    }

    private void renderPage(PageResult<Product> page) {
        this.totalPages = page.getTotalPages();
        if (currentPage > totalPages) {
            currentPage = totalPages;
            reloadCatalog();
            return;
        }
        gridContainer.removeAll();
        if (page.getItems().isEmpty()) {
            JLabel empty = new JLabel("Không tìm thấy điện thoại nào phù hợp với bộ lọc hiện tại.", JLabel.CENTER);
            empty.setFont(UiTheme.FONT_TITLE);
            empty.setForeground(UiTheme.TEXT_MUTED);
            gridContainer.add(empty, BorderLayout.CENTER);
        } else {
            JPanel grid = new JPanel(new GridLayout(0, 3, 14, 14));
            grid.setOpaque(false);
            for (Product p : page.getItems()) {
                grid.add(buildProductCard(p));
            }
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setOpaque(false);
            wrapper.add(grid, BorderLayout.NORTH);
            JScrollPane sp = new JScrollPane(wrapper);
            sp.setBorder(null);
            gridContainer.add(sp, BorderLayout.CENTER);
        }

        pageLabel.setText("Trang " + page.getPage() + " / " + totalPages + " (" + page.getTotalElements() + " máy)");
        firstBtn.setEnabled(page.getPage() > 1);
        prevBtn.setEnabled(page.getPage() > 1);
        nextBtn.setEnabled(page.getPage() < totalPages);
        lastBtn.setEnabled(page.getPage() < totalPages);
        gridContainer.revalidate();
        gridContainer.repaint();
    }

    private JPanel buildProductCard(Product p) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(UiTheme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel img = new JLabel(imageStorage.loadScaledIcon(p.getImagePath(), 130, 130, p.getFactory()), JLabel.CENTER);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        JLabel name = new JLabel("<html><b>" + p.getName() + "</b></html>");
        name.setFont(UiTheme.FONT_BODY);
        JLabel meta = new JLabel(p.getFactory() + " • Kho: " + p.getQuantity());
        meta.setFont(UiTheme.FONT_SECTION);
        meta.setForeground(UiTheme.TEXT_MUTED);
        JLabel price = new JLabel(MoneyFormatter.formatVnd(p.getPrice()));
        price.setFont(UiTheme.FONT_BRAND);
        price.setForeground(UiTheme.PRIMARY);

        body.add(name);
        body.add(Box.createVerticalStrut(4));
        body.add(meta);
        body.add(Box.createVerticalStrut(4));
        body.add(price);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btns.setOpaque(false);
        JButton detailBtn = UiTheme.navButton("Chi tiết");
        JButton buyBtn = UiTheme.primaryButton(p.getQuantity() > 0 ? "+ Giỏ hàng" : "Hết hàng");
        buyBtn.setEnabled(p.getQuantity() > 0);

        detailBtn.addActionListener(e -> new ProductDetailDialog(
                SwingUtilities.getWindowAncestor(this), p, imageStorage, session, cartService, nav
        ).setVisible(true));
        buyBtn.addActionListener(e -> handleQuickAdd(p));

        btns.add(detailBtn);
        btns.add(buyBtn);

        card.add(img, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        card.add(btns, BorderLayout.SOUTH);
        return card;
    }

    private void handleQuickAdd(Product p) {
        if (!session.isLoggedIn()) {
            JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập để thêm điện thoại vào giỏ hàng.",
                    "Yêu cầu đăng nhập", JOptionPane.INFORMATION_MESSAGE);
            nav.navigateTo(NavigationRoutes.LOGIN);
            return;
        }
        UiTaskRunner.runAsync(
                () -> cartService.addProductToCart(p.getId(), 1),
                s -> JOptionPane.showMessageDialog(this, "Đã thêm '" + p.getName() + "' vào giỏ hàng!",
                        "Giỏ hàng", JOptionPane.INFORMATION_MESSAGE),
                err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Không thể thêm", JOptionPane.WARNING_MESSAGE)
        );
    }

    private void resetFilters() {
        keywordField.setText("");
        factoryChecks.values().forEach(cb -> cb.setSelected(false));
        targetChecks.values().forEach(cb -> cb.setSelected(false));
        priceChecks.keySet().forEach(cb -> cb.setSelected(false));
        sortCombo.setSelectedIndex(0);
        currentPage = 1;
        reloadCatalog();
    }
}



