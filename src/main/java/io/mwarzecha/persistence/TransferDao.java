package io.mwarzecha.persistence;

import io.mwarzecha.model.Transfer;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.RowMapper;

class TransferDao {

  private static final RowMapper<Transfer> TRANSFER_ROW_MAPPER = new TransferRowMapper();

  private final Handle handle;

  static TransferDao withHandle(Handle handle) {
    return new TransferDao(handle);
  }

  private TransferDao(Handle handle) {
    this.handle = handle;
  }

  List<Transfer> getTransfersByAccountId(long accountId) {
    return handle
        .select("SELECT * FROM transfer WHERE from_account = ? OR to_account = ?", accountId,
            accountId)
        .map(TRANSFER_ROW_MAPPER)
        .list();
  }

  Optional<Transfer> getTransferByIdAndAccountId(long transferId, long accountId) {
    return handle
        .select("SELECT * FROM transfer WHERE id = ? AND (from_account = ? OR to_account = ?)",
            transferId, accountId, accountId)
        .map(TRANSFER_ROW_MAPPER)
        .findFirst();
  }

  Transfer persistTransferWithTimestamp(Transfer transfer, Instant timestamp) {
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
