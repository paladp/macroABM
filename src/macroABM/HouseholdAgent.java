/*
 * Class for representing household agents
 */
package macroABM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import org.joda.money.Money;
import repast.simphony.engine.schedule.ScheduledMethod;

public class HouseholdAgent extends EconomicAgent {

  protected static double savingParameter = 0.9;

  int consumptionPerMonth;
  int consumptionPerDay;
  int consumedToday;
  Money reservationWage;

  // Default constructor from super EconomicAgent.java
  public HouseholdAgent() {
    // Set starting cash equal to two months worth of wages for last month
    this.ledger.put("Cash", Money.of(usd, 2240.00d));
    this.consumptionPerDay = 0;
    this.consumedToday = 0;
    this.consumptionPerMonth = 0;
    this.reservationWage = Money.of(usd, 1120.00d);
    System.out.println("Reservation wage is: " + getReservationWage());
  }

  public int getConsumedToday() {
    return this.consumedToday;
  }

  public int getConsumptionPerDay() {
    return this.consumptionPerDay;
  }

  public Money getReservationWage() {
    return this.reservationWage;
  }

  public void updateReservatonWage() {

    // Check if we are employed:
    if (laborNetwork.getDegree(this) == 1) {
      Iterator<Object> employerIterator = this.laborNetwork.getAdjacent(this).iterator();
      ConsumptionFirmAgent employer = (ConsumptionFirmAgent) employerIterator.next();
      if (this.reservationWage.isLessThan(employer.getOfferedWage())) {
        this.reservationWage = employer.getOfferedWage();
      }
    } else {
      this.reservationWage =
          this.reservationWage.multipliedBy(new BigDecimal(0.90), RoundingMode.HALF_DOWN);
    }
  }

  @ScheduledMethod(start = 1, interval = 5, priority = 93)
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

    System.out.println(
        "Average Price of Trading Partners for " + this + " is: " + averagePrice.toString());

    int cashDividedByAveragePrice =
        ((Money) this.ledger.get("Cash"))
            .dividedBy(averagePrice.getAmount(), RoundingMode.HALF_DOWN)
            .getAmountMajorInt();

    System.out.println(
        this
            + " had "
            + this.ledger.get("Cash").toString()
            + " in cash, and avg/cash is: "
            + cashDividedByAveragePrice);

    int consumptionAdjustedForSaving =
        (int) (Math.pow((double) cashDividedByAveragePrice, savingParameter));

    System.out.println(
        this + " has adjusted consumption and it is now " + consumptionAdjustedForSaving);

    this.consumptionPerMonth = Math.min(cashDividedByAveragePrice, consumptionAdjustedForSaving);

    this.consumptionPerDay = (int) (this.consumptionPerMonth / 21);
    System.out.println(
        this + " will now plan on consuming " + this.consumptionPerDay + " goods per day");
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
        this.consumedToday = this.consumedToday + (givenTransaction.getAmountBought().intValue());
        Money newBalance =
            ((Money) this.ledger.get("Cash")).minus(givenTransaction.getAmountMoney());
        this.ledger.put("Cash", newBalance);
      }
    }
  }
}
