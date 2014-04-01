package com.breedish.etalonstyle.ui;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Class ScreensManager.
 *
 * @author zenind
 */
@Component
public class ScreensConfiguration {

    @Value("/styles/styles.css")
    private String styleSheet;

    @Value("${version}")
    private String appVersion;

    public String getStyleSheet() {
        return styleSheet;
    }

    public String getAppVersion() {
        return appVersion;
    }
}
