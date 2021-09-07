package www.info_pro.ru;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.URLUtil;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import www.info_pro.ru.infrastructure.AppSettingController;

/* Форма настроек приложения */
public class SettingActivity extends AppCompatActivity {

    /* Контроллер настроек*/
    private AppSettingController _settingController;
    private EditText _etServerUri;
    private EditText _etLogin;
    private EditText _etPwd;
    private EditText _etBranchCode;
    private EditText _etPinCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        this.configureMenu();

        this._etServerUri = findViewById(R.id.setting_server_url);
        this._etLogin = findViewById(R.id.setting_login);
        this._etPwd = findViewById(R.id.setting_pwd);
        this._etBranchCode = findViewById(R.id.setting_branch_code);
        this._etPinCode = findViewById(R.id.setting_pin_code);
        this.restoreSettings();
    }

    /* конфигурирование меню*/
    private void configureMenu() {
        BottomNavigationView menu = findViewById(R.id.setting_bottom_menu);
        menu.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_action_settings_close:
                        close();
                        return true;
                    case R.id.menu_action_settings_save:
                        save();
                        return true;
                }
                return false;
            }
        });
    }

    private void close() {
        this.finish();
    }

    /* Восстанвление настроек*/
    private void restoreSettings() {
        _settingController = new AppSettingController(this);
        this._etServerUri.setText(_settingController.getServerUrl());
        this._etLogin.setText(_settingController.getLogin());
        this._etPwd.setText(_settingController.getPwd());
        this._etBranchCode.setText(_settingController.getBranchCode());
        this._etPinCode.setText(_settingController.getPIN());
    }

    /* Вызывается при сохранении показаний */
    private void save() {
        String err = this.validate();
        if (err != null) {
            AlertMessages alertMessages = new AlertMessages(this);
            alertMessages.showErrorMsg("Сервер(url) указан неверно. Укажите сервер в формате http://{host}:{port}");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setIcon(R.drawable.icon);
        builder.setTitle("Подтверждение");
        builder.setMessage("Вы действительно хотите сохранить текущие настройки приложения?");
        builder.setPositiveButton("Сохранить",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveSettings();
                        finish();
                    }
                });
        builder.setNegativeButton("Отменить", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /*Сохранение настроек*/
    private void saveSettings() {
        String serverUri = this.getServerUrl();
        this._settingController.setServerUrl(serverUri);

        String login = this._etLogin.getText().toString();
        this._settingController.setLogin(login);

        String pwd = this._etPwd.getText().toString();
        this._settingController.setPwd(pwd);

        String branchCode = this._etBranchCode.getText().toString();
        this._settingController.setBranchCode(branchCode);

        String pin = this._etPinCode.getText().toString();
        this._settingController.setPIN(pin);
    }

    /**
     * Проверка настроек перед сохранением
     */
    private String validate() {
        String serverUri = this.getServerUrl();
        if (!URLUtil.isValidUrl(serverUri)) {
            return "Сервер(url) указан неверно. Укажите сервер в формате http://{host}:{port}";
        }

        return null;
    }

    private String getServerUrl() {
        return this._etServerUri.getText().toString();
    }
}

