package gitlet;

import java.io.File;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author corey hu
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        String cmd = args[0];

        File gitDir = new File(".gitlet");
        if (!gitDir.exists() || !gitDir.isDirectory()) {
            if (!cmd.toLowerCase().equals("init")) {
                System.out.println("Not in an initialized Gitlet directory.");
                return;
            }
        }
        switch (cmd.toLowerCase()) {
        case "init" :
            Command.doInit(args);
            break;
        case "add" :
            Command.doAdd(args);
            break;
        case "log" :
            Command.doLog(args);
            break;
        case "global-log" :
            Command.doGlobalLog(args);
            break;
        case "commit" :
            Command.doCommit(args);
            break;
        case "rm" :
            Command.doRemove(args);
            break;
        case "find" :
            Command.doFind(args);
            break;
        case "status" :
            Command.doStatus(args);
            break;
        case "checkout" :
            Command.doCheckout(args);
            break;
        case "branch" :
            Command.doBranch(args);
            break;
        case "rm-branch" :
            Command.doRemoveBranch(args);
            break;
        case "reset" :
            Command.doReset(args);
            break;
        case "merge" :
            Command.doMerge(args);
            break;
        default:
            System.out.println("No command with that name exists.");
            break;
        }

    }


}
