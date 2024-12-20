/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package edu.ucar.unidata.rosetta.converters;


import jxl.Cell;
import jxl.CellType;
import jxl.DateCell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Locale;


/**
 * Convert xls, xlsx files to a csv file
 *
 * @author Sean Arms
 */
public class XlsToCsv {

    /**
     * If a cell is empty, set the value in the CSV file to -999
     */
    private static final String missingFillValue = "-999";

    /**
     * _more_
     *
     * @param xlsFile Path to xls/xlsx file
     * @param csvFile Path where csv file should be created. If null, csv file will
     *                be created in the same location as @param xlsFile
     */
    public static boolean convert(String xlsFile, String csvFile) {
        boolean successful = false;
        try {
            //Excel document to be imported
            WorkbookSettings ws = new WorkbookSettings();
            ws.setLocale(new Locale("en", "EN"));
            Workbook w = Workbook.getWorkbook(new File(xlsFile), ws);

            //File to store data in form of CSV
            if (csvFile == null) {
                csvFile = xlsFile.replaceFirst("[.][^./]+$", ".csv");
/*                 if (xlsFile.contains(".xlsx")) {
                    csvFile = xlsFile.replace(".xls", ".csv");
                } else {
                    csvFile = xlsFile.replace(".xls", ".csv");
                }
 */            }

            File f = new File(csvFile);
            // if file doesnt exists, then create it
            if (!f.exists()) {
                f.createNewFile();
            }
            OutputStream os = new FileOutputStream(f);
            String encoding = "UTF8";
            OutputStreamWriter osw = new OutputStreamWriter(os,
                    encoding);
            BufferedWriter bw = new BufferedWriter(osw);

            // Gets the sheets from workbook
            for (int sheet = 0; sheet < w.getNumberOfSheets(); sheet++) {
                Sheet s = w.getSheet(sheet);

                //bw.write(s.getName());
                //bw.newLine();

                Cell[] row = null;

                // Gets the cells from sheet
                String contents;
                for (int i = 0; i < s.getRows(); i++) {
                    row = s.getRow(i);
                    if (row.length > 0) {
                        Boolean rowIsEmpty = Boolean.TRUE;
                        Boolean skipFirstComma = Boolean.FALSE;
                        contents = getCellContents(row[0]);
                        if (!contents.equals("")) {
                            rowIsEmpty = Boolean.FALSE;
                            bw.write(contents);
                        } else {
                            skipFirstComma = Boolean.TRUE;
                        }
                        for (int j = 1; j < row.length; j++) {
                            contents = getCellContents(row[j]);
                            if (!contents.equals("")) {
                                rowIsEmpty = Boolean.FALSE;
                                if (!skipFirstComma) {
                                    bw.write(',');
                                    bw.write(contents);
                                } else {
                                    skipFirstComma = Boolean.FALSE;
                                    bw.write(contents);
                                }
                            } else {
                                if ((j != row.length - 1) && (!skipFirstComma)) {
                                    bw.write(" ");
                                }
                                skipFirstComma = Boolean.TRUE;
                            }
                        }
                        if (!rowIsEmpty) {
                            bw.newLine();
                        }
                    }
                }
            }
            bw.flush();
            bw.close();
            successful = true;
        } catch (UnsupportedEncodingException e) {
            System.err.println(e.toString());
        } catch (IOException e) {
            System.err.println(e.toString());
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        return successful;
    }

    /**
     * Get the contents of a cell. If the cell @param row is formatted as a date,
     * then convert the value to seconds since 1970-01-01.
     *
     * @param row Cell object representing a row
     * @return String reprensetation of value contained within the row
     */
    private static String getCellContents(Cell row) {
        String contents = "";

        if (row.getType() == CellType.DATE) {
            DateCell dc = (DateCell) row;
            Date cellDate = dc.getDate();
            // epoch - milliseconds since 1970-01-01
            long epoch = cellDate.getTime();
            // contents - seconds since 1970-01-01
            contents = Long.toString(epoch / 1000);
        } else {
            contents = row.getContents();
            contents = checkCellContents(contents);
        }
        return contents;
    }

    /**
     * Check the cell contents for a missing value. If cell is empty
     * set the cell content value to missingFillValue.
     *
     * @param contents the contents of a cell of the spreadsheet
     * @return contents with the missing value replaced
     */
    private static String checkCellContents(String contents) {
        if (contents.endsWith(",")) {
            contents = contents.replace(",", "");
        }
        if (contents.equals("---")) {
            contents = missingFillValue;
        }
        return contents;
    }
}
