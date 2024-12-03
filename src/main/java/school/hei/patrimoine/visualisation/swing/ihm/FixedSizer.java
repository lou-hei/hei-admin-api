package school.hei.patrimoine.visualisation.swing.ihm;

import java.awt.*;
import java.util.function.BiConsumer;

public class FixedSizer implements BiConsumer<Component, Dimension> {
  @Override
  public void accept(Component component, Dimension dimension) {
    component.setSize(dimension);
    component.setMinimumSize(dimension);
    component.setMaximumSize(dimension);
    component.setPreferredSize(dimension);
  }
}
