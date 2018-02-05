package gitlet;

import java.io.File;
import java.util.List;

/** The Command class. All the commands for gitlet
 * @author corey hu **/
class Command {

    /** Initalizes a .gitlet directory.
     * @param operands the operands of the operation **/
    public static void doInit(String... operands) {
        if (operands.length != 1) {
            System.out.println("Incorrect operands.");
            return;
        }
        Repo repo = new Repo();
        repo.init();
    }

    /** Prints a log of the ancestry of the head commit.
     * @param operands the operands of the operation **/
    public static void doLog(String ... operands) {
        Commit c = Commit.getHeadCommit();
        String result = "";
        while (true) {
            result += c.logEntry() + "\n\n";
            if (c.getParent() == null) {
                break;
            }
            c = Commit.lookup(c.getParent());
        }
        System.out.println(result.trim() + "\n");
    }

    /** Prints the logs of all commits made in the repo.
     * @param operands the operands of the operation **/
    public static void doGlobalLog(String ... operands) {
        List<String> files = Utils.plainFilenamesIn(".gitlet/COMMITS");
        String result = "";
        for (String sha : files) {
            Commit c = Commit.lookup(sha);
            result += c.logEntry() + "\n\n";
        }
        System.out.println(result.trim() + "\n");
    }

    /** Adds a file to the STAGING area.
     * @param operands the operands of the operation **/
    public static void doAdd(String... operands) {
        if (operands.length != 2) {
            System.out.println("Incorrect operands.");
            return;
        }

        String fileName = operands[1];
        if (!new File(fileName).exists()) {
            System.out.println("File does not exist.");
        } else {
            Repo repo = Repo.deserialize();
            repo.stage(fileName);
            repo.serialize();
        }
    }

    /** Makes a commit.
     * @param operands the operands of the operation **/
    public static void doCommit(String... operands) {
        if (operands.length != 2 || operands[1].equals("")) {
            System.out.println("Please enter a commit message.");
            return;
        }
        String msg = operands[1];
        Repo repo = Repo.deserialize();
        repo.stagingToCommit();
        Commit.makeCommit(msg, repo);
        repo.serialize();
    }

    /** Removes a file from STAGING or the WD.
     * @param operands the operands of the operation **/
    public static void doRemove(String... operands) {
        if (operands.length != 2) {
            System.out.println("Incorrect operands.");
            return;
        }
        String fileName = operands[1];
        Repo repo = Repo.deserialize();
        repo.removeStaging(fileName);
        repo.serialize();
    }

    /** Finds a commit with the given msg.
     * @param operands the operands of the operation **/
    public static void doFind(String... operands) {
        if (operands.length != 2) {
            System.out.println("Incorrect operands.");
            return;
        }
        String msg = operands[1];
        String result = "";
        for (String commitID : Utils.plainFilenamesIn(".gitlet/COMMITS")) {
            Commit c = Commit.lookup(commitID);
            if (c.getMsg().equals(msg)) {
                result += c.id() + "\n";
            }
        }

        if (result.equals("")) {
            System.out.println("Found no commit with that message.");
        } else {
            System.out.println(result.trim());
        }

    }

    /** Prints the status of the repo.
     * @param operands the operands of the operation **/
    public static void doStatus(String... operands) {
        if (operands.length != 1) {
            System.out.println("Incorrect operands.");
            return;
        }
        Repo repo = Repo.deserialize();
        String status = repo.getStatusMsg();
        System.out.println(status);
    }

    /** Checks out either a file or a commit or a branch to WD.
     * @param operands the operands of the operation **/
    public static void doCheckout(String... operands) {
        Repo repo = Repo.deserialize();
        if (operands.length == 2) {
            String branch = operands[1];
            repo.checkout(branch);
            return;
        } else if (operands.length == 3) {
            if (operands[1].equals("--")) {
                String fileName = operands[2];
                repo.checkout(Commit.getHeadCommit(), fileName);
                return;
            }
        } else if (operands.length == 4) {
            if (operands[2].equals("--")) {
                String commitID = operands[1];
                String fileName = operands[3];
                Commit c = Commit.lookup(commitID);
                if (c == null) {
                    System.out.println("No commit with that id exists.");
                    return;
                } else {
                    repo.checkout(c, fileName);
                    return;
                }
            }
        }
        System.out.println("Incorrect operands.");
    }

    /** Initalizes a new branch in the repo.
     * @param operands the operands of the operation **/
    public static void doBranch(String... operands) {
        if (operands.length != 2) {
            System.out.println("Incorrect operands.");
            return;
        }
        String branchName = operands[1];
        Repo repo = Repo.deserialize();
        repo.makeBranch(branchName);
    }

    /** Removes a branch from the repo.
     * @param operands the operands of the operation **/
    public static void doRemoveBranch(String... operands) {
        if (operands.length != 2) {
            System.out.println("Incorrect operands.");
            return;
        }
        String branchName = operands[1];
        Repo repo = Repo.deserialize();
        repo.removeBranch(branchName);
    }

    /** Checks out all files in a commit. Restores WD to that commit
     * @param operands the operands of the operation **/
    public static void doReset(String... operands) {
        if (operands.length != 2) {
            System.out.println("Incorrect operands.");
            return;
        }
        String commitID = operands[1];
        Repo repo = Repo.deserialize();
        repo.reset(commitID);
    }

    /** Merges files between two branches.
     * @param operands the operands of the operation **/
    public static void doMerge(String... operands) {
        if (operands.length != 2) {
            System.out.println("Incorrect operands.");
            return;
        }

        String branchName = operands[1];
        Repo repo = Repo.deserialize();
        repo.merge(branchName);
    }

    /** Performs the add remote command.
     * @param operands the operands of the operation **/
    public static void doAddRemote(String... operands) {
        if (operands.length != 3) {
            System.out.println("Incorrect operands.");
            return;
        }

        String name = operands[1];
        Repo repo = Repo.deserialize();
    }



}
