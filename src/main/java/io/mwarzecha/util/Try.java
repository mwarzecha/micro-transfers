package io.mwarzecha.util;

import static java.util.Objects.requireNonNull;

import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public abstract class Try<T> {

  private final T result;
  private final Throwable throwable;

  private Try(T result, Throwable throwable) {
    this.result = result;
    this.throwable = throwable;
  }

  public static <T> Try<T> ofFailable(Callable<T> action) {
    try {
      return success(action.call());
    } catch (Throwable t) {
      return failure(t);
    }
  }

  private static <T> Try<T> success(T value) {
    return new Success<>(value);
  }

  private static <T> Try<T> failure(Throwable throwable) {
    return new Failure<>(throwable);
  }

  public abstract boolean isSuccess();

  public abstract boolean isFailure();

  public T getResult() {
    if (result == null) {
      throw new NoSuchElementException("No result present");
    }
    return result;
  }

  public Throwable getThrowable() {
    if (throwable == null) {
      throw new NoSuchElementException("No throwable present");
    }
    return throwable;
  }

  public void ifSuccessOrElse(Consumer<? super T> action,
      Consumer<? super Throwable> failureAction) {
    if (isSuccess()) {
      action.accept(result);
    } else {
      failureAction.accept(throwable);
    }
  }

  private static class Success<T> extends Try<T> {

    private Success(T value) {
      super(requireNonNull(value), null);
    }

    @Override
    public boolean isSuccess() {
      return true;
    }

    @Override
    public boolean isFailure() {
      return false;
    }
  }

  private static class Failure<T> extends Try<T> {

    private Failure(Throwable throwable) {
      super(null, requireNonNull(throwable));
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public boolean isFailure() {
      return true;
    }
  }
}