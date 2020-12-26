package com.csci366_2020.jihwanjeong.imagecompression;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    // DECLARE VARIABLES
    private ImageView originalImageView;
    private ImageView processedImageView;
    private ImageButton browseButton;
    private ImageButton processButton;
    private ImageButton browseCSZButton;
    private TextView originalTextView;
    private TextView processTextView;
    private TextView processedTextView;
    private TextView browseCSZTextView;
    private int processStatus = 0;
    final int PICK_IMAGE_REQUEST = 123;
    final int PICK_COMPRESSED_IMAGE_REQUEST = 456;
    Bitmap inputBM, outputBM;
    boolean imageLoaded = false;
    private String fileName = "";
    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // CONNECT ALL VARIABLES TO ELEMENTS
        originalImageView = findViewById(R.id.originalImageView);
        processedImageView = findViewById(R.id.processedImageView);
        browseButton = findViewById(R.id.browseButton);
        processButton = findViewById(R.id.processButton);
        browseCSZButton = findViewById(R.id.browseCSZButton);
        originalTextView = findViewById(R.id.originalTextView);
        processTextView = findViewById(R.id.processTextView);
        processedTextView = findViewById(R.id.processedTextView);
        browseCSZTextView = findViewById(R.id.browseCSZTextView);
    }

    public void loadImage(View view) {
        Intent i = new Intent();
        // SHOW ONLY IMAGES
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(i, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    public void loadCSZFile(View view) {
        Intent i = new Intent();
        i.setType("*/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(i, "Select File"), PICK_COMPRESSED_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // WHEN A USER CHOOSES AN ORIGINAL IMAGE
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uri = data.getData();
            InputStream inputFile;
            float fileSize = 0;
            try {
                inputFile = getContentResolver().openInputStream(uri);
                //get file size and convert to kB
                fileSize = (float)inputFile.available() / 1024;
            } catch (IOException e) {
                e.printStackTrace();
            }
            //get last part of path (file name)
            int index = uri.getLastPathSegment().lastIndexOf('/');
            fileName = uri.getLastPathSegment().substring(index+1);
            originalTextView.setText(uri.getLastPathSegment().substring(index+1) + "\nImage Original Size: " + String.format("%.02f",fileSize) + " kB");
            originalTextView.setVisibility(View.VISIBLE);

            originalImageView.setVisibility(View.VISIBLE);
            processedImageView.setVisibility(View.INVISIBLE);
            processButton.setImageResource(R.drawable.compress_icon);
            processedTextView.setText("");
            processTextView.setText("Compress");
            processTextView.setVisibility(View.VISIBLE);
            processButton.setVisibility(View.VISIBLE);

            try {
                inputBM = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                originalImageView.setImageBitmap(inputBM);
                imageLoaded = true;
                processStatus = 1;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // WHEN A USER CHOOSES A CSZ COMPRESSED FILE
        else if (requestCode == PICK_COMPRESSED_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uri = data.getData();
            InputStream inputFile;
            float fileSize = 0;
            try {
                inputFile = getContentResolver().openInputStream(uri);
                //get file size and convert to kB
                fileSize = (float)inputFile.available() / 1024;
            } catch (IOException e) {
                e.printStackTrace();
            }
            //get last part of path (file name)
            int index = uri.getLastPathSegment().lastIndexOf('/');
            fileName = uri.getLastPathSegment().substring(index+1);
            originalTextView.setText(uri.getLastPathSegment().substring(index+1) + "\nImage Compressed Size: " + String.format("%.02f",fileSize) + " kB");
            originalTextView.setVisibility(View.VISIBLE);

            processedImageView.setVisibility(View.INVISIBLE);
            processButton.setImageResource(R.drawable.decompress_icon);
            processedTextView.setText("");
            processTextView.setText("Decompress");
            processTextView.setVisibility(View.VISIBLE);
            processButton.setVisibility(View.VISIBLE);

            imageLoaded = true;
            processStatus = 2;
        }
    }

    public void imageProcess(View view){
        // NOT LOADED
        if (processStatus == 0) {
            Toast.makeText(getApplicationContext(),"Load an image or a csz file first.",Toast.LENGTH_SHORT).show();
        }
        // COMPRESSION
        else if (processStatus == 1) {
            int y_width = inputBM.getWidth();
            int y_height = inputBM.getHeight();
            int crcb_width = (int) Math.ceil(inputBM.getWidth()/2.0);
            int crcb_height = (int) Math.ceil(inputBM.getHeight()/2.0);

            Log.d("debug", "compress: " + y_width + " " + y_height + " " + crcb_width + " " + crcb_height);

            // DECLARE YCrCb ORIGINAL IMAGE and YCrCb SUB-SAMPLING IMAGE AFTER PROCESSING.
            YCrCb_Image ycrcb_original_image = new YCrCb_Image(y_height, y_width);
            YCrCb_Image ycrcb_subsampling_image = new YCrCb_Image(y_height, y_width, crcb_height, crcb_width, crcb_height, crcb_width);

            // CONVERT RGB TO YCrCb.
            ycrcb_original_image = convertToYCrCb(ycrcb_original_image);

            // PERFORM CHROMA SUB-SAMPLING 4:2:0 PROCESS.
            ycrcb_subsampling_image = chromaSubsampling(ycrcb_original_image, ycrcb_subsampling_image);

            // DISPLAY THE IMAGE
            displayProcessedImage(ycrcb_subsampling_image);

            // QUANTIZE Y TO 6 BITS, Cr & Cb TO 5 BITS EACH.
            Quantized_Image quantized_image = quantizeImage(ycrcb_subsampling_image);

            // WRITE FILE.
            fileName = fileName.substring(0, fileName.length() - 4);
            WriteToFileByte(ycrcb_subsampling_image, quantized_image, fileName, this);
        }
        // DECOMPRESSION
        else if (processStatus == 2) {
            ReadToFileByte(fileName, this);
        }
    }

    // CONVERT RGB TO YCrCb.
    public YCrCb_Image convertToYCrCb(YCrCb_Image ycrcb_original_image) {
        int pixel;
        float R, G, B;
        for (int i=0; i<inputBM.getWidth();i++)
        {
            for (int j=0;j<inputBM.getHeight();j++)
            {
                pixel = inputBM.getPixel(i, j);
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);

                ycrcb_original_image.Y[j][i] = (byte) (0.299 * R + 0.587* G + 0.114 * B);
                ycrcb_original_image.Cb[j][i] = (byte) ((-0.169 * R) + (-0.331) * G + 0.500 * B);
                ycrcb_original_image.Cr[j][i] = (byte) ((0.500 * R) + (-0.419) * G + (-0.081) * B);
            }
        }
        return ycrcb_original_image;
    }

    // PERFORM CHROMA SUB-SAMPLING 4:2:0 PROCESS.
    public YCrCb_Image chromaSubsampling(YCrCb_Image original_image, YCrCb_Image subsampled_image) {
        try {
            for (int i=0; i<inputBM.getWidth(); i++) {
                for (int j=0; j<inputBM.getHeight(); j++) {
                    subsampled_image.Y[j][i] = original_image.Y[j][i];
                }
            }

            int width = subsampled_image.y_width;
            int height = subsampled_image.y_height;

            int widthCount = -1;
            int heightCount = 0;
            for(int i = 0; i < width - 2; i += 2) {
                widthCount++;
                heightCount = 0;
                for (int j = 0; j < height - 2; j += 2, heightCount++) {
                    subsampled_image.Cb[heightCount][widthCount] =
                            (byte) Math.round((original_image.Cb[j][i] +
                                    original_image.Cb[j+1][i] +
                                    original_image.Cb[j][i+1] +
                                    original_image.Cb[j+1][i+1]) / 4);
                    subsampled_image.Cr[heightCount][widthCount] =
                            (byte) Math.round((original_image.Cr[j][i] +
                                    original_image.Cr[j+1][i] +
                                    original_image.Cr[j][i+1] +
                                    original_image.Cr[j+1][i+1]) / 4);
                }
            }
            return subsampled_image;
        }
        catch(Exception ex) {
            ex.printStackTrace();
            Toast.makeText(getApplicationContext(),"Error occurred. Please proceed again.",Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    // QUANTIZE THE SUB-SAMPLED IMAGE.
    public Quantized_Image quantizeImage(YCrCb_Image image) {
        try {
            int y_size = image.y_width * image.y_height;
            int crcb_size = image.Cb.length * image.Cb[0].length;
            byte[] y_tempArr = new byte[y_size];
            byte[] cr_tempArr = new byte[crcb_size];
            byte[] cb_tempArr = new byte[crcb_size];

            // CONVERT TWO DIMENSIONAL ARRAY INTO SINGLE ARRAY OF YCrCb AND REDUCE 8 BITS TO 6 BITS FOR Y AND 5 BITS FOR CrCb.
            for (int i = 0; i < image.y_width; i++) {
                for (int j = 0; j < image.y_height; j++) {
                    y_tempArr[(i*image.y_height)+j] = (byte) (Math.round(image.Y[j][i] / 4.0));
                }
            }
            for (int i = 0; i < image.Cb[0].length - 1; i++) {
                for (int j = 0; j < image.Cb.length - 1; j++) {
                    cr_tempArr[(i*image.Cb.length)+j] = (byte) (Math.round(image.Cr[j][i] / 8.0));
                    cb_tempArr[(i*image.Cb.length)+j] = (byte) (Math.round(image.Cb[j][i] / 8.0));
                }
            }

            // CALCULATE THE LENGTH OF 6 BITS FOR Y.
            int y_quantized_length = (int)Math.ceil((6 * y_tempArr.length)/8);

            // CALCULATE THE LENGTH OF 5 BITS FOR Cr, Cb EACH.
            int CrCb_quantized_length = (int)(Math.ceil(((5 * cr_tempArr.length) + (5 * cb_tempArr.length)) / 8));

            // DECLARE THE ARRAYS FOR QUANTIZED VALUES OF YCrCb.
            Quantized_Image quantizedArr = new Quantized_Image(y_quantized_length, CrCb_quantized_length);
            int y_quantized_index = -1;
            int CrCb_quantized_index = -1;

            // QUANTIZE Y TO 6 BITS AND STORE INTO NEW ARRAY.
            for (int i = 0; i < y_tempArr.length - 2; i++) {
                quantizedArr.Y[++y_quantized_index] = (byte) (y_tempArr[i] << 2 | y_tempArr[++i] >> 4);
                quantizedArr.Y[++y_quantized_index] = (byte) (y_tempArr[i] << 4 | y_tempArr[++i] >> 2);
                quantizedArr.Y[++y_quantized_index] = (byte) (y_tempArr[i] << 6 | y_tempArr[++i]);
            }

            // QUANTIZE CrCb TO 5 BITS EACH AND STORE INTO NEW ARRAY.
            for (int i = 0, j = 0; i < cr_tempArr.length - 3; i++, j++) {
                quantizedArr.CrCb[++CrCb_quantized_index] = (byte) (cr_tempArr[i] << 3 | cr_tempArr[++i] >> 2);
                quantizedArr.CrCb[++CrCb_quantized_index] = (byte) (cr_tempArr[i] << 6 | cr_tempArr[++i] << 1 | cr_tempArr[++i] >> 4);
                quantizedArr.CrCb[++CrCb_quantized_index] = (byte) (cr_tempArr[i] << 4 | cb_tempArr[j] >> 1);
                quantizedArr.CrCb[++CrCb_quantized_index] = (byte) (cb_tempArr[j] << 7 | cb_tempArr[++j] << 2 | cb_tempArr[++j] >> 3);
                quantizedArr.CrCb[++CrCb_quantized_index] = (byte) (cb_tempArr[j] << 5 | cb_tempArr[++j]);
            }

            return quantizedArr;
        }
        catch(Exception ex) {
            ex.printStackTrace();
            Toast.makeText(getApplicationContext(),"Error occurred. Please proceed again.",Toast.LENGTH_SHORT).show();
        }

        return null;
    }

    public void displayProcessedImage(YCrCb_Image image) {
        outputBM = Bitmap.createBitmap(inputBM.getWidth(), inputBM.getHeight(), inputBM.getConfig());
        for (int i=0; i<inputBM.getWidth(); i++)
        {
            int r=0,g=0,b=0;

            for (int j=0;j<inputBM.getHeight();j++)
            {
                float Y = image.Y[j][i];
                float Cr = image.Cr[j/2][i/2];
                float Cb = image.Cb[j/2][i/2];
                if(Y < 0){
                    Y += 256;
                }

                r = (int)(Y + 1.402 * (Cr));
                g = (int)(Y - 0.34414 * (Cb) - 0.71414 * (Cr));
                b = (int)(Y + 1.772  * (Cb));

                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                outputBM.setPixel(i,j, Color.argb(255, r, g, b));
            }
        }
        processedImageView.setImageBitmap(outputBM);
        processedImageView.setVisibility(View.VISIBLE);
    }


    public void WriteToFileByte(YCrCb_Image subsampled_image, Quantized_Image quantized_image, String filename, Context c)
    {
        try {
            FileOutputStream compressedFile = new FileOutputStream(new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/" + filename + ".csz"));
            String text = String.valueOf(subsampled_image.Y.length)+" "+String.valueOf(subsampled_image.Y[0].length)+"\n";
            compressedFile.write(text.getBytes());
            text = String.valueOf(subsampled_image.Cr.length)+" "+String.valueOf(subsampled_image.Cr[0].length)+"\n";
            compressedFile.write(text.getBytes());
            text = String.valueOf(subsampled_image.Cb.length)+" "+String.valueOf(subsampled_image.Cb[0].length)+"\n";
            compressedFile.write(text.getBytes());

            compressedFile.write(quantized_image.Y);
            compressedFile.write(quantized_image.CrCb);
            compressedFile.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
        catch(Exception ex) {
            ex.printStackTrace();
            Toast.makeText(getApplicationContext(),"Error occurred. Please proceed again.",Toast.LENGTH_SHORT).show();
        }

        long length = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/" + filename + ".csz").length();
        float size = Float.valueOf(length) / 1024;

        processedTextView.setText(String.format("Compressed File Size: %.02f Kb",size));
        processedTextView.setVisibility(View.VISIBLE);
        Log.e("DEBUG", "Compressed file size: " + size + " Kb");
    }

    public void ReadToFileByte(String filename, Context c)
    {
        try{
            FileInputStream in = new FileInputStream(new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/" + filename));
            String line = null;
            String[] line_t;

            byte[] cc = new byte[1];
            int index = 0;
            String[] tempText = new String[3];
            for (int i = 0; i < 3; i++)
            {
                tempText[i] = "";
            }

            while((in.read(cc)) != -1){
                if((char)cc[0] == '\n')
                {
                    index++;
                    if (index == 3)
                        break;
                }
                else
                {
                    tempText[index] += (char)cc[0];
                }
            }

            line_t = tempText[0].split(" ");
            Log.d("DEBUG", "ReadToFileByte: size of Y" + line_t[0] + " " + line_t[1] );
            int y_height = Integer.valueOf(line_t[0]);
            int y_width = Integer.valueOf(line_t[1]);

            line_t = tempText[1].split(" ");
            Log.d("DEBUG", "ReadToFileByte: size of Cr" + line_t[0] + " " + line_t[1] );
            int cr_height = Integer.valueOf(line_t[0]);
            int cr_width = Integer.valueOf(line_t[1]);

            line_t = tempText[2].split(" ");
            Log.d("DEBUG", "ReadToFileByte: size of Cb" + line_t[0] + " " + line_t[1] );
            int cb_height = Integer.valueOf(line_t[0]);
            int cb_width = Integer.valueOf(line_t[1]);

            byte[] y_tempArr = new byte[y_width * y_height];
            byte[] cr_tempArr = new byte[cr_width * cr_height];
            byte[] cb_tempArr = new byte[cb_width * cb_height];

            // CALCULATE THE LENGTH OF 6 BITS FOR Y.
            int y_quantized_length = (int)Math.ceil((6 * y_tempArr.length)/8);

            // CALCULATE THE LENGTH OF 5 BITS FOR Cr, Cb EACH.
            int CrCb_quantized_length = (int)(Math.ceil(((5 * cr_tempArr.length) + (5 * cb_tempArr.length)) / 8));

            Quantized_Image quantized_image = new Quantized_Image(y_quantized_length, CrCb_quantized_length);

            in.read(quantized_image.Y);

            int tempYIndex = -1;
            int tempCrIndex = -1;
            int tempCbIndex = -1;

            // DE-QUANTIZE Y TO 8 BITS.
            for (int i = 0; i < quantized_image.Y.length - 1; i++) {
                y_tempArr[++tempYIndex] = (byte) ((quantized_image.Y[i] >> 2) * 4);
                y_tempArr[++tempYIndex] = (byte) (((quantized_image.Y[i] << 6 | quantized_image.Y[++i] >> 2) >> 2) * 4);
                y_tempArr[++tempYIndex] = (byte) (((quantized_image.Y[i] << 4 | quantized_image.Y[++i] >> 4) >> 2) * 4);
                y_tempArr[++tempYIndex] = (byte) (((quantized_image.Y[i] << 2) >> 2) * 4);
            }

            in.read(quantized_image.CrCb);

            // DE-QUANTIZE Cr, Cb TO 8 BITS EACH.
            for (int i = 0; i < quantized_image.CrCb.length - 3; i++) {
                cr_tempArr[++tempCrIndex] = (byte) ((quantized_image.CrCb[i] >> 3) * 8);
                cr_tempArr[++tempCrIndex] = (byte) (((quantized_image.CrCb[i] << 5 | quantized_image.CrCb[++i] >> 3) >> 3) * 8);
                cr_tempArr[++tempCrIndex] = (byte) (((quantized_image.CrCb[i] << 2) >> 3) * 8);
                cr_tempArr[++tempCrIndex] = (byte) (((quantized_image.CrCb[i] << 7 | quantized_image.CrCb[++i] >> 1) >> 3) * 8);

                cb_tempArr[++tempCbIndex] = (byte) (((quantized_image.CrCb[i] << 4 | quantized_image.CrCb[++i] >> 4) >> 3) * 8);
                cb_tempArr[++tempCbIndex] = (byte) (((quantized_image.CrCb[i] << 1) >> 3) * 8);
                cb_tempArr[++tempCbIndex] = (byte) (((quantized_image.CrCb[i] << 6 | quantized_image.CrCb[++i] >> 2) >> 3) * 8);
                cb_tempArr[++tempCbIndex] = (byte) (((quantized_image.CrCb[i] << 3) >> 3) * 8);
            }

            // UP-SAMPLE THE CHROMINANCE VALUES.
            YCrCb_Image upsampled_image = new YCrCb_Image(y_height, y_width);

            for (int i = 0; i < y_width; i++) {
                for (int j = 0; j < y_height; j++) {
                    upsampled_image.Y[j][i] = (byte) (y_tempArr[(i*y_height)+j]);
                }
            }
            byte cr_temp = 0, cb_temp = 0;
            byte[][] crTempArr = new byte[cr_height][cr_width];
            byte[][] cbTempArr = new byte[cr_height][cr_width];

            for (int i = 0; i < cb_width; i++) {
                for (int j = 0; j < cb_height; j++) {
                    crTempArr[j][i] = cr_tempArr[i*cb_height+j];
                    cbTempArr[j][i] = cb_tempArr[i*cb_height+j];
                }
            }

            int widthCount = -1;
            int heightCount;
            for(int i = 0; i < y_width - 2; i += 2) {
                widthCount++;
                heightCount = 0;
                for (int j = 0; j < y_height - 2; j += 2, heightCount++) {
                    upsampled_image.Cr[j][i] = crTempArr[heightCount][widthCount];
                    upsampled_image.Cr[j+1][i] = crTempArr[heightCount][widthCount];
                    upsampled_image.Cr[j][i+1] = crTempArr[heightCount][widthCount];
                    upsampled_image.Cr[j+1][i+1] = crTempArr[heightCount][widthCount];
                    upsampled_image.Cb[j][i] = cbTempArr[heightCount][widthCount];
                    upsampled_image.Cb[j+1][i] = cbTempArr[heightCount][widthCount];
                    upsampled_image.Cb[j][i+1] = cbTempArr[heightCount][widthCount];
                    upsampled_image.Cb[j+1][i+1] = cbTempArr[heightCount][widthCount];
                }
            }

            // CONVERT YCrCb TO RGB back.
            outputBM = Bitmap.createBitmap(y_width, y_height, Bitmap.Config.ARGB_4444);
            int pixel;
            int R1 = 0, G1 = 0, B1 = 0;
            int R2 = 0, G2 = 0, B2 = 0;
            int R_diff, G_diff, B_diff;
            long R_diff_total = 0, G_diff_total = 0, B_diff_total = 0;

            for (int i=0; i<y_width; i++)
            {
                for (int j=0;j<y_height;j++)
                {
                    float Y = upsampled_image.Y[j][i];
                    float Cr = upsampled_image.Cr[j][i] * 2.25f;
                    float Cb = upsampled_image.Cb[j][i] * 2.25f;

                    if(Y < 0) Y += 195;

                    R1 = (int)(Y + 1.402 * (Cr));
                    G1 = (int)(Y - 0.34414 * (Cb) - 0.71414 * (Cr));
                    B1 = (int)(Y + 1.772  * (Cb));

                    if(R1 > 255) { R1 = 255; }
                    else if(R1 < 0) { R1 = 0; }

                    if(G1 > 255) { G1 = 255; }
                    else if(G1 < 0) { G1 = 0; }

                    if(B1 > 255) { B1 = 255; }
                    else if(B1 < 0) { B1 = 0; }

                    // CALCULATE MSE (MEAN SQUARE ERROR)
                    pixel = inputBM.getPixel(i, j);
                    R2 = Color.red(pixel);
                    G2 = Color.green(pixel);
                    B2 = Color.blue(pixel);

                    R_diff = (R1 - R2) * (R1 - R2);
                    G_diff = (G1 - G2) * (G1 - G2);
                    B_diff = (B1 - B2) * (B1 - B2);
                    R_diff_total += R_diff;
                    G_diff_total += G_diff;
                    B_diff_total += B_diff;

                    outputBM.setPixel(i, j, Color.argb(255, R1, G1, B1));

                }
            }
            // OBTAIN MEAN SQUARE.
            double R_MSE = R_diff_total / (inputBM.getWidth() * inputBM.getHeight());
            double G_MSE = G_diff_total / (inputBM.getWidth() * inputBM.getHeight());
            double B_MSE = B_diff_total / (inputBM.getWidth() * inputBM.getHeight());
            double RGB_MSE = Math.round(R_MSE + G_MSE + B_MSE / 3);

            processedTextView.setText("Mean Square Error (MSE): " + RGB_MSE);
            processedTextView.setVisibility(View.VISIBLE);
            processedImageView.setImageBitmap(outputBM);
            processedImageView.setVisibility(View.VISIBLE);
        }

        catch(IOException e){
            Toast.makeText(getApplicationContext(),"Error occurred. Please proceed again.",Toast.LENGTH_SHORT).show();
            Log.e("Exception", "File write failed: " + e.toString());
        }
        catch(Exception ex) {
            ex.printStackTrace();
            Toast.makeText(getApplicationContext(),"Error occurred. You should load the original image.",Toast.LENGTH_SHORT).show();
        }
    }

}