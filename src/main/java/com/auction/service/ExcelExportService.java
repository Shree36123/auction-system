package com.auction.service;

import com.auction.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Service to export player and team data to Excel (.xlsx) files.
 */
@Service
public class ExcelExportService {

    /**
     * Export players to Excel.
     * Columns: Full Name, Phone Number, Category, Jersey Size, Base Price, Status, Team, Sold Price, Achievements
     */
    public byte[] exportPlayersToExcel(List<Player> players) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Players");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Full Name", "Phone Number", "Category", "Jersey Size", "Base Price", "Status", "Team", "Sold Price", "Achievements"};
            CellStyle headerStyle = createHeaderStyle(workbook);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            int rowNum = 1;
            for (Player player : players) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(player.getFullName());
                row.createCell(1).setCellValue(player.getPhoneNumber());
                row.createCell(2).setCellValue(player.getCategory().getLabel());
                row.createCell(3).setCellValue(player.getJerseySize());

                Cell basePriceCell = row.createCell(4);
                basePriceCell.setCellValue(player.getBasePrice());
                basePriceCell.setCellStyle(currencyStyle);

                row.createCell(5).setCellValue(player.getStatus().name());
                row.createCell(6).setCellValue(player.getTeam() != null ? player.getTeam().getName() : "");

                Cell soldPriceCell = row.createCell(7);
                soldPriceCell.setCellValue(player.getSoldPrice());
                soldPriceCell.setCellStyle(currencyStyle);

                row.createCell(8).setCellValue(player.getAchievements() != null ? player.getAchievements() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Export teams to Excel.
     * Columns: Team Name, Budget, Remaining Budget, Players Count, Owners, Logo Path
     */
    public byte[] exportTeamsToExcel(List<Team> teams) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Teams");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Team Name", "Budget", "Remaining Budget", "Players Count", "Slots Remaining", "Owners"};
            CellStyle headerStyle = createHeaderStyle(workbook);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            int rowNum = 1;
            for (Team team : teams) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(team.getName());

                Cell budgetCell = row.createCell(1);
                budgetCell.setCellValue(team.getTotalBudget());
                budgetCell.setCellStyle(currencyStyle);

                Cell remainingCell = row.createCell(2);
                remainingCell.setCellValue(team.getRemainingBudget());
                remainingCell.setCellStyle(currencyStyle);

                row.createCell(3).setCellValue(team.getPlayerCount());
                row.createCell(4).setCellValue(team.getSlotsRemaining());
                row.createCell(5).setCellValue(team.getOwnerNames());
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Export auction data (players with their sold/unsold status).
     * Columns: Full Name, Category, Base Price, Status, Team, Sold Price, Auction Timestamp
     */
    public byte[] exportAuctionDataToExcel(List<Player> players) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Auction Data");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Full Name", "Category", "Base Price", "Status", "Team", "Sold Price", "Phone Number", "Achievements"};
            CellStyle headerStyle = createHeaderStyle(workbook);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            int rowNum = 1;
            for (Player player : players) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(player.getFullName());
                row.createCell(1).setCellValue(player.getCategory().getLabel());

                Cell basePriceCell = row.createCell(2);
                basePriceCell.setCellValue(player.getBasePrice());
                basePriceCell.setCellStyle(currencyStyle);

                row.createCell(3).setCellValue(player.getStatus().name());
                row.createCell(4).setCellValue(player.getTeam() != null ? player.getTeam().getName() : "");

                Cell soldPriceCell = row.createCell(5);
                soldPriceCell.setCellValue(player.getSoldPrice());
                soldPriceCell.setCellStyle(currencyStyle);

                row.createCell(6).setCellValue(player.getPhoneNumber());
                row.createCell(7).setCellValue(player.getAchievements() != null ? player.getAchievements() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("₹#,##0.00"));
        return style;
    }
}
