package com.adam.ftsweb.controller;

import com.adam.ftsweb.config.WebConfig;
import com.adam.ftsweb.constant.FileConstant;
import com.adam.ftsweb.service.UserService;
import com.adam.ftsweb.util.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Autowired
    private UserService userService;
    private final String fileSeparator = System.getProperty("file.separator");
    private final String rootPath = File.listRoots()[0].getPath() + fileSeparator + "fts-web";

    @ResponseBody
    @RequestMapping("/upload")
    public Response<String> upload(MultipartFile file, String token) {
        if(file == null) {
            return Response.fail(FileConstant.REQUEST_FILE_IS_NULL);
        }
        if(StringUtils.isBlank(token)) {
            return Response.fail(FileConstant.REQUEST_TOKEN_IS_BLANK);
        }
        log.debug("upload file {} {} token={}", file.getName(), file.getOriginalFilename(), token);
        Long ftsId = userService.getFtsIdByToken(token, false);
        if(ftsId == null) {
            return Response.fail(FileConstant.REQUEST_TOKEN_IS_INVALID);
        }
        try {
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            File file1 = new File(rootPath + fileSeparator + ftsId + fileSeparator + now + file.getOriginalFilename());
            if(file1.mkdirs()) {
                file.transferTo(file1);
                String ip = InetAddress.getLocalHost().getHostAddress();
                String downloadUrl = "http://" + ip + ":" + WebConfig.SERVER_PORT + "/file/download?ftsId=" + ftsId + "&fileName=" + now + file.getOriginalFilename();
                return Response.success(downloadUrl);
            } else {
                return Response.fail(FileConstant.UPLOAD_FAIL);
            }
        } catch (IOException e) {
            log.error("upload file error ftsId={} file={}", ftsId, file.getOriginalFilename(), e);
            return Response.fail(FileConstant.UPLOAD_FAIL);
        }
    }

    @RequestMapping("/download")
    public void downloadLocal(@RequestParam long ftsId, @RequestParam String fileName, HttpServletResponse response) throws IOException {
        String path = rootPath + fileSeparator + ftsId + fileSeparator + fileName;
        File file = new File(path);
        if(!file.exists()) {
            throw new FileNotFoundException("找不到对应的文件，文件可能已被删除或移动");
        }
        // 读到流中
        InputStream inputStream = new FileInputStream(path);// 文件的存放路径
        response.reset();
        response.setContentType("application/octet-stream");
        String filename = new File(path).getName();
        response.addHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(filename, "UTF-8"));
        ServletOutputStream outputStream = response.getOutputStream();
        byte[] b = new byte[1024];
        int len;
        //从输入流中读取一定数量的字节，并将其存储在缓冲区字节数组中，读到末尾返回-1
        while ((len = inputStream.read(b)) > 0) {
            outputStream.write(b, 0, len);
        }
        inputStream.close();
    }

    public static void main(String[] args) {
        String rootPath = File.listRoots()[0].getPath();
        System.out.println(rootPath);
    }
}