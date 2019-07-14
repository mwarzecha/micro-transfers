package io.mwarzecha;

import io.mwarzecha.persistence.Persistence;
import io.mwarzecha.rest.ServerRunner;

public class Application {

  public static void main(String[] args) {
    ServerRunner
        .create(Persistence.persistenceService())
        .addShutdownHook()
        .start(8080);
  }
}