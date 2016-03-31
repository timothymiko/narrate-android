package com.datonicgroup.narrate.app.models;

import com.android.internal.util.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by timothymiko on 8/28/14.
 */
public class MutableArrayList<T> extends ArrayList<T> {

    private final ArrayList<T> mOriginalList;
    private Predicate<T> mFilter;
    private Comparator<T> mSort;

    public MutableArrayList(int capacity) {
        super(capacity);
        mOriginalList = new ArrayList<>(capacity);
    }

    public MutableArrayList() {
        mOriginalList = new ArrayList<>();
    }

    public MutableArrayList(Collection<? extends T> collection) {
        super(collection);
        mOriginalList = new ArrayList<>(collection);
    }

    public ArrayList<T> getOriginalList() {
        return mOriginalList;
    }

    public T find(Predicate<T> predicate) {
        for (T element : this) {
            if (predicate.apply(element)) {
                return element;
            }
        }

        return null;
    }

    public List<T> findAll(Predicate<T> predicate) {
        List<T> results = new ArrayList<>(size());
        for (T element : this) {
            if (predicate.apply(element)) {
                results.add(element);
            }
        }

        return results;
    }

    public void filter(Predicate<T> predicate) {
        this.mFilter = predicate;
        super.clear();

        if ( predicate == null ) {
            super.clear();
            super.addAll(mOriginalList);
        } else {
            for (T element : mOriginalList) {
                if (predicate.apply(element)) {
                    super.add(element);
                }
            }
        }
    }

    public void sort(Comparator<T> comparator) {
        this.mSort = comparator;
        Collections.sort(this, comparator);
        Collections.sort(mOriginalList, comparator);
    }

    @Override
    public boolean add(T object) {
        mOriginalList.add(object);
        return super.add(object);
    }

    @Override
    public void add(int index, T object) {
        mOriginalList.add(index, object);
        super.add(index, object);
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        mOriginalList.addAll(collection);
        boolean result = super.addAll(collection);

        if ( mFilter != null )
            filter(mFilter);

        if ( mSort != null )
            sort(mSort);

        return result;
    }

    @Override
    public void clear() {
        mOriginalList.clear();
        super.clear();
    }

    @Override
    public T remove(int index) {
        T object = super.remove(index);
        mOriginalList.remove(object);
        return object;
    }

    @Override
    public boolean remove(Object object) {
        mOriginalList.remove(object);
        return super.remove(object);
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
    }
}
