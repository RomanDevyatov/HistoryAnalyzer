package com.rd.utils;

import com.rd.ChromeHistoryAnalyzer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class FileUtility {

    private static final Logger log = Logger.getLogger(FileUtility.class.getName());

    public FileUtility() {}

    protected void createFolder(String path) {
        String pathToOtchetTxtFile = String.valueOf(Paths.get(path, ChromeHistoryAnalyzer.OTCHET_FOLDER_NAME));
        String msg = "Folder " + pathToOtchetTxtFile;
        msg += new File(pathToOtchetTxtFile).mkdirs() ? " is created" : " already exists";
        log.info(msg);
    }

    protected void createStatisticFile(String path) throws IOException {
        createFolder(path);
        Path filePath = Paths.get(path, ChromeHistoryAnalyzer.OTCHET_FOLDER_NAME, ChromeHistoryAnalyzer.GENERAL_OTCHET_FILE_TXT_NAME);
        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
        } else {
            new FileOutputStream(String.valueOf(filePath)).close();
        }
    }
}
