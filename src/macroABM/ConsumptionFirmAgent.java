package macroABM;

import java.math.BigDecimal;
import org.joda.money.Money;

public class ConsumptionFirmAgent extends EconomicAgent {
  	
  
  protected static BigDecimal profitBufferCoeff = new BigDecimal("0.10");
  protected static BigDecimal invFloorCoeff = new BigDecimal("0.25");
  protected static BigDecimal invCeilCoeff = new BigDecimal ("1.00");
  protected static BigDecimal priceFloorCoeff = new BigDecimal ("1.025");
  protected static BigDecimal priceCeilCoeff = new BigDecimal ("1.15");
  protected static int techProductionCoeff = 3;
  protected static int wageDecreaseThreshold = 24;
  protected static double upperBoundUniformForWage = 0.019;
  protected static double upperBoundUniformForPrice = 0.02;
  
  protected Money offeredWage;
  protected Money priceOfGoodsSold;
  protected int priceCeiling;
  protected int priceFloor;
  protected int inventoryCeiling;
  protected int inventoryFloor;
  protected boolean positionFilledLastPeriod;
  protected boolean positionOffered;
  protected int monthsEveryPositionFilled;
  
  public ConsumptionFirmAgent() {
	  this.ledger.put("Cash", Money.of(usd, 0.0d));
	  this.ledger.put("Inventories", Integer.parseInt("10"));
	  this.offeredWage = Money.of(usd, 7.00d);
	  this.priceOfGoodsSold = Money.of(usd, 1.00d);
  }

  public ConsumptionFirmAgent(double givenCash) {
    super(givenCash);
  }

  public ConsumptionFirmAgent(double givenCash, int givenInventories) {
    this.ledger.put("cash", Money.of(usd, givenCash));
    this.ledger.put("inventories", givenInventories);
  }
}
