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

public class OperationShare {

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
					Schedulers.currentThread().schedule(new Action0() {
						@Override
						public void call() {
							if (observersCount.decrementAndGet() == 0) {
								mainSubscription.get().unsubscribe();
							}
						}
					});
				}
			};
		}
	}
}
