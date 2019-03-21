package macroABM;

import java.math.BigDecimal;
import org.joda.money.Money;

public class transaction {
  private static int currentTransactionID = 0;
  private int transactionID;
  private Money amountMoney;
  private BigDecimal amountBought;
  private Object seller;
  private Object buyer;
  private String reason;

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
