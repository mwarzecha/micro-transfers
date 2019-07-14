package io.mwarzecha.rest;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mwarzecha.json.AccountSerde;
import io.mwarzecha.json.TransferSerde;
import io.mwarzecha.model.Account;
import io.mwarzecha.model.Transfer;
import io.mwarzecha.persistence.PersistenceService;
import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJson;

public class ServerRunner {

  private final Javalin app;

  public static ServerRunner create(PersistenceService persistenceService) {
    return new ServerRunner(persistenceService);
  }

  private ServerRunner(PersistenceService persistenceService) {
    this.app = configuredJavalin(persistenceService);
  }

  private static Javalin configuredJavalin(PersistenceService persistenceService) {
    var gson = gson();
    JavalinJson.setFromJsonMapper(gson::fromJson);
    JavalinJson.setToJsonMapper(gson::toJson);
    var app = Javalin.create();
    addRoutes(app, accountController(persistenceService), transferController(persistenceService));
    return app;
  }

  private static Gson gson() {
    return new GsonBuilder()
        .registerTypeAdapter(Account.class, new AccountSerde())
        .registerTypeAdapter(Transfer.class, new TransferSerde())
        .create();
  }

  private static AccountController accountController(PersistenceService persistenceService) {
    return new AccountController(persistenceService);
  }

  private static TransferController transferController(PersistenceService persistenceService) {
    return new TransferController(persistenceService);
  }

  private static void addRoutes(Javalin app, AccountController accountController,
      TransferController transferController) {
    app.routes(() ->
        path("api", () -> {
          path("accounts", () -> {
            get(accountController::getAllAccounts);
            post(accountController::createAccount);
            path(":id", () -> {
              get(accountController::getAccountById);
              path("transfers", () -> {
                get(transferController::getAllAccountTransfers);
                path(":transferId", () -> get(transferController::getAccountTransferById));
              });
            });
          });
          path("transfers", () -> post(transferController::makeTransfer));
        })
    );
  }

  public ServerRunner start(int port) {
    app.start(port);
    return this;
  }

  public ServerRunner addShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
    return this;
  }

  public void stop() {
    app.stop();
  }
}