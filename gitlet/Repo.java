package gitlet;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;


/** Our repository object with all functions of gitlet.
 * @author corey hu**/
public class Repo implements Serializable {

    /** The current branch of the repo. **/
    private String currentBranch;
    /** The mapping of file names being tracked to their SHA1 blob codes. **/
    private HashMap<String, String> tracking;
    /** The list of braches in the repo. **/
    private ArrayList<String> branches;
    /** The list of files that are staged to be removed. **/
    private ArrayList<String> stagedRemove;
    /** The map of filenames and their SHA1 codes
     * that are staged to be added. **/
    private HashMap<String, String> stagedAdd;
    /** Hashamp of Remotes/parent repos. **/
    private HashMap<String, String> remotes;



    /** Constructor for the repo object. **/
    public Repo() {
        currentBranch = "master";
        tracking = new HashMap<>();
        branches = new ArrayList<>();
        stagedRemove = new ArrayList<>();
        stagedAdd = new HashMap<>();
        remotes = new HashMap<>();
    }


    /** Initialize our .gitlet repository. **/
    public void init() {
        File dir = new File(".gitlet");

        if (dir.mkdir()) {
            new File(".gitlet/COMMITS").mkdir();
            new File(".gitlet/FILES").mkdir();
            new File(".gitlet/BRANCHES").mkdir();
            new File(".gitlet/STAGING").mkdir();
            new File(".gitlet/REMOTES").mkdir();

            try {
                new File(".gitlet/commitHistory").createNewFile();
                new File(".gitlet/tracking").createNewFile();
            } catch (IOException e) {
                return;
            }
            Commit.makeInitialCommit();
            branches.add("master");
            serialize();
        } else {
            System.out.println("A Gitlet version-control system"
                    + " already exists in the current directory.");
        }
    }

