package com.breedish.etalonstyle;

import com.breedish.etalonstyle.ui.Controller;
import com.breedish.etalonstyle.ui.ScreensConfiguration;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Java FX application.
 */
public class PriceUpdateApp extends Application {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PriceUpdateApp.class);
    /** Spring application context.*/
    private static final ApplicationContext APP_CONTEXT = new AnnotationConfigApplicationContext(ApplicationConfiguration.class);

    public void start(Stage stage) throws Exception {
        Controller mainController = SpringLoader.load(APP_CONTEXT, "/fxml/mainForm.fxml");

        ScreensConfiguration screensConfiguration = APP_CONTEXT.getBean(ScreensConfiguration.class);

        Scene scene = new Scene((Parent) mainController.getView(), 710, 240);
        scene.getStylesheets().add(screensConfiguration.getStyleSheet());
        stage.setTitle(String.format("Price Updater v%s", screensConfiguration.getAppVersion()));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.getIcons().add(new Image(PriceUpdateApp.class.getResourceAsStream("/images/icon.png")));
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                Platform.exit();
                System.exit(0);
            }
        });

        LOG.info("Main form is initialized");
        stage.show();
    }

    /**
     * Application entry point.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
