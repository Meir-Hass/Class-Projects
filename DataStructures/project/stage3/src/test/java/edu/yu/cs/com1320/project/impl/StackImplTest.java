package edu.yu.cs.com1320.project.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StackImplTest {

    private StackImpl<String> stack;

    @BeforeEach
    public void setUp() {
        stack = new StackImpl<>();
    }


    @Test
    public void testPushAndPop() {
        stack.push("A");
        stack.push("B");
        stack.push("null");

        assertEquals(3, stack.size());

        assertEquals("null", stack.pop());
        assertEquals("B", stack.pop());
        assertEquals("A", stack.pop());

        assertEquals(0, stack.size());
        assertNull(stack.pop());
    }

    @Test
    public void testPeek() {
        stack.push("X");
        stack.push("Y");
        stack.push("Z");

        assertEquals(3, stack.size());
        assertEquals("Z", stack.peek());
        assertEquals(3, stack.size());
    }

    @Test
    public void testEmptyStack() {
        assertNull(stack.peek());
        assertNull(stack.pop());
        assertEquals(0, stack.size());
    }
}