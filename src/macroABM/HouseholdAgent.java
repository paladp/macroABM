/*
 * Class for representing household agents
 */
package macroABM;

import repast.simphony.space.graph.Network;

public class HouseholdAgent extends EconomicAgent {

  private static Network<Object> goodsConsumptionNetwork;
  private BankAgent myBank;

  // Default constructor from super EconomicAgent.java
  public HouseholdAgent() {
    super();
  }

  // Constructor from super EconomicAgent.java
  public HouseholdAgent(double givenCash, double givenAssets, double givenLiabilities) {
    super(givenCash, givenAssets, givenLiabilities);
  }

  // Setter function to set static reference to the consumption good network
  public static void setGoodsConsumptionNetwork(Network<Object> givenNetwork) {
    goodsConsumptionNetwork = givenNetwork;
  }

  // Setter function for this agents bank
  public void setBank(BankAgent givenBank) {
    this.myBank = givenBank;
  }
}
