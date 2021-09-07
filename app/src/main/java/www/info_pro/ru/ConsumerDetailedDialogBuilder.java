package www.info_pro.ru;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * Построитель диалогового окна с подробной информацией о потребителе
 */
class ConsumerDetailedDialogBuilder {

    /*Контекст*/
    private Context _context;

    /*Констурктор*/
    ConsumerDetailedDialogBuilder(Context context) {
        this._context = context;
    }

    /* Построение диалогового окна*/
    Dialog Build(RouteSheetItemModel documentItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(_context);
        builder.setTitle("Информация о потребителе");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        if (documentItem == null) {
            return builder.create();
        }

        final View content = createView(documentItem);
        builder.setView(content);
        return builder.create();
    }

    /* Создание View*/
    private View createView(RouteSheetItemModel documentItem) {
        LayoutInflater factory = LayoutInflater.from(_context);
        @SuppressLint("InflateParams") final View content = factory.inflate(R.layout.consumer_detailed_dialog, null);
        TextView txNumber = content.findViewById(R.id.document_number);
        TextView txConsumerName = content.findViewById(R.id.consumer_name);
        TextView txConsumerPhone = content.findViewById(R.id.consumer_phone);
        TextView txLocation = content.findViewById(R.id.consumer_location);
        TextView txMechanismNumber = content.findViewById(R.id.mechanism_number);
        TextView txMechanismType = content.findViewById(R.id.mechanism_type);
        TextView txZoneCount = content.findViewById(R.id.mechanism_zone_count);
        TextView txInstallDt = content.findViewById(R.id.mechanism_install_dt);
        TextView txCapacityAfter = content.findViewById(R.id.mechanism_capacity_after);
        TextView txVerificationDt = content.findViewById(R.id.mechanism_verification_dt);
        TextView txAverage = content.findViewById(R.id.average_value);
        TextView txSeal = content.findViewById(R.id.mechanism_seal);
        TextView txFittingPosition = content.findViewById(R.id.fitting_position);
        TextView txRatio = content.findViewById(R.id.mechanism_ratio);
        TextView txGps = content.findViewById(R.id.consumer_gps);

        String noData = "нет данных";
        SimpleDateFormat dtFormatter = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
        txNumber.setText(documentItem.getDisplayDocNumber());
        txConsumerName.setText(documentItem.Consumer);
        txConsumerPhone.setText(!this.isNullOrEmpty(documentItem.ConsumerPhone) ? documentItem.ConsumerPhone : noData);
        txLocation.setText(documentItem.Location);
        txMechanismNumber.setText(documentItem.MechanismNumber);
        txMechanismType.setText(documentItem.MechanismType);
        txGps.setText(!this.isNullOrEmpty(documentItem.GeoPosition) ? documentItem.GeoPosition : noData);
        txZoneCount.setText(String.format(Locale.US, "%d", documentItem.MechanismZoneCount));
        txRatio.setText(String.format(Locale.US, "%d", documentItem.MechanismRatio));
        txCapacityAfter.setText(String.format(Locale.US, "%d", documentItem.MechanismCapacityAfter));

        Date mechanismInstallDt = documentItem.MechanismInstallDt;
        Date mechanismLastVerifyDt = documentItem.MechanismLastVerifyDt;

        txInstallDt.setText(mechanismInstallDt != null ? dtFormatter.format(mechanismInstallDt) : noData);
        txVerificationDt.setText(mechanismLastVerifyDt != null ? dtFormatter.format(mechanismLastVerifyDt) : noData);
        txSeal.setText(!isNullOrEmpty(documentItem.Seal) ? documentItem.Seal : noData);
        txFittingPosition.setText(!isNullOrEmpty(documentItem.FittingPosition) ? documentItem.FittingPosition : noData);
        txAverage.setText(documentItem.Average == null ? noData : documentItem.Average.toString());

        return content;
    }

    private boolean isNullOrEmpty(String t) {
        if (t == null) {
            return true;
        }

        return t.isEmpty();
    }

}
