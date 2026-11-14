package comp3607_project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class CompileTrackerTest {

    private final CompileTracker t = new CompileTracker();

    @Test void nullMessageBecomesUnknownError() {
        assertEquals("unknown error", t.errorKey(null));
    }

    @Test void onlyFirstLineIsConsidered() {
        String multi = "cannot find symbol\n  symbol: variable x\n  location: class Y";
        assertEquals("cannot find symbol", t.errorKey(multi));
    }

    @Test void cutAtColonWhenNoPatchMatches() {
        assertEquals("custom prefix", t.errorKey("custom prefix: details here"));
    }

    @Test void noColonNoPatchReturnsTrimmed() {
        assertEquals("bare message", t.errorKey("   bare message   "));
    }

    @ParameterizedTest(name = "patch {0} -> {1}")
    @CsvSource({
        "'variable b is already defined in method main', name already defined",
        "'method greet cannot be applied to given types;', method cannot be applied to given types",
        "'variable x might not have been initialized', variable might not have been initialized",
        "'non-static method foo() cannot be referenced from a static context', non-static reference from static context",
        "'class Foo should be declared in a file named Foo.java', class name does not match file name",
        "'unreported exception IOException; must be caught', unreported exception",
        "'foo() has private access in Bar', private access violation",
        "'cannot assign a value to final variable x', assignment to final variable",
        "'int cannot be dereferenced', primitive cannot be dereferenced",
        "'package java.utils does not exist', package does not exist"
    })
    void patchTable(String raw, String expected) {
        assertEquals(expected, t.errorKey(raw));
    }
}
