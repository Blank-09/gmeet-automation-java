package com.automation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
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
    private final String JSON_FILE_PATH = "./assets/meet_links.json";
    private final String DAY_ORDER_FILE = "./assets/day_order.txt";

    private LocalDate lastRunDate = null;
    private int dayOrder;

    private WebDriver driver;
    private Actions actions;
    private ExtentReports reports;
    private Wait<WebDriver> wait;

    private Logger logger = LogManager.getLogger(getClass());

    private Map<Integer, Map<LocalTime, String>> schedule = new HashMap<>();
    private Map<String, String> meetingLinks = new HashMap<>();

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
        try {
            FileInputStream file = new FileInputStream(new File(SCHEDULE_SHEET_PATH));
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            Sheet sheet = workbook.getSheetAt(0);

            List<String> headers = new ArrayList<>();
            Row headerRow = sheet.getRow(0);
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                headers.add(headerRow.getCell(i).getStringCellValue());
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                int currDayOrder = (int) row.getCell(0).getNumericCellValue();
                Map<LocalTime, String> daySchedule = new TreeMap<>();

                for (int j = 1; j < row.getLastCellNum(); j++) {
                    String subject = row.getCell(j).getStringCellValue();
                    LocalTime startTime = LocalTime.parse(headers.get(j).split(" - ")[0].trim());

                    daySchedule.put(startTime, subject);
                }
                schedule.put(currDayOrder, daySchedule);
            }

            workbook.close();
            file.close();

        } catch (IOException e) {
            logger.error("Failed to read or parse the Excel file", e);
        }
    }

    @BeforeTest
    public void setupMeetings() {
        try {
            FileReader fileReader = new FileReader(JSON_FILE_PATH);
            JsonReader reader = Json.createReader(fileReader);
            JsonObject jsonObject = reader.readObject();

            jsonObject.forEach((subject, subjectValue) -> {
                JsonObject subjectObject = (JsonObject) subjectValue;
                String meetLink = subjectObject.getString("meetLink");
                meetingLinks.put(subject, meetLink);
            });

        } catch (IOException e) {
            logger.error("Failed to read or parse the JSON file", e);
        }
    }

    
    @Test
    void testAttendOnlineClasses() throws Exception{
        logger.info("Joining online classes...");

        dayOrder = readDayOrderFromFile();

        LocalDate today = LocalDate.now();
        if (lastRunDate == null || !lastRunDate.equals(today)) {
            dayOrder = (dayOrder + 1) % 6; // Increment day order if new day
            lastRunDate = today;
        }

        writeDayOrderToFile(dayOrder);

        Map<LocalTime, String> dailySchedule = schedule.get(dayOrder);
        if (dailySchedule == null) {
            logger.warn("No schedule found for the specified day order.");
            return;
        }

        for (Map.Entry<LocalTime, String> entry : dailySchedule.entrySet()) {
            LocalTime currentTime = LocalTime.now();
            LocalTime classStartTime = entry.getKey();
            String subject = entry.getValue();

            if (currentTime.isAfter(classStartTime.plusHours(1))) {
                logger.info("Class already ended: " + subject);
                continue;
            }

            if (currentTime.isBefore(classStartTime)) {
                long sleepTime = Duration.between(currentTime, classStartTime).toMillis();
                logger.info("Waiting for the next class: " + subject);
                logger.info("Sleeping for " + sleepTime + " milliseconds");
                Thread.sleep(sleepTime);
            }

            String meetLink = meetingLinks.get(subject);
            if (meetLink != null) {
                logger.info("Joining class: " + subject);
                logger.info("Meet Link: " + meetLink);
                driver.navigate().to(meetLink);

                // Interactions to mute and disable camera
                Thread.sleep(1000);
                actions.keyDown(Keys.CONTROL).sendKeys("d").keyUp(Keys.CONTROL).perform();

                // Join the meeting after 1 minute
                Thread.sleep(1000 * 60);
                driver.findElement(By.xpath("/html/body/div[1]/c-wiz/div/div/div[25]/div[3]/div/div[2]/div[4]/div/div/div[2]/div[1]/div[2]/div[1]/div[1]/button")).click();

                // Wait for the class to end
                long secondsUntilClassEnds = ChronoUnit.SECONDS.between(currentTime, classStartTime.plusHours(1));
                Thread.sleep(secondsUntilClassEnds * 1000);

                //Leave the meeting
                driver.findElement(By.xpath("/html/body/div[1]/c-wiz/div[1]/div/div[24]/div[3]/div[10]/div/div/div[2]/div/div[8]/span/button")).click();
                logger.info("Left the meeting: " + subject);
            } else {
                logger.warn("No meet link found for subject: " + subject);
            }
        }

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

    private int readDayOrderFromFile() throws IOException {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(DAY_ORDER_FILE));
            String line = reader.readLine();
            if (line != null) {
                String[] parts = line.split(","); // Assuming comma (",") as delimiter
                dayOrder = Integer.parseInt(parts[0]);
                lastRunDate = LocalDate.parse(parts[1]);
            }
            reader.close();
            return dayOrder;
        } catch (FileNotFoundException e) {
            // Handle the case where the file doesn't exist initially (set default day order)
            System.out.println("Day order file not found. Setting default day order to 1.");
            return 1;
        }
    }

    private void writeDayOrderToFile(int dayOrder) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(DAY_ORDER_FILE));
        writer.write(dayOrder + "," + LocalDate.now().toString());
        writer.close();
    }


}