    /** Convert all files in our STAGING area to a commit. **/
    public void stagingToCommit() {
        if (stagedRemove.isEmpty() && stagedAdd.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        for (String fileName : stagedAdd.keySet()) {
            String blobSha = stagedAdd.get(fileName);
            Blob b = Blob.deserialize(".gitlet/STAGING/" + blobSha);
            tracking.put(b.getFileName(), blobSha);
            b.serialize(".gitlet/FILES/" + blobSha);
            new File(".gitlet/STAGING/" + blobSha).delete();
        }
        for (String fileName : stagedRemove) {
            tracking.remove(fileName);
        }
        clearStaging();
    }

    /** Make a branch in our repository.
     * @param name name of the branch**/
    public void makeBranch(String name) {
        if (branches.contains(name)) {
            System.out.println("A branch with that name already exists.");
        } else {
            branches.add(name);
            Commit.getHeadCommit().makeBranchHead(name);
            serialize();
        }
    }

    /** Remove a branch in our repository.
     * @param name name of the branch**/
    public void removeBranch(String name) {
        if (!branches.contains(name)) {
            System.out.println("A branch with that name does not exist.");
        } else if (name.equals(currentBranch)) {
            System.out.println("Cannot remove the current branch.");
        } else {
            branches.remove(name);
            new File(".gitlet/BRANCHES/" + name).delete();
            serialize();
        }
    }

    /** Check if a merge is possible/will error.
     * @param otherBranch the other branch being merged with
     * @return returns whether a merge is possible or not **/
    private boolean mergeCheck(String otherBranch) {
        if (!stagedAdd.isEmpty() || !stagedRemove.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return false;
        } else if (!new File(".gitlet/BRANCHES/" + otherBranch).exists()) {
            System.out.println("A branch with that name does not exist.");
            return false;
        } else if (currentBranch.equals(otherBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            return false;
        } else {
            Commit branchHead = Commit.getBranchHead(otherBranch);
            List<String> wd = Utils.plainFilenamesIn(".");
            for (String fileName : branchHead.getBlobs().keySet()) {
                if (!tracking.containsKey(fileName) && wd.contains(fileName)) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it or add it first.");
                    return false;
                }
            }
        }
        return true;
    }

    /** Ties all the merges together.
     * Checks to make sure split and commits aren't the same.
     * @param otherBranch the name of the other branch to be merged **/
    public void merge(String otherBranch) {
        if (!mergeCheck(otherBranch)) {
            return;
        }
        Commit headB = Commit.getHeadCommit();
        Commit otherB = Commit.getBranchHead(otherBranch);
        Commit splitPoint = getSplitPoint(headB, otherB);
        if (splitPoint.id().equals(otherB.id())) {
            System.out.println(
                    "Given branch is an ancestor of the current branch.");
            return;
        } else if (splitPoint.id().equals(headB.id())) {
            otherB.updateHeads();
            otherB.makeBranchHead(currentBranch);
            System.out.println("Current branch fast-forwarded. ");
        } else {
            makeMerge(headB, otherB, splitPoint, otherBranch);
        }
    }

    /** Handles the merging process.
     * @param headB head commit
     * @param otherB other commit
     * @param splitPoint split point of two commits
     * @param otherBranch name of the other branch to merge to **/
    private void makeMerge(Commit headB, Commit otherB,
                           Commit splitPoint, String otherBranch) {
        boolean encounteredConflict = false;
        HashSet<String> allFiles = new HashSet<>();
        allFiles.addAll(headB.getBlobs().keySet());
        allFiles.addAll(otherB.getBlobs().keySet());
        allFiles.addAll(splitPoint.getBlobs().keySet());
        ArrayList<String> modifiedHead = modifiedFiles(headB, splitPoint);
        ArrayList<String> modifiedOther = modifiedFiles(otherB, splitPoint);
        Set<String> presentInHead = headB.getBlobs().keySet();
        Set<String> presentInOther = otherB.getBlobs().keySet();
        Set<String> presentInSplit = splitPoint.getBlobs().keySet();
        for (String fileName : allFiles) {
            if (!modifiedHead.contains(fileName)) {
                if (modifiedOther.contains(fileName)) {
                    checkout(otherB, fileName);
                    stage(fileName);
                } else if (presentInSplit.contains(fileName)
                        && !presentInOther.contains(fileName)) {
                    removeStaging(fileName);
                }
            } else if (!presentInHead.contains(fileName)
                    && presentInOther.contains(fileName)
                    && !presentInSplit.contains(fileName)) {
                checkout(otherB, fileName);
                stage(fileName);
            } else if ((modifiedHead.contains(fileName)
                    && modifiedOther.contains(fileName)
                    && !headB.getBlobs().get(fileName).equals(
                    otherB.getBlobs().get(fileName)))
                    || (presentInSplit.contains(fileName)
                    && ((!presentInHead.contains(fileName)
                    && modifiedOther.contains(fileName))
                    || (!presentInOther.contains(fileName)
                    && modifiedHead.contains(fileName))))) {
                Blob headBlob = Blob.getBlob(
                        headB.getBlobs().get(fileName));
                Blob otherBlob = Blob.getBlob(
                        otherB.getBlobs().get(fileName));
                String headContents = headBlob != null
                        ? headBlob.getContents() : "";
                String otherContents = otherBlob != null
                        ? otherBlob.getContents() : "";
                if (!headContents.equals(otherContents)) {
                    encounteredConflict = true;
                    String newContents = "<<<<<<< HEAD\n"
                            + headContents + "=======\n"
                            + otherContents + ">>>>>>>\n";
                    Utils.writeContents(new File(fileName),
                            newContents.getBytes());
                    stage(fileName);
                }
            }
        }
        notifyMergeConflict(encounteredConflict);
        stagingToCommit();
        String mergeMsg = "Merged " + otherBranch
                + " into " + currentBranch + ".";

        Commit.makeMergeCommit(mergeMsg , this, otherB.id());
    }

    /** Notifies the terminal that a merge conflict is afoot.
     * @param conflicted boolean whether there was a conflict **/
    private void notifyMergeConflict(boolean conflicted) {
        if (conflicted) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** A function that retrieves the split point of two commits.
     * @param a first commit
     * @param b second commit
     * @return a commit that represents the
     * splitpoint of two commits with diff branches **/
    private Commit getSplitPoint(Commit a, Commit b) {
        List<String> ancestorsA = a.getAncestors();
        List<String> ancestorsB = b.getAncestors();
        for (String ancestorSHA : ancestorsA) {
            if (ancestorsB.contains(ancestorSHA)) {
                return Commit.lookup(ancestorSHA);
            }
        }
        return null;
    }

    /** Resets the repo to a specified commitID.
     * @param commitID the commit to reset to **/
    public void reset(String commitID) {
        Commit c = Commit.lookup(commitID);
        if (c == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Set<String> errorOn = new HashSet<>(c.getBlobs().keySet());
        errorOn.removeAll(tracking.keySet());
        List<String> wd = Utils.plainFilenamesIn(".");
        for (String file : errorOn) {
            if (wd.contains(file)) {
                System.out.println(
                        "There is an untracked file in the way; "
                                + "delete it or add it first.");
                return;
            }
        }

        Set<String> deleteFiles = new HashSet<>(tracking.keySet());
        deleteFiles.removeAll(c.getBlobs().keySet());

        for (String fileName : deleteFiles) {
            new File(fileName).delete();
        }

        Set<String> cFiles = c.getBlobs().keySet();

        for (String cFileName : cFiles) {
            checkout(c, cFileName);
        }

        c.updateHeads();
        tracking = new HashMap<>(c.getBlobs());
        clearStaging();
        serialize();
    }

    /** Checksout the repo based on a branchname.
     * @param branch the name of the branch to checkout**/
    public void checkout(String branch) {
        if (branch.equals(currentBranch)) {
            System.out.println("No need to checkout the current branch.");
        } else {
            Commit bh = Commit.getBranchHead(branch);
            if (bh == null) {
                System.out.println("No such branch exists.");
            } else {
                List<String> wd = Utils.plainFilenamesIn(".");
                for (String fileName : bh.getBlobs().keySet()) {
                    if (!tracking.containsKey(fileName)
                            && wd.contains(fileName)) {
                        System.out.println(
                                "There is an untracked file in the way;"
                                + "delete it or add it first.");
                        return;
                    }
                }

                Set<String> notInBranch = new HashSet<>(tracking.keySet());
                notInBranch.removeAll(bh.getBlobs().keySet());
                for (String fileName : notInBranch) {
                    new File(fileName).delete();
                }

                bh.restore();
                bh.updateHeads(branch);
                currentBranch = branch;
                tracking = new HashMap<>(bh.getBlobs());
                clearStaging();
                serialize();
            }
        }
    }

    /** Checks out a file in a commit.
     * @param fileName the filename to check out
     * @param c the commit to lookup the filename in**/
    public void checkout(Commit c, String fileName) {
        if (!c.getBlobs().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        String sha = c.getBlobs().get(fileName);
        Blob b = Blob.getBlob(sha);
        b.restore();
        serialize();
    }

    /** Returns Arraylist of filenames that have
     * been modified between two commits. *
     * @param a first commit to compare against
     * @param b second commit to compare*/
    private ArrayList<String> modifiedFiles(Commit a, Commit b) {
        HashMap<String, String> aFiles = a.getBlobs();
        HashMap<String, String> bFiles = b.getBlobs();

        ArrayList<String> modifiedFiles = new ArrayList<>();

        for (String fileName : aFiles.keySet()) {
            if (!(bFiles.containsKey(fileName)
                    && aFiles.get(fileName).equals(bFiles.get(fileName)))) {
                modifiedFiles.add(fileName);
            }
        }
        return modifiedFiles;
    }

    /** A helper function to find the status msg.
     * @param allFiles all files in WD and head commit
     * @param workingDirFiles all files in WD
     * @param headCommitFiles all files in head commit
     * @param modifiedNotStaged a list for mod not staged files
     * @param removed a list for removed files
     * @param staged a list for staged files
     * @param untracked  a list for untracked files **/
    private void getStatusHelper(Set<String> allFiles,
                                 List<String> workingDirFiles,
                                 HashMap<String, String> headCommitFiles,
                                 Set<String> staged, Set<String> removed,
                                 Set<String> modifiedNotStaged,
                                 Set<String> untracked) {
        for (String fileName : allFiles) {
            if (stagedRemove.contains(fileName)) {
                if (workingDirFiles.contains(fileName)) {
                    untracked.add(fileName);
                } else {
                    removed.add(fileName);
                }
            } else if (!stagedRemove.contains(fileName)
                    && headCommitFiles.containsKey(fileName)
                    && !workingDirFiles.contains(fileName)) {
                modifiedNotStaged.add(fileName + " (deleted)");
            } else if (stagedAdd.containsKey(fileName)) {
                if (!workingDirFiles.contains(fileName)) {
                    modifiedNotStaged.add(fileName + " (deleted)");
                } else if (!Blob.getStagedBlob(
                        stagedAdd.get(fileName)).getContents().equals(
                        new String(Utils.readContents(new File(fileName))))) {
                    modifiedNotStaged.add(fileName + " (modified)");
                } else {
                    staged.add(fileName);
                }
            } else if (!stagedAdd.containsKey(fileName)
                    && headCommitFiles.containsKey(fileName)
                    && !Blob.getBlob(headCommitFiles.
                    get(fileName)).getContents().equals(
                    new String(Utils.readContents(new File(fileName))))) {
                modifiedNotStaged.add(fileName + " (modified)");
            } else if (headCommitFiles.containsKey(fileName)) {
                continue;
            } else {
                untracked.add(fileName);
            }
        }
    }

    /** Returns a status msg.
     * @return the status msg **/
    public String getStatusMsg() {
        Commit head = Commit.getHeadCommit();
        String statusmsg = "";
        String branchResult = "=== Branches ===\n";
        for (String branchName : branches) {
            if (branchName.equals(currentBranch)) {
                branchName = "*" + branchName;
            }
            branchResult += branchName + "\n";
        }
        List<String> workingDirFiles = Utils.plainFilenamesIn(".");
        HashMap<String, String> headCommitFiles = head.getBlobs();
        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(workingDirFiles);
        allFiles.addAll(stagedAdd.keySet());
        allFiles.addAll(stagedRemove);
        allFiles.addAll(head.getBlobs().keySet());
        Set<String> staged = new HashSet<>();
        Set<String> removed = new HashSet<>();
        Set<String> modifiedNotStaged = new HashSet<>();
        Set<String> untracked = new HashSet<>();

        getStatusHelper(allFiles,  workingDirFiles, headCommitFiles,
                staged, removed, modifiedNotStaged, untracked);

        statusmsg += branchResult;
        statusmsg += "\n=== Staged Files ===\n";
        statusmsg += String.join("\n",
                staged).trim() + (staged.size() > 0 ? "\n" : "");
        statusmsg += "\n=== Removed Files ===\n";
        statusmsg += String.join("\n",
                removed).trim()
                + (removed.size() > 0 ? "\n" : "");
        statusmsg += "\n=== Modifications Not Staged For Commit ===\n";
        statusmsg += String.join("\n",
                modifiedNotStaged).trim()
                + (modifiedNotStaged.size() > 0 ? "\n" : "");
        statusmsg += "\n=== Untracked Files ===\n";
        statusmsg += String.join("\n", untracked).trim() + "\n";

        return statusmsg;
    }

    /** Stages a file given a filename.
     * @param fileName the filename to stage**/
    public void stage(String fileName) {
        Blob b = new Blob(fileName);
        if (tracking.containsKey(fileName)
                && tracking.get(fileName).equals(b.id())) {
            if (stagedAdd.containsKey(fileName)) {
                stagedAdd.remove(fileName);
                new File(".gitlet/STAGING/" + fileName).delete();
            }
            if (stagedRemove.contains(fileName)) {
                stagedRemove.remove(fileName);
            }
        } else if (stagedRemove.contains(fileName)) {
            stagedRemove.remove(fileName);
        } else {
            b.stageBlob();
            stagedAdd.put(b.getFileName(), b.id());
        }
    }

    /** Does a remove on a from staging.
     * @param fileName fileName to remove**/
    public void removeStaging(String fileName) {
        if (!tracking.containsKey(fileName)
                && !stagedAdd.containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
        }
        if (stagedAdd.containsKey(fileName)) {
            new File(".gitlet/STAGING/" + stagedAdd.get(fileName)).delete();
            stagedAdd.remove(fileName);
        }
        if (tracking.containsKey(fileName)) {
            stagedRemove.add(fileName);
            Utils.restrictedDelete(fileName);
        }
    }

    /** Return a hashmap of all tracked files. **/
    public HashMap<String, String> getTracking() {
        return tracking;
    }

    /** Return your current branch. **/
    public String getCurrentBranch() {
        return currentBranch;
    }

    /** Return your current branch. **/
    public ArrayList<String> getBranches() {
        return branches;
    }

    /** Deserialize the remote repo with a given name.
     * @param name name of the remote to deserialize
     * @return the deserialized remote repo **/
    public Repo deserializeRemote(String name) {
        return Utils.readObject(
                new File(remotes.get(name) + "/repo"),
                Repo.class);
    }

    /** Deserialize your repo from .gitlet/repo.
     * @return the deserialized repository object **/
    public static Repo deserialize() {
        return Utils.readObject(new File(".gitlet/repo"), Repo.class);
    }

    /** Serialize your repo to .gitlet/repo. **/
    public void serialize() {
        Utils.writeObject(new File(".gitlet/repo"), this);
    }

    /** Empty STAGING area. **/
    private void clearStaging() {
        stagedRemove.clear();
        stagedAdd.clear();
        List<String> stagedBlobs = Utils.plainFilenamesIn(".gitlet/STAGING");
        for (String blobSHA: stagedBlobs) {
            new File(".gitlet/STAGING/" + blobSHA).delete();
        }
    }

    /** Adding remotes.
     * @param name name of the remote
     * @param remotePath the path of the remote **/
    public void addRemote(String name, String remotePath) {
        if (remotes.containsKey(name)) {
            System.out.println("A remote with that name already exists.");
        } else {
            remotes.put(name, remotePath);
        }
    }
    /** Removing remotes.
     * @param name name of the remote **/
    public void removeRemote(String name) {
        if (!remotes.containsKey(name)) {
            System.out.println("A remote with that name does not exist.");
        } else {
            remotes.remove(name);
        }
    }

    /** Push your commit to your remote.
     * @param name name of the remote
     * @param branch the brand you wish to push to on the remote **/
    public void pushRemote(String name, String branch) {
        Repo remote = deserializeRemote(name);
        if (!remote.getBranches().contains(branch)) {
            remote.makeBranch(branch);
        }
        Commit curHead = Commit.getHeadCommit();
        Commit remoteHead = Commit.
                deserializeRemoteHead(remotes.get(name), branch);

        fastForward(remoteHead, curHead);

    }

    /** Updates the commits between two commits.
     * @param past the commit in the history of another
     * @param future the more updated commit **/
    private void fastForward(Commit past, Commit future) {
        List<String> currAncestors = future.getAncestors();
        if (future.getAncestors().
                contains(past.id())) {
            System.out.println(
                    "Please pull down remote changes before pushing.");
            return;
        }

    }

}
