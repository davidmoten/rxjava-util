package com.github.davidmoten.rx.operators;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.util.functions.Func0;

public class OperationShare {

	public static <T> OnSubscribeFunc<T> share(Func0<Observable<T>> factory) {
		return new ShareFactory<T>(factory);
	}

	public static <T> OnSubscribeFunc<T> share(Observable<T> source) {
		return new Share<T>(source);
	}

	private static class Share<T> implements OnSubscribeFunc<T> {

		private final Observable<? extends T> source;
		private final PublishSubject<T> subject = PublishSubject.create();
		private final AtomicInteger observersCount = new AtomicInteger(0);
		private final AtomicReference<Subscription> mainSubscription = new AtomicReference<Subscription>();

		Share(Observable<? extends T> source) {
			this.source = source;
		}

		@Override
		public Subscription onSubscribe(Observer<? super T> observer) {
			final Subscription sub = subject.subscribe(observer);
			if (observersCount.incrementAndGet() == 1) {
				mainSubscription.set(source.subscribe(subject));
			}
			return new Subscription() {
				@Override
				public void unsubscribe() {
					sub.unsubscribe();
					if (observersCount.decrementAndGet() == 0) {
						mainSubscription.get().unsubscribe();
					}
				}
			};
		}
	}

	private static class ShareFactory<T> implements OnSubscribeFunc<T> {

		private final Func0<Observable<T>> factory;
		private final AtomicReference<Observable<T>> source;
		private final Object lock = new Object();
		private final AtomicReference<PublishSubject<T>> subject = new AtomicReference<PublishSubject<T>>(
				null);
		private final AtomicInteger observersCount = new AtomicInteger(0);
		private final AtomicReference<Subscription> mainSubscription = new AtomicReference<Subscription>();

		ShareFactory(Func0<Observable<T>> factory) {
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
					synchronized (lock) {
						sub.unsubscribe();
						if (observersCount.decrementAndGet() == 0) {
							// once main sub has been abandoned need to
							// regenerate the source using a factory. This is
							// because for example if the source uses
							// CompositeSubscription (like OperatorRetry) then
							// once completely
							// unsubscribed every new subscription forces an
							// unsubscribe action straight away thereby
							// sabotaging the observable.
							mainSubscription.get().unsubscribe();
							source.set(null);
							subject.set(null);
						}
					}
				}
			};
		}
	}
}
