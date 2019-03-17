package macroABM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.joda.money.Money;
import repast.simphony.random.RandomHelper;

public class ConsumptionFirmAgent extends EconomicAgent {

  protected static BigDecimal profitBufferCoeff = new BigDecimal("0.10");
  protected static BigDecimal invFloorCoeff = new BigDecimal("0.25");
  protected static BigDecimal invCeilCoeff = new BigDecimal("1.00");
  protected static BigDecimal priceFloorCoeff = new BigDecimal("1.025");
  protected static BigDecimal priceCeilCoeff = new BigDecimal("1.15");
  protected static int techProductionCoeff = 3;
  protected static int wageDecreaseThreshold = 24;
  protected static double upperBoundUniformForWage = 0.019;
  protected static double upperBoundUniformForPrice = 0.02;

  protected Money offeredWage;
  protected Money priceOfGoodsSold;
  protected BigDecimal lastMonthSales;
  protected int priceCeiling;
  protected int priceFloor;
  protected BigDecimal inventoryCeiling;
  protected BigDecimal inventoryFloor;
  protected boolean wasPositionFilledLastPeriod;
  protected boolean wasPositionOffered;
  protected int monthsEveryPositionFilled;
  protected boolean firingWorker;
  protected boolean hiringWorker;

  public ConsumptionFirmAgent() {
    this.ledger.put("Cash", Money.of(usd, 0.0d));
    this.ledger.put("Inventories", Integer.parseInt("10"));
    // Works out to 7 dollars/hour at 40 hour weeks
    this.offeredWage = Money.of(usd, 1120.00d);
    this.priceOfGoodsSold = Money.of(usd, 5.80d);
  }

  public ConsumptionFirmAgent(double givenCash) {
    super(givenCash);
  }

  public ConsumptionFirmAgent(double givenCash, int givenInventories) {
    this.ledger.put("cash", Money.of(usd, givenCash));
    this.ledger.put("inventories", givenInventories);
  }

  protected void updateWage(
      boolean givenWasPositionFilledLastPeriod, int givenMonthsEveryPositionFilled) {

    RandomHelper.createUniform();
    if (givenWasPositionFilledLastPeriod == false) {
      offeredWage =
          offeredWage.multipliedBy(
              1.00 + RandomHelper.getUniform().nextDoubleFromTo(0.0d, upperBoundUniformForWage),
              RoundingMode.HALF_UP);
    } else if (givenMonthsEveryPositionFilled >= 24) {
      offeredWage =
          offeredWage.multipliedBy(
              1.00 - RandomHelper.getUniform().nextDoubleFromTo(0.0d, upperBoundUniformForWage),
              RoundingMode.HALF_UP);
    } else {
      System.out.println(this + " did not raise the wage this time step");
    }
  }

  protected void calcInvBounds(BigDecimal givenSales) {
    this.inventoryCeiling = this.invCeilCoeff.multiply(givenSales);
    this.inventoryFloor = this.invFloorCoeff.multiply(givenSales);
  }

  public void updateHiringDecision(BigDecimal cieling, BigDecimal floor) {
    if (((BigDecimal) this.ledger.get("Inventories")).compareTo(cieling) == 1) {
      this.hiringWorker = false;
      this.firingWorker = true;
    } else if (((BigDecimal) this.ledger.get("Inventories")).compareTo(floor) == -1) {
      this.hiringWorker = true;
      this.firingWorker = false;
    } else {
      this.hiringWorker = false;
      this.firingWorker = false;
    }
  }

  //////////////////////////// GETTER METHODS ///////////////////////////////////////////////
  public Money getOfferedWage() {
    return this.offeredWage;
  }

  //////////////////////////// PUBLIC METHODS FOR SIM////////////////////////////////////////
  public void runUpdateWage() {
    updateWage(this.wasPositionFilledLastPeriod, this.monthsEveryPositionFilled);
  }

  public void runUpdateHiringDecision() {
    calcInvBounds(this.lastMonthSales);
    updateHiringDecision(this.inventoryCeiling, this.inventoryFloor);
  }
}
