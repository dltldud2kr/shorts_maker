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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ShortsController {

    private final String uploadDir = "C:/shorts/upload/";


    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<String> handleUpload(
            @RequestParam("videoFile") MultipartFile[] videoFiles,
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam("startTime") String startTime) {

        try {
            // 업로드 디렉토리 생성
            String currentDateTime = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String uploadDirWithDate = uploadDir + currentDateTime + "/";
            File dir = new File(uploadDirWithDate);
            if (!dir.exists()) dir.mkdirs();

            // 영상 파일 저장 및 경로 수집
            List<String> videoPaths = new ArrayList<>();
            int idx = 1;
            for (MultipartFile videoFile : videoFiles) {
                String videoPath = uploadDirWithDate + "video" + idx++ + ".mp4";
                videoFile.transferTo(new File(videoPath));
                videoPaths.add(videoPath);
            }

            // 오디오 파일 저장
            String audioPath = uploadDirWithDate + "original_audio.mp3";
            audioFile.transferTo(new File(audioPath));

            // 시작 시간 (초 단위)
            String[] timeParts = startTime.split(":");
            int startSeconds = Integer.parseInt(timeParts[0]) * 60 + Integer.parseInt(timeParts[1]);

            // 출력 파일명 설정
            String outputPath = uploadDirWithDate + "shorts_final.mp4";

            // 파이썬 인자 구성 (video1.mp4 video2.mp4 ... audio.mp3 output.mp4 startTime)
            List<String> command = new ArrayList<>();
            command.add("python");
            command.add("C:/pythonShortsProject/test2.py");
            command.addAll(videoPaths);
            command.add(audioPath);
            command.add(outputPath);
            command.add(String.valueOf(startSeconds));

            // 파이썬 실행
            ProcessBuilder pb = new ProcessBuilder(command);
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

            // 원본 파일 삭제
            for (String path : videoPaths) new File(path).delete();
            new File(audioPath).delete();

            return ResponseEntity.ok("🎬 영상 생성 완료!\n경로: " + outputPath);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("에러 발생: " + e.getMessage());
        }
    }
}
