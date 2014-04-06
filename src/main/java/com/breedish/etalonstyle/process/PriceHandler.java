package com.breedish.etalonstyle.process;

import javafx.concurrent.Task;
import org.apache.poi.ss.usermodel.*;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    @Autowired
    private MessageSource messageSource;

    private ExecutorService service = Executors.newFixedThreadPool(1);

    @Override
    public void afterPropertiesSet() throws Exception {
        Class.forName("org.firebirdsql.jdbc.FBDriver").newInstance();
    }

    public void process(final UpdateOptions options, final File priceFile, final File dbFile, final ProgressListener listener) {

        Task<Integer> task = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                try(final Connection dbConnection = connect(dbFile)) {
                    final Workbook book = WorkbookFactory.create(priceFile);
                    dbConnection.setAutoCommit(false);

                    PreparedStatement updatePriceStatement = dbConnection.prepareStatement("UPDATE TARTSVST SET CLPRC=?, CLPRV=? WHERE ASKL1=?");
                    PreparedStatement getPriceStatement = dbConnection.prepareStatement(
                        "SELECT TARTSVST.CLPRC,TARTIKLS.AMASS, TARTSVST.ASKL1 FROM TARTSVST LEFT OUTER JOIN TARTIKLS ON TARTIKLS.ANUMB=TARTSVST.ANUMB WHERE TARTSVST.ASKL1=?");

                    int processed = 0;
                    int totalUpdated = 0;
                    for (UpdateOptions.PriceType priceType : options.getPriceTypes()) {

                        updateProgress(++processed, options.getPriceTypes().size());
                        if (!priceType.isUpdate()) {
                            LOG.info("Price {} is skipped", priceType.getName());
                            continue;
                        }

                        updateMessage(messageSource.getMessage("ui.processing", new String[]{priceType.getName()}, Locale.getDefault()));

                        Sheet sheet = book.getSheet(priceType.getName());
                        if (sheet == null) {
                            LOG.error("Unable to open '{}' sheet in excel", priceType.getName());
                            continue;
                        }

                        LOG.info("Processing '{}' data set", priceType.getName());
                        for (int i = 0; i < sheet.getLastRowNum() + 10; i++) {
                            Row row = sheet.getRow(i);
                            if (row == null) {
                                continue;
                            }
                            Cell productIdCell = row.getCell(priceType.getCodeColumn());
                            if (productIdCell == null || productIdCell.getCellType() != Cell.CELL_TYPE_NUMERIC) {
                                continue;
                            }

                            long productId = (long) productIdCell.getNumericCellValue();
                            double priceValue = row.getCell(priceType.getPriceColumn()).getNumericCellValue();
                            double oldVat = row.getCell(priceType.getVatColumn()).getNumericCellValue();

                            Pair<Double, Double> oldPrice = getOldPrice(getPriceStatement, productId);
                            if (oldPrice == null) {
                                continue;
                            }
                            double newPrice = scalePrice(calculatePrice(priceValue, oldVat, options.getNewVat()));
                            double newMassPrice = scalePrice(calculateMassPrice(newPrice, oldPrice.getValue1()));
                            updatePriceStatement.setDouble(1, newPrice);
                            updatePriceStatement.setDouble(2, newMassPrice);
                            updatePriceStatement.setString(3, String.valueOf(productId));
                            updatePriceStatement.addBatch();

                            LOG.info("{}: {} - {} {} -> {} {}", new Number[]{i, productId, priceValue, oldVat, newPrice, newMassPrice});
                        }
                        int[] updated = updatePriceStatement.executeBatch();
                        for (int e : updated) totalUpdated += e;
                        dbConnection.commit();
                    }

                    updateMessage(messageSource.getMessage("ui.process.finished", new String[] {String.valueOf(totalUpdated)}, Locale.getDefault()));
                } catch (Exception e) {
                    LOG.error("Issue", e);
                    updateMessage(messageSource.getMessage("ui.error", new String[]{e.getMessage().substring(0, 50)}, Locale.getDefault()));
                    throw e;
                }

                return 1;
            }
        };

        listener.getProgressBar().progressProperty().bind(task.progressProperty());
        listener.getUpdateComponent().textProperty().bind(task.messageProperty());

        service.submit(task);
    }

    private double scalePrice(BigDecimal decimal) {
        return decimal.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal calculatePrice(double newPrice, double oldVat, double newVat) {
        return BigDecimal.valueOf(newPrice).multiply(BigDecimal.valueOf(1 + newVat)).divide(BigDecimal.valueOf(1 + oldVat), RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMassPrice(double newPrice, double aMass) {
        if (aMass == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(newPrice).divide(BigDecimal.valueOf(aMass), BigDecimal.ROUND_HALF_UP);
    }

    private Pair<Double, Double> getOldPrice(PreparedStatement statement, long productId) throws Exception {
        statement.setString(1, String.valueOf(productId));
        ResultSet resultSet = statement.executeQuery();
        if (!resultSet.next()) {
            return null;
        }
        return new Pair<>(resultSet.getDouble(1), resultSet.getDouble(2));
    }

    private Connection connect(File dbFile) throws Exception {
        return DriverManager.getConnection(String.format("jdbc:firebirdsql:localhost:%s", dbFile.getAbsolutePath()), "SYSDBA", "masterkey");
    }
}
