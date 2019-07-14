package io.mwarzecha.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public class Money {

  private final Currency currency;
  private final BigDecimal value;

  public static Money zeroOf(String currency) {
    return of(currency, BigDecimal.ZERO);
  }

  public static Money of(String currencyCode, BigDecimal value) {
    return new Money(Currency.getInstance(currencyCode), value);
  }

  private Money(Currency currency, BigDecimal value) {
    this.currency = currency;
    this.value = value.setScale(currency.getDefaultFractionDigits(), RoundingMode.DOWN);
  }

  String getCurrencyCode() {
    return currency.getCurrencyCode();
  }

  BigDecimal getValue() {
    return value;
  }
}