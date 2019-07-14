package io.mwarzecha.model;

import java.math.BigDecimal;
import java.time.Instant;

public class Transfer {

  private final long id;
  private final long fromAccountId;
  private final long toAccountId;
  private final Money amount;
  private final Instant timestamp;

  public static Builder newBuilder() {
    return new Builder();
  }

  private Transfer(Builder builder) {
    this(builder.id, builder.fromAccountId, builder.toAccountId, builder.amount, builder.timestamp);
  }

  private Transfer(long id, long fromAccountId, long toAccountId, Money amount, Instant timestamp) {
    this.id = id;
    this.fromAccountId = fromAccountId;
    this.toAccountId = toAccountId;
    this.amount = amount;
    this.timestamp = timestamp;
  }

  public long getId() {
    return id;
  }

  public long getFromAccountId() {
    return fromAccountId;
  }

  public long getToAccountId() {
    return toAccountId;
  }

  public BigDecimal getAmount() {
    return amount.getValue();
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getCurrency() {
    return amount.getCurrencyCode();
  }

  public Transfer withIdAndTimestamp(long id, Instant timestamp) {
    return new Transfer(id, this.fromAccountId, this.toAccountId, this.amount, timestamp);
  }
  
  public static class Builder {

    private long id;
    private long fromAccountId;
    private long toAccountId;
    private Money amount;
    private Instant timestamp;

    private Builder() {}

    public Builder id(long id) {
      this.id = id;
      return this;
    }

    public Builder fromAccountId(long fromAccountId) {
      this.fromAccountId = fromAccountId;
      return this;
    }

    public Builder toAccountId(long toAccountId) {
      this.toAccountId = toAccountId;
      return this;
    }

    public Builder amount(Money amount) {
      this.amount = amount;
      return this;
    }

    public Builder timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Transfer build() {
      return new Transfer(this);
    }
  }
}