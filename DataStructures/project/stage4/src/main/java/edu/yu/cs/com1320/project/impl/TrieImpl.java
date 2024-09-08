package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Trie;

import java.util.*;

/**
 * FOR STAGE 3
 * @param <Value>
 */
public class TrieImpl<Value> implements Trie<Value> {

    private static final int alphabetSize = 256; // extended ASCII

    private Node root;

    private static class Node<Value> {
        private Set<Value> values;

        private Node[] next;

        private Node() {
            this.values = new HashSet<>();
            this.next = new Node[alphabetSize];
        }
    }


    /**
     * add the given value at the given key
     *
     * @param key
     * @param val
     */
    @Override
    public void put(String key, Value val) {
        if (key == null || val == null) {
            throw new IllegalArgumentException();
        }
        else {
            root = put(root, key, val, 0);
        }
    }

    private Node<Value> put(Node<Value> x, String key, Value val, int d) {
        if (x == null) {
            x = new Node<>();
        }
        if (d == key.length()) {
            x.values.add(val);
            return x;
        }
        char c = key.charAt(d);
        x.next[c] = put(x.next[c], key, val, d + 1);
        return x;
    }


    /**
     * Get all exact matches for the given key, sorted in descending order, where "descending" is defined by the comparator.
     * NOTE FOR COM1320 PROJECT: FOR PURPOSES OF A *KEYWORD* SEARCH, THE COMPARATOR SHOULD DEFINE ORDER AS HOW MANY TIMES THE KEYWORD APPEARS IN THE DOCUMENT.
     * Search is CASE SENSITIVE.
     *
     * @param key
     * @param comparator used to sort values
     * @return a List of matching Values. Empty List if no matches.
     */
    @Override
    public List<Value> getSorted(String key, Comparator<Value> comparator) {
        if (key == null ||comparator == null) {
            throw new IllegalArgumentException();
        }
        Set<Value> Docs = new HashSet<>();
        Node<Value> node = getNode(root, key, 0);
        if (node != null) {
            Docs.addAll(node.values);
        }
        List<Value> Values = new ArrayList<>(Docs);
        Values.sort(comparator);
        return Values;

    }

    private Node<Value> getNode(Node<Value> x, String key, int d) {
        if (x == null) return null;
        if (d == key.length()) return x;
        char c = key.charAt(d);
        return getNode(x.next[c], key, d + 1);
    }

