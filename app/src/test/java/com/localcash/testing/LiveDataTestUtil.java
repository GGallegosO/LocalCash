package com.localcash.testing;

import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import org.robolectric.Shadows;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * Utilidad de test para consumir {@link LiveData} de Room en JVM con Robolectric.
 * <p>
 * Room ejecuta la consulta en un hilo de fondo y "postea" el resultado al hilo
 * principal (loper pausado en Robolectric). El patrón es: observar, hacer
 * {@code idle()} del looper principal en bucle y evaluar una condición, con
 * tope de tiempo. Sin dependencias adicionales (evita `androidx.arch.core:core-testing`,
 * que no está registrado en tech-stack.md).
 * </p>
 */
public final class LiveDataTestUtil {

    /** Tiempo máximo de espera antes de fallar el test. */
    private static final long TIMEOUT_MS = 5_000L;

    /** Pausa entre reintentos de idle/evaluación. */
    private static final long POLL_MS = 10L;

    private LiveDataTestUtil() {
        // Utilidad de test: no debe instanciarse.
    }

    /**
     * Espera hasta que la LiveData tenga un valor no nulo.
     *
     * @param liveData LiveData observada (Room la dispara al observarla)
     * @return valor actual no nulo
     */
    public static <T> T awaitValue(LiveData<T> liveData) {
        return awaitValue(liveData, Objects::nonNull);
    }

    /**
     * Espera hasta que la LiveData cumpla la condición dada.
     *
     * @param liveData  LiveData observada
     * @param condition condición que debe cumplir el valor (ej. "lista vacía")
     * @return primer valor que cumple la condición
     * @throws AssertionError si se agota el tiempo de espera
     */
    public static <T> T awaitValue(LiveData<T> liveData, Predicate<T> condition) {
        AtomicReference<T> ultimo = new AtomicReference<>();
        Observer<T> observer = ultimo::set;
        liveData.observeForever(observer);
        try {
            long limite = System.currentTimeMillis() + TIMEOUT_MS;
            while (System.currentTimeMillis() < limite) {
                Shadows.shadowOf(Looper.getMainLooper()).idle();
                T valor = liveData.getValue();
                if (valor != null && condition.test(valor)) {
                    return valor;
                }
                dormir(POLL_MS);
            }
            throw new AssertionError(
                    "LiveData no cumplió la condición en " + TIMEOUT_MS
                            + " ms; último valor = " + liveData.getValue());
        } finally {
            liveData.removeObserver(observer);
        }
    }

    private static void dormir(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
