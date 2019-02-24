/*
 * Class that represents the general functions of Consumption and Capital firms
 */
package macroABM;

import org.joda.money.Money;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.Network;

public class FirmAgent extends EconomicAgent {

  protected static Network<Object> capitalConsumptionNetwork;
  protected BankAgent myBank;

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

  // temporary testing function to generate wealth
  public void genCash() {
    this.ledger.put(
        "cash",
        this.ledger.get("cash").plus(Money.of(usd, (double) RandomHelper.nextIntFromTo(1, 5))));
  }
}
