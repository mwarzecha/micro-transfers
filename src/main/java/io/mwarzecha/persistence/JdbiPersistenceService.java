package io.mwarzecha.persistence;

import io.mwarzecha.model.Account;
import io.mwarzecha.model.Transfer;
import io.mwarzecha.util.Try;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

class JdbiPersistenceService implements PersistenceService {

  private final Jdbi jdbi;
  private final Clock clock;

  JdbiPersistenceService(Jdbi jdbi, Clock clock) {
    this.jdbi = jdbi;
    this.clock = clock;
  }

  @Override
  public List<Transfer> getTransfersByAccountId(long accountId) {
    return jdbi.withHandle(handle -> TransferDao.withHandle(handle)
        .getTransfersByAccountId(accountId));
  }

  @Override
  public Optional<Transfer> getTransferByIdAndAccountId(long transferId, long accountId) {
    return jdbi.withHandle(handle -> TransferDao.withHandle(handle)
        .getTransferByIdAndAccountId(transferId, accountId));
  }

  @Override
  public List<Account> getAllAccounts() {
    return jdbi.withHandle(handle -> AccountDao.withHandle(handle)
        .getAllAccounts());
  }

  @Override
  public Optional<Account> getAccountById(long accountId) {
    return jdbi.withHandle(handle -> AccountDao.withHandle(handle)
        .getAccountById(accountId));
  }

  @Override
  public Account persistAccount(Account account) {
    return jdbi.withHandle(handle -> AccountDao.withHandle(handle)
        .persistAccount(account));
  }

  @Override
  public Try<Transfer> makeTransfer(Transfer transfer) {
    return Try.ofFailable(() -> doMakeTransfer(transfer));
  }

  private Transfer doMakeTransfer(Transfer transfer) {
    return jdbi.inTransaction(TransactionIsolationLevel.READ_COMMITTED, handle -> {
      updateBalances(handle, transfer);
      return TransferDao.withHandle(handle)
          .persistTransferWithTimestamp(transfer, clock.instant());
    });
  }

  private void updateBalances(Handle handle, Transfer transfer) {
    var amount = transfer.getAmount();
    var currency = transfer.getCurrency();
    var from = transfer.getFromAccountId();
    var to = transfer.getToAccountId();
    if (to > from) {
      AccountDao.withHandle(handle)
          .debitAccount(from, amount, currency)
          .creditAccount(to, amount, currency);
    } else {
      AccountDao.withHandle(handle)
          .creditAccount(to, amount, currency)
          .debitAccount(from, amount, currency);
    }
  }
}