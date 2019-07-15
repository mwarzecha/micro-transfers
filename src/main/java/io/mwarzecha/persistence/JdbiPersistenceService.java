package io.mwarzecha.persistence;

import io.mwarzecha.model.Account;
import io.mwarzecha.model.Transfer;
import io.mwarzecha.util.Try;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
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
  public List<Transfer> getTransfersByAccountId(long accountId) {
    return jdbi.withHandle(handle -> handle
        .select("SELECT * FROM transfer WHERE from_account = ? OR to_account = ?", accountId,
            accountId)
        .map(TRANSFER_ROW_MAPPER)
        .list());
  }

  @Override
  public Optional<Transfer> getTransferByIdAndAccountId(long transferId, long accountId) {
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
  public Optional<Account> getAccountById(long accountId) {
    return jdbi.withHandle(handle -> handle
        .select("SELECT * FROM account WHERE id = ?", accountId)
        .map(ACCOUNT_ROW_MAPPER)
        .findFirst());
  }

  @Override
  public Account persistAccount(Account account) {
    long accountId = jdbi.withHandle(handle -> handle
        .createUpdate("INSERT INTO account (owner, currency, balance) VALUES (?, ?, ?)")
        .bind(0, account.getOwner())
        .bind(1, account.getCurrency())
        .bind(2, account.getBalance())
        .executeAndReturnGeneratedKeys()
        .mapTo(Long.class)
        .one());
    return account.withId(accountId);
  }

  @Override
  public Try<Transfer> makeTransfer(Transfer transfer) {
    return Try.ofFailable(() -> doMakeTransfer(transfer));
  }

  private Transfer doMakeTransfer(Transfer transfer) {
    return jdbi.inTransaction(TransactionIsolationLevel.READ_COMMITTED, handle -> {
      var amount = transfer.getAmount();
      var currency = transfer.getCurrency();
      debitAccount(handle, transfer.getFromAccountId(), amount, currency);
      creditAccount(handle, transfer.getToAccountId(), amount, currency);
      return persistTransfer(handle, transfer);
    });
  }

  private static void debitAccount(Handle handle, long accountId, BigDecimal amount,
      String currency) {
    try {
      doDebitAccount(handle, accountId, amount, currency);
    } catch (UnableToExecuteStatementException ex) {
      throw new IllegalStateException("Insufficient funds");
    }
  }

  private static void doDebitAccount(Handle handle, long accountId, BigDecimal amount,
      String currency) {
    int rowsUpdated = handle
        .execute("UPDATE account SET balance = balance - ? WHERE id = ? AND currency = ?",
            amount, accountId, currency);
    assertOne(rowsUpdated, accountNotFoundMessageSupplier(accountId, currency));
  }

  private static void assertOne(int value, Supplier<String> messageSupplier) {
    if (value != 1) {
      throw new IllegalStateException(messageSupplier.get());
    }
  }

  private static Supplier<String> accountNotFoundMessageSupplier(long accountId, String currency) {
    return () -> String.format("Account with id %d and currency %s not found", accountId, currency);
  }

  private static void creditAccount(Handle handle, long accountId, BigDecimal amount,
      String currency) {
    int rowsUpdated = handle
        .execute("UPDATE account SET balance = balance + ? WHERE id = ? AND currency = ?",
            amount, accountId, currency);
    assertOne(rowsUpdated, accountNotFoundMessageSupplier(accountId, currency));
  }

  private Transfer persistTransfer(Handle handle, Transfer transfer) {
    var timestamp = clock.instant();
    long transferId = handle
        .createUpdate("INSERT INTO transfer (from_account, to_account, currency, amount, timestamp) VALUES (?, ?, ?, ?, ?)")
        .bind(0, transfer.getFromAccountId())
        .bind(1, transfer.getToAccountId())
        .bind(2, transfer.getCurrency())
        .bind(3, transfer.getAmount())
        .bind(4, timestamp)
        .executeAndReturnGeneratedKeys()
        .mapTo(Long.class)
        .one();
    return transfer.withIdAndTimestamp(transferId, timestamp);
  }
}