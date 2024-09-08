package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.MinHeap;

import java.util.Arrays;
import java.util.NoSuchElementException;

public class MinHeapImpl<E extends Comparable<E>> extends MinHeap<E> {

    private int initialCapacity = 10;

    public MinHeapImpl(){
        this.elements = (E[]) new Comparable[initialCapacity + 1];
    }

    public void reHeapify(E element){
try {

    upHeap(getArrayIndex(element));
    downHeap(getArrayIndex(element));
} catch (NoSuchElementException ignored){
}



    }

    protected int getArrayIndex(E element){
        for (int i=0; i< elements.length; i++){
            if(element.equals(elements[i])){
                    return i;
            }
        }
            throw new NoSuchElementException();
        }


    protected void doubleArraySize(){
        int newSize = this.elements.length * 2;
        elements = Arrays.copyOf(this.elements, newSize);
    }

}
