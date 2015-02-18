package com.scispike.callback;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class EventEmitter<S> {

  @SuppressWarnings("hiding")
  abstract class Once<S> extends Event<S> {
    private Callback<S, S> off;

    Callback<S,S> getOff() {
      return off;
    }

    void setOff(Callback<S,S> off) {
      this.off = off;
    }
  }

  final Map<S, Set<Event<S>>> listeners = new Hashtable<S, Set<Event<S>>>();

  public Callback<S,S> once(final S msg, final Event<S> cb) {
    Once<S> once = new Once<S>() {

      @SuppressWarnings("unchecked")
      @Override
      public void onEmit(S... args) {

        getOff().call(null);
        cb.onEmit(args);
      }

    };
    Callback<S, S> off = on(msg, once);
    // get around the final variable not init'd problem
    once.setOff(off);
    return off;
  }

  public Callback<S, S> on(final S msg, final Event<S> cb) {
    Callback<S, S> off = new Callback<S, S>() {
      @SuppressWarnings("unchecked")
      @Override
      public void call(S error, S... args) {
        listeners.get(msg).remove(cb);
      }
    };
    Set<Event<S>> l = listeners.get(msg);
    if (l == null) {
      l = new LinkedHashSet<Event<S>>();
      listeners.put(msg, l);
    }
    l.add(cb);
    return off;
  }

  @SuppressWarnings("unchecked")
  public void emit(S msg, S... data) {
    // toArray so we don't get concurrent modification from off
    Set<Event<S>> set = listeners.get(msg);
    if(set != null && set.size()>0){
      Event<S>[] events = set.toArray(new Event[] {});
      for (Event<S> cb : events) {
        cb.onEmit(data);
      }
    }
  }

  public void removeAllListeners(S msg) {
    listeners.remove(msg);
  }
  public void removeAllListeners(){
    listeners.clear();
  }

}