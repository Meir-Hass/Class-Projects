package edu.yu.cs.com1320.project.stage3.impl;

import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.stage3.Document;

import java.net.URI;
import java.util.Arrays;



public class DocumentImpl implements Document {

    private final URI uri;
    private final String text;
    private final byte[] binaryData;
    private final HashTable<String, String> metadata;

    public DocumentImpl(URI uri, String txt){
        if (uri == null || txt == null || txt.isBlank() || uri.toString().isBlank()) {
            throw new IllegalArgumentException("URI or text cannot be null or empty");
        }
        this.uri = uri;
        this.text = txt;
        this.binaryData = null;
        this.metadata = new HashTableImpl<>();

    }
    public DocumentImpl(URI uri, byte[] binaryData) {
        if (uri == null || binaryData == null || uri.toString().isBlank() || binaryData.length == 0 ) {
            throw new IllegalArgumentException("URI or binaryData cannot be null");
        }
        this.uri = uri;
        this.binaryData = binaryData;
        this.text = null;
        this.metadata =  new HashTableImpl<>();
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
    public HashTable<String, String> getMetadata() {
        HashTable<String, String> copyMetadata = new HashTableImpl<>();
        for (String key : metadata.keySet()) {
            copyMetadata.put(key, metadata.get(key));
        }
        return copyMetadata;
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


}

