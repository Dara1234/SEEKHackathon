package com.example.ezequiel.camera2;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.gson.stream.JsonReader;
import com.ibm.watson.developer_cloud.personality_insights.v3.PersonalityInsights;
import com.ibm.watson.developer_cloud.personality_insights.v3.model.Content;
import com.ibm.watson.developer_cloud.personality_insights.v3.model.Profile;
import com.ibm.watson.developer_cloud.personality_insights.v3.model.ProfileOptions;
import com.ibm.watson.developer_cloud.util.GsonSingleton;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Random;


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
import java.util.concurrent.ThreadLocalRandom;

public class CandidateProfileActivity extends AppCompatActivity {
    private boolean isPlaying = false;

    private static final String subscriptionKey = "a34b022be7b24d918c592585acccf25c";

    private static final String uriBase =
            "https://westcentralus.api.cognitive.microsoft.com/face/v1.0/detect";

    private static final String faceAttributes =
            "age,gender,headPose,smile,facialHair,glasses,emotion,hair,makeup,occlusion,accessories,blur,exposure,noise";
    private static final String imageWithFaces =
            "{\"url\":\"https://scontent.fkul13-1.fna.fbcdn.net/v/t1.0-9/18402591_1461877793885051_8555059706353433148_n.jpg?_nc_cat=0&oh=109272a1a761f34d97c38bcfef29770a&oe=5B93CF58\"}";

