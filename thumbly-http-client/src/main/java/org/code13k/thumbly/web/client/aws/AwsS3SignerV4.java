package org.code13k.thumbly.web.client.aws;

import io.vertx.core.MultiMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.lucasweb.aws.v4.signer.HttpRequest;
import uk.co.lucasweb.aws.v4.signer.Signer;
import uk.co.lucasweb.aws.v4.signer.credentials.AwsCredentials;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class AwsS3SignerV4 {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(AwsS3SignerV4.class);

    // Const
    private static final String CONTENT_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String AMAZON_DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'";

    /**
     * Put headers
     */
    public static boolean putHeaders(MultiMap headers, String url, AwsS3SignValue awsS3SignValue) {
        try {
            // Check
            if(StringUtils.isEmpty(awsS3SignValue.getAccessKey())==true){
                return true;
            }
            if(StringUtils.isEmpty(awsS3SignValue.getSecretKey())==true){
                return false;
            }

            // Init
            URI uri = new URI(url);
            HttpRequest request = new HttpRequest("GET", uri);
            Signer.Builder signerBuilder = Signer.builder();

            // Put Headers
            headers.add("Host", uri.getHost());
            headers.add("X-Amz-Date", getCurrentDate());
            headers.add("X-Amz-Content-Sha256", CONTENT_SHA256);

            // Set Data For Sign
            signerBuilder.awsCredentials(new AwsCredentials(awsS3SignValue.getAccessKey(), awsS3SignValue.getSecretKey()));
            signerBuilder.region(awsS3SignValue.getRegion());
            headers.forEach(header -> {
                signerBuilder.header(header.getKey(), header.getValue());
            });

            // Generate authorization string
            String authorization = signerBuilder.buildS3(request, CONTENT_SHA256).getSignature();

            // Result
            headers.add("Authorization", authorization);
            return true;
        } catch (Exception e) {
            mLogger.error("Failed to put headers for s3", e);
            return false;
        }
    }

    /**
     * getCurrentDate()
     */
    private static String getCurrentDate() {
        SimpleDateFormat format = new SimpleDateFormat(AMAZON_DATE_FORMAT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String result = format.format(System.currentTimeMillis());
        return result;
    }

}
