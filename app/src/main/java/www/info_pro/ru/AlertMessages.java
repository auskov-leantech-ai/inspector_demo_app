package www.info_pro.ru;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import java.net.SocketTimeoutException;

/* Всплывающие сообщения*/
public class AlertMessages {
    private Context _context;

    public AlertMessages(Context context) {
        this._context = context;
    }

    /* Отобразить сообщение о исключении */
    void showException(Throwable t, String header) {
        if (t instanceof SocketTimeoutException) {
            showErrorMsg(header + " Превышено время ожидания получения ответа!");
            return;
        }

        StringBuilder errMsg = new StringBuilder(header + t.fillInStackTrace().getMessage());
        while (t != null) {
            errMsg.append(" ").append(t.getMessage());
            t = t.getCause();
        }
        showErrorMsg(errMsg.toString());
    }

    /* Создание сообщения об успехе */
    void showSuccessMsg(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this._context);
        builder.setCancelable(false);
        builder.setIcon(R.drawable.success_icon);
        builder.setTitle("Задача выполнена!");
        builder.setMessage(msg);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /* Создание сообщения с предупреждением */
    void showWarningMsg(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this._context);
        builder.setCancelable(false);
        builder.setIcon(R.drawable.warning_icon);
        builder.setTitle("Предупреждение");
        builder.setMessage(msg);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /* Создание сообщения об успехе */
    void showErrorMsg(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this._context);
        builder.setCancelable(false);
        builder.setIcon(R.drawable.error_icon);
        builder.setTitle("Ошибка!");
        builder.setMessage(msg);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /* Возвращает построитель диалогового окна с вопросом*/
    AlertDialog.Builder getConfirmBuilder(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this._context);
        builder.setCancelable(true);
        builder.setIcon(R.drawable.quest_icon);
        builder.setTitle("Подтверждение");
        builder.setMessage(message);
        builder.setNegativeButton("НЕТ", null);
        return builder;
    }

    /*Создает диалоговое окно с прогрессом*/
    public AlertDialog createProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this._context);
        builder.setCancelable(false);
        builder.setView(R.layout.progress_dialog);
        return builder.create();
    }

}

