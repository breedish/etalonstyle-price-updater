package com.breedish.etalonstyle.process;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Integer.valueOf;

/**
 * Update options.
 */
@Component
public class UpdateOptions {

    @Value("/price-sheets.csv")
    private String config;

    @Value("${vat}")
    private Double newVat;

    private List<PriceType> priceTypes = new ArrayList<>();

    @PostConstruct
    public void init() throws Exception {
        priceTypes = Files.lines(Paths.get(PriceUpdaterService.class.getResource(config).toURI()), Charset.forName("windows-1251"))
            .map(line -> {
                String values[] = line.split(",");
                return new PriceType(values[0], valueOf(values[1]), valueOf(values[2]), valueOf(values[3]));
            })
            .collect(Collectors.toList());
    }

    public Double getNewVat() {
        return newVat;
    }

    public List<PriceType> getPriceTypes() {
        return this.priceTypes;
    }
}
