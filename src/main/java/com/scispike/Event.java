package com.scispike;

public abstract class Event<S> {
  abstract void onEmit(S... data);
}