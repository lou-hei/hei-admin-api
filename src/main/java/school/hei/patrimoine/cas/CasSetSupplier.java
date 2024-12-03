package school.hei.patrimoine.cas;

import java.util.function.Supplier;

public class CasSetSupplier implements Supplier<CasSet> {
  @Override
  public CasSet get() {
    throw new RuntimeException("Implement it");
  }
}
