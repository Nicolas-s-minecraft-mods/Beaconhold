package io.github.nicolasgoyon.beaconhold.menu;

/**
 * The rules of the buttons of the function screen. This class uses no class of the game.
 *
 * <p>The screen shows a list of entries: the entry 0 removes the function, and the entry {@code i + 1} selects the
 * function {@code i} of the sorted list of the functions. The id of the button of an entry is the index of the entry.
 */
public final class FunctionButtons {
    /** The id of the button that removes the function. */
    public static final int NONE = 0;

    /** The id of the button that opens the vanilla beacon screen again. */
    public static final int BACK = -1;

    private FunctionButtons() {
    }

    /**
     * Gives the id of the button of a function.
     *
     * @param functionIndex the index of the function in the sorted list
     * @return the id of the button
     */
    public static int buttonOf(int functionIndex) {
        return functionIndex + 1;
    }

    /**
     * Gives the index of the function of a button.
     *
     * @param buttonId the id of the button
     * @return the index of the function in the sorted list, or -1 for the button {@link #NONE}
     */
    public static int functionOf(int buttonId) {
        return buttonId - 1;
    }

    /**
     * Tells if a button selects a function or removes the function.
     *
     * @param buttonId the id of the button
     * @param functionCount the number of functions
     * @return true for {@link #NONE} and for the button of each function
     */
    public static boolean isSelection(int buttonId, int functionCount) {
        return buttonId >= NONE && buttonId <= functionCount;
    }

    /**
     * Gives the number of entries of the screen.
     *
     * @param functionCount the number of functions
     * @return the number of functions, plus the entry that removes the function
     */
    public static int entryCount(int functionCount) {
        return Math.max(0, functionCount) + 1;
    }

    /**
     * Gives the number of pages of the screen.
     *
     * @param entryCount the number of entries
     * @param rowsPerPage the number of rows on one page
     * @return 1 or more
     */
    public static int pageCount(int entryCount, int rowsPerPage) {
        if (entryCount <= 0 || rowsPerPage <= 0) {
            return 1;
        }
        return (entryCount + rowsPerPage - 1) / rowsPerPage;
    }

    /**
     * Gives a valid page.
     *
     * @param page the page, from 0
     * @param pageCount the number of pages
     * @return the page, from 0 to {@code pageCount - 1}
     */
    public static int clampPage(int page, int pageCount) {
        return Math.max(0, Math.min(page, pageCount - 1));
    }

    /**
     * Gives the page that shows an entry.
     *
     * @param entry the index of the entry
     * @param rowsPerPage the number of rows on one page
     * @return the page, from 0
     */
    public static int pageOf(int entry, int rowsPerPage) {
        return rowsPerPage <= 0 || entry <= 0 ? 0 : entry / rowsPerPage;
    }
}
