package com.rd;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Date;
import java.util.Objects;
import java.util.Collections;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.IntStream;


public class ChromeHistoryAnalyzer extends FileUtility {

    public static final String OTCHET_FOLDER_NAME = "Otchet";
    public static final String GENERAL_OTCHET_FILE_TXT_NAME = "generalOtchet.txt";
    public static final String OTCHET_SHEET_NAME = "GeneralOtchetSheet";

    private static final Logger log = Logger.getLogger(ChromeHistoryAnalyzer.class.getName());

    private static final String HH_RU_RESUME_SEARCH_RESULT = "hhtmFrom=resume_search_result";
    private static final String HH_RU_RESUMES_CATALOG_RESULT = "hhtmFrom=resumes_catalog";

    private static final String RESULT_HISTORY_FOLDER_NAME_PATH = "ResultHistory";
    private static final String HISTORY_RES = "_historyRes_";
    private static final String TXT_FORMAT = ".txt";
    private static final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("_yyyy-MM-dd_HH_mm_ss");
    private static final String OTCHET_EXCEL_FILE_NAME = "GeneralOtchet";
    private static final String OTCHET_EXCEL_FILE_NAME_FORMAT = ".xls";
    private static final String USER_NAME_COL_NAME = "User";
    private static final String MOS_STRING = "MOS"; // it's for MOS only
    private static final SimpleDateFormat standardDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final Pattern FILE_NAME_PATTERN = Pattern.compile(".*_historyRes_\\d{4}-\\d{2}-\\d{2}\\.txt",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private final String generalFolderFullPath;

    public ChromeHistoryAnalyzer(String path) {
        log.setLevel(Level.INFO);
        this.generalFolderFullPath = path;
        createFolder(this.generalFolderFullPath);
    }

    public void startStatisticProcess() {
        try {
            generateStatistics();
        } catch (FileNotFoundException e) {
            log.severe("Not found file " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateStatistics() throws IOException {
        String pathToResultHistory = this.generalFolderFullPath + "/" + RESULT_HISTORY_FOLDER_NAME_PATH;
        log.info("pathToResultHistory: " + pathToResultHistory);
        File folder = new File(pathToResultHistory);
        List<File> listOfFiles = Arrays.asList(Objects.requireNonNull(folder.listFiles()));
        Collections.sort(listOfFiles);
        List<String> userNameHistory = new ArrayList<>(),
                datesHistory = new ArrayList<>();
        readUserNameAndDate(listOfFiles, userNameHistory, datesHistory);
        log.info("Got table fields");
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
        AtomicInteger userInd = new AtomicInteger(0);
        AtomicReference<String> prevUser = new AtomicReference<>(StringUtils.substringBefore(listOfFiles.get(0).getName(), HISTORY_RES));
        listOfFiles.forEach(file -> {
            String fileName = file.getName();
            if (validateFileNameFormat(fileName)) {
                String user = StringUtils.substringBefore(fileName, HISTORY_RES);
                if (!StringUtils.equals(prevUser.get(), user)) {
                    log.info("Prepared user: " + prevUser);
                    prevUser.set(user);
                    userInd.getAndIncrement();
                }
                String date = StringUtils.substringBetween(fileName, HISTORY_RES, TXT_FORMAT);
                int dateInd = datesHistory.indexOf(date);
                String currFullFileName = file.getAbsolutePath();
                try {
                    table[userInd.get()][dateInd] = countNewVisits(currFullFileName);
                } catch (IOException ioException) {
                    log.severe("Error while getting countNewVisita: " + ioException.getMessage());
                }
            }
        });

        return table;
    }

    private boolean validateFileNameFormat(String fileName) {
        if (FILE_NAME_PATTERN.matcher(fileName).find()) {
            return true;
        }
        log.severe("Incorrect file name format: " + fileName);
        return false;
    }

    private void createOtchetXLS(List<String> userNameHistory, List<String> datesHistory, Integer[][] table) {
        try {
            log.info("Creating XLS file, OK");
            Date date = new Date();
            String formattedDate = standardDateFormat.format(date);
            String excelFileName = OTCHET_EXCEL_FILE_NAME + fileNameDateFormat.format(date) + OTCHET_EXCEL_FILE_NAME_FORMAT;
            String filename = this.generalFolderFullPath + "/" + OTCHET_FOLDER_NAME + "/" + excelFileName;
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet(OTCHET_SHEET_NAME);
            HSSFRow rowForDate = sheet.createRow((short)0);
            rowForDate.createCell(0).setCellValue(formattedDate);
            HSSFRow rowhead = sheet.createRow((short)1);
            rowhead.createCell(0).setCellValue(USER_NAME_COL_NAME);
            AtomicInteger counter = new AtomicInteger(0);
            datesHistory.forEach(element -> rowhead.createCell(counter.getAndIncrement() + 1).setCellValue(element));
            for (int k = 0; k < userNameHistory.size(); k++) {
                HSSFRow newRow = sheet.createRow((short) (k + 2));
                newRow.createCell(0).setCellValue(userNameHistory.get(k));
                for (int t = 0; t < datesHistory.size(); t++) {
                    Integer value = Optional.ofNullable(table[k][t]).orElse(0);
                    newRow.createCell(t + 1).setCellValue(value);
                }
            }
            IntStream.range(0, datesHistory.size() + 1)
                    .forEach(sheet::autoSizeColumn);

            FileOutputStream fileOut = new FileOutputStream(filename);
            workbook.write(fileOut);
            log.info("Writing XLS file, OK");
            fileOut.close();
            workbook.close();
        } catch ( Exception ex ) {
            log.severe("Error while generating excel file: " + ex.getMessage());
        }
    }

    private int countNewVisits(String filePath) throws IOException {
        FileInputStream inputStream = new FileInputStream(filePath);
        BufferedReader bfReader = new BufferedReader(new InputStreamReader(inputStream));
        int visitNumber = 0;
        String line;
        while ((line = bfReader.readLine()) != null) {
            if (StringUtils.contains(line, HH_RU_RESUME_SEARCH_RESULT) || StringUtils.contains(line, HH_RU_RESUMES_CATALOG_RESULT)) {
                visitNumber++;
            }
        }
        bfReader.close();
        return visitNumber;
    }
}