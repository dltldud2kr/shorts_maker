package com.example.youtubeshortsproject.web;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ShortsController {

    private final String uploadDir = "C:/shorts/upload/";

    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<String> handleUpload(
            @RequestParam("videoFile") MultipartFile videoFile,
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam("startTime") String startTime) {

        try {
            // 현재 날짜와 시간을 기반으로 디렉토리 생성
            String currentDateTime = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String uploadDirWithDate = uploadDir + currentDateTime + "/";  // 날짜와 시간 포함된 디렉토리 경로
            File dir = new File(uploadDirWithDate);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 파일명 난수값
            String videoFileName = UUID.randomUUID().toString() + "_" + videoFile.getOriginalFilename();
            String audioFileName = UUID.randomUUID().toString() + "_" + audioFile.getOriginalFilename();

            // 저장할 파일 경로
            String videoPath = uploadDirWithDate + videoFileName;
            String audioPath = uploadDirWithDate + audioFileName;

            // 파일 저장
            videoFile.transferTo(new File(videoPath));
            audioFile.transferTo(new File(audioPath));

            // Start Time 을 초 단위로 변환
            String[] timeParts = startTime.split(":");
            int startSeconds = Integer.parseInt(timeParts[0]) * 60 + Integer.parseInt(timeParts[1]);

            // 파이썬 스크립트 실행
            String outputPath = uploadDirWithDate + UUID.randomUUID().toString() + "_output_with_bgm.mp4";  // 출력 파일 이름에 난수값 추가

            ProcessBuilder pb = new ProcessBuilder(
                    "python", "C:/pythonShortsProject/test.py", videoPath, audioPath, outputPath, String.valueOf(startSeconds));
            System.out.println("startSeconds: " + startSeconds);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return ResponseEntity.status(500).body("Python script failed.");
            }

            return ResponseEntity.ok("영상 생성 완료! 파일 경로: " + outputPath);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("에러: " + e.getMessage());
        }
    }
}
