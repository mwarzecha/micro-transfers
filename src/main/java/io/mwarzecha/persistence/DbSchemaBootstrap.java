package io.mwarzecha.persistence;

import java.util.function.Consumer;
import org.jdbi.v3.core.Handle;

class DbSchemaBootstrap implements Consumer<Handle> {

  @Override
  public void accept(Handle handle) {
    handle.execute("CREATE TABLE account ("
        + "id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY, "
        + "owner VARCHAR(50) NOT NULL, "
        + "currency CHAR(3) NOT NULL, "
        + "balance DECIMAL(19, 4) NOT NULL CHECK (balance>=0.0)"
        + ")");
    handle.execute("CREATE TABLE transfer ("
        + "id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY, "
        + "from_account BIGINT NOT NULL REFERENCES account(id), "
        + "to_account BIGINT NOT NULL REFERENCES account(id), "
        + "currency CHAR(3) NOT NULL, "
        + "amount DECIMAL(19, 4) NOT NULL, "
        + "timestamp TIMESTAMP WITH TIME ZONE NOT NULL"
        + ")");
  }
}
