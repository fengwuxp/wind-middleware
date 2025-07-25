package com.wind.security.crypto;

import com.wind.security.authentication.WindAuthenticationProperties;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

class AuthenticationParameterEncryptorTest {

    @Test
    void decrypt() {
        KeyPair keyPair = genKeyPir();
        AuthenticationParameterEncryptor.TEXT_ENCRYPTOR.set(RasTextEncryptor.ofPublicEncrypt(keyPair.getPublic(), keyPair.getPrivate()));
        String principal = RandomStringUtils.randomAlphabetic(16);
        String credentials = RandomStringUtils.randomAlphabetic(16);
        AuthenticationParameterEncryptor.AuthenticationParameter encrypt1 = AuthenticationParameterEncryptor.encrypt(principal, credentials);
        AuthenticationParameterEncryptor.AuthenticationParameter encrypt2 = AuthenticationParameterEncryptor.encrypt(principal, credentials);
        Assertions.assertNotEquals(encrypt1.principal(), encrypt2.principal());
        Assertions.assertNotEquals(encrypt1.credentials(), encrypt2.credentials());
        AuthenticationParameterEncryptor.AuthenticationParameter decrypt = AuthenticationParameterEncryptor.decrypt(encrypt1.principal(),
                encrypt1.credentials());
        Assertions.assertEquals(principal, decrypt.principal());
        Assertions.assertEquals(credentials, decrypt.credentials());
    }

