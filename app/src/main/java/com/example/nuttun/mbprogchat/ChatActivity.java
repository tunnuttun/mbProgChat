package com.example.nuttun.mbprogchat;

//import android.app.Fragment;
import android.app.FragmentManager;
//import android.app.FragmentActivity;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.app.FragmentTransaction;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.FrameLayout;
import android.support.v4.app.Fragment;

public class ChatActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Chats chat = new Chats();
        chat.setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction().add(R.id.fragment_container,chat).commit();

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle("Chats with " + getIntent().getStringExtra("selectedUsername"));
    }

}
