/*
 *	A parent class to the bank,consumption firm, capital firm, and household agents
 *	Creates static variables to hold references to the context and projections the
 *	agents operate in, and creates the hash table that works as a balance sheet
 */
package macroABM;

import java.math.BigDecimal;
import java.util.Hashtable;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import repast.simphony.context.Context;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;

public abstract class EconomicAgent {

  // Static variables that hold references to the context and projections the agent operates
  // within, as well as parameters used by all agents in their behaviors
  protected static Context<Object> mainContext;
  protected static ContinuousSpace<Object> mainSpace;
  protected static Network<Object> consumptionNetwork;
  protected static Network<Object> laborNetwork;
  protected static CurrencyUnit usd = CurrencyUnit.of("USD");

  // Variables that all agents have
  protected Hashtable<String, Object> ledger = new Hashtable<String, Object>();

  public EconomicAgent() {
    ledger.put("cash", Money.of(usd, 0.0d));
    // ledger.put("assets", Money.of(usd, 0.0d));
    // ledger.put("liabilities", Money.of(usd, 0.0d));
  }

  public static void setContext(Context<Object> givenContext) {
    mainContext = givenContext;
  }

  public static void setSpace(ContinuousSpace<Object> givenSpace) {
    mainSpace = givenSpace;
  }

  public static void setConsumptionNetwork(Network<Object> givenNetwork) {
    consumptionNetwork = givenNetwork;
  }

  public static void setLaborNetwork(Network<Object> givenNetwork) {
    laborNetwork = givenNetwork;
  }

  // temporary testing method to access cash
  public BigDecimal getCash() {
    return ((Money) this.ledger.get("cash")).getAmount();
  }

  // temporary testing method to print out cash
  public void printCash() {
    System.out.println(this + " has " + this.ledger.get("cash").toString() + " in cash.");
  }

  public void increaseCash(Money amountToIncrease) {
    Money newBalance = ((Money) this.ledger.get("Cash")).plus(amountToIncrease);
    this.ledger.put("Cash", newBalance);
  }

  public void decreaseCash(Money amountToDecrease) {
    Money newBalance = ((Money) this.ledger.get("Cash")).minus(amountToDecrease);
    this.ledger.put("Cash", newBalance);
  }
}
