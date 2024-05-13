package com.automation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.WebDriver;

public class ClassSchedule {
    
    
    public WebDriver driver;
    private final String DAY_ORDER_FILE = "./assets/day_order.txt";
    Map<Integer, Map<LocalTime, String>> schedule = new HashMap<>();
    private LocalDate lastRunDate = null;
    private int dayOrder;
    public ClassSchedule(WebDriver driver) {
        this.driver = driver;           
    }   

    public Map<Integer, Map<LocalTime, String>> setupExcel(String SCHEDULE_SHEET_PATH) throws IOException {
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
            System.out.println("Failed to read or parse the Excel file");
        }
        return schedule;
    }

    public Map<String,String> setupMeetings(String JSON_FILE_PATH) {
        Map<String, String> meetingLinks = new HashMap<>();
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
            System.out.println("Failed to read or parse the JSON file");
        }
        return meetingLinks;
    }

    public void readDayOrderFromFile() throws IOException {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(DAY_ORDER_FILE));
            String line = reader.readLine();
            String[] parts = line.split(","); // Assuming comma (",") as delimiter
            this.dayOrder = Integer.parseInt(parts[0]);
            this.lastRunDate = LocalDate.parse(parts[1]);
            reader.close();
        } catch (FileNotFoundException e) {
            // Handle the case where the file doesn't exist initially (set default day order)
            System.out.println("Day order file not found. Setting default day order to 1.");
        }
    }

    public void writeDayOrderToFile() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(DAY_ORDER_FILE));
        writer.write(dayOrder + "," + LocalDate.now().toString());
        writer.close();
    }

    public void incrementDayOrder() {
        LocalDate today = LocalDate.now();
        if (lastRunDate == null || !lastRunDate.equals(today)) {
            //Increment day order
            dayOrder++;
            if(dayOrder > 5)
            {
                dayOrder = 1;
            }
            lastRunDate = today;
        }
    }

    public int getDayOrder() throws IOException{
        readDayOrderFromFile();
        incrementDayOrder();
        writeDayOrderToFile();
        return dayOrder;
    }   
    

}
