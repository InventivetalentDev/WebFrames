package org.inventivetalent.webframes;

public interface Callback<V> {

	void call(V value, Throwable error);

}
