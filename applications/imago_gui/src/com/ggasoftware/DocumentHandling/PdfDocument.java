/****************************************************************************
 * Copyright (C) 2009-2013 GGA Software Services LLC
 *
 * This file is part of Imago OCR project.
 *
 * This file may be distributed and/or modified under the terms of the
 * GNU General Public License version 3 as published by the Free Software
 * Foundation and appearing in the file LICENSE.GPL included in the
 * packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
 * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 ***************************************************************************/

package com.ggasoftware.DocumentHandling;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class PdfDocument implements Document {

    public class PdfDocumentPageAsImage implements DocumentPageAsImage,
            ImageObserver {

        private PDFPage page;
        private BufferedImage image;
        private ImageObserver observer;
        private double scale;
        
        public PdfDocumentPageAsImage(PDFPage pg, ImageObserver obsrvr) {
            page = pg;
            observer = obsrvr;
            
            try {
                page.waitForFinish();
            } catch(InterruptedException e) {
                System.out.println(e.getMessage());
            }

        }

        public void setScale(double scl) {
            if (scl == scale)
                return;
            
            scale = scl;
            int h = (int)(page.getHeight() * scale);
            int w = (int)(page.getWidth() * scale);

            loadingFinished = false;
            
            Image img = page.getImage(w, h, null, this);
            
            while(!loadingFinished) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {}
            }

            image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            image.getGraphics().drawImage(img, 0, 0, observer);
        }

        private boolean loadingFinished;

        public boolean imageUpdate(Image img, int infoflags, int x, int y,
                int width, int height) {
            if ((infoflags & ALLBITS) != 0) {
                loadingFinished = true;
                return false;
            }
            return true;
        }

        public Dimension getSize() {
            return new Dimension(image.getWidth(), image.getHeight());
        }

        public Dimension getUnscaledSize() {
            return new Dimension((int)page.getWidth(), (int)page.getHeight());
        }

        public void paint(Graphics g) {
            if (image != null) {
                g.drawImage(image, 0, 0, null);
                g.setColor(Color.black);
                g.drawRect(-1, -1, image.getWidth(), image.getHeight());
            }
        }

        public BufferedImage getSelectedRectangle(Rectangle rect,
                ImageObserver observer) {
            double cx, cy, cw, ch;
            cx = rect.x / scale;
            cy = rect.y / scale;
            cw = rect.width / scale;
            ch = rect.height / scale;
            
            cy = page.getHeight() - cy - ch;
            Rectangle2D.Double clip = new Rectangle2D.Double(cx, cy, cw, ch);

            int w, h;
            
            if (cw > ch)
            {
                w = 1024;
                h = (int)(ch * w / cw);
            }
            else
            {
                h = 768;
                w = (int)(cw * h / ch);
            }
            
            loadingFinished = false;
            
            Image img = page.getImage(w, h, clip, this);
            
            while(!loadingFinished) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {}
            }

            BufferedImage image2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            image2.getGraphics().drawImage(img, 0, 0, observer);
            
            return image2;
            
        }
    }

    private PDFFile pdfFile;

    public PdfDocument(File file) throws FileNotFoundException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel channel = raf.getChannel();

        try {
            ByteBuffer buf =
                channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            pdfFile = new PDFFile(buf);
        } catch(IOException e) {
            System.out.println(e.getMessage() + "\nasdasd");
        }

    }

    public int getPageCount() {
        return pdfFile.getNumPages();
    }

    public DocumentPageAsImage getPage(int page, ImageObserver observer) {
        return new PdfDocumentPageAsImage(pdfFile.getPage(page), observer);
    }
}
