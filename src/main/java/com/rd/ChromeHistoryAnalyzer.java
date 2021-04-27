package com.rd;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class ChromeHistoryAnalyzer extends FileUtility{

    public static final String OTCHET_FOLDER_NAME = "Otchet";
    public static final String GENERAL_OTCHET_FILE_TXT_NAME = "generalOtchet.txt";
    public static final String OTCHET_SHEET_NAME = "GeneralOtchetSheet";

    private static final Logger log = Logger.getLogger(ChromeHistoryAnalyzer.class.getName());

    private static final Pattern HH_RU_PATTERN = Pattern.compile(".*https:\\/\\/hh.ru.*\\?.*hhtmFrom=resume_search_result.*",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final String RESULT_HISTORY_FOLDER_NAME_PATH = "ResultHistory";
    private static final String HISTORY_RES = "_historyRes_";
    private static final String TXT_FORMAT = ".txt";
    private static final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("_yyyy-MM-dd_HH_mm_ss");
    private static final String OTCHET_EXCEL_FILE_NAME = "GeneralOtchet";
    private static final String OTCHET_EXCEL_FILE_NAME_FORMAT = ".xls";
    private static final String USER_NAME_COL_NAME = "User";
    private static final String GENERAL_OTCHET_FILE_CSV_NAME = "generalOtchet.csv";
    private static final int SPACE_PARAMETER = 20;
    private static final String COLUMN_DELIMITER = "|";
    private static final String FORMAT_STRING = String.join("", "%-", String.valueOf(SPACE_PARAMETER), "s" + COLUMN_DELIMITER);
    private static final String ROW_DELIMITER_PART = "_";
    private static final String ROW_DELIMITER = StringUtils.leftPad(StringUtils.EMPTY, SPACE_PARAMETER, ROW_DELIMITER_PART) + COLUMN_DELIMITER;
    private static final String UNDEFINED_STRING = "none";
    private static final SimpleDateFormat standardDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final String generalFolderFullPath;

    public ChromeHistoryAnalyzer(String path) {
        log.setLevel(Level.INFO);
        this.generalFolderFullPath = path;
        createFolder(this.generalFolderFullPath);
    }

    public void startStatisticProcess() {
        try {
            createStatisticFile(this.generalFolderFullPath);
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
        File[] listOfFiles = folder.listFiles();
        List<String>
                userNameHistory = new ArrayList<>(),
                datesHistory = new ArrayList<>();
        readUserNameAndDate(listOfFiles, userNameHistory, datesHistory);
        Collections.sort(datesHistory);
        List<String> outPutStatistics = fillTableWithVisitInfo(listOfFiles, userNameHistory, datesHistory);
        String csvOutputStatistics = outPutStatistics.get(0);
        String txtOutputStatistics = outPutStatistics.get(1);
        //log.info("CSV OutPutStatistics:\n" + csvOutputStatistics + "\n" + "TXT OutPutStatistics:\n" + txtOutputStatistics);
        String pathToCsvFile = String.valueOf(Paths.get(this.generalFolderFullPath, OTCHET_FOLDER_NAME, GENERAL_OTCHET_FILE_CSV_NAME));
        log.info("Path To Csv File: " + pathToCsvFile);
        PrintWriter pw = new PrintWriter(new File(pathToCsvFile));
        pw.write(csvOutputStatistics);
        pw.close();
        log.info("Writing CSV file, OK");
        Files.write(Paths.get(this.generalFolderFullPath, OTCHET_FOLDER_NAME, GENERAL_OTCHET_FILE_TXT_NAME), Collections.singleton(txtOutputStatistics), StandardCharsets.UTF_8);
        log.info("Writing TXT file, OK");
    }

    private void readUserNameAndDate(File[] listOfFiles, List<String> userHistory, List<String> datesHistory) {
        // let's find out how many users and dates are
        for (int i = 0; i < Objects.requireNonNull(listOfFiles) .length; i++) {
            File file = listOfFiles[i];
            String fileName = file.getName();
            if (file.isFile() && fileName.endsWith(".txt")) {
                String userTxtStr = StringUtils.substringBefore(file.getName(), HISTORY_RES);
                if (!StringUtils.isBlank(userTxtStr) && !userHistory.contains(userTxtStr)) {
                    userHistory.add(userTxtStr);
                }
                String dateTxtStr = StringUtils.substringBetween(fileName, HISTORY_RES, TXT_FORMAT);
                if (!StringUtils.isBlank(dateTxtStr) && !datesHistory.contains(dateTxtStr)) {
                    datesHistory.add(dateTxtStr);
                }
            }
        }
    }

    private List<String> fillTableWithVisitInfo(File[] listOfFiles, List<String> userNameHistory, List<String> datesHistory) throws IOException {
        String[][] table = new String[userNameHistory.size()][datesHistory.size()]; // 1 - arg user, 2 - date
        int userInd = 0;
        for (String userName : userNameHistory) {
            for (int ind = 0; ind < Objects.requireNonNull(listOfFiles).length; ind++) {
                File file = listOfFiles[ind];
                String fileName = file.getName();
                int dateInd = 0;
                for (String date : datesHistory) {
                    String currFileName = userName + HISTORY_RES + date + TXT_FORMAT;
                    if (file.isFile() && fileName.endsWith(TXT_FORMAT) && StringUtils.equals(currFileName, fileName)) {
                        String pathToFile = this.generalFolderFullPath + "/" + RESULT_HISTORY_FOLDER_NAME_PATH + "/" + currFileName;
                        table[userInd][dateInd] = String.valueOf(countNewVisits(pathToFile));
                    }
                    dateInd++;
                }
            }
            userInd++;
        }
        createOtchetXLS(userNameHistory, datesHistory, table);
        return Arrays.asList(createOutMessageCSV(userNameHistory, datesHistory, table), createOutMessageTXT(userNameHistory, datesHistory, table));
    }

    private String createOutMessageTXT(List<String> userNameHistory, List<String> datesHistory, String[][] table) {
        String currentRowDelimiter = IntStream.range(0, datesHistory.size() + 1)
                .mapToObj(i -> ROW_DELIMITER)
                .collect(Collectors.joining(""));
        StringBuilder sb = new StringBuilder();

        sb.append("Otchet date: ").append((new GregorianCalendar()).toZonedDateTime()).append('\n');

        sb.append(currentRowDelimiter.replace(COLUMN_DELIMITER, ROW_DELIMITER_PART)).append("\n");
        sb.append(String.format(FORMAT_STRING, ""));

        for (String ds : datesHistory) {
            sb.append(String.format("%-20s|", ds));
        }
        sb.append("\n");

        sb.append(currentRowDelimiter).append("\n");

        for (int k = 0; k < userNameHistory.size(); k++) {
            sb.append(String.format(FORMAT_STRING, userNameHistory.get(k)));
            for (int t = 0; t < datesHistory.size(); t++) {
                String value = Optional.ofNullable(table[k][t]).orElse(UNDEFINED_STRING);
                String count = StringUtils.leftPad(value, 2, " ");
                sb.append(String.format(FORMAT_STRING, count));
            }
            sb.append("\n");
            sb.append(currentRowDelimiter).append("\n");
        }
        return StringUtils.isNotBlank(sb.toString()) ? sb.toString() : StringUtils.EMPTY;
    }

    private String createOutMessageCSV(List<String> userNameHistory, List<String> datesHistory, String[][] table) {
        StringBuilder sb = new StringBuilder();

        sb.append("Otchet date: ").append((new GregorianCalendar()).toZonedDateTime()).append('\n');
        sb.append(USER_NAME_COL_NAME).append(",").append(String.join(",", datesHistory)).append('\n');

        for (int k = 0; k < userNameHistory.size(); k++) {
            sb.append(userNameHistory.get(k));
            for (int t = 0; t < datesHistory.size(); t++) {
                String count = Optional.ofNullable(table[k][t]).orElse(UNDEFINED_STRING);
                sb.append(",").append(count);
            }
            sb.append("\n");
        }
        return StringUtils.isNotBlank(sb.toString()) ? sb.toString() : StringUtils.EMPTY;
    }

    private void createOtchetXLS(List<String> userNameHistory, List<String> datesHistory, String[][] table) {
        try {
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
            for (int k = 0; k < datesHistory.size(); k++) {
                rowhead.createCell(k + 1).setCellValue(datesHistory.get(k));
            }
            for (int k = 0; k < userNameHistory.size(); k++) {
                HSSFRow newRow = sheet.createRow((short) (k + 2));
                newRow.createCell(0).setCellValue(userNameHistory.get(k));
                for (int t = 0; t < datesHistory.size(); t++) {
                    String value = Optional.ofNullable(table[k][t]).orElse(UNDEFINED_STRING);
                    newRow.createCell(t + 1).setCellValue(value);
                }
            }
            for (int i = 0; i < datesHistory.size() + 1; i++){
                sheet.autoSizeColumn(i);
            }

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
        File file = new File(filePath);
        FileReader fileReader = new FileReader(file);
        BufferedReader reader = new BufferedReader(fileReader);
        String line = reader.readLine();
        int visitNumber = 0;
        while (line != null) {
            Matcher hhMatcher = HH_RU_PATTERN.matcher(line);
            if (hhMatcher.find()) {
                visitNumber++;
            }
            line = reader.readLine();
        }
        return visitNumber;
    }

}