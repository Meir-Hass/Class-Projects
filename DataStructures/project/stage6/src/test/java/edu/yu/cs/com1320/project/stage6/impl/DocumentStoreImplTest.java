package edu.yu.cs.com1320.project.stage6.impl;

import edu.yu.cs.com1320.project.stage6.Document;
import edu.yu.cs.com1320.project.stage6.DocumentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DocumentStoreImplTest {
    private DocumentStoreImpl documentStore;
    private URI uriTxt;
    private URI uriBinary;

    @BeforeEach
    public void setUp() {
        documentStore = new DocumentStoreImpl();
        uriTxt = URI.create("http://www.example.com/document/txt");
        uriBinary = URI.create("http://www.example.com/document/binary");
    }

    @Test
    public void testPutAndGetTextDocument() throws IOException {
        String txt = "!txt Test  Test test   ";
        InputStream inputStream = new ByteArrayInputStream(txt.getBytes());
        int documenthashcode = documentStore.put(inputStream, uriTxt, DocumentStore.DocumentFormat.TXT);
        assertEquals(0, documenthashcode);
        Document document = documentStore.get(uriTxt);
        assertEquals(txt, document.getDocumentTxt());
        List<Document> docs = documentStore.search("txt");
        assertTrue(docs.contains(document));
        int count = document.wordCount("Test");
        assertEquals(2, count);

        Set words = document.getWords();
        assertEquals(3, words.size());
        assertTrue(words.contains("txt") && words.contains("Test"));
        documentStore.undo();
        Document undidDoc = documentStore.get(uriTxt);
        assertNull(undidDoc);




    }

    @Test
    public void testPutAndGetBinaryDocument() throws IOException {
        byte[] binaryData = {0, 1, 2, 3, 4};
        InputStream inputStream = new ByteArrayInputStream(binaryData);

        int hashCode = documentStore.put(inputStream, uriBinary, DocumentStore.DocumentFormat.BINARY);

        assertEquals(0, hashCode);
        Document document = documentStore.get(uriBinary);
        assertArrayEquals(binaryData, document.getDocumentBinaryData());
    }



    @Test public void testSetAndGetMetadata() throws IOException {
        String txt = "txt test";
        String key = "key";
        String value = "value";
        InputStream inputStream = new ByteArrayInputStream(txt.getBytes());
        documentStore.put(inputStream, uriTxt, DocumentStore.DocumentFormat.TXT);
        String oldmetadata = documentStore.setMetadata(uriTxt, key, value);
        assertEquals(null, oldmetadata);
        String insertedmetadata = documentStore.getMetadata(uriTxt, key);
        assertEquals("value", insertedmetadata);
        documentStore.undo();
        String insertedmetadata2 = documentStore.getMetadata(uriTxt, key);
        assertEquals(null, insertedmetadata2);


    }

    @Test public void deletedocument() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("Test Delete".getBytes());
        documentStore.put(inputStream, uriTxt, DocumentStore.DocumentFormat.TXT);
        Document document = documentStore.get(uriTxt);
        List<Document> results = documentStore.search("Delete");
        assertEquals(1, results.size());
        boolean deleted = documentStore.delete(uriTxt);
        assertTrue(deleted);
        List<Document> results2 = documentStore.search("Delete");
        assertEquals(0, results2.size());

        Document deletedDoc = documentStore.get(uriTxt);
        assertNull(deletedDoc);
        documentStore.undo();
        Document document2 = documentStore.get(uriTxt);
        assertEquals(document.getDocumentTxt(), document2.getDocumentTxt());
        List<Document> results3 = documentStore.search("Delete");
        assertEquals(1, results3.size());
    }

    @Test
    public void testDeleteDocumentUsingPutWithExistingDoc() throws IOException {
        String content = "delete";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());

        documentStore.put(inputStream, uriTxt, DocumentStore.DocumentFormat.TXT);
        assertNotNull(documentStore.get(uriTxt));
        List<Document> results = documentStore.search("delete");
        assertEquals(1, results.size());

        Document oldhashcode = documentStore.get(uriTxt);
        int hashCode = documentStore.put(null, uriTxt, DocumentStore.DocumentFormat.TXT);
        Document deletedDoc = documentStore.get(uriTxt);

        assertEquals(oldhashcode.hashCode(), hashCode);
        assertNull(deletedDoc);
        List<Document> results2 = documentStore.search("delete");
        assertEquals(0, results2.size());
        documentStore.undo();
        List<Document> results3 = documentStore.search("delete");
        assertEquals(1, results3.size());
        Document recoveredDoc = documentStore.get(uriTxt);
        assertEquals(oldhashcode, recoveredDoc);

    }

    @Test
    public void testDeleteDocumentUsingPutWithNoExistingDoc() throws IOException {

        Document oldhashcode = documentStore.get(uriTxt);
        assertNull(oldhashcode);
        int hashCode = documentStore.put(null, uriTxt, DocumentStore.DocumentFormat.TXT);
        assertEquals(0, hashCode);

    }

    @Test
    public void undoWithUri() throws IOException {
        String txt = "txt test";
        InputStream inputStream1 = new ByteArrayInputStream(txt.getBytes());
        int documenthashcode = documentStore.put(inputStream1, uriTxt, DocumentStore.DocumentFormat.TXT);

        byte[] binaryData = {0, 1, 2, 3, 4};
        InputStream inputStream2 = new ByteArrayInputStream(binaryData);

        int hashCode = documentStore.put(inputStream2, uriBinary, DocumentStore.DocumentFormat.BINARY);

        assertEquals(0, documenthashcode);
        Document document = documentStore.get(uriTxt);
        assertEquals(txt, document.getDocumentTxt());

        assertEquals(0, hashCode);
        Document document2 = documentStore.get(uriBinary);
        assertArrayEquals(binaryData, document2.getDocumentBinaryData());
        //make stack public for testing

        /* int stackSizeBeforeUndo = documentStore.commandStack.size();
        assertEquals(2, stackSizeBeforeUndo);
        //make table public for testing
        int tableSizeBeforeUndo = documentStore.documentStore.size();
        assertEquals(2, tableSizeBeforeUndo);

         */

        documentStore.undo(uriTxt);

        /*
        int stackSizeAfterUndo = documentStore.commandStack.size();
        int tableSizeAfterUndo = documentStore.documentStore.size();
        assertEquals(1, tableSizeAfterUndo);
        assertEquals(1, stackSizeAfterUndo);

         */


        assertNull(documentStore.get(uriTxt));


    }

    @Test
    public void searchByKeyword() throws IOException{

        URI uriTxt1 = URI.create("http://www.example.com/document/txt1");
        URI uriTxt2 = URI.create("http://www.example.com/document/txt2");
        URI uriTxt3 = URI.create("http://www.example.com/document/txt3");
        //put a text doc
        String txt1 = "Test test test";
        InputStream inputStream1 = new ByteArrayInputStream(txt1.getBytes());
        documentStore.put(inputStream1, uriTxt1, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt2 = "Tes!t Test    Test";
        InputStream inputStream2 = new ByteArrayInputStream(txt2.getBytes());
        documentStore.put(inputStream2, uriTxt2, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt3 = "test test test";
        InputStream inputStream3 = new ByteArrayInputStream(txt3.getBytes());
        documentStore.put(inputStream3, uriTxt3, DocumentStore.DocumentFormat.TXT);
        // put binary doc
        byte[] binaryData = {0, 1, 2, 3, 4};
        InputStream inputStream = new ByteArrayInputStream(binaryData);
        documentStore.put(inputStream, uriBinary, DocumentStore.DocumentFormat.BINARY);


        List<Document> results = documentStore.search("Test");
        assertEquals(2, results.size());
        Document doc1 = documentStore.get(uriTxt1);
        Document doc2 = documentStore.get(uriTxt2);
        assertEquals(doc2, results.get(0));
        assertEquals(doc1, results.get(1));

        List<Document> results2 = documentStore.search("What");
        assertTrue(results2.isEmpty());

    }

    @Test
    public void searchByPrefix() throws IOException{

        URI uriTxt1 = URI.create("http://www.example.com/document/txt1");
        URI uriTxt2 = URI.create("http://www.example.com/document/txt2");
        URI uriTxt3 = URI.create("http://www.example.com/document/txt3");
        //put a text doc
        String txt1 = "app Apple application ";
        InputStream inputStream1 = new ByteArrayInputStream(txt1.getBytes());
        documentStore.put(inputStream1, uriTxt1, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt2 = "App Apple Application";
        InputStream inputStream2 = new ByteArrayInputStream(txt2.getBytes());
        documentStore.put(inputStream2, uriTxt2, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt3 = "app apple application";
        InputStream inputStream3 = new ByteArrayInputStream(txt3.getBytes());
        documentStore.put(inputStream3, uriTxt3, DocumentStore.DocumentFormat.TXT);
        // put binary doc
        byte[] binaryData = {0, 1, 2, 3, 4};
        InputStream inputStream = new ByteArrayInputStream(binaryData);
        documentStore.put(inputStream, uriBinary, DocumentStore.DocumentFormat.BINARY);

        List<Document> results = documentStore.searchByPrefix("App");
        assertEquals(2, results.size());
        Document doc1 = documentStore.get(uriTxt1);
        Document doc2 = documentStore.get(uriTxt2);
        assertEquals(doc2, results.get(0));
        assertEquals(doc1, results.get(1));

    }

    @Test
    public void searchByMetadata() throws IOException{

        URI uriTxt1 = URI.create("http://www.example.com/document/txt1");
        URI uriTxt2 = URI.create("http://www.example.com/document/txt2");
        URI uriTxt3 = URI.create("http://www.example.com/document/txt3");
        //put a text doc
        String txt1 = "app Apple application ";
        InputStream inputStream1 = new ByteArrayInputStream(txt1.getBytes());
        documentStore.put(inputStream1, uriTxt1, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt2 = "App Apple Application";
        InputStream inputStream2 = new ByteArrayInputStream(txt2.getBytes());
        documentStore.put(inputStream2, uriTxt2, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt3 = "app apple application";
        InputStream inputStream3 = new ByteArrayInputStream(txt3.getBytes());
        documentStore.put(inputStream3, uriTxt3, DocumentStore.DocumentFormat.TXT);
        // put binary doc
        byte[] binaryData = {0, 1, 2, 3, 4};
        InputStream inputStream = new ByteArrayInputStream(binaryData);
        documentStore.put(inputStream, uriBinary, DocumentStore.DocumentFormat.BINARY);

        Document doc1 = documentStore.get(uriTxt1);
        Document doc2 = documentStore.get(uriTxt2);
        Document doc3 = documentStore.get(uriTxt3);
        Document doc4 = documentStore.get(uriBinary);


        Map<String, String> metadata = new HashMap<>();
        metadata.put("one","1");
        metadata.put("two","2");
        metadata.put("three","3");

        documentStore.setMetadata(uriTxt1, "one", "1");

        documentStore.setMetadata(uriTxt2, "one", "1");
        documentStore.setMetadata(uriTxt2, "two", "2");
        documentStore.setMetadata(uriTxt2, "Three", "3");

        documentStore.setMetadata(uriBinary, "one", "1");
        documentStore.setMetadata(uriBinary, "two", "2");
        documentStore.setMetadata(uriBinary, "three", "3");

        List<Document> results = documentStore.searchByMetadata(metadata);
        assertEquals(1, results.size());
        assertTrue(results.contains(doc4));

    }

    @Test
    public void searchByKeywordAndMetadata() throws IOException{

        URI uriTxt1 = URI.create("http://www.example.com/document/txt1");
        URI uriTxt2 = URI.create("http://www.example.com/document/txt2");
        URI uriTxt3 = URI.create("http://www.example.com/document/txt3");
        //put a text doc
        String txt1 = "Test test test";
        InputStream inputStream1 = new ByteArrayInputStream(txt1.getBytes());
        documentStore.put(inputStream1, uriTxt1, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt2 = "Tes!t found Test";
        InputStream inputStream2 = new ByteArrayInputStream(txt2.getBytes());
        documentStore.put(inputStream2, uriTxt2, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt3 = "test test test";
        InputStream inputStream3 = new ByteArrayInputStream(txt3.getBytes());
        documentStore.put(inputStream3, uriTxt3, DocumentStore.DocumentFormat.TXT);
        // put binary doc
        byte[] binaryData = {0, 1, 2, 3, 4};
        InputStream inputStream = new ByteArrayInputStream(binaryData);
        documentStore.put(inputStream, uriBinary, DocumentStore.DocumentFormat.BINARY);

        Document doc1 = documentStore.get(uriTxt1);
        Document doc2 = documentStore.get(uriTxt2);
        Document doc3 = documentStore.get(uriTxt3);
        Document doc4 = documentStore.get(uriBinary);


        Map<String, String> metadata = new HashMap<>();
        metadata.put("one","1");
        metadata.put("two","2");
        metadata.put("three","3");

        documentStore.setMetadata(uriTxt1, "one", "1");

        documentStore.setMetadata(uriTxt2, "one", "1");
        documentStore.setMetadata(uriTxt2, "two", "2");
        documentStore.setMetadata(uriTxt2, "three", "3");

        documentStore.setMetadata(uriBinary, "one", "1");
        documentStore.setMetadata(uriBinary, "two", "2");
        documentStore.setMetadata(uriBinary, "three", "3");


        List<Document> results = documentStore.searchByKeywordAndMetadata( "found", metadata);
        assertEquals(1, results.size());
        assertTrue(results.contains(doc2));

    }

    @Test
    public void searchByPrefixAndMetadata() throws IOException{

        URI uriTxt1 = URI.create("http://www.example.com/document/txt1");
        URI uriTxt2 = URI.create("http://www.example.com/document/txt2");
        URI uriTxt3 = URI.create("http://www.example.com/document/txt3");
        //put a text doc
        String txt1 = "app Apple application ";
        InputStream inputStream1 = new ByteArrayInputStream(txt1.getBytes());
        documentStore.put(inputStream1, uriTxt1, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt2 = "App Apple Application";
        InputStream inputStream2 = new ByteArrayInputStream(txt2.getBytes());
        documentStore.put(inputStream2, uriTxt2, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt3 = "app apple application";
        InputStream inputStream3 = new ByteArrayInputStream(txt3.getBytes());
        documentStore.put(inputStream3, uriTxt3, DocumentStore.DocumentFormat.TXT);
        // put binary doc
        byte[] binaryData = {0, 1, 2, 3, 4};
        InputStream inputStream = new ByteArrayInputStream(binaryData);
        documentStore.put(inputStream, uriBinary, DocumentStore.DocumentFormat.BINARY);


        Document doc1 = documentStore.get(uriTxt1);
        Document doc2 = documentStore.get(uriTxt2);
        Document doc3 = documentStore.get(uriTxt3);
        Document doc4 = documentStore.get(uriBinary);


        Map<String, String> metadata = new HashMap<>();
        metadata.put("one","1");
        metadata.put("two","2");
        metadata.put("three","3");

        documentStore.setMetadata(uriTxt1, "one", "1");

        documentStore.setMetadata(uriTxt2, "one", "1");
        documentStore.setMetadata(uriTxt2, "two", "2");
        documentStore.setMetadata(uriTxt2, "three", "3");

        documentStore.setMetadata(uriBinary, "one", "1");
        documentStore.setMetadata(uriBinary, "two", "2");
        documentStore.setMetadata(uriBinary, "three", "3");

        List<Document> results = documentStore.searchByPrefixAndMetadata( "App", metadata);
        assertEquals(1, results.size());
        assertTrue(results.contains(doc2));

    }


    @Test
    public void deleteAllWithKeyword() throws IOException{

        URI uriTxt1 = URI.create("http://www.example.com/document/txt1");
        URI uriTxt2 = URI.create("http://www.example.com/document/txt2");
        URI uriTxt3 = URI.create("http://www.example.com/document/txt3");
        //put a text doc
        String txt1 = "app Found application ";
        InputStream inputStream1 = new ByteArrayInputStream(txt1.getBytes());
        documentStore.put(inputStream1, uriTxt1, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt2 = "App Found Application";
        InputStream inputStream2 = new ByteArrayInputStream(txt2.getBytes());
        documentStore.put(inputStream2, uriTxt2, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt3 = "app apple application";
        InputStream inputStream3 = new ByteArrayInputStream(txt3.getBytes());
        documentStore.put(inputStream3, uriTxt3, DocumentStore.DocumentFormat.TXT);
        // put binary doc
        byte[] binaryData = {0, 1, 2, 3, 4};
        InputStream inputStream = new ByteArrayInputStream(binaryData);
        documentStore.put(inputStream, uriBinary, DocumentStore.DocumentFormat.BINARY);

        List<Document> results3 = documentStore.search("Found");
        assertEquals(2, results3.size());

        Set<URI> results = documentStore.deleteAll("Found");
        assertEquals(2,results.size());
        assertTrue(results.contains(uriTxt1));
        assertTrue(results.contains(uriTxt2));
        assertNull(documentStore.get(uriTxt1));
        assertNull(documentStore.get(uriTxt2));
        List<Document> results2 = documentStore.search("Found");
        assertEquals(0, results2.size());

        documentStore.undo();
        assertNotNull(documentStore.get(uriTxt1));
        assertNotNull(documentStore.get(uriTxt2));
        documentStore.deleteAll("Found");

        documentStore.undo(uriTxt1);
        assertNotNull(documentStore.get(uriTxt1));
        assertNull(documentStore.get(uriTxt2));

    }


    @Test
    public void deleteAllWithPrefix() throws IOException{

        URI uriTxt1 = URI.create("http://www.example.com/document/txt1");
        URI uriTxt2 = URI.create("http://www.example.com/document/txt2");
        URI uriTxt3 = URI.create("http://www.example.com/document/txt3");
        URI uriTxt4 = URI.create("http://www.example.com/document/txt4");
        //put a text doc
        String txt1 = "app Found Application ";
        InputStream inputStream1 = new ByteArrayInputStream(txt1.getBytes());
        documentStore.put(inputStream1, uriTxt1, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt2 = "App Found Application";
        InputStream inputStream2 = new ByteArrayInputStream(txt2.getBytes());
        documentStore.put(inputStream2, uriTxt2, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt3 = "app apple application";
        InputStream inputStream3 = new ByteArrayInputStream(txt3.getBytes());
        documentStore.put(inputStream3, uriTxt3, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt4 = "Found";
        InputStream inputStream4 = new ByteArrayInputStream(txt4.getBytes());
        documentStore.put(inputStream4, uriTxt4, DocumentStore.DocumentFormat.TXT);
        // put binary doc
        byte[] binaryData = {0, 1, 2, 3, 4};
        InputStream inputStream = new ByteArrayInputStream(binaryData);
        documentStore.put(inputStream, uriBinary, DocumentStore.DocumentFormat.BINARY);

        List<Document> results3 = documentStore.searchByPrefix("App");
        assertEquals(2, results3.size());

        Set<URI> results = documentStore.deleteAllWithPrefix("App");
        assertEquals(2,results.size());
        assertTrue(results.contains(uriTxt1));
        assertTrue(results.contains(uriTxt2));
        assertNull(documentStore.get(uriTxt1));
        assertNull(documentStore.get(uriTxt2));
        List<Document> results2 = documentStore.searchByPrefix("App");
        assertEquals(0, results2.size());
        Set<URI> results4 = documentStore.deleteAll("Found");
        assertTrue(results4.contains(uriTxt4));
        documentStore.undo(uriTxt1);
        assertNotNull(documentStore.get(uriTxt1));
    }


    @Test
    public void testlimits() throws IOException {
        URI uriTxt1 = URI.create("http://www.example.com/document/txt1");
        URI uriTxt2 = URI.create("http://www.example.com/document/txt2");
        URI uriTxt3 = URI.create("http://www.example.com/document/txt3");
        //put a text doc
        String txt1 = "app Apple application ";
        InputStream inputStream1 = new ByteArrayInputStream(txt1.getBytes());
        documentStore.put(inputStream1, uriTxt1, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt2 = "App Apple Application";
        InputStream inputStream2 = new ByteArrayInputStream(txt2.getBytes());
        documentStore.put(inputStream2, uriTxt2, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt3 = "app apple application";
        InputStream inputStream3 = new ByteArrayInputStream(txt3.getBytes());
        documentStore.put(inputStream3, uriTxt3, DocumentStore.DocumentFormat.TXT);
        // put binary doc
        byte[] binaryData = {0, 1, 2, 3, 4};
        InputStream inputStream = new ByteArrayInputStream(binaryData);
        documentStore.put(inputStream, uriBinary, DocumentStore.DocumentFormat.BINARY);

        Document doc1 = documentStore.get(uriTxt1);
        Document doc2 = documentStore.get(uriTxt2);
        Document doc3 = documentStore.get(uriTxt3);
        Document doc4 = documentStore.get(uriBinary);

        assertNotNull(doc1);
        assertNotNull(doc2);
        assertNotNull(doc3);
        assertNotNull(doc4);
//lastused 1 2 3 4

        documentStore.setMaxDocumentCount(3);
        assertNotNull(documentStore.get(uriTxt1));
        assertNotNull(documentStore.get(uriTxt2));
        assertNotNull(documentStore.get(uriTxt3));
        assertNotNull(documentStore.get(uriBinary));

    }

    @Test
    public void testlimitswhenundoingacmdset() throws IOException {
        URI uriTxt1 = URI.create("http://www.example.com/document/txt1");
        URI uriTxt2 = URI.create("http://www.example.com/document/txt2");
        URI uriTxt3 = URI.create("http://www.example.com/document/txt3");
        //put a text doc
        String txt1 = "app Apple application ";
        InputStream inputStream1 = new ByteArrayInputStream(txt1.getBytes());
        documentStore.put(inputStream1, uriTxt1, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt2 = "App Apple Application";
        InputStream inputStream2 = new ByteArrayInputStream(txt2.getBytes());
        documentStore.put(inputStream2, uriTxt2, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt3 = "app apple application";
        InputStream inputStream3 = new ByteArrayInputStream(txt3.getBytes());
        documentStore.put(inputStream3, uriTxt3, DocumentStore.DocumentFormat.TXT);
        // put binary doc
        byte[] binaryData = {0, 1, 2, 3, 4};
        InputStream inputStream = new ByteArrayInputStream(binaryData);
        documentStore.put(inputStream, uriBinary, DocumentStore.DocumentFormat.BINARY);

        Document doc1 = documentStore.get(uriTxt1);
        Document doc2 = documentStore.get(uriTxt2);
        Document doc3 = documentStore.get(uriTxt3);
        Document doc4 = documentStore.get(uriBinary);

        assertNotNull(doc1);
        assertNotNull(doc2);
        assertNotNull(doc3);
        assertNotNull(doc4);
//lastused 1 2 3 4
        documentStore.deleteAll("Apple");
        documentStore.delete(uriBinary);
        assertNull(documentStore.get(uriTxt1));
        assertNull(documentStore.get(uriTxt2));
        assertNull(documentStore.get(uriBinary));
        assertNotNull(documentStore.get(uriTxt3));
        documentStore.setMaxDocumentCount(0);
        documentStore.setMaxDocumentCount(1);
        documentStore.undo();
        assertNotNull(documentStore.get(uriBinary));
        documentStore.undo(uriTxt1);
        assertNotNull(documentStore.get(uriTxt1));

    }

    @Test
    public void searchByPrefix2() throws IOException{

        URI uriTxt1 = URI.create("http://www.example.com/document/txt1");
        URI uriTxt2 = URI.create("http://www.example.com/document/txt2");
        URI uriTxt3 = URI.create("http://www.example.com/document/txt3");
        //put a text doc
        String txt1 = "app Apple application ";
        InputStream inputStream1 = new ByteArrayInputStream(txt1.getBytes());
        documentStore.put(inputStream1, uriTxt1, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt2 = "App Apple Application";
        InputStream inputStream2 = new ByteArrayInputStream(txt2.getBytes());
        documentStore.put(inputStream2, uriTxt2, DocumentStore.DocumentFormat.TXT);
        //put another text doc
        String txt3 = "app apple application";
        InputStream inputStream3 = new ByteArrayInputStream(txt3.getBytes());
        documentStore.put(inputStream3, uriTxt3, DocumentStore.DocumentFormat.TXT);
        // put binary doc
        byte[] binaryData = {0, 1, 2, 3, 4};
        InputStream inputStream = new ByteArrayInputStream(binaryData);
        documentStore.put(inputStream, uriBinary, DocumentStore.DocumentFormat.BINARY);

        List<Document> results = documentStore.searchByPrefix("App");
        assertEquals(2, results.size());
        documentStore.setMaxDocumentCount(0);
        List<Document> results2 = documentStore.searchByPrefix("App");
        assertEquals(2, results2.size());

    }


}

