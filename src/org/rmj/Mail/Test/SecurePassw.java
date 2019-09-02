/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.Mail.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.rmj.appdriver.GCrypt;

/**
 *
 * @author kalyptus
 */
public class SecurePassw {
   public static void main(String[] args) {
      String SIGNATURE = "08220326";
      GCrypt loEnc = new GCrypt(SIGNATURE);
    
      String sender;
//         sender = "Taig2ITatua";
      if(args.length == 0)
         sender = "48269661945";
      else
         sender = args[0];
      
      Properties props = new Properties();
      props.put("mail.user.id", "lgk_guanzon@yahoo.com");
      props.put("mail.user.auth", loEnc.decrypt(sender));
   
      try {
          props.store(new FileOutputStream("D:/GGC_Java_Systems/config/Password.properties"), null);
      } catch (IOException ex) {
          ex.printStackTrace();
      }
   }   
}
