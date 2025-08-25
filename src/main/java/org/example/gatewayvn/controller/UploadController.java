package org.example.gatewayvn.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/upload")
public class UploadController {

    @Value("${upload.base-dir}")
    private String uploadBaseDir;

    @PostMapping("/upload_excel")
    public ResponseEntity<String> uploadExcel(@RequestParam("file") MultipartFile file,
                                              @RequestParam("process_name") String processName) {
        log.info("接收到文件数据:{}", file.getOriginalFilename());
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("没有接收到文件");
        }

        // 创建对应工序目录
        String dirPath = uploadBaseDir + processName + "/";
        File dir = new File(dirPath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                return ResponseEntity.internalServerError().body("工序目录创建失败: " + dirPath);
            }
        }

        // 保存文件
        File dest = new File(dir, file.getOriginalFilename());
        try {
            file.transferTo(dest);
            return ResponseEntity.ok("上传成功: " + dest.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("文件保存失败：" + e.getMessage());
        }
    }
}

