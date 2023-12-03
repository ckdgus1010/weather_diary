package zerobase.weather.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import zerobase.weather.domain.Diary;
import zerobase.weather.service.DiaryService;

import java.time.LocalDate;
import java.util.List;

@RestController
public class DiaryController {
    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    /**
     * 일기 작성하기
     * @param date 일기를 쓸 날자
     * @param text 일기 내용
     */
    @PostMapping("/create/diary")
    void createDiary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
                    , @RequestBody String text) {
        diaryService.createDiary(date, text);
    }

    /**
     * 일기 조회하기
     * @param date 조회할 날짜
     * @return 해당 날짜에 작성한 일기 목록
     */
    @GetMapping("/read/diary")
    List<Diary> readDiary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return diaryService.readDiary(date);
    }

    /**
     * 일기 조회하기
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 해당 기간동안 작성한 일기 목록
     */
    @GetMapping("/read/diaries")
    List<Diary> readDiaries(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
                           , @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return diaryService.readDiaries(startDate, endDate);
    }

    /**
     * 일기 수정하기(해당 날짜의 첫번째 일기를 수정)
     * @param date 수정할 날짜
     * @param text 수정할 내용
     */
    @PutMapping("/update/diary")
    void updateDiary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestBody String text) {
        diaryService.updateDiary(date, text);
    }

    /**
     * 일기 삭제하기(해당 날짜의 모든 일기 삭제)
     * @param date 일기를 삭제할 날짜
     */
    @DeleteMapping("/delete/diary")
    void deleteDiary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        diaryService.deleteDiary(date);
    }
}
