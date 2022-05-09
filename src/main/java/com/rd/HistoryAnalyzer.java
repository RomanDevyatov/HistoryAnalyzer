package com.rd;

import com.rd.models.HistoryRecord;
import com.rd.utils.FileUtility;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class HistoryAnalyzer extends FileUtility {

    public static final String OTCHET_FOLDER_NAME = "Otchet";
    public static final String GENERAL_OTCHET_FILE_TXT_NAME = "generalOtchet.txt";
    public static final String OTCHET_SHEET_NAME = "GeneralOtchetSheet";

    private static final Logger logger = Logger.getLogger(HistoryAnalyzer.class.getName());

    private static final String HH_RU_RESUME_SEARCH_RESULT = "hhtmFrom=resume_search_result";
    private static final String HH_RU_RESUMES_CATALOG_RESULT = "hhtmFrom=resumes_catalog";
    private static final String HH_RU_CONTACTS_OPENED_TRUE = "contactsOpened=true";

    private static final String RESULT_HISTORY_FOLDER_NAME_PATH = "ResultHistory";
    private static final String HISTORY_RES = "_historyRes_";
    private static final String TXT_FORMAT = ".txt";
    public static final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("_yyyy-MM-dd_HH_mm_ss");
    private static final DateTimeFormatter localDateFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String OTCHET_EXCEL_FILE_NAME = "GeneralOtchet";
    private static final String OTCHET_EXCEL_FILE_NAME_FORMAT = ".xls";
    private static final String USER_NAME_COL_NAME = "User";
    private static final String MOS_STRING = "MOS"; // it's for MOS only, prefix before
    public static final SimpleDateFormat standardDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final Pattern FILE_NAME_PATTERN = Pattern.compile(".*_historyRes_\\d{4}-\\d{2}-\\d{2}\\.txt",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private final String generalFolderFullPath;
    private String searchingString = "contactsOpened=true";

    public HistoryAnalyzer(String path, String searchingString) {
        logger.setLevel(Level.INFO);
        this.generalFolderFullPath = path;
        if (StringUtils.isNotBlank(searchingString)) {
            this.searchingString = searchingString;
        }
        createFolder(this.generalFolderFullPath);
    }

    public void startStatisticProcess() {
        try {
            generateStatistics();

            if (Main.fileHandler != null) {
                Main.fileHandler.close();
            }
        } catch (FileNotFoundException e) {
            logger.severe("Not found file " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateStatistics() throws IOException {
        String pathToResultHistory = this.generalFolderFullPath + "/" + RESULT_HISTORY_FOLDER_NAME_PATH;
        logger.info("pathToResultHistory: " + pathToResultHistory);

        File folder = new File(pathToResultHistory);
        List<File> listOfFiles = Arrays.asList(Objects.requireNonNull(folder.listFiles()));
        Collections.sort(listOfFiles);

        List<String> userNameHistory = new ArrayList<>();
        List<String> datesHistory = new ArrayList<>();
        readUserNameAndDate(listOfFiles, userNameHistory, datesHistory);
        logger.info("Got table fields:");
        logger.info("userNameHistory List: " + userNameHistory + " datesHistory List: " + datesHistory);
        Collections.sort(datesHistory);

        Integer[][] table = fillTableWithVisitInfo(listOfFiles, userNameHistory, datesHistory);
        createOtchetXLS(userNameHistory, datesHistory, table);
    }

    private void readUserNameAndDate(List<File> listOfFiles, List<String> userHistory, List<String> datesHistory) {
        // let's find out how many users and dates are in ResultHistory folder
        listOfFiles.stream()
                .filter(file -> file.exists() && validateFileNameFormat(file.getName()))
                .forEach(file -> {
                    String fileName = file.getName();
                    String userTxtStr = StringUtils.substringBefore(fileName, HISTORY_RES);
                    if (!userHistory.contains(userTxtStr)) {
                        userHistory.add(userTxtStr);
                    }
                    String dateTxtStr = StringUtils.substringBetween(fileName, HISTORY_RES, TXT_FORMAT);
                    if (!datesHistory.contains(dateTxtStr)) {
                        datesHistory.add(dateTxtStr);
                    }
                });
    }

    private Integer[][] fillTableWithVisitInfo(List<File> listOfFiles, List<String> userNameHistory, List<String> datesHistory) {
        //String pathToResultHistory = this.generalFolderFullPath + "/" + RESULT_HISTORY_FOLDER_NAME_PATH;
        Integer[][] table = new Integer[userNameHistory.size()][datesHistory.size()]; // 1 - arg user, 2 - date
        logger.info("created table size: " + "user count: " + userNameHistory.size() + " dates count: " + datesHistory.size());
        try {
            AtomicReference<String> currentUser = new AtomicReference<>(StringUtils.substringBefore(listOfFiles.get(0).getName(), HISTORY_RES));
            logger.info("Current User: " + currentUser + ", NameId: " + userNameHistory.indexOf(currentUser.get()));
            listOfFiles.forEach(file -> {
                String fileName = file.getName();
                if (validateFileNameFormat(fileName)) {
                    String userFromFileName = StringUtils.substringBefore(fileName, HISTORY_RES);

                    if (!StringUtils.equals(currentUser.get(), userFromFileName)) {
                        currentUser.set(userFromFileName);
                        logger.info("Current User: " + currentUser + ", NameId: " + userNameHistory.indexOf(currentUser.get()));
                    }

                    String dateFromFileName = StringUtils.substringBetween(fileName, HISTORY_RES, TXT_FORMAT);
                    logger.info("Current dateFromFileName: " + dateFromFileName);
                    int dateInd = datesHistory.indexOf(dateFromFileName); // getting datesHistory index by dateFromFileName
                    String currFullFileName = file.getAbsolutePath();
                    try {
                        int userIndex = userNameHistory.indexOf(currentUser.get());
                        table[userIndex][dateInd] = countNewVisits(currFullFileName);
                    } catch (IOException ioException) {
                        logger.severe("Error while getting countNewVisita: " + ioException.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            logger.severe("Error in fillTableWithVisitInfo: " + e.getMessage());
            System.exit(-1);
        }

        return table;
    }

    private boolean validateFileNameFormat(String fileName) {
        if (FILE_NAME_PATTERN.matcher(fileName).find()) {
            return true;
        }
        logger.severe("Incorrect file name format: " + fileName);

        return false;
    }

    private void createOtchetXLS(List<String> userNameHistory, List<String> datesHistory, Integer[][] table) {
        try {
            logger.info("Creating XLS file, OK");
            Date date = new Date();
            String formattedDate = standardDateFormat.format(date);
            String excelFileName = MOS_STRING + OTCHET_EXCEL_FILE_NAME + fileNameDateFormat.format(date) + OTCHET_EXCEL_FILE_NAME_FORMAT;
            String filename = this.generalFolderFullPath + "/" + OTCHET_FOLDER_NAME + "/" + excelFileName;
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet(OTCHET_SHEET_NAME);
            HSSFRow rowForDate = sheet.createRow((short)0);
            rowForDate.createCell(0).setCellValue(formattedDate);
            HSSFRow rowhead = sheet.createRow((short)1);
            rowhead.createCell(0).setCellValue(USER_NAME_COL_NAME);
            AtomicInteger counter = new AtomicInteger(0);

            userNameHistory.forEach(element -> rowhead.createCell(counter.getAndIncrement() + 1).setCellValue(element));

            // tableIndex corresponds to table, sheetIndex correspond sheet
            for (int tableIndex = 0, sheetIndex = 0; tableIndex < datesHistory.size(); tableIndex++, sheetIndex++) {
                String currentDateHist = datesHistory.get(tableIndex);
                LocalDate currentLocalDate = LocalDate.parse(currentDateHist);
                // check if there more then 1 days between dates
                if (tableIndex > 0 && tableIndex < datesHistory.size() - 1) {
                    String prevDateHistString = datesHistory.get(tableIndex - 1);
                    sheetIndex = addEmptyStrings(sheet, sheetIndex, LocalDate.parse(prevDateHistString), currentLocalDate, userNameHistory);
                }

                HSSFRow newRow = sheet.createRow((short) (sheetIndex + 2));
                newRow.createCell(0).setCellValue(datesHistory.get(tableIndex));
                for (int t = 0; t < userNameHistory.size(); t++) {
                    Integer value = Optional.ofNullable(table[t][tableIndex]).orElse(0);
                    newRow.createCell(t + 1).setCellValue(value);
                }

                if (tableIndex == datesHistory.size() - 1) { // while not the last element
                    LocalDate prevLocalDate = currentLocalDate;

                    LocalDate lastDayOfCurrentMonth = currentLocalDate.with(TemporalAdjusters.lastDayOfMonth());
                    currentLocalDate = lastDayOfCurrentMonth.plusDays(1);

                    addEmptyStrings(sheet, ++sheetIndex, prevLocalDate, currentLocalDate, userNameHistory);
                }
            }

            IntStream.range(0, datesHistory.size() + 1)
                    .forEach(sheet::autoSizeColumn);

            FileOutputStream fileOut = new FileOutputStream(filename);
            workbook.write(fileOut);
            logger.info("Writing XLS file, OK");
            fileOut.close();
            workbook.close();
        } catch ( Exception ex ) {
            logger.severe("Error while generating excel file: " + ex.getMessage());
        }
    }

    private int addEmptyStrings(HSSFSheet sheet, int sheetIndex, LocalDate prevLocalDate, LocalDate currentLocalDate, List<String> userNameHistory) {
        long cntDaysBetweenDates = ChronoUnit.DAYS.between(prevLocalDate, currentLocalDate);

        if (cntDaysBetweenDates > 1) {
            LocalDate beforeCurrentDate = prevLocalDate;
            while (cntDaysBetweenDates-- > 1) {
                beforeCurrentDate = beforeCurrentDate.plusDays(1);
                String dateString = beforeCurrentDate.format(localDateFormater);
                addEmptyRow(sheet, sheetIndex++, dateString, userNameHistory);
            }
        }
        return sheetIndex;
    }

    private void addEmptyRow(HSSFSheet sheet, Integer sheetIndex, String date, List<String> userNameHistory) {
        HSSFRow emptyRow = sheet.createRow((short) (sheetIndex + 2));
        emptyRow.createCell(0).setCellValue(date);

        for (int t = 0; t < userNameHistory.size(); t++) {
            final int zeroValue = 0;
            emptyRow.createCell(t + 1).setCellValue(zeroValue);
        }
    }

    private int countNewVisits(String filePath) throws IOException {
        Set<String> historyFileLinesSet;
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            historyFileLinesSet = lines.collect(Collectors.toSet());
        }

        Set<HistoryRecord> historyRecordSet = new HashSet<>();
        for (String historyLine : historyFileLinesSet) {
            String[] args = historyLine.split(HistoryRecord.HISTORY_RECORD_DELEMITER, 2);
            if (args.length == 2) {
                historyRecordSet.add(new HistoryRecord(args[0], args[1]));
            } else {
                logger.info("This line was passed: \"" + historyLine + "\"");
            }
        }

        int visitNumber = 0;
        for (HistoryRecord hr : historyRecordSet) {
            if (StringUtils.contains(hr.getUrl(), this.searchingString)) {
                visitNumber++;
            }
        }

        logger.info("visitNumber: " + visitNumber);

        return visitNumber;
    }
}
