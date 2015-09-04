package org.inventivetalent.webframes;

import javax.annotation.Nullable;

public interface Callback<V> {

	void call(V value, @Nullable Throwable error);

}
