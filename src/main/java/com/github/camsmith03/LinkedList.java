package com.github.camsmith03;

import java.util.NoSuchElementException;

public class LinkedList<T> {
    public Node<T> head;

    public LinkedList() {
        head = new Node<>();
        head.data = null;
        head.next = null;
    }


    public Node<T> delete(T element) {
        Node<T> curr = head;
        Node<T> prev = curr;

        while (curr != null && curr.data != element) {
            prev = curr;
            curr = curr.next;
        }

        if (curr == null) {
            throw new NoSuchElementException("Element not found");
        }

        prev.next = curr.next;

        return curr;
    }


    public static class Node<T> {
        public T data;
        public Node<T> next;
    }
}
