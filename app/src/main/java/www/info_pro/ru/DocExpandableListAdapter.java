package www.info_pro.ru;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


/**
 * Адаптер списка документов/потребителей для обхода
 */
public class DocExpandableListAdapter extends BaseExpandableListAdapter {

    /*Контекст*/
    private Context _context;

    /* Все группы с документами*/
    private ArrayList<RouteSheetModel> _originalDocumentGroups;

    /* Отображаемые группы и документы*/
    private ArrayList<RouteSheetModel> _viewDocumentGroups = new ArrayList<>();

    /*Конструктор*/
    DocExpandableListAdapter(Context context, ArrayList<RouteSheetModel> documentGroups) {
        this._context = context;
        this._originalDocumentGroups = documentGroups;
        this._viewDocumentGroups.addAll(documentGroups);
    }

    @Override
    public int getGroupCount() {
        return this._viewDocumentGroups.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        RouteSheetModel group = this._viewDocumentGroups.get(groupPosition);
        return group.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this._viewDocumentGroups.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        RouteSheetModel group = _viewDocumentGroups.get(groupPosition);
        if (group.Documents.size() == 0) {
            return null;
        }
        return group.Documents.get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inaInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inaInflater.inflate(R.layout.doc_list_group, null);
        }

        TextView tvName = convertView.findViewById(R.id.docGroupName);
        TextView tvCount = convertView.findViewById(R.id.docGroupCounts);

        RouteSheetModel group = this._viewDocumentGroups.get(groupPosition);
        if (group == null) {
            tvName.setText(" --- ");
        } else {
            tvName.setText(group.Name);
            int itemCounts = group.size();
            int completedCount = group.completedCount();
            String countText = "[" + completedCount + "/" + itemCounts + "]";
            tvCount.setText(countText);
        }

        return convertView;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inaInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inaInflater.inflate(R.layout.doc_list_item, null);
        }

        RouteSheetModel group = this._viewDocumentGroups.get(groupPosition);
        RouteSheetItemModel item = group.Documents.get(childPosition);

        String lbl1 = item.getDisplayDocNumber() + "  ПУ:" + item.MechanismNumber;
        TextView tvHeader = convertView.findViewById(R.id.doc_list_item_name);
        tvHeader.setText(lbl1);

        TextView tvLocation = convertView.findViewById(R.id.doc_list_item_location);
        tvLocation.setText(item.Location);

        TextView tvDistance = convertView.findViewById(R.id.doc_list_item_distance);
        tvDistance.setText(item.Distance == null ? "" : "(" + item.Distance + " м.)");

        ImageView imageView = convertView.findViewById(R.id.consumer_state_icon);
        Drawable state = this.getStateView(item);
        imageView.setImageDrawable(state);

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    /*Возвращает соостояние элемента*/
    Drawable getStateView(RouteSheetItemModel item) {
        if (item.DataState == RouteSheetItemDataStates.Sync) {
            return this._context.getDrawable(R.drawable.sync_data_state);
        } else if (item.DataState == RouteSheetItemDataStates.Modify) {
            return this._context.getDrawable(R.drawable.modify_data_state);
        }
        return this._context.getDrawable(R.drawable.not_data_state);
    }

    /* Фильтрация данных в списке по строке запроса*/
    void filterByData(String query) {
        this.viewGroupsReset();
        if (query.isEmpty()) {
            this._viewDocumentGroups.addAll(this._originalDocumentGroups);
        } else {
            query = query.toLowerCase();
            for (int g = 0; g < this._originalDocumentGroups.size(); g++) {
                RouteSheetModel group = this._originalDocumentGroups.get(g);
                ArrayList<RouteSheetItemModel> filterItems = new ArrayList<>();
                for (int i = 0; i < group.Documents.size(); i++) {
                    RouteSheetItemModel item = group.Documents.get(i);
                    if (item.DocumentNumber != null && item.DocumentNumber.toLowerCase().contains(query)) {
                        filterItems.add(item);
                    } else if (item.MechanismNumber != null && item.MechanismNumber.toLowerCase().contains(query)) {
                        filterItems.add(item);
                    } else if (item.Consumer != null && item.Consumer.toLowerCase().contains(query)) {
                        filterItems.add(item);
                    }
                }

                if (filterItems.size() > 0) {
                    this._viewDocumentGroups.add(new RouteSheetModel(group.Id, group.Name, filterItems));
                }
            }
        }
        this.notifyDataSetChanged();
    }

    /* Фильтрация данных по GPS координатам */
    void filterByGps(Location location, double maxRadius) {
        this.viewGroupsReset();
        if (location != null) {
            for (int g = 0; g < this._originalDocumentGroups.size(); g++) {
                RouteSheetModel group = this._originalDocumentGroups.get(g);
                ArrayList<RouteSheetItemModel> filterItems = new ArrayList<>();
                for (int i = 0; i < group.Documents.size(); i++) {
                    RouteSheetItemModel item = group.Documents.get(i);
                    if (item.GeoPosition == null || item.GeoPosition.isEmpty()) {
                        continue;
                    }

                    String[] coordinates = item.GeoPosition.split(";");
                    if (coordinates.length != 2) {
                        continue;
                    }
                    try {
                        double latitude = Double.parseDouble(coordinates[0].replace(',','.'));
                        double longitude = Double.parseDouble(coordinates[1].replace(',','.'));

                        Location itemLocation = new Location("this");
                        itemLocation.setLatitude(latitude);
                        itemLocation.setLongitude(longitude);
                        float distance = location.distanceTo(itemLocation);
                        if (distance <= maxRadius) { // проверяем что входит в радиус поиска
                            item.Distance = new Double(Math.round(distance));
                            filterItems.add(item);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                }

                if (filterItems.size() > 0) {
                    Collections.sort(filterItems, new Comparator<RouteSheetItemModel>() {
                        @Override
                        public int compare(RouteSheetItemModel o1, RouteSheetItemModel o2) {
                            return o1.Distance.compareTo(o2.Distance);
                        }
                    });
                    this._viewDocumentGroups.add(new RouteSheetModel(group.Id, group.Name, filterItems));
                }
            }
        }
        this.notifyDataSetChanged();
    }

    /* Сбрасывает настройки списка на отображение */
    private void viewGroupsReset() {
        for (int i = 0; i < this._viewDocumentGroups.size(); i++) {
            ArrayList<RouteSheetItemModel> documents = _viewDocumentGroups.get(i).Documents;
            for (int j = 0; j < documents.size(); j++) {
                documents.get(j).Distance = null;
            }
        }
        this._viewDocumentGroups.clear();
    }
}

