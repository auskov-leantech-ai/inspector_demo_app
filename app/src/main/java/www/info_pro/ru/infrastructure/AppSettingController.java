package www.info_pro.ru.infrastructure;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

/* Контроллер для управления настройками приложения*/
public class AppSettingController {

    private final String SETTING_KEY = "ServerSetting";
    private final String SERVER_URI_KEY = "server_uri";
    private final String LOGIN_KEY = "server_login";
    private final String PWD_KEY = "server_pwd";
    private final String BRANCH_KEY = "server_branch_code";
    private final String PIN_KEY = "server_pin_code";
    private final String ENCODE_KEY = "infoproXZQ";

    /* SharedPreferences */
    private SharedPreferences _settings;

    /* Конструктор*/
    public AppSettingController(Context context) {
        _settings = context.getSharedPreferences(SETTING_KEY, Context.MODE_PRIVATE);
    }

    /* Установка нового сервера*/
    public void setServerUrl(String serverUri) {
        SharedPreferences.Editor prefEditor = _settings.edit();
        prefEditor.putString(SERVER_URI_KEY, encode(serverUri));
        prefEditor.apply();
    }

    /* Возвращает логин*/
    public String getLogin() {
        return this.decode(_settings.getString(LOGIN_KEY, ""));
    }

    /* Установка логина*/
    public void setLogin(String login) {
        SharedPreferences.Editor prefEditor = _settings.edit();
        prefEditor.putString(LOGIN_KEY, encode(login));
        prefEditor.apply();
    }

    /* Возвращает пароля*/
    public String getPwd() {
        return this.decode(_settings.getString(PWD_KEY, ""));
    }

    /* Установка пароля*/
    public void setPwd(String pwd) {
        SharedPreferences.Editor prefEditor = _settings.edit();
        prefEditor.putString(PWD_KEY, encode(pwd));
        prefEditor.apply();
    }

    /* Возвращает код филиала*/
    public String getBranchCode() {
        return this.decode(_settings.getString(BRANCH_KEY, ""));
    }

    /* Установка кода филиала*/
    public void setBranchCode(String code) {
        SharedPreferences.Editor prefEditor = _settings.edit();
        prefEditor.putString(BRANCH_KEY, encode(code));
        prefEditor.apply();
    }

    /* Возвращает пин-код*/
    public String getPIN() {
        return this.decode(_settings.getString(PIN_KEY, ""));
    }

    /* Установка пин-кода*/
    public void setPIN(String pin) {
        SharedPreferences.Editor prefEditor = _settings.edit();
        prefEditor.putString(PIN_KEY, encode(pin));
        prefEditor.apply();
    }

    /* Возвращает uri сервера*/
    public String getServerUrl() {
        String url = _settings.getString(SERVER_URI_KEY, "");
        return this.decode(url);
    }

    /*Проверка настроек на достоверность*/
    public String validate() {
        String serverUrl = this.getServerUrl();
        if (serverUrl == null || serverUrl.isEmpty()) {
            return "Не указан url - сервера";
        }

        String branchCode = this.getBranchCode();
        if (branchCode == null || branchCode.isEmpty()) {
            return "Не указан код-филиала";
        }

        String pin = this.getPIN();
        if (pin == null || pin.isEmpty()) {
            return "Не указан PIN-код";
        }
        return null;
    }

    /* Кодируем строку */
    private String encode(String s) {
        return Base64.encodeToString(xor(s.getBytes(), ENCODE_KEY.getBytes()), 0);
    }

    /* decode */
    private String decode(String s) {
        return new String(xor(Base64.decode(s, 0), ENCODE_KEY.getBytes()));
    }

    /* XOR */
    private static byte[] xor(byte[] a, byte[] key) {
        byte[] out = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = (byte) (a[i] ^ key[i % key.length]);
        }
        return out;
    }
}
