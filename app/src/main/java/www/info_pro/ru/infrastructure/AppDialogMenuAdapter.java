package www.info_pro.ru.infrastructure;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import www.info_pro.ru.R;

public class AppDialogMenuAdapter extends ArrayAdapter<AppDialogMenuItem> {

    public AppDialogMenuAdapter(@NonNull Context context, @NonNull AppDialogMenuItem[] objects) {
        super(context, android.R.layout.select_dialog_item, android.R.id.text1, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = super.getView(position, convertView, parent);
        TextView tv = v.findViewById(android.R.id.text1);
        tv.setTextSize(16);
        Typeface font = ResourcesCompat.getFont(this.getContext(), R.font.roboto_medium);
        tv.setTypeface(font, Typeface.NORMAL);

        AppDialogMenuItem item = this.getItem(position);
        int icon = item != null ? item.icon : R.drawable.error_icon;
        tv.setTextColor(icon != R.drawable.delete ? Color.parseColor("#212126") : Color.parseColor("#E20101"));
        tv.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);

        int dp5 = (int) (5 * this.getContext().getResources().getDisplayMetrics().density + 0.5f);
        tv.setCompoundDrawablePadding(25 + dp5);

        return v;
    }
}