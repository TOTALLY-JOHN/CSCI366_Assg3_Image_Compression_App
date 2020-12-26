package com.csci366_2020.jihwanjeong.imagecompression;

public class YCrCb_Image {
    public byte[][] Y;
    public byte[][] Cr;
    public byte[][] Cb;
    public int y_width;
    public int y_height;
    public int cr_width;
    public int cr_height;
    public int cb_width;
    public int cb_height;

    public YCrCb_Image(){
        Y = new byte[0][0];
        Cr = new byte[0][0];
        Cb = new byte[0][0];
    }

    public YCrCb_Image(int height, int width){
        Y = new byte[height][width];
        Cr = new byte[height][width];
        Cb = new byte[height][width];
    }

    public YCrCb_Image(int y_height, int y_width, int cr_height, int cr_width, int cb_height, int cb_width){
        Y = new byte[y_height][y_width];
        Cr = new byte[cr_height][cr_width];
        Cb = new byte[cb_height][cb_width];
        this.y_width = y_width;
        this.y_height = y_height;
        this.cr_width = cr_width;
        this.cr_height = cr_height;
        this.cb_width = cb_width;
        this.cb_height = cb_height;
    }
}
