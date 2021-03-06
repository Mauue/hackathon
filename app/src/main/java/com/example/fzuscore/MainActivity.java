package com.example.fzuscore;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    long lastBackTime;
    SharedPreferences spf;
    int thisTerm;
    List<List<Subject>> termSubjectList = new ArrayList<>();
    List<TermScoreFragment> termScoreFragmentList = new ArrayList<>();
    List<String> termsList = new ArrayList<>();
    TabLayout tabLayout;
    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        tabLayout = findViewById(R.id.tabs);
        viewPager = findViewById(R.id.viewpager);
        spf = getSharedPreferences("info", MODE_PRIVATE);
        initInfo();
        for(int i=0;i<termsList.size();i++)
            requestScores(Integer.valueOf(termsList.get(i)));
        initViewPager();
    }

    TermScoreFragmentAdapter adapter;
    private synchronized void initViewPager() {
        adapter = new TermScoreFragmentAdapter(getSupportFragmentManager(), termScoreFragmentList,termsList);
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                termScoreFragmentList.clear();
                termSubjectList.clear();
                for (int i = 0; i < termsList.size(); i++)
                    requestScores(Integer.valueOf(termsList.get(i)));
                    refreshAdapter();
                break;
        }
        return true;
    }

    synchronized void  refreshAdapter() {
        runOnUiThread(()->viewPager.getAdapter().notifyDataSetChanged());
    }
    private void initInfo() {

        if (spf.getBoolean("logined", false)) {
            int amountOfTerms = spf.getInt("term_amount", 0);
            String userName = spf.getString("user_name", "用户名");
            int userId = spf.getInt("user_account", 0);
            UserInfo.setInfo(userId, userName);
            try {
                JSONArray termsJSON = new JSONArray(spf.getString("termJSONArray", ""));
                int[] termJSONArray = JSONUtils.getIntArrayFromJSONArray(termsJSON);
                thisTerm = termJSONArray[0];
                for (int i = 0; i < amountOfTerms; i++) {
                    termsList.add(String.valueOf(termJSONArray[i]));
                    tabLayout.addTab(tabLayout.newTab().setText(String.valueOf(termJSONArray[i])));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void requestScores(int term) {
        new Thread(() -> {
            try {
                updateLists(term);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateLists(int term) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
        String url = "http://47.112.10.160:3389/api/score";
        RequestScoreJSON requestScoreJSON = new RequestScoreJSON(UserInfo.getStudent_id(), term);
        String json = new Gson().toJson(requestScoreJSON);
        RequestBody requestBody = FormBody.create(MediaType.parse("application/json; charset=utf-8"), json);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Connection", "close")
                .build();
        Response response = client.newCall(request).execute();
        String responseData = response.body().string();
        System.out.println(responseData);
        System.out.println("-=-=-=--=-="+term+"=-=-==-=-");
        parseJSON(responseData,term);
    }

    private synchronized void parseJSON(String responseData,int term) {
        try {
            JSONObject jsonObject = new JSONObject(responseData);
            int subjectsAmount = jsonObject.getInt("subjects_amount");
            JSONArray jsonArray = jsonObject.getJSONArray("subjects");
            List<Subject> subjectList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject subjectJSON = jsonArray.getJSONObject(i);
                double subject_score = subjectJSON.getDouble("subject_score");
                int subject_rank = subjectJSON.getInt("subject_rank");
                String subject_name = subjectJSON.getString("subject_name");
                double subject_averscore = subjectJSON.getDouble("subject_averscore");
                subjectList.add(new Subject(subject_name, subject_score, subject_averscore, subject_rank));
            }
            termSubjectList.add(subjectList);
            termScoreFragmentList.add(TermScoreFragment.newInstance(subjectList,termsList));
            System.out.println("添加了一个fragment");
            runOnUiThread(()->{
                viewPager.getAdapter().notifyDataSetChanged();
                System.out.println(term+"finish");
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            long thisTime = Calendar.getInstance().getTimeInMillis();
            if (thisTime - lastBackTime > 1000) {
                lastBackTime = thisTime;
                Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show();
            } else {
                finish();
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Intent intent;
        switch (id) {
            case R.id.nav_overview:
                break;
            case R.id.nav_analysis:
                break;
            case R.id.nav_class_overview:
                intent = new Intent(this, ClassActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_forms:
                intent = new Intent(this, ScoreListActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_logout:
                quitAccount();
                intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.nav_exit:
                finish();
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void quitAccount() {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient.Builder()
                    .readTimeout(8, TimeUnit.SECONDS)//设置读取超时时间
                    .writeTimeout(8,TimeUnit.SECONDS)//设置写的超时时间
                    .connectTimeout(9,TimeUnit.SECONDS)//设置连接超时时间
                    .build();
            try {
                logout(client);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

    private void logout(OkHttpClient client) throws IOException {
        SharedPreferences.Editor spf = getSharedPreferences("info", MODE_PRIVATE).edit();
        spf.clear();
        spf.apply();
        spf.commit();
        String url = "http://47.112.10.160:3389/api/logout";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        client.newCall(request).execute();
    }
}