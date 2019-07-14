package io.mwarzecha.persistence;

import java.sql.SQLException;
import java.time.Clock;
import java.util.function.Consumer;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.h2.H2DatabasePlugin;

public class Persistence {

  public static PersistenceService persistenceService() {
    return new JdbiPersistenceService(configuredJdbi(), clock());
  }

  private static Jdbi configuredJdbi() {
    var jdbi = Jdbi.create(basicDataSource());
    jdbi.installPlugin(new H2DatabasePlugin());
    var dbSchemaBootstrap = dbSchemaBootstrap();
    jdbi.useHandle(dbSchemaBootstrap::accept);
    return jdbi;
  }

  private static DataSource basicDataSource() {
    var ds = new BasicDataSource();
    ds.setDriverClassName("org.h2.Driver");
    ds.setUrl("jdbc:h2:mem:appDB;DB_CLOSE_DELAY=-1");
    ds.setUsername("sa");
    ds.setPassword("");
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        ds.close();
      } catch (SQLException e) {
        //ignored
      }
    }));
    return ds;
  }

  private static Consumer<Handle> dbSchemaBootstrap() {
    return new DbSchemaBootstrap();
  }

  private static Clock clock() {
    return Clock.systemUTC();
  }
}
