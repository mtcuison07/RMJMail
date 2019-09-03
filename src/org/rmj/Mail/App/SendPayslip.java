/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.Mail.App;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import javax.mail.MessagingException;
import org.rmj.appdriver.SQLUtil;
import org.rmj.lib.mail.SendMail;
import org.rmj.lib.net.LogWrapper;
import org.rmj.appdriver.agent.GRiderX;
import org.rmj.lib.mail.MessageInfo;
import org.rmj.appdriver.agent.MsgBox;
import javax.net.ssl.SSLSocketFactory;

/**
 *
 * @author kalyptus
 */
public class SendPayslip {
   private static final String jar_path = new File(FTP_Download.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getPath().replace("%20", " "); 
   private static GRiderX instance = null;
   private static SendMail pomail;
   //private static final LogWrapper logwrapr = new LogWrapper("RMJMail.SendPaySlip", jar_path + "/temp/SendPaySlip.log");
   private static final LogWrapper logwrapr = new LogWrapper("RMJMail.SendPaySlip", "D:/GGC_Java_Systems/temp/SendPaySlip.log");

   public static void main(String[] args) {
      String sender;

        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Java_Systems";
        }
        else{
            path = "/srv/GGC_Java_Systems";
        }
        System.setProperty("sys.default.path.config", path);

      
      instance = new GRiderX("gRider");
      
      ResultSet rsToSend;
   
      //set guanzon SMTP configuration as default
      if(args.length == 0)
         sender = "GMail";
      else
         sender = args[0];

      //load the configuration
      pomail = new SendMail("D:/GGC_Java_Systems", sender);
         
      //try to connect to the SMTP server
      if(pomail.connect(true)){
      
         //extract records to send
         rsToSend = extract2send(sender);
         
         //try to send email
         try {
            while(rsToSend.next()){
               try {
                  
                  //byte email[] = rsToSend.getString("sEmailAdd").getBytes("UTF-8");
                  
                  //System.out.println("utf8 " + java.net.IDN.toASCII(new String(encode(email))));
                  //System.out.println("utf8 " + java.net.IDN.toUnicode(new String(encode(email))));
                  //System.out.println("latin1 " + new String(email, "windows-1252"));
                  //System.out.println("latin1 " + new String(email, "ISO-8859-1"));
                  
                  MessageInfo msginfo = new MessageInfo();
                  
                  //msginfo.addTo(java.net.IDN.toUnicode(new String(encode(email))));
                  msginfo.addTo(rsToSend.getString("sEmailAdd"));
                  msginfo.setSubject("PAYSLIP (" + rsToSend.getString("dPeriodFr") + " - " + rsToSend.getString("dPeriodTo") + ")");
                  msginfo.setBody(rsToSend.getString("sEmployNm"));
                  msginfo.addAttachment("C:/GGC_Systems/Temp/payslip/" + rsToSend.getString("sEmployID") + rsToSend.getString("sPayPerID") + ".pdf");
                  //kalyptus - 2017.05.19 01:44pm
                  //set the no reply account as the sender instead of the petmgr
                  msginfo.setFrom("No Reply <no-reply@guanzongroup.com.ph>");
                  pomail.sendMessage(msginfo);
                  
//                  pomail.initmsg();
//                  // pomail.setFrom("NO REPLY <petmgr@guanzongroup.com.ph>");
//                  //pomail.setRecipients(Message.RecipientType.CC, "stargaze_75@yahoo.com");
//                  pomail.setRecipients(Message.RecipientType.TO, rsToSend.getString("sEmailAdd"));               
//                  pomail.setSubject("PAYSLIP (" + rsToSend.getString("dPeriodFr") + " - " + rsToSend.getString("dPeriodTo") + ")");
//                  pomail.setBody(rsToSend.getString("sEmployNm"));
//                  pomail.addAttachment("C:/GGC_Systems/Temp/" + rsToSend.getString("sEmployID") + rsToSend.getString("sPayPerID") + ".pdf");
//                  pomail.sendMessage();

                  String lsSQL;
                  lsSQL = "UPDATE Payroll_Summary_New" +
                         " SET cMailSent = '2'" +
                         " WHERE sPayPerID = " + SQLUtil.toSQL(rsToSend.getString("sPayPerID")) +
                           " AND sEmployID = " + SQLUtil.toSQL(rsToSend.getString("sEmployID"));
                  instance.getConnection().createStatement().executeUpdate(lsSQL);
                  System.out.println(Calendar.getInstance().getTime());
               }   
               catch (MessagingException ex) {
                  logwrapr.severe("main: MessagingException error detected.", ex);
               } catch (IOException ex) {
                  logwrapr.severe("main: IOException error detected.", ex);         
               }
            }
         } catch (SQLException ex) {
            logwrapr.severe("main: SQLException error detected.", ex);
         } 
      }
      else{
          System.out.println("not connected");
      }
   }
   
   private static ResultSet extract2send(String sender){
      ResultSet rs = null;
      try {
          
         String lsSQL = "SELECT a.sBranchCD, a.sPayPerID, a.sEmployID, b.sEmailAdd, c.dPeriodFr, c.dPeriodTo, CONCAT(b.sLastName, ', ', b.sFrstname) sEmployNm" +
                       " FROM Payroll_Summary a" +
                            " LEFT JOIN Client_Master b ON a.sEmployID = b.sClientID" +
                            " LEFT JOIN Payroll_Period c ON a.sPayPerID = c.sPayPerID" +
                       " WHERE cMailSent = '1'" +
                       " UNION " +
                       " SELECT a.sBranchCD, a.sPayPerID, a.sEmployID, b.sEmailAdd, c.dPeriodFr, c.dPeriodTo, CONCAT(b.sLastName, ', ', b.sFrstname) sEmployNm" +
                       " FROM Payroll_Summary_New a" +
                            " LEFT JOIN Client_Master b ON a.sEmployID = b.sClientID" +
                            " LEFT JOIN Payroll_Period c ON a.sPayPerID = c.sPayPerID" +
                       " WHERE cMailSent = '1'" +                  
                       " ORDER BY sBranchCD DESC, sEmployNm" + 
                       (sender.compareToIgnoreCase("ymail") == 0 ? " LIMIT 150" : " LIMIT 350");

//                            " AND b.sEmailAdd" +  (sender.compareToIgnoreCase("ymail") == 0 ? " " : " NOT ") + "LIKE '%gmail.com%'" + 

         //create statement to be use in executing queries
         System.out.println(lsSQL);
         rs = instance.getConnection().createStatement().executeQuery(lsSQL);
      } 
      catch (SQLException ex) {
       logwrapr.severe("extract2send: SQLException error detected.", ex);
      }

      return rs;
   }    
   
   private static byte[] encode(byte[] arr){
        Charset utf8charset = Charset.forName("UTF-8");
        Charset iso88591charset = Charset.forName("ISO-8859-15");

        ByteBuffer inputBuffer = ByteBuffer.wrap(arr);

        // decode UTF-8
        CharBuffer data = utf8charset.decode(inputBuffer);

        // encode ISO-8559-1
        ByteBuffer outputBuffer = iso88591charset.encode(data);
        byte[] outputData = outputBuffer.array();

        return outputData;
    }
}
