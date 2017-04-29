package com.example.nuttun.mbprogchat;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private ListView mFriendListView;
    private List<String> mContactList;
    private String mSession_id;
    private String mUsername;
    private Toolbar myToolbar;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_friend:
                    //mTextMessage.setText(mUsername + ": " + mSession_id);
                    setSupportActionBar(myToolbar);
                    getSupportActionBar().setTitle("Friend List");

                    return true;
                case R.id.navigation_addFriend:
                    //mTextMessage.setText(R.string.title_dashboard);
                    setSupportActionBar(myToolbar);
                    getSupportActionBar().setTitle("Add Friend");
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Find session_id first]
        int checkFile = 0;
        try {
            FileInputStream fileInputStream = openFileInput("session_id");
            mSession_id = convertStreamToString(fileInputStream);
            fileInputStream.close();
            fileInputStream = openFileInput("username");
            mUsername = convertStreamToString(fileInputStream);
            fileInputStream.close();
        } catch (FileNotFoundException e){
            e.printStackTrace();
            checkFile = 1;
        } catch (Exception e) {
            e.printStackTrace();
            checkFile = 1;
        }

        //Create layout

        if(checkFile == 1){
            startActivity(new Intent(this,LoginActivity.class));
            finish();
        }

        setContentView(R.layout.activity_main);
        myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle("Friend List");
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //initialize list
        mFriendListView = (ListView) findViewById(R.id.friendList);

        if(checkFile == 0)
            new ContactListTask().execute();
    }

    //Convert streaminput bytes to string
    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logout_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if(item.getItemId() == R.id.logout_menu){
            Context context = getApplicationContext();
            context.deleteFile("session_id");
            context.deleteFile("username");
            startActivity(new Intent(this,LoginActivity.class));
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public class ContactListTask extends AsyncTask<Void, Void, List<String>>{

        @Override
        protected List<String> doInBackground(Void... params) {
            HTTPHelper helper = new HTTPHelper();
            HashMap<String,String> hm = new HashMap<String, String>();
            hm.put("sessionid",mSession_id.trim());
            String result = helper.POST("https://mis.cp.eng.chula.ac.th/mobile/service.php?q=api/getContact",hm);
            JSONObject obj = null;
            List<String> contactList = new ArrayList<String>();
            try {
                obj = new JSONObject(result);
                String return_type = obj.getString("type");
                if(return_type.equals("error")){
                    //Invalid session_id
                    CharSequence text = "Unexpected Error!, please try logout";
                    Toast toast = Toast.makeText(getApplicationContext(),text,Toast.LENGTH_LONG);
                    toast.show();
                }
                else{
                    //Valid session id
                    JSONArray jsonArray = obj.getJSONArray("content");
                    if (jsonArray != null) {
                        for (int i=0;i<jsonArray.length();i++){
                            contactList.add(jsonArray.getString(i));
                        }
                    }
                    return contactList;
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<String> arrayList) {
            mContactList = arrayList;
            setFriendListAdapter();
        }
    }

    private void setFriendListAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mContactList);
        mFriendListView.setAdapter(adapter);
    }


}
