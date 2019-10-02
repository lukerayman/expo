package expo.modules.screenorientation;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

import org.unimodules.core.ExportedModule;
import org.unimodules.core.ModuleRegistry;
import org.unimodules.core.Promise;
import org.unimodules.core.errors.InvalidArgumentException;
import org.unimodules.core.interfaces.ActivityProvider;
import org.unimodules.core.interfaces.ExpoMethod;
import org.unimodules.core.interfaces.LifecycleEventListener;
import org.unimodules.core.interfaces.services.UIManager;

public class ScreenOrientationModule extends ExportedModule implements LifecycleEventListener {
  private ActivityProvider mActivityProvider;
  private Integer mInitialOrientation = null;
  static final String ERR_SCREEN_ORIENTATION = "ERR_SCREEN_ORIENTATION";

  public ScreenOrientationModule(Context context) {
    super(context);
  }

  @Override
  public String getName() {
    return "ExpoScreenOrientation";
  }

  @Override
  public void onCreate(ModuleRegistry moduleRegistry) {
    mActivityProvider = moduleRegistry.getModule(ActivityProvider.class);
    moduleRegistry.getModule(UIManager.class).registerLifecycleEventListener(this);
  }

  @Override
  public void onHostResume() {
    Activity activity = mActivityProvider.getCurrentActivity();
    if (activity != null && mInitialOrientation == null) {
      mInitialOrientation = activity.getRequestedOrientation();
    }
  }

  @Override
  public void onHostPause() {

  }

  @Override
  public void onHostDestroy() {

  }

  @Override
  public void onDestroy() {
    Activity activity = mActivityProvider.getCurrentActivity();
    if (activity != null && mInitialOrientation != null) {
      activity.setRequestedOrientation(mInitialOrientation);
    }
  }

  @ExpoMethod
  public void lockAsync(String orientationLockStr, Promise promise) {
    Activity activity = mActivityProvider.getCurrentActivity();

    try {
      OrientationLock orientationLock = OrientationLock.valueOf(orientationLockStr);
      int orientationAttr = orientationLockJSToNative(orientationLock);
      activity.setRequestedOrientation(orientationAttr);
    } catch (IllegalArgumentException e) {
      promise.reject("ERR_SCREEN_ORIENTATION_INVALID_ORIENTATION_LOCK", "An invalid OrientationLock was passed in: " + orientationLockStr, e);
      return;
    } catch (InvalidArgumentException e) {
      promise.reject(e);
      return;
    } catch (Exception e) {
      promise.reject(ERR_SCREEN_ORIENTATION, "Could not apply the ScreenOrientation lock: " + orientationLockStr, e);
      return;
    }
    promise.resolve(null);
  }

  @ExpoMethod
  public void lockPlatformAsync(int orientationAttr, Promise promise) {
    Activity activity = mActivityProvider.getCurrentActivity();

    try {
      activity.setRequestedOrientation(orientationAttr);
    } catch (Exception e) {
      promise.reject(ERR_SCREEN_ORIENTATION, "Could not apply the ScreenOrientation platform lock: " + orientationAttr, e);
      return;
    }
    promise.resolve(null);

  }

  @ExpoMethod
  public void unlockAsync(Promise promise) {
    lockAsync(OrientationLock.DEFAULT.toString(), promise);
  }

  @ExpoMethod
  public void getOrientationAsync(Promise promise) {
    Activity activity = mActivityProvider.getCurrentActivity();

    try {
      Orientation orientation = getScreenOrientation(activity);
      promise.resolve(orientation.toString()); // may not work
    } catch (Exception e) {
      promise.reject(ERR_SCREEN_ORIENTATION, "Could not get the current screen orientation", e);
    }
  }

  @ExpoMethod
  public void getOrientationLockAsync(Promise promise) {
    Activity activity = mActivityProvider.getCurrentActivity();

    try {
      int orientationAttr = activity.getRequestedOrientation();
      OrientationLock orientationLock = orientationLockNativeToJS(orientationAttr);
      promise.resolve(orientationLock.toString());
    } catch (Exception e) {
      promise.reject(ERR_SCREEN_ORIENTATION, "Could not get the current screen orientation lock", e);
    }

  }

  @ExpoMethod
  public void getPlatformOrientationLockAsync(Promise promise) {
    Activity activity = mActivityProvider.getCurrentActivity();
    try {
      promise.resolve(activity.getRequestedOrientation());
    } catch (Exception e) {
      promise.reject(ERR_SCREEN_ORIENTATION, "Could not get the current screen orientation platform lock", e);
    }

  }

