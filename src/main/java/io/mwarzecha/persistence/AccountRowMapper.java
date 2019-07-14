package io.mwarzecha.persistence;

import io.mwarzecha.model.Account;
import io.mwarzecha.model.Money;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

class AccountRowMapper implements RowMapper<Account> {

  @Override
  public Account map(ResultSet rs, StatementContext ctx) throws SQLException {
    return Account.newBuilder()
        .id(rs.getLong("id"))
        .owner(rs.getString("owner"))
        .balance(Money.of(rs.getString("currency"),
            rs.getBigDecimal("balance")))
        .build();
  }
}