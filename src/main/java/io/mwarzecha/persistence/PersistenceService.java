package io.mwarzecha.persistence;

import io.mwarzecha.model.Account;
import io.mwarzecha.model.Transfer;
import io.mwarzecha.util.Try;
import java.util.List;
import java.util.Optional;

public interface PersistenceService {

  List<Transfer> getTransfersByAccountId(long accountId);

  Optional<Transfer> getTransferByIdAndAccountId(long transferId, long accountId);

  List<Account> getAllAccounts();

  Optional<Account> getAccountById(long accountId);

  Account persistAccount(Account account);

  Try<Transfer> makeTransfer(Transfer transfer);
}