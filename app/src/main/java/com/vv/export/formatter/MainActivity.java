package com.vv.export.formatter;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.vv.export.formatter.port.Export;
import com.vv.export.formatter.util.Helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.vv.export.formatter.util.Constants.CORE_STR;
import static com.vv.export.formatter.util.Constants.CORE_STR_FILE;
import static com.vv.export.formatter.util.Constants.FACTOR_GB;
import static com.vv.export.formatter.util.Constants.FACTOR_KB;
import static com.vv.export.formatter.util.Constants.FACTOR_MB;
import static com.vv.export.formatter.util.Constants.SLEEP_TIME_MONITORING_FILE;

/**
 * @author Vivek Verma
 * @since 30/8/20
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainActivity.class);

    private final Activity context = this;
    private Button initiateButton;
    private EditText upperLimitInGB;
    private TextView displaySize;
    private Handler handler;

    private ExecutorService formatTaskThread;
    private ScheduledExecutorService monitoringThread;
    private Export export;
    private String filePath = "";
    private boolean continueFilling = true;
    private boolean oom = false;
    private double upperLimit = 1.0; //in GB

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initiateButton = findViewById(R.id.initiateButton);
        initiateButton.setOnClickListener(this);

        upperLimitInGB = findViewById(R.id.upperLimitInGB);
        displaySize = findViewById(R.id.displaySize);

        formatTaskThread = Executors.newSingleThreadExecutor();
        monitoringThread = Executors.newSingleThreadScheduledExecutor();
        monitoringThread.scheduleAtFixedRate(this::monitorFileSize, 0, SLEEP_TIME_MONITORING_FILE, TimeUnit.SECONDS);

        export = new Export(context);
        handler = new Handler();
    }

    @Override
    public void onClick(View v) {
        if (v == initiateButton) {
            LOGGER.info("Initiating Formatting!");

            Editable editable = upperLimitInGB.getText();
            if (editable == null || editable.length() == 0) {
                Helper.shortToaster(context, "Enter an upper limit in GB!");
                LOGGER.warn("No upper limit detected as input. Skipping this iteration.");
                return;
            }
            Double upperLimit = Double.parseDouble(editable.toString());
            LOGGER.info("Upper limit set: {}", upperLimit);
            setUpperLimit(upperLimit);
            Helper.shortToaster(context, String.format("Upper limit: %.2f GB", upperLimit));

            boolean creationResult = export.touch(CORE_STR_FILE);
            LOGGER.info("Creation result of the empty filer: {}", creationResult);
            if (creationResult) {
                initiateButton.setEnabled(false);
                setFilePath(export.getSrcFilePath());
                setContinueFilling(true);

                Runnable task = generateFormattingTask();
                formatTaskThread.submit(task);
            } else {
                LOGGER.warn("As failed to create empty file. Check logs for warning if any.");
            }
        }
    }

    private Runnable generateFormattingTask() {
        return () -> {
            LOGGER.info("Initiating file filling for {}, filling status: {}", getFilePath(), isContinueFilling());
            File file = new File(getFilePath());
            while (isContinueFilling()) {
                try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true))) {
                    bufferedWriter.write(CORE_STR);
                } catch (IOException e) {
                    LOGGER.error("Failed to augment file size. Err:", e);
                    setContinueFilling(false);
                    setOom(true);
                }
                /*try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
            }
        };
    }

    private void monitorFileSize() {
        if (getFilePath().isEmpty()) return;

        File file = new File(getFilePath());
        boolean exists = file.exists();
        LOGGER.info("File '{}', exists: {}", file.getName(), exists);
        if (exists) {
            LOGGER.info("File '{}' size: {}", file.getName(), getFileSize(file));
            double sizeGb = file.length() / FACTOR_GB;
            setContinueFilling(sizeGb < getUpperLimit() && !isOom());
            LOGGER.info("Verdict on filling: {}", continueFilling);
            try {
                handler.post(() -> {
                    try {
                        double sizeMb = file.length() / FACTOR_MB;
                        displaySize.setText(String.format("%.2f MB", sizeMb));
                    } catch (Exception e) {
                        LOGGER.error("Internal post error. ", e);
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Error while updating ");
            }
        }
    }

    private String getFileSize(File file) {
        long size = file.length(); //in bytes
        return String.format("%d B -> %.2f KB -> %.2f MB -> %.2f GB",
                size,
                size / FACTOR_KB,
                size / FACTOR_MB,
                size / FACTOR_GB);
    }

    @Override
    protected void onDestroy() {
        LOGGER.info("Shutting down all executors!");
        formatTaskThread.shutdown();
        monitoringThread.shutdown();
        super.onDestroy();
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isContinueFilling() {
        return continueFilling;
    }

    public void setContinueFilling(boolean continueFilling) {
        this.continueFilling = continueFilling;

        if (!continueFilling) {
            handler.post(() -> {
                initiateButton.setEnabled(true);
                initiateButton.setClickable(true);
            });
        }
    }

    public double getUpperLimit() {
        return upperLimit;
    }

    public void setUpperLimit(double upperLimit) {
        this.upperLimit = upperLimit;
    }

    public boolean isOom() {
        return oom;
    }

    public void setOom(boolean oom) {
        this.oom = oom;
    }
}