// src/com/toda/structure/Node.java
package com.toda.structure;

/** A single link in the {@link DoublyLinkedList}. Package-private so the list owns its wiring. */
class Node<T> {
    T data;
    Node<T> prev;
    Node<T> next;

    Node(T data) {
        this.data = data;
    }
}
