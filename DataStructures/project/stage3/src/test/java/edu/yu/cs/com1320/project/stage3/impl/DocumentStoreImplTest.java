package edu.yu.cs.com1320.project.stage3.impl;

import edu.yu.cs.com1320.project.stage3.Document;
import edu.yu.cs.com1320.project.stage3.DocumentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

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
        String txt = "txt test";
        InputStream inputStream = new ByteArrayInputStream(txt.getBytes());
        int documenthashcode = documentStore.put(inputStream, uriTxt, DocumentStore.DocumentFormat.TXT);

        assertEquals(0, documenthashcode);
        Document document = documentStore.get(uriTxt);
        assertEquals(txt, document.getDocumentTxt());
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

        boolean deleted = documentStore.delete(uriTxt);
        assertTrue(deleted);

        Document deletedDoc = documentStore.get(uriTxt);
        assertNull(deletedDoc);
        documentStore.undo();
        Document document2 = documentStore.get(uriTxt);
        assertEquals(document, document2);
    }

    @Test
    public void testDeleteDocumentUsingPutWithExistingDoc() throws IOException {
        String content = "delete";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());

        documentStore.put(inputStream, uriTxt, DocumentStore.DocumentFormat.TXT);
        assertNotNull(documentStore.get(uriTxt));

        Document oldhashcode = documentStore.get(uriTxt);
        int hashCode = documentStore.put(null, uriTxt, DocumentStore.DocumentFormat.TXT);
        Document deletedDoc = documentStore.get(uriTxt);

        assertEquals(oldhashcode.hashCode(), hashCode);
        assertNull(deletedDoc);
        documentStore.undo();
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



}