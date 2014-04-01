package com.breedish.etalonstyle.process;

import javafx.scene.control.ProgressBar;
import javafx.scene.text.Text;

/**
 * Class ProgressListener.
 *
 * @author zenind
 */
public class ProgressListener {

    private Text updateComponent;

    private ProgressBar progressBar;

    public ProgressListener(Text updateComponent, ProgressBar progressBar) {
        this.updateComponent = updateComponent;
        this.progressBar = progressBar;
    }

    public Text getUpdateComponent() {
        return updateComponent;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }
}
