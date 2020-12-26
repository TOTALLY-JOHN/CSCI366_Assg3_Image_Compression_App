package com.csci366_2020.jihwanjeong.imagecompression;

public class Quantized_Image {
    public byte[] Y;
    public byte[] CrCb;

    public Quantized_Image() {
        Y = new byte[0];
        CrCb = new byte[0];
    }

    public Quantized_Image(int y, int crcb) {
        Y = new byte[y];
        CrCb = new byte[crcb];
    }
}