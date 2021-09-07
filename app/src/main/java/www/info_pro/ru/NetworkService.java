package www.info_pro.ru;

import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Query;

/* Класс для взаимодействия с web api*/
class NetworkService {
    private static NetworkService mInstance;
    private Retrofit _mRetrofit;

    private NetworkService() {
    }

    /**/
    static NetworkService getInstance() {
        if (mInstance == null) {
            mInstance = new NetworkService();
        }
        return mInstance;
    }

    /**/
    IntegrationApi getIntegrationApi(String serverUrl) {
        if (_mRetrofit == null || !_mRetrofit.baseUrl().toString().equals(serverUrl)) {
            _mRetrofit = createRetrofit(serverUrl);
        }

        return _mRetrofit.create(IntegrationApi.class);
    }

    /**/
    private Retrofit createRetrofit(String serverUrl) {
        Gson gson = createGson();

        int timeout = 60;
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(serverUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    /**/
    private Gson createGson() {
        return new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .registerTypeHierarchyAdapter(byte[].class, new ByteArrayToBase64TypeAdapter())
                .create();
    }

    /* Описывает интеграцию */
    public interface IntegrationApi {
        @GET("api/v2/routesheets/actuals")
        Call<ArrayList<ExportRouteSheet>> getRouteSheets(@Header("Authorization") String credentials,
                                                         @Header("infnet_branch_code") String branchCode,
                                                         @Query("pin") String pin);

        @PUT("api/v2/routesheets/items")
        Call<ArrayList<UpdateReadingResultModel>> updateItems(@Header("Authorization") String credentials,
                                                              @Header("infnet_branch_code") String branchCode,
                                                              @Body ArrayList<UpdateReadingDataModel> updateItems);
    }

    // Using Android's base64 libraries. This can be replaced with any base64 library.
    private static class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
        public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Base64.decode(json.getAsString(), Base64.NO_WRAP);
        }

        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64.encodeToString(src, Base64.NO_WRAP));
        }
    }
}

class HttpResponse {
    private HttpResponse() {
    }

    /* Возвращает сообщение по http коду*/
    static String createMsgByResponse(Response response) {
        int statusCode = response.code();
        String originalMsg = "Код ошибки: " + statusCode;
        ResponseBody errorBody = response.errorBody();
        String error = errorBody == null ? "" : errorBody.toString();
        originalMsg = originalMsg + "\nОшибка: " + error;

        if (statusCode == 401) {
            return "Вы ввели неверный логин или пароль для доступа к сервису.";
        }

        if (statusCode == 400) {
            return "Сервер сервер не смог понять запрос из-за недействительного синтаксиса.\n" + originalMsg;
        }

        if (statusCode == 404) {
            return "Не удалось найти ресурс" + "\n" + originalMsg;
        }

        if (statusCode == 500) {
            return "Произошла внутренняя ошибка сервера.\n" + originalMsg;
        }

        return "Необработанная ошибка получения данных.\n" + originalMsg;
    }
}

/* Модель экспортируемых данных*/
class ExportRouteSheet {
    @SerializedName("id")
    @Expose
    private String id;

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("items")
    @Expose
    private ExportRouteSheetItem[] items;

    String getId() {
        return this.id;
    }

    String getName() {
        return this.name;
    }

    ExportRouteSheetItem[] getItems() {
        return this.items;
    }
}

/* Модель экспортируемых данных*/
class ExportRouteSheetItem {
    @SerializedName("id")
    @Expose
    private String id;

    @SerializedName("sortInx")
    @Expose
    private int sortInx;

    @SerializedName("documentType")
    @Expose
    private String docType;

    @SerializedName("documentNumber")
    @Expose
    private String docNumber;

    @SerializedName("consumer")
    @Expose
    private String consumer;

    @SerializedName("consumerPhone")
    @Expose
    private String consumerPhone;

    @SerializedName("location")
    @Expose
    private String location;

    @SerializedName("mechanismNumber")
    @Expose
    private String mechanismNumber;

    @SerializedName("mechanismType")
    @Expose
    private String mechanismType;

    @SerializedName("fittingPosition")
    @Expose
    private String fittingPosition;

    @SerializedName("mechanismInstallDt")
    @Expose
    private Date mechanismInstallDt;

    @SerializedName("mechanismLastVerifyDt")
    @Expose
    private Date mechanismLastVerifyDt;

    @SerializedName("mechanismZoneCount")
    @Expose
    private int mechanismZoneCount;

    @SerializedName("mechanismRatio")
    @Expose
    private int mechanismRatio;

    @SerializedName("seal")
    @Expose
    private String seal;

    @SerializedName("prevReadingDt")
    @Expose
    private Date prevReadingDt;

    @SerializedName("prevZone1Value")
    @Expose
    private Double prevZone1Value;

    @SerializedName("prevZone2Value")
    @Expose
    private Double prevZone2Value;

    @SerializedName("prevZone3Value")
    @Expose
    private Double prevZone3Value;

    @SerializedName("averageTotal")
    @Expose
    private Double averageTotal;

    @SerializedName("geo")
    @Expose
    private String geo;

    ExportRouteSheetItem() {
    }

    String getId() {
        return this.id;
    }

    int getSortInx() {
        return this.sortInx;
    }

