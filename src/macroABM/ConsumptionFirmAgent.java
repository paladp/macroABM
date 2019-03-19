package macroABM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.joda.money.Money;
import repast.simphony.random.RandomHelper;

public class ConsumptionFirmAgent extends EconomicAgent {

  // This is the coefficient used when determining what amount of post wage paid profits to keep as
  // a buffer for bad times
  protected static BigDecimal profitBufferCoeff = new BigDecimal("0.10");

  // This is the coefficient used when calculating the inventory floor
  protected static BigDecimal invFloorCoeff = new BigDecimal("0.25");

  // This is the coefficient used when calculating the inventory ceiling
  protected static BigDecimal invCeilCoeff = new BigDecimal("1.00");

  // This is the coefficient used when calculating the price floor
  protected static BigDecimal priceFloorCoeff = new BigDecimal("1.025");

  // This is the coefficient used when calculating the price ceiling
  protected static BigDecimal priceCeilCoeff = new BigDecimal("1.15");

  // This is the coefficient used when determining how many consumption goods each worker produces
  // per day
  protected static BigDecimal techProductionCoeff = new BigDecimal("3.00");

  // This is the threshold that determines how many consecutive months where all open positions have
  // been filled are needed before the firm starts to decrease wages
  protected static int wageDecreaseThreshold = 24;

  // This is the upper bound for the uniform distribution used to increase and decrease wage
  protected static double upperBoundUniformForWage = 0.019;

  // This is the upper bound for the uniform distribution used to increase price
  protected static double upperBoundUniformForPrice = 0.02;

  ////////////////////////////////////////// INSTANCE VARIABLES///////////////////////////

  // This is the wage this firms pays to its workers
  protected Money offeredWage;

  // This is the amount of workers currently employed by the firm
  protected BigDecimal amountOfWorkers;

  // This is the price this firm charges for its consumption goods
  protected Money priceOfGoodsSold;

  // This is the amount of goods sold in the most recent month
  protected BigDecimal lastMonthSales;

  // This is the price floor for this firm. Price decreases cannot go below this price
  protected Money priceFloor;

  // This is the price ceiling for this firm. Price increases cannot go above this price
  protected Money priceCeiling;

  // This is the amount it costs the firm to produce every unit of consumption good
  protected Money costPerUnitProduced;

  // This is the inventory floor for this firm. This determines the lower threshold for amount of
  // inventories appropriate for this firm. This will influence price and hiring decisions of this
  // firm
  protected BigDecimal inventoryFloor;

  // This is the inventory ceiling for this firm. This determines the upper threshold for amount of
  // inventories appropriate for this firm. This will influence price and hiring decisions of this
  // firm
  protected BigDecimal inventoryCeiling;

  // This determines whether a position offered last period was filled
  protected boolean wasPositionFilledLastPeriod;

  // This determines if there was a position offered last period at all
  protected boolean wasPositionOfferedLastPeriod;

  // This keeps track of the amount of consecutive months where every position in the company was
  // filled
  protected int monthsEveryPositionFilled;

  // This keeps track of whether this firm plans to fire an existing worker
  protected boolean firingWorker;

  // This keeps track of whether this firm plans to hire an additional worker
  protected boolean hiringWorker;

  protected String payOffDecision;

  // Default Constructor for a consumption firm agent. Determines the starting parameters of the
  // simulation
  public ConsumptionFirmAgent() {
    this.ledger.put("Cash", Money.of(usd, 0.0d));
    this.ledger.put("Inventories", new BigDecimal("10"));
    // Works out to 7 dollars/hour at 40 hour weeks
    this.offeredWage = Money.of(usd, 1120.00d);
    this.priceOfGoodsSold = Money.of(usd, 5.80d);
  }

  public ConsumptionFirmAgent(double givenCash, int givenInventories) {
    this.ledger.put("Cash", Money.of(usd, givenCash));
    this.ledger.put("Inventories", new BigDecimal(givenInventories));
    this.priceOfGoodsSold = Money.of(usd, 5.80d);
  }

