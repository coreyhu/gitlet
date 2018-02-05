package gitlet;


import java.io.File;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** The commit class. Has a bunch of utility functions that help gitlet
 * @author corey hu **/
public class Commit implements Serializable {

    /** The date object. **/
    private ZonedDateTime datetime;

    /** The commit's message. **/
    private String msg;

    /** The branch name. **/
    private String branch;

    /** The parent SHA1. **/
    private String parent;

    /** The merge parent (if applicable) SHA1. **/
    private String mergeParent;

    /** A hashmap mapping all tracked filenames to their SHA1 code. **/
    private HashMap<String, String> blobs;

    /** Commit object constructor for initial commit. **/
    public Commit() {
        datetime = ZonedDateTime.now();
        msg = "initial commit";
        branch = "master";
        parent = null;
        mergeParent = null;
        blobs = new HashMap<>();

    }

    /** The second constructor.
     * @param r the repo the commit is made in
     * @param head the current head (soon to be parent)
     * @param message the commit's message **/
    public Commit(String message, Commit head, Repo r) {
        this.datetime = ZonedDateTime.now();
        this.msg = message;
        this.branch = r.getCurrentBranch();
        this.parent = head.id();
        this.mergeParent = null;
        this.blobs = r.getTracking();
    }

    /** Make the first commit (following git init). **/
    public static void makeInitialCommit() {
        Commit head = new Commit();
        head.saveToDump();
        head.updateHeads();
        head.updateCommitHist();
    }

    /** Make a commit based on the current state of Repo r.
     * @param msg the commit message
     * @param r the repo being committed to **/
    public static void makeCommit(String msg, Repo r) {
        Commit head = deserialize(".gitlet/head");
        Commit c = new Commit(msg, head, r);
        c.saveToDump();
        c.updateHeads();
        c.updateCommitHist();
    }

    /** Make a commit following a merge.
     * @param r the repo the merge commit is being made in
     * @param msg the commit message
     * @param mergeParentID the other parent's ID **/
    public static void makeMergeCommit(String msg,
                                       Repo r, String mergeParentID) {
        Commit head = deserialize(".gitlet/head");
        Commit c = new Commit(msg, head, r);
        c.mergeParent = mergeParentID;
        c.saveToDump();
        c.updateHeads();
        c.updateCommitHist();
    }

    /** Returns the commit's parent. **/
    public String getParent() {
        return parent;
    }

    /** Returns the commit's msg. **/
    public String getMsg() {
        return msg;
    }

    /** Returns the commit's branch. **/
    public String getBranch() {
        return branch;
    }

    /** Returns the blob map of the commit.  **/
    public HashMap<String, String> getBlobs() {
        return blobs;
    }

    /** Serializes commit to the COMMIT folder dump. **/
    private void saveToDump() {
        String filepath = ".gitlet/COMMITS/" + id();
        serialize(filepath);
    }

    /** Serializes the current commit to the head file
     * and updates its branch's head commit. **/
    public void updateHeads() {
        updateHeads(branch);
    }

    /** Updating heads using a specific branchName.
     * @param branchName name of branch to update head of **/
    public void updateHeads(String branchName) {
        serialize(".gitlet/head");
        serialize(".gitlet/BRANCHES/" + branchName);
    }

    /** Updates the commithistory file by adding a SHA1 to the list. **/
    private void updateCommitHist() {
        File history = new File(".gitlet/commitHistory");
        String cur = new String(
                Utils.readContents(new File(".gitlet/commitHistory")));
        cur += id() + "\n";
        Utils.writeContents(history, cur.getBytes());
    }

    /** Restores all files (blobs) tied to the commit. **/
    public void restore() {
        for (String sha : blobs.values()) {
            Blob b = Blob.getBlob(sha);
            b.restore();
        }
    }

    /** Serializes the commit into path.
     * @param path the path the commit object is being serialized to **/
    private void serialize(String path) {
        Utils.writeObject(new File(path), this);
    }

    /** Lookup the commit in the commit dump folder given a SHA1 code.
     * @param sha the SHA code being looked up
     * @return the commit after being lookedup **/
    public static Commit lookup(String sha) {
        if (sha.length() != Utils.UID_LENGTH) {
            for (String commitSHA : Utils.plainFilenamesIn(".gitlet/COMMITS")) {
                if (commitSHA.startsWith(sha)) {
                    sha = commitSHA;
                    break;
                }
            }
        }
        return deserialize(".gitlet/COMMITS/" + sha);
    }

    /** Deserialize the head commit.
     * @return a commit head object **/
    public static Commit getHeadCommit() {
        return deserialize(".gitlet/head");
    }

    /** Returns the deserialized head commit of Branch branchName.
     * @param branchName the name of the branch being looked up**/
    public static Commit getBranchHead(String branchName) {
        return deserialize(".gitlet/BRANCHES/" + branchName);
    }

    /** Serializes a certain commit to replace the
     * serialization of Branch branchname's head commit.
     * @param branchName name of branch to replace headfile of **/
    public void makeBranchHead(String branchName) {
        serialize(".gitlet/BRANCHES/" + branchName);
    }

    /** Deserializes the head commit of a branch in a remote repo.
     * @return deserialized commit
     * @param remoteRepoPath the path to the remote repo to be deserialized
     * @param branchName the name of the branch to deserialize **/
    public static Commit deserializeRemoteHead(String remoteRepoPath,
                                               String branchName) {
        return Utils.readObject(new File(remoteRepoPath
                + "/BRANCHES/" + branchName), Commit.class);
    }

    /** Deserializes the commit from path.
     * @return deserialized commit
     * @param path the path of the object to be deserialized **/
    private static Commit deserialize(String path) {
        File f = new File(path);
        if (f.exists()) {
            return Utils.readObject(f, Commit.class);
        }
        return null;
    }

    /** Returns a list of SHA1s all previous parents. **/
    public List<String> getAncestors() {
        List<String> ancestors = new ArrayList<>();
        ancestors.add(id());

        if (parent != null) {
            Commit p = lookup(parent);
            ancestors.addAll(p.getAncestors());
        }
        return ancestors;
    }

    /** Returns the SHA1 id of the commit. **/
    public String id() {
        return Utils.sha1(this.datetime
                + this.msg + this.branch
                + this.blobs + this.parent + this.mergeParent);
    }

    /** Returns the log entry of the commit. **/
    public String logEntry() {
        String mergeLine = "";
        if (mergeParent != null) {
            mergeLine = "\nMerge: " + parent.substring(0, 7)
                    + " " + mergeParent.substring(0, 7);
        }
        DateTimeFormatter format = DateTimeFormatter.
                ofPattern("EEE MMM d yy:HH:mm yyyy ZZZ");
        String result = "==="
                + "\ncommit " + id()
                + mergeLine
                + "\nDate: " + datetime.format(format)
                + "\n" + msg;
        return result;
    }

}
