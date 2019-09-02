/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.Mail.Test;

import java.io.File;
import java.io.IOException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 *
 * @author kalyptus
 */
public class POIExcelTest {
   public static void main(String[] args) throws IOException, InvalidFormatException{
      Workbook wb = WorkbookFactory.create(new File("D:/GGC_Java_Systems/temp/download/M00115000003/SSRF.xlsX")); // Or .xlsx
      Sheet s = wb.getSheet("Sheet1");
      //Access B7
      Row r1 = s.getRow(6);
      System.out.println(r1.getCell(1).getStringCellValue());
   }   
}
