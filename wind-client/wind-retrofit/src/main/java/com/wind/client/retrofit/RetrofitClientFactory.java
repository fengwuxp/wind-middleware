package com.wind.client.retrofit;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.wind.api.core.ApiResponse;
import com.wind.api.core.ImmutableApiResponse;
import com.wind.api.core.signature.ApiSecretAccount;
import com.wind.client.retrofit.converter.JacksonConverterFactory;
import com.wind.client.retrofit.converter.JacksonResponseCallAdapterFactory;
import com.wind.common.exception.ApiClientException;
import com.wind.common.exception.AssertUtils;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import retrofit2.Retrofit;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.wind.common.WindDateFormatPatterns.HH_MM_SS;
import static com.wind.common.WindDateFormatPatterns.YYYY_MM_DD;
import static com.wind.common.WindDateFormatPatterns.YYYY_MM_DD_HH_MM_SS;

/**
 * RetrofitClient 服务工厂
 *
 * <p>For example,
 *
 * <pre><code>
 *   RetrofitClientFactory factory = RetrofitClientFactory.builder()
 *                  .baseUrl("You Api Server Address")
 *                  .authenticationHeaderPrefix("Wind")
 *                  .account(ApiSecretAccount.sha256WithRsa("example", "test"))
 *                  .restful();
 *    ExampleRetrofitClient exampleRetrofitClient = factory.create(ExampleRetrofitClient.class);
 * </code></pre>
 *
 * @author wuxp
 * @date 2024-02-27 11:32
 **/
public final class RetrofitClientFactory {

    private final Retrofit retrofit;

    private RetrofitClientFactory(Retrofit retrofit) {
        this.retrofit = retrofit;
    }

    public <T> T create(Class<T> apiClientClass) {
        return retrofit.create(apiClientClass);
    }

    public static RetrofitClientFactoryBuilder builder() {
        return new RetrofitClientFactoryBuilder();
    }

    public static class RetrofitClientFactoryBuilder {

        /**
         * api base url
         */
        private String baseUrl;

        /**
         * 认证相关请求头前缀
         */
        private String authenticationHeaderPrefix;

        /**
         * Api 认证服务账号，如果不为空，则自动添加 {@link OkHttpApiSignatureRequestInterceptor}
         */
        private ApiSecretAccount account;

        /**
         * 自定义的 ObjectMapper
         *
         * @see #buildObjectMapper()
         */
        private ObjectMapper objectMapper;

        /**
         * 自定义的拦截器，当自定义了 {@link #httpClient}，则不会处理该字段
         *
         * @see OkHttpApiSignatureRequestInterceptor
         */
        private List<Interceptor> httpClientInterceptors;

        /**
         * 自定义的 OkHttpClient，若设置需要自行配置超时时间，请求认证相关等配置
         *
         * @see OkHttpApiSignatureRequestInterceptor
         * @see #detaultClientBuilder()
         */
        private OkHttpClient httpClient;

        /**
         * 国际化，请求的语言
         *
         * @see #checkAndUseDefaultConfig()
         */
        private Locale language;

        /**
         * 读写超时时间，单位：秒
         */
        private int readWriteTimeoutSeconds = 20;

        /**
         * 连接超时时间，单位：秒
         */
        private int connectTimeoutSeconds = 5;

        public RetrofitClientFactoryBuilder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public RetrofitClientFactoryBuilder authenticationHeaderPrefix(String authenticationHeaderPrefix) {
            this.authenticationHeaderPrefix = authenticationHeaderPrefix;
            return this;
        }

        public RetrofitClientFactoryBuilder account(ApiSecretAccount account) {
            this.account = account;
            return this;
        }

        public RetrofitClientFactoryBuilder interceptors(Interceptor... interceptors) {
            this.httpClientInterceptors = Arrays.asList(interceptors);
            return this;
        }


        public RetrofitClientFactoryBuilder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public RetrofitClientFactoryBuilder httpClient(OkHttpClient client) {
            this.httpClient = client;
            return this;
        }

