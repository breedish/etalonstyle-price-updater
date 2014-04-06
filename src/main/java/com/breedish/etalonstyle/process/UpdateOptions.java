package com.breedish.etalonstyle.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Class UpdateOptions.
 *
 * @author zenind
 */
@Component
public class UpdateOptions implements InitializingBean {

    /** Logger.*/
    private static final Logger LOG = LoggerFactory.getLogger(UpdateOptions.class);

    @Value("/price-sheets.csv")
    private String config;

    @Value("${vat}")
    private Double newVat;

    private List<PriceType> priceTypes;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.priceTypes = new LinkedList<>();
        for (String priceType : loadPriceTypes()) {
            String values[] = priceType.split(",");
            priceTypes.add(new PriceType(values[0], Integer.valueOf(values[1]), Integer.valueOf(values[2]), Integer.valueOf(values[3])));
        }
    }

    private List<String> loadPriceTypes() {
        List<String> configs = new ArrayList<>();

        try (final BufferedReader configStream = new BufferedReader(new InputStreamReader(PriceHandler.class.getResourceAsStream(config)))) {
            String configLine;
            while ((configLine = configStream.readLine()) != null) {
                configs.add(configLine);
            }
        } catch (IOException e) {
            LOG.error("Error while opening sheets-config file", e);
        }
        return configs;
    }

    public static class PriceType {

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
    }

    public Double getNewVat() {
        return newVat;
    }

    public List<PriceType> getPriceTypes() {
        return this.priceTypes;
    }
}
