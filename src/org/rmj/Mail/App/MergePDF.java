/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.rmj.mail.App;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.utils.PdfMerger;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author Administrator
 */
public class MergePDF {
    public static void main(String args[]) throws IOException {
        if(args.length != 3){
            System.out.println("MergePDF <source_pdf1> <source_pdf1> <output_pdf>");
            System.exit(1);
        }
        
        String source_pdf1 = args[0];
        String source_pdf2 = args[0];
        String output_pdf = args[0];

        new MergePDF().createPdf(source_pdf1, source_pdf2, output_pdf);
    }
    
    public void createPdf(String source_pdf1, String source_pdf2, String output_pdf) throws IOException {
        File file = new File(output_pdf);
        //Initialize PDF document with output intent
        PdfDocument pdf = new PdfDocument(new PdfWriter(file));
        PdfMerger merger = new PdfMerger(pdf);

        //Add pages from the first document
        PdfDocument firstSourcePdf = new PdfDocument(new PdfReader(source_pdf1));
        merger.merge(firstSourcePdf, 1, firstSourcePdf.getNumberOfPages());

        //Add pages from the second pdf document
        PdfDocument secondSourcePdf = new PdfDocument(new PdfReader(source_pdf2));
        merger.merge(secondSourcePdf, 1, secondSourcePdf.getNumberOfPages());

        firstSourcePdf.close();
        secondSourcePdf.close();
        pdf.close();
    }    
}
