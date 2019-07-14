package io.mwarzecha.rest;

import static java.net.http.HttpClient.newHttpClient;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.mwarzecha.model.Account;
import io.mwarzecha.model.Transfer;
import io.mwarzecha.model.Money;
import io.mwarzecha.persistence.Persistence;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
class ServerRunnerIntegrationTest {

  private static final int PORT = 7001;
  private static final String BASE_PATH = "http://localhost:" + PORT + "/api";
  private static final Gson GSON = GsonFactory.create();
  private static Type ACCOUNT_LIST_TYPE = new TypeToken<List<Account>>(){}.getType();
  private static Type TRANSFER_LIST_TYPE = new TypeToken<List<Transfer>>(){}.getType();

  private static ServerRunner serverRunner;
  private static HttpClient httpClient;

  @BeforeAll
  static void setUpClass() {
    serverRunner = ServerRunner
        .create(Persistence.persistenceService())
        .start(PORT);
    httpClient = newHttpClient();
  }

  @AfterAll
  static void tearDownClass() {
    serverRunner.stop();
  }

  @Test
  @Order(1)
  void testGetAllAccountsWhenEmpty() throws IOException, InterruptedException {
    assertGetAllAccountsSize(0);
  }

  private static void assertGetAllAccountsSize(int expectedSize)
      throws IOException, InterruptedException {
    var httpResponse = get("/accounts");
    List<Account> accounts = GSON.fromJson(httpResponse.body(), ACCOUNT_LIST_TYPE);

    assertEquals(200, httpResponse.statusCode());
    assertEquals(expectedSize, accounts.size());
  }

  private static HttpResponse<String> get(String path) throws IOException, InterruptedException {
    var httpRequest = HttpRequest.newBuilder()
        .uri(URI.create(BASE_PATH + path))
        .GET()
        .build();
    return httpClient.send(httpRequest, BodyHandlers.ofString());
  }

  @Test
  @Order(2)
  void testCreateAccountBadRequests() throws IOException, InterruptedException {
    assertPostBadRequest("/accounts", "{}");
    assertPostBadRequest("/accounts",
        "{\"currency\":\"wrong\",\"owner\":\"Joe\",\"balance\":\"0.00\"}");
    assertPostBadRequest("/accounts",
        "{\"currency\":\"USD\",\"owner\":\"Joe\"}");
    assertPostBadRequest("/accounts",
        "{\"currency\":\"USD\",\"balance\":\"0.00\"}");
    assertPostBadRequest("/accounts",
        "{\"owner\":\"Joe\",\"balance\":\"0.00\"}");
    assertGetAllAccountsSize(0);
  }

  private static void assertPostBadRequest(String path, String body)
      throws IOException, InterruptedException {
    assertEquals(400, post(path, body).statusCode());
  }

  private static HttpResponse<String> post(String path, String body)
      throws IOException, InterruptedException {
    var request = HttpRequest.newBuilder()
        .uri(URI.create(BASE_PATH + path))
        .POST(BodyPublishers.ofString(body))
        .build();
    return httpClient.send(request, BodyHandlers.ofString());
  }

  @Test
  @Order(3)
  void testCreateAccounts() throws IOException, InterruptedException {
    assertCreateAccountWithExpectedId(1L,
        newAccount("Joe", "USD", new BigDecimal("100.00")));
    assertCreateAccountWithExpectedId(2L,
        newAccount("Mike", "USD", new BigDecimal("50.21")));
    assertCreateAccountWithExpectedId(3L,
        newAccount("Steve", "EUR", new BigDecimal("89.11")));
    assertCreateAccountWithExpectedId(4L,
        newAccount("Bob", "EUR", new BigDecimal("124.60")));
    assertGetAllAccountsSize(4);
  }

