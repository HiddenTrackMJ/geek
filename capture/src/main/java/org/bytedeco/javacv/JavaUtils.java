package org.bytedeco.javacv;

import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.opencv.core.CvType;

public class JavaUtils {

  private  int BLACK_VALUE = 5;

  public WritableImage removeBlackEdge(WritableImage writableImage) {

    int ih = (int)writableImage.getHeight();//初始图像的宽高
    int iw = (int)writableImage.getWidth();
    int ihLimit = ih/4;
    int iwLimit = iw/4;//去黑边边框宽度的阈值
    PixelReader reader = writableImage.getPixelReader();
    boolean contin = true;
    int iwRecord = 0 ;//左右黑边的宽度
    int ihRecord = 0 ; //上下黑边的宽度

    //先检测上边框
    for (int y = 0; y < ih ; y+=2) {
      for(int x = 0; x < iw ; x+=10){
        Color color = reader.getColor(x,y);
        double brightness = color.getBrightness();
        if(brightness>0.02) {
          contin = false;
          break;
        }
      }
      if(contin == false || y>=ihLimit ) {
        ihRecord = y;
        break;
      }
    }
    contin = true;
    //检测左边框
    for( int x  = 0; x < iw ; x+=2){
      for (int y = 0; y < ih ; y+=10){
        Color color = reader.getColor(x,y);
        double brightness = color.getBrightness();
        if(brightness>0.02) {
          contin = false;
          break;
        }
      }
      if(contin == false ||x>=iwLimit) {
        iwRecord = x;
        break;
      }
    }
    //原始图像无黑边，直接返回
    if(iwRecord==0&&ihRecord==0) return writableImage;
    else{
      //原图有黑边 裁剪之后返回
      writableImage = new WritableImage(reader,iwRecord,ihRecord,iw-iwRecord*2,ih-ihRecord*2);
      return writableImage;
    }
  }


  /**
   * 去除图片黑边，若无黑边，则原图返回。默认“全黑”阈值为 {@code BLACK_VALUE}
   *
   * @param srcMat 预去除黑边的Mat
   * @return 去除黑边之后的Mat
   */
  public  Mat removeBlackEdge(Mat srcMat) {
    return removeBlackEdge(srcMat, BLACK_VALUE);
  }

  /**
   * 去除图片黑边，若无黑边，则原图返回。
   *
   * @param blackValue 一般低于5的已经是很黑的颜色了
   * @param srcMat     源Mat对象
   * @return Mat对象
   */
  public  Mat removeBlackEdge(Mat srcMat, int blackValue) {
    Mat grayMat = srcMat;

    int topRow = 0;
    int leftCol = 0;
    int rightCol = grayMat.arrayWidth() - 1;
    int bottomRow = grayMat.arrayHeight() - 1;

    // 上方黑边判断
    for (int row = 0; row < grayMat.arrayHeight(); row++) {
      // 判断当前行是否基本“全黑”，阈值自定义；
      if (sum(grayMat.row(row)) / grayMat.arrayWidth() < blackValue) {
        // 更新截取条件
        topRow = row;
      } else {
        break;
      }
    }
    // 左边黑边判断
    for (int col = 0; col < grayMat.arrayWidth(); col++) {
      // 判断当前列是否基本“全黑”，阈值自定义；
      if (sum(grayMat.col(col)) / grayMat.arrayHeight() < blackValue) {
        // 更新截取条件
        leftCol = col;
      } else {
        break;
      }
    }
    // 右边黑边判断
    for (int col = grayMat.arrayWidth() - 1; col > 0; col--) {
      // 判断当前列是否基本“全黑”，阈值自定义；
      if (sum(grayMat.col(col)) / grayMat.arrayHeight() < blackValue) {
        // 更新截取条件
        rightCol = col;
      } else {
        break;
      }
    }
    // 下方黑边判断
    for (int row = grayMat.arrayHeight() - 1; row > 0; row--) {
      // 判断当前行是否基本“全黑”，阈值自定义；
      if (sum(grayMat.row(row)) / grayMat.arrayWidth() < blackValue) {
        // 更新截取条件
        bottomRow = row;
      } else {
        break;
      }
    }

    int x = leftCol;
    int y = topRow;
    int width = rightCol - leftCol;
    int height = bottomRow - topRow;

    if (leftCol == 0 && rightCol == grayMat.arrayWidth() - 1 && topRow == 0 && bottomRow == grayMat.arrayHeight() - 1) {
      return srcMat;
    }
    return cut(srcMat, x, y, width, height);
  }

  /**
   * @param srcMat 源mat
   * @param x
   * @param y
   * @param width
   * @param height
   * @return 裁剪后的mat
   */
  private  Mat cut(Mat srcMat, int x, int y, int width, int height) {
    //目标Mat
    Mat imgDesc = new Mat(width, height, CvType.CV_8UC3);
    //设置ROI
    Mat imgROI = new Mat(srcMat, new Rect(x, y, width, height));

    //从ROI中剪切图片
    imgROI.copyTo(imgDesc);

    return imgDesc;
  }


  /**
   * 求和
   *
   * @param mat mat
   * @return sum
   */
  private  int sum(Mat mat) {
    int sum = 0;
    for (int row = 0; row < mat.arrayHeight(); row++) {
      for (int col = 0; col < mat.arrayWidth(); col++) {
        sum += getMatElement(mat,row,col,0);
      }
    }
    return sum;
  }

  public int getMatElement(Mat img,int row,int col,int channel){
    //获取字节指针
    BytePointer bytePointer = img.ptr(row, col);
    int value = bytePointer.get(channel);
    if(value<0){
      value=value+256;
    }
    return value;
  }
}
