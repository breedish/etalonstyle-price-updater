package com.breedish.etalonstyle.process;

import java.util.Objects;

/**
 * Price Types.
 */
public final class PriceType {

    private final String name;

    private final int priceColumn;

    private final int vatColumn;

    private final int codeColumn;

    private boolean update;

    public PriceType(String name, int priceColumn, int vatColumn, int codeColumn) {
        this.name = name;
        this.priceColumn = priceColumn;
        this.vatColumn = vatColumn;
        this.codeColumn = codeColumn;
        this.update = true;
    }

    public String getName() {
        return name;
    }

    public int getPriceColumn() {
        return priceColumn;
    }

    public int getVatColumn() {
        return vatColumn;
    }

    public int getCodeColumn() {
        return codeColumn;
    }

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PriceType priceType = (PriceType) o;
        return Objects.equals(name, priceType.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
