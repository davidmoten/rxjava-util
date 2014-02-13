package com.github.davidmoten.rx;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func1;
import rx.util.functions.Functions;

import com.github.davidmoten.rx.operators.OperationLog;
import com.github.davidmoten.rx.operators.OperationShare;

public class RxUtil {

	public static <T> Observable<Observable<T>> doWhenAllComplete(
			final Observable<Observable<T>> original, final Action0 action) {
		return Observable.create(new OnSubscribeFunc<Observable<T>>() {

			@Override
			public Subscription onSubscribe(Observer<? super Observable<T>> o) {
				ConnectableObservable<Observable<T>> published = original
						.publish();
				Subscription sub1 = Observable.merge(published)
						.doOnCompleted(action).subscribe();
				Subscription sub2 = published.subscribe(o);
				Subscription sub3 = published.connect();
				return Subscriptions.from(sub1, sub2, sub3);
			}
		});
	}

	public static <T> Func1<T, T> println(final OutputStream out) {
		return new Func1<T, T>() {
			@Override
			public T call(T t) {
				try {
					out.write(t.toString().getBytes());
					out.write('\n');
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				return t;
			}
		};
	}

	public static <T> Func1<T, T> println() {
		return println(System.out);
	}

	public static <T> void print(Observable<T> o) {
		o.materialize().toBlockingObservable()
				.forEach(new Action1<Notification<T>>() {
					@Override
					public void call(Notification<T> notification) {
						System.out.println(notification);
					}
				});
	}

	public static <T> Observable<T> count(final Observable<T> obs,
			final Action1<Long> action) {
		return Observable.create(new OnSubscribeFunc<T>() {

			@Override
			public Subscription onSubscribe(final Observer<? super T> o) {
				final AtomicLong count = new AtomicLong(0);
				final Subscription sub = obs.subscribe(new Observer<T>() {

					@Override
					public void onCompleted() {
						action.call(count.get());
						o.onCompleted();
					}

					@Override
					public void onError(Throwable e) {
						o.onError(e);
					}

					@Override
					public void onNext(T t) {
						count.incrementAndGet();
						o.onNext(t);
					}
				});
				return new Subscription() {

					@Override
					public void unsubscribe() {
						sub.unsubscribe();
						action.call(count.get());
					}
				};
			}
		});
	}

	@SuppressWarnings("unchecked")
	public static <T> Observable<T> concatButIgnoreFirstSequence(
			Observable<?> o1, Observable<T> o2) {
		return Observable.concat(
				(Observable<T>) o1.filter(Functions.alwaysFalse()), o2);
	}

	/**
	 * All subscribers to <code>share(source)</code> will be actually be
	 * observers of a singleton subscription to the source. When all subscribers
	 * to <code>share(source)</code> have unsubscribed the singleton
	 * subscription is unsubscribed. You might use this method if the source
	 * Observable is resource intensive and should only be run once at a time
	 * with its emissions shared amongst many observers. An example is a high
	 * rate infinite stream read from a server socket. One might want multiple
	 * consumers to share the same stream rather than establishing their own
	 * socket connections to the server socket.
	 * 
	 * @param source
	 * @return <code>source</code> with modified subscription behaviour
	 */
	public static <T> Observable<T> share(final Observable<T> source) {
		// TODO share does not seem to play nicely with
		// Observable.retry().
		return Observable.create(OperationShare.share(source));
	}

	public static <T> Observable<T> log(final Observable<T> source) {
		return Observable.create(OperationLog.log(source));
	}

}
