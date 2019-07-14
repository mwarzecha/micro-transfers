package io.mwarzecha.rest;

import io.mwarzecha.model.Transfer;
import io.mwarzecha.persistence.PersistenceService;
import io.javalin.http.Context;
import java.math.BigDecimal;

class TransferController {

  private final PersistenceService persistenceService;

  TransferController(PersistenceService persistenceService) {
    this.persistenceService = persistenceService;
  }

  void getAllAccountTransfers(Context ctx) {
    var transfers = persistenceService.findAllAccountTransfers(
        ctx.pathParam("id", Long.class).get());
    ctx.json(transfers);
  }

  void getAccountTransferById(Context ctx) {
    persistenceService
        .findAccountTransferById(
            ctx.pathParam("id", Long.class).get(),
            ctx.pathParam("transferId", Long.class).get())
        .ifPresentOrElse(ctx::json, () -> ctx.status(404).result("Transfer not found"));
  }

  void makeTransfer(Context ctx) {
    persistenceService
        .makeTransfer(
            ctx.bodyValidator(Transfer.class)
                .check(transfer -> transfer.getAmount().compareTo(BigDecimal.ZERO) > 0,
                    "Transfer amount must be greater than 0")
                .check(transfer -> transfer.getFromAccountId() != transfer.getToAccountId(),
                    "Cannot transfer to the same account")
                .get())
        .ifSuccessOrElse(transfer -> ctx.status(201).json(transfer),
            throwable -> ctx.status(400).result(throwable.getMessage()));
  }
}
