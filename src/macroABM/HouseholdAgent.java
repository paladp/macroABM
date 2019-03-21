/*
 * Class for representing household agents
 */
package macroABM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import org.joda.money.Money;

public class HouseholdAgent extends EconomicAgent {

  protected static double savingParameter = 0.9;

  int consumptionPerMonth;
  int consumptionPerDay;
  int consumedToday;
  Money reservationWage;

  // Default constructor from super EconomicAgent.java
  public HouseholdAgent() {
    // Set starting cash equal to wage for last month
    this.ledger.put("Cash", Money.of(usd, 1120.00d));
    consumptionPerDay = 0;
    consumedToday = 0;
    consumptionPerMonth = 0;
    Money reservationWage = Money.of(usd, 1120.00d);
  }

  public int getConsumedToday() {
    return this.consumedToday;
  }

  public int getConsumptionPerDay() {
    return this.consumptionPerMonth;
  }

  public void updateConsumption() {
    // Calculate average price of all trading partner goods
    ArrayList<ConsumptionFirmAgent> firmsTradingWith = new ArrayList<ConsumptionFirmAgent>();
    Iterable<Object> tradingPartners = consumptionNetwork.getAdjacent(this);
    for (Object currentFirm : tradingPartners) {
      firmsTradingWith.add((ConsumptionFirmAgent) currentFirm);
    }

    Money sumOfPrices = Money.of(usd, 0.00d);
    for (ConsumptionFirmAgent currentTradingPartner : firmsTradingWith) {
      sumOfPrices = sumOfPrices.plus(currentTradingPartner.getPrice());
    }

    BigDecimal numberOfTradingPartners = new BigDecimal(firmsTradingWith.size());
    Money averagePrice = sumOfPrices.dividedBy(numberOfTradingPartners, RoundingMode.HALF_UP);

    int cashDividedByAveragePrice =
        ((Money) this.ledger.get("Cash"))
            .dividedBy(averagePrice.getAmount(), RoundingMode.HALF_DOWN)
            .getAmountMajorInt();

    int consumptionAdjustedForSaving =
        (int) (Math.pow((double) cashDividedByAveragePrice, savingParameter));

    this.consumptionPerMonth = Math.min(cashDividedByAveragePrice, consumptionAdjustedForSaving);

    this.consumptionPerDay = (int) (this.consumptionPerMonth / 21);
  }

  public void handleTransaction(transaction givenTransaction) {
    if (givenTransaction.getReason().equals("wage")) {
      if (givenTransaction.getSeller() == this) {
        Money newBalance =
            ((Money) this.ledger.get("Cash")).plus(givenTransaction.getAmountMoney());
        this.ledger.put("Cash", newBalance);
      }
    }

    if (givenTransaction.getReason().equals("consumption")) {
      if (givenTransaction.getBuyer() == this) {
        int consumedToday = this.consumedToday + (givenTransaction.getAmountBought().intValue());
        Money newBalance =
            ((Money) this.ledger.get("Cash")).minus(givenTransaction.getAmountMoney());
        this.ledger.put("Cash", newBalance);
      }
    }
  }
}
