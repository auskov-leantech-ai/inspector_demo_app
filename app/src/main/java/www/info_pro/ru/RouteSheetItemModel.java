package www.info_pro.ru;

import android.location.Location;

import java.util.Date;


/**
 * Элемент маршрутного листа
 */
class RouteSheetItemModel {

    /* Идентификатор записи */
    String Id;

    /* Тип документа (Строка из внешней системы)*/
    String DocType;

    /*Номер документа*/
    String DocumentNumber;

    /* Потребитель */
    String Consumer;

    /* Номер телефона потребителя*/
    String ConsumerPhone;

    /* Местоположение */
    String Location;

    /*Номер ПУ*/
    String MechanismNumber;

    /* Тип ПУ*/
    String MechanismType;

    /* Место устанвоки ПУ*/
    String FittingPosition;

    /* Дата установки прибора */
    Date MechanismInstallDt;

    /* Дата последней поверки прибора */
    Date MechanismLastVerifyDt;

    /* Кол - во зон прибора учета*/
    int MechanismZoneCount;

    /*КТ*/
    int MechanismRatio;

    /* Пломба */
    String Seal;

    /* Дата последних показаний*/
    Date PrevReadingDt;

    /* Дата текущих показаний*/
    Date CurrentReadingDt;

    /*Показания предыдущие по зоне 1 (День/Пик)*/
    Double PrevZone1Value;

    /*Показания предыдущие по зоне 2 (Ночь)*/
    Double PrevZone2Value;

    /*Показания предыдущие по зоне 3 (Полупик)*/
    Double PrevZone3Value;

    /*Показания текущие по зоне 1 (День/Пик)*/
    Double CurrentZone1Value;

    /*Показания текущие по зоне 2 (Ночь)*/
    Double CurrentZone2Value;

    /*Показания текущие по зоне 3 (Полупик)*/
    Double CurrentZone3Value;

    /* Кол-во знаков после запятой в ПКУ */
    int MechanismCapacityAfter;

    /* Средний расход */
    Double Average;

    /* Индекс сортировки (порядок следования)*/
    int SortInx;

    /* Состояние данных */
    RouteSheetItemDataStates DataState;

    /* Путь к фотограции зоны 1*/
    String Zone1_photoPath;

    /* Путь к фотограции зоны 2*/
    String Zone2_photoPath;

    /* Путь к фотограции зоны 3*/
    String Zone3_photoPath;

    /* Примечание */
    String Comment;

    /* Аномалия (предупреждения при сохранении показаний)*/
    String Anomaly;

    /* Гео локация*/
    String GeoPosition;

    /* NOT_SAVED Расстояние до объекта при поиске по GPS в метрах*/
    Double Distance;

    /* Возвращает истину если расход увеличен в 2 раза */
    boolean isTwiceTheAverage(NewValue nv) {
        if (this.Average == null) {
            return false;
        }

        double currentTotal = this.getDoubleValue(nv.CurrentZone1Value) - this.getDoubleValue(this.PrevZone1Value);
        if (this.MechanismZoneCount >= 2) {
            currentTotal += this.getDoubleValue(nv.CurrentZone2Value) - this.getDoubleValue(this.PrevZone2Value);
        }

        if (this.MechanismZoneCount == 3) {
            currentTotal += this.getDoubleValue(nv.CurrentZone3Value) - this.getDoubleValue(this.PrevZone3Value);
        }

        return currentTotal >= this.getDoubleValue(this.Average) * 2;
    }

    /* Отображаемый номер документа*/
    String getDisplayDocNumber() {
        return this.DocType + " " + this.DocumentNumber;
    }

    /* Возвращает наименование потребителя*/
    String getConsumer() {
        if (this.Consumer == null) {
            return "";
        }
        return this.Consumer;
    }

    /*Возвращает комментарий*/
    String getComment() {
        if (this.Comment == null || this.Comment.isEmpty()) {
            return null;
        }

        return this.Comment;
    }

    /* Возвращает истину если значение v равно значению зоны 1*/
    boolean equalsZone1Value(Double v) {
        return this.equalsDouble(v, this.CurrentZone1Value);
    }

    /* Возвращает истину если значение v равно значению зоны 2*/
    boolean equalsZone2Value(Double v) {
        return this.equalsDouble(v, this.CurrentZone2Value);
    }

