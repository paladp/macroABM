/*
 *	A parent class to the bank,consumption firm, capital firm, and household agents
 *	Creates static variables to hold references to the context and projections the 
 *	agents operate in, and creates the hash table that works as a balance sheet
 */
package macroABM;

import java.util.Hashtable;
import repast.simphony.context.Context;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;

public abstract class EconomicAgent {

  protected static Context<Object> mainContext;
  protected static ContinuousSpace<Object> mainSpace;
  protected static Network<Object> bankingNetwork;
  protected Hashtable<String, Integer> ledger = new Hashtable<String, Integer>();

  public EconomicAgent() {
    ledger.put("cash", 0);
    ledger.put("assets", 0);
    ledger.put("liabilities", 0);
  }

  public EconomicAgent(Integer startingCash, Integer startingAssets, Integer startingLiability) {
    ledger.put("cash", startingCash);
    ledger.put("assets", startingAssets);
    ledger.put("liabilities", startingLiability);
  }

  public static void setContext(Context<Object> givenContext) {
    mainContext = givenContext;
  }

  public static void setSpace(ContinuousSpace<Object> givenSpace) {
    mainSpace = givenSpace;
  }

  public static void setBankingNetwork(Network<Object> givenNetwork) {
    bankingNetwork = givenNetwork;
  }
}
