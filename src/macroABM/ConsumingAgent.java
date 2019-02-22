package macroABM;

import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.Network;

public class ConsumingAgent extends EconomicAgent {

  protected static Network<Object> consumptionNetwork;
  protected BankAgent myBank;

  public ConsumingAgent() {
    super();
  }

  public ConsumingAgent(int givenCash, int givenAssets, int givenLiabilities) {
    super(givenCash, givenAssets, givenLiabilities);
  }
  // temporary testing function to generate wealth
  public void genCash() {
    this.ledger.put("cash", this.ledger.get("cash") + RandomHelper.nextIntFromTo(1, 5));
  }

  // temporary testing method to access cash
  public Integer getCash() {
    return this.ledger.get("cash");
  }

  public void setBank(BankAgent givenBank) {
    this.myBank = givenBank;
  }
}
