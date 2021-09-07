package www.info_pro.ru;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.Credentials;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import www.info_pro.ru.infrastructure.AppDialogMenuAdapter;
import www.info_pro.ru.infrastructure.AppDialogMenuItem;
import www.info_pro.ru.infrastructure.AppSettingController;
import www.info_pro.ru.infrastructure.Constants;
import www.info_pro.ru.infrastructure.GpsCoordinateFormatter;
import www.info_pro.ru.infrastructure.PhotoFileWrapper;

/**
 * Основное окно приложения.
 */
public class MainActivity extends AppCompatActivity {

    /* Кнопка фото зоны 1*/
    private ImageButton _btn_photo_1;

    /* Кнопка фото зоны 2*/
    private ImageButton _btn_photo_2;

    /* Кнопка фото зоны 3*/
    private ImageButton _btn_photo_3;

    /* Текущия фотография */
    private PhotoFileWrapper _currentPhotoFile;

    /* меню приложения */
    private BottomNavigationView _menu;

    /* хост вкладок */
    private TabHost _tabsHost;

    /* Всплывающие сообщения*/
    private AlertMessages _alertMessages;

    /* Диалог с прогрессом*/
    private AlertDialog _progressDialog;

    /* Компонент списка обходных листов*/
    private ExpandableListView _routeSheetListView;

    /* Компонент для фильтрации обходных листов*/
    private EditText _teFilter;

    /* Местоположение */
    private TextView _tvLocation;

    /* Текущее местоположение */
    private Location _currentLocation;

    /* Адаптер данных листа*/
    private DocExpandableListAdapter _expandableListAdapter;

    /* Выбранный документ*/
    private RouteSheetItemModel _selectedRouteSheetItem;

    /* Обработчик базы данных */
    private DatabaseHandler _databaseHandler;

