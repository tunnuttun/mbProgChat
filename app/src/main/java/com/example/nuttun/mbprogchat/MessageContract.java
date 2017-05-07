package com.example.nuttun.mbprogchat;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Created by Nuttun on 07/05/2017.
 */

public final class MessageContract {
    private MessageContract() {}

    public static class MessageEntry implements BaseColumns{
        public static final String TABLE_NAME = "allMessage";
        public static final String COLUMN_NAME_SEQNO = "seqno";
        public static final String COLUMN_NAME_DATETIME = "datetime";
        public static final String COLUMN_NAME_FROM = "from_user";
        public static final String COLUMN_NAME_TO = "to_user";
        public static final String COLUMN_NAME_MESSAGE = "message";
    }


}
