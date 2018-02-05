package gitlet;

import ucb.junit.textui;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/** The suite of all JUnit tests for the gitlet package.
 *  @author
 */
public class UnitTest {

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    /** A dummy test to avoid complaint. */
    @Test
    public void placeholderTest() {
        Commit c = Commit.lookup("2bcaf1");
        assertNotNull(c);
    }

    @Test
    public void testGetAncestors() {
        Set<String> setTest = new HashSet<>();
        setTest.add("Sup");
        setTest.remove("nope");
    }

    @Test
    public void testPath1() {


    }

}


