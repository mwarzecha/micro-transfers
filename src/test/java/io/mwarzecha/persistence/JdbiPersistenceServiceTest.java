package io.mwarzecha.persistence;

import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.mwarzecha.model.Account;
import io.mwarzecha.model.Transfer;
import io.mwarzecha.model.Money;
import io.mwarzecha.util.Try;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMappers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JdbiPersistenceServiceTest {

  private static final RowMapper<Account> ACCOUNT_ROW_MAPPER = new AccountRowMapper();
  private static final RowMapper<Transfer> TRANSFER_ROW_MAPPER = new TransferRowMapper();
  private static final DbSchemaBootstrap SCHEMA_BOOTSTRAP = new DbSchemaBootstrap();
  private static final Instant TIMESTAMP = Instant.ofEpochMilli(123123123L);

  private static Jdbi jdbi;

  @Mock
  private Clock clock;
  private JdbiPersistenceService persistenceService;

  @BeforeAll
  static void setUpClass() {
    jdbi = Jdbi.create("jdbc:h2:mem:testDB;DB_CLOSE_DELAY=-1", "sa", "");
    jdbi.installPlugin(new H2DatabasePlugin());
    jdbi.configure(RowMappers.class, rm -> {
      rm.register(ACCOUNT_ROW_MAPPER);
      rm.register(TRANSFER_ROW_MAPPER);
    });
  }

  @BeforeEach
  void setUp() {
    jdbi.useHandle(SCHEMA_BOOTSTRAP::accept);
    persistenceService = new JdbiPersistenceService(jdbi, clock);
  }

  @AfterEach
  void tearDown() {
    jdbi.useHandle(handle -> handle.execute("DROP ALL OBJECTS"));
  }

  @Test
  void testPersistAccount() {
    Account result = persistenceService.persistAccount(Account.newBuilder()
        .balance(Money.zeroOf("USD"))
        .owner("Mike")
        .build());

    List<Account> accounts = selectFromAccount();
    var account = accounts.get(0);

    assertEquals(1, accounts.size());
    assertEquals("USD", account.getCurrency());
    assertEquals("Mike", account.getOwner());
    assertEquals(new BigDecimal("0.00"), account.getBalance());
    assertAccountEquals(account, result);
  }

  private static List<Account> selectFromAccount() {
    return jdbi.withHandle(handle -> handle
        .select("SELECT * FROM account")
        .map(ACCOUNT_ROW_MAPPER)
        .list());
  }

  private static void assertAccountEquals(Account expected, Account actual) {
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getBalance(), actual.getBalance());
    assertEquals(expected.getCurrency(), actual.getCurrency());
    assertEquals(expected.getOwner(), actual.getOwner());
  }

  @Test
  void testFindAccountById() {
    jdbi.useHandle(handle -> insertIntoAccount(handle, 123L, "Mike", "USD",
        new BigDecimal("1.20")));

    var account = persistenceService.findAccountById(123L)
        .orElseThrow();

    assertEquals(123L, account.getId());
    assertEquals("USD", account.getCurrency());
    assertEquals("Mike", account.getOwner());
    assertEquals(new BigDecimal("1.20"), account.getBalance());
  }

  private static void insertIntoAccount(Handle handle, long id, String owner, String currency,
      BigDecimal balance) {
    handle.execute("INSERT INTO account (id, owner, currency, balance) VALUES (?, ?, ?, ?)",
        id, owner, currency, balance);
  }

  @Test
  void testFindNonExistentAccountById() {
    Optional<Account> result = persistenceService.findAccountById(1L);

    assertTrue(result.isEmpty());
  }

  @Test
  void testGetAllAccounts() {
    jdbi.useTransaction(handle -> {
      insertIntoAccount(handle, 123L, "Joe", "USD", new BigDecimal("1.20"));
      insertIntoAccount(handle, 245L, "Steve", "EUR", new BigDecimal("3.10"));
    });

    List<Account> accounts = persistenceService.getAllAccounts();

    assertEquals(2, accounts.size());
  }

  @Test
  void testGetAllAccountsWhenEmpty() {
    List<Account> accounts = persistenceService.getAllAccounts();

    assertTrue(accounts.isEmpty());
  }

  @Test
  void testMakeTransfer() {
    when(clock.instant()).thenReturn(TIMESTAMP);
    jdbi.useTransaction(handle -> {
      insertIntoAccount(handle, 1L, "Joe", "USD", new BigDecimal("100.21"));
      insertIntoAccount(handle, 2L, "Steve", "USD", new BigDecimal("35.17"));
    });

    Try<Transfer> result = persistenceService.makeTransfer(Transfer.newBuilder()
        .fromAccountId(1L)
        .toAccountId(2L)
        .amount(Money.of("USD", new BigDecimal("10.12")))
        .build());

    Map<Long, Account> accountsById = selectFromAccountMappedById();
    var joeAccount = accountsById.get(1L);
    var steveAccount = accountsById.get(2L);
    List<Transfer> transfers = selectFromTransfer();
    var transfer = transfers.get(0);

    assertTrue(result.isSuccess());
    assertEquals(1, transfers.size());
    assertEquals(new BigDecimal("90.09"), joeAccount.getBalance());
    assertEquals(new BigDecimal("45.29"), steveAccount.getBalance());
    assertEquals(1L, transfer.getFromAccountId());
    assertEquals(2L, transfer.getToAccountId());
    assertEquals("USD", transfer.getCurrency());
    assertEquals(new BigDecimal("10.12"), transfer.getAmount());
    assertEquals(TIMESTAMP, transfer.getTimestamp());
    assertTransferEquals(transfer, result.getResult());
  }

  private static Map<Long, Account> selectFromAccountMappedById() {
    return selectFromAccount()
        .stream()
        .collect(toMap(Account::getId, a -> a));
  }

  private static List<Transfer> selectFromTransfer() {
    return jdbi.withHandle(handle -> handle
        .select("SELECT * FROM transfer")
        .map(TRANSFER_ROW_MAPPER)
        .list());
  }

  private static void assertTransferEquals(Transfer expected, Transfer actual) {
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getFromAccountId(), actual.getFromAccountId());
    assertEquals(expected.getToAccountId(), actual.getToAccountId());
    assertEquals(expected.getAmount(), actual.getAmount());
    assertEquals(expected.getCurrency(), actual.getCurrency());
    assertEquals(expected.getTimestamp(), actual.getTimestamp());
  }

  @Test
  void testMakeTransferWithInvalidCurrency() {
    jdbi.useTransaction(handle -> {
      insertIntoAccount(handle, 1L, "Joe", "USD", new BigDecimal("100.21"));
      insertIntoAccount(handle, 2L, "Steve", "USD", new BigDecimal("35.17"));
    });

    Try<Transfer> result = persistenceService.makeTransfer(Transfer.newBuilder()
        .fromAccountId(1L)
        .toAccountId(2L)
        .amount(Money.of("EUR", new BigDecimal("10.12")))
        .build());

    Map<Long, Account> accountsById = selectFromAccountMappedById();
    var joeAccount = accountsById.get(1L);
    var steveAccount = accountsById.get(2L);
    List<Transfer> transfers = selectFromTransfer();

    assertTrue(result.isFailure());
    assertTrue(transfers.isEmpty());
    assertEquals(new BigDecimal("100.21"), joeAccount.getBalance());
    assertEquals(new BigDecimal("35.17"), steveAccount.getBalance());
  }

  @Test
  void testMakeTransferWithInsufficientFunds() {
    jdbi.useTransaction(handle -> {
      insertIntoAccount(handle, 1L, "Joe", "USD", new BigDecimal("1.21"));
      insertIntoAccount(handle, 2L, "Steve", "USD", new BigDecimal("35.17"));
    });

    Try<Transfer> result = persistenceService.makeTransfer(Transfer.newBuilder()
        .fromAccountId(1L)
        .toAccountId(2L)
        .amount(Money.of("USD", new BigDecimal("10.12")))
        .build());

    Map<Long, Account> accountsById = selectFromAccountMappedById();
    var joeAccount = accountsById.get(1L);
    var steveAccount = accountsById.get(2L);
    List<Transfer> transfers = selectFromTransfer();

    assertTrue(result.isFailure());
    assertTrue(transfers.isEmpty());
    assertEquals(new BigDecimal("1.21"), joeAccount.getBalance());
    assertEquals(new BigDecimal("35.17"), steveAccount.getBalance());
  }

  @Test
  void testMakeTransferToNonExistentAccount() {
    jdbi.useHandle(handle -> insertIntoAccount(handle, 1L, "Joe", "USD",
        new BigDecimal("100.21")));

    Try<Transfer> result = persistenceService.makeTransfer(Transfer.newBuilder()
        .fromAccountId(1L)
        .toAccountId(2L)
        .amount(Money.of("USD", new BigDecimal("10.12")))
        .build());

    var joeAccount = selectFromAccount().get(0);
    List<Transfer> transfers = selectFromTransfer();

    assertTrue(result.isFailure());
    assertTrue(transfers.isEmpty());
    assertEquals(new BigDecimal("100.21"), joeAccount.getBalance());
  }

  @Test
  void testFindAllAccountTransfers() {
    jdbi.useTransaction(handle -> {
      insertIntoAccount(handle, 1L, "Joe", "USD", new BigDecimal("100.21"));
      insertIntoAccount(handle, 2L, "Steve", "USD", new BigDecimal("35.17"));
      insertIntoAccount(handle, 3L, "John", "USD", new BigDecimal("45.18"));
      insertIntoAccount(handle, 4L, "Dan", "EUR", new BigDecimal("80.02"));
      insertIntoAccount(handle, 5L, "Mike", "EUR", new BigDecimal("150.11"));
      insertIntoTransfer(handle, 1L, 1L, 2L, "USD", new BigDecimal("10.00"));
      insertIntoTransfer(handle, 2L, 2L, 3L, "USD", new BigDecimal("15.00"));
      insertIntoTransfer(handle, 3L, 4L, 5L, "EUR", new BigDecimal("30.00"));
    });

    List<Transfer> steveTransfers = persistenceService.findAllAccountTransfers(2L);

    assertEquals(2, steveTransfers.size());
  }

  @Test
  void testFindAccountTransferById() {
    jdbi.useTransaction(handle -> {
      insertIntoAccount(handle, 1L, "Joe", "USD", new BigDecimal("100.21"));
      insertIntoAccount(handle, 2L, "Steve", "USD", new BigDecimal("35.17"));
      insertIntoTransfer(handle, 1L, 1L, 2L, "USD", new BigDecimal("5.00"));
    });

    var joeTransfer = persistenceService.findAccountTransferById(1L, 1L)
        .orElseThrow();

    assertEquals(1L, joeTransfer.getId());
    assertEquals(1L, joeTransfer.getFromAccountId());
    assertEquals(2L, joeTransfer.getToAccountId());
    assertEquals("USD", joeTransfer.getCurrency());
    assertEquals(new BigDecimal("5.00"), joeTransfer.getAmount());
    assertEquals(TIMESTAMP, joeTransfer.getTimestamp());
  }

  @Test
  void testFindNonExistentAccountTransferById() {
    jdbi.useTransaction(handle -> {
      insertIntoAccount(handle, 1L, "Joe", "USD", new BigDecimal("100.21"));
      insertIntoAccount(handle, 2L, "Steve", "USD", new BigDecimal("35.17"));
    });

    Optional<Transfer> joeTransfer = persistenceService
        .findAccountTransferById(1L, 1L);

    assertTrue(joeTransfer.isEmpty());
  }

  @Test
  void testFindAccountTransferByIdForWrongAccount() {
    jdbi.useTransaction(handle -> {
      insertIntoAccount(handle, 1L, "Joe", "USD", new BigDecimal("100.21"));
      insertIntoAccount(handle, 2L, "Steve", "USD", new BigDecimal("35.17"));
      insertIntoAccount(handle, 3L, "John", "USD", new BigDecimal("45.18"));
      insertIntoTransfer(handle, 1L, 2L, 3L, "USD", new BigDecimal("5.00"));
      insertIntoTransfer(handle, 2L, 2L, 1L, "USD", new BigDecimal("2.00"));
    });

    Optional<Transfer> joeTransfer = persistenceService
        .findAccountTransferById(1L, 1L);

    assertTrue(joeTransfer.isEmpty());
  }

  private static void insertIntoTransfer(Handle handle, long id, long from, long to,
      String currency, BigDecimal amount) {
    handle.execute("INSERT INTO transfer (id, from_account, to_account, currency, amount, timestamp) VALUES (?, ?, ?, ?, ?, ?)",
        id, from, to, currency, amount, JdbiPersistenceServiceTest.TIMESTAMP);
  }
}