package com.example.abdul.emotion;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    String likelihoods;
    private Button capture;
    private final int PICK_IMAGE = 1, CAP_IMAGE=4;
    private ProgressDialog detectionProgressDialog;
      @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button1 = (Button)findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
                gallIntent.setType("image/*");
                startActivityForResult(Intent.createChooser(gallIntent, "Select Picture"), PICK_IMAGE);
            }
        });
        capture = findViewById(R.id.button);
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), CAP_IMAGE);
            }
        });

        detectionProgressDialog = new ProgressDialog(this);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final ImageView imageView = (ImageView) findViewById(R.id.imageView1);
        Bitmap bitmap = null;
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri auri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), auri);
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(requestCode == CAP_IMAGE && data != null) {
            bitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(bitmap);
        }
            try {
                bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                Vision.Builder visionBuilder = new Vision.Builder(new NetHttpTransport(),new AndroidJsonFactory(),null);
                visionBuilder.setVisionRequestInitializer(new VisionRequestInitializer("AIzaSyBL8_UGBRiNVJE8Bvq-cu2LLAS2JxCXs3k"));
                final Vision vision = visionBuilder.build();

                AsyncTask<Integer, String, String> detectTask =
                        new AsyncTask<Integer, String, String>() {
                            @Override
                            protected String doInBackground(Integer... params) {
                                try {
                                    //InputStream inputStream = getResources().openRawResource();
                                    byte[] photoData = baos.toByteArray();
                                    //inputStream.close();

                                    Image inputImage = new Image();
                                    inputImage.encodeContent(photoData);

                                    Feature desiredFeature = new Feature();
                                    desiredFeature.setType("FACE_DETECTION");

                                    publishProgress("Detecting...");

                                    AnnotateImageRequest request = new AnnotateImageRequest();
                                    request.setImage(inputImage);
                                    request.setFeatures(Arrays.asList(desiredFeature));

                                    BatchAnnotateImagesRequest batchRequest = new BatchAnnotateImagesRequest();
                                    batchRequest.setRequests(Arrays.asList(request));

                                    BatchAnnotateImagesResponse batchResponse = vision.images().annotate(batchRequest).execute();

                                    List<FaceAnnotation> faces = batchResponse.getResponses().get(0).getFaceAnnotations();

                                    int numberOfFaces = faces.size();
                                    likelihoods = "";
                                    for(int i=0;i<numberOfFaces;i++){
                                        likelihoods += "\n It is " + faces.get(i).getJoyLikelihood() + " that face " + i + " is happy";
                                    }

                                    final String message = "This photo has " + numberOfFaces + " faces" + likelihoods;
                                    return message;

                                } catch (Exception e) {
                                    publishProgress("Detection failed");
                                    return "Detection failed";
                                }
                            }
                            @Override
                            protected void onPreExecute() {
                                detectionProgressDialog.show();                    }
                            @Override
                            protected void onProgressUpdate(String... progress) {
                                detectionProgressDialog.setMessage(progress[0]);                    }
                            @Override
                            protected void onPostExecute(final String message) {
                                detectionProgressDialog.dismiss();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show();
                                        System.out.println(message);
                                    }
                                });
                            }
                        };
                detectTask.execute(1);
            }
            catch (Exception e)
            {
                System.out.println(e.getMessage());
            }
        }
    }
