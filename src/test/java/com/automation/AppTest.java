package com.automation;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.automation.utils.PropertiesUtil;
import com.aventstack.extentreports.ExtentReports;

class AppTest {

    private final PropertiesUtil props = new PropertiesUtil();

    private final String EXECUTABLE_PATH = props.getProperty("EXECUTABLE_PATH");
    private final String USER_DATA_DIR = props.getProperty("USER_DATA_DIR");
    private final String PROFILE_DIRECTORY = props.getProperty("PROFILE_DIRECTORY");

    private final String SCHEDULE_SHEET_PATH = "./assets/schedule/schedule.xlsx";
    private final String REPORT_PATH = "./out/reports/index.html";
    private final String LOGGER_PATH = "./out/logs/app.log";
    private final String SCREENSHOT_PATH = "./out/screenshots/";
    private final String PDF_PATH = "./out/pdf/";

    private WebDriver driver;
    private Actions actions;
    private ExtentReports reports;
    private Wait<WebDriver> wait;

    private Logger logger = LogManager.getLogger(getClass());

    @BeforeTest
    public void setupDriver() {
        logger.info("Setting up Chrome driver...");
        logger.info("Creating ChromeOptions object...");

        ChromeOptions options = new ChromeOptions();

        logger.info("Setting executable path to " + EXECUTABLE_PATH);
        options.setBinary(EXECUTABLE_PATH);

        logger.info("Setting user data directory to " + USER_DATA_DIR);
        options.addArguments("--user-data-dir=" + USER_DATA_DIR);

        logger.info("Setting profile directory to " + PROFILE_DIRECTORY);
        options.addArguments("--profile-directory=" + PROFILE_DIRECTORY);

        logger.info("Creating ChromeDriver object.");

        this.driver = new ChromeDriver(options);
        this.actions = new Actions(driver);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        logger.info("Chrome driver setup complete.");
    }

    @BeforeTest
    public void setupExcel() throws IOException {

        Workbook workbook = new XSSFWorkbook(SCHEDULE_SHEET_PATH);
        Sheet sheet = workbook.getSheetAt(0);

        // TODO: Implement Excel read logic here

        workbook.close();
    }

    @Test
    void testAttendOnlineClasses() {

        // Get the next class link

        // Join the class

        // Wait for it to end

        // Repeat until all classes are attended

    }

    @AfterTest
    public void wrapUp() {
        logger.info("Wrapping up...");
        logger.info("Quitting WebDriver");
        driver.quit();
        // reports.flush();
        logger.info("Wrap-up complete");
    }

    // All your private function goes here lexigraphically
    private void takeScreenshot(String name) throws IOException {
        logger.info("Taking Screenshot...");

        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String screenshotPath = SCREENSHOT_PATH + name + "_" + timestamp + ".png";
        File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

        FileUtils.copyFile(screenshotFile, new File(screenshotPath));
        logger.info("Screenshot saved at " + screenshotPath);
    }
}
