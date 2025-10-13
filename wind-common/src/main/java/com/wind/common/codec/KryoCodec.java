package com.wind.common.codec;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.util.Pool;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import jakarta.validation.constraints.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * kryo 序列化工具类（仅限框架内部使用）
 *
 * @author wuxp
 * @link <a href="https://github.com/EsotericSoftware/kryo">kryo</a>
 * @date 2025-10-13 11:18
 **/
public final class KryoCodec {

    private static final KryoCodec INSTANCE = new KryoCodec();

    private final Pool<Kryo> kryoPool;

    private final Pool<Input> inputPool;

    private final Pool<Output> outputPool;

    public KryoCodec() {
        this(null);
    }

    public KryoCodec(ClassLoader classLoader) {

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

    public static KryoCodec getInstance() {
        return INSTANCE;
    }

    private Kryo createKryo(ClassLoader classLoader) {
        Kryo kryo = new Kryo();
        if (classLoader != null) {
            kryo.setClassLoader(classLoader);
        }
        kryo.setRegistrationRequired(false);
        kryo.setReferences(false);
        kryo.addDefaultSerializer(Throwable.class, new JavaSerializer());
        return kryo;
    }

    @NotNull
    public Object decode(byte[] bytes) {
        Kryo kryo = kryoPool.obtain();
        Input input = inputPool.obtain();
        try {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
                input.setInputStream(inputStream);
                return kryo.readClassAndObject(input);
            } finally {
                kryoPool.free(kryo);
                inputPool.free(input);
            }
        } catch (IOException exception) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "kryo decode error", exception);
        }
    }

    @NotNull
    public byte[] encode(Object in) {
        Kryo kryo = kryoPool.obtain();
        Output output = outputPool.obtain();
        try {
            try (OutputStream outputStream = new ByteArrayOutputStream()) {
                output.setOutputStream(outputStream);
                kryo.writeClassAndObject(output, in);
                output.flush();
                return output.getBuffer();
            } finally {
                kryoPool.free(kryo);
                outputPool.free(output);
            }
        } catch (IOException exception) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "kryo encode error", exception);
        }
    }
}