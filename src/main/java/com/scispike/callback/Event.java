package com.scispike.callback;

public abstract class Event<S> {
  public abstract void onEmit(@SuppressWarnings("unchecked") S... data);
}