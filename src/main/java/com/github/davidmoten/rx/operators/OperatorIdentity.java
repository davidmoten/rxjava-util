package com.github.davidmoten.rx.operators;

import rx.Observable.Operator;
import rx.Subscriber;

public class OperatorIdentity<T> implements Operator<T, T> {

	@Override
	public Subscriber<? super T> call(Subscriber<? super T> child) {
		return new Subscriber<T>(child) {

			@Override
			public void onCompleted() {
				// TODO Auto-generated method stub

			}

			@Override
			public void onError(Throwable arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onNext(T arg0) {
				// TODO Auto-generated method stub

			}
		};

	}
}
