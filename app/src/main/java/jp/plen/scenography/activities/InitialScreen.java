package jp.plen.scenography.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageButton;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import jp.plen.scenography.R;
import jp.plen.scenography.models.preferences.MainPreferences_;


@EActivity(R.layout.activity_initial_screen_white)
public class InitialScreen extends AppCompatActivity {

    @ViewById(R.id.ButtonToController) ImageButton toController;
    @ViewById(R.id.ButtonToProgramming) ImageButton toProgramming;
    @Pref MainPreferences_ mPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @AfterViews
    void afterViews(){
        toController.setOnClickListener(v -> {
            Intent intent = new Intent(InitialScreen.this, MainActivity_.class);
            intent.putExtra("joystick",true);
            startActivity(intent);
        });

        toProgramming.setOnClickListener(v -> {
            Intent intent = new Intent(InitialScreen.this, MainActivity_.class);
            intent.putExtra("joystick",false);
            startActivity(intent);
        });
    }
}
