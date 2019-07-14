package io.mwarzecha.persistence;

import io.mwarzecha.model.Account;
import io.mwarzecha.model.Transfer;
import io.mwarzecha.util.Try;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

class JdbiPersistenceService implements PersistenceService {

  private static final RowMapper<Account> ACCOUNT_ROW_MAPPER = new AccountRowMapper();
  private static final RowMapper<Transfer> TRANSFER_ROW_MAPPER = new TransferRowMapper();

  private final Jdbi jdbi;
  private final Clock clock;

  JdbiPersistenceService(Jdbi jdbi, Clock clock) {
    this.jdbi = jdbi;
    this.clock = clock;
  }

  @Override
  public List<Transfer> findAllAccountTransfers(long accountId) {
    return jdbi.withHandle(handle -> handle
        .select("SELECT * FROM transfer WHERE from_account = ? OR to_account = ?", accountId,
            accountId)
        .map(TRANSFER_ROW_MAPPER)
        .list());
  }

  @Override
  public Optional<Transfer> findAccountTransferById(long accountId, long transferId) {
    return jdbi.withHandle(handle -> handle
        .select("SELECT * FROM transfer WHERE id = ? AND (from_account = ? OR to_account = ?)",
            transferId, accountId, accountId)
        .map(TRANSFER_ROW_MAPPER)
        .findFirst());
  }

  @Override
  public List<Account> getAllAccounts() {
    return jdbi.withHandle(handle -> handle
        .select("SELECT * FROM account")
        .map(ACCOUNT_ROW_MAPPER)
        .list());
  }

  @Override
  public Optional<Account> findAccountById(long accountId) {
    return jdbi.withHandle(handle -> handle
        .select("SELECT * FROM account WHERE id = ?", accountId)
        .map(ACCOUNT_ROW_MAPPER)
        .findFirst());
  }

  @Override
  public Account persistAccount(Account account) {
    long id = jdbi.withHandle(handle -> handle
        .createUpdate("INSERT INTO account (owner, currency, balance) VALUES (?, ?, ?)")
        .bind(0, account.getOwner())
        .bind(1, account.getCurrency())
        .bind(2, account.getBalance())
        .executeAndReturnGeneratedKeys()
        .mapTo(Long.class)
        .one());
    return account.withId(id);
  }

  @Override
  public Try<Transfer> makeTransfer(Transfer transfer) {
    return Try.ofFailable(() -> doMakeTransfer(transfer));
  }

  private Transfer doMakeTransfer(Transfer transfer) {
    return jdbi.inTransaction(TransactionIsolationLevel.READ_COMMITTED, handle -> {
      validateCurrencies(handle, transfer);
      var amount = transfer.getAmount();
      debitAccount(handle, transfer.getFromAccountId(), amount);
      creditAccount(handle, transfer.getToAccountId(), amount);
      return persistTransfer(handle, transfer);
    });
  }

  private static void validateCurrencies(Handle handle, Transfer transfer) {
    var fromAccountCurrency = getAccountCurrencyById(handle, transfer.getFromAccountId());
    var toAccountCurrency = getAccountCurrencyById(handle, transfer.getToAccountId());
    var transferCurrency = transfer.getCurrency();
    requireCurrencyEquals(transferCurrency, fromAccountCurrency);
    requireCurrencyEquals(transferCurrency, toAccountCurrency);
  }

  private static String getAccountCurrencyById(Handle handle, long accountId) {
    return handle.select("SELECT currency FROM account WHERE id = ?", accountId)
        .mapTo(String.class)
        .findFirst()
        .orElseThrow(accountNotFound(accountId));
  }

  private static Supplier<NoSuchElementException> accountNotFound(long accountId) {
    return () -> new NoSuchElementException(
        String.format("Account with id %d not found", accountId));
  }

  private static void requireCurrencyEquals(String expectedCurrency, String actualCurrency) {
    if (!expectedCurrency.equals(actualCurrency)) {
      throw new IllegalArgumentException("Invalid currency");
    }
  }

  private static void debitAccount(Handle handle, long accountId, BigDecimal amount) {
    try {
      handle.execute("UPDATE account SET balance = balance - ? WHERE id = ?", amount,
          accountId);
    } catch (UnableToExecuteStatementException ex) {
      throw new IllegalStateException("Insufficient funds");
    }
  }

  private static void creditAccount(Handle handle, long accountId, BigDecimal amount) {
    handle.execute("UPDATE account SET balance = balance + ? WHERE id = ?", amount, accountId);
  }

  private Transfer persistTransfer(Handle handle, Transfer transfer) {
    var timestamp = clock.instant();
    long id = handle
        .createUpdate("INSERT INTO transfer (from_account, to_account, currency, amount, timestamp) VALUES (?, ?, ?, ?, ?)")
        .bind(0, transfer.getFromAccountId())
        .bind(1, transfer.getToAccountId())
        .bind(2, transfer.getCurrency())
        .bind(3, transfer.getAmount())
        .bind(4, timestamp)
        .executeAndReturnGeneratedKeys()
        .mapTo(Long.class)
        .one();
    return transfer.withIdAndTimestamp(id, timestamp);
  }
}