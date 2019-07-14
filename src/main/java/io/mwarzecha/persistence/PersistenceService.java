package io.mwarzecha.persistence;

import io.mwarzecha.model.Account;
import io.mwarzecha.model.Transfer;
import io.mwarzecha.util.Try;
import java.util.List;
import java.util.Optional;

public interface PersistenceService {

  List<Transfer> findAllAccountTransfers(long accountId);

  Optional<Transfer> findAccountTransferById(long accountId, long transferId);

  List<Account> getAllAccounts();

  Optional<Account> findAccountById(long accountId);

  Account persistAccount(Account account);

  Try<Transfer> makeTransfer(Transfer transfer);
}
