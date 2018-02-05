package gitlet;


import java.io.File;
import java.io.Serializable;

/** The Blob class. Has some utilities to allow Commits to manipulate blobs
 * @author corey hu **/
public class Blob implements Serializable {

    /** The filename that the blob represents. **/
    private String fileName;
    /** Contents of said filename. **/
    private String contents;

    /** Constructor for blobs.
     * @param filename the filename the blob is to clone **/
    public Blob(String filename) {
        this.fileName = filename;
        this.contents = new String(Utils.readContents(new File(fileName)));
    }

    /** A static method of staging file given just a fileName.
     * @param fileName the fileName in WD that is to be staged
     * @return returns the blob object of the staged file **/
    public static Blob stageFile(String fileName) {
        Blob b = new Blob(fileName);
        b.stageBlob();
        return b;
    }

    /** Adds a serialized blob to STAGING. **/
    public void stageBlob() {
        String filePath = ".gitlet/STAGING/" + id();
        serialize(filePath);
    }

    /** Returns the blob from the FILE dump.
     * @param sha the SHA1 code of the blob in FILES**/
    public static Blob getBlob(String sha) {
        return deserialize(".gitlet/FILES/" + sha);
    }

    /** Returns a deserialized blob from the STAGING area.
     * @param sha the SHA1 code of the blob in STAGING **/
    public static Blob getStagedBlob(String sha) {
        return deserialize(".gitlet/STAGING/" + sha);
    }

    /** Reads the blob's contents and replaces the file
     * in the working dir with the contents of blob. **/
    public void restore() {
        Utils.writeContents(new File(fileName), contents.getBytes());
    }

    /** Serializes the blob into the given path.
     * @param path the path to serialize to **/
    public void serialize(String path) {
        Utils.writeObject(new File(path), this);
    }

    /** Returns the contents of the blob.
     * @param path the path of the blob file being deserialized **/
    public static Blob deserialize(String path) {
        File f = new File(path);
        if (f.exists()) {
            return Utils.readObject(f, Blob.class);
        }
        return null;
    }

    /** Returns the blob's file's fileName. **/
    public String getFileName() {
        return fileName;
    }

    /** Returns the contents of the blob. **/
    public String getContents() {
        return contents;
    }

    /** Returns the SHA1 id of the blob. **/
    public String id() {
        return Utils.sha1(this.fileName + this.contents);
    }

}
