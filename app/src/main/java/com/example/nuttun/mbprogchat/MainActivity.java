package com.example.nuttun.mbprogchat;

import com.example.nuttun.mbprogchat.MessageContract;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebHistoryItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    private ListView mFriendListView;
    private List<String> mContactList;

    private LinearLayout mAddFriendLayout;
    private EditText mSearchField;
    private ListView mSearchedUserListView;
    private List<String> mSearchedUserList;

    private String mSession_id;
    private String mUsername;

    private Toolbar myToolbar;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_friend:
                    //Set toolbar
                    setSupportActionBar(myToolbar);
                    getSupportActionBar().setTitle("Friend List");

                    //set visibility
                    mAddFriendLayout.setVisibility(View.INVISIBLE);
                    mFriendListView.setVisibility(View.VISIBLE);

                    //Set list adapter
                    setFriendListAdapter();

                    //Check if list is updated
                    new ContactListTask().execute();

                    return true;
                case R.id.navigation_addFriend:
                    //Set toolbar
                    setSupportActionBar(myToolbar);
                    getSupportActionBar().setTitle("Add Friend");

                    //set visibility
                    mAddFriendLayout.setVisibility(View.VISIBLE);
                    mFriendListView.setVisibility(View.INVISIBLE);


                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Find session_id file
        boolean isSessionFile;
        try {
            isSessionFile = true;
            FileInputStream fileInputStream = openFileInput("session_id");
            mSession_id = convertStreamToString(fileInputStream);
            fileInputStream.close();
            fileInputStream = openFileInput("username");
            mUsername = convertStreamToString(fileInputStream);
            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            isSessionFile = false;
        }

        if(!isSessionFile){
            startActivity(new Intent(this,LoginActivity.class));
            finish();
        }


        //Create layout
        setContentView(R.layout.activity_main);
        myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle("Friend List");
        myToolbar.setTitleTextColor(Color.rgb(255,255,255));
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //initialize search field and searched list
        mAddFriendLayout = (LinearLayout) findViewById(R.id.add_friend_layout);
        mSearchedUserListView = (ListView) findViewById(R.id.searched_user_list_view);
        mSearchField = (EditText) findViewById(R.id.search_field);
        mAddFriendLayout.setVisibility(View.INVISIBLE);
        TextView emptyText = (TextView)findViewById(android.R.id.empty);
        mSearchedUserListView.setOnItemClickListener(mSearchedFriendListClickedHandler);
        mSearchedUserListView.setEmptyView(emptyText);
        mSearchField.addTextChangedListener(
                new TextWatcher() {
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                    private Timer timer=new Timer();
                    @Override
                    public void afterTextChanged(final Editable s) {
                        timer.cancel();
                        timer = new Timer();
                        timer.schedule(
                                new TimerTask() {
                                    @Override
                                    public void run() {
                                        //HTTP request searched list
                                        String searchInput = mSearchField.getText().toString();
                                        new SearchUserTask().execute(searchInput);
                                    }
                                },
                                500 //Delay values(milli sec)
                        );
                    }
                }
        );

        //initialize friend list
        mFriendListView = (ListView) findViewById(R.id.friendList);
        mFriendListView.setOnItemClickListener(mFriendListClickedHandler);

        setupUI(findViewById(R.id.main_parent_layout));

        //Read friend list from file and refresh contact from internet
        if(isSessionFile) {
            try{
                FileInputStream fileInputStream = openFileInput("contact_list");
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                mContactList = (List<String>) objectInputStream.readObject();
                objectInputStream.close();
                fileInputStream.close();
                setFriendListAdapter();
            } catch (Exception e){
                e.printStackTrace();
                mContactList=null;
            }
            new ContactListTask().execute();

            //Read and write message database
            repeatUpdateDb();
        }


    }

    public void repeatUpdateDb() {

        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        new updateDbTask().execute(getApplicationContext(),mSession_id);
                    }
                });
            }
        };
        timer.schedule(task, 0, 60*1000);  // interval of one minute
    }

    //Convert stream input bytes to string
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


    private class ContactListTask extends AsyncTask<Void, Void, List<String>>{
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
                    return null;
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
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<String> list) {
            if(list == null) return;

            //if list not equal, save the list and set ListAdapter
            if(mContactList==null || !mContactList.equals(list)){
                FileOutputStream outputStream = null;
                try {
                    outputStream = openFileOutput("contact_list", Context.MODE_PRIVATE);
                    ObjectOutputStream objectOutputStream =  new ObjectOutputStream(outputStream);
                    objectOutputStream.writeObject(list);
                    objectOutputStream.close();
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mContactList = list;
                setFriendListAdapter();
            }
            //If list was equal, do nothing.
        }
    }

    private class SearchUserTask extends AsyncTask<String, Void, List<String>>{

        @Override
        protected List<String> doInBackground(String... username) {
            HTTPHelper helper = new HTTPHelper();
            HashMap<String,String> hm = new HashMap<>();
            hm.put("sessionid",mSession_id.trim());
            hm.put("keyword", username[0].trim());
            String result = helper.POST("https://mis.cp.eng.chula.ac.th/mobile/service.php?q=api/searchUser",hm);
            JSONObject obj = null;
            List<String> searchedUser = new ArrayList<String>();
            try {
                obj = new JSONObject(result);
                String return_type = obj.getString("type");
                if(return_type.equals("error")){
                    return null;
                }
                else{
                    //Valid session id
                    JSONArray jsonArray = obj.getJSONArray("content");
                    if (jsonArray != null) {
                        for (int i=0;i<jsonArray.length();i++){
                            searchedUser.add(jsonArray.getString(i));
                        }
                    }
                    return searchedUser;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<String> searchedUser) {
            if(searchedUser == null) mSearchedUserListView.setAdapter(null);
            else {
                if(mContactList != null) searchedUser.removeAll(mContactList);
                mSearchedUserList = searchedUser;
                setSearchedUserListAdapter();
            }
        }
    }

    private class addContactTask extends AsyncTask<String, Void, Boolean>{
        private String thisUsername;

        @Override
        protected Boolean doInBackground(String... usernames) {
            HTTPHelper helper = new HTTPHelper();
            HashMap<String,String> hm = new HashMap<>();
            thisUsername = usernames[0].trim();
            hm.put("sessionid",mSession_id.trim());
            hm.put("username", thisUsername);
            String result = helper.POST("https://mis.cp.eng.chula.ac.th/mobile/service.php?q=api/addContact",hm);
            JSONObject obj = null;
            try {
                obj = new JSONObject(result);
                String return_type = obj.getString("type");
                if(return_type.equals("error")){
                    return false;
                }
                else{
                    //Valid session id
                    if(obj.getString("content").equals("false")){
                        return false;
                    } else{

                        return true;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean succeed) {
            if(succeed){
                String text = thisUsername + " has been added to friend list";
                showToast(text);
            }
            else {
                showToast("Unexpected Error!, try re-login");
            }
        }
    }


    private void setFriendListAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mContactList);
        mFriendListView.setAdapter(adapter);
    }

    private void setSearchedUserListAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mSearchedUserList);
        mSearchedUserListView.setAdapter(adapter);
        /*
        // Force hide keyboard
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        */
    }



    private AdapterView.OnItemClickListener mSearchedFriendListClickedHandler = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            // Create add friend dialog
            new AddfriendDialogFragment().newInstance(position).show(getFragmentManager(),"addFriendTag");
        }
    };

    private AdapterView.OnItemClickListener mFriendListClickedHandler = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            String selectedUsername = mContactList.get(position);
            startChatActivity(selectedUsername);
        }
    };

    private void startChatActivity(String selectedUsername){
        Intent intent = new Intent(this,ChatActivity.class);
        intent.putExtra("selectedUsername",selectedUsername);
        intent.putExtra("session_id",mSession_id);
        intent.putExtra("thisUsername",mUsername);
        this.startActivity(intent);
    }


    private class AddfriendDialogFragment extends DialogFragment{

        public AddfriendDialogFragment newInstance(int index) {
            AddfriendDialogFragment frag = new AddfriendDialogFragment();
            Bundle args = new Bundle();
            args.putInt("index", index);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            int index = getArguments().getInt("index");
            final String username = mSearchedUserList.get(index);
            builder.setTitle("Confirm adding friend?")
                    .setMessage("Add " + username + " to friend list")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //HTTP post to add contact
                            new addContactTask().execute(username);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showToast("Adding friend cancel");
                        }
                    });
            return builder.create();
        }

    }

    private void showToast(String text){
        Toast.makeText(getApplicationContext(),text,Toast.LENGTH_SHORT).show();
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);
    }

    public void setupUI(View view) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(MainActivity.this);
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }
}
