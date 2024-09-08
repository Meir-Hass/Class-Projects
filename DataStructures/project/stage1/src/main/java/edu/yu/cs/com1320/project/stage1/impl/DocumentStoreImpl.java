package edu.yu.cs.com1320.project.stage1.impl;

import edu.yu.cs.com1320.project.stage1.Document;
import edu.yu.cs.com1320.project.stage1.DocumentStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;

public class DocumentStoreImpl implements DocumentStore {
    private final HashMap<URI, Document> documentStore;

    public DocumentStoreImpl(){
        this.documentStore = new HashMap<>();
    }

    @Override
    public String setMetadata(URI uri, String key, String value) {
        if(uri == null|| uri.toString().isBlank() || key == null || key.isBlank()){
            throw new IllegalArgumentException("URI or key cannot be null or empty");
        }
        Document document = documentStore.get(uri);
        if (document == null){
            throw new IllegalArgumentException("No document found");
        }
        return document.setMetadataValue(key, value);
    }

    @Override
    public String getMetadata(URI uri, String key) {
        if(uri == null|| uri.toString().isBlank() || key == null || key.isBlank()){
            throw new IllegalArgumentException("URI or key cannot be null or empty");
        }
        Document document = documentStore.get(uri);
        if (document == null) {
            throw new IllegalArgumentException("No document Found");
        }
        return document.getMetadataValue(key);

    }
    /**
     * @param input  the document being put
     * @param uri    unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return if there is no previous doc at the given URI, return 0. If there is a previous doc, return the hashCode of the previous doc. If InputStream is null, this is a delete, and thus return either the hashCode of the deleted doc or 0 if there is no doc to delete.
     * @throws IOException              if there is an issue reading input
     * @throws IllegalArgumentException if uri is null or empty, or format is null
     */
    @Override
    public int put(InputStream input, URI uri, DocumentFormat format) throws IOException {
        if (uri == null || uri.toString().isEmpty() || format == null){
            throw new IllegalArgumentException();
        }
        Document previousdoc = documentStore.get(uri);
        if (input == null){
            if (previousdoc == null){
                return 0;
            } else {
                delete(uri);
                return previousdoc.hashCode();
            }
        }
        byte[] data = input.readAllBytes();
        input.close();
        Document document = null;
        if (format == DocumentFormat.TXT){
            String text = new String(data);
            document = new DocumentImpl(uri, text);
        } else if (format == DocumentFormat.BINARY) {
                document = new DocumentImpl(uri, data);
            }

        Document existingdoc = documentStore.put(uri, document);
            if (existingdoc != null){
                return existingdoc.hashCode();
            } else{
                return 0;
            }
    }

    @Override
    public Document get(URI url) {
        return documentStore.get(url);
    }

    @Override
    public boolean delete(URI url) {
        return documentStore.remove(url) != null;
    }
}