        public RetrofitClientFactoryBuilder language(Locale language) {
            this.language = language;
            return this;
        }

        public RetrofitClientFactoryBuilder readWriteTimeoutSeconds(int readWriteTimeoutSeconds) {
            this.readWriteTimeoutSeconds = readWriteTimeoutSeconds;
            return this;
        }

        public RetrofitClientFactoryBuilder connectTimeoutSeconds(int connectTimeoutSeconds) {
            this.connectTimeoutSeconds = connectTimeoutSeconds;
            return this;
        }

        /**
         * 创建默认风格 API RetrofitClientFactory
         *
         * @return 工厂实例
         */
        public RetrofitClientFactory defaults() {
            checkAndUseDefaultConfig();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(httpClient)
                    .addCallAdapterFactory(new JacksonResponseCallAdapterFactory(objectMapper))
                    .addConverterFactory(new JacksonConverterFactory(objectMapper))
                    .build();
            return new RetrofitClientFactory(retrofit);
        }

        /**
         * 创建 restful 风格 API RetrofitClientFactory
         *
         * @return 工厂实例
         */
        public RetrofitClientFactory restful() {
            checkAndUseDefaultConfig();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(httpClient)
                    .addCallAdapterFactory(new JacksonResponseCallAdapterFactory(objectMapper, ImmutableApiResponse.class,
                            RetrofitClientFactoryBuilder::defaultResponseExtractor))
                    .addConverterFactory(new JacksonConverterFactory(objectMapper, ImmutableApiResponse.class,
                            RetrofitClientFactoryBuilder::defaultResponseExtractor))
                    .build();

            return new RetrofitClientFactory(retrofit);
        }

        private void checkAndUseDefaultConfig() {
            if (httpClient == null) {
                AssertUtils.notNull(account, "api authentication account must not null");
                OkHttpClient.Builder builder = detaultClientBuilder()
                        .addInterceptor(new OkHttpApiSignatureRequestInterceptor(key -> account, authenticationHeaderPrefix));
                if (httpClientInterceptors != null) {
                    // 添加自定义拦截器
                    builder.interceptors().addAll(httpClientInterceptors);
                }
                if (language != null) {
                    // 添加国际化请求头
                    builder.interceptors()
                            .add(new Interceptor() {
                                @NotNull
                                @Override
                                public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
                                    Request request = chain.request()
                                            .newBuilder()
                                            .addHeader("Accept-Language", language.toLanguageTag())
                                            .build();
                                    return chain.proceed(request);
                                }
                            });
                }
                httpClient = builder.build();
            }
            if (objectMapper == null) {
                objectMapper = buildObjectMapper();
            }
        }

        private OkHttpClient.Builder detaultClientBuilder() {
            // 设置超时时间，这里以秒为单位
            return new OkHttpClient.Builder()
                    .readTimeout(readWriteTimeoutSeconds, TimeUnit.SECONDS)
                    .writeTimeout(readWriteTimeoutSeconds, TimeUnit.SECONDS)
                    .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true);
        }

        @NotNull
        private static ObjectMapper buildObjectMapper() {
            ObjectMapper result = new ObjectMapper();
            // 配置忽略未知属性
            result.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            result.registerModule(buildJavaTimeModule());
            return result;
        }

        @NotNull
        private static JavaTimeModule buildJavaTimeModule() {
            JavaTimeModule result = new JavaTimeModule();
            result.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS)));
            result.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
            result.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(HH_MM_SS)));
            result.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS)));
            result.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
            result.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(HH_MM_SS)));
            return result;
        }

        @NotNull
        @SuppressWarnings({"rawtypes"})
        private static Object defaultResponseExtractor(Object data) {
            if (data instanceof ApiResponse response) {
                AssertUtils.state(response.isSuccess(), () -> new ApiClientException(response, response.getErrorMessage()));
                return response.getData();
            }
            return data;
        }
    }
}