  private static void assertCreateAccountWithExpectedId(long expectedId, Account account)
      throws IOException, InterruptedException {
    var httpResponse = post("/accounts", GSON.toJson(account));
    var resultAccount = GSON.fromJson(httpResponse.body(), Account.class);

    assertEquals(201, httpResponse.statusCode());
    assertEqualsExceptTimestampAndId(account, resultAccount);
    assertEquals(expectedId, resultAccount.getId());
  }

  private static void assertEqualsExceptTimestampAndId(Account expected, Account actual) {
    assertEquals(expected.getOwner(), actual.getOwner());
    assertEquals(expected.getCurrency(), actual.getCurrency());
    assertEquals(expected.getBalance(), actual.getBalance());
  }

  private static Account newAccount(String owner, String currency, BigDecimal balance) {
    return Account.newBuilder()
        .owner(owner)
        .balance(Money.of(currency, balance))
        .build();
  }

  @Test
  @Order(4)
  void testGetAccount() throws IOException, InterruptedException {
    var httpResponse = get("/accounts/4");
    var account = GSON.fromJson(httpResponse.body(), Account.class);

    assertEquals(200, httpResponse.statusCode());
    assertEquals("Bob", account.getOwner());
    assertEquals("EUR", account.getCurrency());
    assertEquals(4L, account.getId());
    assertEquals(new BigDecimal("124.60"), account.getBalance());
  }

  @Test
  @Order(5)
  void testMakeTransferBadRequests() throws IOException, InterruptedException {
    assertPostBadRequest("/transfers", "{}");
    assertPostBadRequest("/transfers",
        "{\"from_account\":1,\"to_account\":1,\"balance\":\"1.00\",\"currency\":\"USD\"}");
    assertPostBadRequest("/transfers",
        "{\"from_account\":1,\"to_account\":2,\"balance\":\"1000000.00\",\"currency\":\"USD\"}");
    assertPostBadRequest("/transfers",
        "{\"from_account\":1,\"to_account\":2,\"balance\":\"0.00\",\"currency\":\"USD\"}");
    assertPostBadRequest("/transfers",
        "{\"from_account\":1,\"to_account\":2,\"balance\":\"-1.00\",\"currency\":\"USD\"}");
    assertPostBadRequest("/transfers",
        "{\"from_account\":1,\"to_account\":2,\"balance\":\"1.00\",\"currency\":\"EUR\"}");
    assertPostBadRequest("/transfers",
        "{\"from_account\":1,\"to_account\":3,\"balance\":\"1.00\",\"currency\":\"USD\"}");
  }

  @Test
  @Order(6)
  void testUnchangedBalances() throws IOException, InterruptedException {
    Map<Long, Account> accountsById = getAllAccountsMappedById();

    assertEquals(new BigDecimal("100.00"), accountsById.get(1L).getBalance());
    assertEquals(new BigDecimal("50.21"), accountsById.get(2L).getBalance());
    assertEquals(new BigDecimal("89.11"), accountsById.get(3L).getBalance());
    assertEquals(new BigDecimal("124.60"), accountsById.get(4L).getBalance());
  }

  private static Map<Long, Account> getAllAccountsMappedById()
      throws IOException, InterruptedException {
    var httpResponse = get("/accounts");
    List<Account> accounts = GSON.fromJson(httpResponse.body(), ACCOUNT_LIST_TYPE);
    return accounts.stream()
        .collect(toMap(Account::getId, a -> a));
  }

  @Test
  @Order(7)
  void testGetAccountTransfersEmpty() throws IOException, InterruptedException {
    assertGetAllAccountTransfersSize(1L, 0);
    assertGetAllAccountTransfersSize(2L, 0);
    assertGetAllAccountTransfersSize(3L, 0);
    assertGetAllAccountTransfersSize(4L, 0);
  }

