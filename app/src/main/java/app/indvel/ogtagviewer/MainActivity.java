package app.indvel.ogtagviewer;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog mDialog;
    private ConnectivityManager cManager;
    EditText editUrl;
    ImageView image;
    TextView textTitle, textDesc, textUrl;
    String strUrl;
    String imageUrl;
    String title, desc, url;
    Bitmap bitmap;
    List<String> tags = new ArrayList();
    OGTask asyncTask;

    @Override
    protected void onStop() { //멈추었을때 다이어로그를 제거해주는 메서드
        super.onStop();
        if (mDialog != null)
            mDialog.dismiss(); //다이어로그가 켜져있을경우 (!null) 종료시켜준다
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editUrl = (EditText) findViewById(R.id.editText);
        image = (ImageView) findViewById(R.id.imageView);
        textTitle = (TextView) findViewById(R.id.textTitle);
        textDesc = (TextView) findViewById(R.id.textDesc);
        textUrl = (TextView) findViewById(R.id.textUrl);

        editUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(!editable.toString().startsWith("http://") || editable.toString().startsWith("https://")) {
                    editUrl.setText("http://");
                    editUrl.setSelection(editUrl.getText().length());
                }
            }
        });
    }

    public void onButtonClick(View v) {
        strUrl = editUrl.getText().toString();

        if(isInternetCon()) {
            asyncTask = new OGTask();
            asyncTask.execute();
        } else {
            makeDialog("인터넷 연결 안됨","인터넷에 연결되어 있지 않습니다.");
        }
    }

    public void makeDialog(String title, String content) {

        final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);

        alert.setTitle(title);
        alert.setMessage(content);
        alert.setCancelable(false);
        alert.setPositiveButton("확인",null);

        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            public void run() {
                alert.show();
            }
        });
    }

    public static int dpToPx(int dp, Context context) {
        float density = context.getResources()
                .getDisplayMetrics()
                .density;
        return Math.round((float) dp * density);
    }

    public class OGTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected void onPreExecute() {

            mDialog = new ProgressDialog(MainActivity.this);
            mDialog.setMessage("잠시만 기다려주세요..");
            mDialog.setCancelable(false);
            mDialog.show();

            imageUrl = "";
            bitmap = null;
            title = "";
            desc = "";
            url = "";

            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(String...params) {

            try {

                Document document = Jsoup.connect(strUrl)
                        .timeout(3000)
                        .get();

                Elements ogTags = document.select("meta[property^=og:]");

                for(Element e : ogTags) {

                    if(e.attr("property").equals("og:image")) {
                        imageUrl = e.attr("content");
                    } else if(e.attr("property").equals("og:title")) {
                        title = e.attr("content");
                    } else if(e.attr("property").equals("og:description")) {
                        desc = e.attr("content");
                    } else if(e.attr("property").equals("og:url")) {
                        url = e.attr("content");
                    }
                }

                if(title.length() == 0) {
                    title = document.select("title").text();
                }

                if(desc.length() == 0) {
                    desc = document.select("meta[name=description]").attr("content");
                }

                if(strUrl.contains("twitter.com/")) {
                    imageUrl = document.select("div.ProfileAvatar img.ProfileAvatar-image").attr("src");
                    desc = document.select("div.ProfileHeaderCard p.ProfileHeaderCard-bio.u-dir").text();
                }

                if(strUrl.contains("play.google.com/store/apps/details?id=")) {
                    imageUrl = document.select("div.oQ6oV > div.hkhL9e > div.dQrBL > img").attr("src");
                    desc = document.select("div.DWPxHb > content > div").text();
                }

                if(strUrl.contains("cafe.naver.com")) {
                    String doct = document.select("script").html();
                    String clubId = doct.substring(doct.indexOf("g_sClubId = ") + 13, doct.indexOf("var g_mobileWebLink") - 4);
                    System.out.println("Naver Cafe ClubId = " + clubId);

                    if(clubId != "") {
                        Document cafepro = Jsoup.connect("http://cafe.naver.com/CafeProfileView.nhn?clubid=" + clubId).get();
                        imageUrl = cafepro.select("div.mcafe_icon.cafe_thumb_70 > img").attr("src");
                        desc = cafepro.select("td.invite-padd02.m-tcol-c").get(2).text();
                        url = cafepro.select("td.invite-padd02").get(1).text();
                    }
                }

                if(imageUrl == "") {
                    if(document.select("img").size() != 0) {
                        imageUrl = document.select("img").get(0).attr("src");
                    }
                }

                if(imageUrl.length() != 0) {
                    if(imageUrl.contains("//")) {
                        if (imageUrl.contains("http:") || imageUrl.contains("https:")) {
                            System.out.println("Image Checked");
                        } else {
                            imageUrl = "http:" + imageUrl;
                        }
                    }
                }

                System.out.println("Image : " + imageUrl);
                System.out.println("Title : " + title);
                System.out.println("Description : " + desc);

                if(imageUrl != "") {

                    HttpURLConnection im1 = (HttpURLConnection) new URL(imageUrl).openConnection();
                    im1.setRequestMethod("GET");
                    im1.setDoInput(true);
                    im1.setDoOutput(false);
                    im1.connect();

                    if (im1.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        bitmap = BitmapFactory.decodeStream(im1.getInputStream());
                    } else {
                        bitmap = null;
                    }

                } else if(imageUrl == "") {
                    bitmap = null;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Bitmap s) {
            mDialog.dismiss();
            super.onPostExecute(s);

            if(bitmap != null) {
                image.setImageBitmap(bitmap);
            } else {
                image.setImageBitmap(null);
            }

            if(strUrl.contains("cafe.naver.com")) {
                image.requestLayout();
                image.getLayoutParams().width = dpToPx(72, getApplicationContext());
                image.getLayoutParams().height = dpToPx(72, getApplicationContext());
            } else {
                image.requestLayout();
                image.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
                image.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
            }

            if(title != "") {
                textTitle.setText(title);
            } else {
                textTitle.setText("알수없음");
            }

            if(desc != "") {
                textDesc.setText(desc);
            } else {
                textDesc.setText("알수없음");
            }

            if(url != "") {
                textUrl.setText(url);
            } else {
                textUrl.setText("알수없음");
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(asyncTask != null) {
            asyncTask.cancel(true);
        }
    }

    private boolean isInternetCon() {
        cManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        return cManager.getActiveNetworkInfo() != null;
    }
}
