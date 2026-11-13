package com.storephone.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductFilterCriteria {
    public enum SortBy {
        NEWEST("id DESC"),
        PRICE_ASC("price ASC, id DESC"),
        PRICE_DESC("price DESC, id DESC"),
        NAME_ASC("name ASC, id DESC");

        private final String orderByClause;

        SortBy(String orderByClause) {
            this.orderByClause = orderByClause;
        }

        public String getOrderByClause() {
            return orderByClause;
        }
    }

    public record PriceRange(BigDecimal min, BigDecimal max) {
    }

    private String keyword;
    private List<String> factories = new ArrayList<>();
    private List<String> targets = new ArrayList<>();
    private List<PriceRange> priceRanges = new ArrayList<>();
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private boolean includeInactive = false;
    private SortBy sortBy = SortBy.NEWEST;
    private int page = 1;
    private int pageSize = 10;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<String> getFactories() {
        return factories;
    }

    public void setFactories(List<String> factories) {
        this.factories = factories == null ? new ArrayList<>() : new ArrayList<>(factories);
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets == null ? new ArrayList<>() : new ArrayList<>(targets);
    }

    public List<PriceRange> getPriceRanges() {
        return priceRanges;
    }

    public void setPriceRanges(List<PriceRange> priceRanges) {
        this.priceRanges = priceRanges == null ? new ArrayList<>() : new ArrayList<>(priceRanges);
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public boolean isIncludeInactive() {
        return includeInactive;
    }

    public void setIncludeInactive(boolean includeInactive) {
        this.includeInactive = includeInactive;
    }

    public SortBy getSortBy() {
        return sortBy == null ? SortBy.NEWEST : sortBy;
    }

    public void setSortBy(SortBy sortBy) {
        this.sortBy = sortBy == null ? SortBy.NEWEST : sortBy;
    }

    public int getPage() {
        return Math.max(1, page);
    }

    public void setPage(int page) {
        this.page = Math.max(1, page);
    }

    public int getPageSize() {
        return Math.max(1, pageSize);
    }

    public void setPageSize(int pageSize) {
        this.pageSize = Math.max(1, pageSize);
    }

    public int getOffset() {
        return (getPage() - 1) * getPageSize();
    }
}
