/*
 * A class that represents a transaction between two agents, used to help coordinate agent interaction
 */
package macroABM;

import java.math.BigDecimal;
import org.joda.money.Money;

public class transaction {

  // A static int used to help account for transaction correctness if needed, by allowing the user
  // to keep track of and differentiate transactions
  private static int currentTransactionID = 0;

  // Instance variables used to define this transaction.
  private int transactionID;
  private Money amountMoney;
  private BigDecimal amountBought;
  private Object seller;
  private Object buyer;
  private String reason;

  // Default constructor
  public transaction(
      Object givenBuyer,
      Money givenAmountMoney,
      int givenAmountBought,
      Object givenSeller,
      String givenReason) {
    this.buyer = givenBuyer;
    this.amountMoney = givenAmountMoney;
    this.amountBought = new BigDecimal(givenAmountBought);
    this.seller = givenSeller;
    this.reason = givenReason;
    this.transactionID = currentTransactionID + 1;
    currentTransactionID++;
  }

  //////////////////////////// GETTER METHODS ///////////////////////////////////////////////

  public Object getSeller() {
    return this.seller;
  }

  public Object getBuyer() {
    return this.buyer;
  }

  public Money getAmountMoney() {
    return this.amountMoney;
  }

  public BigDecimal getAmountBought() {
    return this.amountBought;
  }

  public String getReason() {
    return this.reason;
  }
}
