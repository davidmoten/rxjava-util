package com.github.davidmoten.rx.operators;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.observers.Subscribers;

public class OperatorIdentity2<T> implements Operator<T, T> {

	@Override
	public Subscriber<? super T> call(Subscriber<? super T> child) {
		Subscriber<T> parent = Subscribers.from(child);
		// must do this otherwise unsubscription will not work across the
		// operator
		child.add(parent);
		return parent;
	}
}
