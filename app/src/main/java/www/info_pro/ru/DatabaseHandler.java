package www.info_pro.ru;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

/* Обработчик базы данных*/
public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 8;
    private static final String DATABASE_NAME = "offline_db";

    /*Конструктор*/
    DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /* Вызывается при создании БД*/
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(RouteSheetContract.CREATE_SCRIPT);
        db.execSQL(RouteSheetItemContract.CREATE_SCRIPT);
        this.testDataGenerate(db);
    }

    /*onUpgrade */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + RouteSheetItemContract.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + RouteSheetContract.TABLE_NAME);
        onCreate(db);
    }

    /* Возвращает все маршрутные листы сохраненные в локальной БД*/
    ArrayList<RouteSheetModel> GetAllRouteSheets() {
        SQLiteDatabase db = this.getWritableDatabase();

        ArrayList<RouteSheetModel> groups = this.getRouteSheets(db);
        Cursor itemsCursor = db.rawQuery("SELECT * FROM " + RouteSheetItemContract.TABLE_NAME + " order by " + RouteSheetItemContract.COLUMN_SORT_INX, null);
        if (itemsCursor.moveToFirst()) {
            do {
                String groupId = itemsCursor.getString(1);
                RouteSheetModel group = null;
                for (RouteSheetModel g : groups) {
                    if (g.Id.equals(groupId)) {
                        group = g;
                        break;
                    }
                }

                if (group == null) {
                    continue;
                }

                RouteSheetItemModel item = new RouteSheetItemModel();
                item.Id = this.getString(itemsCursor, RouteSheetItemContract.COLUMN_ID);
                item.DocType = this.getString(itemsCursor, RouteSheetItemContract.COLUMN_DOC_TYPE);
                item.DocumentNumber = this.getString(itemsCursor, RouteSheetItemContract.COLUMN_DOC_NUMBER);
                item.Consumer = this.getString(itemsCursor, RouteSheetItemContract.COLUMN_CONSUMER);
                item.ConsumerPhone = this.getString(itemsCursor, RouteSheetItemContract.COLUMN_CONSUMER_PHONE);
                item.Location = this.getString(itemsCursor, RouteSheetItemContract.COLUMN_LOCATION);
                item.MechanismNumber = this.getString(itemsCursor, RouteSheetItemContract.COLUMN_MECHANISM_NUMBER);
                item.MechanismType = this.getString(itemsCursor, RouteSheetItemContract.COLUMN_MECHANISM_TYPE);
                item.MechanismInstallDt = this.getDate(itemsCursor, RouteSheetItemContract.COLUMN_MECHANISM_INSTALL_DT);
                item.MechanismLastVerifyDt = this.getDate(itemsCursor, RouteSheetItemContract.COLUMN_MECHANISM_CHECK_DT);
                item.MechanismZoneCount = this.getInt(itemsCursor, RouteSheetItemContract.COLUMN_MECHANISM_ZONE_COUNT);
                item.MechanismRatio = this.getInt(itemsCursor, RouteSheetItemContract.COLUMN_MECHANISM_RATIO);
                item.Seal = this.getString(itemsCursor, RouteSheetItemContract.COLUMN_MECHANISM_SEAL);
                item.MechanismCapacityAfter = this.getInt(itemsCursor, RouteSheetItemContract.COLUMN_MECHANISM_CAPACITY_AFTER);
                item.FittingPosition = this.getString(itemsCursor, RouteSheetItemContract.COLUMN_FITTING_POSITION);
                item.PrevReadingDt = this.getDate(itemsCursor, RouteSheetItemContract.COLUMN_PREV_READING_DT);
                item.CurrentReadingDt = this.getDate(itemsCursor, RouteSheetItemContract.COLUMN_CURRENT_READING_DT);
                item.PrevZone1Value = this.getDouble(itemsCursor, RouteSheetItemContract.COLUMN_PREV_ZONE1_VALUE);
                item.PrevZone2Value = this.getDouble(itemsCursor, RouteSheetItemContract.COLUMN_PREV_ZONE2_VALUE);
                item.PrevZone3Value = this.getDouble(itemsCursor, RouteSheetItemContract.COLUMN_PREV_ZONE3_VALUE);
                item.CurrentZone1Value = this.getDouble(itemsCursor, RouteSheetItemContract.COLUMN_CURRENT_ZONE1_VALUE);
                item.CurrentZone2Value = this.getDouble(itemsCursor, RouteSheetItemContract.COLUMN_CURRENT_ZONE2_VALUE);
                item.CurrentZone3Value = this.getDouble(itemsCursor, RouteSheetItemContract.COLUMN_CURRENT_ZONE3_VALUE);
                item.Average = this.getDouble(itemsCursor, RouteSheetItemContract.COLUMN_AVERAGE);
                item.Zone1_photoPath = this.getString(itemsCursor, RouteSheetItemContract.COLUMN_ZONE1_PHOTO);
                item.Zone2_photoPath = this.getString(itemsCursor, RouteSheetItemContract.COLUMN_ZONE2_PHOTO);
                item.Zone3_photoPath = this.getString(itemsCursor, RouteSheetItemContract.COLUMN_ZONE3_PHOTO);
                item.SortInx = this.getInt(itemsCursor, RouteSheetItemContract.COLUMN_SORT_INX);

                int stateInx = this.getInt(itemsCursor, RouteSheetItemContract.COLUMN_DATA_STATE);
                item.DataState = stateInx == 1
                        ? RouteSheetItemDataStates.Modify
                        : (stateInx == 2 ? RouteSheetItemDataStates.Sync : RouteSheetItemDataStates.None);
                item.Comment = this.getString(itemsCursor, RouteSheetItemContract.COLUMN_COMMENT);
                item.Anomaly = this.getString(itemsCursor, RouteSheetItemContract.COLUMN_ANOMALY);
                item.GeoPosition = this.getString(itemsCursor, RouteSheetItemContract.COLUMN_GEO);

                group.addItem(item);
            } while (itemsCursor.moveToNext());
        }
        itemsCursor.close();
        return groups;
    }

    /* Обновление введенных данных в БД*/
    Boolean updateRouteSheetItem(RouteSheetItemModel item) {
        if (item == null) {
            return false;
        }

        ContentValues val = new ContentValues();
        val.put(RouteSheetItemContract.COLUMN_CURRENT_READING_DT, persistDate(item.CurrentReadingDt));
        val.put(RouteSheetItemContract.COLUMN_CURRENT_ZONE1_VALUE, item.CurrentZone1Value);
        val.put(RouteSheetItemContract.COLUMN_CURRENT_ZONE2_VALUE, item.CurrentZone2Value);
        val.put(RouteSheetItemContract.COLUMN_CURRENT_ZONE3_VALUE, item.CurrentZone3Value);
        val.put(RouteSheetItemContract.COLUMN_DATA_STATE, item.DataState.index());
        val.put(RouteSheetItemContract.COLUMN_COMMENT, item.Comment);
        val.put(RouteSheetItemContract.COLUMN_ANOMALY, item.Anomaly);
        val.put(RouteSheetItemContract.COLUMN_ZONE1_PHOTO, item.Zone1_photoPath);
        val.put(RouteSheetItemContract.COLUMN_ZONE2_PHOTO, item.Zone2_photoPath);
        val.put(RouteSheetItemContract.COLUMN_ZONE3_PHOTO, item.Zone3_photoPath);
        val.put(RouteSheetItemContract.COLUMN_GEO, item.GeoPosition);
        String whereClause = RouteSheetItemContract.COLUMN_ID + "= ?";

        SQLiteDatabase db = this.getWritableDatabase();
        int updateResult = db.update(RouteSheetItemContract.TABLE_NAME, val, whereClause, new String[]{item.Id});
        db.close();

        return updateResult == 1;
    }

    /* Импорт новых маршрутных листов*/
    void importRouteSheets(ArrayList<ExportRouteSheet> importGroups) {
        if (importGroups == null || importGroups.size() == 0) {
            return;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        ArrayList<String> notUpdateGroupIds = this.getAllSavedGroupIds(db); // группы которые не обновлены
        ArrayList<String> notUpdateItemIds = this.getAllSavedItemIds(db); // элементы которые не обновлены

        // обновление существующих или создание новых данных
        for (ExportRouteSheet group : importGroups) {
            String groupId = group.getId();
            if (notUpdateGroupIds.contains(groupId)) {
                notUpdateGroupIds.remove(groupId);
                ContentValues content = this.getGroupContentValues(group, false);
                String whereClause = RouteSheetItemContract.COLUMN_ID + "= ?";
                db.update(RouteSheetContract.TABLE_NAME, content, whereClause, new String[]{groupId});
            } else {
                // создаем новую группу
                ContentValues content = this.getGroupContentValues(group, true);
                db.insert(RouteSheetContract.TABLE_NAME, null, content);
            }

            for (ExportRouteSheetItem item : group.getItems()) {
                String itemId = item.getId();
                if (notUpdateItemIds.contains(itemId)) {
                    notUpdateItemIds.remove(itemId);
                    ContentValues content = getItemContentValues(groupId, item, false);
                    String whereClause = RouteSheetItemContract.COLUMN_ID + "= ?";
                    db.update(RouteSheetItemContract.TABLE_NAME, content, whereClause, new String[]{itemId});
                } else {
                    ContentValues content = getItemContentValues(groupId, item, true);
                    db.insert(RouteSheetItemContract.TABLE_NAME, null, content);
                }
            }
        }

        // удаляем элементы маршрута которые не были обработаны
        for (String itemId : notUpdateItemIds) {
            String whereClause = RouteSheetItemContract.COLUMN_ID + "= ?";
            db.delete(RouteSheetItemContract.TABLE_NAME, whereClause, new String[]{itemId});
        }

        // удаляем группы которые не были обработаны
        for (String groupId : notUpdateGroupIds) {
            String whereClause = RouteSheetItemContract.COLUMN_HEADER_ID + "= ?";
            db.delete(RouteSheetItemContract.TABLE_NAME, whereClause, new String[]{groupId});

            whereClause = RouteSheetContract.COLUMN_ID + "= ?";
            db.delete(RouteSheetContract.TABLE_NAME, whereClause, new String[]{groupId});
        }

        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    /* Возвращает показания которые необходимо отправить на сервер*/
    ArrayList<UpdateReadingDataModel> getUpdateReadingDataModels() {
        ArrayList<UpdateReadingDataModel> readings = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "SELECT * FROM " + RouteSheetItemContract.TABLE_NAME + " WHERE "
                + RouteSheetItemContract.COLUMN_DATA_STATE + " = " + RouteSheetItemDataStates.Modify.index();
        Cursor itemsCursor = db.rawQuery(sql, null);
        if (itemsCursor.moveToFirst()) {
            do {
                UpdateReadingDataModel model = new UpdateReadingDataModel();
                model.setRouteSheetItemId(this.getString(itemsCursor, RouteSheetItemContract.COLUMN_ID));
                model.setMechanismNumber(this.getString(itemsCursor, RouteSheetItemContract.COLUMN_MECHANISM_NUMBER));
                model.setReadingDt(this.getDate(itemsCursor, RouteSheetItemContract.COLUMN_CURRENT_READING_DT));
                model.setZone1Value(this.getDouble(itemsCursor, RouteSheetItemContract.COLUMN_CURRENT_ZONE1_VALUE));
                model.setZone2Value(this.getDouble(itemsCursor, RouteSheetItemContract.COLUMN_CURRENT_ZONE2_VALUE));
                model.setZone3Value(this.getDouble(itemsCursor, RouteSheetItemContract.COLUMN_CURRENT_ZONE3_VALUE));
                model.setAnomaly(this.getString(itemsCursor, RouteSheetItemContract.COLUMN_ANOMALY));
                model.setComment(this.getString(itemsCursor, RouteSheetItemContract.COLUMN_COMMENT));
                model.setGeo(this.getString(itemsCursor, RouteSheetItemContract.COLUMN_GEO));
                model.setPhotoZ1Path(this.getString(itemsCursor, RouteSheetItemContract.COLUMN_ZONE1_PHOTO));
                model.setPhotoZ2Path(this.getString(itemsCursor, RouteSheetItemContract.COLUMN_ZONE2_PHOTO));
                model.setPhotoZ3Path(this.getString(itemsCursor, RouteSheetItemContract.COLUMN_ZONE3_PHOTO));
                readings.add(model);
            } while (itemsCursor.moveToNext());
        }

        itemsCursor.close();
        db.close();
        return readings;
    }

    /* Обновление информации о синхронизации показаний */
    void updateSyncInfo(ArrayList<UpdateReadingResultModel> syncResults) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues val = new ContentValues();
        for (UpdateReadingResultModel r : syncResults) {
            if (r.isError()) { // ошибка синхронизации
                continue;
            }

            RouteSheetItemDataStates state = r.isDataUpdate()
                    ? RouteSheetItemDataStates.Sync
                    : RouteSheetItemDataStates.None;
            val.put(RouteSheetItemContract.COLUMN_DATA_STATE, state.index());
            String whereClause = RouteSheetItemContract.COLUMN_ID + "= ?";
            db.update(RouteSheetItemContract.TABLE_NAME, val, whereClause, new String[]{r.getRouteSheetItemId()});
        }
        db.close();
    }

    /* Возвращает маршрутные листы из бд */
    private ArrayList<RouteSheetModel> getRouteSheets(SQLiteDatabase db) {
        ArrayList<RouteSheetModel> groups = new ArrayList<>();
        Cursor groupCursor = db.rawQuery("SELECT * FROM " + RouteSheetContract.TABLE_NAME, null);
        if (groupCursor.moveToFirst()) {
            do {
                String id = groupCursor.getString(0);
                String name = groupCursor.getString(1);
                RouteSheetModel group = new RouteSheetModel(id, name, new ArrayList<RouteSheetItemModel>());
                groups.add(group);
            } while (groupCursor.moveToNext());
        }
        groupCursor.close();
        return groups;
    }

    /* Возвращает все актуальные идентификаторы сохраненных групп*/
    private ArrayList<String> getAllSavedGroupIds(SQLiteDatabase db) {
        ArrayList<String> groups = new ArrayList<>();
        Cursor groupCursor = db.rawQuery("SELECT " + RouteSheetContract.COLUMN_ID + " FROM " + RouteSheetContract.TABLE_NAME, null);
        if (groupCursor.moveToFirst()) {
            do {
                String id = groupCursor.getString(0);
                groups.add(id);
            } while (groupCursor.moveToNext());
        }
        groupCursor.close();
        return groups;
    }

    /* Возвращает все актуальные идентификаторы сохраненных элементов маршрута*/
    private ArrayList<String> getAllSavedItemIds(SQLiteDatabase db) {
        ArrayList<String> groups = new ArrayList<>();
        Cursor groupCursor = db.rawQuery("SELECT " + RouteSheetItemContract.COLUMN_ID + " FROM " + RouteSheetItemContract.TABLE_NAME, null);
        if (groupCursor.moveToFirst()) {
            do {
                String id = groupCursor.getString(0);
                groups.add(id);
            } while (groupCursor.moveToNext());
        }
        groupCursor.close();
        return groups;
    }

    /* ContentValues для создания/обновления элементов маршрута*/
    private ContentValues getItemContentValues(String groupId, ExportRouteSheetItem item, boolean isInsert) {
        ContentValues content = new ContentValues();
        if (isInsert) {
            content.put(RouteSheetItemContract.COLUMN_ID, item.getId());
            content.put(RouteSheetItemContract.COLUMN_HEADER_ID, groupId);
            content.put(RouteSheetItemContract.COLUMN_DATA_STATE, RouteSheetItemDataStates.None.index());
        }

        content.put(RouteSheetItemContract.COLUMN_CONSUMER, item.getConsumer());
        content.put(RouteSheetItemContract.COLUMN_CONSUMER_PHONE, item.getConsumerPhone());
        content.put(RouteSheetItemContract.COLUMN_DOC_TYPE, item.getDocType());
        content.put(RouteSheetItemContract.COLUMN_DOC_NUMBER, item.getDocNumber());
        content.put(RouteSheetItemContract.COLUMN_MECHANISM_NUMBER, item.getMechanismNumber());
        content.put(RouteSheetItemContract.COLUMN_MECHANISM_TYPE, item.getMechanismType());
        content.put(RouteSheetItemContract.COLUMN_LOCATION, item.getLocation());
        content.put(RouteSheetItemContract.COLUMN_MECHANISM_ZONE_COUNT, item.getMechanismZoneCount());
        content.put(RouteSheetItemContract.COLUMN_MECHANISM_INSTALL_DT, persistDate(item.getMechanismInstallDt()));
        content.put(RouteSheetItemContract.COLUMN_MECHANISM_CHECK_DT, persistDate(item.getMechanismLastVerifyDt()));
        content.put(RouteSheetItemContract.COLUMN_MECHANISM_SEAL, item.getSeal());
        content.put(RouteSheetItemContract.COLUMN_FITTING_POSITION, item.getFittingPosition());
        content.put(RouteSheetItemContract.COLUMN_MECHANISM_RATIO, item.getMechanismRatio());
        content.put(RouteSheetItemContract.COLUMN_MECHANISM_CAPACITY_AFTER, 2); // TODO Поправить - брать из сервиса
        content.put(RouteSheetItemContract.COLUMN_SORT_INX, item.getSortInx());
        content.put(RouteSheetItemContract.COLUMN_PREV_READING_DT, persistDate(item.getPrevReadingDt()));
        content.put(RouteSheetItemContract.COLUMN_PREV_ZONE1_VALUE, item.getPrevZone1Value());
        content.put(RouteSheetItemContract.COLUMN_PREV_ZONE2_VALUE, item.getPrevZone2Value());
        content.put(RouteSheetItemContract.COLUMN_PREV_ZONE3_VALUE, item.getPrevZone3Value());
        content.put(RouteSheetItemContract.COLUMN_AVERAGE, item.getAverageTotal());
        content.put(RouteSheetItemContract.COLUMN_GEO, item.getGeo());

        return content;
    }

    /* ContentValues для создания/обновления группы*/
    private ContentValues getGroupContentValues(ExportRouteSheet group, boolean isInsert) {
        ContentValues content = new ContentValues();
        if (isInsert) {
            content.put(RouteSheetContract.COLUMN_ID, group.getId());
        }
        content.put(RouteSheetContract.COLUMN_NAME, group.getName());
        return content;
    }

    /*Создает тестовые данные*/
    private void testDataGenerate(SQLiteDatabase db) {
        for (int i = 1; i <= 2; i++) {

            String route_sheet_uid = "route_sheet_" + i;
            ContentValues groupValues = new ContentValues();
            groupValues.put(RouteSheetContract.COLUMN_ID, route_sheet_uid);
            groupValues.put(RouteSheetContract.COLUMN_NAME, "Тестовый маршрут №" + i);
            db.insert(RouteSheetContract.TABLE_NAME, null, groupValues);

            Random random = new Random();
            for (int j = 1; j <= 500; j++) {
                ContentValues itemValues = new ContentValues();
                itemValues.put(RouteSheetItemContract.COLUMN_ID, "item_" + route_sheet_uid + "_" + j);
                itemValues.put(RouteSheetItemContract.COLUMN_HEADER_ID, route_sheet_uid);
                itemValues.put(RouteSheetItemContract.COLUMN_CONSUMER, "Васильев А. В.");
                itemValues.put(RouteSheetItemContract.COLUMN_CONSUMER_PHONE, "8(927)6939140");
                itemValues.put(RouteSheetItemContract.COLUMN_DOC_TYPE, "ЛС");
                itemValues.put(RouteSheetItemContract.COLUMN_DOC_NUMBER, "84" + random.nextInt(99000000 + j));
                itemValues.put(RouteSheetItemContract.COLUMN_MECHANISM_NUMBER, "06" + random.nextInt(88000000 + j));
                itemValues.put(RouteSheetItemContract.COLUMN_MECHANISM_TYPE, "Меркурий");
                itemValues.put(RouteSheetItemContract.COLUMN_LOCATION, "ул. Ставропольская 22 кв. " + j);
                int zone_count = random.nextInt(3) + 1;
                itemValues.put(RouteSheetItemContract.COLUMN_MECHANISM_ZONE_COUNT, zone_count);
                itemValues.put(RouteSheetItemContract.COLUMN_MECHANISM_RATIO, 1);

                int caracity_after = random.nextInt(4);
                itemValues.put(RouteSheetItemContract.COLUMN_MECHANISM_CAPACITY_AFTER, caracity_after);
                itemValues.put(RouteSheetItemContract.COLUMN_FITTING_POSITION, "в квартире");

                Calendar instance = Calendar.getInstance();
                instance.set(2019, 0, 1);

                itemValues.put(RouteSheetItemContract.COLUMN_PREV_READING_DT, persistDate(instance.getTime()));

                instance.set(2000, 0, 1);
                itemValues.put(RouteSheetItemContract.COLUMN_MECHANISM_INSTALL_DT, persistDate(instance.getTime()));

                instance.set(2010, 0, 1);
                itemValues.put(RouteSheetItemContract.COLUMN_MECHANISM_CHECK_DT, persistDate(instance.getTime()));

                itemValues.put(RouteSheetItemContract.COLUMN_PREV_ZONE1_VALUE, random.nextInt(10000) + 0.0);
                if (zone_count == 2) {
                    itemValues.put(RouteSheetItemContract.COLUMN_PREV_ZONE2_VALUE, random.nextInt(10000) + 0.0);
                } else if (zone_count == 3) {
                    itemValues.put(RouteSheetItemContract.COLUMN_PREV_ZONE2_VALUE, random.nextInt(1000) + 0.0);
                    itemValues.put(RouteSheetItemContract.COLUMN_PREV_ZONE3_VALUE, random.nextInt(500) + 0.0);
                }

                itemValues.put(RouteSheetItemContract.COLUMN_MECHANISM_SEAL, "738929СК");
                itemValues.put(RouteSheetItemContract.COLUMN_SORT_INX, j);
                itemValues.put(RouteSheetItemContract.COLUMN_AVERAGE, random.nextInt(300) + 0.0);
                itemValues.put(RouteSheetItemContract.COLUMN_DATA_STATE, RouteSheetItemDataStates.None.index());
                long insertResult = db.insert(RouteSheetItemContract.TABLE_NAME, null, itemValues);
                Log.i("insert_item", insertResult + "");
            }
        }
    }

    /* Преобразовывает дату в long*/
    private Long persistDate(Date date) {
        return date != null
                ? date.getTime()
                : null;
    }

    /* Возвращает дату*/
    private Date getDate(Cursor cursor, String colName) {
        int index = cursor.getColumnIndex(colName);
        return cursor.isNull(index)
                ? null
                : new Date(cursor.getLong(index));
    }

    /* Возвращает строку */
    private String getString(Cursor cursor, String colName) {
        int index = cursor.getColumnIndex(colName);
        return cursor.isNull(index)
                ? null
                : cursor.getString(index);
    }

    /* Возвращает число*/
    private Double getDouble(Cursor cursor, String colName) {
        int index = cursor.getColumnIndex(colName);
        return cursor.isNull(index)
                ? null
                : cursor.getDouble(index);
    }

    /* get int*/
    private int getInt(Cursor cursor, String colName) {
        int index = cursor.getColumnIndex(colName);
        return cursor.isNull(index)
                ? 0
                : Integer.parseInt(cursor.getString(index));
    }
}

