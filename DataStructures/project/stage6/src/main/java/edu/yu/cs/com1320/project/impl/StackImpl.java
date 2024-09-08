package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Stack;

public class StackImpl<T> implements Stack<T>{

    private Node<T> head;
    private int size;


    private static class Node<T>{

        private T data;
        private Node<T> next;

        private Node(T data, Node<T> next) {
            this.data = data;
            this.next = next;
        }
    }



    public StackImpl(){
        this.head = null;
        this.size = 0;
    }

    /**
     * @param element object to add to the Stack
     */
    @Override
    public void push(T element){
        this.head = new Node(element, head);
        this.size++;
    }

    /**
     * removes and returns element at the top of the stack
     * @return element at the top of the stack, null if the stack is empty
     */
    @Override
    public T pop(){
        if (this.head == null || this.size == 0){
            return null;
        }
        Node oldHead = this.head;
        head = oldHead.next;
        this.size--;
        return (T) oldHead.data;
    }

    /**
     *
     * @return the element at the top of the stack without removing it
     */
    @Override
    public T peek() {
        if (this.size == 0) {
            return null;
        } else {
            return (T) this.head.data;
        }
    }

    /**
     *
     * @return how many elements are currently in the stack
     */
    @Override
    public int size(){
        return this.size;
    }

}