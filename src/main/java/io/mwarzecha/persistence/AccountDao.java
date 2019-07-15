package io.mwarzecha.persistence;

import io.mwarzecha.model.Account;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

class AccountDao {

  private static final RowMapper<Account> ACCOUNT_ROW_MAPPER = new AccountRowMapper();

  private final Handle handle;

  static AccountDao withHandle(Handle handle) {
    return new AccountDao(handle);
  }

  private AccountDao(Handle handle) {
    this.handle = handle;
  }

  List<Account> getAllAccounts() {
    return handle
        .select("SELECT * FROM account")
        .map(ACCOUNT_ROW_MAPPER)
        .list();
  }

  Optional<Account> getAccountById(long accountId) {
    return handle
        .select("SELECT * FROM account WHERE id = ?", accountId)
        .map(ACCOUNT_ROW_MAPPER)
        .findFirst();
  }

  Account persistAccount(Account account) {
    long accountId = handle
        .createUpdate("INSERT INTO account (owner, currency, balance) VALUES (?, ?, ?)")
        .bind(0, account.getOwner())
        .bind(1, account.getCurrency())
        .bind(2, account.getBalance())
        .executeAndReturnGeneratedKeys()
        .mapTo(Long.class)
        .one();
    return account.withId(accountId);
  }

  AccountDao debitAccount(long accountId, BigDecimal amount, String currency) {
    int rowsUpdated = executeDebitUpdate(accountId, amount, currency);
    assertOne(rowsUpdated, accountNotFoundMessageSupplier(accountId, currency));
    return this;
  }

  private int executeDebitUpdate(long accountId, BigDecimal amount, String currency) {
    try {
      return handle
          .execute("UPDATE account SET balance = balance - ? WHERE id = ? AND currency = ?",
              amount, accountId, currency);
    } catch (UnableToExecuteStatementException ex) {
      throw new IllegalStateException("Insufficient funds");
    }
  }

  private static void assertOne(int value, Supplier<String> messageSupplier) {
    if (value != 1) {
      throw new IllegalStateException(messageSupplier.get());
    }
  }

  private static Supplier<String> accountNotFoundMessageSupplier(long accountId, String currency) {
    return () -> String.format("Account with id %d and currency %s not found", accountId, currency);
  }

  void creditAccount(long accountId, BigDecimal amount, String currency) {
    int rowsUpdated = handle
        .execute("UPDATE account SET balance = balance + ? WHERE id = ? AND currency = ?",
            amount, accountId, currency);
    assertOne(rowsUpdated, accountNotFoundMessageSupplier(accountId, currency));
  }
}