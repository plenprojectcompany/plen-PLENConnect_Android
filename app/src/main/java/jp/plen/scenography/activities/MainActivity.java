package jp.plen.scenography.activities;

import android.app.ActivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;

import jp.plen.scenography.R;
import jp.plen.scenography.fragments.NavigationDrawerFragment;
import jp.plen.scenography.fragments.EditFragment;

public class MainActivity extends AppCompatActivity implements NavigationDrawerFragment.Callbacks {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String STATE_CURRENT_SECTION_NUMBER = "current page";
    private static final String STATE_FRAGMENT_STATES = "fragment_states";
    private String[] mSectionTitles;
    private Bundle mFragmentStates = new Bundle();

    private int mCurrentSectionNumber = 0;
    private Fragment mCurrentSectionFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 各ページのタイトル
        mSectionTitles = new String[]{getString(R.string.title_edit_fragment)};

        // 状態復元
        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }

        // NavigationDrawerの設定
        NavigationDrawerFragment navigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        navigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                drawerLayout,
                mSectionTitles,
                mCurrentSectionNumber);

        setTaskDescription(new ActivityManager.TaskDescription(
                getString(R.string.app_name), null, getResources().getColor(R.color.theme500)));
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mFragmentStates = savedInstanceState.getBundle(STATE_FRAGMENT_STATES);
        mCurrentSectionNumber = savedInstanceState.getInt(STATE_CURRENT_SECTION_NUMBER);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        onSaveCurrentSectionState();

        String title = mSectionTitles[position];

        if (title.equals(getString(R.string.title_edit_fragment))) {
            mCurrentSectionFragment = EditFragment.newInstance();
        } else {
            throw new AssertionError();
        }

        mCurrentSectionNumber = position;
        mCurrentSectionFragment.setArguments(
                mFragmentStates.getBundle(title));

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, mCurrentSectionFragment)
                .commit();
    }

    private void onSaveCurrentSectionState() {
        if (mCurrentSectionFragment == null) {
            return;
        }
        Bundle state = new Bundle();
        mCurrentSectionFragment.onSaveInstanceState(state);
        mFragmentStates.putBundle(mSectionTitles[mCurrentSectionNumber], state);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        onSaveCurrentSectionState();

        outState.putBundle(STATE_FRAGMENT_STATES, mFragmentStates);
        outState.putInt(STATE_CURRENT_SECTION_NUMBER, mCurrentSectionNumber);
    }
}