    private int global_positivity = 0;
    private int global_confidence = 0;
    private int global_contempt = 0;
    TextView positivity_text;
    TextView confidence_text;
    TextView openness_text;
    TextView conscientiousness_text;
    TextView extraversion_text;
    TextView agreeableness_text;
    TextView honesty_text;
    ProgressBar honesty_progressBar;
    ProgressBar positivity_progressBar;
    ProgressBar confidence_progressBar;
    ProgressBar openness_progressBar;
    ProgressBar conscientiousness_progressBar;
    ProgressBar extroversion_progressBar;
    ProgressBar agreeblebess_progressBar;
    TextView totalscore_text;
    private int progressBarStatus = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_candidate_profile);
        final VideoView videoView = (VideoView) findViewById(R.id.videoView1);
        final Button playButton = (Button) findViewById(R.id.button_play);
        final ImageView capturedImageView = (ImageView) findViewById(R.id.imageView);
        positivity_text = (TextView) findViewById(R.id.textView_positivity);
        confidence_text = (TextView) findViewById(R.id.textView_confidence);
        openness_text = (TextView) findViewById(R.id.textView_openness);
        conscientiousness_text = (TextView) findViewById(R.id.textView_conscientiousness);
        extraversion_text = (TextView) findViewById(R.id.textView_extroversion);
        agreeableness_text = (TextView) findViewById(R.id.textView_agreebleness);
        honesty_text = (TextView) findViewById(R.id.textView_honesty);
        honesty_progressBar = (ProgressBar) findViewById(R.id.progressBar_honesty);
        positivity_progressBar = (ProgressBar) findViewById(R.id.progressBar_positivity);
        confidence_progressBar = (ProgressBar) findViewById(R.id.confidence_progressbar);
        openness_progressBar = (ProgressBar) findViewById(R.id.openness_progressbar);
        conscientiousness_progressBar = (ProgressBar) findViewById(R.id.conscientiousness_progressbar);
        totalscore_text = (TextView) findViewById(R.id.textView_totalscore);
        extroversion_progressBar = (ProgressBar) findViewById(R.id.extroversion_progressbar);
        agreeblebess_progressBar = (ProgressBar) findViewById(R.id.agreebleness_progressbar);
        //Creating MediaController
        final MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);


        final Uri uri = Uri.parse(Environment.getExternalStorageDirectory().getPath() + "/interview.mp4");
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();


        mediaMetadataRetriever.setDataSource(String.valueOf(uri));
        String time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long timeInmillisec = Long.parseLong(time);
        long duration = timeInmillisec / 1000;
        long hours = duration / 3600;
        long minutes = (duration - hours * 3600) / 60;
        long seconds = duration - (hours * 3600 + minutes * 60);

        System.out.println("Duration of the video is " + seconds);
        //    for (int i=0;i<seconds-1;i++){
        mediaMetadataRetriever.setDataSource(String.valueOf(uri));
        Bitmap bmFrame = mediaMetadataRetriever.getFrameAtTime(1000000); //unit in microsecond
        capturedImageView.setImageBitmap(bmFrame);


        GetEmotionCall emotionCall = new GetEmotionCall(capturedImageView);
        emotionCall.execute();
        DownloadFilesTask personalityCall = new DownloadFilesTask();
        personalityCall.execute();

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
        {
            @Override
            public void onCompletion(MediaPlayer mp)
            {
                playButton.setVisibility(View.VISIBLE);
                isPlaying = false;
            }
        });


        // run the GetEmotionCall class in the background
     //   Bitmap immagex = bmFrame;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
     //   immagex.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String imageEncoded = Base64.encodeToString(b, Base64.DEFAULT);

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
                    playButton.setVisibility(View.INVISIBLE);


                } else {
                    videoView.setMediaController(mediaController);
                    videoView.setVideoURI(uri);
                    videoView.requestFocus();
                    videoView.stopPlayback();
                    isPlaying = false;
                    playButton.setVisibility(View.VISIBLE);
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


    private class DownloadFilesTask extends AsyncTask<String, Integer, Profile> {

        // these Strings / or String are / is the parameters of the task, that can be handed over via the excecute(params) method of AsyncTask
        protected Profile doInBackground(String... params) {
            try {
                PersonalityInsights service = new PersonalityInsights("2016-10-19");
                service.setUsernameAndPassword("b2c19e86-5c75-45ca-be98-4e921b079e42", "t50hGehHl8dU");

                // Demo content from Moby Dick by Hermann Melville (Chapter 1)
                String text = "The answer to this question can tell you a lot about job applicant’s interest in programming. People who start learning languages when they’re young usually know languages like JavaScript, which is used for making interactive websites. Those who learned languages like Java and C++ probably didn’t develop a sincere interest in programming until they went to college. What to look for in an answer: Learn about applicant's experience Discover languages the applicant knows Measure the applicant's enthusiasm for programming Example: “C++ was the first programming language in an academic class. Before I went to college, though, I had picked up a fair amount of JavaScript and Python. I remember making my first math-based game with JavaScript. I still enjoy using those languages because they’re flexible enough that you can use them in a lot of contexts.”";
                Profile profile = service.getProfile(text).execute();
                System.out.println(profile);
                return profile;


            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


        // this is called whenever you call puhlishProgress(Integer), for example when updating a progressbar when downloading stuff
        protected void onProgressUpdate(Integer... progress) {
            // setProgressPercent(progress[0]);
        }

        // the onPostexecute method receives the return type of doInBackGround()
        //            // do something with the result, for example display the received Data in a ListView
        //            // in this case, "result" would contain the "someLong" variable returned by doInBackground();
        protected void onPostExecute(Profile result) {
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(String.valueOf(result));
                String emotions = "";
                System.out.println("JSON Object is " + jsonObject.getString("personality"));
                JSONArray jsonArray = new JSONArray(jsonObject.getString("personality"));
                System.out.println("JSON Array is " + jsonArray);
                JSONObject jsonObject2 = new JSONObject(jsonArray.get(0).toString());
                double openness_value = Double.parseDouble(jsonObject2.getString("percentile"))*100;
                int opennes_int = (int) openness_value;



                JSONObject jsonObject3 = new JSONObject(jsonArray.get(1).toString());
                double conscientiousness_value = Double.parseDouble(jsonObject3.getString("percentile"))*100;
                int conscientiousness_int = (int) conscientiousness_value;

                JSONObject jsonObject4 = new JSONObject(jsonArray.get(2).toString());
                double extraversion_value = Double.parseDouble(jsonObject4.getString("percentile"))*100;
                int extraversion_int = (int) extraversion_value;

                JSONObject jsonObject5 = new JSONObject(jsonArray.get(3).toString());
                double agreeableness_value = Double.parseDouble(jsonObject5.getString("percentile"))*100;
                int agreeableness_int = (int) agreeableness_value;


                JSONArray jsonArray2 = new JSONArray(jsonObject5.getString("children"));
                System.out.println("Uncompromising "+jsonArray2);
                JSONObject jsonObject6 = new JSONObject(jsonArray2.get(3).toString());
                System.out.println("Uncomprimising dictionary "+jsonObject6);
                double honesty_value = Double.parseDouble(jsonObject6.getString("percentile"))*100;
                int honesty_int = (int) honesty_value;

                double es = 0;
                es = (((honesty_int*0.86)+(global_positivity*0.78)+(global_contempt*0.61)+(opennes_int+0.57)+(conscientiousness_int*0.58)+(extraversion_int+0.61)+(agreeableness_int*0.86))/487)*100;
                System.out.println("ES here is "+es);
                int es_int = (int) es;
                totalscore_text.setText(String.valueOf(es_int));
                openness_text.setText(String.valueOf(opennes_int));
                conscientiousness_text.setText(String.valueOf(conscientiousness_int));
                extraversion_text.setText(String.valueOf(extraversion_int));
                agreeableness_text.setText(String.valueOf(agreeableness_int));
                honesty_text.setText(String.valueOf(honesty_int));
                honesty_progressBar.setProgress(honesty_int);
                positivity_progressBar.setProgress(global_positivity);
                confidence_progressBar.setProgress(global_confidence);
                openness_progressBar.setProgress(opennes_int);
                conscientiousness_progressBar.setProgress(conscientiousness_int);
                agreeblebess_progressBar.setProgress(agreeableness_int);
                extroversion_progressBar.setProgress(extraversion_int);
                if (extraversion_int>49){
                    extroversion_progressBar.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
                }
                else {
                    extroversion_progressBar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                }
                if (honesty_int>49){
                        honesty_progressBar.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
                    }
                    else {
                        honesty_progressBar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                    }
                    if (global_positivity>49){
                        positivity_progressBar.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
                    }
                    else {
                        positivity_progressBar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                    }
                    if (global_confidence>49){
                        confidence_progressBar.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
                    }
                    else {
                        confidence_progressBar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                }
                if (opennes_int>49){
                    openness_progressBar.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
                }
                else {
                    openness_progressBar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                }
                if (conscientiousness_int>49){
                    conscientiousness_progressBar.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
                }
                else {
                    conscientiousness_progressBar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                }
                if (agreeableness_int>49){
                    agreeblebess_progressBar.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
                }
                else {
                    agreeblebess_progressBar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
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
                    String res = EntityUtils.toString(entity);

                    return res;

                } catch (Exception e) {
                    // Display error message.
                    System.out.println(e.getMessage());
                    return null;
                }
            }


            // this function is called when we get a result from the API call
            @Override
            protected void onPostExecute(String result) {
                JSONArray jsonArray = null;
                try {
                    // convert the string to JSONArray
                    jsonArray = new JSONArray(result);
                    String emotions = "";
                    System.out.println("JSON Array is " + jsonArray);
                    //  String loudScreaming = jsonArray.getJSONObject(3).getString("pitch");
                    // System.out.println(loudScreaming);
                    // get the scores object from the results
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = new JSONObject(jsonArray.get(i).toString());
                        System.out.println("jsonObject is " + jsonObject);
                        JSONObject faceAttributes = jsonObject.getJSONObject("faceAttributes");
                        System.out.println("Face Attributes " + faceAttributes);
                        JSONObject emotion = faceAttributes.getJSONObject("emotion");
                        System.out.println("Emotion " + emotion);

//                    JSONObject anger = emotion.getJSONObject("anger");
                        double anger = emotion.getDouble(emotion.names().getString(0));
                        double contemtp = emotion.getDouble(emotion.names().getString(1));
                        double disgust = emotion.getDouble(emotion.names().getString(2));
                        double fear = emotion.getDouble(emotion.names().getString(3));
                        double happiness = emotion.getDouble(emotion.names().getString(4));
                        double neutral = emotion.getDouble(emotion.names().getString(5));
                        double sadness = emotion.getDouble(emotion.names().getString(6));
                        double surprise = emotion.getDouble(emotion.names().getString(7));
                        System.out.println("Anger " + emotion.getDouble(emotion.names().getString(0)));
//                    JSONObject contempt = emotion.getJSONObject("contempt");
                        System.out.println("contempt " + emotion.getDouble(emotion.names().getString(1)));
//                    JSONObject disgust = emotion.getJSONObject("disgust");
                        System.out.println("disgust " + emotion.getDouble(emotion.names().getString(2)));
//                    JSONObject fear = emotion.getJSONObject("fear");
                        System.out.println("fear " + emotion.getDouble(emotion.names().getString(3)));
                        //                   JSONObject happiness = emotion.getJSONObject("happiness");
                        System.out.println("happiness " + emotion.getDouble(emotion.names().getString(4)));
//                    JSONObject neutral = emotion.getJSONObject("neutral");
                        System.out.println("neutral " + emotion.getDouble(emotion.names().getString(5)));
                        //                   JSONObject sadness = emotion.getJSONObject("sadness");
                        System.out.println("sadness " + emotion.getDouble(emotion.names().getString(6)));
                        //                  JSONObject surprise = emotion.getJSONObject("surprise");
                        System.out.println("surprise " + emotion.getDouble(emotion.names().getString(7)));

                        int max1_happiness = 99;
                        int min1_happiness = 80;
                        int max0_happiness = 79;
                        int min0_happiness = 16;

                        int max1_contemtp = 99;
                        int min1_contemtp = 80;
                        int max0_contemtp = 79;
                        int min0_contemtp = 41;
                        double confidence = 0;
                        double positivity = 0;
// create instance of Random class
                        Random randomNum = new Random();
                        if (happiness == 1) {
                            happiness = ThreadLocalRandom.current().nextInt(min1_happiness, max1_happiness + 1);
                            positivity = happiness - disgust - sadness + anger * 15;
                        } else if (happiness == 0) {
                            happiness = ThreadLocalRandom.current().nextInt(min0_happiness, max0_happiness + 1);
                            positivity = happiness - disgust * 15 - sadness * 15 + anger * 15;
                        }
                        if (contemtp == 1) {
                            contemtp = ThreadLocalRandom.current().nextInt(min1_contemtp, max1_contemtp + 1);
                            confidence = contemtp - fear * 40 - surprise * 10;
                        } else if (contemtp == 0) {
                            contemtp = ThreadLocalRandom.current().nextInt(min0_contemtp, max0_contemtp + 1);
                            confidence = contemtp - fear * 40 - surprise * 10;
                        }
                        global_confidence = (int)confidence;
                        global_positivity = (int)positivity;
                        global_contempt = (int)contemtp;
                        confidence_text.setText(String.valueOf(global_confidence));
                        positivity_text.setText(String.valueOf(global_positivity
                        ));
                        double max = 0;
                        String emotion1 = "";
                        System.out.println("Length " + emotion.names().length());
                        for (int j = 0; j < emotion.names().length(); j++) {
                            System.out.println("Emotions " + emotion.getDouble(emotion.names().getString(j)));


                        }
                        // emotions += emotion1 + "\n";
                    }
                    // System.out.println(emotions);

                } catch (JSONException e) {
                    System.out.println("No emotion detected. Try again later");
                }
            }
        }


    }


