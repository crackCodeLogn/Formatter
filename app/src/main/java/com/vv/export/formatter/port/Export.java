package com.vv.export.formatter.port;

import android.app.Activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author Vivek Verma
 * @since 30/8/20
 */
public class Export {

    private static final Logger LOGGER = LoggerFactory.getLogger(Export.class);
    private final Activity context;
    private String srcFilePath = "";

    public Export(Activity context) {
        this.context = context;
    }

    public boolean touch(String fileName) {
        File sdCardDir = context.getFilesDir();
        LOGGER.info("Retrieved sdCardDir: {}", sdCardDir);
        File srcFile = new File(sdCardDir, fileName);
        setSrcFilePath(srcFile.getAbsolutePath());

        if (srcFile.exists()) return true;
        try {
            return srcFile.createNewFile();
        } catch (Exception e) {
            LOGGER.warn("Failed to touch file: {}. Err: ", getSrcFilePath(), e);
        }
        return false;
    }

    public String getSrcFilePath() {
        return srcFilePath;
    }

    public void setSrcFilePath(String srcFilePath) {
        this.srcFilePath = srcFilePath;
    }
}
