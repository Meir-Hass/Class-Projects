package edu.yu.cs.com1320.project.stage1.impl;
import edu.yu.cs.com1320.project.stage1.Document;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;

public class DocumentImpl implements Document {

    private final URI uri;
    private final String text;
    private final byte[] binaryData;
    private final HashMap<String, String> metadata;

    public DocumentImpl(URI uri, String txt){
        if (uri == null || txt == null || txt.isBlank() || uri.toString().isBlank()) {
            throw new IllegalArgumentException("URI or text cannot be null or empty");
        }
        this.uri = uri;
        this.text = txt;
        this.binaryData = null;
        this.metadata = new HashMap<>();
    }

    public DocumentImpl(URI uri, byte[] binaryData) {
        if (uri == null || binaryData == null || uri.toString().isBlank() || binaryData.length == 0 ) {
            throw new IllegalArgumentException("URI or binaryData cannot be null");
        }
        this.uri = uri;
        this.binaryData = binaryData;
        this.text = null;
        this.metadata = new HashMap<>();
    }


    @Override
    public String setMetadataValue(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Metadata key cannot be null or empty");
        }
        return metadata.put(key,value);
    }

    @Override
    public String getMetadataValue(String key) {
        if (key == null || key.isBlank()){
            throw new IllegalArgumentException("key cannot be null or empty");
        }
        return metadata.get(key);
    }

    @Override
    public HashMap<String, String> getMetadata() {
        // is a hard copy needed?
        return new HashMap<>(metadata);
    }

    @Override
    public String getDocumentTxt() {
        return this.text;
    }

    @Override
    public byte[] getDocumentBinaryData() {
        return this.binaryData;
    }

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
