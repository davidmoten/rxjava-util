package com.github.davidmoten.rx.operators;

import rx.Observable.Operator;
import rx.Subscriber;

public class OperatorIdentity1<T> implements Operator<T, T> {

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        // return a facade around child
        return new Subscriber<T>(child) {

            @Override
            public void onCompleted() {
                child.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onNext(T t) {
                child.onNext(t);
            }
        };

    }
}
