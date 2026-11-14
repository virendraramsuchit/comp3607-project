package com.comp3607_project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class EditCalculatorTest {

    private final EditCalculator calc = new EditCalculator();

    // isChanged()

    @Test
    void isChanged_returnsFalseForIdenticalText() {
        assertFalse(calc.isChanged("hello", "hello"));
    }

    @Test
    void isChanged_returnsTrueForOneCharDifference() {
        assertTrue(calc.isChanged("hello", "hellO"));
    }

    @Test
    void isChanged_returnsTrueForInsertion() {
        assertTrue(calc.isChanged("hello", "helloo"));
    }

    @Test
    void isChanged_returnsTrueForDeletion() {
        assertTrue(calc.isChanged("hello", "hell"));
    }

    @Test
    void isChanged_returnsFalseForTwoEmptyStrings() {
        assertFalse(calc.isChanged("", ""));
    }

    @Test
    void isChanged_returnsTrueWhenOneSideEmpty() {
        assertTrue(calc.isChanged("", "x"));
    }

    // getDistance ()

    @Test
    void getDistance_identicalStringsIsZero() {
        assertEquals(0, EditCalculator.getDistance("abc", "abc"));
    }

    @Test
    void getDistance_emptyToEmptyIsZero() {
        assertEquals(0, EditCalculator.getDistance("", ""));
    }

    @Test
    void getDistance_emptyToStringIsLength() {
        assertEquals(5, EditCalculator.getDistance("", "hello"));
    }

    @Test
    void getDistance_stringToEmptyIsLength() {
        assertEquals(5, EditCalculator.getDistance("hello", ""));
    }

    @Test
    void getDistance_singleSubstitution() {
        assertEquals(1, EditCalculator.getDistance("kitten", "sitten"));
    }

    @Test
    void getDistance_classicKittenSittingExample() {
        // standard Levenshtein test: kitten -> sitting = 3
        assertEquals(3, EditCalculator.getDistance("kitten", "sitting"));
    }

    @Test
    void getDistance_multipleEdits() {
        // flog -> flap = 2 substitutions (o→a, g→p)
        assertEquals(2, EditCalculator.getDistance("flog", "flap"));
    }

    // levenshtein - optimised

    @Test
    void levenshtein_returnsSameAsGetDistanceForSimpleCase() {
        assertEquals(
            EditCalculator.getDistance("abcdef", "abXdef"),
            EditCalculator.levenshtein("abcdef", "abXdef")
        );
    }

    @Test
    void levenshtein_handlesLongUnchangedPrefixAndSuffix() {
        String prefix = "public class Foo { ".repeat(50);
        String suffix = " }".repeat(50);
        String a = prefix + "int x = 1;" + suffix;
        String b = prefix + "int x = 2;" + suffix;
        
        assertEquals(1, EditCalculator.levenshtein(a, b));
    }

    @Test
    void levenshtein_identicalLargeStringsIsZero() {
        String big = "x".repeat(10_000);
        assertEquals(0, EditCalculator.levenshtein(big, big));
    }

    // getNumChanges()

    @Test
    void getNumChanges_identicalTextIsZero() {
        assertEquals(0, calc.getNumChanges("same", "same"));
    }

    @Test
    void getNumChanges_oneCharDiffIsOne() {
        assertEquals(1, calc.getNumChanges("abc", "abd"));
    }

    @Test
    void getNumChanges_completelyDifferent() {
        assertEquals(3, calc.getNumChanges("abc", "xyz"));
    }

    @Test
    void getNumChanges_emptyToSomething() {
        assertEquals(4, calc.getNumChanges("", "test"));
    }
}