/*
 * Class for representing household agents
 */
package macroABM;

import repast.simphony.space.graph.Network;

public class HouseholdAgent extends EconomicAgent {

  private static Network<Object> goodsConsumptionNetwork;

  // Default constructor from super EconomicAgent.java
  public HouseholdAgent() {
    super();
  }

  // Constructor from super EconomicAgent.java
  public HouseholdAgent(double givenCash) {
    super(givenCash);
  }

  // Setter function to set static reference to the consumption good network
  public static void setGoodsConsumptionNetwork(Network<Object> givenNetwork) {
    goodsConsumptionNetwork = givenNetwork;
  }
}
