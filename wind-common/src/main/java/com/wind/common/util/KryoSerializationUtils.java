package com.wind.common.util;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.util.Pool;
import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * kryo 序列化工具类（仅限框架内部使用）
 *
 * @author wuxp
 * @link <a href="https://github.com/EsotericSoftware/kryo">kryo</a>
 * @date 2025-10-13 11:18
 **/
@Slf4j
public final class KryoSerializationUtils {

    private static final KryoSerializationUtils INSTANCE = new KryoSerializationUtils();

    private final Pool<Kryo> kryoPool;
    private final Pool<Input> inputPool;
    private final Pool<Output> outputPool;

    private KryoSerializationUtils() {
        this(null);
    }

    private KryoSerializationUtils(ClassLoader classLoader) {
        this.kryoPool = new Pool<>(true, false, 1024) {
            @Override
            protected Kryo create() {
                return createKryo(classLoader);
            }
        };
        this.inputPool = new Pool<>(true, false, 512) {
            @Override
            protected Input create() {
                return new Input(8192);
            }
        };
        this.outputPool = new Pool<>(true, false, 512) {
            @Override
            protected Output create() {
                return new Output(8192, -1);
            }
        };
    }

    public static KryoSerializationUtils getInstance() {
        return INSTANCE;
    }

    public static KryoSerializationUtils create(ClassLoader classLoader) {
        return new KryoSerializationUtils(classLoader);
    }

    private Kryo createKryo(ClassLoader classLoader) {
        Kryo kryo = new Kryo();
        if (classLoader != null) {
            kryo.setClassLoader(classLoader);
        }
        kryo.setRegistrationRequired(false);
        // ✅ 防止循环引用错误
        kryo.setReferences(true);
        kryo.addDefaultSerializer(Throwable.class, new JavaSerializer());
        // 避免无默认构造函数的类反序列化失败
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        return kryo;
    }

    @NotNull
    public Object decode(byte[] bytes) {
        AssertUtils.isTrue(bytes.length > 0, "argument bytes must not empty");
        Kryo kryo = kryoPool.obtain();
        Input input = inputPool.obtain();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            input.setInputStream(bais);
            return kryo.readClassAndObject(input);
        } catch (Exception e) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "kryo decode error", e);
        } finally {
            input.reset();
            kryoPool.free(kryo);
            inputPool.free(input);
        }
    }

    @NotNull
    public Object decode(@NotBlank String text) {
        AssertUtils.hasText(text, "argument text must not null");
        byte[] bytes = Base64.getDecoder().decode(text);
        return decode(bytes);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T decodeAs(@NotBlank String text) {
        return (T) decode(text);
    }

    @NotNull
    public byte[] encode(Object in) {
        Kryo kryo = kryoPool.obtain();
        Output output = outputPool.obtain();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            output.setOutputStream(baos);
            kryo.writeClassAndObject(output, in);
            output.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "kryo encode error", e);
        } finally {
            output.reset();
            kryoPool.free(kryo);
            outputPool.free(output);
        }
    }

    @NotBlank
    public String encodeToString(@NotNull Object in) {
        AssertUtils.notNull(in, "argument in must not null");
        return Base64.getEncoder().encodeToString(encode(in));
    }
}