    @Test
    void testDecrypt() {
        WindAuthenticationProperties properties = new WindAuthenticationProperties();
        properties.setRsaPublicKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnsKeGqh2NuPw+A5Uenlj98jUt95jVzfJSm29IGSxkpz/zIt3I" +
                "+bTsk0m2hHir0xlWTbUSiwTWjeznKmV2r5PN1+mXgLyt2Zl4778CC6NzrFSS8t7xGLN+2a+KkEulyiYiPxF" +
                "+w4e5bITIh7oI2A9ehViNA1jaCgnCfyV4MDL4E2E47jKRqvGHnB1ILZ1rOG3Mp2sgaKaC95eSeNJSwhpjIzUKt+gkqCBBaH0boqcTgJ0+EjK3HejVz4EIR3jZUlM4Fx" +
                "+SlUufAm0Gf0/221wO1kCX96+y6h/5vD+qinsAPpY9rg1tljLzn0UxYGpgEyz+Aq5vsM5kTeZ18Hs445DpwIDAQAB");
        properties.setRsaPrivateKey("MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCewp4aqHY24/D4DlR6eWP3yNS33mNXN8lKbb0gZLGSnP" +
                "/Mi3cj5tOyTSbaEeKvTGVZNtRKLBNaN7OcqZXavk83X6ZeAvK3ZmXjvvwILo3OsVJLy3vEYs37Zr4qQS6XKJiI/EX7Dh7lshMiHugjYD16FWI0DWNoKCcJ" +
                "/JXgwMvgTYTjuMpGq8YecHUgtnWs4bcynayBopoL3l5J40lLCGmMjNQq36CSoIEFofRuipxOAnT4SMrcd6NXPgQhHeNlSUzgXH5KVS58CbQZ/T/bbXA7WQJf3r7LqH" +
                "/m8P6qKewA+lj2uDW2WMvOfRTFgamATLP4Crm+wzmRN5nXwezjjkOnAgMBAAECggEAMOyAlG59n4S26XphBi0KZX16L/9UVbhXS5xtv+HH5wqCuaHw4Rts" +
                "+XFVG7aPSL4eLVP8L69Nd2va6dhI2dPzzXioaCg4a9QeagSc3liaUqvKVJksVuSr3WFYEOVtWh" +
                "+kgfJcDnXXh6lMxCMKHny19T1Jugi2LY7SGP3BojVHnqNIWoCfndQeUdz8elzYrtXfMc/XGAPN9eRj4Q4QBdVFTTs0c6veMX4wXrTjEZoKWRzzGW1WDcp/Pj4W" +
                "/g3uItb6yH7muruvPQ7VgXbUfs5gClxREFS/dNJARJVgDGwhzN0rklg+xOitjj3NIrOgNhKQYN3mNL8SWQfgolkTWVRsYQKBgQDXwIE+BKlwCSSDUU7VTX633" +
                "/L0sF0mdgRDNTNjDsU3dnYS5HjH7JQjfQnrowy2wGO+bQFVtzJEAKvt8OuyZmX839N1K0OFInknqC7Q5QEbWLmXywQbxYCNrOCtGYS/SWACzeQbJ+cwFoFZ9jGU" +
                "+QvvBlpIdbxIz5xLtpoR1ufYLQKBgQC8YGc7juDezepcs1lkFzQsMv1aRaYeBxfJ/Pvwu3XO7hPk7vxLoENnw6Wmpx+j50WgAHLQufnpv49VQtfWBkbFfON" +
                "/8uBAb4skPvJoZrG1QzUmYKhXjtR5nm9hAbX63QjqHgf1elJYCzH9bWU8tuD1x0XGYdr+Cgh" +
                "/o2Pq1pp7owKBgQC48eEYO8DPNFcjZmiQeoy8rHHFBQXkuVfwEMPLD1dJsveGPGm433khbsc5Qhzc79DQ5Po28wJYvsajAlnZJOQaP/gxQXPwxVtb" +
                "/lSbZyNqwMTfdP1BnS0UxXQXjn93xjtSTBJUoQER/MjHSD66eD6dmDF+WcacF7PgR1iNIE6/xQKBgAsyz/BCdivIof4Qy2ef2YInJdiP" +
                "+2gdk9R2LaQKAN7kWFpruqPkSx2IC/j0RnU/8muomyq9Y5X5xVOUPbCDfwGOgxfAIIS0HNlyxIZgVIAqFqbJaDiMTSVNveFOcLsRGM6SKnb1Zp/FSpt" +
                "+PV52oLj7t7IrbxFbF1Y/KFl1mIlxAoGAfZTPT4UD5wYJgvues31TR6LKVowIcDAwgw0FgBlMoeLVOYMncY9sfFJPrPfyj1DqVaTjcPyAQBaa4SStFwrYJ" +
                "/AUID8ibuw9q7fupUR+keO1myqYHspvQ9CeuwblFO9RNirursSW9ra6Kr5zeMKptvQSHA4YtXXBLIXr6q/8uRs=");
        KeyPair keyPair = properties.getKeyPair();
        AuthenticationParameterEncryptor.TEXT_ENCRYPTOR.set(RasTextEncryptor.ofPublicEncrypt(keyPair.getPublic(), keyPair.getPrivate()));
        String content = "TGPerUamYrUFvSbSe3+QUfUgwDaVEQHq3IekyopWblZeEJBXWot261kwN4hX/1WduuVFilNMo9bOobuG7" +
                "/1PrZGcO2q0STpvyof8H6H8VU3d1mhp1dUIQoF23ZJIPZHe+1/BBz/Raw9DyMvJczaI5yNrgnuPO0KHcn3LUST/ZxH9eLL1jAByOg56Gnjrw" +
                "/d4Qt5KWrGZF5DdrVQjZ0IXiFA41GBPbXd6/yPIx6PXhtrmsEiSlPcinYKF95NVIYYJZPORKHjEnPPLAs2or7xXRLShswwD9TT6kI8ZZusxE8rH0lpxR5xm3C/hD9UnMKv" +
                "+zH3Et1DemhHxZHdLGywXmA==";
        AuthenticationParameterEncryptor.AuthenticationParameter parameter = AuthenticationParameterEncryptor.decrypt(content, content);
        Assertions.assertNotNull(parameter);

    }

    private KeyPair genKeyPir() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}