    /**
     * get all exact matches for the given key.
     * Search is CASE SENSITIVE.
     *
     * @param key
     * @return a Set of matching Values. Empty set if no matches.
     */
    @Override
    public Set<Value> get(String key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }
        HashSet<Value> values = new HashSet<>();
        Node x = getNode(root, key, 0);
        if (x == null) {
            return values;
        } else {
            values.addAll(x.values);
            return values;
        }

    }

    /**
     * get all matches which contain a String with the given prefix, sorted in descending order, where "descending" is defined by the comparator.
     * NOTE FOR COM1320 PROJECT: FOR PURPOSES OF A *KEYWORD* SEARCH, THE COMPARATOR SHOULD DEFINE ORDER AS HOW MANY TIMES THE KEYWORD APPEARS IN THE DOCUMENT.
     * For example, if the key is "Too", you would return any value that contains "Tool", "Too", "Tooth", "Toodle", etc.
     * Search is CASE SENSITIVE.
     *
     * @param prefix
     * @param comparator used to sort values
     * @return a List of all matching Values containing the given prefix, in descending order. Empty List if no matches.
     */
    @Override
    public List<Value> getAllWithPrefixSorted(String prefix, Comparator<Value> comparator) {
        if (prefix == null || comparator == null) {
            throw new IllegalArgumentException();
        }
        Set<Value> Docs = new HashSet<>();
        Node<Value> x = getNode(root, prefix, 0);
        if (x != null) {
            collectList(x, Docs);
        }
        List<Value> values = new ArrayList<>(Docs);
        values.sort(comparator);
        return values;
    }


    private void collectList(Node<Value> x, Set<Value> values) {
        if (x == null) {
            return;
        }
        values.addAll(x.values);
        for (Node<Value> nextNode : x.next) {
            collectList(nextNode, values);
        }
    }


    /**
     * Delete the subtree rooted at the last character of the prefix.
     * Search is CASE SENSITIVE.
     *
     * @param prefix
     * @return a Set of all Values that were deleted.
     */
    @Override
    public Set<Value> deleteAllWithPrefix(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException();
        }
        Set<Value> values = new HashSet<>();
        Node<Value> x = getNode(root, prefix, 0);
        if (x != null) {
            if (x.next != null) {
                collectSet(x, values);
                x.next = new Node[alphabetSize];

            } else {
                try {
                    values.addAll(x.values);
                    x.values.clear();
                } catch (NullPointerException ignore) {
                }
            }
            if (x.values.isEmpty()) {
                deleteAll(this.root, prefix, 0, new HashSet<>());
            }
        }
        return values;
    }

    private void collectSet(Node<Value> x, Set<Value> values) {
        if (x == null) {
            return;
        }
        values.addAll(x.values);
        x.values.clear();
        for (Node<Value> nextNode : x.next) {
            collectSet(nextNode, values);
        }
    }


    /**
     * Delete all values from the node of the given key (do not remove the values from other nodes in the Trie)
     *
     * @param key
     * @return a Set of all Values that were deleted.
     */
    @Override
    public Set<Value> deleteAll(String key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }
        Set<Value> deletedValues = new HashSet<>();
        this.root = deleteAll(this.root, key, 0, deletedValues);
        return deletedValues;
    }

    private Node<Value> deleteAll(Node<Value> x, String key, int d, Set<Value> values) {
        if (x == null) {
            return null;
        }
        //we're at the node to del - set the val to null
        if (d == key.length()) {
            values.addAll(x.values);
            x.values.clear();
        }
        //continue down the trie to the target node
        else {
            char c = key.charAt(d);
            x.next[c] = this.deleteAll(x.next[c], key, d + 1, values);
        }
        //this node has a val – do nothing, return the node
        if (!x.values.isEmpty()) {
            return x;
        }
        //remove subtrie rooted at x if it is completely empty
        for (int c = 0; c < TrieImpl.alphabetSize; c++) {
            if (x.next[c] != null) {
                return x; //not empty
            }
        }
        //empty - set this link to null in the parent
        return null;
    }


    /**
     * Remove the given value from the node of the given key (do not remove the value from other nodes in the Trie)
     * @param key
     * @param val
     * @return the value which was deleted. If the key did not contain the given value, return null.
     */
    @Override
    public Value delete(String key, Value val){
        if (key == null || val == null) {
            throw new IllegalArgumentException();
        }
        Node<Value> x = getNode(this.root, key,0);
        if (x != null && x.values.contains(val)){
            x.values.remove(val);
            if(x.values.isEmpty()) {
                deleteAll(this.root, key, 0, new HashSet<>());
            }
            return val;
            } else{
            return null;
        }
    }

    private void prune(Node<Value> x, String key, String lastkey) {

        //makes sure the values of x. values is empty
        boolean nullvalues = x.values.isEmpty();

        //if confirmed that x.values is empty make sure it doesn't have references
        if (nullvalues) {
            for (Node<Value> y : x.next) {
                if (y != null) {
                    nullvalues = false;
                    break;
                }
            }
        }

        // if no values or references were found and we are not at the root
        if (nullvalues && x != this.root){
            //take the last letter off the key
            String choppedString = key.substring(0, key.length() - 1);
            //recursive call with the node at the chopped string, chopped string as the new key and the current key as last key
            prune(getNode(root, choppedString, 0), choppedString, key);
        }

        // check to make sure this is not the first call
        if(lastkey!= null) {
            x.next[lastkey.charAt(lastkey.length() - 1)] = null;
        }

    }
}
