package com.example.ezequiel.camera2;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import cz.msebera.android.httpclient.entity.ByteArrayEntity;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.util.EntityUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.URI;

public class CandidateProfileActivity extends AppCompatActivity {
    private boolean isPlaying = false;

    private static final String subscriptionKey = "a34b022be7b24d918c592585acccf25c";

    private static final String uriBase =
            "https://westcentralus.api.cognitive.microsoft.com/face/v1.0/detect";

    private static final String faceAttributes =
            "age,gender,headPose,smile,facialHair,glasses,emotion,hair,makeup,occlusion,accessories,blur,exposure,noise";
    private static final String imageWithFaces =
            "{\"url\":\"https://upload.wikimedia.org/wikipedia/commons/c/c3/RH_Louise_Lillian_Gish.jpg\"}";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_candidate_profile);
        final VideoView videoView =(VideoView)findViewById(R.id.videoView1);
        Button playButton = (Button)findViewById(R.id.button_play);
        final ImageView capturedImageView = (ImageView)findViewById(R.id.imageView);
        //Creating MediaController
        final MediaController mediaController= new MediaController(this);
        mediaController.setAnchorView(videoView);

        //specify the location of media file
        final Uri uri=Uri.parse(Environment.getExternalStorageDirectory().getPath()+"/interview.mp4");
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();


        mediaMetadataRetriever.setDataSource(String.valueOf(uri));
        String time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long timeInmillisec = Long.parseLong( time );
        long duration = timeInmillisec / 1000;
        long hours = duration / 3600;
        long minutes = (duration - hours * 3600) / 60;
        long seconds = duration - (hours * 3600 + minutes * 60);

        System.out.println("Duration of the video is "+ seconds);
    //    for (int i=0;i<seconds-1;i++){
            mediaMetadataRetriever.setDataSource(String.valueOf(uri));
            Bitmap bmFrame = mediaMetadataRetriever.getFrameAtTime(1000000); //unit in microsecond
            capturedImageView.setImageBitmap(bmFrame);
            // Get the data from an ImageView as bytes

        //    GetEmotionCall emotionCall = new GetEmotionCall(capturedImageView);
         //   emotionCall.execute();

     //   }

            // run the GetEmotionCall class in the background
        Bitmap immagex=bmFrame;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        immagex.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String imageEncoded = Base64.encodeToString(b,Base64.DEFAULT);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Setting MediaController and URI, then starting the videoView
                if (isPlaying == false) {
                    videoView.setMediaController(mediaController);
                    videoView.setVideoURI(uri);
                    videoView.requestFocus();
                    videoView.start();
                    isPlaying = true;
                    GetEmotionCall emotionCall = new GetEmotionCall(capturedImageView);
                    emotionCall.execute();

                }
                else {
                    videoView.setMediaController(mediaController);
                    videoView.setVideoURI(uri);
                    videoView.requestFocus();
                    videoView.stopPlayback();
                    isPlaying = false;
                }
            }
        });
    }

    // convert image to base 64 so that we can send the image to Emotion API
    public byte[] toBase64(ImageView imgPreview) {
        Bitmap bm = ((BitmapDrawable) imgPreview.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
        return baos.toByteArray();
    }

    // asynchronous class which makes the api call in the background
    private class GetEmotionCall extends AsyncTask<Void, Void, String> {

        private final ImageView img;

        GetEmotionCall(ImageView img) {
            this.img = img;
        }

        // this function is called before the api call is made
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            System.out.println("Getting results...");
        }

        // this function is called when the api call is made
        @Override
        protected String doInBackground(Void... params) {
            HttpClient httpclient = new DefaultHttpClient();

            try {
                URIBuilder builder = new URIBuilder(uriBase);

                // Request parameters. All of them are optional.
                builder.setParameter("returnFaceId", "true");
                builder.setParameter("returnFaceLandmarks", "false");
                builder.setParameter("returnFaceAttributes", faceAttributes);

                // Prepare the URI for the REST API call.
                URI uri = builder.build();
                HttpPost request = new HttpPost(uri);

                // Request headers.
                request.setHeader("Content-Type", "application/json");
                request.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);

                // Request body.
           //     request.setEntity(new ByteArrayEntity(toBase64(img)));
                StringEntity reqEntity = new StringEntity(imageWithFaces);
                request.setEntity(reqEntity);
                // Execute the REST API call and get the response entity.
                HttpResponse response = httpclient.execute(request);
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    // Format and display the JSON response.
                    System.out.println("REST Response:\n");

                    String jsonString = EntityUtils.toString(entity).trim();
                    if (jsonString.charAt(0) == '[') {
                        JSONArray jsonArray = new JSONArray(jsonString);
                        System.out.println(jsonArray.toString(2));
                    } else if (jsonString.charAt(0) == '{') {
                        JSONObject jsonObject = new JSONObject(jsonString);
                        System.out.println(jsonObject.toString(2));
                    } else {
                        System.out.println(jsonString);

                    }
                    return jsonString;
                }

            } catch (Exception e) {
                // Display error message.
                System.out.println(e.getMessage());
            }


            return null;
        }




//            HttpClient httpclient = HttpClients.createDefault();
//            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//            StrictMode.setThreadPolicy(policy);
//
//            try {
//                URIBuilder builder = new URIBuilder("https://westcentralus.api.cognitive.microsoft.com/face/v1.0/recognize");
//
//                URI uri = builder.build();
//                HttpPost request = new HttpPost(uri);
//                request.setHeader("Content-Type", "application/json");
//                // enter you subscription key here
//                request.setHeader("Ocp-Apim-Subscription-Key", "a34b022be7b24d918c592585acccf25c");
//
//                // Request body.The parameter of setEntity converts the image to base64
//                request.setEntity(new ByteArrayEntity(toBase64(img)));
//
//                // getting a response and assigning it to the string res
//                HttpResponse response = httpclient.execute(request);
//                HttpEntity entity = response.getEntity();
//                String res = EntityUtils.toString(entity);
//                System.out.println(res);
//                return res;
//
//            }
//            catch (Exception e){
//                return "null";
//            }



        // this function is called when we get a result from the API call
        @Override
        protected void onPostExecute(String result) {
            JSONArray jsonArray = null;
            try {
                // convert the string to JSONArray
                jsonArray = new JSONArray(result);
                String emotions = "";
                // get the scores object from the results
                for(int i = 0;i<jsonArray.length();i++) {
                    JSONObject jsonObject = new JSONObject(jsonArray.get(i).toString());
                    JSONObject scores = jsonObject.getJSONObject("scores");
                    double max = 0;
                    String emotion = "";
                    for (int j = 0; j < scores.names().length(); j++) {
                        if (scores.getDouble(scores.names().getString(j)) > max) {
                            max = scores.getDouble(scores.names().getString(j));
                            emotion = scores.names().getString(j);
                        }
                    }
                    emotions += emotion + "\n";
                }
                System.out.println(emotions);

            } catch (JSONException e) {
                System.out.println("No emotion detected. Try again later");
            }
        }
    }

}
