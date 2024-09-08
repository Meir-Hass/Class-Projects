package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.BTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BTreeImplTest {
    private BTree<String, Integer> hashTable;

    @BeforeEach
    void setUp() {
        hashTable = new BTreeImpl<>();
        hashTable.put("one", 1);
        hashTable.put("two", 2);
        hashTable.put("three", 3);
    }

    @Test
    void testGet() throws IOException {
        assertEquals(1, hashTable.get("one"));
        assertEquals(2, hashTable.get("two"));
        assertEquals(3, hashTable.get("three"));
        assertEquals(1, hashTable.get("one"));

    }

    @Test
    void testPut() {
        assertEquals(1, hashTable.put("one", 10));
        assertEquals(10, hashTable.get("one"));
        assertNull(hashTable.put("four", 4));
        assertEquals(4, hashTable.get("four"));
    }



    @Test
    void testDelete() {
        assertEquals(1, hashTable.get("one"));
        Integer deletedValue = hashTable.put("one", null);
        assertEquals(1, deletedValue);
        assertNull(hashTable.get("one"));
    }
}


