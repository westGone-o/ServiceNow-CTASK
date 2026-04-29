package com.servicenow.ctaskcreator.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class ExcelReader {

    public static Map<String, Double> parseEffortData(String excelPath, String assignedPerson) throws Exception {
        Map<String, Double> efforts = new HashMap<>();
        efforts.put("Analysis", 0.0);
        efforts.put("Design", 0.0);
        efforts.put("Development", 0.0);
        efforts.put("TEST", 0.0);
        efforts.put("TAS", 0.0);
        efforts.put("Documentation", 0.0);
        efforts.put("Deployment", 0.0);

        try (FileInputStream fis = new FileInputStream(excelPath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            // Debug: Print all sheet names
            System.out.println("\n===== Excel File Sheet List =====");
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                System.out.println("Sheet " + i + ": " + workbook.getSheetAt(i).getSheetName());
            }
            System.out.println("=================================\n");

            Sheet sheet = workbook.getSheet("Effort Estimation");
            if (sheet == null) {
                // Case-insensitive search
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    String sheetName = workbook.getSheetAt(i).getSheetName();
                    if (sheetName.toLowerCase().contains("effort") || sheetName.toLowerCase().contains("estimation")) {
                        sheet = workbook.getSheetAt(i);
                        System.out.println("Similar sheet found: " + sheetName);
                        break;
                    }
                }
            }

            if (sheet == null) {
                throw new Exception("'Effort Estimation' sheet not found. Please check the sheet name.");
            }

            System.out.println("Using sheet: " + sheet.getSheetName());
            System.out.println("Total rows: " + sheet.getLastRowNum());

            // First: Find Total effort near row 38 (for Documentation)
            double totalEffort = 0.0;
            for (int rowIdx = 30; rowIdx <= 60; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                for (int colIdx = 0; colIdx < row.getLastCellNum(); colIdx++) {
                    Cell cell = row.getCell(colIdx);
                    if (cell != null && cell.getCellType() == CellType.STRING) {
                        String value = cell.getStringCellValue().trim();
                        if (value.equalsIgnoreCase("Total")) {
                            // Find numeric value to the right of Total
                            for (int nextCol = colIdx + 1; nextCol < row.getLastCellNum(); nextCol++) {
                                Cell effortCell = row.getCell(nextCol);
                                if (effortCell != null) {
                                    double val = getNumericValue(effortCell);
                                    if (val > 0) {
                                        totalEffort = val;
                                        System.out.println("Total effort found (for Documentation): " + totalEffort + "h (row: " + rowIdx + ")");
                                        break;
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
                if (totalEffort > 0) break;
            }

            // Second: Find header near row 46 (Module, Type, Estimated M/H, PIC)
            Row headerRow = null;
            int headerRowIndex = -1;
            int typeCol = -1;
            int effortCol = -1;
            int picCol = -1;

            for (int rowIdx = 35; rowIdx <= 100; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                boolean hasModule = false;
                boolean hasType = false;
                boolean hasPIC = false;

                for (int colIdx = 0; colIdx < row.getLastCellNum(); colIdx++) {
                    Cell cell = row.getCell(colIdx);
                    if (cell != null && cell.getCellType() == CellType.STRING) {
                        String value = cell.getStringCellValue().trim();
                        if (value.equalsIgnoreCase("Module")) hasModule = true;
                        if (value.equalsIgnoreCase("Type")) {
                            hasType = true;
                            typeCol = colIdx;
                        }
                        if (value.equalsIgnoreCase("PIC")) {
                            hasPIC = true;
                            picCol = colIdx;
                        }
                        if (value.contains("Estimated") || value.contains("M/H")) {
                            effortCol = colIdx;
                        }
                    }
                }

                if (hasModule && hasType && hasPIC) {
                    headerRow = row;
                    headerRowIndex = rowIdx;
                    System.out.println("Detail header row found: row " + rowIdx);
                    System.out.println("   Type col: " + typeCol + ", Effort col: " + effortCol + ", PIC col: " + picCol);
                    break;
                }
            }

            if (headerRow == null) {
                throw new Exception("Detail header row (Module, Type, PIC) not found (searched rows 40-50)");
            }

            // Third: Aggregate effort by assignee
            System.out.println("\n===== Effort Summary for " + assignedPerson + " =====");
            String currentType = null;

            for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell typeCell = row.getCell(typeCol);
                Cell effortCell = row.getCell(effortCol);
                Cell picCell = row.getCell(picCol);

                // Read Type (keep previous value if merged)
                if (typeCell != null && typeCell.getCellType() == CellType.STRING) {
                    String type = typeCell.getStringCellValue().trim();
                    if (!type.isEmpty()) {
                        currentType = type;
                    }
                }

                // Check PIC
                String pic = "";
                if (picCell != null && picCell.getCellType() == CellType.STRING) {
                    pic = picCell.getStringCellValue().trim();
                }

                // Add effort only if matches assignedPerson
                if (pic.contains(assignedPerson) && currentType != null) {
                    double effort = getNumericValue(effortCell);

                    if (effort > 0) {
                        System.out.println("  Row " + i + ": Type=[" + currentType + "], Effort=" + effort + "h");

                        // Accumulate effort by Type
                        String typeKey = currentType.replaceAll("\\s+", "").replaceAll("\\(.*?\\)", "");

                        if (typeKey.equalsIgnoreCase("Analysis")) {
                            efforts.put("Analysis", efforts.get("Analysis") + effort);
                        } else if (typeKey.equalsIgnoreCase("Design")) {
                            efforts.put("Design", efforts.get("Design") + effort);
                        } else if (typeKey.equalsIgnoreCase("Development")) {
                            efforts.put("Development", efforts.get("Development") + effort);
                        } else if (typeKey.equals("TEST")) {
                            // "TEST" (uppercase) -> TEST category
                            efforts.put("TEST", efforts.get("TEST") + effort);
                        } else if (typeKey.contains("TestAutomation")) {
                            // "Test Automation system" -> TAS category
                            efforts.put("TAS", efforts.get("TAS") + effort);
                        } else if (typeKey.equalsIgnoreCase("Documentation") || typeKey.equalsIgnoreCase("Document")) {
                            // Documentation ignores individual effort, uses Total later
                        } else if (typeKey.equalsIgnoreCase("Deployment")) {
                            efforts.put("Deployment", efforts.get("Deployment") + effort);
                        }
                    }
                }
            }

            // Documentation uses Total value
            if (totalEffort > 0) {
                efforts.put("Documentation", totalEffort);
                System.out.println("  Documentation: Using Total value = " + totalEffort + "h");
            }

            System.out.println("\n===== Final Effort Summary =====");
            System.out.println("Analysis: " + efforts.get("Analysis") + "h");
            System.out.println("Design: " + efforts.get("Design") + "h");
            System.out.println("Development: " + efforts.get("Development") + "h");
            System.out.println("TEST: " + efforts.get("TEST") + "h");
            System.out.println("TAS: " + efforts.get("TAS") + "h");
            System.out.println("Documentation: " + efforts.get("Documentation") + "h");
            System.out.println("Deployment: " + efforts.get("Deployment") + "h");
            System.out.println("================================\n");
        }

        return efforts;
    }

    // Helper method to extract numeric value from cell
    private static double getNumericValue(Cell cell) {
        if (cell == null) return 0.0;

        if (cell.getCellType() == CellType.FORMULA) {
            CellType resultType = cell.getCachedFormulaResultType();
            if (resultType == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            }
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue().replaceAll("[^0-9.]", ""));
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
}