package edu.yu.cs.com1320.project.stage3.impl;

import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.Stack;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.stage3.Document;
import edu.yu.cs.com1320.project.stage3.DocumentStore;
import edu.yu.cs.com1320.project.undo.Command;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Consumer;


public class DocumentStoreImpl implements DocumentStore {
    private final HashTable<URI, Document> documentStore;
    private final Stack<Command> commandStack;



    public DocumentStoreImpl(){
        this.documentStore = new HashTableImpl<>();
        this.commandStack = new StackImpl<Command>();
    }
    /**
     * set the given key-value metadata pair for the document at the given uri
     *
     * @param uri
     * @param key
     * @param value
     * @return the old value, or null if there was no previous value
     * @throws IllegalArgumentException if the uri is null or blank, if there is no document stored at that uri, or if the key is null or blank
     */
    @Override
    public String setMetadata(URI uri, String key, String value) {
        if(uri == null|| uri.toString().isBlank() || key == null || key.isBlank()){
            throw new IllegalArgumentException("URI or key cannot be null or empty");
        }
        Document document = documentStore.get(uri);
        if (document == null){
            throw new IllegalArgumentException("No document found");
        }
        String oldMetaData = document.getMetadataValue(key);

        Consumer<URI> undoLogic = (url) -> {
            document.setMetadataValue(key,oldMetaData);
        };

        Command undo = new Command(uri, undoLogic);
        commandStack.push(undo);

        return document.setMetadataValue(key, value);
    }

    /**
     * get the value corresponding to the given metadata key for the document at the given uri
     *
     * @param uri
     * @param key
     * @return the value, or null if there was no value
     * @throws IllegalArgumentException if the uri is null or blank, if there is no document stored at that uri, or if the key is null or blank
     */
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
     * @param url    unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return if there is no previous doc at the given URI, return 0. If there is a previous doc, return the hashCode of the previous doc. If InputStream is null, this is a delete, and thus return either the hashCode of the deleted doc or 0 if there is no doc to delete.
     * @throws IOException              if there is an issue reading input
     * @throws IllegalArgumentException if url or format are null
     */
    @Override
    public int put(InputStream input, URI url, DocumentFormat format) throws IOException {
        if (url == null || url.toString().isEmpty() || format == null){
            throw new IllegalArgumentException();
        }
        Document previousdoc = documentStore.get(url);
        if (input == null){
            if (previousdoc == null){
                return 0;
            } else {
                delete(url);
                return previousdoc.hashCode();
            }
        }
        byte[] data = input.readAllBytes();
        input.close();
        Document document = null;
        if (format == DocumentFormat.TXT){
            String text = new String(data);
            document = new DocumentImpl(url, text);
        } else if (format == DocumentFormat.BINARY) {
            document = new DocumentImpl(url, data);
        }

        Document existingdoc = documentStore.put(url, document);

        Consumer<URI> undoLogic = (uri) -> {
            if (existingdoc != null){
                documentStore.put(url, existingdoc);
            } else {
                documentStore.put(url, null);
            }
        };

        Command undo = new Command(url, undoLogic);
        commandStack.push(undo);

        if (existingdoc != null){
            return existingdoc.hashCode();
        } else{
            return 0;
        }
    }

    /**
     * @param url the unique identifier of the document to get
     * @return the given document
     */
    @Override
    public Document get(URI url) {
        return documentStore.get(url);
    }

    /**
     * @param url the unique identifier of the document to delete
     * @return true if the document is deleted, false if no document exists with that URI
     */
    @Override
    public boolean delete(URI url) {

        Document existingDoc = documentStore.get(url);

        Consumer<URI> undoLogic = (uri) -> {
            if (existingDoc != null){
                documentStore.put(url, existingDoc);
            }
        };

        Command undo = new Command(url, undoLogic);
        commandStack.push(undo);

        return documentStore.put(url, null) != null;
    }

    /**
     * undo the last put or delete command
     * @throws IllegalStateException if there are no actions to be undone, i.e. the command stack is empty
     */
    @Override
    public void undo() throws IllegalStateException{
        if (commandStack.size() == 0){
            throw new IllegalStateException();
        }
        Command lastCommand = commandStack.pop();
        lastCommand.undo();
    }

    /**
     * undo the last put or delete that was done with the given URI as its key
     * @param url
     * @throws IllegalStateException if there are no actions on the command stack for the given URI
     */

     @Override
    public void undo(URI url) throws IllegalStateException{
         Stack<Command> tempStack = new StackImpl<Command>();
    boolean uriFound = false;
        while(commandStack.size() > 0) {

            Command currentCommand = commandStack.pop();

            if (currentCommand.getUri().equals(url)) {
                uriFound = true;
                currentCommand.undo();
                break;
            } else {
                tempStack.push(currentCommand);
            }
        }

            while (tempStack.size() != 0){
                commandStack.push(tempStack.pop());
            }

           if(!uriFound){
               throw new IllegalStateException();
           }
        }
}

