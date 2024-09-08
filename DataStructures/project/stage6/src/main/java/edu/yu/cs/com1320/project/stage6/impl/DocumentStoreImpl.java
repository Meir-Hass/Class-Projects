package edu.yu.cs.com1320.project.stage6.impl;


import edu.yu.cs.com1320.project.BTree;
import edu.yu.cs.com1320.project.MinHeap;
import edu.yu.cs.com1320.project.Stack;
import edu.yu.cs.com1320.project.Trie;
import edu.yu.cs.com1320.project.impl.BTreeImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage6.Document;
import edu.yu.cs.com1320.project.stage6.DocumentStore;
import edu.yu.cs.com1320.project.undo.CommandSet;
import edu.yu.cs.com1320.project.undo.GenericCommand;
import edu.yu.cs.com1320.project.undo.Undoable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.function.Consumer;


public class DocumentStoreImpl implements DocumentStore {
    private final BTree<URI, Document> documentStore;
    private final Stack<Undoable> commandStack;

    private final Trie<URI> documentTrie;

    private final MinHeap<minHeapNode> documentMinHeap;

    private int MaxDocumentCount;

    private int MaxDocumentBytes;

    private int CurrentDocumentCount;

    private int CurrentDocumentBytes;
    private Set<URI> currentUris;
    private Map<URI, Map<String,String>> metaTracking;

    private class minHeapNode implements Comparable<minHeapNode> {
        private URI uri;


        //sees btree so it can compare docs via .getLastUseTime
        public minHeapNode(URI uri) {
            this.uri = uri;
        }

        public Document getDoc() {
            return documentStore.get(uri);
        }

        public URI getUri() {
            return this.uri;
        }


        @Override
        public int compareTo(minHeapNode other) {


            return this.getDoc().compareTo(other.getDoc());

        }

