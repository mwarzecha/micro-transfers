package io.mwarzecha.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TryTest {

  @Mock
  private Consumer<Object> action;

  @Test
  void testFailure() {
    var ex = new Exception();

    Try<Object> tryObject = Try.ofFailable(() -> {
      throw ex;
    });

    assertTrue(tryObject.isFailure());
    assertFalse(tryObject.isSuccess());
    assertEquals(ex, tryObject.getThrowable());
    assertThrows(NoSuchElementException.class, tryObject::getResult);
  }

  @Test
  void testSuccess() {
    var object = new Object();

    Try<Object> tryObject = Try.ofFailable(() -> object);

    assertTrue(tryObject.isSuccess());
    assertFalse(tryObject.isFailure());
    assertEquals(object, tryObject.getResult());
    assertThrows(NoSuchElementException.class, tryObject::getThrowable);
  }

  @Test
  void testIfSuccessOrElseSuccess() {
    var object = new Object();

    Try<Object> tryObject = Try.ofFailable(() -> object);
    tryObject.ifSuccessOrElse(action, throwable -> fail());

    assertTrue(tryObject.isSuccess());
    verify(action).accept(object);
  }

  @Test
  void testIfSuccessOrElseFailure() {
    var ex = new Exception();

    Try<Object> tryObject = Try.ofFailable(() -> {
      throw ex;
    });
    tryObject.ifSuccessOrElse(object -> fail(), action);

    assertTrue(tryObject.isFailure());
    verify(action).accept(ex);
  }
}