  private static void assertGetAllAccountTransfersSize(long accountId, int expectedSize)
      throws IOException, InterruptedException {
    var httpResponse = get("/accounts/" + accountId + "/transfers");
    List<Transfer> transfers = GSON.fromJson(httpResponse.body(), TRANSFER_LIST_TYPE);

    assertEquals(200, httpResponse.statusCode());
    assertEquals(expectedSize, transfers.size());
  }

  @Test
  @Order(8)
  void testMakeTransfers() throws IOException, InterruptedException {
    assertTransferMadeWithExpectedId(1L,
        newTransfer(1L, 2L, "USD", new BigDecimal("1.10")));
    assertTransferMadeWithExpectedId(2L,
        newTransfer(2L, 1L, "USD", new BigDecimal("5.60")));
    assertTransferMadeWithExpectedId(3L,
        newTransfer(3L, 4L, "EUR", new BigDecimal("10.29")));
  }

  private static void assertTransferMadeWithExpectedId(long expectedId, Transfer transfer)
      throws IOException, InterruptedException {
    var httpResponse = post("/transfers", GSON.toJson(transfer));
    var resultTransfer = GSON.fromJson(httpResponse.body(), Transfer.class);

    assertEquals(201, httpResponse.statusCode());
    assertEqualsExceptTimestampAndId(transfer, resultTransfer);
    assertEquals(expectedId, resultTransfer.getId());
  }

  private static void assertEqualsExceptTimestampAndId(Transfer expected, Transfer actual) {
    assertEquals(expected.getFromAccountId(), actual.getFromAccountId());
    assertEquals(expected.getToAccountId(), actual.getToAccountId());
    assertEquals(expected.getCurrency(), actual.getCurrency());
    assertEquals(expected.getAmount(), actual.getAmount());
  }

  private static Transfer newTransfer(long fromAccount, long toAccount, String currency,
      BigDecimal amount) {
    return Transfer.newBuilder()
        .fromAccountId(fromAccount)
        .toAccountId(toAccount)
        .amount(Money.of(currency, amount))
        .build();
  }

  @Test
  @Order(9)
  void testUpdatedBalances() throws IOException, InterruptedException {
    Map<Long, Account> accountsById = getAllAccountsMappedById();

    assertEquals(new BigDecimal("104.50"), accountsById.get(1L).getBalance());
    assertEquals(new BigDecimal("45.71"), accountsById.get(2L).getBalance());
    assertEquals(new BigDecimal("78.82"), accountsById.get(3L).getBalance());
    assertEquals(new BigDecimal("134.89"), accountsById.get(4L).getBalance());
  }

  @Test
  @Order(10)
  void testGetAccountTransfers() throws IOException, InterruptedException {
    assertGetAllAccountTransfersSize(1L, 2);
    assertGetAllAccountTransfersSize(2L, 2);
    assertGetAllAccountTransfersSize(3L, 1);
    assertGetAllAccountTransfersSize(4L, 1);
  }

  @Test
  @Order(11)
  void testGetAccountTransferById() throws IOException, InterruptedException {
    var httpResponse = get("/accounts/1/transfers/1");
    var transfer = GSON.fromJson(httpResponse.body(), Transfer.class);

    assertEquals(200, httpResponse.statusCode());
    assertEquals(1L, transfer.getId());
    assertEquals(1L, transfer.getFromAccountId());
    assertEquals(2L, transfer.getToAccountId());
    assertEquals("USD", transfer.getCurrency());
    assertEquals(new BigDecimal("1.10"), transfer.getAmount());
  }

  @Test
  @Order(12)
  void testNotFound() throws IOException, InterruptedException {
    assertNotFound("/accounts/5");
    assertNotFound("/accounts/1/transfers/3");
    assertNotFound("/accounts/3/transfers/1");
    assertNotFound("/accounts/3/transfers/5");
    assertNotFound("/accounts/6/transfers/10");
  }

  private static void assertNotFound(String path) throws IOException, InterruptedException {
    var response = get(path);
    assertEquals(404, response.statusCode());
  }
}