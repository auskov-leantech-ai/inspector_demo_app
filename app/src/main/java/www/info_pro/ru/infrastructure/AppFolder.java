package www.info_pro.ru.infrastructure;

import android.content.Context;

import java.io.File;

/* Описывает папки приложения */
public class AppFolder {
    /* Возвращает ссылку на папку с фотограциями */
    public static File getPhotoFolder(Context context){
        return context.getExternalFilesDir("photos");
    }
}
