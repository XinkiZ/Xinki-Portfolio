package com.xinki.portfolio.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.xinki.portfolio.common.BusinessException;
import com.xinki.portfolio.service.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
public class OssServiceImpl implements OssService {

    private final OSS ossClient;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    public OssServiceImpl(OSS ossClient) {
        this.ossClient = ossClient;
    }

    @Override
    public String upload(MultipartFile file) {
        String objectName = generateObjectName(file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName, objectName, inputStream, metadata);

            ossClient.putObject(putObjectRequest);

            log.info("OSS upload success: {}", objectName);
        } catch (OSSException e) {
            log.error("OSS error: code={}, message={}, requestId={}",
                    e.getErrorCode(), e.getErrorMessage(), e.getRequestId());
            throw new BusinessException("OSS 上传失败：" + e.getErrorMessage());
        } catch (ClientException e) {
            log.error("OSS client error: {}", e.getMessage());
            throw new BusinessException("OSS 客户端异常");
        } catch (IOException e) {
            log.error("File read error: {}", e.getMessage());
            throw new BusinessException("文件读取失败");
        }

        return "https://" + bucketName + "." + endpoint + "/" + objectName;
    }

    private String generateObjectName(String originalFilename) {
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return "portfolio/" + datePath + "/" + UUID.randomUUID() + ext;
    }
}