package com.breedish.etalonstyle.ui;

import javafx.scene.Node;

/**
 * Class AbstractController.
 *
 * @author zenind
 */
public abstract class AbstractController implements Controller {

    private Node view;

    @Override
    public void setView(Node node) {
        this.view = node;
    }

    @Override
    public Node getView() {
        return this.view;
    }
}
