package com.github.davidmoten.rx.operators;

import rx.Observable.Operator;
import rx.Subscriber;

public class OperatorEmitNothing<S, T> implements Operator<T, S> {

	@Override
	public Subscriber<? super S> call(final Subscriber<? super T> child) {
		Subscriber<S> parent = new Subscriber<S>() {

			@Override
			public void onCompleted() {
			}

			@Override
			public void onError(Throwable e) {
			}

			@Override
			public void onNext(S s) {
			}
		};
		child.add(parent);
		child.onCompleted();
		return parent;
	}
}
