// src/com/toda/structure/DoublyLinkedList.java
package com.toda.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Hand-built generic doubly linked list. Head and tail pointers give O(1) add/remove at both
 * ends, and the back-links let us unlink any node in place (used when a driver leaves the line).
 * Nothing outside this class can touch a {@link Node}, so the chain can never be corrupted.
 */
public class DoublyLinkedList<T> {
    private Node<T> head;
    private Node<T> tail;
    private int size;

    public void addLast(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot store null in the list");
        }
        Node<T> node = new Node<>(item);
        if (tail == null) {
            head = tail = node;
        } else {
            tail.next = node;
            node.prev = tail;
            tail = node;
        }
        size++;
    }

    public void addFirst(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot store null in the list");
        }
        Node<T> node = new Node<>(item);
        if (head == null) {
            head = tail = node;
        } else {
            node.next = head;
            head.prev = node;
            head = node;
        }
        size++;
    }

    /** Removes and returns the first item, or null when empty. */
    public T removeFirst() {
        if (head == null) {
            return null;
        }
        return unlink(head);
    }

    public T getFirst() {
        return head == null ? null : head.data;
    }

    public T getLast() {
        return tail == null ? null : tail.data;
    }

    /** Returns the first item matching the predicate, or null. */
    public T find(Predicate<T> matcher) {
        Node<T> node = findNode(matcher);
        return node == null ? null : node.data;
    }

    /** Returns the zero-based position of the first matching item, or -1. */
    public int indexOf(Predicate<T> matcher) {
        int index = 0;
        for (Node<T> cur = head; cur != null; cur = cur.next, index++) {
            if (matcher.test(cur.data)) {
                return index;
            }
        }
        return -1;
    }

    /** Unlinks the first matching item in place and returns it, or null when absent. */
    public T removeIf(Predicate<T> matcher) {
        Node<T> node = findNode(matcher);
        return node == null ? null : unlink(node);
    }

    /** Moves the first matching item to the head. Returns false when it was not found. */
    public boolean moveToFront(Predicate<T> matcher) {
        Node<T> node = findNode(matcher);
        if (node == null) {
            return false;
        }
        if (node != head) {
            T data = unlink(node);
            addFirst(data);
        }
        return true;
    }

    /** Swaps the first matching item with the one in front of it. Returns false when not found. */
    public boolean moveUp(Predicate<T> matcher) {
        Node<T> node = findNode(matcher);
        if (node == null) {
            return false;
        }
        if (node.prev != null) {
            T tmp = node.prev.data;
            node.prev.data = node.data;
            node.data = tmp;
        }
        return true;
    }

    /**
     * Stable insertion sort performed directly on the links. Equal items keep their relative
     * order, which is what keeps the FIFO line fair when auto-route sorting is switched on.
     */
    public void stableSort(Comparator<T> comparator) {
        if (size < 2) {
            return;
        }
        Node<T> cur = head.next;
        while (cur != null) {
            Node<T> next = cur.next;
            Node<T> scan = cur.prev;
            while (scan != null && comparator.compare(scan.data, cur.data) > 0) {
                scan = scan.prev;
            }
            if (scan != cur.prev) {
                T data = unlink(cur);
                insertAfter(scan, data);
            }
            cur = next;
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        head = tail = null;
        size = 0;
    }

    /** Read-only snapshot in head-to-tail order (safe to serialise; mutations do not leak back). */
    public List<T> toList() {
        List<T> out = new ArrayList<>(size);
        for (Node<T> cur = head; cur != null; cur = cur.next) {
            out.add(cur.data);
        }
        return Collections.unmodifiableList(out);
    }

    private Node<T> findNode(Predicate<T> matcher) {
        for (Node<T> cur = head; cur != null; cur = cur.next) {
            if (matcher.test(cur.data)) {
                return cur;
            }
        }
        return null;
    }

    private void insertAfter(Node<T> anchor, T data) {
        if (anchor == null) {
            addFirst(data);
            return;
        }
        if (anchor == tail) {
            addLast(data);
            return;
        }
        Node<T> node = new Node<>(data);
        node.prev = anchor;
        node.next = anchor.next;
        anchor.next.prev = node;
        anchor.next = node;
        size++;
    }

    private T unlink(Node<T> node) {
        if (node.prev == null) {
            head = node.next;
        } else {
            node.prev.next = node.next;
        }
        if (node.next == null) {
            tail = node.prev;
        } else {
            node.next.prev = node.prev;
        }
        node.prev = node.next = null;
        size--;
        return node.data;
    }
}
