package com.action.screenmirror.utils;

import android.util.Log;

public class LogUtils {

	private static final String TAG = LogUtils.class.getSimpleName();

	private LogUtils() {
		/* cannot be instantiated */
		throw new UnsupportedOperationException("cannot be instantiated");
	}

	private static final boolean isDebug = false;// 闁哄嫷鍨伴幆渚�妫侀敓鐣屾啺娴ｇ懓鈪甸柛妤冩珰ug闁挎稑鑻ぐ鍙夌閵夈儲韬琣pplication闁汇劌鍩Create闁告垼濮ら弳鐔兼煂瀹�鍕〃闁告帗绻傞～鎰板礌閿燂拷

	// 濞戞挸顑夊浼村炊濞戞鍤嬮柡鍕靛灦缁垳鎷嬮ˇ姝沢闁汇劌瀚崵閬嶅极閿燂拷
	public static void i(String msg) {
		if (isDebug)
			Log.i(TAG, msg);
	}

	public static void d(String msg) {
		if (isDebug)
			Log.d(TAG, msg);
	}

	public static void e(String msg) {
		if (true)
			Log.e(TAG, msg);
	}

	public static void v(String msg) {
		if (isDebug)
			Log.v(TAG, msg);
	}

	// 濞戞挸顑夊浼村及椤栨瑧鐐婇柛蹇嬪劥閸ゆ粎锟借鐭粻鐒g闁汇劌瀚崵閬嶅极閿燂拷
	public static void i(String tag, String msg) {
		if (isDebug)
			Log.i(tag, msg);
	}

	public static void d(String tag, String msg) {
		if (isDebug)
			Log.i(tag, msg);
	}

	public static void e(String tag, String msg) {
		if (true)
			Log.i(tag, msg);
	}

	public static void v(String tag, String msg) {
		if (isDebug)
			Log.i(tag, msg);
	}

}
