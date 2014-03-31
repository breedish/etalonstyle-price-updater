package com.breedish.etalonstyle.process;

import javafx.concurrent.Task;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class PriceHandler.
 *
 * @author zenind
 */
@Service
public class PriceHandler implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(PriceHandler.class);

    @Value("/price-sheets.csv")
    private String config;

    @Autowired
    private MessageSource messageSource;

    private ExecutorService service = Executors.newFixedThreadPool(1);

    @Override
    public void afterPropertiesSet() throws Exception {
        Class.forName("org.firebirdsql.jdbc.FBDriver").newInstance();
    }

    public void process(final File priceFile, final File dbFile, final ProgressListener listener) {

        Task<Integer> task = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                try {
                    final Workbook book = WorkbookFactory.create(priceFile);
                    final Connection dbConnection = connect(dbFile);
                    dbConnection.setAutoCommit(false);

                    PreparedStatement preparedStatement = dbConnection.prepareStatement("UPDATE TARTSVST SET CLPRC=? WHERE ASKL1=?");

                    List<String> configs = getSheetConfig();

                    int processed = 0;
                    int totalUpdated = 0;
                    for (String sheetConfig : configs) {
                        String[] item = sheetConfig.split(",");

                        updateProgress(++processed, configs.size());
                        updateMessage(messageSource.getMessage("ui.processing", new String[]{item[0]}, Locale.getDefault()));

                        Sheet sheet = book.getSheet(item[0]);
                        if (sheet == null) {
                            LOG.error("Unable to open 'item[0]' sheet in excel");
                            continue;
                        }
                        for (int i = 0; i < sheet.getLastRowNum() + 10; i++) {
                            Row row = sheet.getRow(i);
                            if (row == null) {
                                continue;
                            }
                            Cell productIdCell = row.getCell(Integer.valueOf(item[3]));
                            if (productIdCell == null || productIdCell.getCellType() != Cell.CELL_TYPE_NUMERIC) {
                                continue;
                            }

                            long productId = (long) productIdCell.getNumericCellValue();
                            double price = row.getCell(Integer.valueOf(item[1])).getNumericCellValue();
                            double percentage = row.getCell(Integer.valueOf(item[2])).getNumericCellValue();

                            preparedStatement.setDouble(1, price*1.2/(1+percentage));
                            preparedStatement.setString(2, String.valueOf(productId));
                            preparedStatement.addBatch();

                            LOG.info("{}: {} -> {} {}", new Number[]{i, productId, price, percentage});
                        }
                        int[] updated = preparedStatement.executeBatch();
                        for (int e : updated) totalUpdated += e;
                        dbConnection.commit();
                    }

                    updateMessage(messageSource.getMessage("ui.process.finished", new String[] {String.valueOf(totalUpdated)}, Locale.getDefault()));
                } catch (Exception e) {
                    LOG.error("Issue", e);
                    updateMessage(messageSource.getMessage("ui.error", new String[]{e.getMessage()}, Locale.getDefault()));
                    throw e;
                }

                return 1;
            }
        };

        listener.getProgressBar().progressProperty().bind(task.progressProperty());
        listener.getUpdateComponent().textProperty().bind(task.messageProperty());

        service.submit(task);
    }

    private List<String> getSheetConfig() {
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

    private Connection connect(File dbFile) throws Exception {
        return DriverManager.getConnection(String.format("jdbc:firebirdsql:localhost:%s", dbFile.getAbsolutePath()), "SYSDBA", "masterkey");
    }
}
