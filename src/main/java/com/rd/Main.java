package com.rd;

import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.logging.Logger;

public class Main {

    private static final Logger log = Logger.getLogger(ChromeHistoryAnalyzer.class.getName());

    public static void main(String[] args) {
        try {
            log.info("program started at " + (new GregorianCalendar()).toZonedDateTime());
            String generalFolderPath = args[0];
            log.info("Args: " + Arrays.stream(args));
            ChromeHistoryAnalyzer chromeHistoryAnalyzer = new ChromeHistoryAnalyzer(generalFolderPath);
            chromeHistoryAnalyzer.startStatisticProcess();
            log.info("Program has finished.");
        } catch (Exception e) {
            log.severe("Program has finished with error: " + e.getMessage());
            System.exit(-1);
        }
    }
}
