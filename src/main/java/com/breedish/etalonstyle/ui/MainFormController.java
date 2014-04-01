package com.breedish.etalonstyle.ui;

import com.breedish.etalonstyle.process.PriceHandler;
import com.breedish.etalonstyle.process.ProgressListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Controller form.
 */
@Component
public class MainFormController extends AbstractController {

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

    @Autowired
    private PriceHandler priceHandler;

    @Autowired
    private MessageSource messageSource;

    public void initialize() {
        bindOnChooseEvent(priceButton, priceFile, Arrays.asList("*.xls", "*.xlsx"));
        bindOnChooseEvent(dbButton, dbFile, Arrays.asList("*.gdb"));
    }

    private void bindOnChooseEvent(final Control control, final TextField textField, final List<String> filter) {
        control.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                FileChooser chooser = new FileChooser();
                chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter(messageSource.getMessage("ui.chooser.title", null, Locale.getDefault()), filter));
                File selectedFile = chooser.showOpenDialog(null);
                if (selectedFile != null) {
                    LOG.info("File '{}' was selected", selectedFile.getAbsoluteFile());
                    textField.setText(selectedFile.getAbsolutePath());
                }
            }
        });

        updateProgress.setDisable(true);
        updateProgress.setProgress(0);
    }

    public void update() {
        if (StringUtils.isEmpty(priceFile.getText()) || StringUtils.isEmpty(dbFile.getText())) {
            progressDetails.setText(messageSource.getMessage("error.choose.files", null, Locale.getDefault()));
            return;
        }

        LOG.info("Update Price {} from DB {}", dbFile.getText(), priceFile.getText());

        updateProgress.setDisable(false);
        priceHandler.process(new File(priceFile.getText()), new File(dbFile.getText()), new ProgressListener(progressDetails, updateProgress));
    }

}
