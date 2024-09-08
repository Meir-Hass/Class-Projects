package edu.yu.cs.com1320.project.impl;

import static org.junit.jupiter.api.Assertions.*;

import edu.yu.cs.com1320.project.Trie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Set;



public class TrieImplTest {

    private Trie<String> trie;

    @BeforeEach
    void setUp() {

        trie = new TrieImpl<>();
    }

    @Test
    void testPutAndGet() {
        trie.put("apple", "value1");
        Set<String> appleValues = trie.get("apple");
        assertEquals(1, appleValues.size());
        assertTrue(appleValues.contains("value1"));
    }



    @Test
    void testGetSorted() {
        trie.put("apple", "value1");
        trie.put("banana", "value2");
        trie.put("banana", "value3");

        List<String> sortedValues = trie.getSorted("banana", Comparator.naturalOrder());
        assertEquals(2, sortedValues.size());
        assertEquals("value2", sortedValues.get(0));
        assertEquals("value3", sortedValues.get(1));
    }

    @Test
    void testGetAllWithPrefixSorted() {
        trie.put("apple", "value1");
        trie.put("banana", "value2");
        trie.put("banana", "value3");

        List<String> valuesWithPrefix = trie.getAllWithPrefixSorted("ban", Comparator.naturalOrder());
        assertEquals(2, valuesWithPrefix.size());
        assertEquals("value2", valuesWithPrefix.get(0));
        assertEquals("value3", valuesWithPrefix.get(1));
    }

    @Test
    void testDeleteAllWithPrefix() {
        trie.put("apple", "value1");
        trie.put("banana", "value2");
        trie.put("banana", "value3");

        Set<String> deletedValues = trie.deleteAllWithPrefix("banana");
        assertEquals(2, deletedValues.size());
        assertTrue(deletedValues.contains("value2"));
        assertTrue(deletedValues.contains("value3"));

        assertTrue(trie.get("banana").isEmpty());
    }

    @Test
    void testDeleteAll() {
        trie.put("apple", "value1");
        trie.put("banana", "value2");
        trie.put("banana", "value3");

        Set<String> deletedValues = trie.deleteAll("banana");
        assertEquals(2, deletedValues.size());
        assertTrue(deletedValues.contains("value2"));
        assertTrue(deletedValues.contains("value3"));

        assertTrue(trie.get("banana").isEmpty());
    }

    @Test
    void testDelete() {
        trie.put("apple", "value1");
        trie.put("banana", "value2");

        String deletedValue = trie.delete("banana", "value2");
        assertEquals("value2", deletedValue);

        Set<String> bananaValues = trie.get("banana");
        assertTrue(bananaValues.isEmpty());
    }
}