package io.github.nicolasgoyon.beaconhold.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FunctionButtonsTest {
    @Test
    void buttonOfAFunctionComesAfterTheButtonNone() {
        assertEquals(1, FunctionButtons.buttonOf(0));
        assertEquals(0, FunctionButtons.functionOf(FunctionButtons.buttonOf(0)));
        assertEquals(-1, FunctionButtons.functionOf(FunctionButtons.NONE));
    }

    @Test
    void selectionButtonsAreNoneAndOneButtonForEachFunction() {
        assertTrue(FunctionButtons.isSelection(FunctionButtons.NONE, 2));
        assertTrue(FunctionButtons.isSelection(1, 2));
        assertTrue(FunctionButtons.isSelection(2, 2));
        assertFalse(FunctionButtons.isSelection(3, 2), "No third function");
        assertFalse(FunctionButtons.isSelection(FunctionButtons.BACK, 2), "Back is not a selection");
        assertFalse(FunctionButtons.isSelection(-7, 2));
    }

    @Test
    void noFunctionGivesOnlyTheButtonNone() {
        assertEquals(1, FunctionButtons.entryCount(0));
        assertTrue(FunctionButtons.isSelection(FunctionButtons.NONE, 0));
        assertFalse(FunctionButtons.isSelection(1, 0));
    }

    @Test
    void pagesHoldTheEntries() {
        assertEquals(1, FunctionButtons.pageCount(3, 5));
        assertEquals(1, FunctionButtons.pageCount(5, 5));
        assertEquals(2, FunctionButtons.pageCount(6, 5));
        assertEquals(1, FunctionButtons.pageCount(0, 5));
        assertEquals(1, FunctionButtons.pageCount(4, 0));
    }

    @Test
    void pageIsInTheValidRange() {
        assertEquals(0, FunctionButtons.clampPage(-1, 2));
        assertEquals(1, FunctionButtons.clampPage(5, 2));
        assertEquals(1, FunctionButtons.clampPage(1, 2));
    }

    @Test
    void pageOfAnEntry() {
        assertEquals(0, FunctionButtons.pageOf(0, 5));
        assertEquals(0, FunctionButtons.pageOf(4, 5));
        assertEquals(1, FunctionButtons.pageOf(5, 5));
        assertEquals(0, FunctionButtons.pageOf(-1, 5));
    }
}
