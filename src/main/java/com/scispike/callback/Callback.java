package com.scispike.callback;

public abstract class Callback<E,S> {
  public abstract void call(E error, @SuppressWarnings("unchecked") S... args);
}