  @ExpoMethod
  public void supportsOrientationLockAsync(String orientationLockStr, Promise promise){
    try {
      // If we can get the native orientation value from the given string without throwing, we resolve with true
      OrientationLock lockJS = OrientationLock.valueOf(orientationLockStr);
      orientationLockJSToNative(lockJS);
      promise.resolve(true);
    } catch (Exception e) {
      promise.resolve(false);
    }
  }

  // https://stackoverflow.com/questions/10380989/how-do-i-get-the-current-orientation-activityinfo-screen-orientation-of-an-a
  // Will not work in all cases as surface rotation is not standardized across android devices, but this is best effort
  private Orientation getScreenOrientation(Activity activity) {
    WindowManager windowManager = activity.getWindowManager();
    int rotation = windowManager.getDefaultDisplay().getRotation();
    DisplayMetrics dm = new DisplayMetrics();
    windowManager.getDefaultDisplay().getMetrics(dm);
    int width = dm.widthPixels;
    int height = dm.heightPixels;
    Orientation orientation;
    // if the device's natural orientation is portrait:
    if ((rotation == Surface.ROTATION_0
        || rotation == Surface.ROTATION_180) && height > width ||
        (rotation == Surface.ROTATION_90
            || rotation == Surface.ROTATION_270) && width > height) {
      switch (rotation) {
        case Surface.ROTATION_0:
          orientation = Orientation.PORTRAIT_UP;
          break;
        case Surface.ROTATION_90:
          orientation = Orientation.LANDSCAPE_LEFT;
          break;
        case Surface.ROTATION_180:
          orientation = Orientation.PORTRAIT_DOWN;
          break;
        case Surface.ROTATION_270:
          orientation = Orientation.LANDSCAPE_RIGHT;
          break;
        default:
          orientation = Orientation.UNKNOWN;
          break;
      }
    }

    // if the device's natural orientation is landscape or if the device
    // is square:
    else {
      switch (rotation) {
        case Surface.ROTATION_0:
          orientation = Orientation.LANDSCAPE_LEFT;
          break;
        case Surface.ROTATION_90:
          orientation = Orientation.PORTRAIT_DOWN;
          break;
        case Surface.ROTATION_180:
          orientation = Orientation.LANDSCAPE_RIGHT;
          break;
        case Surface.ROTATION_270:
          orientation = Orientation.PORTRAIT_UP;
          break;
        default:
          orientation = Orientation.UNKNOWN;
          break;
      }
    }
    return orientation;
  }

  public enum Orientation {
    PORTRAIT_UP,
    PORTRAIT_DOWN,
    LANDSCAPE_LEFT,
    LANDSCAPE_RIGHT,
    UNKNOWN;
  }

  public enum OrientationLock {
    DEFAULT,
    ALL,
    PORTRAIT,
    PORTRAIT_UP,
    PORTRAIT_DOWN,
    LANDSCAPE,
    LANDSCAPE_LEFT,
    LANDSCAPE_RIGHT,
    OTHER,
    ALL_BUT_UPSIDE_DOWN; // deprecated
  }

  private OrientationLock orientationLockNativeToJS(int orientationAttr) {
    switch (orientationAttr) {
      case ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED:
        return OrientationLock.DEFAULT;
      case ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR:
        return OrientationLock.ALL;
      case ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT:
        return OrientationLock.PORTRAIT;
      case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
        return OrientationLock.PORTRAIT_UP;
      case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
        return OrientationLock.PORTRAIT_DOWN;
      case ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE:
        return OrientationLock.LANDSCAPE;
      case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
        return OrientationLock.LANDSCAPE_LEFT;
      case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
        return OrientationLock.LANDSCAPE_RIGHT;
      default:
        return OrientationLock. OTHER;
    }
  }

  private int orientationLockJSToNative(OrientationLock orientationLock) {
    switch (orientationLock) {
      case DEFAULT:
        return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
      case ALL:
        return ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;
      case ALL_BUT_UPSIDE_DOWN:
        return ActivityInfo.SCREEN_ORIENTATION_SENSOR;
      case PORTRAIT:
        return ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
      case PORTRAIT_UP:
        return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
      case PORTRAIT_DOWN:
        return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
      case LANDSCAPE:
        return ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
      case LANDSCAPE_LEFT:
        return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
      case LANDSCAPE_RIGHT:
        return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
      default:
        throw new InvalidArgumentException("OrientationLock " + orientationLock.toString() + " is not mapped to a native Android orientation attr");
    }
  }
}
