package com.breedish.etalonstyle;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.InputStream;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Application entry point.
 */
public class PriceUpdateApp extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(PriceUpdateApp.class);

    public void start(Stage stage) throws Exception {
        final ApplicationContext context = new AnnotationConfigApplicationContext(ApplicationConfiguration.class);
        ApplicationConfiguration priceUpdaterConfig = context.getBean(ApplicationConfiguration.class);

        final Scene scene = new Scene(loadScene(context, "/fxml/mainForm.fxml"), 720, 360);
        scene.getStylesheets().add(priceUpdaterConfig.getStyleSheetLocation());
        stage.setTitle(String.format("Price Updater v%s", priceUpdaterConfig.getAppVersion()));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.getIcons().add(new Image(PriceUpdateApp.class.getResourceAsStream("/images/icon.png")));
        stage.setOnCloseRequest((event) -> {
            Platform.exit();
            System.exit(0);
        });
        LOG.info("Main form is initialized");
        stage.show();
    }

    public Parent loadScene(final ApplicationContext context, final String fxmlPath) {
        try (InputStream fxmlStream = PriceUpdateApp.class.getResourceAsStream(fxmlPath)) {
            FXMLLoader loader = new FXMLLoader();
            loader.setResources(ResourceBundle.getBundle("fxml.bundle", Locale.getDefault()));
            loader.setControllerFactory(context::getBean);
            return loader.load(fxmlStream);
        } catch (Exception e) {
            LOG.error("Issue loading {} form", fxmlPath);
            throw new RuntimeException(e);
        }
    }

    /**
     * Application entry point.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
