package edu.yu.cs.com1320.project.stage6.impl;

import edu.yu.cs.com1320.project.stage6.Document;
import edu.yu.cs.com1320.project.stage6.PersistenceManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import com.google.gson.Gson;



public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {


    private final File baseDir;
    public DocumentPersistenceManager(File baseDir) {


        if (baseDir==null){
            this.baseDir = new File(System.getProperty("user.dir"));
        } else{
            this.baseDir= baseDir;
        }

    }

    public void serialize(URI key, Document val) throws IOException {
        if (key == null || val == null) {
            throw new IllegalArgumentException("key or value cannot be null");
        }

        // Convert URI to a file path
        String filePath = key.toString().replaceFirst("^https?://", "") + ".json";
        File file = new File(baseDir,filePath);
        file.getParentFile().mkdirs();

        Gson gson = new Gson();

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(val, writer);
        } catch (IOException e) {
            throw e;
        }
    }
    @Override
    public Document deserialize(URI key) throws IOException {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        String filePath = key.toString().replaceFirst("^https?://", "") + ".json";

        File file = new File(baseDir, filePath);

        if (!file.exists()) {
            return null;
        }

        Gson gson = new Gson();

        try (FileReader reader = new FileReader(file)) {
            DocumentImpl doc = gson.fromJson(reader, DocumentImpl.class);
            doc.setLastUseTime(System.nanoTime());
            delete(key);
            return doc;
        } catch (IOException e) {

            throw e;
        }
    }
    /**
     * delete the file stored on disk that corresponds to the given key
     * @param key
     * @return true or false to indicate if deletion occured or not
     * @throws IOException
     */
    @Override
    public boolean delete(URI key) throws IOException {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        String filePath = key.toString().replaceFirst("^https?://", "") + ".json";
        File file = new File(baseDir, filePath);
        if (!file.exists()) {
            return false;
        } else {
            try {
                return file.delete();
            } catch (Exception e) {
                return false;

            }
        }
    }



}