package io.mwarzecha.model;

import java.math.BigDecimal;

public class Account {

  private final long id;
  private final Money balance;
  private final String owner;

  public static Builder newBuilder() {
    return new Builder();
  }

  private Account(Builder builder) {
    this(builder.id, builder.balance, builder.owner);
  }

  private Account(long id, Money balance, String owner) {
    this.id = id;
    this.balance = balance;
    this.owner = owner;
  }

  public long getId() {
    return id;
  }

  public BigDecimal getBalance() {
    return balance.getValue();
  }

  public String getCurrency() {
    return balance.getCurrencyCode();
  }

  public String getOwner() {
    return owner;
  }

  public Account withId(long id) {
    return new Account(id, this.balance, this.owner);
  }
  
  public static class Builder {

    private long id;
    private Money balance;
    private String owner;

    private Builder() {}

    public Builder id(long id) {
      this.id = id;
      return this;
    }

    public Builder balance(Money balance) {
      this.balance = balance;
      return this;
    }

    public Builder owner(String owner) {
      this.owner = owner;
      return this;
    }

    public Account build() {
      return new Account(this);
    }
  }
}