/* Контракт данных маршрутного листа*/
final class RouteSheetContract {
    /*Наименование таблицы*/
    static final String TABLE_NAME = "route_sheets";     // список маршрутных листов
    /*Уникальный идентификатор маршрутного листа*/
    static final String COLUMN_ID = "id";
    /*Наименование*/
    static final String COLUMN_NAME = "name";
    /*Скрипт на создании таблицы*/
    static final String CREATE_SCRIPT = "CREATE TABLE " + TABLE_NAME + "(\n" +
            "  " + COLUMN_ID + " TEXT(128) PRIMARY KEY NOT NULL, \n" +  //
            "  " + COLUMN_NAME + " TEXT NOT NULL);";

    private RouteSheetContract() {
    }
}

/* Элемент маршрутного листа */
final class RouteSheetItemContract {
    /*Наименование таблицы*/
    static final String TABLE_NAME = "route_sheet_items";     // список маршрутных листов
    static final String COLUMN_ID = "id";
    static final String COLUMN_HEADER_ID = "route_sheet_id";
    static final String COLUMN_DOC_TYPE = "doc_type";
    static final String COLUMN_DOC_NUMBER = "doc_number";
    static final String COLUMN_CONSUMER = "consumer";
    static final String COLUMN_CONSUMER_PHONE = "consumerPhone";
    static final String COLUMN_LOCATION = "location";
    static final String COLUMN_MECHANISM_NUMBER = "mechanism_number";
    static final String COLUMN_MECHANISM_TYPE = "mechanism_type";
    static final String COLUMN_MECHANISM_ZONE_COUNT = "mechanism_zone_count";
    static final String COLUMN_MECHANISM_INSTALL_DT = "mechanism_install_dt";
    static final String COLUMN_MECHANISM_CHECK_DT = "mechanism_check_dt";
    static final String COLUMN_MECHANISM_SEAL = "mechanism_seal";
    static final String COLUMN_FITTING_POSITION = "fitting_pos";
    static final String COLUMN_MECHANISM_RATIO = "mechanism_ratio";
    static final String COLUMN_PREV_READING_DT = "prev_reading_dt";
    static final String COLUMN_CURRENT_READING_DT = "current_reading_dt";
    static final String COLUMN_PREV_ZONE1_VALUE = "prev_zone1_value";
    static final String COLUMN_PREV_ZONE2_VALUE = "prev_zone2_value";
    static final String COLUMN_PREV_ZONE3_VALUE = "prev_zone3_value";
    static final String COLUMN_CURRENT_ZONE1_VALUE = "current_zone1_value";
    static final String COLUMN_CURRENT_ZONE2_VALUE = "current_zone2_value";
    static final String COLUMN_CURRENT_ZONE3_VALUE = "current_zone3_value";
    static final String COLUMN_SORT_INX = "sort_inx";
    static final String COLUMN_MECHANISM_CAPACITY_AFTER = "m_capacity_after";
    static final String COLUMN_DATA_STATE = "data_state";
    static final String COLUMN_COMMENT = "comment";
    static final String COLUMN_ANOMALY = "anomaly";
    static final String COLUMN_AVERAGE = "average";
    static final String COLUMN_GEO = "geo_position";
    static final String COLUMN_ZONE1_PHOTO = "zone1_photo";
    static final String COLUMN_ZONE2_PHOTO = "zone2_photo";
    static final String COLUMN_ZONE3_PHOTO = "zone3_photo";

