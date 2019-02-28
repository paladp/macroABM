/*
 * Class that represents the general functions of Consumption and Capital firms
 */
package macroABM;

import java.math.BigDecimal;

import org.joda.money.Money;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.Network;

public class FirmAgent extends EconomicAgent {

  protected static Network<Object> capitalConsumptionNetwork;
  protected BankAgent myBank;
  protected Money pastPeriodSales;
  protected Money pastPeriodSalesExpectations;
  protected Money currentPeriodSalesExpectations;
  protected Money desiredOutput;
  protected static final BigDecimal shareOfExpectedSharesBuffer = new BigDecimal("0.10");

  // default constructor from EconomicAgent.java
  public FirmAgent() {
    super();
  }

  // constructor from EconomicAgent.java
  public FirmAgent(int givenCash, int givenAssets, int givenLiabilities) {
    super(givenCash, givenAssets, givenLiabilities);
  }

  // setter method for an agents bank
  public void setBank(BankAgent givenBank) {
    this.myBank = givenBank;
  }

  // setter method for all agents static reference to the capital goods consumption network
  public static void setCapitalCOnsumptionNetwork(Network<Object> givenNetwork) {
    capitalConsumptionNetwork = givenNetwork;
  }
  
  protected void calcCurrentPeriodSalesExpectations() {
	  this.currentPeriodSalesExpectations = calculateExpectedVariable(pastPeriodSalesExpectations, pastPeriodSales);
  }
  
  protected void calcDesiredOutput() {
	  
  }
  
  

  // temporary testing function to generate wealth
  public void genCash() {
    this.ledger.put(
        "cash",
        this.ledger.get("cash").plus(Money.of(usd, (double) RandomHelper.nextIntFromTo(1, 5))));
  }
}
