package br.unb.cic.tdp.proof.util;

import lombok.val;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListOfCycles {
    public static final ListOfCycles EMPTY_LIST = new ListOfCycles();
    public int size;
    public Node head;
    public Node tail;

    public ListOfCycles() {
    }

    public static ListOfCycles singleton(int[] data) {
        val singleton = new ListOfCycles();
        singleton.head = new Node(data);
        singleton.size = 1;
        return singleton;
    }

    public static ListOfCycles asList(int[]... elements) {
        val list = new ListOfCycles();
        for (int[] element : elements) {
            list.add(element);
        }
        return list;
    }

    public void add(int[] data) {
        val newNode = new Node(data);

        if (head == null) {
            head = tail = newNode;
            head.previous = null;
        } else {
            tail.next = newNode;
            newNode.previous = tail;
            tail = newNode;
        }

        tail.next = null;

        size++;
    }

    public void remove(int[] data) {
        var current = tail;

        // do the search walking backwards
        while (current != null && current.data != data) {
            current = current.previous;
        }

        if (current == null) {
            return;
        }

        if (current.previous == null) {
            head = current.next;
        } else{
            current.previous.next = current.next;
        }

        if (current.next == null) {
            tail = current.previous;
        } else {
            current.next.previous = current.previous;
        }

        size--;
    }

    public boolean contains(final int[] data) {
        var current = this.head;
        for (int i = 0; i < this.size; i++) {
            if (current.data == data) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    @Override
    public String toString() {
        val str = new StringBuilder();
        str.append("[");

        for (var current = head; current != null; current = current.next) {
            str.append(Arrays.toString(current.data));
            str.append(" ");
        }

        str.append("]");

        return str.toString();
    }

    public void removeAll(final ListOfCycles other) {
        for (var current = other.head; current != null; current = current.next) {
            this.remove(current.data);
        }
    }

    public void addAll(final ListOfCycles other) {
        for (var current = other.head; current != null; current = current.next) {
            this.add(current.data);
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public List<int[]> toList() {
        val list = new ArrayList<int[]>();
        for (var current = head; current != null; current = current.next) {
            list.add(current.data);
        }
        return list;
    }

    public static class Node {
        public final int[] data;
        public Node next;
        public Node previous;
        public Node(int[] d) { data = d; }

        @Override
        public String toString() {
            return Arrays.toString(data);
        }
    }
}