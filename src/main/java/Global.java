public class Global {
    /** ANSI code. */
    public static final String ANSI_BOLD = "\u001B[1m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    /** Configuration. */
    public static final String DOWNLOADS_PATH = "/Users/Ryan7WU/Downloads/";

    public static void updateProgress(double progress, long size) {
        // System.out.print(Global.ANSI_BOLD);
        int percentage = (int) Math.round(progress * 100);
        final int width = 50; // progress bar width in chars
        StringBuilder progressBar = new StringBuilder("╢");
        for (int i = 0; i < width; i++) {
            if (i <= (percentage / 2)) { // <= looks better
                progressBar.append('█');
            } else if (i == (percentage / 2)) {
                progressBar.append('▓');
            } else {
                progressBar.append('░');
            }
        }
        progressBar.append('╟');
        System.out.printf("\r%s  %.2f%%", progressBar.toString(), progress * 100);
        if (percentage == 100) {
            System.out.printf(" ☕️" + ANSI_BOLD + ANSI_GREEN + " Mission Accomplished!" + ANSI_RESET,
                    (double) progress * size / 1024 / 1024, (double) size / 1024 / 1024);
        } else {
            System.out.printf(ANSI_BOLD + ANSI_BLUE + " %.2f/%.2fMB" + ANSI_RESET,
                    (double) progress * size / 1024 / 1024, (double) size / 1024 / 1024);
        }
    }
}
