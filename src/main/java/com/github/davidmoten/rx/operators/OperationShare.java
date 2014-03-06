package com.github.davidmoten.rx.operators;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;

public class OperationShare {

	public static <T> OnSubscribe<T> share(Observable<T> source) {
		return new Share<T>(source);
	}

	private static class Share<T> implements OnSubscribe<T> {

		private final Observable<? extends T> source;
		private final PublishSubject<T> subject = PublishSubject.create();
		private final AtomicInteger observersCount = new AtomicInteger(0);
		private final AtomicReference<Subscription> mainSubscription = new AtomicReference<Subscription>(
				Subscriptions.empty());
		private final Object lock = new Object();

		Share(Observable<? extends T> source) {
			this.source = source;
		}

		@Override
		public void call(Subscriber<? super T> subscriber) {
			subject.subscribe(subscriber);
			subscriber.add(new Subscription() {

				@Override
				public void unsubscribe() {
					synchronized (lock) {
						if (observersCount.decrementAndGet() == 0)
							mainSubscription.get().unsubscribe();
					}
				}

				@Override
				public boolean isUnsubscribed() {
					return mainSubscription.get().isUnsubscribed();
				}
			});

			synchronized (lock) {
				if (observersCount.incrementAndGet() == 1) {
					mainSubscription.set(source.subscribe(subject));
				}
			}
		}
	}

	public static class ShareOperator<T> implements Operator<T, T> {

		private final Observable<? extends T> source;
		private final PublishSubject<T> subject = PublishSubject.create();
		private final AtomicInteger observersCount = new AtomicInteger(0);
		private final AtomicReference<Subscription> mainSubscription = new AtomicReference<Subscription>(
				Subscriptions.empty());
		private final Object lock = new Object();

		ShareOperator(Observable<? extends T> source) {
			this.source = source;
		}

		@Override
		public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {
			subject.subscribe(subscriber);
			synchronized (lock) {
				if (observersCount.incrementAndGet() == 1) {
					mainSubscription.set(source.subscribe(subject));
				}
			}
			Subscriber<T> result = new Subscriber<T>() {

				@Override
				public void onCompleted() {
					subscriber.onCompleted();
				}

				@Override
				public void onError(Throwable e) {
					subscriber.onError(e);
				}

				@Override
				public void onNext(T t) {
					subscriber.onNext(t);
				}
			};
			subscriber.add(result);
			return result;
		}

	}
}
