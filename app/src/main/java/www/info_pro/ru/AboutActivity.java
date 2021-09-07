package www.info_pro.ru;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView versionView = findViewById(R.id.about_version_lbl);
        versionView.setText("Версия " + BuildConfig.VERSION_NAME);
    }
}
