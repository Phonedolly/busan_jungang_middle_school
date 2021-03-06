package com.busan.jungangmiddleschool;

import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.Calendar;

import toast.library.meal.MealLibrary;
import com.busan.jungangmiddleschool.bap.BapActivity;
import com.busan.jungangmiddleschool.info.SchoolInfo;
import com.busan.jungangmiddleschool.schedule.ScheduleActivity;
import com.busan.jungangmiddleschool.settings.SettingsActivity;
import com.busan.jungangmiddleschool.timetable.TimeTableActivity;
import com.busan.jungangmiddleschool.todaylist.TodayList;
import com.busan.jungangmiddleschool.tool.BapTool;
import com.busan.jungangmiddleschool.tool.Database;
import com.busan.jungangmiddleschool.tool.Preference;
import com.busan.jungangmiddleschool.tool.TimeTableTool;
import com.busan.jungangmiddleschool.tool.copyAssets;

public class MainActivity extends ActionBarActivity {
    ActionBarDrawerToggle mToggle;
    DrawerLayout mDrawer;
    Toolbar mToolbar;

    LinearLayout container;

    Calendar mCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.mToolbar);
        mToolbar.setBackgroundColor(getResources().getColor(R.color.primary));
        setSupportActionBar(mToolbar);
        mToolbar.setTitleTextColor(Color.WHITE);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mToggle = new ActionBarDrawerToggle(this, mDrawer, R.string.app_name, R.string.app_name);
        mDrawer.setDrawerListener(mToggle);

        mCalendar = Calendar.getInstance();

        container = (LinearLayout) findViewById(R.id.container);
        container.addView(getTodayBapData(), 0);
        container.addView(getTodayTimeTable(), 1);
    }

    public View getTodayBapData() {
        View mView = getLayoutInflater().inflate(R.layout.activity_main_cardview_bap, null);

        int year = mCalendar.get(Calendar.YEAR);
        int month = mCalendar.get(Calendar.MONTH);
        int day = mCalendar.get(Calendar.DAY_OF_MONTH);

        BapTool.restoreBapDateClass mData =
                BapTool.restoreBapData(this, year, month, day);

        if (!mData.isBlankDay) {
            int hour = mCalendar.get(Calendar.HOUR_OF_DAY);

            LinearLayout todayBapLayout = (LinearLayout) mView.findViewById(R.id.todayBapLayout);
            todayBapLayout.setVisibility(View.VISIBLE);

            TextView todayBapType = (TextView) mView.findViewById(R.id.todayBapType);
            TextView todayBapData = (TextView) mView.findViewById(R.id.todayBapData);

            /**
             * hour : 0~23
             *
             * 0~13 : Lunch
             * 14~23 : Dinner
             */
            String mTodayMeal;
            if (hour <= 13) {
                todayBapType.setText(R.string.today_lunch);
                mTodayMeal = mData.Lunch;
                todayBapData.setText(!MealLibrary.isMealCheck(mTodayMeal) ? getResources().getString(R.string.no_data_lunch) : mTodayMeal);
            } else {
                todayBapType.setText(R.string.today_dinner);
                mTodayMeal = mData.Dinner;
                todayBapData.setText(!MealLibrary.isMealCheck(mTodayMeal) ? getResources().getString(R.string.no_data_dinner) : mTodayMeal);
            }
        }

        return mView;
    }

    public View getTodayTimeTable() {
        View mView = getLayoutInflater().inflate(R.layout.activity_main_cardview_timetable, null);

        Preference mPref = new Preference(getApplicationContext());

        int mGrade = mPref.getInt("myGrade", -1);
        int mClass = mPref.getInt("myClass", -1);

        if (mGrade == -1 || mClass == -1) {
            return mView;
        }

        int DayOfWeek = mCalendar.get(Calendar.DAY_OF_WEEK);
        if (DayOfWeek > 1 && DayOfWeek < 7) {
            DayOfWeek -= 2;
        } else {
            return mView;
        }

        if (TimeTableTool.getDBUpdate(getApplicationContext())) {
            copyAssets copyAssets = new copyAssets();
            copyAssets.assetsFileCopy(getApplicationContext(), TimeTableTool.mFilePath, TimeTableTool.TimeTableDBName);
        }

        boolean mFileExists = new File(TimeTableTool.mFilePath + TimeTableTool.TimeTableDBName).exists();
        if (!mFileExists)
            return mView;

        String mTimeTable = "";
        LinearLayout todayTimeTableLayout = (LinearLayout) mView.findViewById(R.id.todayTimeTableLayout);
        TextView todayOfWeek = (TextView) mView.findViewById(R.id.todayOfWeek);
        TextView todayTimeTable = (TextView) mView.findViewById(R.id.todayTimeTable);

        Database mDatabase = new Database();
        mDatabase.openOrCreateDatabase(TimeTableTool.mFilePath, TimeTableTool.TimeTableDBName, TimeTableTool.tableName, "");

        Cursor mCursor = mDatabase.getData(TimeTableTool.tableName, "*");

        mCursor.moveToPosition((DayOfWeek * 7) + 1);

        for (int period = 1; period <= 7; period++) {
            String mSubject;

            if (mGrade == 1) {
                mSubject = mCursor.getString((mClass * 2) - 2);
//                mRoom = mCursor.getString((mClass * 2) - 1);
            } else if (mGrade == 2) {
                mSubject = mCursor.getString(18 + (mClass * 2));
//                mRoom = mCursor.getString(19 + (mClass * 2));
            } else {
                mSubject = mCursor.getString(39 + mClass);
//                mRoom = null;
            }

            if (mSubject != null && !mSubject.isEmpty()
                    && mSubject.indexOf("\n") != -1)
                mSubject = mSubject.replace("\n", " (") + ")";

            String tmp = Integer.toString(period) + ". " + mSubject;
            mTimeTable += tmp;

            if (mCursor.moveToNext()) {
                mTimeTable += "\n";
            }
        }

        todayTimeTableLayout.setVisibility(View.VISIBLE);
        todayOfWeek.setText(String.format(getString(R.string.today_timetable), TimeTableTool.mDisplayName[DayOfWeek]));
        todayTimeTable.setText(mTimeTable);

        return mView;
    }

    public void Bap(View mView) {
        startActivity(new Intent(this, BapActivity.class));
    }

    public void TimeTable(View mView) {
        startActivity(new Intent(this, TimeTableActivity.class));
    }

    public void Schedule(View mView) {
        startActivity(new Intent(this, ScheduleActivity.class));
    }

    public void Info(View mView) {
        startActivity(new Intent(this, SchoolInfo.class));
    }

    public void TodayList(View mView) {
        startActivity(new Intent(this, TodayList.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;

        } else if (mToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

// Sync the toggle state after onRestoreInstanceState has occurred.
        mToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(Gravity.LEFT)) {
            mDrawer.closeDrawer(Gravity.LEFT);
        } else {
            finish();
        }
    }
}
