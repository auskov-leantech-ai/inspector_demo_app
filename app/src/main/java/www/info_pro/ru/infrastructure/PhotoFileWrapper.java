package www.info_pro.ru.infrastructure;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/*Файл фотограции*/
public class PhotoFileWrapper {
    /*Файл*/
    private File _file;

    /*Контекст*/
    private Context _context;

    /*Код объекта*/
    private int _objCode;

    /*Конструктор*/
    public PhotoFileWrapper(Context context, int objCode) throws IOException {
        this._context = context;
        this._objCode = objCode;
        this._file = this.createImageFile();
    }

    /*Конструктор*/
    public PhotoFileWrapper(Context context, String path, int objCode) {
        this._context = context;
        this._objCode = objCode;
        this._file = new File(path);
    }

    /*Создание файла фотографии*/
    private File createImageFile() throws IOException {
        File storageDir = AppFolder.getPhotoFolder(this._context);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "photo_" + _objCode + "_" + timeStamp + "_";
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    /*Возвращает путь к файлу*/
    public Uri getUri() {
        String authority = this._context.getPackageName() + ".fileProvider";
        return FileProvider.getUriForFile(this._context, authority, this._file);
    }

    public String getAbsolutePath() {
        return this._file.getAbsolutePath();
    }

    /* Возвращает код объекта*/
    public int getObjCode() {
        return this._objCode;
    }

    /*Удаление файла*/
    public boolean delete() {
        try {
            return this._file.delete();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
