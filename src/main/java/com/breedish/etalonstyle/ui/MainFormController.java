package com.breedish.etalonstyle.ui;

import com.breedish.etalonstyle.process.PriceType;
import com.breedish.etalonstyle.process.PriceUpdaterService;
import com.breedish.etalonstyle.process.UpdateOptions;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.springframework.util.StringUtils.isEmpty;

/**
 * Price Updater main form controller.
 */
@Component
public class MainFormController {

    private static final Logger LOG = LoggerFactory.getLogger(MainFormController.class);

    @FXML
    private TextField priceFile;

    @FXML
    private Button priceButton;

    @FXML
    private TextField dbFile;

    @FXML
    private Button dbButton;

    @FXML
    private ProgressBar updateProgress;

    @FXML
    private Text progressDetails;

    @FXML
    private FlowPane optionsPane;

    @Autowired
    private PriceUpdaterService processUpdaterService;

    @Autowired
    private UpdateOptions updateOptions;

    @Autowired
    private MessageSource messageSource;

    public void initialize() {
        bindOnChoose(priceButton, priceFile, Arrays.asList("*.xls", "*.xlsx"));
        bindOnChoose(dbButton, dbFile, Arrays.asList("*.gdb"));
        addAllowedPriceTypes(updateOptions);
    }

    private void addAllowedPriceTypes(UpdateOptions options) {
        for (final PriceType priceType : options.getPriceTypes()) {
            CheckBox checkBox = new CheckBox(priceType.getName());
            checkBox.setSelected(true);
            checkBox.setMinWidth(100);
            checkBox.selectedProperty().addListener((value, from, to)-> priceType.setUpdate(to));
            optionsPane.getChildren().addAll(checkBox);
        }
    }

    private void bindOnChoose(final Control control, final TextField textField, final List<String> filter) {
        control.setOnMouseClicked((event) -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter(getMessage("ui.chooser.title"), filter));
            File selectedFile = chooser.showOpenDialog(null);
            if (selectedFile != null) {
                LOG.info("File '{}' was selected", selectedFile.getAbsoluteFile());
                textField.setText(selectedFile.getAbsolutePath());
            }
        });
        updateProgress.setDisable(true);
        updateProgress.setProgress(0);
    }

    private String getMessage(String messageId) {
        return messageSource.getMessage(messageId, null, Locale.getDefault());
    }

    public void update() {
        if (isEmpty(priceFile.getText()) || isEmpty(dbFile.getText())) {
            progressDetails.setText(getMessage("error.choose.files"));
            return;
        }

        LOG.info("Update Price from {} file to DB {}", dbFile.getText(), priceFile.getText());

        updateProgress.setDisable(false);
        processUpdaterService.process(
            updateOptions,
            new File(priceFile.getText()),
            new File(dbFile.getText()),
            progressDetails,
            updateProgress
        );
    }

}
