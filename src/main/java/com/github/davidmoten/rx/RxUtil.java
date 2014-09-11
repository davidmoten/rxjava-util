package com.github.davidmoten.rx;

import java.io.IOException;
import java.io.OutputStream;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Functions;
import rx.subscriptions.Subscriptions;

public class RxUtil {

    public static <T> Func1<T, T> println(final OutputStream out) {
        return new Func1<T, T>() {
            @Override
            public T call(T t) {
                try {
                    out.write(t.toString().getBytes());
                    out.write('\n');
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return t;
            }
        };
    }

    public static <T> Func1<T, T> println() {
        return println(System.out);
    }

    public static <T> void print(Observable<T> o) {
        o.materialize().toBlocking().forEach(new Action1<Notification<T>>() {
            @Override
            public void call(Notification<T> notification) {
                System.out.println(notification);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> Observable<T> concatButIgnoreFirstSequence(Observable<?> o1, Observable<T> o2) {
        return Observable.concat((Observable<T>) o1.filter(Functions.alwaysFalse()), o2);
    }

    public static <T> Func0<T> constant(final T t) {
        return new Func0<T>() {

            @Override
            public T call() {
                return t;
            }
        };
    }

}
