package edu.yu.cs.com1320.project.stage6.impl;


import edu.yu.cs.com1320.project.stage6.Document;

import java.net.URI;
import java.util.*;


public class DocumentImpl implements Document {

    private final URI uri;
    private final String text;
    private final byte[] binaryData;
    private HashMap<String, String> metadata;

    private HashMap<String, Integer> wordCounts;

    private long nanoTime;

    public DocumentImpl(URI uri, String text, Map<String, Integer> wordCountMap){
        if (uri == null || text == null || text.isBlank() || uri.toString().isBlank()) {
            throw new IllegalArgumentException("URI or text cannot be null or empty");
        }
        this.uri = uri;
        this.text = text;
        this.binaryData = null;
        this.metadata = new HashMap<>();
        if(wordCountMap == null) {
            this.wordCounts = new HashMap<>();
            addWords(text);
        } else{
            wordCounts = (HashMap<String, Integer>) wordCountMap;
        }
        this.nanoTime = System.nanoTime();

    }
    public DocumentImpl(URI uri, byte[] binaryData) {
        if (uri == null || binaryData == null || uri.toString().isBlank() || binaryData.length == 0 ) {
            throw new IllegalArgumentException("URI or binaryData cannot be null");
        }
        this.uri = uri;
        this.binaryData = binaryData;
        this.text = null;
        this.metadata = new HashMap();
        this.wordCounts = null;
        this.nanoTime = System.nanoTime();
    }


    /**
     * @param key   key of document metadata to store a value for
     * @param value value to store
     * @return old value, or null if there was no old value
     */
    @Override
    public String setMetadataValue(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Metadata key cannot be null or empty");
        }
        return metadata.put(key,value);
    }

    /**
     * @param key metadata key whose value we want to retrieve
     * @return corresponding value, or null if there is no such key
     */
    @Override
    public String getMetadataValue(String key) {
        if (key == null || key.isBlank()){
            throw new IllegalArgumentException("key cannot be null or empty");
        }
        return metadata.get(key);
    }

    /**
     * @return a COPY of the metadata saved in this document
     */
    @Override
    public HashMap<String, String> getMetadata() {
        HashMap<String, String> copyMetadata = new HashMap<>();
        for (String key : metadata.keySet()) {
            copyMetadata.put(key, metadata.get(key));
        }
        return copyMetadata;
    }

    @Override
    public void setMetadata(HashMap<String, String> metadata) {
        this.metadata = metadata;
    }
    /**
     * @return content of text document
     */
    @Override
    public String getDocumentTxt() {
        return this.text;
    }

    /**
     * @return content of binary data document
     */
    @Override
    public byte[] getDocumentBinaryData() {
        return this.binaryData;
    }

    /**
     * @return URI which uniquely identifies this document
     */
    @Override
    public URI getKey() {
        return this.uri;
    }




    /**
     * how many times does the given word appear in the document?
     * @param word
     * @return the number of times the given words appears in the document. If it's a binary document, return 0.
     */
    @Override
    public int wordCount(String word) {
        if (this.binaryData != null) {
            return 0;
        } else {
            try {
                return this.wordCounts.get(word);
            }catch (NullPointerException e){
                return 0;
            }
        }
    }


    /**
     * @return all the words that appear in the document
     */
    @Override
    public Set<String> getWords() {
        HashSet<String> words = new HashSet<>();
        if (this.binaryData != null) {
            return words;
        } else {
            try {
                words.addAll(this.wordCounts.keySet());
                return words;
            } catch (NullPointerException e) {
                return words;
            }
        }
    }


    private void addWords (String word ){
        //remove punctuation
        String punctuationRemoved = word.replaceAll("[^a-zA-Z0-9\\s]", "");
        String[] splitString = punctuationRemoved.split("\\s+");
        for (String currentWord : splitString){
            int counter = this.wordCounts.getOrDefault(currentWord, 0);
            this.wordCounts.put(currentWord, counter + 1);
        }
    }


    @Override
    public int hashCode() {
        int result = uri.hashCode();
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(binaryData);
        return Math.abs(result);
    }


    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        DocumentImpl document = (DocumentImpl) obj;
        return this.hashCode() == document.hashCode();
    }

    /**
     * return the last time this document was used, via put/get or via a search result
     * (for stage 4 of project)
     */
    public long getLastUseTime(){
        return this.nanoTime;
    }
    public void setLastUseTime(long timeInNanoseconds){
        this.nanoTime = timeInNanoseconds;
    }

    /**
     * @return a copy of the word to count map so it can be serialized
     */
    @Override
    public HashMap<String, Integer> getWordMap() {
        return new HashMap<>(wordCounts);
    }

    /**
     * This must set the word to count map durlng deserialization
     *
     * @param wordMap
     */
    public void setWordMap(HashMap<String, Integer> wordMap){
        this.wordCounts = wordMap;

    }


    @Override
    public int compareTo(Document o) {

        if (this.nanoTime > o.getLastUseTime()){

            return 1;
        } else if(this.nanoTime< o.getLastUseTime()){
            return -1;

        } else {
            return 0;
        }

    }

}