        public boolean equals(Object other) {
            if (other != null) {
                if (this.uri == ((minHeapNode) other).getUri()) {

                    return true;

                }
                return false;
            }
            return false;

        }
    }
    public DocumentStoreImpl(){
        this.documentStore = new BTreeImpl<>();
        this.commandStack = new StackImpl<>();
        this.documentTrie = new TrieImpl<>();
        this.MaxDocumentCount = Integer.MAX_VALUE;
        this.MaxDocumentBytes = Integer.MAX_VALUE;
        this.documentMinHeap = new MinHeapImpl<>();
        this.CurrentDocumentCount = 0;
        this.CurrentDocumentBytes = 0;
        this.currentUris = new HashSet<>();
        this.metaTracking = new HashMap<>();
        documentStore.setPersistenceManager(new DocumentPersistenceManager(null));
    }
    public DocumentStoreImpl(File baseDir){
        this.documentStore = new BTreeImpl<>();
        this.commandStack = new StackImpl<>();
        this.documentTrie = new TrieImpl<>();
        this.MaxDocumentCount = Integer.MAX_VALUE;
        this.MaxDocumentBytes = Integer.MAX_VALUE;
        this.documentMinHeap = new MinHeapImpl<>();
        this.CurrentDocumentCount = 0;
        this.CurrentDocumentBytes = 0;
        this.currentUris = new HashSet<>();
        this.metaTracking = new HashMap<>();
        documentStore.setPersistenceManager(new DocumentPersistenceManager(baseDir));
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

    //stage 5 logic applied
    @Override
    public String setMetadata(URI uri, String key, String value) throws IOException{
        // checks
        if(uri == null|| uri.toString().isBlank() || key == null || key.isBlank()){
            throw new IllegalArgumentException("URI or key cannot be null or empty");
        }
        Document document = documentStore.get(uri);
        if (document == null){
            throw new IllegalArgumentException("No document found");
        }
        currentUris.add(uri);

        //get old metadata for undo
        String oldMetaData = document.getMetadataValue(key);
        Map<String,String> oldmetamap = document.getMetadata();

        //undo logic
        Consumer<URI> undoLogic = (url) -> {
            String oldmetadata = document.setMetadataValue(key,oldMetaData);
            metaTracking.put(uri, oldmetamap );
            if (oldmetadata != null) {
                document.setLastUseTime(getNanoTime());
                documentMinHeap.reHeapify(new minHeapNode(uri));
            }
        };


        //add undo logic to the stack
        GenericCommand<URI> undo = new GenericCommand<>(uri, undoLogic);
        commandStack.push(undo);


        document.setLastUseTime(System.nanoTime());
        if(!currentUris.contains(document.getKey())){
            documentMinHeap.insert(new minHeapNode(document.getKey()));
        }
        documentMinHeap.reHeapify(new minHeapNode(document.getKey()));
        currentUris.add(document.getKey());
        enforceLimits();
        String Return = document.setMetadataValue(key,value);
        metaTracking.put(uri, document.getMetadata());
        return Return;

    }

    /**
     * get the value corresponding to the given metadata key for the document at the given uri
     *
     * @param uri
     * @param key
     * @return the value, or null if there was no value
     * @throws IllegalArgumentException if the uri is null or blank, if there is no document stored at that uri, or if the key is null or blank
     */

    //stage 5 logic applied
    @Override
    public String getMetadata(URI uri, String key) throws IOException{
        if(uri == null|| uri.toString().isBlank() || key == null || key.isBlank()){
            throw new IllegalArgumentException("URI or key cannot be null or empty");
        }
        Document document = documentStore.get(uri);
        if (document == null) {
            throw new IllegalArgumentException("No document Found");
        }
        currentUris.add(uri);
        document.setLastUseTime(System.nanoTime());
        documentMinHeap.reHeapify(new minHeapNode(document.getKey()));
        enforceLimits();
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

    //stage 5 logic applied
    //work on cleanup
    @Override
    public int put(InputStream input, URI url, DocumentFormat format) throws IOException {
        //checks
        if (url == null || url.toString().isEmpty() || format == null || this.MaxDocumentCount < 1){
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
            document = new DocumentImpl(url, text, null);
            //if the format is binary
        } else if (format == DocumentFormat.BINARY) {
            document = new DocumentImpl(url, data);
        }

        //make sure doc can fit
        if(getDocumentBytes(document) > this.MaxDocumentBytes ){
    throw new IllegalArgumentException();
        }


        //put the new doc in the store
        Document existingdoc = documentStore.put(url, document);
        currentUris.add(url);


        //if there was an existing doc
        if (existingdoc != null) {
            //remove from the heap
            existingdoc.setLastUseTime(Long.MIN_VALUE);
            documentMinHeap.reHeapify(new minHeapNode(existingdoc.getKey()));
            documentMinHeap.remove();
            //if it was a txt doc remove it from the trie
            if (existingdoc.getDocumentBinaryData() != null) {
                Set<String> oldwords = existingdoc.getWords();
                for (String word : oldwords) {
                    documentTrie.delete(word, existingdoc.getKey());
                }
            }
        }

        // if the new doc is a txt doc put it into the trie
        if (format == DocumentFormat.TXT ) {
            Set<String> words = document.getWords();
            for (String word : words) {
                documentTrie.put(word, document.getKey());
            }
        }

        //put document into heap
        documentMinHeap.insert(new minHeapNode(url));

        //update counts;
        updateCounts();

        //enforce limit
        enforceLimits();


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

    //stage 5 logic applied
    @Override
    public Document get(URI url) throws IOException {
        Document document = documentStore.get(url);
        if (document != null){
            document.setLastUseTime(System.nanoTime());
            if(!currentUris.contains(url)){
                documentMinHeap.insert(new minHeapNode(url));
                currentUris.add(url);
                updateCounts();
                enforceLimits();
            }
            documentMinHeap.reHeapify(new minHeapNode(document.getKey()));
        }
        return document;
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

        //undo logic to add the old doc back into the hashtable and trie if it has words(is a txt doc) and heap
        Consumer<URI> undoLogic = (uri) -> {

if(getMaxDocuments() > 0 && !(getDocumentBytes(existingDoc) > getMaxBytes())) {
    documentStore.put(url, existingDoc);
    currentUris.add(url);
    existingDoc.setLastUseTime(getNanoTime());
    documentMinHeap.insert(new minHeapNode(existingDoc.getKey()));
    updateCounts();
    enforceLimits();
    Set<String> words = existingDoc.getWords();
    if (!words.isEmpty()) {
        for (String word : words) {
            documentTrie.put(word, existingDoc.getKey());
        }
    }
} else {
    throw new IllegalArgumentException();
}
        };
        //add undo to stack
        GenericCommand<URI> undo = new GenericCommand<>(url, undoLogic);
        commandStack.push(undo);

        // delete doc from trie if txt doc
        Set<String> words = existingDoc.getWords();
        if (!words.isEmpty()) {
            for (String word : words) {
                documentTrie.delete(word, existingDoc.getKey());
            }
        }

        //remove from heap
        existingDoc.setLastUseTime(Long.MIN_VALUE);
        documentMinHeap.reHeapify(new minHeapNode(url));
        documentMinHeap.remove();
        currentUris.remove(url);
        boolean b = documentStore.put(url, null) != null;
        updateCounts();

        //should not be null bec there was an existing doc but its another check
        return b;
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

    //stage 5 logic applied
    @Override
    public List<Document> search(String keyword) throws IOException{
        if (keyword == null ) {
            throw new IllegalArgumentException();
        }
        Comparator<URI> descendingByKeyWordCount = (o1, o2) -> {
            int count1 = documentStore.get(o1).wordCount(keyword);
            int count2 = documentStore.get(o2).wordCount(keyword);
            return Integer.compare(count2, count1);
        };

        List<URI> docList = documentTrie.getSorted(keyword, descendingByKeyWordCount );
        List<Document> docstoreturn = new ArrayList<>();
        for(URI uri:docList){
            docstoreturn.add(documentStore.get(uri));
            if(!currentUris.contains(uri)){
                documentMinHeap.insert(new minHeapNode(uri));
            }
            currentUris.add(uri);
            documentStore.get(uri).setLastUseTime(System.nanoTime());
            documentMinHeap.reHeapify(new minHeapNode(uri));
            updateCounts();
            enforceLimits();
        }

        return docstoreturn;

    }

    /**
     * Retrieve all documents that contain text which starts with the given prefix
     * Documents are returned in sorted, descending order, sorted by the number of times the prefix appears in the document.
     * Search is CASE SENSITIVE.
     * @param keywordPrefix
     * @return a List of the matches. If there are no matches, return an empty list.
     */

    //stage 5 logic applied
    @Override
    public List<Document> searchByPrefix(String keywordPrefix) throws IOException{
        if (keywordPrefix == null) {
            throw new IllegalArgumentException();
        }
        Comparator<URI> descendingByPrefixCount = (o1, o2) -> {
            int count1 = countPrefix(documentStore.get(o1), keywordPrefix);
            int count2 = countPrefix(documentStore.get(o2), keywordPrefix);
            return Integer.compare(count2, count1);
        };

        List<URI> docList = documentTrie.getAllWithPrefixSorted(keywordPrefix, descendingByPrefixCount );
        List<Document> docstoreturn = new ArrayList<>();
        for(URI uri :docList){
            docstoreturn.add(documentStore.get(uri));
            if(!currentUris.contains(uri)){
                documentMinHeap.insert(new minHeapNode(uri));
            }
            currentUris.add(uri);
            documentStore.get(uri).setLastUseTime(System.nanoTime());
            documentMinHeap.reHeapify(new minHeapNode(uri));
            updateCounts();
            enforceLimits();
        }

        return docstoreturn;
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
        Set<URI> docstodeleteuris = documentTrie.deleteAll(keyword);
        Set<Document>docstodelete = new HashSet<>();
        for(URI uri: docstodeleteuris){
            docstodelete.add(documentStore.get(uri));
        }

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
        Set<URI> docstodeleteuri = documentTrie.deleteAllWithPrefix(keywordPrefix);
        Set<Document> docstodelete = new HashSet<>();
        for(URI uri: docstodeleteuri){
            docstodelete.add(documentStore.get(uri));
        }
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
    public List<Document> searchByMetadata(Map<String,String> keysValues) throws IOException {
        if (keysValues == null) {
            throw new IllegalArgumentException();
        }
        ArrayList<Document> list = new ArrayList<>();
        if (keysValues.isEmpty()) {
            return list;
        }
        Set<String> keyset = keysValues.keySet();
        Collection<URI> uriSet = metaTracking.keySet();

        for (URI uri : uriSet) {
            boolean containsAll = true;
            for (String key : keyset) {
                if (!Objects.equals(keysValues.get(key), metaTracking.get(uri).get(key))) {
                    containsAll = false;
                    break;
                }

            }
            if (containsAll) {
                Document doc = documentStore.get(uri);
                list.add(doc);
                if(!currentUris.contains(uri)){
                    documentMinHeap.insert(new minHeapNode(uri));
                }
                doc.setLastUseTime(System.nanoTime());
                documentMinHeap.reHeapify(new minHeapNode(uri));
                currentUris.add(uri);
                updateCounts();
                enforceLimits();

            }


    }
        for(Document document:list){
            currentUris.add(document.getKey());
            document.setLastUseTime(System.nanoTime());
            documentMinHeap.reHeapify(new minHeapNode(document.getKey()));
            updateCounts();
            enforceLimits();
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

    //stage 5 logic applied
    @Override
    public List<Document> searchByKeywordAndMetadata(String keyword, Map<String,String> keysValues) throws IOException{
        if (keyword == null || keysValues == null) {
            throw new IllegalArgumentException();
        }
        Set<URI> metadocs = searchForMetadataUris(keysValues);
        Set<URI> worddocs = documentTrie.get(keyword);
        List<URI> matches = new ArrayList<>(metadocs);
        matches.retainAll(worddocs);
        List<Document> matchingDocs= new ArrayList<>();
        for(URI uri: matches){
            matchingDocs.add(documentStore.get(uri));
        }
        Comparator<Document> descendingByKeyWordCount = (o1, o2) -> {
            int count1 = o1.wordCount(keyword);
            int count2 = o2.wordCount(keyword);
            return Integer.compare(count2, count1);
        };
        matchingDocs.sort(descendingByKeyWordCount);
        for(Document document:matchingDocs){
            if(!currentUris.contains(document.getKey())){
                documentMinHeap.insert(new minHeapNode(document.getKey()));
            }
            currentUris.add(document.getKey());
            document.setLastUseTime(System.nanoTime());
            documentMinHeap.reHeapify(new minHeapNode(document.getKey()));
            updateCounts();
            enforceLimits();
        }
        return matchingDocs;

    }

    private Set<URI> searchForMetadataUris ( Map<String,String> keysValues){
        Set <URI> uriToReturn = new HashSet<>();
        Set<String> keyset = keysValues.keySet();
        Collection<URI> uriSet = metaTracking.keySet();

        for (URI uri: uriSet) {
            boolean containsAll = true;
            for (String key : keyset) {
                if (!Objects.equals(keysValues.get(key), metaTracking.get(uri).get(key))) {
                    containsAll = false;
                    break;
                }

            }
            if (containsAll) {
                uriToReturn.add(uri);

            }

        }
        return uriToReturn;
    }

    /**
     * Retrieve all documents that contain text which starts with the given prefix AND which has the given key-value pairs in its metadata
     * Documents are returned in sorted, descending order, sorted by the number of times the prefix appears in the document.
     * Search is CASE SENSITIVE.
     * @param keywordPrefix
     * @return a List of the matches. If there are no matches, return an empty list.
     */

    //stage 5 logic applied
    @Override
    public List<Document> searchByPrefixAndMetadata(String keywordPrefix,Map<String,String> keysValues) throws IOException{
        if (keywordPrefix == null || keysValues == null) {
            throw new IllegalArgumentException();
        }
        Set<URI> metadocs = searchForMetadataUris(keysValues);
        List<URI> prefixdocs = documentTrie.getAllWithPrefixSorted(keywordPrefix,null);
        List<URI> matchingUris = new ArrayList<>(metadocs);
        matchingUris.retainAll(prefixdocs);
        Comparator<Document> descendingByPrefixCount = (o1, o2) -> {
            int count1 = countPrefix(o1, keywordPrefix);
            int count2 = countPrefix(o2, keywordPrefix);
            return Integer.compare(count2, count1);
        };
        List<Document> matches = new ArrayList<>();

        for(URI uri: matchingUris){
            matches.add(documentStore.get(uri));
        }

        matches.sort(descendingByPrefixCount);
        for(Document document:matches){
            if(!currentUris.contains(document.getKey())){
                documentMinHeap.insert(new minHeapNode(document.getKey()));
            }
            currentUris.add(document.getKey());
            document.setLastUseTime(System.nanoTime());
            documentMinHeap.reHeapify(new minHeapNode(document.getKey()));
            updateCounts();
            enforceLimits();
        }

        return matches;

    }

    /**
     * Completely remove any trace of any document which has the given key-value pairs in its metadata
     * Search is CASE SENSITIVE.
     * @return a Set of URIs of the documents that were deleted.
     */
    @Override
    public Set<URI> deleteAllWithMetadata(Map<String,String> keysValues) throws IOException{
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
    public Set<URI> deleteAllWithKeywordAndMetadata(String keyword,Map<String,String> keysValues) throws IOException{
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
    public Set<URI> deleteAllWithPrefixAndMetadata(String keywordPrefix,Map<String,String> keysValues) throws IOException{
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


    //stage 5 logic applied
    private GenericCommand<URI> getPutUndoLogic(URI url, Document existingdoc, Document document) {

        //create undo logic
        Consumer<URI> undoLogic = (uri) -> {


            if (existingdoc != null) {
                //meaning it has to be put back in
                if(getDocumentBytes(existingdoc) > getMaxBytes() || getMaxDocuments() < 1){
                    throw new IllegalArgumentException();
                }

                //add the doc back into the hashtable effectively removing the old one
                documentStore.put(url, existingdoc);

                //put the doc back into the trie
                Set<String> Oldwords = existingdoc.getWords();
                if (!Oldwords.isEmpty()) {
                    for (String word : Oldwords) {
                        documentTrie.put(word, existingdoc.getKey());
                    }
                }

                //remove the doc that was put from the trie
                Set<String> words = document.getWords();
                if (!words.isEmpty()) {
                    for (String word : words) {
                        documentTrie.delete(word, document.getKey());
                    }
                }

                //remove the document from the heap
                document.setLastUseTime(Long.MIN_VALUE);
                documentMinHeap.reHeapify(new minHeapNode(document.getKey()));
                documentMinHeap.remove();


                //insert the original doc into the heap
                existingdoc.setLastUseTime(getNanoTime());
                documentMinHeap.insert(new minHeapNode(existingdoc.getKey()));
                //update current counts and enforce limits
                updateCounts();
                enforceLimits();


            //there is no old doc to put back in and there is a doc that exists with the uri
            } else if(!(documentStore.get(url) == null)){
                //remove the doc from the trie
                Set<String> words = document.getWords();
                if (!words.isEmpty()) {
                    for (String word : words) {
                        documentTrie.delete(word, document.getKey());
                    }

                }
                //remove the doc from the heap
                currentUris.remove(url);
                document.setLastUseTime(Long.MIN_VALUE);
                documentMinHeap.reHeapify(new minHeapNode(document.getKey()));
                documentMinHeap.remove();
                //remove the doc from the hashmap
                documentStore.put(url, null);
                updateCounts();
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
        deleteAllTracesFromTrie(docstodelete);
        // get the uri for all the deleted docs
        for (Document doc : docstodelete) {
           URI uri = doc.getKey();
            // undo logic
            Consumer<URI> undoLogic = getDeleteAllUndoLogic(doc, uri);


            GenericCommand<URI> undo = new GenericCommand<>(uri, undoLogic);
            deletedURIs.addCommand(undo);

            deletedUri.add(uri);
        }

        // delete the doc from the hashtable and heap
        for (URI uri : deletedUri) {
            Document doc = documentStore.get(uri);
            if(currentUris.contains(doc.getKey())) {
                doc.setLastUseTime(Long.MIN_VALUE);
                documentMinHeap.reHeapify(new minHeapNode(doc.getKey()));
                documentMinHeap.remove();
            }
            currentUris.remove(uri);
            documentStore.put(uri, null);
            updateCounts();

        }

        commandStack.push(deletedURIs);
    }

    private Consumer<URI> getDeleteAllUndoLogic(Document doc, URI uri) {
        Consumer<URI> undoLogic = (url) -> {
            if(getDocumentBytes(doc) > getMaxBytes() || getMaxDocuments() < 1){
                throw new IllegalArgumentException();
            }

            Set<String> words = doc.getWords();
            for (String word : words) {
                documentTrie.put(word, doc.getKey()); // Restore the document in the trie
            }
            doc.setLastUseTime(getNanoTime());
            documentStore.put(uri, doc); // Restore the document in the hashtable
            currentUris.add(uri);
            documentMinHeap.insert(new minHeapNode(doc.getKey()));
            documentMinHeap.reHeapify(new minHeapNode(doc.getKey()));
            updateCounts();
            enforceLimits();

        };
        return undoLogic;
    }


    //iterate through all deleted docs, get their words and delete that doc from the given word in the trie
    private void deleteAllTracesFromTrie(Set<Document> docstodelete) {
        for (Document doc: docstodelete) {
            URI uri = doc.getKey();
            Set<String> words = doc.getWords();
            for (String word : words) {
                documentTrie.delete(word, uri);

            }
        }
    }


    //**********STAGE 5 ADDITIONS

    /**
     * set maximum number of documents that may be stored
     * @param limit
     * @throws IllegalArgumentException if limit < 1
     */
    public void setMaxDocumentCount(int limit) {
        this.MaxDocumentCount = limit;
        enforceLimits();
    }

    /**
     * set maximum number of bytes of memory that may be used by all the documents in memory combined
     * @param limit
     * @throws IllegalArgumentException if limit < 1
     */
    public void setMaxDocumentBytes(int limit){
        this.MaxDocumentBytes = limit;
        enforceLimits();


}

//stage5 private methods

    private int getDocumentBytes(Document document) {

        if (document.getDocumentTxt() != null) {
            return document.getDocumentTxt().getBytes().length;
        } else if (document.getDocumentBinaryData() != null) {
            return document.getDocumentBinaryData().length;
        }
        throw new IllegalArgumentException();
    }

    private void enforceLimits(){
        while (MaxDocumentCount < CurrentDocumentCount || MaxDocumentBytes < CurrentDocumentBytes){
            if(currentUris.isEmpty()){
                break;
            }
                    Document doctoerase = documentMinHeap.remove().getDoc();
            try {
                currentUris.remove(doctoerase.getKey());
                documentStore.moveToDisk(doctoerase.getKey());;
                CurrentDocumentCount--;
                CurrentDocumentBytes -= getDocumentBytes(doctoerase);
            } catch (Exception ignored){
            }
        }
    }





    private int getMaxDocuments(){
        return this.MaxDocumentCount;
    }

    private int getMaxBytes(){
        return this.MaxDocumentBytes;
    }

    private long getNanoTime(){
        return System.nanoTime();
    }

    private void updateCounts(){
        int docCount= 0;
        int byteCount=0;
        for (URI uri: currentUris){
            Document doc = documentStore.get(uri);
            if(doc != null) {
                docCount++;
                byteCount += (getDocumentBytes(doc));
            }
            this.CurrentDocumentCount=docCount;
            this.CurrentDocumentBytes=byteCount;
        }
    }
}



