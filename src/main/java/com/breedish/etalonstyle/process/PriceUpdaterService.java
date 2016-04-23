package com.breedish.etalonstyle.process;

import javafx.concurrent.Task;
import javafx.scene.control.ProgressBar;
import javafx.scene.text.Text;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for {@link com.breedish.etalonstyle.PriceUpdateApp}.
 */
@Service
public class PriceUpdaterService {

    private static final Logger LOG = LoggerFactory.getLogger(PriceUpdaterService.class);

    @Autowired
    private MessageSource messageSource;

    private ExecutorService service = Executors.newFixedThreadPool(1);

    @PostConstruct
    public void init() throws Exception {
        Class.forName("org.firebirdsql.jdbc.FBDriver").newInstance();
    }

    public void process(final UpdateOptions options, final File priceFile, final File dbFile,
                        final Text updateComponent, final ProgressBar progressBar) {

        Task<Integer> task = new Task<Integer>() {
            AtomicInteger processed = new AtomicInteger(0);
            AtomicInteger totalUpdated = new AtomicInteger(0);

            @Override
            protected Integer call() throws Exception {
                try (final Connection dbConnection = connect(dbFile)) {
                    final Workbook book = WorkbookFactory.create(priceFile);
                    dbConnection.setAutoCommit(false);

                    PreparedStatement updatePriceStatement = dbConnection.prepareStatement("UPDATE TARTSVST SET CLPRC=?, CLPRV=? WHERE ASKL1=?");
                    PreparedStatement getPriceStatement = dbConnection.prepareStatement(
                        "SELECT TARTSVST.CLPRC,TARTIKLS.AMASS, TARTSVST.ASKL1 FROM TARTSVST LEFT OUTER JOIN TARTIKLS ON TARTIKLS.ANUMB=TARTSVST.ANUMB WHERE TARTSVST.ASKL1=?");

                    for (PriceType priceType : options.getPriceTypes()) {
                        processPrice(priceType, book, updatePriceStatement, getPriceStatement, dbConnection);
                    }

                    updateMessage(messageSource.getMessage("ui.process.finished", new String[]{String.valueOf(totalUpdated)}, Locale.getDefault()));
                } catch (Exception e) {
                    LOG.error("Issue", e);
                    updateMessage(messageSource.getMessage("ui.error", new String[]{e.getMessage().substring(0, 50)}, Locale.getDefault()));
                    throw e;
                }

                return 1;
            }

            private void processPrice(PriceType priceType, Workbook book, PreparedStatement updatePriceStatement,
                PreparedStatement getPriceStatement, Connection dbConnection) throws Exception {
                updateProgress(processed.incrementAndGet(), options.getPriceTypes().size());
                if (!priceType.isUpdate()) {
                    LOG.info("Price {} is skipped", priceType.getName());
                    return;
                }

                updateMessage(messageSource.getMessage("ui.processing", new String[]{priceType.getName()}, Locale.getDefault()));

                Sheet sheet = book.getSheet(priceType.getName());
                if (sheet == null) {
                    LOG.error("Unable to open '{}' sheet in excel", priceType.getName());
                    return;
                }

                LOG.info("Processing '{}' data set", priceType.getName());
                for (int i = 0; i < sheet.getLastRowNum() + 10; i++) {
                    Row row = sheet.getRow(i);
                    try {
                        if (row == null) {
                            continue;
                        }
                        Cell productIdCell = row.getCell(priceType.getCodeColumn());
                        if (productIdCell == null || productIdCell.getCellType() != Cell.CELL_TYPE_NUMERIC) {
                            continue;
                        }

                        long productId = (long) productIdCell.getNumericCellValue();
                        OptionalDouble priceValue = getCellNumericValue(row, priceType.getPriceColumn());
                        OptionalDouble oldVat = getCellNumericValue(row, priceType.getVatColumn());

                        if (!priceValue.isPresent() || !oldVat.isPresent()) {
                            continue;
                        }

                        Pair<Double, Double> oldPrice = getOldPrice(getPriceStatement, productId);
                        if (oldPrice == null) {
                            continue;
                        }
                        double newPrice = scalePrice(calculatePrice(priceValue.getAsDouble(), oldVat.getAsDouble(), options.getNewVat()));
                        double newMassPrice = scalePrice(calculateMassPrice(newPrice, oldPrice.getValue1()));
                        updatePriceStatement.setDouble(1, newPrice);
                        updatePriceStatement.setDouble(2, newMassPrice);
                        updatePriceStatement.setString(3, String.valueOf(productId));
                        updatePriceStatement.addBatch();

                        LOG.info("{}: {} - {} {} -> {} {}", new Number[]{i, productId, priceValue.getAsDouble(), oldVat.getAsDouble(), newPrice, newMassPrice});
                    } catch (Exception e) {
                        LOG.error("Error processing {} at row {}. Reason: {}", new Object[] {priceType.getName(), i, e});
                    }
                }
                int[] updated = updatePriceStatement.executeBatch();
                for (int e : updated) {
                    totalUpdated.addAndGet(e);
                }
                dbConnection.commit();
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        updateComponent.textProperty().bind(task.messageProperty());

        service.submit(task);
    }

    private OptionalDouble getCellNumericValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        int cellType = cell.getCellType();
        if (cellType == Cell.CELL_TYPE_ERROR || cellType == Cell.CELL_TYPE_FORMULA || cellType == Cell.CELL_TYPE_BLANK || cellType == Cell.CELL_TYPE_BOOLEAN) {
            return OptionalDouble.empty();
        } else {
            return OptionalDouble.of(cell.getNumericCellValue());
        }
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
