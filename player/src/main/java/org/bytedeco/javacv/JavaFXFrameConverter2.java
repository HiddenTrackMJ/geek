package org.bytedeco.javacv;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.*;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Convert Frames into JavaFX images and vice versa
 *
 * @author johan
 * <p>
 * updated by Arrow
 * 2019/9/2
 */
public abstract class JavaFXFrameConverter2 extends FrameConverter<Image> {

  private Java2DFrameConverter converter = new Java2DFrameConverter();
  private WritableImage answer;
  private Boolean isFirst = true;
  private Boolean needTimestamp = false;

  public void setNeedTimestamp() {
    needTimestamp = true;
  }

  public abstract Long getTime();


  @Override
  public Frame convert(Image f) {
    throw new UnsupportedOperationException("conversion from Image to Frame not supported yet.");
  }

  @Override
  public Image convert(Frame frame) {
    int iw = frame.imageWidth;
    int ih = frame.imageHeight;
    PixelReader pr = new FramePixelReader(frame);

    if (isFirst) {
      isFirst = false;
      answer = new WritableImage(pr, iw, ih);
    } else {
      if (iw != answer.getWidth() || ih != answer.getHeight()) {
        answer = new WritableImage(pr, iw, ih);
      } else {
        BufferedImage bufferedImage = converter.convert(frame);
        if (needTimestamp) {
          Long timeStamp = getTime();
          SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:S");
          String sd = sdf.format(new Date(Long.parseLong(String.valueOf(timeStamp))));
          bufferedImage.getGraphics().drawString(sd, iw * 7 / 10, ih * 9 / 10);
        }
        SwingFXUtils.toFXImage(bufferedImage, answer);
      }
    }

    return answer;
  }

  class FramePixelReader implements PixelReader {

    Frame frame;

    FramePixelReader(Frame f) {
      this.frame = f;
    }

    @Override
    public PixelFormat getPixelFormat() {
      throw new UnsupportedOperationException("getPixelFormat not supported yet.");
    }

    @Override
    public int getArgb(int x, int y) {
      throw new UnsupportedOperationException("getArgb not supported yet.");
    }

    @Override
    public Color getColor(int x, int y) {
      throw new UnsupportedOperationException("getColor not supported yet.");
    }

    @Override
    public <T extends Buffer> void getPixels(int x, int y, int w, int h, WritablePixelFormat<T> pixelformat, T buffer, int scanlineStride) {
      int fss = frame.imageStride;
      if (frame.imageChannels != 3) {
        throw new UnsupportedOperationException("We only support frames with imageChannels = 3 (BGR)");
      }
      if (buffer instanceof ByteBuffer) {
        ByteBuffer bb = (ByteBuffer) buffer;
        ByteBuffer b = (ByteBuffer) frame.image[0];
        for (int i = y; i < y + h; i++) {
          for (int j = x; j < x + w; j++) {
            int base = 3 * j;
            bb.put(b.get(fss * i + base));
            bb.put(b.get(fss * i + base + 1));
            bb.put(b.get(fss * i + base + 2));
            bb.put((byte) 255);
          }
        }

      } else throw new UnsupportedOperationException("We only support bytebuffers at the moment");
    }

    @Override
    public void getPixels(int x, int y, int w, int h, WritablePixelFormat<ByteBuffer> pixelformat, byte[] buffer, int offset, int scanlineStride) {
      throw new UnsupportedOperationException("getPixels<ByteBuffer> Not supported yet.");
    }

    @Override
    public void getPixels(int x, int y, int w, int h, WritablePixelFormat<IntBuffer> pixelformat, int[] buffer, int offset, int scanlineStride) {
      throw new UnsupportedOperationException("getPixels<IntBuffer>Not supported yet.");
    }

  }

}

