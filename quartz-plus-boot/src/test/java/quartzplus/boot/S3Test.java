package quartzplus.boot;

import in.clouthink.daas.fss.core.FileStorage;
import in.clouthink.daas.fss.core.StoreFileResponse;
import in.clouthink.daas.fss.core.StoredFileObject;
import in.clouthink.daas.fss.support.DefaultStoreFileRequest;
import in.clouthink.daas.fss.util.MetadataUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.FileOutputStream;
import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = BootApplication.class)
@ActiveProfiles("local")
public class S3Test {

    @Resource
    private FileStorage fileStorage;

    private ClassPathResource txtResource = new ClassPathResource("test.txt");

    /**
     * 测试文件上传、下载和删除功能
     */
    @Test
    public void test() throws IOException {
        Assert.assertTrue(txtResource.exists());

        DefaultStoreFileRequest request = new DefaultStoreFileRequest();
        request.setOriginalFilename(txtResource.getFilename());
        request.setContentType(MediaType.TEXT_PLAIN.toString());
        request.setSize(txtResource.contentLength());
        StoreFileResponse response = fileStorage.store(txtResource.getInputStream(), request);

        Assert.assertEquals("s3", response.getProviderName());

        StoredFileObject storedFileObject = response.getStoredFileObject();
        Assert.assertNotNull(storedFileObject);

        storedFileObject = fileStorage.findByStoredFilename(storedFileObject.getStoredFilename());
        String saveToFilename = MetadataUtils.generateFilename(request);
        storedFileObject.writeTo(new FileOutputStream(saveToFilename), 1024 * 4);

        storedFileObject = fileStorage.delete(storedFileObject.getStoredFilename());
        Assert.assertNull(storedFileObject.getImplementation());
    }

}
