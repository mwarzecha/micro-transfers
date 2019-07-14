package io.mwarzecha.persistence;

import io.mwarzecha.model.Transfer;
import io.mwarzecha.model.Money;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

class TransferRowMapper implements RowMapper<Transfer> {

  @Override
  public Transfer map(ResultSet rs, StatementContext ctx) throws SQLException {
    return Transfer.newBuilder()
        .id(rs.getLong("id"))
        .fromAccountId(rs.getLong("from_account"))
        .toAccountId(rs.getLong("to_account"))
        .amount(Money.of(rs.getString("currency"),
            rs.getBigDecimal("amount")))
        .timestamp(rs.getTimestamp("timestamp").toInstant())
        .build();
  }
}