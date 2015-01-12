package com.scispike;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;


public class EventEmitter {

  abstract class Once extends Callback {
    private Callback off;

    public Callback getOff() {
      return off;
    }

    public void setOff(Callback off) {
      this.off = off;
    }
  }

  final Map<String, Set<Callback>> listeners = new Hashtable<String, Set<Callback>>();

  public Callback once(final String msg, final Callback cb) {
    Once once = new Once() {
      @Override
      void done(String... args) {
        getOff().done(args);
        cb.done(args);
      }
    };
    Callback off = on(msg, once);
    // get around the final variable not init'd problem
    once.setOff(off);
    return off;
  }

  public Callback on(final String msg, final Callback cb) {
    Callback off = new Callback() {
      @Override
      void done(String... args) {
        listeners.get(msg).remove(cb);
      }
    };
    Set<Callback> l = listeners.get(msg);
    if (l == null) {
      l = new HashSet<Callback>();
      listeners.put(msg, l);
    }
    l.add(cb);
    return off;
  }

  public void emit(String msg, String... data) {
    // toArray so we don't get concurrent modification from off
    for (Callback cb : listeners.get(msg).toArray(new Callback[] {})) {
      cb.done(data);
    }
  }

  public void removeAllListeners(String msg) {
    listeners.remove(msg);
  }

}