    String getDocType() {
        return this.docType;
    }

    String getDocNumber() {
        return this.docNumber;
    }

    String getConsumer() {
        return this.consumer;
    }

    String getConsumerPhone() {
        return this.consumerPhone;
    }

    String getLocation() {
        return this.location;
    }

    String getMechanismNumber() {
        return this.mechanismNumber;
    }

    String getMechanismType() {
        return this.mechanismType;
    }

    Date getMechanismInstallDt() {
        return this.mechanismInstallDt;
    }

    Date getMechanismLastVerifyDt() {
        return this.mechanismLastVerifyDt;
    }

    int getMechanismZoneCount() {
        return this.mechanismZoneCount;
    }

    String getSeal() {
        return this.seal;
    }

    Date getPrevReadingDt() {
        return this.prevReadingDt;
    }

    Double getPrevZone1Value() {
        return this.prevZone1Value;
    }

    Double getPrevZone2Value() {
        return this.prevZone2Value;
    }

    Double getPrevZone3Value() {
        return this.prevZone3Value;
    }

    String getGeo() {
        return this.geo;
    }

    int getMechanismRatio() {
        return this.mechanismRatio;
    }

    Double getAverageTotal() {
        return this.averageTotal;
    }

    String getFittingPosition() {
        return this.fittingPosition;
    }
}

/* Модель отправки показаний на сервер */
class UpdateReadingDataModel {
    @SerializedName("routeSheetItemId")
    @Expose
    private String routeSheetItemId;

    @SerializedName("mechanismNumber")
    @Expose
    private String mechanismNumber;

    @SerializedName("readingDt")
    @Expose
    private Date readingDt;

    @SerializedName("zone1Value")
    @Expose
    private Double zone1Value;

    @SerializedName("zone2Value")
    @Expose
    private Double zone2Value;

    @SerializedName("zone3Value")
    @Expose
    private Double zone3Value;

    @SerializedName("photoZ1Name")
    @Expose
    private String photoZ1Name;

    @SerializedName("photoZ1")
    @Expose
    private byte[] photoZ1;

    @SerializedName("photoZ2Name")
    @Expose
    private String photoZ2Name;

    @SerializedName("photoZ2")
    @Expose
    private byte[] photoZ2;

    @SerializedName("photoZ3Name")
    @Expose
    private String photoZ3Name;

    @SerializedName("photoZ3")
    @Expose
    private byte[] photoZ3;

    @SerializedName("anomaly")
    @Expose
    private String anomaly;

    @SerializedName("comment")
    @Expose
    private String comment;

    @SerializedName("geo")
    @Expose
    private String geo;

    void setRouteSheetItemId(String id) {
        this.routeSheetItemId = id;
    }

    void setMechanismNumber(String number) {
        this.mechanismNumber = number;
    }

    void setReadingDt(Date dt) {
        this.readingDt = dt;
    }

    void setZone1Value(Double v) {
        this.zone1Value = v;
    }

    void setZone2Value(Double v) {
        this.zone2Value = v;
    }

    void setZone3Value(Double v) {
        this.zone3Value = v;
    }

    void setComment(String comment) {
        this.comment = comment;
    }

    /* Установка гео данных */
    void setGeo(String location) {
        this.geo = location;
    }

    /* Установка аномалии */
    void setAnomaly(String anomaly) {
        this.anomaly = anomaly;
    }

    /* Установка данных фотограции по 1 тарифу*/
    void setPhotoZ1Path(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }

        this.photoZ1Name = this.createPhotoName(1);
        this.photoZ1 = this.readContentIntoByteArray(new File(path));
    }

    /* Установка данных фотограции по 2 тарифу*/
    void setPhotoZ2Path(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }

        this.photoZ2Name = this.createPhotoName(2);
        this.photoZ2 = this.readContentIntoByteArray(new File(path));
    }

    /* Установка данных фотограции по 3 тарифу*/
    void setPhotoZ3Path(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }

        this.photoZ3Name = this.createPhotoName(3);
        this.photoZ3 = this.readContentIntoByteArray(new File(path));
    }

    /*Создает наименование фотограции */
    private String createPhotoName(int zoneNumber) {
        return this.mechanismNumber + "_тариф№" + zoneNumber + ".jpg";
    }

    /*Чтение файла*/
    private byte[] readContentIntoByteArray(File file) {
        FileInputStream fileInputStream;
        byte[] bFile = new byte[(int) file.length()];
        try {
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bFile);
            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bFile;
    }
}

/*Модель результат отправки показаний на сервер*/
class UpdateReadingResultModel {
    @SerializedName("routeSheetItemId")
    @Expose
    private String routeSheetItemId;

    /* 0 - данные добавлены 1- данные удалены 2 - ошибка синхронизации*/
    @SerializedName("type")
    @Expose
    private int type;

    @SerializedName("description")
    @Expose
    private String description;

    String getRouteSheetItemId() {
        return this.routeSheetItemId;
    }

    String getDescription() {
        return this.description;
    }

    /* Данные успешно добавлены */
    boolean isDataUpdate() {
        return this.type == 0;
    }

    /* Ошибка синхронизации данных*/
    boolean isError() {
        return this.type == 2;
    }
}
