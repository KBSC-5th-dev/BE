package kbsc.greenFunding.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import kbsc.greenFunding.dto.response.ErrorCode;
import kbsc.greenFunding.exception.EmptyFileException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AwsS3Service {

    private final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    public String bucket;

    @Value("${cloud.aws.file_url_format}")
    public String fileUrlFormat;

    // 복수개 이미지 업로드
    public List<String> uploadImage(List<MultipartFile> multipartFile) {
        List<String> fileNameList = new ArrayList<>();

        multipartFile.forEach(file -> {
            String fileName = uploadImage(file);
            fileNameList.add(fileUrlFormat + fileName);
        });

        return fileNameList;
    }

    // 단일 이미지 업로드
    public String uploadImage(MultipartFile file) {
        validateFileExists(file);

        String fileName = createFileName(file.getOriginalFilename());
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(file.getSize());
        objectMetadata.setContentType(file.getContentType());

        try(InputStream inputStream = file.getInputStream()) {
            amazonS3Client.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead));
        } catch(IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다.");
        }

        return fileUrlFormat + fileName;
    }


    private String createFileName(String fileName) {
        return UUID.randomUUID().toString().concat(getFileExtension(fileName));
    }


    private String getFileExtension(String fileName) {
        try {
            return fileName.substring(fileName.lastIndexOf("."));
        } catch (StringIndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 형식의 파일(" + fileName + ") 입니다.");
        }
    }

    private void validateFileExists(MultipartFile multipartFile) {
        if (multipartFile.isEmpty()) {
            throw new EmptyFileException("empty file", ErrorCode.EMPTY_FILE);
        }
    }

}
