package com.meera.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads rows from an {@code .xlsx} sheet into a list of column-name → value maps.
 *
 * <p>This is the Java equivalent of the TypeScript {@code utils/data-utils.js}
 * {@code getTestData(filePath, sheetName)} that used the {@code xlsx} package's
 * {@code sheet_to_json}. Every value is returned as a trimmed String, which
 * matches how the original tests consumed the data.</p>
 */
public final class ExcelDataReader {

    private static final DataFormatter FORMATTER = new DataFormatter();

    private ExcelDataReader() {
    }

    /**
     * Reads a worksheet into a list of maps keyed by the header row.
     *
     * @param filePath  path to the workbook, relative to the project root
     *                  (e.g. {@code "test-data/campaign-data.xlsx"})
     * @param sheetName worksheet name (e.g. {@code "GeneralSettings"})
     * @return one map per data row; blank rows are skipped
     */
    public static List<Map<String, String>> getTestData(String filePath, String sheetName) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("Test data file not found: " + file.getAbsolutePath());
        }

        List<Map<String, String>> rows = new ArrayList<>();

        try (InputStream in = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(in)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new RuntimeException(
                        "Sheet \"" + sheetName + "\" not found in " + filePath);
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return rows;
            }

            List<String> headers = new ArrayList<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                headers.add(cell == null ? "" : FORMATTER.formatCellValue(cell).trim());
            }

            for (int r = headerRow.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }

                Map<String, String> record = new LinkedHashMap<>();
                boolean hasValue = false;

                for (int c = 0; c < headers.size(); c++) {
                    String header = headers.get(c);
                    if (header == null || header.isEmpty()) {
                        continue;
                    }
                    Cell cell = row.getCell(c);
                    String value = cell == null ? "" : FORMATTER.formatCellValue(cell).trim();
                    if (!value.isEmpty()) {
                        hasValue = true;
                    }
                    record.put(header, value);
                }

                if (hasValue) {
                    rows.add(record);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + filePath + " [" + sheetName + "]", e);
        }

        return rows;
    }

    /**
     * Convenience for TestNG {@code @DataProvider} methods, which must hand back
     * an {@code Object[][]} where each row is a single argument (the map).
     */
    public static Object[][] getTestDataAsProvider(String filePath, String sheetName) {
        List<Map<String, String>> data = getTestData(filePath, sheetName);
        Object[][] out = new Object[data.size()][1];
        for (int i = 0; i < data.size(); i++) {
            out[i][0] = data.get(i);
        }
        return out;
    }
}
