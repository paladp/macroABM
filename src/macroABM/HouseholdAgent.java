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

  // This is the parameter used when calculating the amount of consumption for a household
  protected static double savingParameter = 0.9;

  // Variables used when determining the amount of consumption for this household. A monthly and
  // daily version are used, as well as a variable used to keep track of the consumption so far in a
  // given day
  protected int consumptionPerMonth;
  protected int consumptionPerDay;
  protected int consumedToday;

  // This household's reservation wage
  protected Money reservationWage;

  // The amount of employers, should be a maximum of one
  protected int amountOfLaborConnections;

  // The amount of trading partners, should be a maximum of seven
  protected int amountOfConsumptionConnections;

  // The amount this household is getting paid
  protected Money paidWage;

  // Default constructor from super EconomicAgent.java
  public HouseholdAgent() {
    this.ledger.put("Cash", Money.of(usd, 2240.00d));
    this.consumptionPerDay = 0;
    this.consumedToday = 0;
    this.consumptionPerMonth = 0;
    this.reservationWage = Money.of(usd, 1120.00d);
    this.paidWage = Money.of(usd, 0.00d);
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

  // Updates the firms reservation wage as the last step at the end of a month
  @ScheduledMethod(start = 21, interval = 21, priority = 88)
  public void updateReservatonWage() {

    // Check if the household is employed, if it is, then update the households reservation wage to
    // make it the amount they are being paid. If they are not employed, then reduce this
    // household's reservation wage by 10 percent
    if (laborNetwork.getDegree(this) == 1) {
      Iterator<Object> employerIterator = this.laborNetwork.getAdjacent(this).iterator();
      ConsumptionFirmAgent employer = (ConsumptionFirmAgent) employerIterator.next();
      this.paidWage = employer.offeredWage;
      if (this.reservationWage.isLessThan(employer.getOfferedWage())) {
        this.reservationWage = employer.getOfferedWage();
        //        System.out.println(
        //            this + " has set their reservation wage to " +
        // this.reservationWage.toString());
      }
    } else {
      this.paidWage = Money.of(usd, 0.00d);
      this.reservationWage =
          this.reservationWage.multipliedBy(new BigDecimal(0.90), RoundingMode.HALF_DOWN);
      //      System.out.println(
      //          this + " has lowered the reservation wage to: " +
      // this.reservationWage.toString());
    }
  }

  // Updates this household's consumption per month and day
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

    // This is the first measure of consumption, before savings are taken into account
    int cashDividedByAveragePrice =
        ((Money) this.ledger.get("Cash"))
            .dividedBy(averagePrice.getAmount(), RoundingMode.HALF_DOWN)
            .getAmountMajorInt();

    int consumptionAdjustedForSaving =
        (int) (Math.pow((double) cashDividedByAveragePrice, savingParameter));

    //    System.out.println(
    //        this + " has adjusted consumption and it is now " + consumptionAdjustedForSaving);

    this.consumptionPerMonth = Math.min(cashDividedByAveragePrice, consumptionAdjustedForSaving);
    this.consumptionPerDay = (int) (this.consumptionPerMonth / 21);
  }

  public void resetDailyConsumption() {
    this.consumedToday = 0;
  }

  // Handle a transaction object that is passed to this household
  public void handleTransaction(transaction givenTransaction) {

    // If we are being given a wage payment, then update the household's cash
    if (givenTransaction.getReason().equals("wage")) {
      if (givenTransaction.getSeller() == this) {
        Money newBalance =
            ((Money) this.ledger.get("Cash")).plus(givenTransaction.getAmountMoney());
        this.ledger.put("Cash", newBalance);
      }
    }

    // If we are being given a consumption payment, then reduce this household's cash and increase
    // the amount of goods consumed today
    if (givenTransaction.getReason().equals("consumption")) {
      if (givenTransaction.getBuyer() == this) {
        this.consumedToday = this.consumedToday + (givenTransaction.getAmountBought().intValue());
        Money newBalance =
            ((Money) this.ledger.get("Cash")).minus(givenTransaction.getAmountMoney());
        this.ledger.put("Cash", newBalance);
      }
    }
  }

  //////////////////////////// GETTER METHODS ///////////////////////////////////////////////

  public int getConsumedToday() {
    return this.consumedToday;
  }

  public int getConsumptionPerDay() {
    return this.consumptionPerDay;
  }

  public Money getReservationWage() {
    return this.reservationWage;
  }

  public BigDecimal getCashDisplay() {
    return ((Money) this.ledger.get("Cash")).getAmountMajor();
  }

  public BigDecimal getWageDisplay() {
    return ((Money) this.paidWage).getAmountMajor();
  }
}
