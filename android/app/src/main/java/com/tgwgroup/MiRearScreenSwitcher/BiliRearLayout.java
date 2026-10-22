package com.tgwgroup.MiRearScreenSwitcher;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.graphics.Rect;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Runs in the authorized Shizuku user service; no app data or input injection. */
final class BiliRearLayout {
    private static final String PACKAGE = "tv.danmaku.bili";
    private final Map<Integer, Saved> saved = new HashMap<>();
    private static final class Saved {
        final Object token;
        final Rect bounds;
        final int mode;
        Saved(Object token, Rect bounds, int mode) {
            this.token = token; this.bounds = new Rect(bounds); this.mode = mode;
        }
    }
    private Object manager() throws Exception {
        return Class.forName("android.app.ActivityTaskManager").getMethod("getService").invoke(null);
    }
    private Class<?> api() throws Exception { return Class.forName("android.app.IActivityTaskManager"); }
    @SuppressWarnings("unchecked")
    private List<ActivityManager.RunningTaskInfo> tasks() throws Exception {
        return (List<ActivityManager.RunningTaskInfo>) api().getMethod("getTasks",
            int.class, boolean.class, boolean.class, int.class).invoke(manager(), 100, false, false, 1);
    }
    private boolean bili(ActivityManager.RunningTaskInfo task) {
        ComponentName base = task.baseActivity;
        ComponentName top = task.topActivity;
        return task.displayId == 1 && base != null && PACKAGE.equals(base.getPackageName())
            && top != null && PACKAGE.equals(top.getPackageName());
    }
    private Rect bounds(int id) throws Exception {
        return new Rect((Rect) api().getMethod("getTaskBounds", int.class).invoke(manager(), id));
    }
    private Object token(ActivityManager.RunningTaskInfo task) throws Exception {
        return task.getClass().getField("token").get(task);
    }
    private int mode(ActivityManager.RunningTaskInfo task) throws Exception {
        Object config = task.getClass().getField("configuration").get(task);
        Object window = config.getClass().getField("windowConfiguration").get(config);
        return (Integer) window.getClass().getMethod("getWindowingMode").invoke(window);
    }
    private void change(Object token, int mode, Rect bounds) throws Exception {
        Class<?> tokenClass = Class.forName("android.window.WindowContainerToken");
        Class<?> transactionClass = Class.forName("android.window.WindowContainerTransaction");
        Object transaction = transactionClass.getConstructor().newInstance();
        transactionClass.getMethod("setWindowingMode", tokenClass, int.class).invoke(transaction, token, mode);
        transactionClass.getMethod("setBounds", tokenClass, Rect.class).invoke(transaction, token, bounds);
        Class<?> organizerClass = Class.forName("android.window.WindowOrganizer");
        Object organizer = organizerClass.getConstructor().newInstance();
        organizerClass.getMethod("applyTransaction", transactionClass).invoke(organizer, transaction);
    }
    synchronized String apply(int width, int height) {
        Saved before = null;
        int taskId = -1;
        boolean changed = false;
        try {
            if (width <= height || height < 200 || width > 10000) {
                return "请先将背屏调整为参考图的横向方向，再应用布局";
            }
            ActivityManager.RunningTaskInfo selected = null;
            for (ActivityManager.RunningTaskInfo task : tasks()) {
                if (bili(task)) { selected = task; break; }
            }
            if (selected == null) return "请先打开原版B站，用快捷开关投放到背屏，再应用布局";
            taskId = selected.taskId;
            before = new Saved(token(selected), bounds(taskId), mode(selected));
            // Ratio measured from the user's 1536 px screenshot: content begins at x=503.
            Rect wanted = new Rect((int)Math.round(width * (503.0 / 1536.0)), 0, width, height);
            changed = true;
            change(before.token, 5, wanted); // Android freeform windowing mode.
            boolean verified = false;
            for (int attempt = 0; attempt < 8; attempt++) {
                Thread.sleep(150);
                Rect actual = bounds(taskId);
                if (wanted.equals(actual)) {
                    for (ActivityManager.RunningTaskInfo current : tasks()) {
                        if (current.taskId == taskId && bili(current) && mode(current) == 5) verified = true;
                    }
                    if (verified) break;
                }
            }
            if (!verified) throw new IllegalStateException("系统未接受指定窗口范围");
            saved.putIfAbsent(taskId, before);
            return "";
        } catch (Exception e) {
            String rollback = "";
            if (changed && before != null) {
                try {
                    change(before.token, before.mode, before.bounds);
                    if (!before.bounds.equals(bounds(taskId))) throw new IllegalStateException("Rollback not verified");
                }
                catch (Exception ignored) { rollback = "；还原未完成，请点击退出投放并恢复背屏"; }
            }
            android.util.Log.w("BiliRearLayout", "Layout unsupported", e);
            return "当前系统未能应用B站避让窗口" + rollback;
        }
    }
    synchronized void restoreTask(int id) throws Exception {
        Saved old = saved.get(id);
        if (old == null) return;
        change(old.token, old.mode, old.bounds);
        if (!old.bounds.equals(bounds(id))) throw new IllegalStateException("窗口还原未完成");
        saved.remove(id);
    }
    synchronized String reset() {
        try {
            for (ActivityManager.RunningTaskInfo task : tasks()) {
                if (!bili(task)) continue;
                if (saved.containsKey(task.taskId)) restoreTask(task.taskId);
                else change(token(task), 1, null); // Recovery after user-service restart.
            }
            return "";
        } catch (Exception e) {
            android.util.Log.w("BiliRearLayout", "Restore failed", e);
            return "还原B站窗口失败，请检查Shizuku后重试";
        }
    }
}
