package com.github.davidmoten.rx.operators;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.util.functions.Action0;
import rx.util.functions.Func0;

public class OperationShareWithFactory {

	public static <T> OnSubscribeFunc<T> share(Func0<Observable<T>> factory) {
		return new Share<T>(factory);
	}

	private static class Share<T> implements OnSubscribeFunc<T> {

		private final Func0<Observable<T>> factory;
		private final AtomicReference<Observable<T>> source;
		private final Object lock = new Object();
		private final AtomicReference<PublishSubject<T>> subject = new AtomicReference<PublishSubject<T>>(
				null);
		private final AtomicInteger observersCount = new AtomicInteger(0);
		private final AtomicReference<Subscription> mainSubscription = new AtomicReference<Subscription>();

		Share(Func0<Observable<T>> factory) {
			this.factory = factory;
			this.source = new AtomicReference<Observable<T>>(null);
		}

		@Override
		public Subscription onSubscribe(Observer<? super T> observer) {
			final Subscription sub;
			synchronized (lock) {
				if (subject.get() == null)
					subject.set(PublishSubject.<T> create());
				if (source.get() == null)
					source.set(factory.call());
				sub = subject.get().subscribe(observer);
				if (observersCount.incrementAndGet() == 1) {
					mainSubscription.set(source.get().subscribe(subject.get()));
				}
			}
			return new Subscription() {
				@Override
				public void unsubscribe() {
					Schedulers.currentThread().schedule(new Action0() {
						@Override
						public void call() {
							synchronized (lock) {
								sub.unsubscribe();
								if (observersCount.decrementAndGet() == 0) {
									// once main sub has been abandoned need to
									// regenerate the source using a factory.
									// This is
									// because for example if the source uses
									// CompositeSubscription then once
									// completely
									// unsubscribed every new subscription
									// forces an
									// unsubscribe action straight away thereby
									// sabotaging the observable. Retry operator
									// does
									// this in 0.16.1 but is fixed in later
									// versions.
									mainSubscription.get().unsubscribe();
									source.set(null);
									subject.set(null);
								}
							}
						}
					});
				}
			};
		}
	}
}
