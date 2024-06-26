package org.jetbrains.java.decompiler.util.collections.fixed;


import org.jetbrains.java.decompiler.modules.decompiler.ValidationHelper;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class FastFixedSet<E> extends AbstractCollection<E> implements Set<E> {
  protected final FastFixedSetFactory<E> factory;

  protected FastFixedSet(FastFixedSetFactory<E> factory) {
    this.factory = factory;
  }

  @Deprecated
  public final int size() {
    ValidationHelper.validateTrue(false, "The behaviour of FastFixedSet.size() is not correct");
    return this.factory.getEntries().size();
  }

  public abstract int getRealSize();

  @Override
  public abstract FastFixedSet<E> clone();

  public abstract void setAllElements();

  @Override
  public abstract boolean add(E element);

  @Override
  public abstract boolean contains(Object element);

  public abstract boolean containsAll(FastFixedSet<E> set);

  @Override
  @SuppressWarnings("unchecked")
  public boolean containsAll(Collection<?> c) {
    if (c instanceof FastFixedSet<?>) {
      FastFixedSet<?> c1 = (FastFixedSet<?>) c;
      if (c1.factory == this.factory) {
        return this.containsAll((FastFixedSet<E>) c1);
      }
    }

    return this.containsAll(c);
  }

  public abstract boolean addAll(FastFixedSet<E> set);

  @Override
  @SuppressWarnings("unchecked")
  public boolean addAll(Collection<? extends E> c) {
    if (c instanceof FastFixedSet<?>) {
      FastFixedSet<?> c1 = (FastFixedSet<?>) c;
      if (c1.factory == this.factory) {
        return this.addAll((FastFixedSet<E>) c1);
      }
    }

    return super.addAll(c);
  }

  public abstract boolean retainAll(FastFixedSet<E> set);

  @Override
  @SuppressWarnings({"unchecked", "SuspiciousMethodCalls"})
  public boolean retainAll(Collection<?> c) {
    if (c instanceof FastFixedSet<?>) {
      FastFixedSet<?> c1 = (FastFixedSet<?>) c;
      if (c1.factory == this.factory) {
        return this.retainAll((FastFixedSet<E>) c1);
      }
    }

    Set<E> stuffs = new HashSet<>(this.factory.getEntries());
    stuffs.removeAll(c);
    return this.removeAll(stuffs);
  }

  public abstract boolean removeAll(FastFixedSet<E> set);

  @Override
  @SuppressWarnings("unchecked")
  public boolean removeAll(Collection<?> c) {
    if (c instanceof FastFixedSet<?>) {
      FastFixedSet<?> c1 = (FastFixedSet<?>) c;
      if (c1.factory == this.factory) {
        return this.removeAll((FastFixedSet<E>) c1);
      }
    }

    // super uses c.contains, which might be much slower
    // than this.contains, so we just use a simple loop over c
    boolean mutated = false;
    for (Object o : c) {
      mutated |= this.remove(o);
    }

    return mutated;
  }

  @Override
  public abstract boolean equals(Object o);

  @Override
  public abstract int hashCode();

  public abstract String toString();

  public Set<E> toPlainSet() {
    final HashSet<E> set = new HashSet<>(this.getRealSize());
    set.addAll(this);
    return set;
  }

  @Deprecated
  public boolean containsKey(E id) {
    return this.factory.getEntries().contains(id);
  }

  @Deprecated
  public boolean contains(FastFixedSet<E> set) {
    return this.containsAll(set);
  }

  @Deprecated
  public void complement(FastFixedSet<E> set) {
    this.removeAll(set);
  }

  @Deprecated
  public FastFixedSet<E> getCopy() {
    return this.clone();
  }

  @Deprecated
  public void union(FastFixedSet<E> set) {
    this.addAll(set);
  }

  @Deprecated
  public void intersection(FastFixedSet<E> set) {
    this.retainAll(set);
  }
}
