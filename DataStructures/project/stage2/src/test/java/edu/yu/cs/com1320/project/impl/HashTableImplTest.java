package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.HashTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Set;

public class HashTableImplTest {
    private HashTable<String, Integer> hashTable;

    @BeforeEach
    void setUp() {
        hashTable = new HashTableImpl<>();
        hashTable.put("one", 1);
        hashTable.put("two", 2);
        hashTable.put("three", 3);
    }

    @Test
    void testGet() {
        assertEquals(1, hashTable.get("one"));
        assertEquals(2, hashTable.get("two"));
        assertEquals(3, hashTable.get("three"));
        assertNull(hashTable.get("four"));
    }

    @Test
    void testPut() {
        assertEquals(1, hashTable.put("one", 10));
        assertEquals(10, hashTable.get("one"));
        assertNull(hashTable.put("four", 4));
        assertEquals(4, hashTable.get("four"));
    }

    @Test
    void testContainsKey() {
        assertTrue(hashTable.containsKey("one"));
        assertFalse(hashTable.containsKey("four"));
    }

    @Test
    void testKeySet() {
        Set<String> keys = hashTable.keySet();
        assertEquals(3, keys.size());
        assertTrue(keys.contains("one"));
        assertTrue(keys.contains("two"));
        assertTrue(keys.contains("three"));
        assertFalse(keys.contains("four"));
    }

    @Test
    void testValueCollection() {
        Collection<Integer> values = hashTable.values();
        assertEquals(3, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
        assertFalse(values.contains(4));
    }

    @Test
    void testSize() {
        assertEquals(3, hashTable.size());
        hashTable.put("four", 4);
        assertEquals(4, hashTable.size());
        hashTable.put("one", 10);
        assertEquals(4, hashTable.size());
        hashTable.put("five", 5);
        assertEquals(5, hashTable.size());
        hashTable.put("one", null);
        assertEquals(4, hashTable.size());
    }

    @Test
    void testDelete() {
        assertTrue(hashTable.containsKey("one"));
        assertEquals(1, hashTable.get("one"));
        Integer deletedValue = hashTable.put("one", null);
        assertEquals(1, deletedValue);
        assertNull(hashTable.get("one"));
        assertFalse(hashTable.containsKey("one"));
        assertEquals(2, hashTable.size());
    }
}
