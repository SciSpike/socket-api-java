package com.scispike;

public abstract class Callback<E,S> {
  abstract void call(E error, @SuppressWarnings("unchecked") S... args);
}