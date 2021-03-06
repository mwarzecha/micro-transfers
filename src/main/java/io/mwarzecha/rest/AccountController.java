package io.mwarzecha.rest;

import io.mwarzecha.model.Account;
import io.mwarzecha.persistence.PersistenceService;
import io.javalin.http.Context;
import java.math.BigDecimal;

class AccountController {

  private final PersistenceService persistenceService;

  AccountController(PersistenceService persistenceService) {
    this.persistenceService = persistenceService;
  }

  void getAllAccounts(Context ctx) {
    ctx.json(persistenceService.getAllAccounts());
  }

  void getAccountById(Context ctx) {
    persistenceService
        .getAccountById(
            ctx.pathParam("accountId", Long.class).get())
        .ifPresentOrElse(ctx::json,
            () -> ctx.status(404).result("Account not found"));
  }

  void createAccount(Context ctx) {
    var createdAccount = persistenceService.persistAccount(
        ctx.bodyValidator(Account.class)
            .check(account -> account.getOwner().length() <= 50,
                "Owner name characters limit exceeded")
            .check(account -> account.getBalance().compareTo(BigDecimal.ZERO) >= 0,
                "Negative account balance")
            .get());
    ctx.status(201).json(createdAccount);
  }
}