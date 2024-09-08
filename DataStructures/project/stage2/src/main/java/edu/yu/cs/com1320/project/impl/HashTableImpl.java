package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.HashTable;

import java.util.*;

public class HashTableImpl<Key, Value> implements HashTable<Key, Value> {

    private int amountOfPairs;
    private final int tableSize;
    private final entry[] chainedEntrys;

    // nested class for entries that contain key value pairs with a next reference

    private static class entry {
        private Object Key;
        private Object Value;
        private entry next;

        private entry(Object key, Object value, entry next) {
            this.Key = key;
            this.Value = value;
            this.next = next;
        }
    }
// constructor
    public HashTableImpl() {
        this.tableSize = 5;
        chainedEntrys = new entry[5];

    }
// private hashfuntion returns a int one less than tableSize
    private int hashfunction(Key key) {
        return (key.hashCode() & 0x7fffffff) % this.tableSize;
    }

    /**
     * @param k the key whose value should be returned
     * @return the value that is stored in the HashTable for k, or null if there is no such key in the table
     */
    @Override
    public Value get(Key k) {
        int i = hashfunction(k);
        for (entry x = chainedEntrys[i]; x != null; x = x.next) {
            if (k.equals(x.Key)) return (Value) x.Value;
        }
        return null;
    }

    /**
     * @param k the key at which to store the value
     * @param v the value to store
     *          To delete an entry, put a null value.
     * @return if the key was already present in the HashTable, return the previous value stored for the key. If the key was not already present, return null.
     */
    @Override
    public Value put(Key k, Value v){
        if (v == null) {
            return delete(k);
        }
        int i = hashfunction(k);
        for (entry x = chainedEntrys[i]; x != null; x = x.next) {
           //if there is an existing entry for said key
            if (k.equals(x.Key)) {
                Value previousValue = (Value) x.Value;
                x.Value = v;
                return previousValue;
            }
        }
        //if no existing entry
        amountOfPairs++;
        // add new entry in front of linked list for the i index
        chainedEntrys[i] = new entry(k, v, chainedEntrys[i]);
        return null;
    }

    /**
     * @param key the key whose presence in the hashtabe we are inquiring about
     * @return true if the given key is present in the hashtable as a key, false if not
     * @throws NullPointerException if the specified key is null
     */
  @Override
  public boolean containsKey(Key key) throws NullPointerException{
      if (key == null){
          throw new NullPointerException();
      }
      return get(key) != null;
  }

    /**
     * @return an unmodifiable set of all the keys in this HashTable
     * @see java.util.Collections#unmodifiableSet(Set)
     */
    @Override
    public Set<Key> keySet(){
        Set<Key> keySet = new HashSet<>();
    for (entry entry : chainedEntrys) {
        if (entry == null){
            continue;
        }
        while (entry!= null){
            keySet.add((Key)entry.Key);
            entry = entry.next;

        }
    }
    return Collections.unmodifiableSet(keySet);

    }

    /**
     * @return an unmodifiable collection of all the values in this HashTable
     * @see java.util.Collections#unmodifiableCollection(Collection)
     */
    @Override
    public Collection<Value> values(){
        ArrayList<Value> valueList = new ArrayList<>() ;
        for (entry entry : chainedEntrys) {
            if (entry == null){
                continue;
            }
            while (entry!= null){
                valueList.add((Value) entry.Value);
                entry = entry.next;

            }
        }
        return Collections.unmodifiableList(valueList);

    }

/**
 * @return how entries there currently are in the HashTable
 */
@Override
public int size(){
    return this.amountOfPairs;
}

private Value delete (Key k) {
    int i = hashfunction(k);
    entry previous = null;
    entry current = chainedEntrys[i];

    while (current != null) {
        if (k.equals(current.Key)) {
            if (previous == null) {
                //key is found at head
                chainedEntrys[i] = current.next;
            } else {
                //if key if found in middle
                previous.next = current.next;
            }
            amountOfPairs --;
            return (Value) current.Value;
        }
        // if the key is not found in the cycle above
        previous = current;
        current = current.next;
    }
    // if the while loop completed and no matching key was found
    return null;
}
    }
