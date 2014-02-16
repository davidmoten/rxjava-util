/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.davidmoten.rx.operators;

/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class OperationRetry {

	private static final int INFINITE_RETRY = -1;

	public static <T> OnSubscribeFunc<T> retry(final Observable<T> observable,
			final int maxRetries) {
		return new Retry<T>(observable, maxRetries);
	}

	public static <T> OnSubscribeFunc<T> retry(final Observable<T> observable) {
		return new Retry<T>(observable, INFINITE_RETRY);
	}

	private static class Retry<T> implements OnSubscribeFunc<T> {

		private final Observable<T> source;
		private final int maxRetries;

		public Retry(Observable<T> source, int maxRetries) {
			this.source = source;
			this.maxRetries = maxRetries;
		}

		@Override
		public Subscription onSubscribe(Observer<? super T> observer) {
			final AtomicInteger attempts = new AtomicInteger(0);
			final AtomicReference<Subscription> sourceSubscription = new AtomicReference<Subscription>(
					Subscriptions.empty());
			return new RetrySubscription<T>(source, observer,
					sourceSubscription, attempts, maxRetries);
		}
	}

	private static class RetrySubscription<T> implements Subscription {

		private final Observable<T> source;
		private final Observer<? super T> observer;
		private final AtomicReference<Subscription> sourceSubscription;
		private final AtomicInteger attempts;
		private final int maxRetries;
		private final Object lock = new Object();

		RetrySubscription(Observable<T> source, Observer<? super T> observer,
				AtomicReference<Subscription> sourceSubscription,
				AtomicInteger attempts, int maxRetries) {
			this.source = source;
			this.observer = observer;
			this.sourceSubscription = sourceSubscription;
			this.attempts = attempts;
			this.maxRetries = maxRetries;
			subscribeToSource();
		}

		private void subscribeToSource() {
			sourceSubscription.set(source.subscribe(createObserver()));
		}

		private void unsubscribeFromSource() {
			sourceSubscription.get().unsubscribe();
		}

		@Override
		public void unsubscribe() {
			synchronized (lock) {
				unsubscribeFromSource();
			}
		}

		private Observer<? super T> createObserver() {
			return new Observer<T>() {

				@Override
				public void onCompleted() {
					observer.onCompleted();
				}

				@Override
				public void onError(Throwable e) {
					if (attempts.getAndIncrement() < maxRetries
							|| maxRetries == INFINITE_RETRY) {
						synchronized (lock) {
							unsubscribeFromSource();
							subscribeToSource();
						}
					} else
						observer.onError(e);
				}

				@Override
				public void onNext(T t) {
					observer.onNext(t);
				}
			};
		}
	}

}