    /* Сервис для получения местоположений */
    private LocationManager _locationManager;
    private final LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            _currentLocation = location;
            MainActivity.this.onLocationChanged();
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        boolean isTablet = (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
        this.setRequestedOrientation(isTablet ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        this.setContentView(R.layout.activity_main);

        this._databaseHandler = new DatabaseHandler(this);
        this._tvLocation = this.findViewById(R.id.geo_location);
        this._locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        this._alertMessages = new AlertMessages(this);
        this._progressDialog = this._alertMessages.createProgressDialog();
        this._btn_photo_1 = this.findViewById(R.id.btn_photo_1);
        this._btn_photo_2 = this.findViewById(R.id.btn_photo_2);
        this._btn_photo_3 = this.findViewById(R.id.btn_photo_3);

        this.configureBottomMenu();
        this.configureTabsHost();
        this.configureSearchControl();
        this.configureActionBar();
        this.configureDocList();
    }

    /* onResume */
    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Отсутсвуют разрешения на получения данных о местоположении!", Toast.LENGTH_SHORT).show();
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //  requestPermissions(new String[]{ACCESS_FINE_LOCATION},1);
            // }
        } else {
            int minTime = 1000; // минимальное время обновления в мс
            int minDistance = 1; // минимальная дистанция обновления в метрах
            this._locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, locationListener);
            this._locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, locationListener);
        }
    }

    /* onPause */
    @Override
    protected void onPause() {
        super.onPause();
        this._locationManager.removeUpdates(locationListener);
    }

    /*onBackPressed*/
    @Override
    public void onBackPressed() {
        int currentTab = this._tabsHost.getCurrentTab();
        if (currentTab == Constants.MAIN_TAB_DETAIL) {
            this.closeDetailTab();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_PHOTO) {
            PhotoFileWrapper file = this._currentPhotoFile;
            this._currentPhotoFile = null;
            if (file == null) {
                return;
            }

            String recognizedValueStr = data.getStringExtra(Constants.VALUE_WITH_PHOTO);
            if(_selectedRouteSheetItem.MechanismCapacityAfter > 0){
                if(recognizedValueStr!=null && recognizedValueStr.length() > _selectedRouteSheetItem.MechanismCapacityAfter){
                    int c = recognizedValueStr.length()- _selectedRouteSheetItem.MechanismCapacityAfter;
                    recognizedValueStr = recognizedValueStr.substring(0,c) + "." + recognizedValueStr.substring(c);
                }
            }

            if (resultCode == RESULT_OK) {
                int zoneId = file.getObjCode();
                String path = file.getAbsolutePath();
                if (zoneId == Constants.ZONE1_CODE && this._btn_photo_1 != null) {
                    this._btn_photo_1.setTag(path);
                        EditText etValue = findViewById(R.id.current_zone1_value);
                        etValue.setText(recognizedValueStr);
                }

                if (zoneId == Constants.ZONE2_CODE && this._btn_photo_2 != null) {
                    this._btn_photo_2.setTag(path);

                        EditText etValue = findViewById(R.id.current_zone2_value);
                        etValue.setText(recognizedValueStr);
                }

                if (zoneId == Constants.ZONE3_CODE && this._btn_photo_3 != null) {
                    this._btn_photo_3.setTag(path);
                        EditText etValue = findViewById(R.id.current_zone3_value);
                        etValue.setText(recognizedValueStr);
                }
            } else {
                file.delete();
                this.clearBtnPath(file.getObjCode());
            }
            this.refreshPhotoButtons();
        }
    }

    /* Фотограция для зоны 1*/
    public void zone1PhotoClick(View view) {
        this.photoClick(this._btn_photo_1, Constants.ZONE1_CODE);
    }

    /* Фотограция для зоны 2*/
    public void zone2PhotoClick(View view) {
        this.photoClick(this._btn_photo_2, Constants.ZONE2_CODE);
    }

    /* Фотограция для зоны 3*/
    public void zone3PhotoClick(View view) {
        this.photoClick(this._btn_photo_3, Constants.ZONE3_CODE);
    }

    /* Вызывается при нажатии на координаты */
    public void gpsLblClick(View view) {
        if (this._currentLocation == null) {
            return;
        }

        int currentTab = this._tabsHost.getCurrentTab();
        if (currentTab != Constants.MAIN_TAB_LIST) {
            return;
        }

        this._teFilter.setText(Constants.GPS_SEARCH_COMMAND);
        Toast.makeText(this, "Включен режим автопоиска по GPS!", Toast.LENGTH_SHORT).show();
    }

    /* Вызывается при клике на кнопку "Подробно"*/
    private void openConsumerInfo() {
        if (this._selectedRouteSheetItem == null) {
            Toast.makeText(getApplicationContext(), "Выберите документ для ввода показаний", Toast.LENGTH_SHORT).show();
            return;
        }

        ConsumerDetailedDialogBuilder builder = new ConsumerDetailedDialogBuilder(this);
        builder.Build(this._selectedRouteSheetItem).show();
    }

    /* Обработка клика на создание фотограции*/
    private void photoClick(ImageButton btn, int zoneCode) {
        String path = this.getPath(btn);
        if (path != null) {
            PhotoFileWrapper photoFile = new PhotoFileWrapper(this, path, zoneCode);
            this.openPhotoMenu(photoFile);
        } else {
            try {
                PhotoFileWrapper photoFile = new PhotoFileWrapper(this, zoneCode);
                this.openCamera(photoFile);
            } catch (IOException e) {
                e.printStackTrace();
                this._alertMessages.showException(e, "Ошибка запуска камеры: ");
            }
        }
    }

    /* Открытие  меню снимка ПУ */
    private void openPhotoMenu(PhotoFileWrapper photoFile) {
        this._currentPhotoFile = photoFile;
        final AppDialogMenuItem[] items = {
                new AppDialogMenuItem("Просмотреть", R.drawable.preview),
                new AppDialogMenuItem("Заменить", R.drawable.replace),
                new AppDialogMenuItem("Удалить", R.drawable.delete)
        };

        AppDialogMenuAdapter appDialogMenuAdapter = new AppDialogMenuAdapter(this, items);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Меню снимка ПУ");
        builder.setAdapter(appDialogMenuAdapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (_currentPhotoFile == null) {
                    _alertMessages.showErrorMsg("Не удалось получить файл изображения!");
                    return;
                }

                if (item == 0) { // просмотр
                    openPreviewPhoto(_currentPhotoFile);
                } else if (item == 1) { // замена
                    replacePhotoMenuClick();
                } else if (item == 2) { // удаление
                    deleteFileMenuClick();
                }
            }
        });
        builder.show();
    }

    /* Открыть камеру */
    private void openCamera(PhotoFileWrapper photoFile) {
        Intent cameraIntent = new Intent(this, CameraActivity.class);
        this._currentPhotoFile = photoFile;
        String uri = this._currentPhotoFile.getAbsolutePath();
        cameraIntent.putExtra("CameraPhotoPath", uri);
        this.startActivityForResult(cameraIntent, Constants.REQUEST_PHOTO);

/* Вызов стандартной камеры
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            this._currentPhotoFile = photoFile;
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, this._currentPhotoFile.getUri());
            this.startActivityForResult(takePictureIntent, Constants.REQUEST_PHOTO);
        } else {
            this._alertMessages.showWarningMsg("Не найдено системное приложение для работы с камерой!");
        }*/
    }

    /*Открыть фотографию на предварительный просмотр*/
    private void openPreviewPhoto(PhotoFileWrapper photoFile) {
        try {
            Intent previewIntent = new Intent(Intent.ACTION_VIEW, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            previewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            previewIntent.setDataAndType(photoFile.getUri(), "image/*");
            if (previewIntent.resolveActivity(getPackageManager()) != null) {
                this.startActivity(previewIntent);
            } else {
                this._alertMessages.showWarningMsg("Не найдено системное приложение для просмотра изображений!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            this._alertMessages.showException(e, "Ошибка просмотра изображения: ");
        }
    }

    /*Замена фотограции */
    private void replacePhotoMenuClick() {
        try {
            int objCode = this._currentPhotoFile.getObjCode();
            PhotoFileWrapper newPhoto = new PhotoFileWrapper(getApplicationContext(), objCode);
            openCamera(newPhoto);
        } catch (IOException e) {
            e.printStackTrace();
            this._alertMessages.showException(e, "Ошибка создания файла для сохранения фотографии: ");
        }
    }

    /* Удаление файла по нажатию на пункт меню*/
    private void deleteFileMenuClick() {
        AlertDialog.Builder confirmBuilder = this._alertMessages.getConfirmBuilder("Вы действительно хотите удалить фотографию ПУ?");
        confirmBuilder.setIcon(R.drawable.warning_icon);
        confirmBuilder.setPositiveButton("ДА",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // обнулим тэг, файл должен удалить после нажатия на сохранить
                        clearBtnPath(_currentPhotoFile.getObjCode());
                        // обновим иконки на кнопках
                        refreshPhotoButtons();
                    }
                });
        confirmBuilder.create().show();
    }

    /* Конфигурирование меню навигации */
    private void configureBottomMenu() {
        this._menu = findViewById(R.id.main_bottom_menu);
        this._menu.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                CharSequence title = menuItem.getTitle();
                if (title == Constants.MENU_LIST_NAME) {
                    closeDetailTab();
                    return true;
                }

                if (title == Constants.MENU_DOWNLOAD_NAME) {
                    startUpdateRouteSheets();
                    return true;
                }

                if (title == Constants.MENU_UPLOAD_NAME) {
                    startSyncServerReadings();
                    return true;
                }

                if (title == Constants.MENU_SETTING_NAME) {
                    openSettingActivity();
                    return true;
                }

                if (title == Constants.MENU_INFO_NAME) {
                    openConsumerInfo();
                    return true;
                }

                if (title == Constants.MENU_SAVE_NAME) {
                    applyChanged();
                    return true;
                }

                if (title == Constants.MENU_ABOUT_NAME) {
                    openAboutActivity();
                    return true;
                }

                return false;
            }
        });
    }

    /* конфигурирование вкладок */
    private void configureTabsHost() {
        this._tabsHost = findViewById(android.R.id.tabhost);
        this._tabsHost.setup();

        TabHost.TabSpec tab1 = this._tabsHost.newTabSpec("Список абонентов");
        tab1.setIndicator("Список ЛС");
        tab1.setContent(R.id.tab1);
        this._tabsHost.addTab(tab1);

        TabHost.TabSpec tab2 = this._tabsHost.newTabSpec("Показания абонента");
        tab2.setIndicator("Показания");
        tab2.setContent(R.id.tab2);
        this._tabsHost.addTab(tab2);

        TabWidget tabWidget = this._tabsHost.getTabWidget();
        tabWidget.setVisibility(View.GONE);
        this._tabsHost.setCurrentTab(Constants.MAIN_TAB_LIST);
        this.menuItemsConfigure();
    }

    /* Конфигурирование ActionBar */
    private void configureActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
    }


    /* Обновление координат GPS*/
    private void onLocationChanged() {
        try {
            GpsCoordinateFormatter gpsCoordinateFormatter = new GpsCoordinateFormatter();
            String lbl = gpsCoordinateFormatter.toInDegree(this._currentLocation);
            if (lbl == null) {
                this._tvLocation.setText(getString(R.string.geo_lbl_empty_value));
            } else {
                this._tvLocation.setText(lbl);
            }
        } catch (Exception e) {
            e.printStackTrace();
            this._tvLocation.setText(getString(R.string.geo_lbl_empty_value));
        }

        String findText = this._teFilter.getText().toString();
        if (Constants.GPS_SEARCH_COMMAND.equals(findText.toLowerCase().trim())) { // обновим если указаны координаты gps
            //Toast.makeText(this, "GPS: Обновление ...", Toast.LENGTH_SHORT).show();
            this.onFilterChanged(findText);
        }
    }

    /* Конфигурирование визуального отображения маршрутных листов */
    private void configureDocList() {
        this._routeSheetListView = findViewById(R.id.docs_list);
        if (this._routeSheetListView == null) {
            return;
        }

        ArrayList<RouteSheetModel> groups = this._databaseHandler.GetAllRouteSheets();
        this._expandableListAdapter = new DocExpandableListAdapter(this, groups);
        this._routeSheetListView.setAdapter(this._expandableListAdapter);

        if (groups.size() > 0) {
            this._routeSheetListView.expandGroup(0);
        }

        this._routeSheetListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                if (_expandableListAdapter == null) {
                    Toast.makeText(getApplicationContext(), "Адаптер данных не создан!", Toast.LENGTH_SHORT).show();
                    return false;
                }

                int allGroupCount = _expandableListAdapter.getGroupCount();
                if (allGroupCount == 0 || allGroupCount < groupPosition) {
                    return false;
                } else {
                    RouteSheetItemModel selectedItem = (RouteSheetItemModel) _expandableListAdapter.getChild(groupPosition, childPosition);
                    if (selectedItem == null) {
                        return false;
                    }

                    setSelectedRouteSheetItem(groupPosition, childPosition);
                    openInputReadingActivity();
                }
                return true;
            }
        });

        this.clearRouteSheetItemFilter();
    }

    /* закрыть вкладку с детализацией*/
    private void closeDetailTab() {
        this._tabsHost.setCurrentTab(Constants.MAIN_TAB_LIST);
        this.menuItemsConfigure();
    }

    /* Открыть форму Настройки*/
    private void openSettingActivity() {
        Intent intent = new Intent(this, SettingActivity.class);
        this.startActivity(intent);
    }

    /* Применение введенных данных */
    private void applyChanged() {
        RouteSheetItemModel routeSheetItem = this._selectedRouteSheetItem;
        if (routeSheetItem == null) {
            Toast.makeText(getApplicationContext(), "Выберите документ для сохранения показаний", Toast.LENGTH_SHORT).show();
            return;
        }

        int zoneCount = routeSheetItem.MechanismZoneCount;
        final NewValue nv = new NewValue();

        nv.CurrentZone1Value = this.getDouble(R.id.current_zone1_value);
        nv.CurrentZone2Value = this.getDouble(R.id.current_zone2_value);
        nv.CurrentZone3Value = this.getDouble(R.id.current_zone3_value);
        nv.Zone1_photoPath = this.getPath(this._btn_photo_1);
        nv.Zone2_photoPath = this.getPath(this._btn_photo_2);
        nv.Zone3_photoPath = this.getPath(this._btn_photo_3);
        nv.Comment = ((EditText) findViewById(R.id.comment_value)).getText().toString();

        boolean isReadingDataChanged = false; // изменились показания
        if (!routeSheetItem.equalsZone1Value(nv.CurrentZone1Value) || !routeSheetItem.equalsZone1Photo(nv.Zone1_photoPath)) {
            isReadingDataChanged = true;
        }

        if (!isReadingDataChanged && zoneCount >= 2
                && (!routeSheetItem.equalsZone2Value(nv.CurrentZone2Value)) || !routeSheetItem.equalsZone2Photo(nv.Zone2_photoPath)) {
            isReadingDataChanged = true;
        }

        if (!isReadingDataChanged && zoneCount == 3
                && (!routeSheetItem.equalsZone3Value(nv.CurrentZone3Value) || !routeSheetItem.equalsZone3Photo(nv.Zone3_photoPath))) {
            isReadingDataChanged = true;
        }

        boolean isCommentsDataChanged = !routeSheetItem.equalsComment(nv.Comment); // изменились комментарии
        if (!isReadingDataChanged && !isCommentsDataChanged) { // ничего не изменилось
            this._alertMessages.showWarningMsg("Данные не изменились. Сохранение отменено!");
            return;
        }

        // флаг наличия показаний. True - показания добавляются/ изменяются, false - показания удаляются
        boolean isExistReadings = nv.isExistReadings();
        // флаг указывающий что пользователь удаляет данные
        boolean isRemoveData = !isExistReadings && nv.Comment.isEmpty();

        int photoCount = nv.getPhotoCount();
        ArrayList<String> warningList = new ArrayList<>();
        if (isExistReadings) {
            // проверки обязательно заполненных полей
            if (nv.CurrentZone1Value == null) {
                this._alertMessages.showErrorMsg("Введите показания по тарифу 1");
                return;
            }

            if (zoneCount >= 2 && nv.CurrentZone2Value == null) {
                this._alertMessages.showErrorMsg("Введите показания по тарифу 2");
                return;
            }

            if (zoneCount >= 3 && nv.CurrentZone3Value == null) {
                this._alertMessages.showErrorMsg("Введите показания по тарифу 3");
                return;
            }

            // проверки для генерации предупреждений
            if (routeSheetItem.isLessZone1Value(nv.CurrentZone1Value)) {
                warningList.add("- Текущие значения по тарифу 1 меньше предыдущих!");
            }

            if (zoneCount >= 2 && routeSheetItem.isLessZone2Value(nv.CurrentZone2Value)) {
                warningList.add("- Текущие значения по тарифу 2 меньше предыдущих!");
            }

            if (zoneCount >= 3 && routeSheetItem.isLessZone3Value(nv.CurrentZone3Value)) {
                warningList.add("- Текущие значения по тарифу 3 меньше предыдущих!");
            }

            if (routeSheetItem.isTwiceTheAverage(nv)) {
                warningList.add("- Расход больше среднего в два раза!");
            }

            if (photoCount != 0 && photoCount != zoneCount) {
                warningList.add("- Кол-во фотографий не соответствует кол-ву тарифов!");
            }
        } else if (photoCount != 0 && nv.Comment.isEmpty()) { // если фотограции есть, но нет показаний и примечания
            this._alertMessages.showErrorMsg("Запрещено сохранять фотограции без показаний или примечания!");
            return;
        }

        // установка даты ввода в зависимости от наличия показаний
        if (isExistReadings) {
            nv.CurrentReadingDt = Calendar.getInstance().getTime();
        }

        Location currentLocation = this._currentLocation;
        if (currentLocation != null) {
            nv.setLocation(currentLocation);
        }

        // установка состояния строки данных
        RouteSheetItemDataStates oldDataState = routeSheetItem.DataState;
        nv.DataState = oldDataState == RouteSheetItemDataStates.Modify && isRemoveData
                ? RouteSheetItemDataStates.None
                : RouteSheetItemDataStates.Modify;

        if (warningList.size() != 0) {
            StringBuilder warningMsgBuilder = new StringBuilder();
            for (String msg : warningList) {
                warningMsgBuilder.append(msg).append("\n");
            }

            String warningMsg = warningMsgBuilder.toString();
            nv.Anomaly = warningMsg;

            AlertDialog.Builder confirmBuilder = this._alertMessages.getConfirmBuilder(warningMsg + "Продолжить сохранение?");
            confirmBuilder.setIcon(R.drawable.warning_icon);
            confirmBuilder.setPositiveButton("ДА",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            saveChanged(nv);
                        }
                    });
            AlertDialog dialog = confirmBuilder.create();
            dialog.show();
        } else {
            this.saveChanged(nv);
        }
    }

    /*Сохранение данных в базу */
    private void saveChanged(NewValue nv) {
        this._selectedRouteSheetItem.MapNewValue(nv);
        Boolean result = this._databaseHandler.updateRouteSheetItem(this._selectedRouteSheetItem);
        if (!result) {
            Toast.makeText(getApplicationContext(), "НЕ УДАЛОСЬ сохранить данные в локальную БД", Toast.LENGTH_SHORT).show();
            return;
        }
        this._expandableListAdapter.notifyDataSetChanged();
        this.closeDetailTab();
    }

    /* Окрывает форму ввода показаний*/
    private void openInputReadingActivity() {
        this._tabsHost.setCurrentTab(Constants.MAIN_TAB_DETAIL);
        this.menuItemsConfigure();
        this.refreshConsumerBlock();
    }

    /* Открыть форму О приложениии*/
    private void openAboutActivity() {
        Intent intent = new Intent(this, AboutActivity.class);
        this.startActivity(intent);
    }

    /* Конфигурирование меню приложения */
    private void menuItemsConfigure() {
        Menu menu = _menu.getMenu();
        MenuItem item1 = menu.findItem(R.id.main_menu_action_1);
        MenuItem item2 = menu.findItem(R.id.main_menu_action_2);
        MenuItem item3 = menu.findItem(R.id.main_menu_action_3);
        MenuItem item4 = menu.findItem(R.id.main_menu_action_4);

        int currentTab = _tabsHost.getCurrentTab();
        if (currentTab == Constants.MAIN_TAB_LIST) {
            item1.setVisible(true);
            item2.setVisible(true);
            item3.setVisible(true);
            item4.setVisible(true);
            item1.setTitle(Constants.MENU_DOWNLOAD_NAME);
            item2.setTitle(Constants.MENU_UPLOAD_NAME);
            item3.setTitle(Constants.MENU_SETTING_NAME);
            item4.setTitle(Constants.MENU_ABOUT_NAME);
            item1.setIcon(R.drawable.download);
            item2.setIcon(R.drawable.upload);
            item3.setIcon(R.drawable.settings);
            item4.setIcon(R.drawable.about);
        } else if (currentTab == Constants.MAIN_TAB_DETAIL) {
            item1.setVisible(true);
            item2.setVisible(true);
            item3.setVisible(true);
            item4.setVisible(false);
            item1.setTitle(Constants.MENU_LIST_NAME);
            item2.setTitle(Constants.MENU_INFO_NAME);
            item3.setTitle(Constants.MENU_SAVE_NAME);
            item1.setIcon(R.drawable.folder);
            item2.setIcon(R.drawable.info);
            item3.setIcon(R.drawable.save);
        }
    }

    /* Конфигурирование панели поиска*/
    private void configureSearchControl() {
        this._teFilter = findViewById(R.id.document_search);
        this._teFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String findText = _teFilter.getText().toString();
                onFilterChanged(findText);
            }
        });
    }

    /* Вызываетсяпри изменении значения фильтра*/
    private void onFilterChanged(String findText) {
        if (Constants.GPS_SEARCH_COMMAND.equals(findText.toLowerCase().trim())) { // если команда на поиск по GPS
            this._expandableListAdapter.filterByGps(this._currentLocation, 300);
        } else {
            this._expandableListAdapter.filterByData(findText);
        }
        this.expandAllRouteSheets();
    }

    /* Установка элемента маршрутного листа в качестве активного*/
    private void setSelectedRouteSheetItem(int groupInx, int childInx) {
        int allGroupCount = _expandableListAdapter.getGroupCount();
        if (allGroupCount == 0 || allGroupCount < groupInx) {
            this._selectedRouteSheetItem = null;
        } else {
            this._selectedRouteSheetItem = (RouteSheetItemModel) _expandableListAdapter.getChild(groupInx, childInx);
        }
    }

    /* Обновление информации о выбранном потребителе*/
    private void refreshConsumerBlock() {
        TextView tvDocNumber = findViewById(R.id.lbl_document_number);
        TextView tvConsumerName = findViewById(R.id.lbl_consumer_name);
        ImageView imgSyncType = findViewById(R.id.img_sync_type);

        if (this._selectedRouteSheetItem != null) {
            String numberStr = "№" + this._selectedRouteSheetItem.DocumentNumber;
            tvDocNumber.setText(numberStr);
            tvConsumerName.setText(this._selectedRouteSheetItem.getConsumer());
            Drawable state = this._expandableListAdapter.getStateView(_selectedRouteSheetItem);
            imgSyncType.setImageDrawable(state);
        } else {
            String noDataText = "Нет данных";
            tvDocNumber.setText(noDataText);
            tvConsumerName.setText("");
        }

        this.refreshReadingBlock();
        EditText etComment = findViewById(R.id.comment_value);
        etComment.setText(this._selectedRouteSheetItem.getComment());
    }

    /* Обновление блока  показаний по выбранной строке*/
    private void refreshReadingBlock() {
        EditText etPrevDate = findViewById(R.id.prev_dt);
        EditText etPrevZone1Value = findViewById(R.id.prev_zone1_value);
        EditText etPrevZone2Value = findViewById(R.id.prev_zone2_value);
        EditText etPrevZone3Value = findViewById(R.id.prev_zone3_value);

        EditText etCurrentDate = findViewById(R.id.current_dt);
        EditText etCurrentZone1Value = findViewById(R.id.current_zone1_value);
        EditText etCurrentZone2Value = findViewById(R.id.current_zone2_value);
        EditText etCurrentZone3Value = findViewById(R.id.current_zone3_value);

        LinearLayout zone2Layout = findViewById(R.id.zone2_block);
        LinearLayout zone3Layout = findViewById(R.id.zone3_block);

        // сброс тегов
        this.setPath(this._btn_photo_1, null);
        this.setPath(this._btn_photo_2, null);
        this.setPath(this._btn_photo_3, null);

        // установка данных
        String emptyText = "";
        if (this._selectedRouteSheetItem != null) {
            Date prevDt = this._selectedRouteSheetItem.PrevReadingDt;
            Double prevZone1Value = this._selectedRouteSheetItem.PrevZone1Value;
            Double prevZone2Value = this._selectedRouteSheetItem.PrevZone2Value;
            Double prevZone3Value = this._selectedRouteSheetItem.PrevZone3Value;

            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ENGLISH);
            etPrevDate.setText(prevDt != null ? dateFormatter.format(prevDt) : emptyText);
            etPrevZone1Value.setText(prevZone1Value != null ? prevZone1Value.toString() : emptyText);
            etPrevZone2Value.setText(prevZone2Value != null ? prevZone2Value.toString() : emptyText);
            etPrevZone3Value.setText(prevZone3Value != null ? prevZone3Value.toString() : emptyText);

            Date currentDt = this._selectedRouteSheetItem.CurrentReadingDt;
            etCurrentDate.setText(currentDt != null ? dateFormatter.format(currentDt) : dateFormatter.format(Calendar.getInstance().getTime()));

            Double currentZone1Value = this._selectedRouteSheetItem.CurrentZone1Value;
            Double currentZone2Value = this._selectedRouteSheetItem.CurrentZone2Value;
            Double currentZone3Value = this._selectedRouteSheetItem.CurrentZone3Value;
            etCurrentZone1Value.setText(currentZone1Value != null ? currentZone1Value.toString() : "");
            etCurrentZone2Value.setText(currentZone2Value != null ? currentZone2Value.toString() : "");
            etCurrentZone3Value.setText(currentZone3Value != null ? currentZone3Value.toString() : "");

            this.setPath(this._btn_photo_1, this._selectedRouteSheetItem.Zone1_photoPath);
            this.setPath(this._btn_photo_2, this._selectedRouteSheetItem.Zone2_photoPath);
            this.setPath(this._btn_photo_3, this._selectedRouteSheetItem.Zone3_photoPath);

            if (this._selectedRouteSheetItem.MechanismZoneCount == 1) {
                zone2Layout.setVisibility(View.GONE);
                zone3Layout.setVisibility(View.GONE);
            } else if (this._selectedRouteSheetItem.MechanismZoneCount == 2) {
                zone2Layout.setVisibility(View.VISIBLE);
                zone3Layout.setVisibility(View.GONE);
            } else if (this._selectedRouteSheetItem.MechanismZoneCount == 3) {
                zone2Layout.setVisibility(View.VISIBLE);
                zone3Layout.setVisibility(View.VISIBLE);
            }
        } else {
            etPrevDate.setText(emptyText);
            etPrevZone1Value.setText(emptyText);
            etPrevZone2Value.setText(emptyText);
            etPrevZone3Value.setText(emptyText);

            etCurrentDate.setText(emptyText);
            etCurrentZone1Value.setText(emptyText);
            etCurrentZone2Value.setText(emptyText);
            etCurrentZone3Value.setText(emptyText);

            zone2Layout.setVisibility(View.VISIBLE);
            zone3Layout.setVisibility(View.VISIBLE);
        }

        this.refreshPhotoButtons();
    }

    /*Обновление кнопок для фото*/
    private void refreshPhotoButtons() {
        this.updatePhotoIcon(this._btn_photo_1);
        this.updatePhotoIcon(this._btn_photo_2);
        this.updatePhotoIcon(this._btn_photo_3);
    }

    /*Обновление иконки фотограции*/
    private void updatePhotoIcon(ImageButton btn) {
        String path = this.getPath(btn);
        btn.setImageResource(path != null ? R.drawable.done_photo : R.drawable.no_photo);
    }

    /* Очистка полей фильтра*/
    private void clearRouteSheetItemFilter() {
        if (this._teFilter != null) {
            this._teFilter.setText("");
        }
    }

    /*Раскрыть все группы документов*/
    private void expandAllRouteSheets() {
        int count = this._expandableListAdapter.getGroupCount();
        for (int i = 0; i < count; i++) {
            this._routeSheetListView.expandGroup(i);
        }
    }

    /* Обновление списка маршрутных листов*/
    private void startUpdateRouteSheets() {
        String message = "Данные ЗАКРЫТЫХ маршрутных листов будут УДАЛЕНЫ с вашего устройства!\n" +
                "Вы действительно хотите запустить АКТУАЛИЗАЦИЮ маршрутных листов?";
        AlertDialog.Builder builder = this._alertMessages.getConfirmBuilder(message);
        builder.setPositiveButton("ДА",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppSettingController settings = new AppSettingController(getApplicationContext());
                        String error = settings.validate();
                        if (error != null) {
                            _alertMessages.showErrorMsg("Получение данных невозможно, проверьте настройки приложения.\nОшибка: " + error);
                            return;
                        }

                        _progressDialog.show();
                        String basicToken = Credentials.basic(settings.getLogin(), settings.getPwd());
                        NetworkService.getInstance()
                                .getIntegrationApi(settings.getServerUrl())
                                .getRouteSheets(basicToken, settings.getBranchCode(), settings.getPIN())
                                .enqueue(new Callback<ArrayList<ExportRouteSheet>>() {
                                    @Override
                                    public void onResponse(Call<ArrayList<ExportRouteSheet>> call, Response<ArrayList<ExportRouteSheet>> response) {
                                        if (response.isSuccessful()) {
                                            ArrayList<ExportRouteSheet> routeSheets = response.body();
                                            if (routeSheets == null || routeSheets.size() == 0) {
                                                _alertMessages.showWarningMsg("Не удалось получить новые маршрутные листы. Возможно вам ничего не назначили!");

                                            } else {
                                                _databaseHandler.importRouteSheets(routeSheets);
                                                configureDocList();
                                                _alertMessages.showSuccessMsg("Маршрутные листы актуализированы! Можно отправляться в дорогу!");
                                            }
                                        } else {
                                            String errMsg = HttpResponse.createMsgByResponse(response);
                                            _alertMessages.showErrorMsg(errMsg);
                                        }
                                        _progressDialog.dismiss();
                                    }

                                    @Override
                                    public void onFailure(Call<ArrayList<ExportRouteSheet>> call, Throwable t) {
                                        _progressDialog.dismiss();
                                        _alertMessages.showException(t, "Ошибка получения маршрутных листов: ");
                                    }
                                });
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /* Синхронизация показаний с сервером*/
    private void startSyncServerReadings() {
        String message = "Вы действительно хотите синхронизировать показания с сервером?";
        AlertDialog.Builder builder = this._alertMessages.getConfirmBuilder(message);
        builder.setPositiveButton("ДА",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ArrayList<UpdateReadingDataModel> updateReadingItems = _databaseHandler.getUpdateReadingDataModels();
                        if (updateReadingItems.size() == 0) {
                            _alertMessages.showWarningMsg("Не удалось получить показания для синхронизации. Операция отменена!");
                            return;
                        }

                        AppSettingController settings = new AppSettingController(getApplicationContext());
                        String error = settings.validate();
                        if (error != null) {
                            _alertMessages.showErrorMsg("Синхронизация данных невозможна, проверьте настройки приложения.\nОшибка: " + error);
                            return;
                        }

                        _progressDialog.show();
                        String basicToken = Credentials.basic(settings.getLogin(), settings.getPwd());
                        NetworkService.getInstance()
                                .getIntegrationApi(settings.getServerUrl())
                                .updateItems(basicToken, settings.getBranchCode(), updateReadingItems)
                                .enqueue(new Callback<ArrayList<UpdateReadingResultModel>>() {
                                    @Override
                                    public void onResponse(Call<ArrayList<UpdateReadingResultModel>> call, Response<ArrayList<UpdateReadingResultModel>> response) {
                                        if (response.isSuccessful()) {
                                            ArrayList<UpdateReadingResultModel> syncResults = response.body();
                                            if (syncResults == null) {
                                                _alertMessages.showErrorMsg("Сервер не предоставил отчет о результатах синхронизации!");
                                            } else {
                                                updateSyncInfo(syncResults);

                                                int successCount = 0;
                                                ArrayList<String> errors = new ArrayList<>();
                                                for (UpdateReadingResultModel r : syncResults) {
                                                    if (!r.isError()) {
                                                        successCount++;
                                                        continue;
                                                    }
                                                    errors.add(r.getDescription());
                                                }

                                                String msg = "Синхронизация завершена! Успешно синхронизировано " + successCount + " показаний из " + syncResults.size();
                                                if (errors.size() > 0) {
                                                    msg = msg + "\nОшибки: " + errors.toString();
                                                }
                                                _alertMessages.showSuccessMsg(msg);
                                            }
                                        } else {
                                            _alertMessages.showErrorMsg(HttpResponse.createMsgByResponse(response));
                                        }
                                        _progressDialog.dismiss();
                                    }

                                    @Override
                                    public void onFailure(Call<ArrayList<UpdateReadingResultModel>> call, Throwable t) {
                                        _progressDialog.dismiss();
                                        _alertMessages.showException(t, "Ошибка синхронизации показаний");
                                    }
                                });
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /* Обновление информации о синхронизации показаний */
    private void updateSyncInfo(ArrayList<UpdateReadingResultModel> syncResults) {
        if (syncResults == null || syncResults.size() == 0) {
            return;
        }

        this._databaseHandler.updateSyncInfo(syncResults);
        this.configureDocList();
    }

    /* Получить double из editText*/
    private Double getDouble(int editTextId) {
        EditText editText = findViewById(editTextId);
        if (editText == null) {
            return null;
        }

        String txt = editText.getText().toString();
        return !txt.isEmpty()
                ? Double.parseDouble(txt)
                : null;
    }

    /*Установка пути к фотограции*/
    private void setPath(ImageButton btn, String path) {
        if (btn != null) {
            btn.setTag(path);
        }
    }

    /* Очистка пути к файлу из тэга кнопки*/
    private void clearBtnPath(int zoneCode) {
        if (zoneCode == Constants.ZONE1_CODE) {
            this.setPath(this._btn_photo_1, null);
        }

        if (zoneCode == Constants.ZONE2_CODE) {
            this.setPath(this._btn_photo_2, null);
        }

        if (zoneCode == Constants.ZONE3_CODE) {
            this.setPath(this._btn_photo_3, null);
        }
    }

    /* Возвращает путь к фотограции*/
    private String getPath(ImageButton btn) {
        Object tag = btn.getTag();
        if (tag == null) {
            return null;
        }

        return tag.toString();
    }
}

