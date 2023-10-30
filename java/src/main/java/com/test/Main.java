
package com.test;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.StsException;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import software.amazon.awssdk.services.sts.StsClient;
import com.google.auth.oauth2.ComputeEngineCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.awscompat.GCPAWSCredentialProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;


public class Main {

  private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
  // private static final String credFile = "/home/srashid/gcp_misc/certs/mineral-minutia-820-e9a7c8665867.json";
  private static final String target_audience = "https://sts.amazonaws.com";

  public static void main(String[] args) throws Exception {

    Main tc = new Main();

    // IdTokenCredentials tok = tc.getIDTokenFromComputeEngine(target_audience);
    IdTokenCredentials tok = tc.getIdTokenFromSA(
        "demo-service-account@ap-syft-experimental-3.iam.gserviceaccount.com", target_audience);

    // ServiceAccountCredentials sac = ServiceAccountCredentials.fromStream(new FileInputStream(credFile));
    // sac = (ServiceAccountCredentials) sac.createScoped(Arrays.asList(CLOUD_PLATFORM_SCOPE));

    // IdTokenCredentials tok = tc.getIDTokenFromServiceAccount(sac, target_audience);

    // String impersonatedServiceAccount =
    // "impersonated-account@project.iam.gserviceaccount.com";
    // IdTokenCredentials tok =
    // tc.getIDTokenFromImpersonatedCredentials((GoogleCredentials)sac,
    // impersonatedServiceAccount, target_audience);

    // AmazonS3 s3 =
    // AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_2).build();

    // https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html#credentials-specify-provider

    // Primary delegated role.
    String roleArn = "arn:aws:iam::444115380735:role/SdpConnectorDelegatedRole";

    // Role under account. Typically this will be discovered.
    String collectorArn = "arn:aws:iam::129583446118:role/SdpConnectorCollectorRole";

    STSAssumeRoleSessionCredentialsProvider credsProvider = new STSAssumeRoleSessionCredentialsProvider(
        GCPAWSCredentialProvider.builder().roleArn(roleArn).roleSessionName(null)
            .googleCredentials(tok).build(), collectorArn, "gcp-aws-collector-role");

    AmazonS3 s3 = AmazonS3ClientBuilder.standard()
        .withRegion(/*Regions.US_EAST_1*/ Regions.EU_NORTH_1)
        .withCredentials(credsProvider)
        .build();

    // bucket under different account accessible only via collector role.
    String bucket_name = "sdp-test-collector-bucket";

    // Bucket in delegated account.
    // String bucket_name = "connector-delegated-s3";

    ListObjectsV2Result result = s3.listObjectsV2(bucket_name);
    List<S3ObjectSummary> objects = result.getObjectSummaries();
    for (S3ObjectSummary os : objects) {
      System.out.println("* " + os.getKey());
    }

  }

  public IdTokenCredentials getIDTokenFromServiceAccount(ServiceAccountCredentials saCreds,
      String targetAudience) {
    IdTokenCredentials tokenCredential = IdTokenCredentials.newBuilder().setIdTokenProvider(saCreds)
        .setTargetAudience(targetAudience).build();
    return tokenCredential;
  }

  public IdTokenCredentials getIDTokenFromComputeEngine(String targetAudience) {
    ComputeEngineCredentials caCreds = ComputeEngineCredentials.create();
    IdTokenCredentials tokenCredential = IdTokenCredentials.newBuilder().setIdTokenProvider(caCreds)
        .setTargetAudience(targetAudience)
        .setOptions(
            Arrays.asList(IdTokenProvider.Option.FORMAT_FULL, IdTokenProvider.Option.LICENSES_TRUE))
        .build();
    return tokenCredential;
  }

  public IdTokenCredentials getIDTokenFromImpersonatedCredentials(GoogleCredentials sourceCreds,
      String impersonatedServieAccount, String targetAudience) {
    ImpersonatedCredentials imCreds = ImpersonatedCredentials.create(sourceCreds,
        impersonatedServieAccount, null,
        Arrays.asList(CLOUD_PLATFORM_SCOPE), 300);
    IdTokenCredentials tokenCredential = IdTokenCredentials.newBuilder().setIdTokenProvider(imCreds)
        .setTargetAudience(targetAudience)
        .setOptions(Arrays.asList(IdTokenProvider.Option.INCLUDE_EMAIL))
        .build();
    return tokenCredential;
  }

  public IdTokenCredentials getIdTokenFromSA(String serviceAccount, String targetAudience) throws Exception {

    GoogleCredentials creds = GoogleCredentials.getApplicationDefault();
    ImpersonatedCredentials imCreds = ImpersonatedCredentials.create(creds,
        serviceAccount, null,
        Arrays.asList(CLOUD_PLATFORM_SCOPE), 300);
    IdTokenCredentials tokenCredential = IdTokenCredentials.newBuilder().setIdTokenProvider(imCreds)
        .setTargetAudience(targetAudience)
        .setOptions(Arrays.asList(IdTokenProvider.Option.INCLUDE_EMAIL))
        .build();
    return tokenCredential;
  }

}
