package macroABM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import repast.simphony.engine.schedule.ScheduledMethod;
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

  protected Money dividends;

  protected double marketShareByEmployees;

  protected int amountSold;

  // Default Constructor for a consumption firm agent. Determines the starting parameters of the
  // simulation
  public ConsumptionFirmAgent() {
    // Will have 10 employees to start with. Give them enough to pay all for the first period, plus
    // enough for the buffer and one more worker on top of that. 11,200 for ten workers, 1,120 for
    // buffer, 1,120 for one more worker for a total of 13440
    // was 72800
    // was 22500
    this.ledger.put("Cash", Money.of(usd, 6000.0d));

    // Give them 510 to start with
    this.ledger.put("Inventories", new BigDecimal("510.00"));

    // Works out to 7 dollars/hour at 40 hour weeks
    this.offeredWage = Money.of(usd, 1120.00d);

    // At 1120 per month, and producing 3 units per day, every unit costs 17.77 to produce setting
    // an initial lower and upper bound of price at 18.21 and 20.44. Set it in the middle
    this.priceOfGoodsSold = Money.of(usd, 19.00d);

    // Start with zero workers;
    this.amountOfWorkers = new BigDecimal("0.0");

    // Assume that each firm sold 80% of their total productive capacity last period
    this.lastMonthSales = new BigDecimal("504");

    // Both will be updated, but need to be initialized;
    this.inventoryFloor = new BigDecimal("0");
    this.inventoryCeiling = new BigDecimal("0");

    this.wasPositionFilledLastPeriod = true;
    this.wasPositionOfferedLastPeriod = true;
    this.monthsEveryPositionFilled = 10;
    this.firingWorker = false;
    this.hiringWorker = false;
    this.dividends = Money.of(usd, 0.00d);
    this.amountSold = 0;
  }

  public ConsumptionFirmAgent(double givenCash, int givenInventories) {
    this.ledger.put("Cash", Money.of(usd, givenCash));
    this.ledger.put("Inventories", new BigDecimal(givenInventories));
    this.priceOfGoodsSold = Money.of(usd, 5.80d);
  }

  protected void updateMonthsEveryPositionFilled() {
    if (this.wasPositionOfferedLastPeriod == true && this.wasPositionFilledLastPeriod == false
        || this.amountOfWorkers.intValue() == 0) {
      this.monthsEveryPositionFilled = 0;
    } else {
      this.monthsEveryPositionFilled++;
    }
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
    System.out.println(this + " started with wage equal to " + this.offeredWage.toString());
    if (givenWasPositionOfferedLastPeriod == true && givenWasPositionFilledLastPeriod == false) {
      System.out.println(this + " is increasing wage");
      offeredWage =
          offeredWage.multipliedBy(
              1.00 + RandomHelper.getUniform().nextDoubleFromTo(0.0d, upperBoundUniformForWage),
              RoundingMode.HALF_UP);
    } else if (givenMonthsEveryPositionFilled >= 24 && this.amountOfWorkers.intValue() > 0) {
      System.out.println(this + " is increasing wage");
      offeredWage =
          offeredWage.multipliedBy(
              1.00 - RandomHelper.getUniform().nextDoubleFromTo(0.0d, upperBoundUniformForWage),
              RoundingMode.HALF_UP);
    } else {
      System.out.println(this + " did not raise the wage this time step");
    }
    System.out.println(this + " has new wage " + this.offeredWage.toString());
    if (this.offeredWage.isNegative()) {
      System.out.println(
          this + " offered a position last period?" + givenWasPositionOfferedLastPeriod);
      System.out.println(
          this + " offered a filled last period?" + givenWasPositionFilledLastPeriod);
      System.out.println(this + " consecutive full capacity?" + givenMonthsEveryPositionFilled);
      System.exit(0);
    }
  }

  // Method that updates the inventory floor and ceiling that affect hiring and pricing decisions.
  // The floor and ceiling are calculated by multiplying the last months sales by the coefficients.
  protected void calcInvBounds(BigDecimal givenSales) {
    this.inventoryFloor = invFloorCoeff.multiply(givenSales).setScale(2);
    this.inventoryCeiling = invCeilCoeff.multiply(givenSales).setScale(2);
    System.out.println(
        this
            + "has a floor and ceiling of: "
            + this.inventoryFloor
            + " and "
            + this.inventoryCeiling);
  }

  // Method that updates boolean values that represent hiring and firing decisions of this firm.
  // Needs the inventory floor and ceiling. If the inventory is larger than the ceiling, the firm
  // should fire a worker. If the inventory is lower than the floor, the firm should hire a worker.
  // If the inventory is in between, neither fire nor hire a worker.
  protected void updateHiringDecision(BigDecimal floor, BigDecimal cieling) {

    if (((BigDecimal) this.ledger.get("Inventories")).compareTo(floor) == -1) {
      this.hiringWorker = true;
      this.firingWorker = false;
      this.wasPositionOfferedLastPeriod = true;
    } else if (((BigDecimal) this.ledger.get("Inventories")).compareTo(cieling) == 1) {
      this.hiringWorker = false;
      this.firingWorker = true;
      this.wasPositionOfferedLastPeriod = false;
    } else {
      this.hiringWorker = false;
      this.firingWorker = false;
      this.wasPositionOfferedLastPeriod = false;
    }

    System.out.println(
        this
            + " is hiring a worker: "
            + this.hiringWorker
            + ". This is firing a worker: "
            + this.firingWorker);
  }

  // Method that calculates the cost per unit of consumption good produced. Needs the monthly wage,
  // the amount of workers, and the productivity/amount of goods produced by each worker.
  protected void calcCostPerUnitProduced(
      Money givenOfferedWage,
      BigDecimal givenAmountOfWorkers,
      BigDecimal givenTechProductionCoeff) {

    BigDecimal outputPerMonth =
        givenTechProductionCoeff.multiply(givenAmountOfWorkers).multiply(new BigDecimal("21.00"));
    if (givenAmountOfWorkers.intValue() > 0) {
      this.costPerUnitProduced =
          givenOfferedWage
              .multipliedBy(givenAmountOfWorkers, RoundingMode.HALF_UP)
              .dividedBy(outputPerMonth, RoundingMode.HALF_UP);
    } else {
      this.costPerUnitProduced = Money.of(CurrencyUnit.USD, 0.00);
    }

    System.out.println(this + "has cost per unit produced of: " + this.costPerUnitProduced);
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
        || ((BigDecimal) this.ledger.get("Inventories")).compareTo(givenInventoryFloor) == 0
            && givenPriceFloor.isPositive()
            && givenPriceCeiling.isPositive()) {
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
        || ((BigDecimal) this.ledger.get("Inventories")).compareTo(givenInventoryCeiling) == 0
            && givenPriceFloor.isPositive()
            && givenPriceCeiling.isPositive()) {
      Money updatedPrice =
          this.priceOfGoodsSold.multipliedBy(
              1.00 - RandomHelper.getUniform().nextDoubleFromTo(0.0d, upperBoundUniformForPrice),
              RoundingMode.HALF_UP);
      if (updatedPrice.isGreaterThan(givenPriceFloor) == true) {
        this.priceOfGoodsSold = updatedPrice;
      }
    }

    System.out.println(this + "now has a new price of " + this.priceOfGoodsSold.toString());
  }

  @ScheduledMethod(start = 21, interval = 21, priority = 90)
  public void calcPayOffDecision() {
    System.out.println("in calcpayoffdecision");
    // Calculate how much money the firm expectes to pay in wages
    Money projectedWageDisbursement =
        this.offeredWage.multipliedBy(this.amountOfWorkers, RoundingMode.HALF_DOWN);
    System.out.println(
        this
            + " has "
            + this.amountOfWorkers
            + " workers and pays each "
            + this.offeredWage.toString()
            + " for a total of "
            + projectedWageDisbursement);
    // Calculate how much money the firm expects to save
    Money profitBuffer =
        projectedWageDisbursement.multipliedBy(profitBufferCoeff, RoundingMode.HALF_DOWN);

    System.out.println(
        this
            + " will have "
            + ((Money) this.ledger.get("Cash"))
                .minus(projectedWageDisbursement)
                .minus(profitBuffer)
                .toString()
            + " after wages and profit buffer");

    // If paying the wages and having a buffer will make the wage have no or negative money, then
    // have no buffer payment or pay dividends
    if (((Money) this.ledger.get("Cash")).minus(projectedWageDisbursement).isNegativeOrZero()
            == true
        && this.amountOfWorkers.intValue() > 0) {
      // decrease wage until it is exactly zero, then pay using the updated wage also not using
      // any buffer
      this.offeredWage =
          ((Money) this.ledger.get("Cash")).dividedBy(this.amountOfWorkers, RoundingMode.HALF_DOWN);
      this.payOffDecision = "onlywage";
      this.dividends = Money.of(usd, 0.00d);
      System.out.println(this + "has decreased wages to: " + this.offeredWage.toString());
    } else if (((Money) this.ledger.get("Cash"))
                .minus(projectedWageDisbursement)
                .minus(profitBuffer)
                .isNegativeOrZero()
            == true
        && this.amountOfWorkers.intValue() > 0) {

      this.payOffDecision = "nobuffer";
      this.dividends = Money.of(usd, 0.00d);
    } else if (this.amountOfWorkers.intValue() > 0) {
      this.payOffDecision = "regular";
      this.dividends =
          ((Money) this.ledger.get("Cash")).minus(projectedWageDisbursement).minus(profitBuffer);
    } else {
      this.payOffDecision = "noworkers";
      this.dividends = Money.of(usd, 0.00d);
    }

    // Since we know that our cash minus wage payments minus buffer is negative, we also check if
    // just the wages alone will make us have negative or no money if it is, then we update the wage
    // so we can make it

    System.out.println(
        this
            + "has payoff decision: "
            + this.payOffDecision
            + " and will pay dividends:  "
            + this.dividends);
  }

  public void produceGoods(BigDecimal givenAmountOfWorkers, BigDecimal givenTechProductionCoeff) {
    System.out.println("IN PRODUCE");
    BigDecimal initialInventory = (BigDecimal) this.ledger.get("Inventories");
    BigDecimal dailyProduction = givenAmountOfWorkers.multiply(givenTechProductionCoeff);
    System.out.println(this + " had " + this.ledger.get("Inventories") + " inventories");
    this.ledger.put("Inventories", initialInventory.add(dailyProduction));
    System.out.println(this + " now has " + this.ledger.get("Inventories") + " inventories");
  }
  //////////////////////////// GETTER METHODS ///////////////////////////////////////////////
  public int getDemand() {
    return this.amountSold;
  }

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

  public BigDecimal getAmountOfWorkers() {
    return this.amountOfWorkers;
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

  public BigDecimal getCashDisplay() {
    return ((Money) this.ledger.get("Cash")).getAmountMajor();
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

  public Money getDividends() {
    return this.dividends;
  }

  public double getMarketShareByEmployees() {
    return this.marketShareByEmployees;
  }

  public boolean canSell() {
    if (((BigDecimal) this.ledger.get("Inventories")).intValue() <= 0) {
      return false;
    } else return true;
  }
  //////////////////////////// SETTER METHODS ///////////////////////////////////////////////

  public void increaseAmountOfWorkers(int amountToIncrease) {
    this.amountOfWorkers = this.amountOfWorkers.add(new BigDecimal(amountToIncrease));
  }

  public void decreaseAmountOfWorkers(int amountToDecrease) {
    this.amountOfWorkers = this.amountOfWorkers.subtract(new BigDecimal(amountToDecrease));
  }

  public void handleTransaction(transaction givenTransaction) {
    if (givenTransaction.getReason().equals("wage")) {
      if (givenTransaction.getBuyer() == this) {
        Money newBalance =
            ((Money) this.ledger.get("Cash")).minus(givenTransaction.getAmountMoney());
        this.ledger.put("Cash", newBalance);
      }
    }

    if (givenTransaction.getReason().equals("consumption")) {
      if (givenTransaction.getSeller() == this) {
        BigDecimal newInventories =
            ((BigDecimal) this.ledger.get("Inventories"))
                .subtract(givenTransaction.getAmountBought());
        Money newBalance =
            ((Money) this.ledger.get("Cash")).plus(givenTransaction.getAmountMoney());
        this.lastMonthSales = this.lastMonthSales.add(givenTransaction.getAmountBought());
        this.ledger.put("Inventories", newInventories);
        this.ledger.put("Cash", newBalance);
        this.amountSold++;
      }
    }
  }

  public void setMarketShareByEmployees(double givenShare) {
    this.marketShareByEmployees = givenShare;
  }

  public void setWasPositionOfferedLastPeriod(boolean value) {
    this.wasPositionOfferedLastPeriod = value;
  }

  public void setWasPositionFilledLastPeriod(boolean value) {
    this.wasPositionFilledLastPeriod = value;
  }

  public void resetMonthlyDemand() {
    this.lastMonthSales = new BigDecimal(0.0);
  }
  //////////////////////////// PUBLIC METHODS FOR SIM////////////////////////////////////////

  @ScheduledMethod(start = 1, interval = 21, priority = 101)
  public void runUpdateWage() {
    updateMonthsEveryPositionFilled();
    updateWage(
        this.wasPositionOfferedLastPeriod,
        this.wasPositionFilledLastPeriod,
        this.monthsEveryPositionFilled);
    this.wasPositionFilledLastPeriod = false;
  }

  @ScheduledMethod(start = 1, interval = 21, priority = 99)
  public void runUpdateHiringDecision() {
    calcInvBounds(this.lastMonthSales);
    updateHiringDecision(this.inventoryFloor, this.inventoryCeiling);
  }

  @ScheduledMethod(start = 1, interval = 21, priority = 98)
  public void runUpdatePrice() {
    calcInvBounds(this.lastMonthSales);
    calcCostPerUnitProduced(this.offeredWage, this.amountOfWorkers, this.techProductionCoeff);
    calcPriceBounds(this.costPerUnitProduced);
    updatePrice(this.inventoryFloor, this.inventoryCeiling, this.priceFloor, this.priceCeiling);
    resetMonthlyDemand();
  }

  @ScheduledMethod(start = 1, interval = 1, priority = 91)
  public void runProduceGoods() {
    produceGoods(this.amountOfWorkers, techProductionCoeff);
  }
}
