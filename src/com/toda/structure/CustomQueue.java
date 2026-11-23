// src/com/toda/structure/CustomQueue.java
package com.toda.structure;

import com.toda.exception.EmptyQueueException;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * FIFO queue for the terminal line, built on our own {@link DoublyLinkedList} (composition, not
 * java.util.LinkedList). Besides strict enqueue/dequeue it supports the dispatcher's real-world
 * exceptions: pulling a unit out of line, priority overrides and stable route sorting.
 */
public class CustomQueue<T> {
    private final DoublyLinkedList<T> list = new DoublyLinkedList<>();

    /** Joins the back of the line. */
    public void enqueue(T item) {
        list.addLast(item);
    }

    /** Leaves from the front of the line. */
    public T dequeue() throws EmptyQueueException {
        if (list.isEmpty()) {
            throw new EmptyQueueException("The queue is empty. No tricycle is waiting.");
        }
        return list.removeFirst();
    }

    /** Looks at the front of the line without removing it. */
    public T peek() throws EmptyQueueException {
        if (list.isEmpty()) {
            throw new EmptyQueueException("The queue is empty. No tricycle is waiting.");
        }
        return list.getFirst();
    }

    public T find(Predicate<T> matcher) {
        return list.find(matcher);
    }

    public int positionOf(Predicate<T> matcher) {
        int index = list.indexOf(matcher);
        return index < 0 ? -1 : index + 1;
    }

    /** Removes a specific unit (driver on break / maintenance) wherever it stands. */
    public T remove(Predicate<T> matcher) {
        return list.removeIf(matcher);
    }

    /** Priority override: jump to the front. */
    public boolean moveToFront(Predicate<T> matcher) {
        return list.moveToFront(matcher);
    }

    /** Swap one place forward. */
    public boolean moveUp(Predicate<T> matcher) {
        return list.moveUp(matcher);
    }

    /** Stable reorder; equal elements keep arrival order. */
    public void sort(Comparator<T> comparator) {
        list.stableSort(comparator);
    }

    public boolean contains(Predicate<T> matcher) {
        return list.find(matcher) != null;
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    /** Immutable front-to-back snapshot for JSON serialisation. */
    public List<T> toList() {
        return list.toList();
    }
}