  // Method that updates the offered wage of this firm. Needs to know if there was a position
  // offered last period, if a position was filled last period, and the amount of consecutive months
  // that positions were all filled. If positions were offered last period but no one was hired to
  // fill it, increase the wage to attract more workers. If the firm has had all positions filled
  // for a certain amount of months, then decrease the wage
  protected void updateWage(
      boolean givenWasPositionOfferedLastPeriod,
      boolean givenWasPositionFilledLastPeriod,
      int givenMonthsEveryPositionFilled) {

    RandomHelper.createUniform();
    if (givenWasPositionOfferedLastPeriod == true && givenWasPositionFilledLastPeriod == false) {
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

  // Method that updates the inventory floor and ceiling that affect hiring and pricing decisions.
  // The floor and ceiling are calculated by multiplying the last months sales by the coefficients.
  protected void calcInvBounds(BigDecimal givenSales) {
    this.inventoryFloor = invFloorCoeff.multiply(givenSales).setScale(2);
    this.inventoryCeiling = invCeilCoeff.multiply(givenSales).setScale(2);
  }

  // Method that updates boolean values that represent hiring and firing decisions of this firm.
  // Needs the inventory floor and ceiling. If the inventory is larger than the ceiling, the firm
  // should fire a worker. If the inventory is lower than the floor, the firm should hire a worker.
  // If the inventory is in between, neither fire nor hire a worker.
  protected void updateHiringDecision(BigDecimal floor, BigDecimal cieling) {
    if (((BigDecimal) this.ledger.get("Inventories")).compareTo(floor) == -1) {
      this.hiringWorker = true;
      this.firingWorker = false;
    } else if (((BigDecimal) this.ledger.get("Inventories")).compareTo(cieling) == 1) {
      this.hiringWorker = false;
      this.firingWorker = true;
    } else {
      this.hiringWorker = false;
      this.firingWorker = false;
    }
  }

  // Method that calculates the cost per unit of consumption good produced. Needs the monthly wage,
  // the amount of workers, and the productivity/amount of goods produced by each worker.
  protected void calcCostPerUnitProduced(
      Money givenOfferedWage,
      BigDecimal givenAmountOfWorkers,
      BigDecimal givenTechProductionCoeff) {

    BigDecimal outputPerMonth =
        givenTechProductionCoeff.multiply(givenAmountOfWorkers).multiply(new BigDecimal("21.00"));

    this.costPerUnitProduced =
        givenOfferedWage
            .multipliedBy(givenAmountOfWorkers, RoundingMode.HALF_UP)
            .dividedBy(outputPerMonth, RoundingMode.HALF_UP);
  }

  // Method that calculates the floor and ceiling of a firm's consumption good price
  protected void calcPriceBounds(Money givenCostPerUnitProduced) {
    this.priceFloor =
        givenCostPerUnitProduced.multipliedBy(this.priceFloorCoeff, RoundingMode.HALF_UP);
    this.priceCeiling =
        givenCostPerUnitProduced.multipliedBy(this.priceCeilCoeff, RoundingMode.HALF_UP);
  }

  protected void updatePrice(
      BigDecimal givenInventoryFloor,
      BigDecimal givenInventoryCeiling,
      Money givenPriceFloor,
      Money givenPriceCeiling) {

    // If our current level of inventories is less than or equal to the inventory floor, then update
    // the price of consumption goods by increasing it by a growth factor pulled from a uniform
    // distribution as long as it is less than the price ceiling
    if (((BigDecimal) this.ledger.get("Inventories")).compareTo(givenInventoryFloor) == -1
        || ((BigDecimal) this.ledger.get("Inventories")).compareTo(givenInventoryFloor) == 0) {
      Money updatedPrice =
          this.priceOfGoodsSold.multipliedBy(
              1.00 + RandomHelper.getUniform().nextDoubleFromTo(0.0d, upperBoundUniformForPrice),
              RoundingMode.HALF_UP);
      if (updatedPrice.isLessThan(givenPriceCeiling) == true) {
        this.priceOfGoodsSold = updatedPrice;
      }
    }

    // If our current level of inventories is greater than or equal to the inventory ceiling, then
    // update the price of consumption goods by decreasing it by a growth factor pulled from a
    // uniform distribution as long as it is more than the price floor
    else if (((BigDecimal) this.ledger.get("Inventories")).compareTo(givenInventoryCeiling) == 1
        || ((BigDecimal) this.ledger.get("Inventories")).compareTo(givenInventoryCeiling) == 0) {
      Money updatedPrice =
          this.priceOfGoodsSold.multipliedBy(
              1.00 - RandomHelper.getUniform().nextDoubleFromTo(0.0d, upperBoundUniformForPrice),
              RoundingMode.HALF_UP);
      if (updatedPrice.isGreaterThan(givenPriceFloor) == true) {
        this.priceOfGoodsSold = updatedPrice;
      }
    }
  }

  protected void produceGoods(
      BigDecimal givenAmountOfWorkers, BigDecimal givenTechProductionCoeff) {
    BigDecimal initialInventory = (BigDecimal) this.ledger.get("Inventories");
    BigDecimal dailyProduction = givenAmountOfWorkers.multiply(givenTechProductionCoeff);
    this.ledger.put("Inventories", initialInventory.add(dailyProduction));
  }

  protected void calcPayOffDecision() {
    Money projectedWageDisbursement =
        this.offeredWage.multipliedBy(this.amountOfWorkers, RoundingMode.HALF_DOWN);

    Money profitBuffer =
        projectedWageDisbursement.multipliedBy(profitBufferCoeff, RoundingMode.HALF_DOWN);

    if (((Money) this.ledger.get("Cash"))
            .minus(projectedWageDisbursement)
            .minus(profitBuffer)
            .isNegativeOrZero()
        == true) {
      this.payOffDecision = "nobuffer";

      if (((Money) this.ledger.get("Cash")).minus(projectedWageDisbursement).isNegativeOrZero()
          == true) {
        // decrease wage until it is exactly zero, then pay using the updated wage also not using
        // any buffer
      }
    } else {
      this.payOffDecision = "regular";
    }
  }

  //////////////////////////// GETTER METHODS ///////////////////////////////////////////////
  public Money getOfferedWage() {
    return this.offeredWage;
  }

  public boolean getHiringDecision() {
    return this.hiringWorker;
  }

  public boolean getFiringDecision() {
    return this.firingWorker;
  }

  public BigDecimal getInvCiel() {
    return this.inventoryCeiling;
  }

  public BigDecimal getInvFloor() {
    return this.inventoryFloor;
  }

  public Money getPrice() {
    return this.priceOfGoodsSold;
  }

  public Money getCostPerUnitProduced() {
    return this.costPerUnitProduced;
  }

  public Money getPriceCeiling() {
    return this.priceCeiling;
  }

  public Money getPriceFloor() {
    return this.priceFloor;
  }

  public int getInventory() {
    return ((BigDecimal) this.ledger.get("Inventories")).intValue();
  }

  public String getPayOffDecision() {
    return this.payOffDecision;
  }
  //////////////////////////// PUBLIC METHODS FOR SIM////////////////////////////////////////
  public void runUpdateWage() {
    updateWage(
        this.wasPositionOfferedLastPeriod,
        this.wasPositionFilledLastPeriod,
        this.monthsEveryPositionFilled);
  }

  public void runUpdateHiringDecision() {
    calcInvBounds(this.lastMonthSales);
    updateHiringDecision(this.inventoryCeiling, this.inventoryFloor);
  }

  public void runUpdatePrice() {
    calcInvBounds(this.lastMonthSales);
    calcCostPerUnitProduced(
        this.costPerUnitProduced, this.amountOfWorkers, this.techProductionCoeff);
    calcPriceBounds(this.costPerUnitProduced);
    updatePrice(this.inventoryFloor, this.inventoryCeiling, this.priceFloor, this.priceCeiling);
  }
}
