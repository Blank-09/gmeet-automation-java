package com.automation;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private int dayOrder;

    private WebDriver driver;
    private Actions actions;
    private ExtentReports reports;
    private Wait<WebDriver> wait;

    private Logger logger = LogManager.getLogger(getClass());

    private Map<Integer, Map<LocalTime, String>> schedule = new HashMap<>();
    private Map<String, String> meetingLinks = new HashMap<>();

    private ClassSchedule classSchedule;
    
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
        this.classSchedule = new ClassSchedule(driver);

        logger.info("Chrome driver setup complete.");
    }

    @BeforeTest
    public void setupSchedule() throws IOException {
        logger.info("Setting up Excel file...");
        schedule = classSchedule.setupExcel(SCHEDULE_SHEET_PATH);
        logger.info("Excel file setup complete.");

        logger.info("Setting up meetings...");  
        meetingLinks = classSchedule.setupMeetings(JSON_FILE_PATH);
        logger.info("Meetings setup complete.");

        logger.info("Setting up day order...");
        dayOrder = classSchedule.getDayOrder();
        logger.info("Day order setup complete.");
    }

    @Test
    void testAttendOnlineClasses() throws Exception{
        logger.info("Joining online classes... for day order: " + dayOrder);
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

                // Interactions to mute mic
                Thread.sleep(2000);
                actions.keyDown(Keys.CONTROL).sendKeys("d").keyUp(Keys.CONTROL).perform();
                
                // Interactions to turn off camera
                // Thread.sleep(2000);
                // actions.keyDown(Keys.CONTROL).sendKeys("e").keyUp(Keys.CONTROL).perform();

                // Join the meeting
                driver.findElement(By.xpath("/html/body/div[1]/c-wiz/div/div/div[25]/div[3]/div/div[2]/div[4]/div/div/div[2]/div[1]/div[2]/div[1]/div[1]/button")).click();
                logger.info("Joined the meeting: " + subject);

                // Wait for the class to end
                long secondsUntilClassEnds = ChronoUnit.SECONDS.between(currentTime, classStartTime.plusHours(1));
                Thread.sleep(secondsUntilClassEnds * 1000);

                //Leave the meeting
                driver.findElement(By.cssSelector("button[aria-label='Leave call']")).click();
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


}
