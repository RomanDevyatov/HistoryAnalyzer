package com.rd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Main {

    private static final Logger logger = Logger.getLogger(HistoryAnalyzer.class.getName());
    public static Handler fileHandler = null;

    public static void setup(String generalFolderPath) {
        try {
            String formattedDate = HistoryAnalyzer.standardDateFormat.format(new Date());
            String fileStr = "analyzer" + formattedDate + ".log";
            String pathToLogFile = Paths.get(generalFolderPath, "log", "Analyzer", fileStr.replaceAll(":", "_")).normalize().toString();
            File logFile = new File(pathToLogFile);
            logFile.getParentFile().mkdirs();
            logFile.createNewFile();
            logger.info("Log file is created: " + pathToLogFile);

            fileHandler = new FileHandler(pathToLogFile);
            SimpleFormatter simple = new SimpleFormatter();
            fileHandler.setFormatter(simple);

            logger.addHandler(fileHandler);

        } catch (IOException e) {
            logger.severe("Error in setup function: " + e.getMessage());
        }
    }

    public static void main(String... args) {
        try {
            if (args.length == 0) {
                logger.info("no args, add path to ResultHistory folder");
                System.exit(0);
            }

            logger.info("PROGRAM PARAMETERS, Args: " + Arrays.asList(args));

            logger.info("program started at " + (new GregorianCalendar()).toZonedDateTime());

            String generalFolderPath = args[0];

            String searchingString = "";
            if (args.length == 2) {
                searchingString = args[1];
            }

            if (args.length > 2 && args[2].equals("log_on")) {
                setup(generalFolderPath);
            }

            logger.info("Args: " + Arrays.stream(args));
            HistoryAnalyzer historyAnalyzer = new HistoryAnalyzer(generalFolderPath, searchingString);
            historyAnalyzer.startStatisticProcess();
            logger.info("Program has finished.");
        } catch (Exception e) {
            logger.severe("Program has finished with error: " + e.getMessage());
            System.exit(-1);
        }
    }
}
