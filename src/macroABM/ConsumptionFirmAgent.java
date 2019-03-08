package macroABM;

import java.math.BigDecimal;
import org.joda.money.Money;

public class ConsumptionFirmAgent extends EconomicAgent {

  BigDecimal rateOfCapacityUtilization;

  public ConsumptionFirmAgent() {}

  public ConsumptionFirmAgent(double givenCash) {
    super(givenCash);
  }

  public ConsumptionFirmAgent(double givenCash, int givenInventories) {
    this.ledger.put("cash", Money.of(usd, givenCash));
    this.ledger.put("inventories", givenInventories);
  }
}
