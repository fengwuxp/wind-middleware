package com.wind.security.mfa;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import com.warrenstrange.googleauth.HmacHashFunction;
import com.warrenstrange.googleauth.ICredentialRepository;
import com.warrenstrange.googleauth.KeyRepresentation;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * @author wuxp
 * @date 2024-03-05 15:21
 **/
public class GoogleTotpAuthenticator implements TotpAuthenticator {

    private static final int DEFAULT_QR_CODE_SIZE = 300;

    private final GoogleAuthenticator googleAuthenticator;

    public GoogleTotpAuthenticator(GoogleAuthenticatorConfig config, ICredentialRepository repository) {
        GoogleAuthenticator authenticator = new GoogleAuthenticator(config);
        authenticator.setCredentialRepository(repository);
        this.googleAuthenticator = authenticator;

    }

    public GoogleTotpAuthenticator(ICredentialRepository repository) {
        this(getDefaultConfig(), repository);
    }

    @Override
    public boolean verify(String userId, String code) {
        return this.googleAuthenticator.authorizeUser(userId, Integer.parseInt(code));
    }

    @Override
    public String generateBindingQrCode(String userId, String showName, String issuer) {
        String url = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(issuer, showName, googleAuthenticator.createCredentials(userId));
        return convertQrCode(url);
    }

    private String convertQrCode(String url) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, DEFAULT_QR_CODE_SIZE, DEFAULT_QR_CODE_SIZE);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", stream);
            return Base64.getEncoder().encodeToString(stream.toByteArray());
        } catch (WriterException | IOException exception) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "生成绑定MFA认证二维码失败", exception);
        }
    }

    private static GoogleAuthenticatorConfig getDefaultConfig() {
        return new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setCodeDigits(6)
                .setSecretBits(512)
                .setHmacHashFunction(HmacHashFunction.HmacSHA256)
                .setWindowSize(3)
                .setKeyRepresentation(KeyRepresentation.BASE32)
                .build();
    }
}
