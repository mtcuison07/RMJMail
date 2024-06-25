/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.App;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JsonDataSource;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rmj.appdriver.GProperty;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agent.GRiderX;
import org.rmj.lib.net.WebClient;
import org.rmj.xapitoken.util.RequestAccess;

/**
 *
 * @author sayso
 */
public class SendMacroValidation {
    private static GRiderX instance = null;
    
    //PayrollValidation_Macro [config] [paydiv] [div] [emptype] [period] [requestedby]
    public static void main(String args[]) throws SQLException, UnsupportedEncodingException, JRException, Exception{
        String path = "";
        String cDivision = "";
        String sEmpTypID = "";
        String dPeriodFr = "";
        String dPeriodTr = "";
        String sRqstdByx = "";
        String sTransNox = "";
        
        if(args.length == 0){
            path = "D:/GGC_Java_Systems";
            //0123457
            cDivision = "6";    //Mobile Phone
            sEmpTypID = "R";    //Regular
            //sEmpTypID = "T";
            dPeriodFr = "2023-10-01";
            sRqstdByx = "M033070005";
        }
        else if(args.length == 5){
            path = args[0];
            cDivision = args[1];
            sEmpTypID = args[2];
            dPeriodFr = args[3];
            sRqstdByx = args[4];
        }
        else{
            System.out.println("Invalid parameters detected...");
            System.exit(0);
        }

        System.setProperty("sys.default.path.temp", path + "/temp");
        System.setProperty("sys.default.path.config", path);
        System.setProperty("sys.default.LogFile", path + "/temp/SendMacroValidation.log");
        System.setProperty("sys.default.Signature", path + "/images/180492153_794111184853448_4384690834553115402_n.png");
        
        writeLog("++++++++++++++++++++++++++++++++++++++++++++++");
        writeLog("SendMacroValidation Initiated: " + Calendar.getInstance().getTime());
        
        instance = new GRiderX("gRider");
        instance.setOnline(true);

        sTransNox = getTransNo(cDivision, sEmpTypID, dPeriodFr, sRqstdByx);
        
        String sql = "SELECT b.sDivsnDsc, b.sPreAudtr, a.*" + 
                    " FROM Payroll_Validation_Macro_Master a" + 
                        " LEFT JOIN Division b ON a.cDivision = b.sDivsnCde" +
                    " WHERE a.sTransNox = " + SQLUtil.toSQL(sTransNox) ; 
        System.out.println(sql);

        ResultSet rsMstr = instance.executeQuery(sql);
        //rs = instance.executeQuery(sql);
        
        if(!rsMstr.next()){
            writeLog("Send Failed! Macro Validation with TransNo " + sTransNox + "is not existing!");
            writeLog("SendMacroValidation Ended: " + Calendar.getInstance().getTime());
            System.exit(1);
        }

        dPeriodFr = SQLUtil.dateFormat(rsMstr.getDate("dPeriodFr"), SQLUtil.FORMAT_SHORT_DATE);
        dPeriodTr = SQLUtil.dateFormat(rsMstr.getDate("dPeriodTo"), SQLUtil.FORMAT_SHORT_DATE);
        cDivision = rsMstr.getString("cDivision");
        sEmpTypID = rsMstr.getString("sEmpTypID");
        
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", "Guanzon Group of Companies");
        params.put("sBranchNm", "Guanzon Central Office");
        params.put("sAddressx", "Guanzon Bldg., Perez Blvd., Dagupan City");
        params.put("sReportNm", "Macro Payroll Validation - " + (sEmpTypID.equalsIgnoreCase("r") ? "Regular" : "Probationary"));
        params.put("sReportDt", "For Payroll Period: " + rsMstr.getString("sValidDsc") );
        params.put("sPrintdBy", "SYSTEM GENERATED");
        
        JSONArray xrows = new JSONArray();

        sql = "SELECT a.*, b.sDeptName" + 
             " FROM Payroll_Validation_Macro_Detail a" +
                   " LEFT JOIN Department b ON a.sDeptIDxx = b.sDeptIDxx" +
             " WHERE a.sTransNox = " + SQLUtil.toSQL(sTransNox) + 
             " ORDER BY sDeptName, dPeriodFr DESC" ;   
        ResultSet rsDetl = instance.executeQuery(sql);
        
        while(rsDetl.next()){
            JSONObject xrow = new JSONObject() ;
            System.out.println(rsMstr.getString("sDivsnDsc") + "-" + rsDetl.getString("sDeptName"));
            xrow.put("sField01", rsMstr.getString("sDivsnDsc") + "-" + rsDetl.getString("sDeptName"));
            xrow.put("sField02", SQLUtil.dateFormat(rsDetl.getDate("dPeriodFr"), "MM/dd") + " - " + SQLUtil.dateFormat(rsDetl.getDate("dPeriodTo"), "MM/dd/YYYY"));
            xrow.put("nField01", rsDetl.getInt("nEmployee"));
            xrow.put("lField01", rsDetl.getDouble("xBasicSal"));
            xrow.put("lField02", Math.round(rsDetl.getDouble("xBasicSal") / rsDetl.getInt("nEmployee") * 100.0) / 100.0);
            xrows.add(xrow);
            System.out.println(rsDetl.getDate("dPeriodFr"));
        }

       //prepare stream input for the creation of JASPER REPORT
        InputStream stream = new ByteArrayInputStream(xrows.toJSONString().getBytes("UTF-8"));
        JsonDataSource jrjson = new JsonDataSource(stream);
        //System.out.println(xrows.toJSONString());
        //create jasper report
        JasperPrint print = JasperFillManager.fillReport(path + "/reports/PayrollMacro.jasper", params, jrjson);
        String filename = "Macro-" + cDivision + "-" + sEmpTypID + "-" + dPeriodFr;

        //System.out.println(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
        //export jasper report to PDF
        JasperExportManager.exportReportToPdfFile(print, System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
        
        JSONObject param = new JSONObject();
        File filex;
        //param.put("to", "cmespanol@guanzongroup.com.ph");
        param.put("to", "masayson@guanzongroup.com.ph");
        param.put("from", "Payroll Validation Utility<petmgr@guanzongroup.com.ph>");
//        param.put("to", rsMstr.getString("sPreAudtr"));
        param.put("subject", rsMstr.getString("sDivsnDsc") + " - Macro Payroll Validation for " + (sEmpTypID.equalsIgnoreCase("r") ? "Regular" : "Probationary") + "- " + rsMstr.getString("sValidDsc"));
        param.put("body", "Please check attached file.");
        filex =  new File(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
        param.put("data1", encodeFileToBase64Binary(filex));
        param.put("filename1", filex.getName());
        
        if(!sendMail(param)){
            writeLog("Unable to send " + filename + "X.pdf");
        }

        Calendar cal = Calendar.getInstance();
        sql = "UPDATE Payroll_Validation_Macro_Master" +
             " SET cMailSent = '1'" +   
                ", dMailSent = " + SQLUtil.toSQL(cal) +                  
             " WHERE sTransNox = " + SQLUtil.toSQL(sTransNox); 
        instance.executeQuery(sql, "Payroll_Validation_Macro_Master", instance.getBranchCode(), "" );
        
        writeLog("Sent successfully " + filename + "X.pdf");
        writeLog("PayrollValidation_Macro Ended: " + Calendar.getInstance().getTime());
        
        //System.out.println(xrows.toJSONString());
        //send the 
        
    }

   private static String getTransNo(String cDivision, String sEmpTypID, String dPeriodFr, String sRqstdByx) throws SQLException{
       String sTransNox = ""; 
       String sql = "SELECT sTransNox" + 
                    " FROM Payroll_Validation_Macro_Master" + 
                    " WHERE cDivision = " + SQLUtil.toSQL(cDivision) + 
                      " AND sEmpTypID = " + SQLUtil.toSQL(sEmpTypID) + 
                      " AND dPeriodFr = " + SQLUtil.toSQL(dPeriodFr) + 
                      " AND sRqstdByx = " + SQLUtil.toSQL(sRqstdByx); 
        System.out.println(sql);
        ResultSet rs = instance.executeQuery(sql);
        
        instance.beginTrans();
        if(rs.next()){
            sTransNox = rs.getString("sTransNox");
            sql = "DELETE FROM Payroll_Validation_Macro_Master WHERE sTransNox = " + SQLUtil.toSQL(sTransNox);
            instance.executeUpdate(sql);
            sql = "DELETE FROM Payroll_Validation_Macro_Detail WHERE sTransNox = " + SQLUtil.toSQL(sTransNox);
            instance.executeUpdate(sql);
        }
        
        sql = "SELECT cDivision" +
                   ", sDivsnDsc" + 
                   ", sDeptIDxx" + 
                   ", sDeptName" + 
                   ", dPeriodFr" +
                   ", dPeriodTo" +
                   ", COUNT(*) nRecdCtrx" +
                   ", SUM(xBasicSal) xBasicSal" +
                   ", SUM(nAbsencex) nAbsencex" +
                   ", SUM(nHolidayx) nHolidayx" +
                   ", SUM(nTardines) nTardines" +
                   ", SUM(nUndrTime) nUndrTime" +
                   ", SUM(nOverTime) nOverTime" +
                   ", SUM(nBenefits) nBenefits" +
                   ", SUM(nNightDif) nNightDif" +
                   ", SUM(nOTNightD) nOTNightD" +
              " FROM" +
                " (SELECT c.cDivision" +
                       ", d.sDivsnDsc" +
                       ", e.sDeptIDxx" +
                       ", e.sDeptName" + 
                       ", a.dPeriodFr" +
                       ", a.dPeriodTo" +
                       ", @xBasicSal := IFNULL(CAST(AES_DECRYPT(UNHEX(b.nBasicSal), 'LGK_1945UE') AS DECIMAL), 0) xBasicSal" +
                       ", @nDailyRt1 := ROUND(@xBasicSal / IF(c.cPayDivCd = '3' OR a.sEmpTypID = 'R', 13, b.nAttendnc + b.nAbsencex), 2) AS nDailyRt1" +
                       ", @nAbsencex := ROUND(b.nAbsencex * @nDailyRt1, 2) nAbsencex" +
                       ", IFNULL(CAST(AES_DECRYPT(UNHEX(b.nHolidayx), 'LGK_1945UE') AS DECIMAL), 0) + IFNULL(CAST(AES_DECRYPT(UNHEX(b.nAdjHldyx), 'LGK_1945UE') AS DECIMAL), 0) nHolidayx" +
                       ", IFNULL(CAST(AES_DECRYPT(UNHEX(b.nTardines), 'LGK_1945UE') AS DECIMAL), 0) + IFNULL(CAST(AES_DECRYPT(UNHEX(b.nAdjTardi), 'LGK_1945UE') AS DECIMAL), 0) nTardines" +
                       ", IFNULL(CAST(AES_DECRYPT(UNHEX(b.nUndrTime), 'LGK_1945UE') AS DECIMAL), 0) + IFNULL(CAST(AES_DECRYPT(UNHEX(b.nAdjUndrT), 'LGK_1945UE') AS DECIMAL), 0) nUndrTime" +
                       ", IFNULL(CAST(AES_DECRYPT(UNHEX(b.nOverTime), 'LGK_1945UE') AS DECIMAL), 0) + IFNULL(CAST(AES_DECRYPT(UNHEX(b.nAdjOverT), 'LGK_1945UE') AS DECIMAL), 0) nOverTime" +
                       ", IFNULL(CAST(AES_DECRYPT(UNHEX(b.nBenefits), 'LGK_1945UE') AS DECIMAL), 0) + IFNULL(CAST(AES_DECRYPT(UNHEX(b.nAdjBenft), 'LGK_1945UE') AS DECIMAL), 0) nBenefits" +
                       ", IFNULL(CAST(AES_DECRYPT(UNHEX(b.nNightDif), 'LGK_1945UE') AS DECIMAL), 0) + IFNULL(CAST(AES_DECRYPT(UNHEX(b.nAdjNghtD), 'LGK_1945UE') AS DECIMAL), 0) nNightDif" +
                       ", IFNULL(CAST(AES_DECRYPT(UNHEX(b.nOTNightD), 'LGK_1945UE') AS DECIMAL), 0) + IFNULL(CAST(AES_DECRYPT(UNHEX(b.nAdjOTNht), 'LGK_1945UE') AS DECIMAL), 0) nOTNightD" +
                       ", b.nAdjustxx" +
                  " FROM Payroll_Period a" +
                        " LEFT JOIN Payroll_Summary_New b ON a.sPayPerID = b.sPayPerID" +
                        " LEFT JOIN Branch_Others c ON b.sBranchCd = c.sBranchCD" +
                        " LEFT JOIN Division d ON c.cDivision = d.sDivsnCde" +
                        " LEFT JOIN Department e on b.sDeptIDxx = e.sDeptIDxx" + 
                  " WHERE a.dPeriodFr BETWEEN DATE_ADD(" + SQLUtil.toSQL(dPeriodFr) + ", INTERVAL -3 MONTH) AND " + SQLUtil.toSQL(dPeriodFr) + 
                    " AND a.sEmpTypID = " + SQLUtil.toSQL(sEmpTypID) +
                    " AND d.cPreValGr = " + SQLUtil.toSQL(cDivision) + ") a" +
              " GROUP BY cDivision, sDeptName, dPeriodTo" +
              " ORDER BY dPeriodTo DESC, sDeptName";
        System.out.println(sql);
        rs = instance.executeQuery(sql);
        //System.out.println("After ");
        
        if(sTransNox.length() == 0){
            sTransNox = MiscUtil.getNextCode("Payroll_Validation_Macro_Master", "sTransNox", true, instance.getConnection(), instance.getBranchCode());
        }

        if(!rs.next()){
            System.out.println("No record");
            instance.rollbackTrans();
            return null;
        }
        
        sql = SQLUtil.dateFormat(rs.getDate("dPeriodFr"), "MMM dd") + " - " + SQLUtil.dateFormat(rs.getDate("dPeriodTo"), SQLUtil.FORMAT_MEDIUM_DATE);
        System.out.println(sql);
        
        sql = "INSERT INTO Payroll_Validation_Macro_Master" +
             " SET sTransNox = " + SQLUtil.toSQL(sTransNox) +
                ", sEmpTypID = " + SQLUtil.toSQL(sEmpTypID) +
                ", cDivision = " + SQLUtil.toSQL(cDivision) +
                ", dPeriodFr = " + SQLUtil.toSQL(rs.getDate("dPeriodFr")) +
                ", dPeriodTo = " + SQLUtil.toSQL(rs.getDate("dPeriodTo")) +
                ", sValidDsc = " + SQLUtil.toSQL(sql) +
                ", sRemarksx = ''" +
                ", cMailSent = '0'" + 
                ", cTranStat = '0'" + 
                ", sRqstdByx = " + SQLUtil.toSQL(sRqstdByx) + 
                ", dRequestd = " + SQLUtil.toSQL(Calendar.getInstance().getTime());
        System.out.println(sql);
        instance.executeUpdate(sql);
        
        do{
            sql = "INSERT INTO Payroll_Validation_Macro_Detail" +
             " SET sTransNox = " + SQLUtil.toSQL(sTransNox) +
                ", sDeptIDxx = " + SQLUtil.toSQL(rs.getString("sDeptIDxx")) +   
                ", dPeriodFr = " + SQLUtil.toSQL(rs.getDate("dPeriodFr")) +
                ", dPeriodTo = " + SQLUtil.toSQL(rs.getDate("dPeriodTo")) +
                ", nEmployee = " + SQLUtil.toSQL(rs.getInt("nRecdCtrx")) + 
                ", xBasicSal = " + SQLUtil.toSQL(rs.getDouble("xBasicSal"))  +
                ", nAbsencex = " + SQLUtil.toSQL(rs.getDouble("nAbsencex"))  +
                ", nHolidayx = " + SQLUtil.toSQL(rs.getDouble("nHolidayx"))  +
                ", nTardines = " + SQLUtil.toSQL(rs.getDouble("nTardines"))  +
                ", nUndrTime = " + SQLUtil.toSQL(rs.getDouble("nUndrTime"))  +
                ", nOverTime = " + SQLUtil.toSQL(rs.getDouble("nOverTime"))  +
                ", nBenefits = " + SQLUtil.toSQL(rs.getDouble("nBenefits"))  +
                ", nNightDif = " + SQLUtil.toSQL(rs.getDouble("nNightDif"))  +
                ", nOTNightD = " + SQLUtil.toSQL(rs.getDouble("nOTNightD"));
            System.out.println(sql);
            instance.executeUpdate(sql);
                    
        }while(rs.next());
        
        instance.commitTrans();
        return sTransNox;
        
   } 
    
    private static boolean sendMail(JSONObject param) throws IOException, ParseException{
        String sURL = "https://restgk.guanzongroup.com.ph/x-api/v1.0/mail/sendrawmail.php";        
        JSONObject oJson;
        JSONParser oParser = new JSONParser();
       
        oJson = (JSONObject)oParser.parse(new FileReader(System.getProperty("sys.default.path.config") + "/" + "access" + ".token"));
        Calendar current_date = Calendar.getInstance();
        current_date.add(Calendar.MINUTE, -25);
        Calendar date_created = Calendar.getInstance();
        date_created.setTime(SQLUtil.toDate((String) oJson.get("created") , SQLUtil.FORMAT_TIMESTAMP));

        //Check if token is still valid within the time frame
        //Request new access token if not in the current period range
        if(current_date.after(date_created)){
            String[] xargs = new String[] {(String) oJson.get("parent")};
//            System.out.println("RequestAccess.main.start");
            RequestAccess.main(xargs);
//            System.out.println("RequestAccess.main.stop");
            oJson = (JSONObject)oParser.parse(new FileReader(System.getProperty("sys.default.path.config") + "/" + "access" + ".token"));
        }

        //Set the access_key as the header of the URL Request
        JSONObject headers = new JSONObject();
        headers.put("g-access-token", (String)oJson.get("access_key"));
        //System.out.println("printing param:");
        //System.out.println(headers.toJSONString());
        //System.out.println(param.toJSONString());
        //System.out.println("param printed:");
        //System.out.println((String)oJson.get("access_key"));
        
        //System.out.println(param.toJSONString());
        //System.out.println(headers.toJSONString());
        //String response = null;
        String response = WebClient.httpPostJSon(sURL, param.toJSONString(), (HashMap<String, String>) headers);
        //System.out.println(param.toJSONString());
        //System.out.println(headers.toJSONString());
        writeLog(response);
        if(response == null){
            writeLog("HTTP Error detected: " + System.getProperty("store.error.info"));
            return false;
        }
        JSONObject result = (JSONObject) oParser.parse(response);
        
        if(!((String)result.get("result")).equalsIgnoreCase("success")) 
            return false;
        
       return true;
   } 
    
    public static String encodeFileToBase64Binary(File file) throws Exception{
        FileInputStream fileInputStreamReader = new FileInputStream(file);
        byte[] bytes = new byte[(int)file.length()];
        fileInputStreamReader.read(bytes);
        return new String(Base64.encodeBase64(bytes), "UTF-8");
    }
   
    private static GProperty loadConfig(String sprop){
         return(new GProperty(sprop));
    }    

    private static void writeLog(String value){
        try {
            FileWriter fw = new FileWriter(System.getProperty("sys.default.LogFile"), true);
            fw.write(value + "\n");
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }   
}
