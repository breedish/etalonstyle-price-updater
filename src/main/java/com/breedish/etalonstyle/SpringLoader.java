package com.breedish.etalonstyle;

import com.breedish.etalonstyle.ui.Controller;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.InputStream;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Spring loader for JavaFX.
 *
 * @author zenind
 */
public final class SpringLoader {

    private static final Logger LOG = LoggerFactory.getLogger(SpringLoader.class);

    private SpringLoader() {
    }

    public static Controller load(final ApplicationContext context, final String fxmlPath) {
        try (InputStream fxmlStream = SpringLoader.class.getResourceAsStream(fxmlPath)) {
            FXMLLoader loader = new FXMLLoader();
            loader.setResources(ResourceBundle.getBundle("fxml.bundle", Locale.getDefault()));
            loader.setControllerFactory(new Callback<Class<?>, Object>() {
                @Override
                public Object call(Class<?> beanType) {
                    return context.getBean(beanType);
                }
            });

            Node view = (Node) loader.load(fxmlStream);
            Controller controller = loader.getController();
            controller.setView(view);
            return controller;
        } catch (Exception e) {
            LOG.error("Issue loading {} form", fxmlPath);
            throw new RuntimeException(e);
        }
    }


}