    /*Скрипт на создание таблицы*/
    static final String CREATE_SCRIPT = "CREATE TABLE " + TABLE_NAME + "(\n"
            + COLUMN_ID + " TEXT(128) PRIMARY KEY NOT NULL, \n"
            + COLUMN_HEADER_ID + " TEXT NOT NULL, \n"
            + COLUMN_DOC_TYPE + " TEXT(20) NOT NULL, \n"
            + COLUMN_DOC_NUMBER + " TEXT, \n"
            + COLUMN_CONSUMER + " TEXT, \n"
            + COLUMN_CONSUMER_PHONE + " TEXT(200), \n"
            + COLUMN_LOCATION + " TEXT, \n"
            + COLUMN_MECHANISM_NUMBER + " TEXT, \n"
            + COLUMN_MECHANISM_TYPE + " TEXT, \n"
            + COLUMN_MECHANISM_INSTALL_DT + " NUMERIC, \n"
            + COLUMN_MECHANISM_CHECK_DT + " NUMERIC, \n"
            + COLUMN_MECHANISM_ZONE_COUNT + " INT, \n"
            + COLUMN_MECHANISM_SEAL + " TEXT, \n"
            + COLUMN_FITTING_POSITION + " TEXT, \n"
            + COLUMN_MECHANISM_RATIO + " NUMBER, \n"
            + COLUMN_MECHANISM_CAPACITY_AFTER + " NUMBER, \n"
            + COLUMN_PREV_READING_DT + " NUMERIC, \n"
            + COLUMN_CURRENT_READING_DT + " NUMERIC, \n"
            + COLUMN_PREV_ZONE1_VALUE + " REAL, \n"
            + COLUMN_PREV_ZONE2_VALUE + " REAL, \n"
            + COLUMN_PREV_ZONE3_VALUE + " REAL, \n"
            + COLUMN_CURRENT_ZONE1_VALUE + " REAL, \n"
            + COLUMN_CURRENT_ZONE2_VALUE + " REAL, \n"
            + COLUMN_CURRENT_ZONE3_VALUE + " REAL, \n"
            + COLUMN_SORT_INX + " INT NOT NULL, \n"
            + COLUMN_DATA_STATE + " NUMERIC NOT NULL, \n"
            + COLUMN_AVERAGE + " NUMERIC, \n"
            + COLUMN_COMMENT + " TEXT(200), \n"
            + COLUMN_ANOMALY + " TEXT, \n"
            + COLUMN_ZONE1_PHOTO + " TEXT, \n"
            + COLUMN_ZONE2_PHOTO + " TEXT, \n"
            + COLUMN_ZONE3_PHOTO + " TEXT, \n"
            + COLUMN_GEO + " TEXT(50));";

    private RouteSheetItemContract() {
    }
}

/* Состояния данных*/
enum RouteSheetItemDataStates {
    None(0), // данных нет - ни показаний, ни примечаний
    Modify(1), // данные есть - или показания или примечание
    Sync(2); // данные есть и равны данным сервера

    private final int index;

    RouteSheetItemDataStates(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}