    /* Возвращает истину если значение v равно значению зоны 3*/
    boolean equalsZone3Value(Double v) {
        return this.equalsDouble(v, this.CurrentZone3Value);
    }

    /* Возвращает истину если значение v равно значению поля Примечание*/
    boolean equalsComment(String v) {
        return this.equalsString(v, this.Comment);
    }

    /* Возвращает истину если значение v меньше предыдущих значений по тарифу 1*/
    boolean isLessZone1Value(Double v) {
        return this.PrevZone1Value > v;
    }

    /* Возвращает истину если значение v меньше предыдущих значений по тарифу 2*/
    boolean isLessZone2Value(Double v) {
        return this.PrevZone2Value > v;
    }

    /* Возвращает истину если значение v меньше предыдущих значений по тарифу 3*/
    boolean isLessZone3Value(Double v) {
        return this.PrevZone3Value > v;
    }

    /* Возвращает истину если пути к фото1 равны*/
    boolean equalsZone1Photo(String path) {
        return this.equalsString(path, this.Zone1_photoPath);
    }

    /* Возвращает истину если пути к фото2 равны*/
    boolean equalsZone2Photo(String path) {
        return this.equalsString(path, this.Zone2_photoPath);
    }

    /* Возвращает истину если пути к фото2 равны*/
    boolean equalsZone3Photo(String path) {
        return this.equalsString(path, this.Zone3_photoPath);
    }

    /* Установка новых значени */
    void MapNewValue(NewValue nv) {
        this.CurrentReadingDt = nv.CurrentReadingDt;
        this.CurrentZone1Value = nv.CurrentZone1Value;
        this.CurrentZone2Value = nv.CurrentZone2Value;
        this.CurrentZone3Value = nv.CurrentZone3Value;
        this.DataState = nv.DataState;
        this.Zone1_photoPath = nv.Zone1_photoPath;
        this.Zone2_photoPath = nv.Zone2_photoPath;
        this.Zone3_photoPath = nv.Zone3_photoPath;
        this.Comment = nv.Comment;
        this.Anomaly = nv.Anomaly;
        if(nv.GeoPosition != null && !nv.GeoPosition.isEmpty()){
            this.GeoPosition = nv.GeoPosition;
        }
    }

    /* Возвращает истину если стркои равны*/
    private boolean equalsString(String s1, String s2) {
        if (s1 == null || s1.isEmpty()) {
            return s2 == null || s2.isEmpty();
        }

        return s1.equals(s2);
    }

    /* Сравнение double */
    private boolean equalsDouble(Double d1, Double d2) {
        if (d1 == null) {
            return d2 == null;
        }

        return d1.equals(d2);
    }

    private double getDoubleValue(Double v) {
        if (v == null) {
            return 0;
        }

        return v;
    }

}

/* Новые данные для модели*/
class NewValue {

    /* Дата текущих показаний*/
    Date CurrentReadingDt;

    /*Показания текущие по зоне 1 (День/Пик)*/
    Double CurrentZone1Value;

    /*Показания текущие по зоне 2 (Ночь)*/
    Double CurrentZone2Value;

    /*Показания текущие по зоне 3 (Полупик)*/
    Double CurrentZone3Value;

    /* Состояние данных */
    RouteSheetItemDataStates DataState;

    /* Путь к фотограции зоны 1*/
    String Zone1_photoPath;

    /* Путь к фотограции зоны 2*/
    String Zone2_photoPath;

    /* Путь к фотограции зоны 3*/
    String Zone3_photoPath;

    /* Примечание */
    String Comment;

    /* Аномалия (предупреждения при сохранении показаний)*/
    String Anomaly;

    /* Гео локация*/
    String GeoPosition;

    boolean isExistReadings() {
        return CurrentZone1Value != null || CurrentZone2Value != null || CurrentZone3Value != null;
    }

    int getPhotoCount() {
        int photoCount = 0;
        if (Zone1_photoPath != null && !Zone1_photoPath.isEmpty()) photoCount++;
        if (Zone2_photoPath != null && !Zone2_photoPath.isEmpty()) photoCount++;
        if (Zone3_photoPath != null && !Zone3_photoPath.isEmpty()) photoCount++;

        return photoCount;
    }

    void setLocation(Location location) {
        String gps = Location.convert(location.getLatitude(), Location.FORMAT_DEGREES) + ";" + Location.convert(location.getLongitude(), Location.FORMAT_DEGREES);
        this.GeoPosition = gps.replace(',', '.');
    }
}

