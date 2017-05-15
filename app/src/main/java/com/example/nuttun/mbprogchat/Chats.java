package com.example.nuttun.mbprogchat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Fragment;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class Chats extends Fragment implements OnClickListener {

    private EditText msg_edittext;
    private String mUsername;
    private String mSession_id;
    private String mThisUsername;
    public static ArrayList<ChatMessage> chatlist;
    public static ChatAdapter chatAdapter;
    ListView msgListView;

    MessageDbHelper dbHelper;
    SQLiteDatabase db;

    Timer timer1;
    Timer timer2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.chat_layout, container, false);

        mUsername = getArguments().getString("selectedUsername").trim();
        mSession_id = getArguments().getString("session_id").trim();
        mThisUsername = getArguments().getString("thisUsername").trim();
        msg_edittext = (EditText) view.findViewById(R.id.messageEditText);
        msgListView = (ListView) view.findViewById(R.id.msgListView);
        ImageButton sendButton = (ImageButton) view.findViewById(R.id.sendMessageButton);
        sendButton.setOnClickListener(this);

        // ----Set auto scroll of list view when a new message arrives----//
        msgListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        msgListView.setStackFromBottom(true);
        chatlist = new ArrayList<ChatMessage>();
        chatAdapter = new ChatAdapter(getActivity(), chatlist);

        dbHelper = new MessageDbHelper(getActivity());
        db = dbHelper.getReadableDatabase();
        msgListView.setAdapter(chatAdapter);
        readNewMessageFromDB();

        //read new messages db to message list
        timer1 = new Timer();
        repeatUpdateList(timer1);

        //Update db
        timer2 = new Timer();
        repeatUpdateDb(timer2);

        //setupUI(view.findViewById(R.id.chat_parent_layout));
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        timer1.cancel();
        timer2.cancel();
    }

    public void repeatUpdateDb(Timer timer) {

        final Handler handler = new Handler();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        readNewMessageFromDB();
                    }
                });
            }
        };
        timer.schedule(task, 0, 1500);  // milli sec
    }

    public void repeatUpdateList(Timer timer) {

        final Handler handler = new Handler();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        Log.d("updateDB getActivity():", getActivity()==null ? "null" : "have");
                        if(getActivity()!=null)
                            new updateDbTask().execute(getActivity(),mSession_id);
                    }
                });
            }
        };
        timer.schedule(task, 0, 1500);  // milli sec
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
    }

    public void sendTextMessage(View v) {
        String message = msg_edittext.getEditableText().toString();
        if (!message.equalsIgnoreCase("")) {
            new PostMessageTask().execute(message);
            msg_edittext.setText("");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sendMessageButton:
                sendTextMessage(v);

        }
    }

    public void readNewMessageFromDB(){
        String lastedSeqno = chatAdapter.getLastedSeqno();

        String[] projection = {
            MessageContract.MessageEntry.COLUMN_NAME_SEQNO,
            MessageContract.MessageEntry.COLUMN_NAME_DATETIME,
            MessageContract.MessageEntry.COLUMN_NAME_FROM,
            MessageContract.MessageEntry.COLUMN_NAME_TO,
            MessageContract.MessageEntry.COLUMN_NAME_MESSAGE
        };
        String selection = MessageContract.MessageEntry.COLUMN_NAME_SEQNO + " > ? AND (" +
                MessageContract.MessageEntry.COLUMN_NAME_FROM + " = ? OR " +
                MessageContract.MessageEntry.COLUMN_NAME_TO + " = ? )";
        String[] args = {lastedSeqno,mUsername,mUsername};
        Cursor cursor = db.query(MessageContract.MessageEntry.TABLE_NAME,
                projection,
                selection,
                args,
                null,null,
                MessageContract.MessageEntry.COLUMN_NAME_SEQNO
        );
        boolean isUpdate = false;
        while (cursor.moveToNext()){
            String message = cursor.getString(cursor.getColumnIndexOrThrow(MessageContract.MessageEntry.COLUMN_NAME_MESSAGE));
            String seqno = cursor.getString(cursor.getColumnIndexOrThrow(MessageContract.MessageEntry.COLUMN_NAME_SEQNO));
            String from = cursor.getString(cursor.getColumnIndexOrThrow(MessageContract.MessageEntry.COLUMN_NAME_FROM));
            String datetime = cursor.getString(cursor.getColumnIndexOrThrow(MessageContract.MessageEntry.COLUMN_NAME_DATETIME));
            boolean isMine = false;
            if(from.equalsIgnoreCase(mThisUsername)) isMine = true;
            ChatMessage chatMessage = new ChatMessage("thisUser",mUsername,message,seqno,isMine);
            chatMessage.Date = datetime.substring(0,datetime.indexOf(" "));
            chatMessage.Time = datetime.substring(datetime.indexOf(" ")+1);
            chatAdapter.add(chatMessage);
            isUpdate = true;
        }
        cursor.close();
        Log.d("updateList getActivity:", getActivity()==null ? "null" : "have");
        if(isUpdate && getActivity()!=null){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    chatAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private class PostMessageTask extends AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... message) {
            HTTPHelper helper = new HTTPHelper();
            HashMap<String,String> hm = new HashMap<>();
            hm.put("sessionid",mSession_id.trim());
            hm.put("targetname", mUsername.trim());
            hm.put("message", message[0].trim());
            String result = helper.POST("https://mis.cp.eng.chula.ac.th/mobile/service.php?q=api/postMessage",hm);
            JSONObject obj = null;
            try {
                obj = new JSONObject(result);
                String return_type = obj.getString("type");
                if(return_type.equals("error")){
                    showToast("Error sending message");
                    return null;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.d("updateDB getActivity: ", getActivity()==null ? "null" : "have");
            new updateDbTask().execute(getActivity(),mSession_id);
            readNewMessageFromDB();
        }
    }


    private void showToast(String text){
        Toast.makeText(getActivity(),text,Toast.LENGTH_SHORT).show();
    }

    /*
    public void setupUI(View view) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(getActivity());
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

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);
    }
    */
}