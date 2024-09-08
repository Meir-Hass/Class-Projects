package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.Stack;
import edu.yu.cs.com1320.project.Trie;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage4.Document;
import edu.yu.cs.com1320.project.stage4.DocumentStore;
import edu.yu.cs.com1320.project.undo.CommandSet;
import edu.yu.cs.com1320.project.undo.GenericCommand;
import edu.yu.cs.com1320.project.undo.Undoable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.function.Consumer;


public class DocumentStoreImpl implements DocumentStore {
    private final HashTable<URI, Document> documentStore;
    private final Stack<Undoable> commandStack;

    private final Trie<Document> documentTrie;



    public DocumentStoreImpl(){
        this.documentStore = new HashTableImpl<>();
        this.commandStack = new StackImpl<>();
        this.documentTrie = new TrieImpl<>();
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
        // checks
        if(uri == null|| uri.toString().isBlank() || key == null || key.isBlank()){
            throw new IllegalArgumentException("URI or key cannot be null or empty");
        }
        Document document = documentStore.get(uri);
        if (document == null){
            throw new IllegalArgumentException("No document found");
        }

        //get old metadata for undo
        String oldMetaData = document.getMetadataValue(key);
        //undo logic
        Consumer<URI> undoLogic = (url) -> {
            document.setMetadataValue(key,oldMetaData);
        };
        //add undo logic to the stack
        GenericCommand<URI> undo = new GenericCommand<>(uri, undoLogic);
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
        //checks
        if (url == null || url.toString().isEmpty() || format == null) {
            throw new IllegalArgumentException();
        }


        Document previousdoc = documentStore.get(url);
        // null indicates delete
        if (input == null) {
            if (previousdoc == null) {
                return 0;
            } else {
                delete(url);
                return previousdoc.hashCode();
            }
        }

        // normal case

        //read data
        byte[] data = input.readAllBytes();
        input.close();
        //create a document object
        Document document = null;

        //if the format is txt create a text document
        if (format == DocumentFormat.TXT) {
            String text = new String(data);
            //create the txt document
            document = new DocumentImpl(url, text);
        //if the format is binary
        } else if (format == DocumentFormat.BINARY) {
            document = new DocumentImpl(url, data);
        }


        //put the new doc in the store
        Document existingdoc = documentStore.put(url, document);


        //if there was an existing doc
        if (existingdoc != null) {
        //if it was a txt doc remove it from the trie
            if (existingdoc.getDocumentBinaryData() != null) {
                Set<String> oldwords = existingdoc.getWords();
                for (String word : oldwords) {
                    documentTrie.delete(word, existingdoc);
                }
            }
        }
        // if the new doc is a txt doc
        if (format == DocumentFormat.TXT ) {
            Set<String> words = document.getWords();
            for (String word : words) {
                documentTrie.put(word, document);
            }
        }
        //get and push undo logic onto stack
        GenericCommand<URI> undo = getPutUndoLogic(url, existingdoc, document);
        commandStack.push(undo);

            if (existingdoc != null) {
                return existingdoc.hashCode();
            } else {
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

        //get the doc to delete if it exists
        Document existingDoc = documentStore.get(url);
        if (existingDoc == null){
            return false;
        }

        //undo logic to add the old doc back into the hashtable and trie if it has words(is a txt doc)
        Consumer<URI> undoLogic = (uri) -> {
            documentStore.put(url, existingDoc);
            Set<String> words = existingDoc.getWords();
            if (!words.isEmpty()) {
                for (String word : words) {
                    documentTrie.put(word, existingDoc);
                }
            }
        };
        //add undo to stack
        GenericCommand<URI> undo = new GenericCommand<>(url, undoLogic);
        commandStack.push(undo);

        // delete doc from trie if txt doc
        Set<String> words = existingDoc.getWords();
        if (!words.isEmpty()) {
            for (String word : words) {
                documentTrie.delete(word, existingDoc);
            }
        }

        //should not be null bec there was an existing doc but its another check
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
        Undoable lastCommand = commandStack.pop();
        lastCommand.undo();
    }

    /**
     * undo the last put or delete that was done with the given URI as its key
     * @param url
     * @throws IllegalStateException if there are no actions on the command stack for the given URI
     */

    @Override
    public void undo(URI url) throws IllegalStateException {
        //create temp stack
        Stack<Undoable> tempStack = new StackImpl<>();

        boolean uriFound = false;

        while (commandStack.size() > 0) {
            //pop the top cmd
            Undoable currentCommand = commandStack.pop();

            if (currentCommand instanceof GenericCommand) {
                //cast to a new cmd
                GenericCommand<URI> genericCommand = (GenericCommand<URI>) currentCommand;
                //if URI's match
                if (genericCommand.getTarget().equals(url)) {
                    uriFound = true;
                    //undo
                    currentCommand.undo();
                    //jump to restore stack
                    break;

                } else {
                    //move to temp stack
                    tempStack.push(currentCommand);
                }
                // if it's a cmd set
            } else if (currentCommand instanceof CommandSet) {
                CommandSet<URI> commandSet = (CommandSet<URI>) currentCommand;
                //if uri is found in the set
                if (commandSet.containsTarget(url)) {
                    uriFound = true;
                    commandSet.undo(url);

                    //if set is empty don't put it back on stack
                    if (commandSet.isEmpty()) {
                        continue;

                    } else {
                        tempStack.push(currentCommand);
                    }
                    //jump to restore the stack
                    break;
                    //uri not found in this pop. Move on...
                } else {
                    tempStack.push(currentCommand);
                }
            }
        }

        //restore the stack
        while (tempStack.size() != 0) {
            commandStack.push(tempStack.pop());
        }

        if (!uriFound) {
            throw new IllegalStateException();
        }
    }
    //**********STAGE 4 ADDITIONS

    /**
     * Retrieve all documents whose text contains the given keyword.
     * Documents are returned in sorted, descending order, sorted by the number of times the keyword appears in the document.
     * Search is CASE SENSITIVE.
     * @param keyword
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    @Override
    public List<Document> search(String keyword){
        if (keyword == null ) {
            throw new IllegalArgumentException();
        }
        Comparator<Document> descendingByKeyWordCount = (o1, o2) -> {
            int count1 = o1.wordCount(keyword);
            int count2 = o2.wordCount(keyword);
            return Integer.compare(count2, count1);
        };

        return documentTrie.getSorted(keyword, descendingByKeyWordCount );

    }

    /**
     * Retrieve all documents that contain text which starts with the given prefix
     * Documents are returned in sorted, descending order, sorted by the number of times the prefix appears in the document.
     * Search is CASE SENSITIVE.
     * @param keywordPrefix
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    @Override
    public List<Document> searchByPrefix(String keywordPrefix){
        if (keywordPrefix == null) {
            throw new IllegalArgumentException();
        }
        Comparator<Document> descendingByPrefixCount = (o1, o2) -> {
            int count1 = countPrefix(o1, keywordPrefix);
            int count2 = countPrefix(o2, keywordPrefix);
            return Integer.compare(count2, count1);
        };

        return documentTrie.getAllWithPrefixSorted(keywordPrefix, descendingByPrefixCount );


    }

    /**
     * Completely remove any trace of any document which contains the given keyword
     * Search is CASE SENSITIVE.
     * @param keyword
     * @return a Set of URIs of the documents that were deleted.
     */
    @Override
    public Set<URI> deleteAll(String keyword) {
        if (keyword == null) {
            throw new IllegalArgumentException();
        }
        //delete all docs at said keyword and get a set of all deleted docs
        Set<Document> docstodelete = documentTrie.deleteAll(keyword);
        if(docstodelete.isEmpty()){
            return Collections.emptySet();
        }
        //make a set to store uris of deleted documents
        Set<URI> deletedUri = new HashSet<>();
        //make a set of undo commands
        CommandSet<URI> deletedURIs = new CommandSet<>();

        //iterate through all deleted docs, get their words and delete that doc from the given word in the trie
        deleteAndGetUndoLogic(docstodelete, deletedURIs, deletedUri);
        // return the set of deleted docs
            return deletedUri;
        }




    /**
     * Completely remove any trace of any document which contains a word that has the given prefix
     * Search is CASE SENSITIVE.
     * @param keywordPrefix
     * @return a Set of URIs of the documents that were deleted.
     */
    @Override
    public Set<URI> deleteAllWithPrefix(String keywordPrefix){
        if (keywordPrefix == null) {
            throw new IllegalArgumentException();
        }
        //delete all docs at said keyword and get a set of all deleted docs
        Set<Document> docstodelete = documentTrie.deleteAllWithPrefix(keywordPrefix);
        if(docstodelete.isEmpty()){
            return Collections.emptySet();
        }
        //make a set to store uris of deleted documents
        Set<URI> deletedUri = new HashSet<>();
        //make a set of undo commands
        CommandSet<URI> deletedURIs = new CommandSet<>();

        deleteAndGetUndoLogic(docstodelete, deletedURIs, deletedUri);
        // return the set of deleted docs
        return deletedUri;
    }




    /**
     * @param keysValues metadata key-value pairs to search for
     * @return a List of all documents whose metadata contains ALL OF the given values for the given keys. If no documents contain all the given key-value pairs, return an empty list.
     */
    @Override
    public List<Document> searchByMetadata(Map<String,String> keysValues) {
        if (keysValues == null) {
            throw new IllegalArgumentException();
        }
        ArrayList<Document> list = new ArrayList<>();
        if(keysValues.isEmpty()){
            return list;
        }
        Set<String> keyset = keysValues.keySet();
        Collection<Document> valueSet = this.documentStore.values();

        for (Document doc : valueSet) {
            boolean containsAll = true;
            for (String key : keyset) {
                if (!Objects.equals(keysValues.get(key), doc.getMetadataValue(key))) {
                    containsAll = false;
                    break;
                }

            }
            if (containsAll) {
                list.add(doc);

            }

        }
        return list;
    }




    /**
     * Retrieve all documents whose text contains the given keyword AND which has the given key-value pairs in its metadata
     * Documents are returned in sorted, descending order, sorted by the number of times the keyword appears in the document.
     * Search is CASE SENSITIVE.
     * @param keyword
     * @param keysValues
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    @Override
    public List<Document> searchByKeywordAndMetadata(String keyword, Map<String,String> keysValues){
        if (keyword == null || keysValues == null) {
            throw new IllegalArgumentException();
        }
    List<Document> metadocs = searchByMetadata(keysValues);
    List<Document> worddocs = search(keyword);
    List<Document> matches = new ArrayList<>(metadocs);
    matches.retainAll(worddocs);
    Comparator<Document> descendingByKeyWordCount = (o1, o2) -> {
            int count1 = o1.wordCount(keyword);
            int count2 = o2.wordCount(keyword);
            return Integer.compare(count2, count1);
        };
    matches.sort(descendingByKeyWordCount);

    return matches;

    }

    /**
     * Retrieve all documents that contain text which starts with the given prefix AND which has the given key-value pairs in its metadata
     * Documents are returned in sorted, descending order, sorted by the number of times the prefix appears in the document.
     * Search is CASE SENSITIVE.
     * @param keywordPrefix
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    @Override
    public List<Document> searchByPrefixAndMetadata(String keywordPrefix,Map<String,String> keysValues){
        if (keywordPrefix == null || keysValues == null) {
            throw new IllegalArgumentException();
        }
        List<Document> metadocs = searchByMetadata(keysValues);
        List<Document> prefixdocs = searchByPrefix(keywordPrefix);
        List<Document> matches = new ArrayList<>(metadocs);
        matches.retainAll(prefixdocs);
        Comparator<Document> descendingByPrefixCount = (o1, o2) -> {
            int count1 = countPrefix(o1, keywordPrefix);
            int count2 = countPrefix(o2, keywordPrefix);
            return Integer.compare(count2, count1);
        };
        matches.sort(descendingByPrefixCount);

        return matches;

    }

    /**
     * Completely remove any trace of any document which has the given key-value pairs in its metadata
     * Search is CASE SENSITIVE.
     * @return a Set of URIs of the documents that were deleted.
     */
    @Override
    public Set<URI> deleteAllWithMetadata(Map<String,String> keysValues){
        if (keysValues == null) {
            throw new IllegalArgumentException();
        }
        //delete all docs at said keyword and get a set of all deleted docs
        List<Document> results = searchByMetadata(keysValues);
        Set<Document> docstodelete = new HashSet<>(results);
        if(docstodelete.isEmpty()){
            return Collections.emptySet();
        }
        //make a set to store uris of deleted documents
        Set<URI> deletedUri = new HashSet<>();
        //make a set of undo commands
        CommandSet<URI> deletedURIs = new CommandSet<>();

        //iterate through all deleted docs, get their words and delete that doc from the given word in the trie
        deleteAndGetUndoLogic(docstodelete, deletedURIs, deletedUri);
        // return the set of deleted docs
        return deletedUri;
    }



    /**
     * Completely remove any trace of any document which contains the given keyword AND which has the given key-value pairs in its metadata
     * Search is CASE SENSITIVE.
     * @param keyword
     * @return a Set of URIs of the documents that were deleted.
     */
    @Override
    public Set<URI> deleteAllWithKeywordAndMetadata(String keyword,Map<String,String> keysValues){
        if (keyword == null || keysValues == null) {
            throw new IllegalArgumentException();
        }
        //delete all docs at said keyword and get a set of all deleted docs
        List<Document> results = searchByKeywordAndMetadata (keyword, keysValues);
        Set<Document> docstodelete = new HashSet<>(results);
        if(docstodelete.isEmpty()){
            return Collections.emptySet();
        }
        //make a set to store uris of deleted documents
        Set<URI> deletedUri = new HashSet<>();
        //make a set of undo commands
        CommandSet<URI> deletedURIs = new CommandSet<>();

        //iterate through all deleted docs, get their words and delete that doc from the given word in the trie
        deleteAndGetUndoLogic(docstodelete, deletedURIs, deletedUri);
        // return the set of deleted docs
        return deletedUri;
    }

    /**
     * Completely remove any trace of any document which contains a word that has the given prefix AND which has the given key-value pairs in its metadata
     * Search is CASE SENSITIVE.
     * @param keywordPrefix
     * @return a Set of URIs of the documents that were deleted.
     */
    @Override
    public Set<URI> deleteAllWithPrefixAndMetadata(String keywordPrefix,Map<String,String> keysValues){
        if (keywordPrefix == null || keysValues == null) {
            throw new IllegalArgumentException();
        }
//delete all docs at said keyword and get a set of all deleted docs
        List<Document> results = searchByPrefixAndMetadata (keywordPrefix, keysValues);
        Set<Document> docstodelete = new HashSet<>(results);
        if(docstodelete.isEmpty()){
            return Collections.emptySet();
        }
        //make a set to store uris of deleted documents
        Set<URI> deletedUri = new HashSet<>();
        //make a set of undo commands
        CommandSet<URI> deletedURIs = new CommandSet<>();

        //iterate through all deleted docs, get their words and delete that doc from the given word in the trie
        deleteAndGetUndoLogic(docstodelete, deletedURIs, deletedUri);
        // return the set of deleted docs
        return deletedUri;
    }



    // private methods

    private GenericCommand<URI> getPutUndoLogic(URI url, Document existingdoc, Document document) {
        Consumer<URI> undoLogic = (uri) -> {
            if (existingdoc != null) {
                Set<String> Oldwords = existingdoc.getWords();
                documentStore.put(url, existingdoc);
                if (!Oldwords.isEmpty()) {
                    for (String word : Oldwords) {
                        documentTrie.put(word, existingdoc);
                    }
                }
                Set<String> words = document.getWords();
                if (!words.isEmpty()) {
                    for (String word : words) {
                        documentTrie.delete(word, document);
                    }
                }

            } else {
                documentStore.put(url, null);
                Set<String> words = document.getWords();
                if (!words.isEmpty()) {
                    for (String word : words) {
                        documentTrie.delete(word, document);
                    }

                }
            }
        };

        GenericCommand<URI> undo = new GenericCommand<>(url, undoLogic);
        return undo;
    }



    private int countPrefix( Document document, String prefix){
        int counter = 0;
        Set<String> set = document.getWords();
        for (String word: set){
            if(word.startsWith(prefix)){
                counter += document.wordCount(word);
            }
        }
        return counter;
    }

    private void deleteAndGetUndoLogic(Set<Document> docstodelete, CommandSet<URI> deletedURIs, Set<URI> deletedUri) {
        //iterate through all deleted docs, get their words and delete that doc from the given word in the trie
        deleteAllTraces(docstodelete);
        // get the uri for all the deleted docs
        for (Document doc : docstodelete) {
            URI uri = doc.getKey();
            // undo logic
            Consumer<URI> undoLogic = getDeleteAllUndoLogic(doc, uri);


            GenericCommand<URI> undo = new GenericCommand<>(uri, undoLogic);
            deletedURIs.addCommand(undo);

            deletedUri.add(uri);
        }

        // delete the doc from the hashtable
        for (URI uri : deletedUri) {
            documentStore.put(uri, null);
        }
        commandStack.push(deletedURIs);
    }

    //iterate through all deleted docs, get their words and delete that doc from the given word in the trie
    private void deleteAllTraces(Set<Document> docstodelete) {
        for (Document doc : docstodelete) {
            Set<String> words = doc.getWords();
            for (String word : words) {
                documentTrie.delete(word, doc);

            }
        }
    }

    private Consumer<URI> getDeleteAllUndoLogic(Document doc, URI uri) {
        Consumer<URI> undoLogic = (url) -> {
            Set<String> words = doc.getWords();
            for (String word : words) {
                documentTrie.put(word, doc); // Restore the document in the trie
            }
            documentStore.put(uri, doc); // Restore the document in the hashtable
        };
        return undoLogic;
    }


}
