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
  int amountOfLaborConnections;
  int amountOfConsumptionConnections;

  // Default constructor from super EconomicAgent.java
  public HouseholdAgent() {
    // Set starting cash equal to two months worth of wages for last month
    // was 22400
    this.ledger.put("Cash", Money.of(usd, 2240.00d));
    this.consumptionPerDay = 0;
    this.consumedToday = 0;
    this.consumptionPerMonth = 0;
    this.reservationWage = Money.of(usd, 1120.00d);
  }

  public BigDecimal getUnsatisfiedDemandAsPercent() {
    if (consumptionPerDay > 0) {
      BigDecimal unsatisfiedDemand = new BigDecimal(consumptionPerDay - consumedToday);
      unsatisfiedDemand.setScale(2);
      BigDecimal consumptionPerDayDecimal = new BigDecimal(consumptionPerDay);
      consumptionPerDayDecimal.setScale(2);
      BigDecimal unsatisfiedAsPercent =
          unsatisfiedDemand.divide(consumptionPerDayDecimal, 2, RoundingMode.HALF_DOWN);
      return unsatisfiedAsPercent;
    } else {
      return new BigDecimal(0);
    }
  }

  public void setAmountOfLaborConnections(int amount) {}

  public void setAmountOfConsumptionConnections(int amount) {}

  public int getConsumedToday() {
    return this.consumedToday;
  }

  public int getConsumptionPerDay() {
    return this.consumptionPerDay;
  }

  public Money getReservationWage() {
    return this.reservationWage;
  }

  @ScheduledMethod(start = 21, interval = 21, priority = 88)
  public void updateReservatonWage() {

    // Check if we are employed:
    if (laborNetwork.getDegree(this) == 1) {
      Iterator<Object> employerIterator = this.laborNetwork.getAdjacent(this).iterator();
      ConsumptionFirmAgent employer = (ConsumptionFirmAgent) employerIterator.next();
      if (this.reservationWage.isLessThan(employer.getOfferedWage())) {
        this.reservationWage = employer.getOfferedWage();
        System.out.println(
            this + " has set their reservation wage to " + this.reservationWage.toString());
      }
    } else {
      this.reservationWage =
          this.reservationWage.multipliedBy(new BigDecimal(0.90), RoundingMode.HALF_DOWN);
      System.out.println(
          this + " has lowered the reservation wage to: " + this.reservationWage.toString());
    }
  }

  @ScheduledMethod(start = 1, interval = 21, priority = 93)
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
    if (this.consumptionPerDay < 0) {
      System.exit(0);
    }
  }

  public void resetDailyConsumption() {
    this.consumedToday = 0;
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
