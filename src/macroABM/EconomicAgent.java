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

  protected static Context<Object> mainContext;
  protected static ContinuousSpace<Object> mainSpace;
  protected static Network<Object> bankingNetwork;
  protected static CurrencyUnit usd = CurrencyUnit.of("USD");
  protected final BigDecimal adaptiveExpectationsParameter = new BigDecimal("0.25");
  protected Hashtable<String, Money> ledger = new Hashtable<String, Money>();

  public EconomicAgent() {
    ledger.put("cash", Money.of(usd, 0.0d));
    ledger.put("assets", Money.of(usd, 0.0d));
    ledger.put("liabilities", Money.of(usd, 0.0d));
  }

  public EconomicAgent(double startingCash, double startingAssets, double startingLiability) {
    ledger.put("cash", Money.of(usd, startingCash));
    ledger.put("assets", Money.of(usd, startingAssets));
    ledger.put("liabilities", Money.of(usd, startingLiability));
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

  protected Money calculateExpectedVariable(
      Money lastPeriodExpectedVariableValue, Money lastPeriodVariableValue) {
    Money currentPeriodExpectedVariableValue =
        Money.of(
            usd,
            lastPeriodVariableValue
                .getAmount()
                .subtract(lastPeriodExpectedVariableValue.getAmount())
                .multiply(adaptiveExpectationsParameter)
                .add(lastPeriodExpectedVariableValue.getAmount()));
    return currentPeriodExpectedVariableValue;
  }

  // temporary testing method to access cash
  public Money getCash() {
    return this.ledger.get("cash");
  }

  // temporary testing method to print out cash
  public void printCash() {
    System.out.println(this + " has " + this.ledger.get("cash").toString() + " in cash.");
  }
}
