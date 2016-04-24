package com.breedish.etalonstyle.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Integer.valueOf;
import static java.util.Collections.emptyMap;

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
        priceTypes = new BufferedReader(new InputStreamReader(UpdateOptions.class.getResourceAsStream(config))).lines()
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
