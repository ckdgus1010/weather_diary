package zerobase.weather.service;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import zerobase.weather.WeatherApplication;
import zerobase.weather.domain.DateWeather;
import zerobase.weather.domain.Diary;
import zerobase.weather.repository.DateWeatherRepository;
import zerobase.weather.repository.DiaryRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DiaryService {

    @Value("${openweathermap.key}")
    private String apiKey;

    private final DiaryRepository diaryRepository;
    private final DateWeatherRepository dateWeatherRepository;

    private static final Logger logger = LoggerFactory.getLogger(WeatherApplication.class);

    public DiaryService(DiaryRepository diaryRepository, DateWeatherRepository dateWeatherRepository) {
        this.diaryRepository = diaryRepository;
        this.dateWeatherRepository = dateWeatherRepository;
    }

    /**
     * 매일 새벽 1시에 날씨 정보 가져온 후 DB에 저장
     */
    @Transactional
    @Scheduled(cron = "0 0 1 * * *")
    public void saveWeatherData() {
        logger.info("날씨 정보 불러오기");
        dateWeatherRepository.save(getCurrentWeatherFromApi());
    }

    /**
     * 일기 생성하기
     * @param date 생성할 날짜
     * @param text 일기 내용
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createDiary(LocalDate date, String text) {
        logger.debug("start to create diary.");
        DateWeather dateWeather = getDateWeather(date);

        Diary nowDiary = new Diary();
        nowDiary.setDateWeather(dateWeather);
        nowDiary.setText(text);

        diaryRepository.save(nowDiary);
        logger.debug("createDiary() => complete");
    }

    /**
     * 일기 가져오기
     * @param date
     * @return 해당 일자에 작성한 일기 목록
     */
    @Transactional(readOnly = true)
    public List<Diary> readDiary(LocalDate date) {
        logger.debug("read diary.");
        return diaryRepository.findAllByDate(date);
    }

    /**
     * 일기 가져오기
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 해당 기간동안 작성한 일기 목록
     */
    @Transactional(readOnly = true)
    public List<Diary> readDiaries(LocalDate startDate, LocalDate endDate) {
        logger.debug("read diaries.");
        return diaryRepository.findAllByDateBetween(startDate, endDate);
    }

    /**
     * 일기 수정하기
     * @param date 시작일
     * @param text 수정할 내용
     */
    public void updateDiary(LocalDate date, String text) {
        logger.debug("start to update diary.");
        Diary nowDiary = diaryRepository.getFirstByDate(date);
        nowDiary.setText(text);

        diaryRepository.save(nowDiary);
        logger.debug("updateDiary() => complete");
    }

    /**
     * 일기 삭제하기(해당 날짜의 모든 일기 삭제)
     * @param date 삭제할 날짜
     */
    public void deleteDiary(LocalDate date) {
        logger.debug("start to delete diary.");
        diaryRepository.deleteAllByDate(date);
        logger.debug("deleteDiary() => complete");
    }

    /**
     * 날씨 정보 불러오기
     * @param date 날씨 정보를 확인할 날짜
     * @return 해당 날짜의 날씨 정보
     */
    private DateWeather getDateWeather(LocalDate date) {
        // DB에서 날씨 정보 조회하기
        List<DateWeather> dateWeatherListFromDB = dateWeatherRepository.findAllByDate(date);

        if (dateWeatherListFromDB.isEmpty()) {
            // 저장된 데이터가 없으면 없으면 현재 날씨 정보 불러오기
            logger.debug("No saved data. Download current weather data from Open Weather Map.");
            return getCurrentWeatherFromApi();
        } else {
            return dateWeatherListFromDB.get(0);
        }
    }

    /**
     * 현재 날씨 데이터를 open weather map에서 불러오기
     * @return 현재 날씨 정보
     */
    private DateWeather getCurrentWeatherFromApi() {
        String currentWeatherData = getCurrentWeatherData();

        Map<String, Object> parsedWeather = parseWeatherData(currentWeatherData);

        DateWeather dateWeather = new DateWeather();
        dateWeather.setDate(LocalDate.now());
        dateWeather.setWeather(parsedWeather.get("main").toString());
        dateWeather.setIcon(parsedWeather.get("icon").toString());
        dateWeather.setTemperature((Double) parsedWeather.get("temp"));

        return dateWeather;
    }

    /**
     * open weather map 사이트에서 날씨 데이터 가져오기
     * @return open weather map 사이트에서 가져온 날씨 데이터
     */
    private String getCurrentWeatherData() {
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=seoul&appid=" + apiKey;

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            BufferedReader br;
            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }

            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();

            return response.toString();
        } catch (Exception e) {
            logger.error("failed to get response.");
            return "failed to get response.";
        }
    }

    /**
     * 프로그램에서 사용할 데이터만 추출하기
     * @param str 서버에서 받아온 데이터
     * @return 추출된 데이터
     */
    private Map<String, Object> parseWeatherData(String str) {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject;

        try {
            jsonObject = (JSONObject) jsonParser.parse(str);
        } catch (ParseException parseException) {
            throw new RuntimeException(parseException);
        }

        Map<String, Object> resultMap = new HashMap<>();

        JSONObject mainData = (JSONObject) jsonObject.get("main");
        resultMap.put("temp", mainData.get("temp"));

        JSONArray weatherArray = (JSONArray) jsonObject.get("weather");
        JSONObject weatherData = (JSONObject) weatherArray.get(0);
        resultMap.put("main", weatherData.get("main"));
        resultMap.put("icon", weatherData.get("icon"));

        return resultMap;
    }
}
