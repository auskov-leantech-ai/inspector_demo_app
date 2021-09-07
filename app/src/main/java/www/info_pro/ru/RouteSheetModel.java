package www.info_pro.ru;

import java.util.ArrayList;


/**
 * Модель маршрутного листа
 */
class RouteSheetModel {
    /* Идентификатор маршрутного листа*/
    String Id;

    /*Наименование*/
    String Name;

    /*Список элементов */
    ArrayList<RouteSheetItemModel> Documents;

    /*Конструктор*/
    RouteSheetModel(String id,
                    String name,
                    ArrayList<RouteSheetItemModel> documents) {
        this.Id = id;
        this.Name = name;
        this.Documents = documents;
    }

    /* Добавляет новый элемент в группу */
    void addItem(RouteSheetItemModel item) {
        this.Documents.add(item);
    }

    /* Кол-во элементов в группе*/
    int size() {
        return this.Documents.size();
    }

    /* Кол-во проверенных элементов в группе */
    int completedCount() {
        int count = 0;
        for (int i = 0; i < Documents.size(); i++) {
            RouteSheetItemModel item = Documents.get(i);
            if (item.DataState !=RouteSheetItemDataStates.None) {
                count++;
            }
        }
        return count;
    }
}
