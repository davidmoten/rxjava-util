package com.github.davidmoten.rx.operators;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subjects.PublishSubject;

public class OperationShare {

	public static <T> OnSubscribeFunc<T> share(Observable<? extends T> that) {
		return new Share<T>(that);
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
					System.out.println("unsubscribing");
					sub.unsubscribe();
					if (observersCount.decrementAndGet() == 0) {
						System.out.println("unsubscribing main");
						mainSubscription.get().unsubscribe();
					}
				}
			};
		}
	}
}
